# 아키텍처 & 모듈 경계

## 모듈 그래프 (→ = 의존)

```
:app   → :core:{design-foundation, design-system, domain, data, network, database}, :feature:notes
         (composition root: 구현체 모듈을 모두 포함해 Hilt 그래프를 완성)

:feature:notes → :core:common, :core:design-system, :core:domain, :core:navigation   (feature convention 이 자동 주입)

:core:common     — leaf. MVI base(`MviViewModel`) 만. 다른 모듈도, Compose 도 의존하지 않는다.
:core:navigation — leaf. 내비 계약(키·씬·모션·공유요소 배선) + nav3(api). 테마/도메인 의존 없음.
:core:domain   — 가장 안쪽. 모델(Category/Note/NoteBlock) + 인터페이스(NotesRepository/TokenRepository) + UseCase. 의존 없음.
:core:data     → :core:domain   (도메인 인터페이스 구현 + Remote/Local/Token DataSource 인터페이스 소유)
:core:network  → :core:data     (RemoteNotesDataSource 구현 = Contents API, AuthInterceptor + GithubTokenHolder)
:core:database → :core:data     (Local/Token DataSource 구현 = Room 노트캐시 + DataStore 토큰)
:core:design-system → api(:core:design-foundation)
```

> **DIP 핵심:** 인터페이스는 안쪽(domain/data)이 소유하고, 바깥(network/database)이 구현한다.
> 의존 화살표는 항상 바깥→안쪽. `:app` 만 구현체 모듈을 알고 Hilt 로 바인딩한다. **SSOT 는 Room.**

## 하드 룰
- **feature 모듈은 `:core:data`/`:core:network`/`:core:database` 를 직접 의존하지 않는다.** `:core:domain` UseCase 만 경유(도메인 모델은 domain 소유).
- **`:core:network`/`:core:database` 는 `:core:data` 의 DataSource 인터페이스를 구현**하고, `:core:data` 는 그 구현체를 모른다.
- Room `@Entity`·Retrofit·DataStore 타입은 각 모듈 밖으로 새지 않는다(경계 교차 타입: `CachedNote` 등 data 소유 타입).
- **`:core:common` 은 Android UI/Compose 를 의존하지 않는다.** leaf 유지(다른 `:core:*` 의존 금지). 화면 관련 공용물은 `:core:design-system`.
- 역방향 의존 금지: `:core:design-foundation` 은 `:core:design-system` 을 의존하지 않는다. feature ↔ feature 직접 의존 금지.
- 버전은 `/gradle/libs.versions.toml` 에만. `build.gradle.kts` 에 하드코딩 금지(의존성도 `libs.*` alias).

## 프레젠테이션: MVI (전체 재조립형)

상태: `XxxUiState`(`@Stable`/`@Immutable`, `MviState` 구현) · 인텐트: `sealed interface XxxIntent : MviEvent`.
ViewModel 은 `:core:common` 의 **`MviViewModel<S, E>`** 를 상속한다.

- **상태를 만드는 자리는 `build()` 하나뿐.** 등록된 값이 하나라도 바뀌면 상태를 통째로 다시 만든다.
- 값은 성격으로 나눈다 — 이벤트가 쓰는 값은 `trigger(initial)`, 바깥 Flow 는 `flow.triggerIn(initial)`,
  안 바뀌는 참조는 일반 프로퍼티. **등록 기준은 하나: `build()` 가 그 값을 읽는가.**
- `onIntent(event)` 는 **상태를 만들지 않는다.** trigger 값만 바꾸고 재조립은 `build()` 가 한다.
- 상태 노출은 `state: StateFlow<S>` (`WhileSubscribed(5초)` — 구성 변경 유예).
- **파생 계산을 `build()` 안에 두지 않는다.** `triggerIn` 으로 빼야 재료가 바뀔 때만 계산된다.
- **상태홀더(예: `SnackbarHostState`)는 ViewModel 이 인스턴스를 소유**하고 `build()` 는 넘기기만 한다.
  발동은 화면에서 `LaunchedEffect(uiState.errorId)` 처럼 id 를 보고 한다.
- 화면 Composable 은 콜백을 `onIntent: (XxxIntent) -> Unit` **하나만** 받는다. design-system 의 재사용 컴포넌트는 MVI 를 모르고 순수 콜백(`onClick` 등)을 받는다.

> 상세 사용법·주의점은 `core/common/src/main/java/com/hancekim/mynote/core/common/mvi/AGENTS.md` 참조.

### Route / Screen 분리 (필수)
- **`XxxRoute`** = ViewModel 주입(`hiltViewModel()`) + 상태 수집(`collectAsStateWithLifecycle`)만. ViewModel 의존 때문에 프리뷰 불가. `XxxScreen` 을 호출만 한다.
- **`XxxScreen`** = stateless. `uiState` + `onIntent: (Intent) -> Unit`(+ 필요한 host state)만 받는다. **테마 블록(`MyNoteTheme { … }`)은 Screen 에서 시작**한다.
- **Screen 에는 항상 `@Preview`** — ViewModel 없이 더미 `uiState` + `onIntent = {}` 로 렌더. 폼팩터별로 `@Preview(widthDp = …)` 를 여러 개 둔다.

