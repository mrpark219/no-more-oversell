# Load Test

예약/결제 요구사항을 K6로 테스트하기 위한 부하 테스트입니다.
여기서는 별도의 환경 구성 없이 Docker, Docker Compose를 통해 쉽게 테스트를 실행할 수 있습니다.

```bash
./load-test/run.sh
```

기본값은 Checkout API를 `1000TPS x 60초`로 호출한 뒤, 생성된 주문서 토큰을 사용해 Order API를 `1000TPS x 60초`로 이어서 호출합니다.
TPS와 지속 시간은 환경변수로 변경할 수 있습니다.

```bash
LOAD_TEST_TPS=500 LOAD_TEST_DURATION_SECONDS=300 ./load-test/run.sh
```

## 테스트 흐름

00시에 `500~1000TPS`가 `1~5분` 동안 몰리는 상황을 가정하고, 10개 한정 상품의 재고 정합성과 사용자별 구매 제한을 확인합니다.
Checkout과 Order가 분리된 흐름이므로 테스트도 두 단계를 순차 실행합니다.

1. `GET /api/checkout`을 목표 TPS와 지속 시간만큼 호출합니다.
2. Checkout 성공으로 생성된 주문서를 `load-test/results/order-sheets.json`에 저장합니다.
3. 저장된 `userId`, `orderSheetToken`, `stayProductId`로 `POST /api/orders`를 같은 TPS로 호출합니다.
4. DB 기준으로 확정 주문 수, 판매 수량, 사용자별 최대 구매 수를 검증합니다.

K6는 여러 VU의 응답 값을 하나의 파일로 직접 모으기 어렵습니다.
따라서 Checkout 응답으로 생성된 원천 데이터인 `order_sheet` 테이블을 조회해 다음 단계 입력으로 사용합니다.
주문서 토큰은 Checkout 성공 시 DB에 저장되는 값이므로, Order 단계는 실제 Checkout을 거친 주문서만 사용합니다.

## 검증 기준

- Checkout 단계에서 5xx가 발생하지 않아야 합니다.
- Order 단계에서 5xx가 발생하지 않아야 합니다.
- K6 `dropped_iterations`가 없어야 합니다.
- 목표 요청 수가 재고보다 많으면 확정 주문 수는 10개여야 합니다.
- 목표 요청 수가 재고보다 많으면 재고 판매 수량은 10개여야 합니다.
- 사용자별 확정 주문은 최대 1개여야 합니다.
- 재고 소진 이후 실패는 `SOLD_OUT` 같은 비즈니스 실패로 분리되어야 합니다.

## Docker 이미지

기본값은 `BUILD_IMAGE=auto`입니다.
이미 `no-more-oversell-load-test-app:latest` 이미지가 있으면 재사용하고, 없으면 한 번만 빌드합니다.

```bash
BUILD_IMAGE=auto ./load-test/run.sh
```

애플리케이션 코드를 변경한 뒤 새 이미지가 필요하면 강제로 빌드합니다.

```bash
BUILD_IMAGE=true ./load-test/run.sh
```

기존 이미지만 사용하고 싶으면 빌드를 끌 수 있습니다.
이 경우 이미지가 없으면 실행을 중단합니다.

```bash
BUILD_IMAGE=false ./load-test/run.sh
```

## 애플리케이션 설정

부하 테스트는 애플리케이션 튜닝 값을 주입하지 않습니다.
Compose에서는 컨테이너 내부에서 MySQL과 Redis를 찾기 위한 접속 정보만 전달합니다.
Hikari, Tomcat, Redis timeout 같은 값은 애플리케이션 기본 설정 그대로 사용합니다.

## 결과 파일

- `load-test/results/environment.txt`: 실행 조건과 Docker 리소스
- `load-test/results/order-sheets.json`: Checkout 단계에서 생성된 주문서 입력
- `load-test/results/checkout-{TPS}tps.summary.json`: Checkout K6 요약
- `load-test/results/order-{TPS}tps.summary.json`: Order K6 요약

## 사용할 수 있는 옵션

| 환경변수                         |   기본값 | 의미                                           |
|------------------------------|------:|----------------------------------------------|
| `LOAD_TEST_TPS`              |  1000 | Checkout과 Order 각각의 목표 TPS                   |
| `LOAD_TEST_DURATION_SECONDS` |    60 | Checkout과 Order 단계의 지속 시간                    |
| `PRE_ALLOCATED_VUS`          |  1200 | K6 사전 할당 VU                                  |
| `MAX_VUS`                    |  2400 | K6 최대 VU                                     |
| `BUILD_IMAGE`                |  auto | `auto`, `true`, `false` 중 하나로 앱 이미지 빌드 여부 제어 |
| `KEEP_STACK`                 | false | true면 테스트 후 컨테이너를 내리지 않음                     |
