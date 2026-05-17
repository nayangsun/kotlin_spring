# New Architecture

## Overview

이 문서는 신규 Spring Boot 백엔드에서 사용할 수 있는 `domain module first + lightweight hexagonal layers` 구조를 설명한다.

이 구조의 목표는 다음과 같다.

- 하나의 Spring Boot 애플리케이션 안에서 도메인별 경계를 명확히 둔다.
- 전역 `controller/service/repository` 패키지보다 기능 또는 도메인 중심 패키지를 우선한다.
- 각 도메인 모듈 내부에서는 `api`, `application`, `domain`, `infrastructure` 계층을 사용한다.
- 도메인 규칙은 프레임워크와 영속화 기술에 최대한 덜 의존하도록 둔다.
- 모듈 간 직접 결합을 줄이고, 공개 API 또는 이벤트를 통해 통신한다.

이 방식은 모듈러 모놀리스와 헥사고날 아키텍처의 실용적인 조합이다.
프로젝트 전체는 하나의 배포 단위로 유지하지만, 내부 코드는 나중에 독립 서비스로 분리할 수 있도록 도메인 경계를 의식해 구성한다.

## Package Structure

기본 구조는 다음과 같다.

```text
src/main/kotlin/com/example/app
├─ AppApplication.kt
├─ common
├─ member
│  ├─ api
│  ├─ application
│  ├─ domain
│  └─ infrastructure
├─ order
│  ├─ api
│  ├─ application
│  ├─ domain
│  └─ infrastructure
└─ payment
   ├─ api
   ├─ application
   ├─ domain
   └─ infrastructure
```

각 최상위 도메인 패키지는 하나의 애플리케이션 모듈로 본다.
예를 들어 `member`, `order`, `payment`는 서로 다른 기능 영역이며, 각 모듈은 자기 도메인의 API, 유스케이스, 규칙, 기술 구현을 내부에 가진다.

## Module Layout

`order` 모듈을 예로 들면 다음과 같이 구성할 수 있다.

```text
order
├─ api
│  ├─ OrderController.kt
│  ├─ CreateOrderRequest.kt
│  ├─ OrderResponse.kt
│  └─ OrderExceptionHandler.kt
├─ application
│  ├─ CreateOrderCommand.kt
│  ├─ CreateOrderResult.kt
│  ├─ OrderService.kt
│  ├─ OrderUseCase.kt
│  └─ PaymentPort.kt
├─ domain
│  ├─ Order.kt
│  ├─ OrderId.kt
│  ├─ OrderLine.kt
│  ├─ OrderStatus.kt
│  ├─ OrderRepository.kt
│  └─ OrderCreatedEvent.kt
└─ infrastructure
   ├─ OrderJpaEntity.kt
   ├─ OrderJpaRepository.kt
   ├─ OrderRepositoryAdapter.kt
   └─ PaymentClientAdapter.kt
```

파일 수가 적은 초기 단계에서는 모든 하위 패키지를 강제로 만들 필요는 없다.
다만 모듈이 커질 때 위 구조로 자연스럽게 확장할 수 있어야 한다.

## Layer Responsibilities

### api

`api`는 외부 요청이 애플리케이션으로 들어오는 진입점이다.

주로 다음 코드를 둔다.

- Spring MVC Controller
- request DTO
- response DTO
- API 전용 validation
- API 전용 exception handler
- 인증 사용자 변환 또는 HTTP 파라미터 변환

`api` 계층은 HTTP 요청과 응답을 애플리케이션 유스케이스에 맞는 형태로 변환한다.
비즈니스 규칙을 직접 처리하지 않는다.

예시:

```kotlin
@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderUseCase: OrderUseCase,
) {
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): OrderResponse {
        val result = orderUseCase.createOrder(request.toCommand())
        return OrderResponse.from(result)
    }
}
```

### application

`application`은 유스케이스를 실행하는 계층이다.

주로 다음 코드를 둔다.

- use case interface
- application service
- command
- query
- result
- port interface
- 트랜잭션 경계
- 모듈 간 협력 흐름

`application` 계층은 도메인 객체를 사용해 하나의 업무 흐름을 완성한다.
도메인 규칙 자체를 이 계층에 과도하게 두지 않고, 핵심 규칙은 `domain`으로 위임한다.

