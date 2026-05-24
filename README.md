# no-more-oversell

- 제한 수량 숙소 상품의 Checkout, 결제, 주문 확정을 처리하는 Spring Boot 애플리케이션입니다.
- 비관적 락(Pessimistic Lock)과 캐싱을 활용하여 대규모 트래픽 환경에서도 오버셀(Oversell)이 발생하지 않도록 보장합니다.

## 📌 바로가기

- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [로컬 실행](#-로컬-실행)
- [Docker 이미지 실행](#-docker-이미지-실행)
- [API 명세](#-api-명세)
- [예약/결제 흐름](#-예약결제-흐름)
- [ERD](#-erd)
- [테스트 검증](#-테스트-검증)
- [부하 테스트 요약](#-부하-테스트-요약)
- [관련 문서](#-관련-문서)

---

## 🛠 기술 스택

- **Language:** Java 21
- **Framework:** Spring Boot 3.5
- **Data:** Spring Data JPA, QueryDSL, Flyway, MySQL 8.4
- **Cache & Lock:** Redis 8.6.3
- **Stability:** Resilience4j Circuit Breaker
- **Test:** Testcontainers, K6

---

## 📂 프로젝트 구조

```text
src/main/java/me/park/nomoreoversell
├── common                 # 공통 예외 응답, WebMvc 설정, Cache Writer
├── config                 # QueryDSL, Cache Executor 설정
├── inventory              # 재고 도메인 (비관적 락 기반 재고 차감)
├── order                  # 주문 생성 API 및 주문 확정 흐름
├── ordersheet             # Checkout API 및 주문서 상태 관리
├── payment                # 결제 Aggregate, 결제수단 Handler, Mock PG Gateway
├── point                  # 포인트 조회, 차감, 복원
└── stayproduct            # 숙소 상품 조회 및 오픈 여부 검증

src/main/resources
├── application.yml        # Datasource, Redis, Flyway, Resilience4j 설정
└── db
    ├── migration          # Flyway DDL Migration 파일
    └── seed               # 로컬 테스트용 Seed Data

load-test                  # K6 부하 테스트 스크립트
http                       # 수동 API 호출 테스트 파일 (.http)
```

---

## 🚀 로컬 실행

### 1. 필요 도구

- JDK 21
- Docker / Docker Compose

### 2. Infra 컨테이너(MySQL, Redis) 실행

```bash
docker compose up -d mysql redis
```

- **MySQL:** `localhost:3306`
- **Redis:** `localhost:6379`

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

- **Application:** `localhost:8080`

> 💡 애플리케이션이 구동되면 **Flyway**에 의해 자동으로 데이터베이스 마이그레이션(DDL) 및 로컬 테스트용 Seed Data가 적재됩니다.

#### 📊 로컬 Seed Data 주요 정보

| 구분              | 값                                     |
|-----------------|---------------------------------------|
| **예약 가능 상품 ID** | `900001` (판매가: `10,000원` / 재고: `10개`) |
| **품절 상품 ID**    | `900002`                              |
| **테스트 사용자 ID**  | `900001` (보유 포인트: `100,000원`)         |

---

## 🐳 Docker 이미지 실행

### 1. 이미지 빌드

```bash
docker build -t no-more-oversell:local .
```

### 2. Infra 컨테이너 실행

애플리케이션 컨테이너가 Compose 네트워크 내부에서 `mysql`, `redis` 서비스명으로 연동되도록 프로젝트 네임을 고정하여 실행합니다.

```bash
COMPOSE_PROJECT_NAME=no-more-oversell docker compose up -d mysql redis
```

### 3. 앱 컨테이너 실행

```bash
docker run --rm \
  --name no-more-oversell-app \
  --network no-more-oversell_default \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://mysql:3306/no_more_oversell?serverTimezone=Asia/Seoul&characterEncoding=UTF-8' \
  -e SPRING_DATASOURCE_USERNAME=no_more_oversell \
  -e SPRING_DATASOURCE_PASSWORD=no_more_oversell \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  no-more-oversell:local
```

---

## 🔌 API 명세

> ⚠️ 인증/로그인 처리는 범위에서 제외하였으며, 사용자 식별은 HTTP Header의 `userId` 값으로 대신합니다.

### 1. Checkout (주문서 발행)

상품 정보, 실시간 재고 여부, 유저 가용 포인트를 조회하고 주문을 위한 토큰을 발급합니다.

- **HTTP Method & URL:** `GET /api/checkout`
- **Request Example:**

```bash
curl -X GET 'http://localhost:8080/api/checkout?stayProductId=900001' \
  -H 'userId: 900001'
```

- **Response Example (200 OK):**

```json
{
  "orderSheetToken": "4f8a7c7a-0c50-4b6b-b0a9-4c91b9d98b6a",
  "stayProduct": {
    "id": 900001,
    "accommodationName": "로컬 테스트 호텔",
    "roomName": "디럭스 더블",
    "ratePlanName": "선착순 특가",
    "originalPrice": 20000,
    "salePrice": 10000,
    "openAt": "2026-01-01T00:00:00",
    "checkinTime": "15:00:00",
    "checkoutTime": "11:00:00",
    "maxPerUser": 1,
    "hasStock": true
  },
  "user": {
    "availablePoint": 100000
  }
}
```

### 2. Orders (결제 승인 및 주문 확정)

발급받은 주문서 토큰과 복합 결제수단 정보를 검증하여 최종 결제를 완료하고 주문을 생성합니다.

- **HTTP Method & URL:** `POST /api/orders`
- **Request Example:**

```bash
curl -X POST 'http://localhost:8080/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'userId: 900001' \
  -d '{
    "orderSheetToken": "4f8a7c7a-0c50-4b6b-b0a9-4c91b9d98b6a",
    "stayProductId": 900001,
    "paymentDetails": [
      {"paymentMethod": "CARD", "amount": 9000},
      {"paymentMethod": "POINT", "amount": 1000}
    ]
  }'
```

- **Response Example (200 OK):**

```json
{
  "order": {
    "id": 1,
    "orderToken": "129b9e91-ec57-4374-8c08-00f40df452c5",
    "orderSheetToken": "4f8a7c7a-0c50-4b6b-b0a9-4c91b9d98b6a",
    "status": "CONFIRMED"
  },
  "stayProduct": {
    "id": 900001,
    "accommodationName": "로컬 테스트 호텔",
    "roomName": "디럭스 더블",
    "ratePlanName": "선착순 특가"
  },
  "payment": {
    "status": "APPROVED",
    "totalPaymentAmount": 10000,
    "details": [
      {
        "paymentMethod": "CARD",
        "paymentAmount": 9000,
        "status": "APPROVED"
      },
      {
        "paymentMethod": "POINT",
        "paymentAmount": 1000,
        "status": "APPROVED"
      }
    ]
  }
}
```

#### 🚨 주요 비즈니스 에러 코드 (Error Code)

- `SOLD_OUT`: 상품 품절
- `ORDER_IN_PROGRESS`: 현재 주문 처리 중
- `PAYMENT_FAILED`: PG사 결제 승인 실패
- `INVALID_PAYMENT_COMBINATION`: 결제 금액 불일치 혹은 잘못된 조합
- `PURCHASE_LIMIT_EXCEEDED`: 유저당 최대 구매 수량 초과
- `INVALID_ORDER_SHEET_STATE`: 유효하지 않은 주문서 상태
- `ORDER_SHEET_OWNER_MISMATCH`: 주문서 생성자와 요청자 불일치
- `PRODUCT_NOT_OPEN`: 상품 판매 기간이 아님
- `INSUFFICIENT_POINT_BALANCE`: 포인트 잔액 부족

---

## 🔄 예약/결제 흐름

### 1. GET /api/checkout

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant CheckoutController
    participant OrderSheetService
    participant Redis
    participant MySQL

    Client ->> CheckoutController: GET /api/checkout?stayProductId=900001
    CheckoutController ->> OrderSheetService: checkout(userId, stayProductId)
    OrderSheetService ->> Redis: checkout response 조회 (Circuit Breaker)
    alt cache hit
        Redis -->> OrderSheetService: CheckoutResponse (기존 orderSheetToken 반환)
    else cache miss or Redis fallback
        OrderSheetService ->> MySQL: 상품 조회 및 오픈 여부 검증
        OrderSheetService ->> MySQL: 재고 여부 조회
        OrderSheetService ->> MySQL: 포인트 잔액 조회
        OrderSheetService ->> MySQL: order_sheet CREATED 저장
        OrderSheetService --) Redis: checkout response 비동기 저장 (Circuit Breaker)
    end
    OrderSheetService -->> Client: orderSheetToken 포함 checkout 응답
```

### 2. POST /api/orders

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant OrderController
    participant OrderService
    participant PaymentHandler
    participant InventoryService
    participant MySQL

    Client ->> OrderController: POST /api/orders
    OrderController ->> OrderService: createOrder(request)

    Note over OrderService,MySQL: [1단계] 주문서 검증 및 상태 전환 (트랜잭션)
    OrderService ->> MySQL: order_sheet FOR UPDATE
    alt order_sheet CONFIRMED (재요청)
        OrderService ->> MySQL: orders, payment 조회
        OrderService -->> Client: 기존 확정 주문 응답
    else order_sheet APPROVING
        OrderService -->> Client: ORDER_IN_PROGRESS
    else order_sheet CREATED
        OrderService ->> OrderService: 소유자, 상품 ID, 결제 금액 검증
        OrderService ->> MySQL: order_sheet APPROVING 변경
    end

    Note over OrderService,PaymentHandler: [2단계] 결제 승인 (트랜잭션 없음)
    loop 결제수단별
        OrderService ->> PaymentHandler: approve(paymentMethod, amount)
    end
    alt 승인 실패
        OrderService ->> PaymentHandler: 이미 승인된 결제 역순 cancel
        OrderService ->> MySQL: order_sheet FAILED 저장
        OrderService -->> Client: PAYMENT_FAILED
    end

    Note over OrderService,MySQL: [3단계] 주문 확정 (트랜잭션)
    OrderService ->> MySQL: order_sheet FOR UPDATE (재검증)
    OrderService ->> InventoryService: reserveOne(productId)
    InventoryService ->> MySQL: inventory FOR UPDATE, sold_quantity 증가
    OrderService ->> OrderService: 인당 구매 한도 검증
    alt 재고 부족 or 한도 초과
        OrderService ->> PaymentHandler: 승인된 결제 역순 cancel
        OrderService ->> MySQL: order_sheet FAILED, payment(canceled) 저장
        OrderService -->> Client: SOLD_OUT / PURCHASE_LIMIT_EXCEEDED
    end
    OrderService ->> MySQL: payment, orders CONFIRMED 저장
    OrderService ->> MySQL: order_sheet CONFIRMED 변경
    OrderService -->> Client: confirmed order 응답
```

---

## 📊 ERD

```mermaid
erDiagram
    STAY_PRODUCT {
        bigint id PK
        varchar accommodation_name
        varchar room_name
        varchar rate_plan_name
        bigint original_price
        bigint sale_price
        datetime open_at
        varchar status
        time checkin_time
        time checkout_time
        bigint max_per_user
        datetime created_at
        datetime updated_at
    }

    INVENTORY {
        bigint id PK
        bigint product_id UK
        bigint total_quantity
        bigint sold_quantity
        datetime created_at
        datetime updated_at
    }

    POINT {
        bigint id PK
        bigint user_id UK
        bigint balance
        datetime created_at
        datetime updated_at
    }

    ORDER_SHEET {
        bigint id PK
        varchar order_sheet_token UK
        bigint user_id
        bigint product_id
        bigint original_price
        bigint sale_price
        varchar status
        varchar failure_reason
        datetime created_at
        datetime updated_at
    }

    PAYMENT {
        bigint id PK
        bigint order_sheet_id UK
        varchar payment_token UK
        varchar status
        datetime created_at
        datetime updated_at
    }

    PAYMENT_DETAIL {
        bigint id PK
        bigint payment_id FK
        varchar payment_method
        bigint payment_amount
        bigint canceled_amount
        bigint remaining_amount
        varchar external_transaction_key
        varchar cancel_key
        varchar status
        varchar failure_reason
        datetime approved_at
        datetime canceled_at
        datetime created_at
        datetime updated_at
    }

    ORDERS {
        bigint id PK
        varchar order_token UK
        bigint order_sheet_id UK
        bigint user_id
        bigint product_id
        bigint original_price
        bigint sale_price
        varchar status
        datetime created_at
        datetime updated_at
    }

    STAY_PRODUCT ||--|| INVENTORY: ""
    STAY_PRODUCT ||--o{ ORDER_SHEET: ""
    ORDER_SHEET ||--o| PAYMENT: ""
    PAYMENT ||--o{ PAYMENT_DETAIL: ""
    ORDER_SHEET ||--o| ORDERS: ""
```

---

## 🧪 테스트 검증

통합 테스트의 경우 **Testcontainers**를 이용하여 실제 MySQL 환경과 유기적으로 매핑하여 실행됩니다.

- **단위 테스트 수행:**
  ```bash
  ./gradlew test
  ```
- **통합 테스트 수행:**
  ```bash
  ./gradlew integrationTest
  ```
- **전체 코드 검증 및 빌드:**
  ```bash
  ./gradlew check
  ```

---

## 📈 부하 테스트 요약

**K6** 기반 부하 테스트를 통해 대규모 주문 트래픽 상황에서의 동시성 제어 유효성을 검증했습니다.

- **실행 방법:** `./load-test/run.sh`
- **테스트 시나리오:** Checkout API 및 Order API 각각 `1,000 TPS × 60초` 발생

### 📊 성능 지표 요약

- **Checkout 단계:** Redis 캐싱을 기반으로 모든 인프라 환경에서 목표치 **`1,000 TPS` 안정적 유지**
- **Order 단계:** 락 경합(Lock Contention) 제어로 인해 컨테이너 할당 CPU 개수에 따라 처리 속도 차이 발생
    - **6 CPU:** `682.8/s` 처리, p95 `9.80초`
    - **8 CPU:** `820.3/s` 처리, p95 `7.43초`
    - **10 CPU:** `906.4/s` 처리, p95 `5.13초`
- **Order 한계:** 10 CPU에서도 `dropped_iterations`가 남아 목표 `1,000 TPS`를 완전히 유지하지는 못함

> 💡 **주요 결론:** 모든 부하 환경에서 5xx 서버 에러가 0건이었으며, 고부하 경합 상태에서도 최종 확정 주문 수와 재고 차감 개수가 실제 제한 재고 수량을 초과하지 않아 오버셀 제로(0)를 검증했습니다.

---

## 📄 관련 문서

- [DECISIONS.md](DECISIONS.md) - 주요 설계 결정 및 트레이드 오프(Trade-off) 기록
- [AI_USAGE.md](AI_USAGE.md) - 개발 생산성 향상을 위한 AI 활용 기록
- [load-test/README.md](load-test/README.md) - 부하 테스트 환경 구성 및 결과 리포트
