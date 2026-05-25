import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://app:8080';
const PHASE = __ENV.PHASE || 'checkout';
const SCENARIO_NAME = __ENV.SCENARIO_NAME || `reservation-${PHASE}`;
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || '910001');
const SALE_PRICE = Number(__ENV.SALE_PRICE || '10000');
const PAYMENT_METHOD = __ENV.PAYMENT_METHOD || 'POINT';
const BASE_USER_ID = Number(__ENV.BASE_USER_ID || '9100000');
const RATE = Number(__ENV.RATE || '1000');
const DURATION_SECONDS = Number(__ENV.DURATION_SECONDS || '60');
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '4000');
const MAX_VUS = Number(__ENV.MAX_VUS || '7000');
const ORDER_SHEETS_FILE = __ENV.ORDER_SHEETS_FILE || '/results/order-sheets.json';

const orderSheets = new SharedArray('order sheets', () => {
    if (PHASE !== 'order') {
        return [];
    }

    return JSON.parse(open(ORDER_SHEETS_FILE));
});

if (PHASE === 'order' && orderSheets.length === 0) {
    throw new Error(`order sheets file is empty: ${ORDER_SHEETS_FILE}`);
}

const checkoutSuccess = new Counter('checkout_success');
const orderRequest = new Counter('order_request');
const orderConfirmedResponse = new Counter('order_confirmed_response');
const soldOut = new Counter('sold_out');
const purchaseLimitExceeded = new Counter('purchase_limit_exceeded');
const orderInProgress = new Counter('order_in_progress');
const paymentFailed = new Counter('payment_failed');
const invalidOrderSheetState = new Counter('invalid_order_sheet_state');
const businessFailure = new Counter('business_failure');
const http5xx = new Counter('http_5xx');
const successRate = new Rate('success_rate');
const checkoutDuration = new Trend('checkout_duration');
const orderDuration = new Trend('order_duration');

export const options = {
    scenarios: {
        reservation: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: `${DURATION_SECONDS}s`,
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
            tags: {
                scenario_name: SCENARIO_NAME,
                phase: PHASE,
            },
        },
    },
    thresholds: {
        http_5xx: ['count==0'],
    },
    summaryTrendStats: ['min', 'avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
    if (PHASE === 'checkout') {
        checkout(userIdForIteration());
        return;
    }

    const orderSheet = orderSheetForIteration();
    if (!orderSheet) {
        return;
    }

    createOrder(orderSheet);
}

function userIdForIteration() {
    return BASE_USER_ID + exec.scenario.iterationInTest + 1;
}

function orderSheetForIteration() {
    const index = exec.scenario.iterationInTest;
    if (index >= orderSheets.length) {
        return null;
    }
    return orderSheets[index];
}

function checkout(userId) {
    // Checkout 단계는 주문서 진입 API의 00시 집중 트래픽을 검증한다.
    // 응답으로 생성된 주문서는 run.sh가 DB에서 추출해 order 단계 입력으로 사용한다.
    const response = http.get(`${BASE_URL}/api/checkout?stayProductId=${PRODUCT_ID}`, {
        headers: {
            userId: String(userId),
        },
        tags: {
            phase: 'checkout',
            scenario_name: SCENARIO_NAME,
        },
    });

    checkoutDuration.add(response.timings.duration);
    recordHttpFailure(response);

    const body = parseJson(response);
    const ok = response.status === 200 && Boolean(body && body.orderSheetToken);
    checkoutSuccess.add(ok ? 1 : 0);
    successRate.add(ok);

    check(response, {
        'checkout returns order sheet token': () => ok,
    });
}

function createOrder(orderSheet) {
    // Order 단계는 checkout 단계에서 생성된 주문서 토큰을 그대로 사용해 결제/예약 완료를 검증한다.
    // POINT 단일 결제로 Mock PG 확률 실패를 제거하고 재고 정합성에 집중한다.
    orderRequest.add(1);

    const response = http.post(
        `${BASE_URL}/api/orders`,
        JSON.stringify({
            orderSheetToken: orderSheet.orderSheetToken,
            stayProductId: orderSheet.stayProductId,
            paymentDetails: [
                {
                    paymentMethod: PAYMENT_METHOD,
                    amount: SALE_PRICE,
                },
            ],
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                userId: String(orderSheet.userId),
            },
            tags: {
                phase: 'order',
                scenario_name: SCENARIO_NAME,
            },
        }
    );

    orderDuration.add(response.timings.duration);
    recordHttpFailure(response);

    const body = parseJson(response);
    const confirmed = response.status === 200 && body && body.order && body.order.status === 'CONFIRMED';
    orderConfirmedResponse.add(confirmed ? 1 : 0);

    if (!confirmed) {
        recordBusinessFailure(body);
    }

    const expected = confirmed || isExpectedBusinessFailure(body);
    successRate.add(expected);
    check(response, {
        'order is confirmed or expected business failure': () => expected,
    });
}

function isExpectedBusinessFailure(body) {
    const code = body && body.code;
    return [
        'SOLD_OUT',
        'PURCHASE_LIMIT_EXCEEDED',
        'ORDER_IN_PROGRESS',
        'PAYMENT_FAILED',
        'INVALID_ORDER_SHEET_STATE',
    ].includes(code);
}

function recordHttpFailure(response) {
    if (response.status >= 500) {
        http5xx.add(1);
    }
}

function recordBusinessFailure(body) {
    const code = body && body.code;
    if (!code) {
        return;
    }

    businessFailure.add(1);

    switch (code) {
        case 'SOLD_OUT':
            soldOut.add(1);
            break;
        case 'PURCHASE_LIMIT_EXCEEDED':
            purchaseLimitExceeded.add(1);
            break;
        case 'ORDER_IN_PROGRESS':
            orderInProgress.add(1);
            break;
        case 'PAYMENT_FAILED':
            paymentFailed.add(1);
            break;
        case 'INVALID_ORDER_SHEET_STATE':
            invalidOrderSheetState.add(1);
            break;
        default:
            break;
    }
}

function parseJson(response) {
    try {
        return response.json();
    } catch (e) {
        return null;
    }
}
