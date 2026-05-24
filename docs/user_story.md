# Kotlin 증권 과제: 자산 가격 추적 API 개발기

## 프롤로그

Kotlin 증권은 고객이 여러 시장의 투자 자산 가격을 손쉽게 추적할 수 있는 서비스를 준비하고 있습니다.

처음에는 단순했습니다.

> "마켓을 만들고, 자산을 등록하고, 가격을 저장하자."

하지만 실제 금융 서비스에서는 단순한 CRUD만으로는 부족했습니다.

자산은 반드시 특정 마켓에 속해야 했고, 가격 이력은 특정 자산에 연결되어야 했습니다.
시장마다 통화와 시간대가 달랐고, 자산은 거래 중지나 상장 폐지 상태가 될 수도 있었습니다.

가격 데이터 역시 단순히 "언제 얼마였는가"만 저장해서는 부족했습니다.
외부 시세 제공자마다 데이터가 들어오는 시점이 다를 수 있고, 실제 시장 시각과 서버가 데이터를 받은 시각도 구분되어야 했습니다.

고객은 가격 목록뿐 아니라 최고가, 최저가, 평균가 같은 분석 정보를 원했고, 인기 자산에는 동시에 많은 가격 업데이트가 들어오기 시작했습니다.

## 도메인 모델

### Market

마켓은 자산이 거래되는 시장입니다.

예: KOSPI, NASDAQ, CRYPTO

| 컬럼 | 설명 |
| --- | --- |
| `id` | 마켓 ID |
| `name` | 마켓 이름 |
| `timezone` | 마켓 기준 시간대 |
| `createdAt` | 생성 시각 |
| `updatedAt` | 수정 시각 |

#### 제약 조건

- `name`은 중복될 수 없습니다.
- `timezone`은 필수입니다.
- `createdAt`, `updatedAt`은 서버에서 기록합니다.

### Asset

자산은 반드시 하나의 마켓에 속합니다.

예:

- KOSPI - 삼성전자
- NASDAQ - AAPL
- CRYPTO - BTC

| 컬럼 | 설명 |
| --- | --- |
| `id` | 자산 ID |
| `market` | 소속 마켓 |
| `symbol` | 자산 심볼 |
| `name` | 자산 이름 |
| `status` | 자산 상태 |
| `currency` | 가격 통화 |
| `createdAt` | 생성 시각 |
| `updatedAt` | 수정 시각 |

#### AssetStatus 예시

- `ACTIVE`
- `INACTIVE`
- `DELISTED`

#### Currency 예시

- `KRW`
- `USD`
- `BTC`
- `USDT`

#### 제약 조건

- 자산은 반드시 마켓에 속해야 합니다.
- 하나의 마켓 안에서 같은 `symbol`은 중복될 수 없습니다.
- Unique index: `(market_id, symbol)`

### 최신 가격 정보

가격 이력만 append 한다면 동시성 문제는 비교적 단순합니다.
하지만 화면이나 외부 연동에서 자산의 최신 가격을 빠르게 조회하려면 최신 가격 정보를 별도로 관리해야 합니다.

아래 방식 중 하나를 선택해 구현합니다.

#### 선택지 A. Asset에 최신 가격 컬럼 추가

- `Asset.currentPrice`
- `Asset.lastPriceAt`
- `Asset.lastPriceSource`
- `Asset.version`

#### 선택지 B. LatestPrice 테이블 분리

| 컬럼 | 설명 |
| --- | --- |
| `asset` | 대상 자산 |
| `price` | 최신 가격 |
| `timestamp` | 최신 가격의 시장 기준 시각 |
| `source` | 최신 가격 출처 |
| `version` | 낙관적 락 버전 |
| `updatedAt` | 수정 시각 |

최신 가격을 갱신하는 로직에는 동시성 제어가 적용되어야 합니다.

> 최신 가격은 자산 마스터 정보와 변경 빈도와 갱신 주체가 다르다.
> 가격은 SYSTEM에 의해 자주 갱신되고 낙관적 락 대상이 되지만, 자산의 이름, 상태, 통화 같은 마스터 정보는 ADMIN에 의해 상대적으로 드물게 변경된다.
> 최신 가격을 Asset에 포함하면 가격 갱신마다 assets row의 version이 증가하여 자산 마스터 수정과 불필요한 락 충돌이 발생할 수 있다.
> 따라서 가격 갱신의 동시성 제어 범위를 최신 가격 row로 좁히기 위해 LatestPrice를 별도 엔티티로 분리하는 것을 선택한다.

