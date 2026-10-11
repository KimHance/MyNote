# AGENTS.md — :core:common

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈의 자리

프로젝트에서 가장 바깥 레이어가 공통으로 쓰는 **얇은 뼈대 세 개**만 둔다.

| 패키지 | 내용 | 쓰는 곳 |
|---|---|---|
| `mvi/` | `MviViewModel<S, E>` · 마커 `MviState` / `MviEvent` | `:feature:notes` 의 `NotesViewModel` |
| `throttle/` | `Throttler` · `Clock.kt`(단조 `fun interface Clock` · `SystemClockSource`, 벽시계 `fun interface WallClock` · `SystemWallClockSource`) | `:core:navigation` 의 `Navigator`, `:core:design-system` 의 `throttledProcess`, `:feature:notes` 의 ViewModel(주입받음 · `:app` 의 `AppModule` 이 묶음) |
| `result/` | `runCatchingCancellable` | `:core:data` 의 `NotesRepositoryImpl`, `:core:ai` 의 `LiteRtLmQuizGenerator` |

**leaf 다.** `build.gradle.kts` 의 구현 의존은 `androidx.lifecycle.viewmodel` 과
`kotlinx-coroutines-android` 둘뿐이고, 다른 `:core:*` 도 Compose 도 없다. 여기에 의존을
더하면 모듈 그래프 전체가 그걸 따라오므로 **추가하지 말고, 화면 관련 공용물은
`:core:design-system` 으로 보낸다.**

`:core:common` 을 가져다 쓰는 쪽은 `projects.core.common` 을 직접 선언하고
(`:core:navigation` · `:core:design-system` · `:core:data` · `:core:ai` · `:app`),
`:feature:*` 는 `AndroidFeatureConventionPlugin` 이 자동으로 넣는다.

> **경계의 유일한 예외:** `Clock.kt` 의 `SystemClockSource` 가 `android.os.SystemClock.elapsedRealtime()` 을 쓴다
> (옆의 `SystemWallClockSource` 는 `System.currentTimeMillis()`).
> UI 는 아니지만 안드로이드 SDK 다 — 그래서 이 모듈은 순수 JVM 모듈이 아니라 안드로이드
> 라이브러리이고, 아래 "Throttler" 절의 테스트 제약이 여기서 나온다.

---

## MVI — 전체 재조립형

**상태의 일부를 고치는 코드가 없다.** 등록된 값이 하나라도 바뀌면 `build()` 가 상태를
통째로 다시 만든다. 그 대가로 "지금 화면이 왜 이 값인가"가 **함수 하나를 읽는 문제**가 되고,
`copy()` 가 여기저기 흩어져 서로를 덮어쓰는 일이 구조적으로 불가능해진다.

`MviViewModel.kt` 는 45줄이다. 전부가 아래 파이프라인 하나다.

```
[onIntent]                       [유스케이스 / 외부 Flow]
    │                                      │
    ▼                                      ▼
trigger(initial)                 flow.triggerIn(initial)
= MutableStateFlow (쓰기)         = StateFlow (읽기 전용)
    └──────────────┬───────────────────────┘
                   ▼
        triggers (선언 자리에서 자동 등록되는 리스트)
                   │  하나라도 방출 → "바뀌었다" 신호. 흘러온 값 자체는 버린다
                   ▼
                merge()
                   ▼
      map { build() }  — 각 값의 .value 를 그 순간 읽어 상태 전체를 조립
                   ▼
   stateIn(WhileSubscribed 5초) → state → 화면이 collect
```

흘러온 값을 쓰지 않고 **신호로만** 쓰는 게 핵심이다. `build()` 가 각 값의 현재값을 직접
읽으므로 등록이 늘어도 `combine` 의 조합 타입이 늘지 않고, 여러 값이 거의 동시에 바뀌어도
반쯤 낡은 조합이 조립되지 않는다.

### 등록 기준은 하나 — `build()` 가 그 값을 읽는가

