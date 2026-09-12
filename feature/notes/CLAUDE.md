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

**백스택의 소유자는 `activeNoteId` 다.** nav3 가 상태를 들지 않는다 —
`notesBackStackOf(activeNoteId)` 로 파생하고, `onBack` 은 `NotesIntent.CloseNote` 를 쏜다.
`rememberNavBackStack` 을 쓰지 않는 이유가 이것이다(상태가 두 군데가 되면 MVI 가 깨진다).

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
- **뒤로가기 우선순위**(LIFO): 드로어 → `NavDisplay` → 앱 종료(1초 내 두 번).
  드로어는 `ModalDrawerSheet(drawerState = …)` 오버로드를 써야 핸들러가 붙는다.

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