### PriceHistory

가격 이력은 특정 자산의 특정 시점 가격을 의미합니다.

| 컬럼 | 설명 |
| --- | --- |
| `id` | 가격 이력 ID |
| `asset` | 대상 자산 |
| `price` | 가격 |
| `timestamp` | 실제 시장 기준 시각 |
| `source` | 가격 데이터 출처 |
| `receivedAt` | 서버가 데이터를 수신한 시각 |
| `createdAt` | 생성 시각 |
| `updatedAt` | 수정 시각 |

`timestamp`와 `receivedAt`은 서로 다릅니다.

| 컬럼 | 의미 |
| --- | --- |
| `timestamp` | 실제 시장에서 가격이 발생한 시각 |
| `receivedAt` | Kotlin 증권 서버가 데이터를 받은 시각 |

예를 들어 외부 시세 제공자가 10:00 가격을 10:00:03에 보냈다면 다음처럼 저장될 수 있습니다.

```json
{
  "price": 72000,
  "timestamp": "2026-05-03T10:00:00",
  "receivedAt": "2026-05-03T10:00:03"
}
```

#### 제약 조건

- 가격은 0보다 커야 합니다.
- 가격 이력은 반드시 자산에 속해야 합니다.
- `price > 0` 제약을 DB에도 둡니다.
- `createdAt`, `updatedAt`은 서버에서 기록합니다.

## 에러 응답 형식

모든 에러는 아래 형식을 따릅니다.

```json
{
  "code": "ASSET_NOT_FOUND",
  "message": "Asset not found."
}
```

## Chapter 1. 첫 번째 기능: 마켓을 만든다

서비스의 출발점은 마켓입니다.

Kotlin 증권은 KOSPI, NASDAQ, CRYPTO처럼 다양한 시장을 관리하려고 합니다.
관리자는 새로운 마켓을 등록할 수 있어야 하며, 같은 이름의 마켓이 중복으로 생성되어서는 안 됩니다.
사용자와 운영 도구는 등록된 마켓을 목록으로 조회하거나, 특정 마켓을 ID 또는 이름으로 조회할 수 있어야 합니다.

### 구현할 API

`POST /markets`

`GET /markets`

`GET /markets/{marketId}`

`GET /markets/name/{name}`

### 생성 요청 예시

```json
{
  "name": "KOSPI",
  "timezone": "Asia/Seoul"
}
```

### 조회 요청 예시

목록 조회

`GET /markets`

ID 단건 조회

`GET /markets/1`

이름 단건 조회

`GET /markets/name/KOSPI`

### 요구사항

- 마켓 이름은 중복될 수 없습니다.
- 마켓 이름은 unique 하므로 이름으로 단건 조회할 수 있습니다.
- `timezone`은 필수입니다.
- 성공 시 `201 Created`를 반환합니다.
- 이미 존재하는 마켓이면 `409 Conflict`를 반환합니다.
- 목록 조회 시 `200 OK`와 마켓 목록을 반환합니다.
- ID 단건 조회 시 `200 OK`와 마켓 정보를 반환합니다.
- 이름 단건 조회 시 `200 OK`와 마켓 정보를 반환합니다.
- 존재하지 않는 마켓 ID 또는 이름이면 `404 Not Found`를 반환합니다.
- 중복 제약은 애플리케이션 레벨과 DB unique index 양쪽에서 처리합니다.

## Chapter 2. 마켓에는 자산이 속한다

고객이 실제로 추적하고 싶은 것은 자산입니다.

자산은 독립적으로 존재하지 않습니다.
반드시 하나의 마켓에 속해야 합니다.

### 구현할 API

`POST /markets/{marketId}/assets`

`GET /markets/{marketId}/assets`

`GET /markets/{marketId}/assets/{assetId}`

### 생성 요청 예시

```json
{
  "symbol": "005930",
  "name": "삼성전자",
  "currency": "KRW"
}
```

### 조회 요청 예시

자산 목록 조회

`GET /markets/1/assets`

자산 단건 조회

`GET /markets/1/assets/10`

### 요구사항

