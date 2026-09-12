# :feature:notes — Claude Code Instructions

3화면(Home·Reader·ListPanel) + MVI ViewModel + Nav3 호스트. feature convention 이
`:core:common` + `:core:design-system` + `:core:domain` + `:core:navigation` + lifecycle + coroutines 를 자동 주입.
**`:core:data` 를 직접 의존하지 않는다**(UseCase 경유). feature ↔ feature 의존 금지.

> **목적지 키·씬·모션은 여기 없다 — `:core:navigation` 에 있다.** 여기는 그걸 **쓰는 쪽**이다.

## 패키지
| 경로 | 역할 |
|---|---|
| `element/` | `NotesUiState`(`@Stable`) · `NotesIntent`(`sealed interface`) |
| `mapper/` | 도메인 → UI 모델 단방향 매핑(`NotesUiMapper`, `NoteBlockMapper`) |
| `navigation/` | 노트 전용 공유 요소 키 묶음(`NoteSharedModifiers`)만. 나머지는 `:core:navigation` |
| `screen/` | `HomeScreen` · `NoteReaderScreen` · `ListPanelScreen` (전부 stateless + `@Preview`) |
| (루트) | `NotesRoute`(호스트) · `NotesUiModel` · `NotesViewModel` |

## MVI (필수)
- 상태: `NotesUiState` — 단일 소스. 컬렉션은 `ImmutableList`/`ImmutableMap`.
- 인텐트: `sealed interface NotesIntent`(UI → ViewModel 단방향).
- ViewModel 은 `:core:common` 의 `MviViewModel<S, E>` 상속. `build()` 하나가 상태를 통째로 재조립.
- 화면 Composable 은 콜백을 `onIntent: (NotesIntent) -> Unit` **하나만** 받는다.

## Navigation 3 (필수)

**백스택의 소유자는 `:core:navigation` 의 `Navigator` 다.** 여기는 그걸 읽어 나르기만 한다 —
`NotesViewModel` 이 `navigator.stack`/`isSettled` 를 `triggerIn` 하고, `NotesUiState.backStack`·
`activeNoteId`·`isNavSettled` 는 전부 거기서 파생된다. `rememberNavBackStack` 도, VM 의
`activeNoteId` trigger 도 쓰지 않는다(상태가 두 군데가 되면 MVI 가 깨진다).
이동은 `SelectNote` → `navigator.push`, `CloseNote`/`Back` → `navigator.pop` 으로만 나간다.

**`Back` 과 `CloseNote` 를 가른다.** 둘 다 pop 이지만 더 갈 데가 없을 때가 다르다 — `Back` 은
앱 종료로 이어지고 `CloseNote`(하단 내비 홈 버튼)는 아무 일도 없어야 한다. 종료 판단은 화면이
아니라 `Navigator` 가 내린다(`pop() == Ignored`).

**두 번 누르기는 ViewModel 이 센다.** 횟수를 상태로 흘리면 `StateFlow` 가 conflate 해
(0→1→2 가 한 프레임에 묶이면 2 만 보인다) 눌림이 조용히 씹힌다. 상태로 나가는 건
`exitHintId`/`exitRequestId` 같은 **일회성 신호**뿐이고, 화면은 그걸 받아 실행만 한다.

- **`NavDisplay` 는 하나.** 폼팩터로 갈리는 건 `sceneStrategies` 뿐:
  - 너비 Compact → 전략 없음 → 기본 `SinglePaneSceneStrategy`. 홈/리더가 별개 씬 레이어로 쌓이고
    `SceneMotion` 의 spec 이 슬라이드·스케일·예측 뒤로가기를 맡는다. 목록은 `ModalNavigationDrawer`.
  - 너비 Medium+ → `ListDetailSceneStrategy(sceneKey = NOTES_SCENE_KEY)`. 고정 패널 + 상세 한 칸,
    **헤더도 씬 안**(상세 칸 위).
- **씬 슬롯(`panel`/`header`/`panelExpanded`)은 `remember` 로 정체성 고정.** 매번 새 람다가 들어가면
  전략이 바뀐 것으로 보여 씬이 통째로 재생성된다. 안의 값은 `rememberUpdatedState` 로 읽는다.