```kotlin
@Composable fun XxxRoute(vm: XxxViewModel = hiltViewModel()) {
    val uiState by vm.state.collectAsStateWithLifecycle()
    XxxScreen(uiState, onIntent = vm::onIntent)
}
@Composable fun XxxScreen(uiState: XxxUiState, onIntent: (XxxIntent) -> Unit) {
    MyNoteTheme(...) { /* 화면 */ }
}
@Preview @Composable private fun XxxScreenPreview() = XxxScreen(previewUiState, onIntent = {})
```

## 내비게이션: Navigation 3 (`NavDisplay` + 커스텀 Scene)

**목적지 키·씬·모션은 전부 `:core:navigation`** 에 모인다. feature 끼리 서로를 몰라도 되고,
나중에 `:feature:root` 가 하나의 백스택으로 전부 태울 수 있게 하기 위해서다.
`MyNoteNavKey` 는 `sealed` 라 **새 feature 의 키도 그 모듈에 추가**한다.

**백스택의 소유자는 `:core:navigation` 의 `Navigator` 하나다.** nav3 도 ViewModel 도 스택을
들지 않는다 — ViewModel 은 `navigator.stack` 을 `triggerIn` 으로 읽어 상태로 나르고,
`NotesUiState.activeNoteId` 는 저장값이 아니라 **스택에서 파생된** 값이다. 소유자가 하나라는
성질은 그대로고 자리만 옮겼다.

**스택을 바꾸는 길은 `Navigator.dispatch(NavCommand)` 하나뿐이다.** 입구가 하나여야 거기서
쓰로틀을 걸 수 있다 — 전환이 끝나기 전에 들어온 이동은 **버린다**(큐에 쌓지 않는다. 쌓으면
누른 만큼 결국 다 이동해 막는 의미가 없다). 스택을 **바꾸기 전에** 막는 것이 핵심이다.
먼저 바꾸고 되돌리면 그사이 `NavDisplay` 가 전환을 시작해 화면과 스택이 어긋난다.
사용자 입력이 아닌 정정(동기화 중 삭제된 노트)·딥링크는 `dispatchImmediately` 로 게이트를 우회한다.

- **`NavDisplay` 는 하나뿐이다.** 폼팩터에 따라 갈리는 건 **씬 전략(sceneStrategies)** 뿐.
  | 너비 | 전략 | 결과 |
  |---|---|---|
  | Compact | (없음) → 기본 `SinglePaneSceneStrategy` | 홈/리더가 **별개 씬 레이어**로 쌓임. 목록은 `ModalNavigationDrawer` |
  | Medium+ | `ListDetailSceneStrategy` | 고정 패널 + 상세 한 칸. 헤더도 씬 안(상세 칸 위)에 |
- **커스텀 씬의 핵심은 `key` 가 상세에 따라 안 바뀐다는 것.** 그래야 `NavDisplay` 가 씬을 통째로
  슬라이드시키지 않고, 씬이 자기 안의 `AnimatedContent` 로 본문만 페이드 스루로 바꾼다.
  `previousEntries = entries.dropLast(1)` 이라 뒤로가기는 그대로 살아 있다.
- 씬에 넘기는 슬롯(`panel`/`header`/`panelExpanded`)은 **`remember` 로 정체성을 고정**한다.
  매 컴포지션마다 새 람다가 들어가면 전략이 달라진 것으로 보여 씬이 통째로 다시 만들어진다.
  안의 값은 `rememberUpdatedState` 로 읽는다.
- **모션은 `NavDisplay` 의 세 spec 에 모아 둔다** (`SceneMotion`). 예측 뒤로가기는
  `SeekableTransitionState` 가 이 전환을 직접 seek 한다 — 별도 `PredictiveBackHandler` 를 달지 않는다.
  `EnterTransition` 으로 표현 못 하는 값(모서리 반경·그림자)만 엔트리가 자기
  `AnimatedContentScope` 의 트랜지션을 읽어 `graphicsLayer` 에서 계산한다(`rememberSceneEdgeTransform`).
- **씬은 테마를 모른다.** 패널의 폭·구분선·색은 `panel` 슬롯이 그린다 — 그래야 씬이 feature 를 안 탄다.
  > **커밋 이후 마무리 애니메이션도 `predictivePopTransitionSpec` 그대로 이어서 재생된다.**
  > 그래서 그 spec 은 진행률 1 에서 화면 밖으로 완전히 빠지는 값이어야 한다.