예시:

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val paymentPort: PaymentPort,
    private val eventPublisher: OrderEventPublisher,
) : OrderUseCase {

    @Transactional
    override fun createOrder(command: CreateOrderCommand): CreateOrderResult {
        val order = Order.create(
            memberId = command.memberId,
            lines = command.lines,
        )

        orderRepository.save(order)
        eventPublisher.publish(OrderCreatedEvent(order.id))

        return CreateOrderResult.from(order)
    }
}
```

### domain

`domain`은 비즈니스 규칙과 핵심 모델을 표현하는 계층이다.

주로 다음 코드를 둔다.

- entity
- value object
- domain service
- domain event
- repository contract
- domain exception
- enum

`domain`은 가능하면 Spring, HTTP, JPA, Kafka 같은 기술 세부사항을 모르게 둔다.
도메인 객체는 자기 규칙을 직접 보호해야 한다.

예시:

```kotlin
class Order(
    val id: OrderId,
    val memberId: MemberId,
    private val lines: MutableList<OrderLine>,
    var status: OrderStatus,
) {
    fun totalPrice(): Money {
        return lines.fold(Money.ZERO) { acc, line -> acc + line.totalPrice() }
    }

    fun cancel() {
        check(status != OrderStatus.PAID) { "Paid order cannot be cancelled" }
        status = OrderStatus.CANCELLED
    }

    companion object {
        fun create(memberId: MemberId, lines: List<OrderLine>): Order {
            require(lines.isNotEmpty()) { "Order must have at least one line" }

            return Order(
                id = OrderId.newId(),
                memberId = memberId,
                lines = lines.toMutableList(),
                status = OrderStatus.CREATED,
            )
        }
    }
}
```

### infrastructure

`infrastructure`는 외부 기술과 연결되는 구현 계층이다.

주로 다음 코드를 둔다.

- JPA entity
- Spring Data repository
- QueryDSL 또는 jOOQ 구현
- Redis adapter
- Kafka 또는 RabbitMQ adapter
- 외부 API client
- 파일 저장소 adapter
- 메일, 알림, 결제 PG 연동 구현

`infrastructure`는 `domain` 또는 `application`에서 정의한 contract를 구현한다.
기술 세부사항이 바뀌어도 `domain`과 `application` 변경을 최소화하는 것이 목적이다.

예시:

```kotlin
@Repository
class OrderRepositoryAdapter(
    private val jpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Order {
        val entity = OrderJpaEntity.from(order)
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: OrderId): Order? {
        return jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }
}
```

## Dependency Direction

기본 의존 방향은 다음과 같다.

```text
api -> application -> domain
infrastructure -> application/domain contracts
```

중요한 규칙은 다음과 같다.

- `api`는 `application`을 호출한다.
- `application`은 `domain`을 사용한다.
- `domain`은 다른 계층을 모른다.
- `infrastructure`는 `application` 또는 `domain`에 선언된 interface를 구현한다.
- `domain`에서 `api` 또는 `infrastructure`를 참조하지 않는다.

이를 그림으로 표현하면 다음과 같다.

```text
api ───────────────▶ application ───────────────▶ domain
                         ▲                         ▲
                         │                         │
                  infrastructure ──────────────────┘
```

## Module Communication

도메인 모듈 간 통신은 공개된 contract를 통해 수행한다.
다른 모듈의 내부 구현체를 직접 주입하지 않는다.

### Direct Public API

동기 호출이 필요하면 대상 모듈은 공개 API를 제공한다.

```text
payment
├─ PaymentApi.kt
└─ internal
   └─ PaymentService.kt
```

예시:

```kotlin
interface PaymentApi {
    fun requestPayment(command: PaymentRequestCommand): PaymentResult
}
```

`order` 모듈은 `PaymentService`가 아니라 `PaymentApi`에만 의존한다.

```kotlin
@Service
class OrderService(
    private val paymentApi: PaymentApi,
) {
    fun payOrder(orderId: OrderId) {
        paymentApi.requestPayment(PaymentRequestCommand(orderId))
    }
}
```

### Event-Based Communication

모듈 간 결합을 더 낮추려면 이벤트를 사용한다.

```kotlin
data class OrderCreatedEvent(
    val orderId: OrderId,
    val memberId: MemberId,
    val amount: Money,
)
```

발행 측:

```kotlin
eventPublisher.publish(OrderCreatedEvent(order.id, order.memberId, order.totalPrice()))
```

구독 측:

```kotlin
@Component
class OrderCreatedEventHandler {

    @EventListener
    fun handle(event: OrderCreatedEvent) {
        // prepare payment
    }
}
```

동일 애플리케이션 안에서는 Spring application event 또는 Spring Modulith event를 사용할 수 있다.
나중에 독립 서비스로 분리해야 하는 경우 Kafka, RabbitMQ, Spring Cloud Stream 같은 외부 메시징으로 확장할 수 있다.

## Persistence Strategy

영속화 구조는 프로젝트 복잡도에 따라 선택한다.

### Simple Domain

단순 CRUD 중심이면 도메인 객체에 JPA annotation을 직접 사용할 수 있다.

```text
order
├─ domain
│  └─ Order.kt
└─ infrastructure
   └─ OrderJpaRepository.kt
