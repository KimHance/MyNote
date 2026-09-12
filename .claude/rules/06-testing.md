# 테스트

## 위치 / 스택
| 종류 | 위치 | 스택 |
|---|---|---|
| 단위(reconcile·DTO·도메인·MVI base) | `src/test/` | JUnit4 (+ `kotlinx-coroutines-test` `runTest`) |
| 동시성(실제 스레드) | `src/test/` | JUnit4 + `runBlocking` + `CyclicBarrier` |
| 계측(화면 UI) | `src/androidTest/` | Compose UI test (추후) |

## 규칙
- **`kotlin("test")` 를 쓰지 않는다** — AGP 9 빌트인 Kotlin 에서 버전 해석이 안 됨. 각 모듈에서 `testImplementation(libs.junit)` 로 JUnit4 를 직접 선언하고 `org.junit.Test` / `org.junit.Assert.*` 를 import 한다.
- 코루틴 테스트는 `testImplementation(libs.kotlinx.coroutines.test)` 의 `runTest`. fake DataSource 로 reconcile 검증(예: `NotesReconcileTest`).
- 테스트명 한국어 backtick 권장: `` `해시가 같으면 본문을 다시 받지 않는다`() ``.
- **테스트를 나중에 쓴 경우(tests-after) 통과만으로 믿지 않는다.** 구현을 일부러 망가뜨려(변이) 해당 테스트가 실제로 빨간불이 되는지 확인한다. 예: `SharingStarted` 변경, 등록 누락.

### 함정 — `advanceUntilIdle()` 은 `backgroundScope` 를 깨우지 않는다
가상시간 테스트에서 Flow 수집기를 `backgroundScope.launch { }` 로만 띄우면 **수집기가 붙지 않은 채 테스트가 통과**한다
(끝나지 않는 백그라운드 작업 때문에 무한루프에 빠지지 않도록 의도적으로 배제된다). 반드시 이렇게 띄운다:

```kotlin
backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { … } }
```

## 현재 커버리지 (총 70)
| 모듈 | 테스트 | 건수 |
|---|---|---|
| `:core:common` | `MviViewModelTest`(초기값·재조립·일관성·수명) · `RunCatchingCancellableTest` | 23 |
| `:core:common` | `MviViewModelConcurrencyTest` + `MviViewModelMainImmediateConcurrencyTest`(실제 스레드) | 4 |
| `:core:data` | `NotesReconcileTest`(add/update/skip/delete) · `NoteDtoSerializationTest`(polymorphic) | 11 |
| `:core:design-foundation` | `AdaptiveTokensTest`(너비×높이 토큰, 레일/바 판정, 히어로 생략) | 9 |
| `:core:navigation` | `NavigatorTest`(쓰로틀 게이트·no-op·창 경계·Throttled↔Ignored) · `ListDetailSceneStrategyTest` · `NotesNavKeyTest` | 17 |
| `:core:design-system` | `FloatingNavScrollStateTest`(떨림 무시·안전지대·복귀·리셋) | 6 |

`NotesViewModel` 단위 테스트와 화면 UI 테스트는 다음 단계.

## 실행
```bash
./gradlew testDebugUnitTest          # 전 모듈
./gradlew :core:common:testDebugUnitTest
```

> 테스트가 없는 모듈은 Gradle 9 부터 test 태스크가 실패하므로 convention 에서
> `failOnNoDiscoveredTests.set(false)` 로 해제해 뒀다.