- 존재하지 않는 마켓에는 자산을 등록할 수 없습니다.
- 하나의 마켓 안에서 같은 `symbol`은 중복될 수 없습니다.
- 자산 생성 시 기본 상태는 `ACTIVE`입니다.
- `currency`는 필수입니다.
- 성공 시 `201 Created`를 반환합니다.
- 존재하지 않는 마켓이면 `404 Not Found`를 반환합니다.
- 중복된 자산이면 `409 Conflict`를 반환합니다.
- 목록 조회 시 `200 OK`와 특정 마켓에 속한 자산 목록을 반환합니다.
- 단건 조회 시 `200 OK`와 자산 정보를 반환합니다.
- 존재하지 않는 자산이거나 `marketId`와 `assetId`의 관계가 맞지 않으면 `404 Not Found`를 반환합니다.
- 자산을 생성한 뒤 조회 API를 통해 등록된 내역을 확인할 수 있어야 합니다.
- 중복 제약은 애플리케이션 레벨과 DB unique index 양쪽에서 처리합니다.

## Chapter 3. 자산에는 상태가 있다

실제 금융 서비스에서 자산은 항상 거래 가능한 상태가 아닙니다.

어떤 자산은 정상 거래 중일 수 있고, 어떤 자산은 일시적으로 비활성화될 수 있으며, 어떤 자산은 상장 폐지될 수도 있습니다.

### 상태 예시

- `ACTIVE`
- `INACTIVE`
- `DELISTED`

### 요구사항

- 가격 등록은 `ACTIVE` 상태의 자산에만 가능합니다.
- `INACTIVE`, `DELISTED` 상태의 자산에는 가격을 등록할 수 없습니다.
- 상태 변경 API를 구현하거나, 구현하지 않는 경우 설계 설명에 상태 전환 방식을 포함합니다.

### 선택 구현 API

`PATCH /markets/{marketId}/assets/{assetId}/status`

### 요청 예시

```json
{
  "status": "INACTIVE"
}
```

## Chapter 4. 자산에는 가격 이력이 쌓인다

자산이 등록되었다면 이제 가격을 기록해야 합니다.

가격은 현재 값 하나만 중요한 것이 아닙니다.
언제 얼마였는지 계속 쌓여야 나중에 추이와 통계를 분석할 수 있습니다.

### 구현할 API

`POST /markets/{marketId}/assets/{assetId}/prices`

### 요청 예시

```json
{
  "price": 72000,
  "timestamp": "2026-05-03T10:00:00",
  "source": "SYSTEM_A"
}
```

### 요구사항

- 가격은 0보다 커야 합니다.
- `timestamp`는 필수입니다.
- `source`는 필수입니다.
- `receivedAt`은 서버에서 저장 시점에 기록합니다.
- 존재하지 않는 자산에는 가격을 저장할 수 없습니다.
- `marketId`와 `assetId`의 관계가 맞지 않으면 저장할 수 없습니다.
- 자산 상태가 `ACTIVE`가 아니면 가격을 저장할 수 없습니다.
- 가격 이력 저장과 최신 가격 갱신은 하나의 트랜잭션으로 처리합니다.
- 성공 시 `201 Created`를 반환합니다.
- 자산이 없거나 소속 관계가 맞지 않으면 `404 Not Found`를 반환합니다.
- 가격이 유효하지 않으면 `400 Bad Request`를 반환합니다.
- 자산 상태가 유효하지 않으면 `400 Bad Request`를 반환합니다.

## Chapter 5. 고객은 숫자가 아니라 인사이트를 원한다

고객은 단순한 가격 목록이 아니라 분석 정보를 원합니다.

> "이번 달 삼성전자의 최고가는 얼마였나요?"
> "최저가는요?"
> "평균 가격은 어느 정도였나요?"

모든 가격 데이터를 애플리케이션 메모리로 가져와 계산하는 방식은 데이터가 많아질수록 위험합니다.
따라서 DB 레벨의 집계 쿼리가 필요합니다.

### 구현할 API

`GET /markets/{marketId}/assets/{assetId}/prices/statistics?from=2026-05-01T00:00:00&to=2026-05-03T23:59:59`

### 응답 예시

```json
{
  "assetId": 1,
  "symbol": "005930",
  "currency": "KRW",
  "minPrice": 71000,
  "maxPrice": 73500,
  "averagePrice": 72250
}
```

### 요구사항

- 특정 기간의 최고가, 최저가, 평균가를 반환해야 합니다.
- DB 레벨 집계 쿼리를 사용해야 합니다.
- 존재하지 않는 자산이면 `404 Not Found`를 반환합니다.
- `marketId`와 `assetId`의 관계가 맞지 않으면 `404 Not Found`를 반환합니다.
- `from`은 `to`보다 이전이어야 합니다.
- `from`, `to`가 없거나 파싱할 수 없으면 `400 Bad Request`를 반환합니다.
- 날짜 범위가 유효하지 않으면 `400 Bad Request`를 반환합니다.

