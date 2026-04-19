# TASK-AUTH-COOKIE — httpOnly Cookie 발급 및 CORS 로컬 설정

## 목표
- AuthController: 로그인/refresh 응답에 httpOnly cookie 추가, 로그아웃 시 쿠키 삭제
- SecurityConfig: CorsConfigurationSource 빈 추가 (localhost:3000 허용)

## 변경 파일
1. `src/main/kotlin/com/demo/tourwave/adapter/in/web/auth/AuthController.kt`
2. `src/main/kotlin/com/demo/tourwave/bootstrap/SecurityConfig.kt`

## 구현 계획

### AuthController.kt
- `login` 엔드포인트: 응답에 `Set-Cookie: access_token` (httpOnly, path=/), `Set-Cookie: refresh_token` (httpOnly, path=/auth/refresh) 추가
- `refresh` 엔드포인트: 새 토큰으로 동일하게 쿠키 재발급
- `logout` 엔드포인트: maxAge(0)으로 두 쿠키 모두 삭제
- 기존 JSON body(accessToken, refreshToken) 유지 (하위 호환)

### SecurityConfig.kt
- `corsConfigurationSource()` 빈 추가: allowedOrigins=["http://localhost:3000"], allowCredentials=true
- 기존 `.cors(Customizer.withDefaults())`가 자동으로 이 빈을 사용함

## 쿠키 사양
| 이름 | path | maxAge | httpOnly | secure | sameSite |
|------|------|--------|----------|--------|----------|
| access_token | / | 3600s | true | false | Strict |
| refresh_token | /auth/refresh | 14d | true | false | Strict |

## 완료 기준
- `./gradlew test` 통과
- AuthControllerIntegrationTest 기존 테스트 통과 (JSON 바디 호환)
- Set-Cookie 헤더가 login/refresh 응답에 포함
- logout 응답에 maxAge=0 Set-Cookie 헤더 포함