```

이 방식은 빠르게 개발할 수 있지만, 도메인 모델이 JPA 제약에 영향을 받는다.

### Rich Domain

도메인 규칙이 복잡하거나 장기 유지보수가 중요하면 domain model과 JPA entity를 분리한다.

```text
order
├─ domain
│  └─ Order.kt
└─ infrastructure
   ├─ OrderJpaEntity.kt
   ├─ OrderJpaRepository.kt
   └─ OrderRepositoryAdapter.kt
```

이 방식은 매핑 코드가 늘어나지만, 도메인 규칙과 DB 구현을 분리할 수 있다.

선택 기준은 다음과 같다.

| 상황 | 권장 방식 |
| --- | --- |
| 단순 CRUD | domain model과 JPA entity 통합 |
| 복잡한 비즈니스 규칙 | domain model과 JPA entity 분리 |
| DB 스키마 변경 가능성이 큼 | 분리 |
| 개발 속도가 더 중요함 | 통합 |
| 테스트 가능한 순수 도메인이 중요함 | 분리 |

## Common Package Rules

`common`은 최소한으로 사용한다.

넣을 수 있는 항목은 다음과 같다.

- 공통 예외 응답
- 공통 error code
- 인증 사용자 표현
- 공통 web response
- 공통 pagination model
- 날짜와 시간 처리 helper
- base entity

주의할 항목은 다음과 같다.

- 도메인 service
- 도메인 repository
- 도메인 DTO
- 특정 업무의 enum
- 특정 업무의 value object
- 무분별한 util class

`common`에 도메인 개념이 들어가기 시작하면 모듈 경계가 약해진다.
특정 업무에 속하는 개념은 가능한 한 해당 도메인 모듈 안에 둔다.

## Testing Strategy

테스트도 운영 코드와 같은 도메인 구조를 따른다.

```text
src/test/kotlin/com/example/app
├─ member
│  ├─ application
│  └─ domain
├─ order
│  ├─ api
│  ├─ application
│  └─ domain
└─ payment
   ├─ application
   └─ domain
```

계층별 테스트 기준은 다음과 같다.

| 계층 | 테스트 방식 |
| --- | --- |
| `domain` | 순수 단위 테스트 |
| `application` | repository와 port를 fake 또는 mock으로 대체한 유스케이스 테스트 |
| `infrastructure` | `@DataJpaTest`, Testcontainers, 외부 client contract 테스트 |
| `api` | `@WebMvcTest`, MockMvc, REST Docs 또는 OpenAPI 검증 |
| module | Spring Modulith `@ApplicationModuleTest` |

도메인 테스트는 Spring context 없이 빠르게 실행되는 것을 우선한다.
통합 테스트는 실제 DB, transaction, serialization, security filter처럼 런타임에서 문제가 생길 수 있는 지점을 검증한다.

## Growth Path

초기 프로젝트에서는 다음처럼 단순하게 시작할 수 있다.

```text
order
├─ OrderController.kt
├─ OrderService.kt
├─ Order.kt
└─ OrderRepository.kt
```

기능이 커지면 다음 구조로 확장한다.

```text
order
├─ api
├─ application
├─ domain
└─ infrastructure
```

모듈 간 의존성이 복잡해지면 다음을 도입한다.

- 공개 API interface
- domain event
- Spring Modulith
- ArchUnit 의존성 검증
- module-level integration test

특정 모듈에 독립 배포, 독립 확장, 장애 격리 요구가 생기면 해당 모듈을 마이크로서비스 후보로 본다.
이 구조의 목적은 처음부터 분산 시스템을 만드는 것이 아니라, 필요한 시점에 분리할 수 있는 경계를 미리 마련하는 것이다.

## Rules Summary

이 구조의 핵심 규칙은 다음과 같다.

- 기능 또는 도메인을 최상위 패키지로 둔다.
- 전역 `controller`, `service`, `repository` 패키지를 기본 구조로 사용하지 않는다.
- Controller는 얇게 유지한다.
- application service는 유스케이스 흐름을 조율한다.
- domain은 핵심 비즈니스 규칙을 가진다.
- infrastructure는 기술 세부사항을 구현한다.
- 다른 모듈의 내부 구현을 직접 참조하지 않는다.
- 모듈 간 통신은 공개 API 또는 이벤트를 사용한다.
- `common`은 최소화한다.
- 테스트 구조는 운영 코드의 도메인 구조를 따른다.

## Decision Summary

신규 Spring Boot 백엔드의 기본 구조는 다음 문장으로 요약한다.

> 하나의 Spring Boot 애플리케이션 안에서 도메인별 모듈을 먼저 나누고, 각 모듈 내부는 API, application, domain, infrastructure 계층으로 가볍게 분리한다.

이 구조는 작은 프로젝트에서는 단순한 모놀리스처럼 개발할 수 있고, 도메인이 커지면 모듈러 모놀리스로 유지할 수 있으며, 필요가 증명된 일부 모듈은 나중에 독립 서비스로 분리할 수 있다.
