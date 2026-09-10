# MVI ViewModel (`:core:common` / `mvi`)

전체 재조립형 MVI base. **상태 일부를 고치지 않는다** — 등록된 값이 하나라도 바뀌면
`build()` 로 상태를 통째로 다시 만든다. 그래서 상태를 만드는 자리가 `build()` 한 곳뿐이고,
"지금 화면이 왜 이 값인가"는 함수 하나를 읽는 문제가 된다.

## 구성

| 파일 | 내용 |
|---|---|
| `MviViewModel.kt` | base — `trigger` / `triggerIn` / `build()` / `onIntent` / `state` |
| `MviElement.kt` | 마커 인터페이스 `MviState`, `MviEvent` |

```kotlin
abstract class MviViewModel<S : MviState, E : MviEvent> : ViewModel() {
    protected fun <T> trigger(initial: T): MutableStateFlow<T>
    protected fun <T> Flow<T>.triggerIn(
        initial: T,
        started: SharingStarted = SharingStarted.WhileSubscribed(0),
    ): StateFlow<T>

    protected abstract fun build(): S      // 상태를 만드는 유일한 자리
    abstract fun onIntent(event: E)        // 값만 바꾼다. 상태를 만들지 않는다
    val state: StateFlow<S>                // 화면이 구독하는 것
}
```

## 동작

```
[onIntent]                    [유스케이스 / 외부 Flow]
     │                                 │
     ▼                                 ▼
 trigger(initial)             flow.triggerIn(initial)
 MutableStateFlow(쓰기)        StateFlow(읽기 전용)
     └───────────┬─────────────────────┘
                 ▼
          triggers (선언 자리에서 자동 등록)
                 │  하나라도 방출 → "바뀌었다" 신호 (값은 버림)
                 ▼
              merge()
                 ▼
   build() — 모든 값의 .value 를 읽어 상태 전체 조립
                 ▼
   stateIn(WhileSubscribed 5초) → state → UI collect
```

흘러온 값 자체는 쓰지 않고 **신호로만** 쓴다. 값이 필요 없는 이유는 `build()` 가 각 값의
현재값을 그 순간 직접 읽기 때문이다. 덕분에 등록 개수가 늘어도 조합 타입이 안 늘고,
여러 값이 거의 동시에 바뀌어도 반쯤 낡은 조합이 조립되지 않는다.

## 값의 세 성격 — 등록 기준은 하나

**`build()` 가 그 값을 읽는가.**

| 성격 | 만드는 법 | 타입 |
|---|---|---|
| 이벤트가 직접 쓰는 값 (검색어, 선택한 탭) | `trigger(initial)` | `MutableStateFlow` — 쓰기 가능 |
| 바깥 흐름에서 오는 값 (유스케이스 스트림, 파생) | `flow.triggerIn(initial)` | `StateFlow` — 읽기 전용 |
| 안 바뀌는 참조 (`Flow<PagingData>` 등) | 일반 프로퍼티 | 그대로 |

`build()` 가 안 읽고 다른 흐름의 재료로만 쓰이는 값(재조회 트리거 등)은 등록할 이유가 없다.

## 쓰는 법

```kotlin
@HiltViewModel
class NotesViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
) : MviViewModel<NotesUiState, NotesIntent>() {

    // 1) 이벤트가 쓰는 값
    private val query = trigger("")

    // 2) 바깥 스트림 — Room/DataStore 처럼 살려두는 게 이득이면 Lazily 로 옵트인
    private val categories = getCategories()
        .catch { emit(emptyList()) }
        .triggerIn(emptyList(), started = SharingStarted.Lazily)

    // 3) 파생도 triggerIn 으로 — 재료가 바뀔 때만 계산된다(= remember 의 대응물)
    private val visible = combine(categories, query) { c, q -> filter(c, q) }
        .triggerIn(persistentListOf())

    // 4) build 는 조립만
    override fun build() = NotesUiState(
        query = query.value,
        categories = visible.value,
    )

    override fun onIntent(event: NotesIntent) {
        when (event) {
            is NotesIntent.QueryChanged -> query.value = event.query
        }
    }
}
```

Route 는 `state` 를 `collectAsStateWithLifecycle()` 로 받는다. 구독 관리는 그쪽이 한다.

---

# 주의점

## 1. `build()` 안에 계산을 두지 마라 — 파생은 `triggerIn` 으로

`build()` 는 그냥 함수라 불릴 때마다 안의 계산을 전부 다시 실행한다. `remember` 같은
메모이제이션이 없다. **첫 구독만으로도 `build()` 는 등록 개수만큼 돈다**(측정: 등록 4개 → 8회).

```kotlin
// X — query 만 바뀌어도 전체 정렬이 다시 돈다
override fun build() = UiState(
    recent = categories.value.flatMap { … }.sortedByDescending { … },
)

// O — categories 가 바뀔 때만 계산된다
private val recent = categories.map { it.flatMap { … }.sortedByDescending { … } }
    .triggerIn(persistentListOf())
override fun build() = UiState(recent = recent.value)
```

## 2. `trigger.value = trigger.value + 1` 금지 — `update {}` 만 써라

read-modify-write 라 원자적이지 않다. 동시 갱신 199,968회 측정에서 직접 대입은
**4,760~9,438건만 살아남았다(95~98% 유실)**. `update {}` 는 매번 정확히 199,968.

```kotlin
expanded.update { it + (name to !(it[name] ?: false)) }   // O
expanded.value = expanded.value + (name to …)             // X
```

## 3. 함께 움직이는 값은 한 trigger 로 묶어라