| 성격 | 만드는 법 | 타입 |
|---|---|---|
| 이벤트가 직접 쓰는 값 (검색어, 선택한 탭) | `trigger(initial)` | `MutableStateFlow` — 쓰기 가능 |
| 바깥에서 오는 값, 파생 값 | `flow.triggerIn(initial)` | `StateFlow` — 읽기 전용 |
| `build()` 가 안 읽는 값 (Job, 시퀀스 카운터, 다른 흐름의 재료) | 일반 프로퍼티 | 그대로 |

실물 예: `NotesViewModel` 의 `refreshTicks` 는 `MutableStateFlow` 지만 `trigger` 가 아니다 —
`sync` 흐름의 재료일 뿐 `build()` 가 읽지 않는다. `quizJob` / `undoSeq` / `lastRootBackMillis`
도 같은 이유로 그냥 프로퍼티다.

### 쓰는 법

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
        .triggerIn(persistentListOf(), started = SharingStarted.Lazily)

    // 3) 파생도 triggerIn — 재료가 바뀔 때만 계산된다(Compose 의 remember 에 해당)
    private val visible = combine(categories, query) { list, q -> filter(list, q) }
        .triggerIn(persistentListOf())

    // 4) build 는 조립만 한다
    override fun build() = NotesUiState(query = query.value, categories = visible.value)

    // 5) onIntent 는 값만 바꾼다. 상태를 만들지 않는다
    override fun onIntent(event: NotesIntent) = when (event) {
        is NotesIntent.QueryChanged -> query.value = event.query
    }
}
```

화면 쪽 배선은 `XxxRoute` 가 `hiltViewModel()` 과 `collectAsStateWithLifecycle()` 로 상태를 모아
`XxxScreen(uiState, onIntent)`(ViewModel 없이 프리뷰되는 stateless)을 부르는 모양이다. 화면별 규칙은 `feature/notes/AGENTS.md`.

---

## 모르고 쓰면 깨지는 것

### 1. 등록에서 빠지면 화면이 조용히 멈춘다
바뀌는 값을 일반 프로퍼티로 두고 `build()` 에서 읽으면 재조립이 안 돌아 화면이 그대로
멈춘다. 예외도 로그도 없고 증상은 항상 "안 바뀐다" 하나다
(`등록하지 않은 값은 바뀌어도 재조립되지 않는다`). 극단은 등록이 0개인 경우 —
`emptyList().merge()` 는 즉시 완결되는 Flow 라 `state` 가 초기값에서 영구히 굳는다.

### 2. `build()` 안에 계산을 두지 마라
`build()` 는 그냥 함수다. 메모이제이션이 없어 불릴 때마다 안의 계산을 전부 다시 한다.
게다가 **구독이 붙는 순간 등록된 trigger 들이 각자 현재값을 즉시 방출**하므로 `build()` 는
등록 개수만큼 연달아 돈다(trigger 1개인 테스트 VM 이 재구독에서 정확히 +1회).
정렬·필터·매핑은 `triggerIn` 으로 빼라 — 그래야 재료가 바뀔 때만 돈다.

```kotlin
// X — query 만 바뀌어도 전체 정렬이 다시 돈다
override fun build() = UiState(recent = categories.value.flatMap { … }.sortedBy { … })