### 데이터가 없는 경우

기간 내 가격 데이터가 없다면 `200 OK`와 함께 집계 값을 `null`로 반환합니다.

```json
{
  "assetId": 1,
  "symbol": "005930",
  "currency": "KRW",
  "minPrice": null,
  "maxPrice": null,
  "averagePrice": null
}
```

## Chapter 6. 동시에 들어오는 가격 업데이트

서비스가 외부 시세 제공자와 연결되면서 문제가 생겼습니다.

같은 자산의 가격 데이터가 여러 경로에서 거의 동시에 들어오기 시작했습니다.

가격 이력만 단순히 append 한다면 동시성 문제는 비교적 적습니다.
하지만 최신 가격을 함께 갱신한다면 문제가 발생할 수 있습니다.

예를 들어 두 요청이 동시에 같은 자산의 최신 가격을 읽고, 각자 최신 가격을 갱신하려 한다면 더 늦게 도착한 오래된 가격이 최신 가격을 덮어쓸 수도 있습니다.

### 구현 또는 설계할 내용

- 가격 이력 저장과 최신 가격 갱신을 하나의 트랜잭션으로 처리합니다.
- 최신 가격 갱신 과정에서 동시성 문제가 발생할 수 있는 지점을 설명합니다.
- 낙관적 락 또는 비관적 락을 적용합니다.
- 낙관적 락을 사용하는 경우 최신 가격 정보를 관리하는 엔티티에 `@Version` 필드를 둡니다.
- 충돌 발생 시 `409 Conflict`를 반환합니다.
- 가능하다면 동시 요청 테스트를 작성합니다.

### 낙관적 락 예시

최신 가격 정보를 관리하는 엔티티에 `version` 컬럼을 둡니다.

- `Asset.version`
- `LatestPrice.version`

충돌이 발생하면 다음 응답을 반환합니다.

```json
{
  "code": "CONCURRENCY_ERROR",
  "message": "Price update conflict occurred. Please retry."
}
```

## Chapter 7. 충돌이 너무 많아지면 락도 병목이 된다

인기 자산에는 가격 업데이트가 매우 자주 들어옵니다.

낙관적 락을 적용했더라도 충돌이 계속 발생하면 요청들이 반복적으로 실패하고 재시도하게 됩니다.
이 재시도가 과도해지면 애플리케이션은 CPU와 DB 리소스를 낭비할 수 있습니다.

이제 문제는 "락을 걸었는가"가 아니라, "락 경합을 어떻게 줄였는가"입니다.

### 구현 또는 설계할 내용

다음 중 하나 이상을 선택해 해결합니다.

- 재시도 횟수 제한
- Exponential Backoff
- 비동기 큐 기반 가격 업데이트
- 자산 단위 직렬 처리
- 분산 락
- 마지막 가격만 반영하는 Coalescing 전략

### 권장 정책 예시

- 낙관적 락 충돌 시 최대 3회까지만 재시도합니다.
- 재시도 간격은 Exponential Backoff를 적용합니다.
- 예: 100ms, 200ms, 400ms
- 충돌이 계속 발생하면 `409 CONCURRENCY_ERROR`를 반환합니다.
- 고빈도 자산은 큐 기반으로 자산 단위 직렬 처리를 고려합니다.
- 같은 자산에 짧은 시간 동안 여러 가격이 들어오면 마지막 가격만 최신 가격에 반영하는 Coalescing 전략을 고려합니다.

## Chapter 8. 금융 데이터는 아무나 만질 수 없다

자산 가격 데이터는 서비스의 핵심 데이터입니다.

모든 사용자가 마켓을 만들거나 가격을 등록할 수 있어서는 안 됩니다.
일반 사용자는 조회만 가능해야 하고, 관리자나 시스템 권한을 가진 사용자만 데이터를 등록할 수 있어야 합니다.

### 구현할 내용

- Spring Security 적용
- 인증 처리
- Role 기반 권한 처리
- 엔드포인트별 접근 권한 분리

### 권한 예시

| Role | 가능 작업 |
| --- | --- |
| `USER` | 마켓 조회, 자산 조회, 가격 이력 조회, 통계 조회 |
| `ADMIN` | 마켓 생성, 자산 등록, 자산 상태 변경 |
| `SYSTEM` | 가격 등록 |

