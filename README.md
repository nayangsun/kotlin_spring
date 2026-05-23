# Kotlin Spring

## Getting Started

### Development

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### Security

API는 세션 쿠키 인증과 CSRF 보호를 사용합니다.
세션 쿠키는 `HttpOnly`, `SameSite=Lax`로 설정합니다.
운영 환경에서는 HTTPS 뒤에서 `Secure` 속성을 함께 사용하는 것을 전제로 합니다.

개발용 계정:

개발용 기본 계정은 `dev` 프로필에서 Flyway seed로 생성됩니다.

| Username | Password | Role |
| --- | --- | --- |
| `user@example.com` | `Password1!` | `USER` |
| `admin@example.com` | `Password1!` | `ADMIN` |
| `system@example.com` | `Password1!` | `SYSTEM` |

권한 정책:

- `USER`, `ADMIN`, `SYSTEM`: 마켓 조회, 자산 조회, 가격 통계 조회
- `ADMIN`: 마켓 생성, 자산 등록, 자산 상태 변경
- `SYSTEM`: 가격 등록

Swagger UI에서는 `JSESSIONID` 세션 쿠키와 `XSRF-TOKEN` 쿠키를 기준으로 `X-XSRF-TOKEN` 헤더를 자동 전송합니다.
`Authorize`에는 CSRF 토큰을 수동으로 넣지 않고, 로그인 또는 로그아웃 후에는 `/auth/csrf`를 다시 호출해 새 토큰을 발급받습니다.

예시:

```bash
curl -c cookies.txt http://localhost:8080/auth/csrf
curl -b cookies.txt -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: <csrf-token>' \
  -d '{"username":"new-user@example.com","password":"Password1!"}' \
  http://localhost:8080/auth/register
curl -b cookies.txt -c cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: <csrf-token>' \
  -d '{"username":"admin@example.com","password":"Password1!"}' \
  http://localhost:8080/auth/login
curl -b cookies.txt -c cookies.txt http://localhost:8080/auth/csrf
curl -b cookies.txt http://localhost:8080/markets
curl -b cookies.txt -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: <new-csrf-token>' \
  -d '{"name":"KOSPI","timezone":"Asia/Seoul"}' \
  http://localhost:8080/markets
```

### Database Scripts

개발용 데이터베이스 유틸은 `scripts/` 아래에서 관리합니다.

```bash
./scripts/create-db.sh
./scripts/drop-db.sh
./scripts/reset-db.sh
./scripts/migrate-db.sh
./scripts/psql.sh
./scripts/dev-reset.sh
```

기본 흐름:

- `./scripts/reset-db.sh`: 개발 DB를 drop/create
- `./scripts/migrate-db.sh`: 웹 서버 없이 Flyway migration만 실행
- `./gradlew bootRun`: 서버 시작 시 Flyway migration 적용 후 기동
- `./scripts/dev-reset.sh`: DB 초기화 후 바로 서버 실행

### CI

```bash
./gradlew --no-daemon check
```

#### Test

```bash
./gradlew test
```

#### Static Analysis

```bash
./gradlew detekt
```

자주 쓰는 추가 명령:

```bash
./gradlew detektMain
./gradlew detektTest
./gradlew detekt --auto-correct
```

설정 파일 위치:

```text
config/detekt/detekt.yml
```

리포트 출력 위치:

```text
build/reports/detekt/detekt.html
build/reports/detekt/detekt.md
build/reports/detekt/detekt.xml
build/reports/detekt/detekt.sarif
```

### User Story

도메인 요구사항과 기능 맥락은 [docs/user_story.md](docs/user_story.md)에 정리되어 있습니다.

### Architecture

현재 패키지 구조와 아키텍처 결정 근거는 [docs/architecture.md](docs/architecture.md)에 정리되어 있습니다.

## Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.6/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.6/gradle-plugin/packaging-oci-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.6/reference/web/servlet.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.6/reference/data/sql.html#data.sql.jpa-and-spring-data)

## Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

## Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
