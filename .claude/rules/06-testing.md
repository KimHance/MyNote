# 테스트

## 위치 / 스택
| 종류 | 위치 | 스택 |
|---|---|---|
| 단위(reconcile·DTO·도메인·ViewModel) | `src/test/` | JUnit4 (+ `kotlinx-coroutines-test` `runTest`) |
| 계측(화면 UI) | `src/androidTest/` | Compose UI test (추후) |

## 규칙
- **`kotlin("test")` 를 쓰지 않는다** — AGP 9 빌트인 Kotlin 에서 버전 해석이 안 됨. 각 모듈에서 `testImplementation(libs.junit)` 로 JUnit4 를 직접 선언하고 `org.junit.Test` / `org.junit.Assert.*` 를 import 한다.
- 코루틴 테스트는 `testImplementation(libs.kotlinx.coroutines.test)` 의 `runTest`. fake DataSource 로 reconcile 검증(예: `NotesReconcileTest`).
- 테스트명 한국어 backtick 권장: `` `해시가 같으면 본문을 다시 받지 않는다`() ``.
- 현재 커버리지: `:core:data` 의 `NotesReconcileTest`(add/update/skip/delete) + `NoteDtoSerializationTest`(polymorphic). ViewModel(`onIntent`/토큰 흐름) 단위 테스트와 화면 UI 테스트는 다음 단계.

## 실행
```bash
./gradlew :core:data:testDebugUnitTest
```