- **씬은 테마를 모른다** — 패널의 폭(`paneWidth`)과 구분선은 `panelSlot` 이 직접 그린다.
- 엔트리 본문은 `ProvideNavAnimatedScope` 로 감싼다. 그래야 그 아래에서 공유 요소와
  `rememberSceneEdgeTransform` 이 동작한다. 화면에서 `LocalNavAnimatedContentScope` 를 직접 읽지 않는다.
- 공유 요소 키(`chip-/date-/title-{noteId}`)는 `NoteSharedModifiers` 가 정한다 — 배선은 `:core:navigation`.
- **뒤로가기 우선순위**(LIFO): 드로어 → 전환 중 삼킴 → `NavDisplay` → 펼친 고정 패널 접기
  → 앱 종료(1초 내 두 번).
- **넓은 창에서 펼친 패널은 뒤로가기로 접는다**(`collapsiblePanelOpen`). Compact 의 드로어와 대칭이고,
  종료 핸들러를 그 조건으로 막지 않으면 홈에서 패널 펴 둔 채 뒤로가기가 종료 안내로 샌다.
  패널은 백스택 엔트리가 아니라 로컬 상태라 `Navigator` 를 거치지 않는다.
- **드로어 가드는 `drawerEngaged = isOpen || targetValue == Open`.** `isOpen`(=`currentValue`)만 보면
  **여는 애니메이션 동안** 가드가 뚫려 종료 안내가 먹는다 — 그 구간엔 Material3 핸들러도 꺼져 있다.
  그리고 `drawerOpening` 구간은 우리가 받아 닫아야 한다. 비활성만 시키면 시스템 back 으로 넘어가
  앱이 꺼진다.
  드로어는 `ModalDrawerSheet(drawerState = …)` 오버로드를 써야 핸들러가 붙는다.
- **삼킴 핸들러는 `NavDisplay` 호출 뒤에 둔다** — `BackHandler(enabled = !uiState.isNavSettled && !drawerState.isOpen)`.
  앞에 두면 `NavDisplay` 가 먼저 받아 애니메이션을 커밋해 버린다. 드로어 조건을 빼는 이유는
  드로어 닫기가 언제나 살아 있어야 하기 때문이다. 이 핸들러가 앱종료 핸들러보다 뒤라서
  **리더가 닫히는 중에 종료 안내가 뜨던 문제**도 같이 막힌다.

> 씬 키 고정·예측 뒤로가기 등 **내비 자체의 주의점은 `:core:navigation/CLAUDE.md`** 에 있다.

## 반응형 / 인셋
- 폼팩터 판정은 `LocalWindowInfo.current.containerDpSize` — **너비·높이 둘 다**.
- 홈 그리드는 `GridCells.Adaptive(minSize = adaptive.cardMinWidth)`. **고정 열 수 금지.**
- 플로팅 내비 기준 컨테이너는 **헤더 아래 콘텐츠 영역**. Compact 는 Scaffold topBar 가 밀어 주지만,
  넓은 창은 헤더가 씬 안에 있어 `headerOffset` 을 직접 빼 준다.
- 인셋은 한 군데서만: `TopAppBar`/`ModalDrawerSheet` 가 자기 몫을 처리한다.
  `ListPanelScreen(applyStatusInset = true)` 는 **고정 패널일 때만** — 드로어에선 두 번 밀린다.
- `Scaffold(contentWindowInsets = WindowInsets(0))` + 하단 여백은 `contentBottomPadding` 으로 명시 전달.

## 그 외
- 데이터 DTO/도메인 → UI 모델 매핑은 **여기서** 단방향. 도메인 모델이 Composable 까지 오지 않는다.
- 화면 전용 Composable 만 — 재사용 위젯은 design-system 으로(공유 요소는 `Modifier` 슬롯으로 주입).
- 복구 안내는 `SnackbarHostState`. **Toast 금지.** 빈/에러는 전용 상태 Composable.
