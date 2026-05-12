# Architecture

## Overview

이 프로젝트는 Kotlin Spring 진영에서 최근 실무적으로 많이 사용하는 `feature-first + thin layers` 구조를 기준으로 정리한다.

현재 기준의 핵심 원칙은 다음과 같다.

- 루트 패키지는 운영 도메인을 기준으로 `com.kotlinspring`을 사용한다.
- 기능은 최상위 하위 패키지로 분리한다. 현재는 `market`이 첫 번째 기능 모듈이다.
- 기능 아래에서는 필요한 만큼만 얇게 레이어를 둔다.
- 전역 `controller/service/repository` 구조보다 기능 중심 구성을 우선한다.

## Current Package Structure

```text
src/main/kotlin/com/kotlinspring
├─ KotlinSpringApplication.kt
├─ api
│  └─ HealthController.kt
└─ market
   ├─ api
   │  ├─ CreateMarketRequest.kt
   │  ├─ MarketController.kt
   │  └─ MarketExceptionHandler.kt
   ├─ application
   │  ├─ CreateMarketCommand.kt
   │  ├─ CreateMarketService.kt
   │  └─ CreateMarketUseCase.kt
   ├─ domain
   │  ├─ Market.kt
   │  ├─ MarketAlreadyExistsException.kt
   │  └─ MarketRepository.kt
   └─ persistence
      ├─ MarketJpaEntity.kt
      ├─ MarketJpaRepository.kt
      └─ MarketPersistenceRepository.kt
```

테스트도 같은 방향을 따른다.

```text
src/test/kotlin/com/kotlinspring
├─ KotlinSpringApplicationTests.kt
├─ config
│  └─ TestEmbeddedPostgresConfig.kt
└─ market
   ├─ api
   │  └─ MarketApiTest.kt
   └─ application
      └─ CreateMarketPersistenceTest.kt
```

## Package Rules

### Root Package

- 루트 패키지는 `com.kotlinspring`이다.

### Feature First

- 최상위에서 `market`처럼 기능 단위로 먼저 패키지를 나눈다.

### Thin Layers Inside Each Feature

- `api`: Controller, request/response DTO, exception handler처럼 외부 HTTP 진입점에 가까운 코드
- `application`: 유스케이스 조합, command/result, 서비스 오케스트레이션
- `domain`: 도메인 규칙, 예외, 핵심 모델, repository contract
- `persistence`: Repository 구현체, JPA Entity, Spring Data 연동처럼 `src/main/kotlin` 아래에 두는 영속화 코드 패키지

## Why This Architecture

### Why not global controller/service/repository

전역 `controller`, `service`, `repository` 구조는 초기에는 단순하지만 기능이 늘수록 서로 관련된 코드가 여러 디렉터리로 흩어진다.

예를 들어 마켓 기능 하나를 수정할 때도 controller, service, repository, dto, exception 파일을 여러 전역 패키지에서 찾아야 한다. 기능 중심 구조는 이 탐색 비용을 줄인다.

## Sources And Rationale

이 구조는 아래 기준을 조합해 정한 것이다.

### 1. Spring Boot Reference: Structuring Your Code

참고: Spring Boot Reference, `Structuring Your Code`

- Spring Boot는 메인 애플리케이션 클래스를 루트 패키지에 두는 것을 권장한다.
- reverse-domain 기반 패키지명을 권장한다.
- 문서 예시도 `customer`, `order` 같은 기능 패키지를 루트 아래에 두는 형태를 보여준다.

이 프로젝트에서 `com.kotlinspring`를 루트로 두고, 그 아래 `market` 기능을 두는 이유가 여기서 나온다.

### 2. Spring Modulith Reference: Fundamentals

참고: Spring Modulith Reference, `Fundamentals`

- Spring Modulith는 Spring Boot 애플리케이션에서 메인 패키지의 직접 하위 패키지를 애플리케이션 모듈로 보는 접근을 제시한다.
- 각 모듈은 API와 내부 구현을 구분할 수 있어야 한다는 관점을 제공한다.
- 이는 기능 중심 패키징과 모듈 경계 의식을 강화한다.

이 프로젝트는 Spring Modulith 자체를 도입한 것은 아니지만, `market`를 하나의 기능 모듈처럼 두고 그 아래를 `api`, `application`, `domain`으로 나누는 판단에 이 관점을 참고했다.

### 3. Kotlin Coding Conventions

참고: Kotlin Coding Conventions, package and naming rules

- Kotlin 패키지명은 일반적으로 소문자를 사용한다.
- `_`가 들어간 `kotlin_spring`보다는 `kotlinspring`이 Kotlin 패키지 컨벤션에 더 가깝다.

그래서 이 프로젝트는 `com.example.kotlin_spring` 대신 `com.kotlinspring` 방향으로 정리했다.

## Decision Summary

이 프로젝트의 아키텍처 결정은 아래 한 줄로 요약할 수 있다.

> 운영 도메인 기반 루트 패키지 아래에 기능을 먼저 나누고, 각 기능 내부는 API, application, domain 정도로 가볍게 분리한다.

현재 프로젝트에서는 그 결정이 다음 구조로 나타난다.

- `com.kotlinspring.api`
- `com.kotlinspring.market.api`
- `com.kotlinspring.market.application`
- `com.kotlinspring.market.domain`
- `com.kotlinspring.market.persistence`
