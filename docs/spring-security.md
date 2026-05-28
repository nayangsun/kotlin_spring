# Spring Security

API는 세션 쿠키 인증과 CSRF 보호를 사용합니다.
세션 쿠키는 `HttpOnly`, `SameSite=Lax`로 설정합니다.
운영 환경에서는 HTTPS 뒤에서 `Secure` 속성을 함께 사용하는 것을 전제로 합니다.

## Swagger UI

Swagger UI에서는 `JSESSIONID` 세션 쿠키와 `XSRF-TOKEN` 쿠키를 기준으로 `X-XSRF-TOKEN` 헤더를 자동 전송합니다.
`Authorize`에는 CSRF 토큰을 수동으로 넣지 않고, 로그인 또는 로그아웃 후에는 `/auth/csrf`를 다시 호출해 새 토큰을 발급받습니다.

## 예시

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