- 공유 요소는 `SharedTransitionLayout` 이 `NavDisplay` 를 감싸고, 엔트리가 자기
  `AnimatedContentScope` 를 `ProvideNavAnimatedScope` 로 내려 준다. 키는 `chip-/date-/title-{noteId}`.
  `LocalNavAnimatedContentScope` 를 화면에서 직접 읽지 않는다 — 미설정 시 예외라 프리뷰가 깨진다.
- **뒤로가기 우선순위**(LIFO — 나중에 컴포즈된 핸들러가 이긴다):
  드로어(`ModalDrawerSheet(drawerState=…)`) → **전환 중 삼킴** → `NavDisplay` → **펼친 고정 패널 접기**
  → 앱 종료(두 번 누르기).
- **펼친 고정 패널도 뒤로가기를 먹는다.** 넓은 창의 목록 패널은 백스택 엔트리가 아니지만
  (`rememberSaveable` 로컬 상태), 펼쳐 둔 걸 접는 게 Compact 의 드로어와 대칭이다. 이걸 빠뜨리면
  홈에서 패널을 펴 둔 채 뒤로가기 했을 때 **종료 안내가 먼저 먹는다.**
- **드로어 판정은 `isOpen` 이 아니라 `targetValue` 로 한다.** `DrawerState.isOpen` 은 `currentValue`
  이고 그 값은 `AnchoredDraggable` 이 **애니메이션이 끝난 뒤** `finally` 에서 넣는다. 그래서 여는
  동안은 `isOpen == false` 인데, Material3 의 핸들러도 `PredictiveBackHandler(enabled = isOpen)` 이라
  같이 꺼져 있다 — 이 틈에 뒤로가기가 **종료 안내로 샌다**(logcat 으로 `consumed: EXIT` 확인).
  `targetValue == Open` 까지 봐야 틈이 없다.
- **여는 중인 드로어는 우리가 받아 닫는다.** 비활성만 시키면 그 틈에 아무도 back 을 안 받아
  시스템으로 넘어가 앱이 그냥 꺼진다.
- **시스템 뒤로가기는 `NavDisplay` 에 닿기 전에 막는다.** `NavDisplay` 는 예측 뒤로가기
  애니메이션을 **이미 커밋한 뒤** `onBack` 을 부르므로, 거기서 버리면 화면은 닫혔는데 스택은
  그대로라 어긋난다. 그래서 `NavDisplay` **호출 뒤에** `BackHandler(enabled = !isNavSettled)` 를
  놓아 창이 열려 있는 동안 삼킨다(`activity-compose` 의 `BackHandler` 는 `NavigationEventDispatcher`
  가 있으면 그쪽에 등록하므로 nav3 의 핸들러와 같은 LIFO 를 탄다).

## 레이아웃: 너비 × 높이 양축
- 최종 레이아웃 = **너비 클래스 × 높이 클래스**. 너비는 가로 구조(패널·본문 폭·카드 폭),
  높이는 세로 리듬(헤더·여백·히어로)과 **플로팅 내비 형태**를 정한다. 둘 다 `AdaptiveTokens` 에.
- 홈 그리드는 **`GridCells.Adaptive(minSize = cardMinWidth)`** — 고정 열 수 금지
  (폴더블 접기/펴기·분할 화면·자유 창에서 너비가 연속적으로 변한다).
- 플로팅 내비: `useFloatingRail` = 너비 Expanded+ **또는** 높이 Compact → 우측 세로 레일, 아니면 하단 바.
  기준 컨테이너는 **헤더 아래 콘텐츠 영역**이다(넓은 창은 헤더가 씬 안이라 오프셋을 직접 빼 준다).
- 목록 패널의 ☰ 토글은 두 폼팩터 모두 살아 있다. 펼침 상태는 `rememberSaveable`.
- ViewModel 주입: `hiltViewModel()` (`androidx.hilt.lifecycle.viewmodel.compose`).

## Convention Plugins (항상 사용, 수동 설정 금지)
| alias | 용도 |
|---|---|
| `hancenote.android.application` (+ `.compose`) | `:app` |
| `hancenote.android.library` (+ `.compose`) | core 라이브러리 |
| `hancenote.android.feature` | `:feature:*` (`:core:common` + `:core:design-system` + `:core:domain` + `:core:navigation` + lifecycle + coroutines 자동) |
| `hancenote.android.hilt` | Hilt 모듈 |
| `hancenote.android.room` | Room 모듈(`:core:database`) |

`hancenote.ktlint`(ktlint + compose-rules)는 application/library convention 이 자동으로 적용하므로 모듈에서 따로 선언하지 않는다.

> AGP 9 기준: Kotlin 빌트인이라 **`org.jetbrains.kotlin.android` 적용 금지**(새 DSL 과 비호환).
> `CommonExtension` 은 타입 파라미터가 없어졌고 블록 메서드는 사라졌지만 getter 는 남아 있다
> (`commonExtension.defaultConfig.minSdk = …`). Circuit·JVM library·android test convention 은 제거됨.