`Main.immediate` 라 trigger 를 쓰면 수집기가 같은 콜스택에서 인라인으로 깨어난다.
한 핸들러가 trigger N개를 바꾸면 **N번 조립되고 N번 방출되며, N-1개가 중간 상태**다
(측정: trigger 3개 변경 → 방출 3회). trigger 하나만 건드리면 정확히 1회로 최적.

```kotlin
// X — "로딩 끝 + 데이터 없음" 같은 존재하지 않는 조합이 잠깐 방출된다
private val loading = trigger(true)
private val error = trigger<String?>(null)
private val data = trigger(emptyList<Note>())

// O — 한 타입으로 묶는다
private val load = trigger<LoadState>(LoadState.Loading)
```

단, **입력과 그 결과는 일부러 나눈다.** 검색어(trigger)와 결과(triggerIn)가 둘 다 등록돼
방출이 2번 나는 건 의도된 동작이다 — 입력은 즉시, 결과는 도착했을 때 반영되어
타이핑이 네트워크를 기다리지 않는다.

## 4. `S` 는 값 동등성이 있어야 한다

같은 값이면 `build()` 조차 안 돌고(측정: 재대입 3회 → build 0회) 방출도 없다. 이 이점은
`equals` 에 기댄다. **identity equals 인 값을 `build()` 안에서 새로 만들면 안 된다.**

```kotlin
// X — 매번 새 인스턴스 → equals 가 깨져 conflate 가 통째로 무력화
override fun build() = UiState(snackbarHostState = SnackbarHostState())

// O — ViewModel 이 소유하고 넘기기만
private val snackbarHostState = SnackbarHostState()
override fun build() = UiState(snackbarHostState = snackbarHostState)
```

## 5. `triggerIn` 은 선언 순서에 의존한다

호출 즉시 upstream 을 잡으므로, 그 흐름이 참조하는 `trigger` 는 **반드시 위쪽 줄**에
선언돼 있어야 한다.

## 6. 이벤트 핸들러에서 `state.value` 를 읽지 마라

구독 전에는 초기값이다. 핸들러는 `trigger` 를 직접 읽는다.

## 7. 등록은 생성 시점에만

`trigger`/`triggerIn` 은 프로퍼티 초기화 때 호출한다. 초기화 이후(메서드 안 등)에 부르면
파이프라인이 이미 만들어진 뒤라 조용히 무시될 수 있다.

## 8. 등록에서 빠지면 화면이 멈춘다

바뀌는 값을 `trigger`/`triggerIn` 없이 일반 프로퍼티로 두고 `build()` 에서 읽으면,
값이 바뀌어도 재조립이 안 돌아 화면이 그대로 멈춘다. 증상은 항상 "안 바뀐다" 하나다.

## 9. 수명 — 정책은 `state` 한 곳뿐

`state` 가 `WhileSubscribed(5초)`(구성 변경 유예), `triggerIn` 은 기본이
`WhileSubscribed(0)` 이라 `state` 가 멈추면 곧바로 따라 멈춘다. **체인이 깊어져도
정지까지 5초로 고정**된다(예전엔 단계마다 5초씩 쌓여 3단이면 15초였다).

Room/DataStore 처럼 변경 시에만 방출하는 옵저버는 살려두는 게 이득이다:

```kotlin
.triggerIn(emptyList(), started = SharingStarted.Lazily)
```

`Lazily` 는 구독 전엔 시작하지 않지만 **한 번 시작하면 `onCleared` 까지 안 멈춘다.**
폴링·위치·센서처럼 계속 도는 게 부담인 흐름에는 쓰지 말 것.

## 10. 일회성 효과 자리는 아직 없다

스낵바·네비게이션처럼 상태가 아닌 사건을 놓을 채널이 base 에 없다. 현재는 상태에
`errorId: Int` 같은 카운터를 두고 그 값이 바뀔 때 발동시키는 식으로 우회한다.

---

# 보장되는 것 (측정 완료)

| 항목 | 근거 |
|---|---|
| `build()` 동시 재진입 없음 | Main = Default / immediate 양쪽, `maxInFlight` 항상 1 |
| `update {}` lost update 없음 | 19만회 동시 갱신에서 무손실 |
| 최종 상태 유실 없음 | 40만회 폭주에도 항상 정확 (중간 방출은 conflate) |
| 파이프라인 단일 초기화 | 48스레드 동시 첫 접근에 인스턴스 1개, `build` 1회 |
| 같은 값은 조립조차 안 함 | 재대입 3회 → `build` 0회 |

# 테스트

`core/common/src/test/.../mvi/` — 23건.

| 파일 | 조건 | 축 |
|---|---|---|
| `MviViewModelTest` | 가상시간(`StandardTestDispatcher`) | 초기값 · 재조립 트리거 · 조립 일관성 · 수명 · trigger 여럿 · 유스케이스 스트림 |
| `MviViewModelConcurrencyTest` | 실제 스레드, Main = `Dispatchers.Default` | `by lazy` 동시 초기화 · 동시 갱신 |
| `MviViewModelMainImmediateConcurrencyTest` | 실제 스레드, Main = immediate 인라인 | 실제 안드로이드 조건의 재진입·정확성 |

**base 를 고치면 변이 테스트로 검출력을 확인할 것.** 등록 누락·`SharingStarted` 변경 같은
회귀는 현재 스위트가 7~9건씩 잡는다.

주의: 가상시간 테스트에서 수집기는 반드시
`backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))` 로 띄운다.
**`advanceUntilIdle()` 은 `backgroundScope` 코루틴을 깨우지 않아** 수집기가 안 붙은 채로
테스트가 통과해버린다.

```bash
./gradlew :core:common:testDebugUnitTest
```
