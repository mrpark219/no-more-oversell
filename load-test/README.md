# Load Test

- 예약/결제 고가용성 확인을 위한 K6 기반의 부하 테스트입니다.
- 여기서는 별도의 환경 구성 없이 Docker, Docker Compose를 통해 쉽게 테스트를 실행할 수 있습니다.

```bash
./load-test/run.sh
```

- 기본값은 Checkout API를 `1000TPS x 60초`로 호출한 뒤, 생성된 주문서 토큰을 사용해 Order API를 `1000TPS x 60초`로 이어서 호출합니다.
  TPS와 지속 시간은 환경변수로 변경할 수 있습니다.

```bash
LOAD_TEST_TPS=500 LOAD_TEST_DURATION_SECONDS=300 ./load-test/run.sh
```

## 테스트 흐름

- 00시에 `500~1000TPS`가 `1~5분` 동안 몰리는 상황을 가정하고, 10개 한정 상품의 재고 정합성과 사용자별 구매 제한을 확인합니다.
- Checkout과 Order가 분리된 흐름이므로 테스트도 두 단계를 순차 실행합니다.

1. `GET /api/checkout`을 목표 TPS와 지속 시간만큼 호출합니다.
2. Checkout 성공으로 생성된 주문서를 `load-test/results/order-sheets.json`에 저장합니다.
3. 저장된 `userId`, `orderSheetToken`, `stayProductId`로 `POST /api/orders`를 같은 TPS로 호출합니다.
4. DB 기준으로 확정 주문 수, 판매 수량, 사용자별 최대 구매 수를 검증합니다.

## 검증 기준

- Checkout, Order 단계에서 5xx가 발생하지 않아야 합니다.
- Checkout 단계에서는 목표 요청 수만큼 주문서가 생성되어야 합니다.
- Order 단계의 `dropped_iterations`는 처리 여력 확인 지표로 기록하되, 최종 성공 여부는 5xx 발생 여부와 DB 정합성으로 판단합니다.
- 목표 요청 수가 재고보다 많으면 확정 주문 수는 10개여야 합니다.
- 목표 요청 수가 재고보다 많으면 재고 판매 수량은 10개여야 합니다.
- 사용자별 확정 주문은 최대 1개여야 합니다.
- 재고 소진 이후 실패는 `SOLD_OUT` 같은 비즈니스 실패로 분리되어야 합니다.

## 측정 결과

- `PRE_ALLOCATED_VUS=4000`, `MAX_VUS=7000`으로 고정하고 Docker CPU만 6, 8, 10으로 변경해 측정했습니다.
- Checkout은 모든 환경에서 목표 TPS를 유지했고, Order는 CPU를 늘릴수록 처리량과 지연 시간이 개선되었습니다.

### 테스트 사양

| 항목               | 값                                                 |
|------------------|---------------------------------------------------|
| Docker CPU       | 6, 8, 10                                          |
| Docker Memory    | 4GiB                                              |
| 이미지 빌드           | `BUILD_IMAGE=false`                               |
| 애플리케이션 포트        | `18080`                                           |
| 테스트 흐름           | Checkout `1000TPS x 60초` -> Order `1000TPS x 60초` |
| Checkout 목표 요청 수 | 60,000                                            |
| K6 VU            | `preAllocatedVUs=4000`, `maxVUs=7000`             |

### 결과

| Docker CPU | 단계       |  처리 건수 |     처리량 |   p95 |   p99 | dropped iterations | 5xx |
|-----------:|----------|-------:|--------:|------:|------:|-------------------:|----:|
|          6 | Checkout | 60,001 | 999.8/s | 3.04s | 3.31s |                  0 |   0 |
|          6 | Order    | 47,140 | 682.8/s | 9.80s | 9.98s |             12,859 |   0 |
|          8 | Checkout | 60,000 | 999.8/s | 1.30s | 1.54s |                  0 |   0 |
|          8 | Order    | 55,257 | 820.3/s | 7.43s | 7.58s |              4,746 |   0 |
|         10 | Checkout | 60,004 | 999.8/s | 1.26s | 1.51s |                  0 |   0 |
|         10 | Order    | 58,647 | 906.4/s | 5.13s | 5.43s |              1,354 |   0 |

### 해석

- CPU를 늘릴수록 Order 처리량은 `682.8/s` -> `820.3/s` -> `906.4/s`로 증가했습니다.
- Order p95 응답 시간은 `9.80s` -> `7.43s` -> `5.13s`로 감소했고, `dropped_iterations`도 12,859건 -> 4,746건 -> 1,354건으로 줄었습니다.
- CPU 증가에 따라 처리량과 지연 시간이 함께 개선되므로, 현재 Order 병목에는 애플리케이션 CPU 자원이 의미 있게 영향을 주는 것으로 볼 수 있습니다.
- 10 CPU에서도 `dropped_iterations`가 남아 Order 단계는 목표 `1000TPS`를 완전히 유지하지는 못했습니다. 다만 모든 환경에서 5xx는 0건이었고, 처리된 요청은 확정 또는 기대한 비즈니스 실패로 분리되었습니다.
- 세 테스트 모두 Order에 확정 주문은 10건만 생성되었고, DB 기준 확정 주문 수 10건, 판매 수량 10개, 구매자 10명으로 재고 정합성 검증은 통과했습니다.

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
| `PRE_ALLOCATED_VUS`          |  4000 | K6 사전 할당 VU                                  |
| `MAX_VUS`                    |  7000 | K6 최대 VU                                     |
| `BUILD_IMAGE`                |  auto | `auto`, `true`, `false` 중 하나로 앱 이미지 빌드 여부 제어 |
| `KEEP_STACK`                 | false | true면 테스트 후 컨테이너를 내리지 않음                     |
