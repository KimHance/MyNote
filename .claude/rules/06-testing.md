# 테스트

## 위치 / 스택
| 종류 | 위치 | 스택 |
|---|---|---|
| 단위(파서·도메인·ViewModel) | `src/test/` | JUnit4 |
| 계측(화면 UI) | `src/androidTest/` | Compose UI test (추후) |

## 규칙
- **`kotlin("test")` 를 쓰지 않는다** — AGP 9 빌트인 Kotlin 에서 버전 해석이 안 됨. 각 모듈에서 `testImplementation(libs.junit)` 로 JUnit4 를 직접 선언하고 `org.junit.Test` / `org.junit.Assert.*` 를 import 한다.
- 테스트명 한국어 backtick 권장: `` `H2 와 H3 헤딩을 구분해 파싱한다`() ``.
- 현재 커버리지: `:core:design-foundation` 의 `MarkdownParserTest`. ViewModel(`onIntent` 상태 전이) 단위 테스트와 화면 UI 테스트는 다음 단계.

## 실행
```bash
./gradlew :core:design-foundation:testDebugUnitTest
```
