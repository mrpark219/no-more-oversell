#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
PROJECT_NAME="${LOAD_TEST_PROJECT_NAME:-no-more-oversell-load-test}"
APP_IMAGE="no-more-oversell-load-test-app:latest"

export LOAD_TEST_APP_PORT="${LOAD_TEST_APP_PORT:-18080}"
export LOAD_TEST_TPS="${LOAD_TEST_TPS:-1000}"
export LOAD_TEST_DURATION_SECONDS="${LOAD_TEST_DURATION_SECONDS:-60}"
export PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-4000}"
export MAX_VUS="${MAX_VUS:-7000}"
export BUILD_IMAGE="${BUILD_IMAGE:-auto}"

COMPOSE=(docker compose -f "${COMPOSE_FILE}" -p "${PROJECT_NAME}")
FAILURES=0

PRODUCT_ID=910001
BASE_USER_ID=9100000
SALE_PRICE=10000
PAYMENT_METHOD=POINT
EXPECTED_SOLD_QUANTITY=10
TOTAL_REQUESTS=$((LOAD_TEST_TPS * LOAD_TEST_DURATION_SECONDS))
EXPECTED_CONFIRMED_QUANTITY="${EXPECTED_SOLD_QUANTITY}"
if [[ "${TOTAL_REQUESTS}" -lt "${EXPECTED_CONFIRMED_QUANTITY}" ]]; then
    EXPECTED_CONFIRMED_QUANTITY="${TOTAL_REQUESTS}"
fi
SEEDED_USER_COUNT=$((TOTAL_REQUESTS + 1000))
ORDER_SHEETS_FILE="${RESULTS_DIR}/order-sheets.json"

cleanup() {
    if [[ "${KEEP_STACK:-false}" != "true" ]]; then
        "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
    fi
}

trap cleanup EXIT