// O — categories 가 바뀔 때만 계산된다
private val recent = categories.map { it.flatMap { … }.sortedBy { … } }.triggerIn(persistentListOf())
```

### 3. `S` 는 값 동등성이 있어야 한다
같은 값이면 `stateIn` 이 방출을 삼킨다(`첫 구독 때 상태가 그대로면 중복 방출하지 않는다`
— `build()` 는 두 번 돌지만 방출은 1회). 이 이점 전부가 `equals` 에 기댄다.
**identity equals 인 객체를 `build()` 안에서 새로 만들면 conflate 가 통째로 무력화된다.**
상태홀더는 ViewModel 이 인스턴스를 소유하고 `build()` 는 넘기기만 한다
(`NotesViewModel` 의 `private val snackbarHostState = SnackbarHostState()`).

### 4. `triggerIn` 은 선언 순서에 의존한다
`triggerIn` 은 호출되는 즉시 `stateIn` 을 만들고, 그 표현식은 재료를 그 자리에서 참조한다.
재료가 되는 `trigger` 는 **반드시 위쪽 줄**에 선언돼 있어야 한다. 아래에 있으면 프로퍼티
초기화 순서상 아직 `null` 이라 생성 시점에 터진다.

### 5. 등록은 생성 시점에만 — `state` 는 `by lazy` 다
`triggers` 는 동기화 없는 `mutableListOf` 이고, `state` 가 처음 읽힐 때 그 리스트를 한 번
스냅샷해 파이프라인을 만든다. 그래서 `trigger`/`triggerIn` 은 **프로퍼티 초기화나 `init`
안에서만** 부른다. 그 뒤에 부르면 조용히 무시된다.

`by lazy` 라는 성질에는 따라오는 게 하나 더 있다 — **초기 `build()` 는 생성 시점이 아니라
`state` 를 처음 읽는 시점에 돈다.** 생성과 첫 읽기 사이에 `build()` 가 읽는 값이 바뀌면
초기 상태가 그 나중 값을 담는다. 테스트에서 특히 걸리는 지점이다(아래 "테스트" 참조).

### 6. `trigger.value = trigger.value + 1` 금지 — `update {}` 를 써라
read-modify-write 는 원자적이지 않다. 동시 갱신은 `update {}` 로만 보장된다
(`MviViewModelConcurrencyTest` 가 실제 스레드 19만회 갱신에서 무손실을 확인한다).
누적·토글은 전부 `update {}`: `exitRequests.update { it + 1 }`,
`expanded.update { it.putting(name, !(it[name] ?: false)) }`.

### 7. 함께 움직이는 값은 한 trigger 로 묶어라
`merge` 는 방출마다 `build()` 를 한 번 부른다. 한 핸들러가 trigger N개를 바꾸면
**N번 조립되고 N번 방출되며, 중간 N-1개는 아직 덜 바뀐 조합**이다. `Main.immediate` 라
수집기가 같은 콜스택에서 인라인으로 깨어나므로 그 중간값이 실제로 화면에 나간다.

```kotlin
// X — "로딩 끝 + 데이터 없음" 같은 존재하지 않는 조합이 잠깐 방출된다
private val loading = trigger(true)
private val error = trigger<String?>(null)