## 조회 API 확장

각 Chapter에서 생성한 리소스를 확인할 수 있는 기본 조회 API를 먼저 제공합니다.
이후 서비스가 커지면서 목록 조회 조건, 가격 이력 기간 조건, 통계 조회처럼 추가로 필요한 조회 기능을 확장합니다.

### 마켓 목록 조회

`GET /markets`

### 특정 마켓의 자산 목록 조회

`GET /markets/{marketId}/assets`

### 특정 마켓의 자산 단건 조회

`GET /markets/{marketId}/assets/{assetId}`

### 특정 자산의 가격 이력 조회

`GET /markets/{marketId}/assets/{assetId}/prices?from=2026-05-01T00:00:00&to=2026-05-03T23:59:59`

### 요구사항

- 조회 API는 `USER`, `ADMIN`, `SYSTEM` 모두 접근할 수 있습니다.
- 존재하지 않는 마켓이면 `404 Not Found`를 반환합니다.
- 존재하지 않는 자산이거나 마켓과 자산의 관계가 맞지 않으면 `404 Not Found`를 반환합니다.
- 가격 이력 조회는 `from`, `to` 기간 조건에 해당하는 이력을 시간순으로 반환합니다.
- 자산 목록 조회는 이후 필요에 따라 `status`, `symbol`, `currency` 같은 검색 조건을 추가할 수 있습니다.
- 가격 이력 조회는 `from`, `to` 기간 조건을 받을 수 있습니다.
- `from`, `to`가 모두 주어졌다면 `from`은 `to`보다 이전이어야 합니다.
- 날짜 조건이 유효하지 않으면 `400 Bad Request`를 반환합니다.

## Bonus Chapter 1. 공통 응답 포맷을 맞춘다

기능이 늘어나면 응답 구조를 일관되게 관리해야 합니다.

프론트엔드와 외부 연동 클라이언트가 매번 다른 응답 형태를 분기하지 않도록, 바디가 있는 API 응답은 공통 envelope인 `ApiResponse<T>`로 감싸서 반환합니다.

다만 HTTP status가 이미 성공/실패를 표현하므로 `success` 필드는 두지 않습니다.

### 성공 응답 형식

실제 payload는 `data` 필드에 담습니다.

```json
{
  "code": "SUCCESS",
  "message": "Request processed successfully.",
  "data": {
    "id": 1,
    "name": "KOSPI",
    "timezone": "Asia/Seoul"
  }
}
```

목록 조회도 동일하게 `data`에 배열을 담습니다.

```json
{
  "code": "SUCCESS",
  "message": "Request processed successfully.",
  "data": [
    {
      "id": 1,
      "name": "KOSPI",
      "timezone": "Asia/Seoul"
    }
  ]
}
```

생성 API처럼 별도 payload가 없는 성공 응답은 `data`를 `null`로 둡니다.

```json
{
  "code": "SUCCESS",
  "message": "Market created successfully.",
  "data": null
}
```

단, `204 No Content` 응답은 HTTP 의미상 바디를 반환하지 않습니다.

### 실패 응답 형식

실패 응답도 같은 envelope를 사용합니다.

```json
{
  "code": "ASSET_NOT_FOUND",
  "message": "Asset not found.",
  "data": null
}
```

### 요구사항

- 바디가 있는 성공 응답은 `ApiResponse<T>`를 사용합니다.
- 바디가 있는 실패 응답은 `ApiResponse<Nothing>`을 사용합니다.
- 실제 payload는 항상 `data` 필드에 담습니다.
- `success` 필드는 두지 않습니다.
- `code`는 클라이언트가 분기할 수 있는 안정적인 코드입니다.
- `message`는 사람이 읽을 수 있는 설명입니다.
- `204 No Content` 응답은 바디를 반환하지 않습니다.

## 에러 코드

| HTTP Status | Code |
| --- | --- |
| 400 | `INVALID_PRICE` |
| 400 | `INVALID_DATE_RANGE` |
| 400 | `INVALID_ASSET_STATUS` |
| 400 | `INVALID_REQUEST` |
| 401 | `UNAUTHORIZED` |
| 403 | `ACCESS_DENIED` |
| 404 | `MARKET_NOT_FOUND` |
| 404 | `ASSET_NOT_FOUND` |
| 409 | `MARKET_ALREADY_EXISTS` |
| 409 | `ASSET_ALREADY_EXISTS` |
| 409 | `CONCURRENCY_ERROR` |