main() {
    mkdir -p "${RESULTS_DIR}"
    rm -f "${RESULTS_DIR}"/*.json "${RESULTS_DIR}"/*.txt 2>/dev/null || true

    print_environment
    write_environment_file

    "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
    prepare_app_image
    "${COMPOSE[@]}" up -d --no-build mysql redis app
    wait_for_app

    reset_data
    warm_up_app
    reset_data

    run_k6_phase "checkout-${LOAD_TEST_TPS}tps" "checkout" "${LOAD_TEST_DURATION_SECONDS}"
    export_order_sheets
    assert_order_sheet_count

    run_k6_phase "order-${LOAD_TEST_TPS}tps" "order" "${LOAD_TEST_DURATION_SECONDS}"

    print_k6_summary_table
    print_product_summary
    assert_product_state

    echo
    echo "== Result files =="
    ls -1 "${RESULTS_DIR}" || true

    if [[ "${FAILURES}" -gt 0 ]]; then
        echo
        echo "Load test finished with ${FAILURES} failure(s). Check ${RESULTS_DIR} and Docker logs."
        exit 1
    fi

    echo
    echo "Load test finished without assertion failures."
}

print_environment() {
    echo "== Load test stack =="
    echo "flow: checkout ${LOAD_TEST_TPS}TPS x ${LOAD_TEST_DURATION_SECONDS}s -> order ${LOAD_TEST_TPS}TPS"
    echo "total checkout target requests: ${TOTAL_REQUESTS}"
    echo "app port: ${LOAD_TEST_APP_PORT}"
    echo "app image: ${APP_IMAGE}"
    echo "build image: ${BUILD_IMAGE}"
    print_docker_resource_hint
    echo
}

prepare_app_image() {
    case "${BUILD_IMAGE}" in
        true)
            echo "building app image: ${APP_IMAGE}"
            "${COMPOSE[@]}" build app
            ;;
        false)
            if ! docker image inspect "${APP_IMAGE}" >/dev/null 2>&1; then
                echo "app image not found: ${APP_IMAGE}"
                echo "Run BUILD_IMAGE=true ./load-test/run.sh once, or set BUILD_IMAGE=auto."
                exit 1
            fi
            echo "using existing app image: ${APP_IMAGE}"
            ;;
        auto)
            if docker image inspect "${APP_IMAGE}" >/dev/null 2>&1; then
                echo "using existing app image: ${APP_IMAGE}"
            else
                echo "app image not found. building app image: ${APP_IMAGE}"
                "${COMPOSE[@]}" build app
            fi
            ;;
        *)
            echo "invalid BUILD_IMAGE=${BUILD_IMAGE}. Use auto, true, or false."
            exit 1
            ;;
    esac
}

print_docker_resource_hint() {
    local docker_info docker_cpus docker_memory_bytes docker_memory_gib
    docker_info="$(docker info --format '{{.NCPU}} {{.MemTotal}}' 2>/dev/null || true)"
    if [[ -z "${docker_info}" ]]; then
        echo "docker resources: unavailable"
        return
    fi

    read -r docker_cpus docker_memory_bytes <<< "${docker_info}"
    docker_memory_gib=$(((docker_memory_bytes + 1073741823) / 1073741824))
    echo "docker resources: cpu=${docker_cpus}, memory=${docker_memory_gib}GiB"
}

write_environment_file() {
    local file="${RESULTS_DIR}/environment.txt"
    local docker_info docker_cpus docker_memory_bytes docker_memory_gib
    docker_info="$(docker info --format '{{.NCPU}} {{.MemTotal}}' 2>/dev/null || true)"

    docker_cpus="unknown"
    docker_memory_bytes="unknown"
    docker_memory_gib="unknown"
    if [[ -n "${docker_info}" ]]; then
        read -r docker_cpus docker_memory_bytes <<< "${docker_info}"
        docker_memory_gib=$(((docker_memory_bytes + 1073741823) / 1073741824))
    fi

    {
        printf "created_at=%s\n" "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
        printf "scenario=checkout_then_order\n"
        printf "load_test_tps=%s\n" "${LOAD_TEST_TPS}"
        printf "load_test_duration_seconds=%s\n" "${LOAD_TEST_DURATION_SECONDS}"
        printf "total_checkout_target_requests=%s\n" "${TOTAL_REQUESTS}"
        printf "expected_confirmed_quantity=%s\n" "${EXPECTED_CONFIRMED_QUANTITY}"
        printf "app_image=%s\n" "${APP_IMAGE}"
        printf "build_image=%s\n" "${BUILD_IMAGE}"
        printf "docker_cpus=%s\n" "${docker_cpus}"
        printf "docker_memory_bytes=%s\n" "${docker_memory_bytes}"
        printf "docker_memory_gib=%s\n" "${docker_memory_gib}"
    } > "${file}"
}

wait_for_app() {
    echo
    echo "== Waiting for application =="
    local url="http://localhost:${LOAD_TEST_APP_PORT}/api/checkout?stayProductId=900001"
    for _ in $(seq 1 120); do
        if curl -fsS -H "userId: 900001" "${url}" >/dev/null 2>&1; then
            echo "application is ready"
            return
        fi
        sleep 1
    done

    echo "application did not become ready in time"
    "${COMPOSE[@]}" logs app || true
    exit 1
}

reset_data() {
    echo
    echo "== Reset load-test data =="
    {
        printf "SET @load_test_user_count = %s;\n" "${SEEDED_USER_COUNT}"
        cat "${SCRIPT_DIR}/sql/reset-load-test-data.sql"
    } | mysql_exec
    "${COMPOSE[@]}" exec -T redis redis-cli FLUSHALL >/dev/null
}

warm_up_app() {
    echo
    echo "== Warm up application =="
    local checkout_url="http://localhost:${LOAD_TEST_APP_PORT}/api/checkout?stayProductId=${PRODUCT_ID}"
    local pids=()

    for index in $(seq 1 40); do
        local user_id=$((BASE_USER_ID + index))
        curl -fsS -H "userId: ${user_id}" "${checkout_url}" >/dev/null 2>&1 &
        pids+=("$!")
    done

    for pid in "${pids[@]}"; do
        wait "${pid}" || true
    done

    echo "application warm-up completed"
}

mysql_exec() {
    "${COMPOSE[@]}" exec -T -e MYSQL_PWD=no_more_oversell mysql \
        mysql -u no_more_oversell no_more_oversell "$@"
}

mysql_scalar() {
    local sql="$1"
    mysql_exec -N -B -e "${sql}"
}

run_k6_phase() {
    local name="$1"
    local phase="$2"
    local duration_seconds="$3"

    echo
    echo "== K6: ${name} =="
    local status=0
    "${COMPOSE[@]}" run --rm --no-deps \
        -e BASE_URL=http://app:8080 \
        -e PHASE="${phase}" \
        -e SCENARIO_NAME="${name}" \
        -e PRODUCT_ID="${PRODUCT_ID}" \
        -e SALE_PRICE="${SALE_PRICE}" \
        -e PAYMENT_METHOD="${PAYMENT_METHOD}" \
        -e BASE_USER_ID="${BASE_USER_ID}" \
        -e RATE="${LOAD_TEST_TPS}" \
        -e DURATION_SECONDS="${duration_seconds}" \
        -e PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS}" \
        -e MAX_VUS="${MAX_VUS}" \
        -e ORDER_SHEETS_FILE=/results/order-sheets.json \
        k6 run \
        --summary-export "/results/${name}.summary.json" \
        /scripts/reservation-scenarios.js || status=$?

    if [[ "${status}" -ne 0 ]]; then
        echo "[FAIL] ${name}: k6 exited with status ${status}"
        FAILURES=$((FAILURES + 1))
    fi
}

export_order_sheets() {
    echo
    echo "== Export checkout order sheets =="
    mysql_exec -N -B -e "
        SELECT COALESCE(
            JSON_ARRAYAGG(
                JSON_OBJECT(
                    'userId', user_id,
                    'orderSheetToken', order_sheet_token,
                    'stayProductId', product_id
                )
            ),
            JSON_ARRAY()
        )
        FROM (
            SELECT user_id, order_sheet_token, product_id
            FROM order_sheet
            WHERE product_id = ${PRODUCT_ID}
            ORDER BY id
            LIMIT ${TOTAL_REQUESTS}
        ) AS order_sheets;
    " > "${ORDER_SHEETS_FILE}"

    local db_count exported_count
    db_count="$(order_sheet_count)"
    exported_count="${db_count}"
    if [[ "${exported_count}" -gt "${TOTAL_REQUESTS}" ]]; then
        exported_count="${TOTAL_REQUESTS}"
    fi
    echo "exported order sheets: ${exported_count}"
}

order_sheet_count() {
    mysql_scalar "SELECT COUNT(*) FROM order_sheet WHERE product_id = ${PRODUCT_ID};"
}

assert_order_sheet_count() {
    local count
    count="$(order_sheet_count)"
    if [[ "${count}" -ge "${TOTAL_REQUESTS}" ]]; then
        echo "[PASS] checkout-created-order-sheets=${count} >= ${TOTAL_REQUESTS}"
        return
    fi

    echo "[FAIL] checkout-created-order-sheets expected>=${TOTAL_REQUESTS}, actual=${count}"
    FAILURES=$((FAILURES + 1))
}

print_k6_summary_table() {
    echo
    echo "== K6 summary table =="

    if ! command -v jq >/dev/null 2>&1; then
        echo "jq is not installed. Read raw summary JSON files in ${RESULTS_DIR}."
        return
    fi

    printf "%-18s %8s %8s %8s %8s %8s %8s %8s %10s %10s %6s %7s\n" \
        "scenario" "iter/s" "http/s" "co/s" "order/s" "ok/s" "sold/s" "biz/s" "co_p95" "order_p95" "5xx" "dropped"

    local file scenario iterations_rate http_rate checkout_rate order_rate confirmed_rate sold_out_rate business_rate checkout_p95 order_p95 http_5xx dropped
    for file in "${RESULTS_DIR}"/*.summary.json; do
        scenario="$(basename "${file}" .summary.json)"
        iterations_rate="$(summary_metric "${file}" "iterations" "rate")"
        http_rate="$(summary_metric "${file}" "http_reqs" "rate")"
        checkout_rate="$(summary_metric "${file}" "checkout_success" "rate")"
        order_rate="$(summary_metric "${file}" "order_request" "rate")"
        confirmed_rate="$(summary_metric "${file}" "order_confirmed_response" "rate")"
        sold_out_rate="$(summary_metric "${file}" "sold_out" "rate")"
        business_rate="$(summary_metric "${file}" "business_failure" "rate")"
        checkout_p95="$(summary_metric "${file}" "checkout_duration" "p(95)")"
        order_p95="$(summary_metric "${file}" "order_duration" "p(95)")"
        http_5xx="$(summary_metric "${file}" "http_5xx" "count")"
        dropped="$(summary_metric "${file}" "dropped_iterations" "count")"

        printf "%-18s %8.1f %8.1f %8.1f %8.1f %8.1f %8.1f %8.1f %10.0f %10.0f %6.0f %7.0f\n" \
            "${scenario}" \
            "${iterations_rate}" \
            "${http_rate}" \
            "${checkout_rate}" \
            "${order_rate}" \
            "${confirmed_rate}" \
            "${sold_out_rate}" \
            "${business_rate}" \
            "${checkout_p95}" \
            "${order_p95}" \
            "${http_5xx}" \
            "${dropped}"
    done
}

summary_metric() {
    local file="$1"
    local metric="$2"
    local field="$3"

    jq -r --arg metric "${metric}" --arg field "${field}" '
        (.metrics[$metric][$field] // 0)
    ' "${file}"
}

print_product_summary() {
    echo
    echo "== DB summary: product ${PRODUCT_ID} =="
    mysql_exec -t -e "
        SELECT
            i.product_id,
            i.total_quantity,
            i.sold_quantity,
            COUNT(o.id) AS confirmed_orders,
            COUNT(DISTINCT o.user_id) AS distinct_buyers
        FROM inventory i
        LEFT JOIN orders o
            ON o.product_id = i.product_id
           AND o.status = 'CONFIRMED'
        WHERE i.product_id = ${PRODUCT_ID}
        GROUP BY i.product_id, i.total_quantity, i.sold_quantity;

        SELECT
            status,
            COALESCE(failure_reason, '-') AS failure_reason,
            COUNT(*) AS count
        FROM order_sheet
        WHERE product_id = ${PRODUCT_ID}
        GROUP BY status, failure_reason
        ORDER BY status, failure_reason;
    "
}

assert_product_state() {
    local result
    result="$(mysql_scalar "
        SELECT
            (SELECT COUNT(*) FROM orders WHERE product_id = ${PRODUCT_ID} AND status = 'CONFIRMED') AS confirmed_orders,
            (SELECT sold_quantity FROM inventory WHERE product_id = ${PRODUCT_ID}) AS sold_quantity,
            (
                SELECT COALESCE(MAX(order_count), 0)
                FROM (
                    SELECT COUNT(*) AS order_count
                    FROM orders
                    WHERE product_id = ${PRODUCT_ID}
                      AND status = 'CONFIRMED'
                    GROUP BY user_id
                ) AS user_orders
            ) AS max_orders_per_user;
    ")"

    local confirmed_orders sold_quantity max_orders_per_user
    read -r confirmed_orders sold_quantity max_orders_per_user <<< "${result}"

    assert_equals "reservation" "confirmed-orders" "${confirmed_orders}" "${EXPECTED_CONFIRMED_QUANTITY}"
    assert_equals "reservation" "sold-quantity" "${sold_quantity}" "${EXPECTED_CONFIRMED_QUANTITY}"
    assert_less_or_equal "reservation" "max-orders-per-user" "${max_orders_per_user}" 1
    assert_less_or_equal "reservation" "sold-quantity-cap" "${sold_quantity}" "${EXPECTED_SOLD_QUANTITY}"
}

assert_equals() {
    local name="$1"
    local label="$2"
    local actual="$3"
    local expected="$4"

    if [[ "${actual}" == "${expected}" ]]; then
        echo "[PASS] ${name}: ${label}=${actual}"
        return
    fi

    echo "[FAIL] ${name}: ${label} expected=${expected}, actual=${actual}"
    FAILURES=$((FAILURES + 1))
}

assert_less_or_equal() {
    local name="$1"
    local label="$2"
    local actual="$3"
    local expected_max="$4"

    if [[ "${actual}" -le "${expected_max}" ]]; then
        echo "[PASS] ${name}: ${label}=${actual} <= ${expected_max}"
        return
    fi

    echo "[FAIL] ${name}: ${label} expected<=${expected_max}, actual=${actual}"
    FAILURES=$((FAILURES + 1))
}

main "$@"