// O — 한 타입으로 묶는다 (NotesViewModel 의 SyncState 가 이 형태)
private val load = trigger<LoadState>(LoadState.Loading)
```

단, **입력과 그 결과는 일부러 나눈다.** 검색어(`trigger`)와 결과(`triggerIn`)가 둘 다 등록돼
방출이 2회 나는 건 의도된 것이다 — 입력은 즉시, 결과는 도착했을 때 반영되어 타이핑이
네트워크를 기다리지 않는다(`입력은 즉시 반영되고 유스케이스 결과는 도착 시점에 반영된다`).

### 8. 핸들러에서 `state.value` 를 읽지 마라
구독 전이면 초기값이다. `onIntent` 는 `trigger` 를 직접 읽는다.

### 9. 수명 — 정책이 있는 곳은 `state` 한 군데뿐
`state` 는 `WhileSubscribed(5초)`(구성 변경 유예), `triggerIn` 기본값은
`WhileSubscribed(0)`. 그래서 `state` 가 멈추면 체인이 곧바로 따라 멈추고 **파생이 몇 단이든
정지까지는 5초로 고정**된다(단계마다 5초씩 쌓이지 않는다 —
`파생 triggerIn 을 거쳐도 정지 타임아웃은 쌓이지 않는다`).

Room/DataStore 처럼 변경 시에만 방출하는 옵저버는 살려두는 게 이득이라
`triggerIn(…, started = SharingStarted.Lazily)` 로 옵트인한다. `Lazily` 는 구독 전엔 시작하지
않지만 **한 번 시작하면 `onCleared` 까지 안 멈춘다** — 폴링·센서처럼 계속 도는 게 부담인
흐름에는 쓰지 말 것.

### 10. 일회성 효과를 놓을 자리가 base 에 없다
스낵바·네비게이션처럼 "상태가 아닌 사건"을 위한 채널이 없다. 현재 우회법은 상태에
**id 카운터**를 두고 화면이 그 값 변화를 보고 발동하는 것이다 — `errorId` / `infoId` /
`exitHintId` / `quizUndoId` 를 `LaunchedEffect(uiState.errorId)` 가 받는다. base 에 이벤트
채널을 새로 만들기 전에 이 패턴을 먼저 고려하라.

id 만 두면 Activity 가 다시 만들어질 때 새 컴포지션의 `LaunchedEffect` 가 같은 id 로 다시 발동한다.
화면이 효과를 끝낸 뒤 "봤음" 인텐트를 보내고 VM 이 그 id 를 지우게 한다(`:feature:notes` 의 `SignalShown`).

---

## Throttler

"마지막으로 **통과한** 시점"이 기준이다. 구간 시작이 아니고, **버려진 호출은 기준을 밀지
않는다**(`버려진 호출은 기준 시점을 밀지 않는다`). 그래서 연타 중에도 최소 간격마다
정확히 한 번 통과한다. 기본 간격은 `DEFAULT_MIN_INTERVAL_MILLIS = 500L`.

### `isIdle()` 과 `tryAccept()` 를 왜 나눴는가
`process { }` / `tryAccept()` 는 "확인하고 바로 창을 소비한다". 단순 클릭 쓰로틀
(`throttledProcess`)에는 그게 맞다. 하지만 **통과 여부를 먼저 보고, 조건을 더 따진 뒤에야
창을 소비해야 하는** 호출자가 있다. `Navigator.dispatch` 가 그렇다.

```kotlin
if (!throttle.isIdle()) return NavResult.Throttled      // 스택을 바꾸기 전에 막는다
val next = reduce(stack.value, command) ?: return NavResult.Ignored  // no-op 은 창을 안 먹는다
apply(next)                                             // 여기서 throttle.tryAccept()
```

`tryAccept()` 하나로 합치면 **아무것도 바꾸지 않는 no-op 명령이 쓰로틀 창을 소비**해
뒤이은 정상 이동을 막는다. 둘을 나눈 이유가 이것이다.

### `Clock` 을 주입 가능하게 한 이유
두 가지가 동시에 해결된다.

- **테스트.** `fun interface` 라 SAM 람다 한 줄로 시간을 지배한다. `ThrottlerTest` 는
  `Throttler(500L) { now }` 로 `now` 를 직접 밀고, `NavigatorTest` 는
  `Navigator(this, home, WINDOW) { currentTime }` 로 코루틴 가상시간에 묶어
  `advanceTimeBy` 가 쓰로틀 창까지 같이 움직이게 한다.
- **필요성.** 기본값 `SystemClockSource` 는 `SystemClock.elapsedRealtime()` 이고, 이 프로젝트의
  단위 테스트는 `returnDefaultValues` 를 켜지 않아 **JVM 테스트에서 부르면 "not mocked" 로
  터진다.** 즉 주입은 편의가 아니라 단위 테스트의 전제 조건이다.

`currentTimeMillis` 가 아니라 `elapsedRealtime` 인 것도 의도다 — 단조 증가라 사용자가
시계를 돌려도 창이 뒤틀리지 않는다.
저장에 남는 시각(에포크 밀리초 — 읽기 세션 · 퀴즈 결과 · 북마크 이벤트)은 따로 `WallClock` 으로 받는다.
`WallClock` 으로 간격을 재지 않는다.

---

## runCatchingCancellable

`runCatching` 은 `CancellationException` 까지 삼킨다. 그 결과 `flatMapLatest` 가 이전 작업을
취소했을 때 **그 취소가 `Result.failure` 로 둔갑해 "동기화 실패" 로 사용자에게 뜨고**,
취소된 코루틴이 계속 진행하려 든다. 이 함수는 취소만 다시 던지고 나머지를 `Result` 로
감싼다. **코루틴 안에서 `runCatching` 을 쓰지 말고 항상 이걸 쓴다.**

`inline` 이라 `block` 안의 `suspend` 호출이 그대로 살아 있고, 취소 재던지기 덕에
`runCatchingCancellable { … }` 뒤의 코드는 취소 시 실행되지 않는다
(`코루틴이 취소되면 블록 이후로 진행하지 않는다`).

---

## 테스트

`./gradlew :core:common:testDebugUnitTest` — 현재 **34건 전부 통과**.

| 파일 | 조건 | 건수 | 축 |
|---|---|---|---|
| `MviViewModelTest` | 가상시간 (`StandardTestDispatcher`) | 19 | 초기값 · 재조립 트리거 · 조립 일관성 · 구독/upstream 수명 · trigger 여럿 · 유스케이스 스트림 |
| `MviViewModelConcurrencyTest` | 실제 스레드, Main = `Dispatchers.Default` | 2 | `by lazy` 동시 초기화 · 동시 갱신 |
| `MviViewModelMainImmediateConcurrencyTest` | 실제 스레드, Main = 단일 스레드 + immediate 인라인 | 2 | 실제 안드로이드 조건에서의 재진입·정확성 |
| `ThrottlerTest` | 주입 clock | 7 | 기준 시점 · 연타 · reset |
| `RunCatchingCancellableTest` | `runTest` | 4 | 취소 재던지기 · 일반 예외 감싸기 |

### 동시성 테스트가 왜 실제 스레드인가
가상시간 디스패처는 **직렬화된 단일 스레드**라 경합 자체가 일어나지 않는다. 잡으려는 건
가상시간으로는 만들 수 없는 세 가지다.

- `state` 의 `by lazy` 를 48스레드가 동시에 처음 읽을 때 파이프라인이 한 개만 만들어지는가
  (`assertSame` + `builds == 1`).
- `MutableStateFlow.update {}` 폭주(5만×스레드)에서 lost update 가 없는가.
- **`build()` 가 동시에 겹쳐 돌지 않는가** — VM 이 `maxInFlight` 를 세고 항상 1임을 단언한다.
  단일 collector 라는 성질이 깨지면 `build()` 가 재진입해 상태가 찢어진다.

두 클래스로 나뉜 이유는 Main 의 성격이 다르기 때문이다. `Dispatchers.Default` 는 항상
디스패치하는 경로, `AndroidLikeMain`(단일 스레드 + `isDispatchNeeded` false)은 **수집기가
같은 콜스택에서 인라인으로 깨어나는** 실제 `Main.immediate` 경로다. 재진입은 후자에서만
드러난다.

### 함정 — `advanceUntilIdle()` 은 `backgroundScope` 를 깨우지 않는다
가상시간 테스트의 수집기는 **반드시** 이렇게 띄운다.

```kotlin
backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { … } }
```

`MviViewModelTest.subscribe()` 가 그 형태다. 디스패처를 빼고 그냥
`backgroundScope.launch { }` 로 바꿔 돌려 보면 **19건 중 5건이 빨간불이 되고 14건은 그대로
통과한다.** 실패 내용이 증상을 말해 준다 — 수집기가 붙지 않아 `seen` 이 빈 리스트이고,
`state` 가 처음 읽히는 시점이 밀려 **초기 `build()` 가 나중 값을 담는다**
(`expected:<초기> but was:<바뀜>`). 그대로 통과한 14건이 더 위험하다: 수집기 없이
초기값만 보고 초록불이 나는 **거짓 통과**다.

### 습관
이 모듈의 base 를 고치면 **변이 테스트로 검출력을 확인한다.** 통과만으로 믿지 않는다 —
등록 누락, `SharingStarted` 값 변경, 취소 재던지기 제거처럼 일부러 망가뜨려 해당 테스트가
실제로 빨간불이 되는지 본다. 위의 수집기 실험이 그 방식 그대로다.

`kotlin("test")` 는 쓰지 않는다(AGP 9 빌트인 Kotlin 에서 버전 해석 실패). `libs.junit` +
`org.junit.Assert.*`, 코루틴은 `libs.kotlinx.coroutines.test`. 테스트명은 한국어 backtick.
