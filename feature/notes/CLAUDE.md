# :feature:notes — Claude Code Instructions

3화면(Home·Reader·ListPanel) + MVI ViewModel + Nav3 그래프. feature convention 이 `:core:design-system`
+ `:core:domain` + lifecycle + coroutines 를 자동 주입. **`:core:data` 를 직접 의존하지 않는다**(UseCase 경유,
도메인 모델은 domain 의 `api(:core:data)`… 가 아니라 도메인 모델로 transitive). feature ↔ feature 의존 금지.

## MVI (필수)
- 상태: `NotesUiState` — 단일 소스. 컬렉션은 `ImmutableList`/`ImmutableMap`. `Category`(내부 `List<Note>`)를
  품으므로 **`@Stable`**, `RecentNote` 같이 deeply 불변인 것은 **`@Immutable`**.
- 인텐트: `sealed interface NotesIntent`(UI → ViewModel 단방향).
- ViewModel: `@HiltViewModel`, 단일 진입점 `onIntent(intent)`. 내부 `MutableStateFlow` 들을 `combine` →
  `stateIn` 으로 `uiState` 노출. startup 동기화는 `init { sync() }`(SyncNotesUseCase) 1회.
- 화면 Composable 은 콜백을 `onIntent: (NotesIntent) -> Unit` **하나만** 받는다.

## Route / Screen 분리 (필수)
- **`XxxRoute`** = `hiltViewModel()` 주입 + `collectAsStateWithLifecycle` 만. `XxxScreen` 호출만.
- **`XxxScreen`** = stateless. `uiState` + `onIntent`(+ 필요한 host state)만. **테마 블록은 Screen 에서 시작.**
- **Screen 에는 항상 `@Preview`** — 더미 `uiState` + `onIntent = {}`, 폼팩터별 `@Preview(widthDp=…)` 다수.

## 그 외
- 데이터 DTO/도메인 → UI 모델(`NoteContentBlock`) 매핑은 **여기서** 단방향(`NoteBlockMapper`).
- 화면 전용 Composable 만 — 재사용 위젯은 design-system 으로.
- 폼팩터 판정은 `LocalWindowInfo.current.containerDpSize`(이미 Dp). `BoxWithConstraints` 로 윈도우 크기 재지 않음.
- 내비/레이아웃: 폼팩터를 `containerDpSize` 로 판정해 **목록 패널 표현만** 달리한다(콘텐츠는 `activeNote` 상태가 구동).
  Compact 는 `ModalNavigationDrawer`(화면 위 오버레이), Medium+ 는 `Row` + `AnimatedVisibility` 접이식 사이드
  패널(옆에서 미는 push). **둘 다 ☰ 토글이 살아 있다.** (Nav3 `NavDisplay`/`ListDetailSceneStrategy` 미사용 —
  단일 상태 기반으로 통일.)
- 인셋은 `Scaffold`/`TopAppBar`/`ModalDrawerSheet` 가 처리 — 수동 `statusBarsPadding` 추가 금지.
- 복구 안내는 `SnackbarHostState`(Scaffold `snackbarHost`). **Toast 금지.** 빈/에러는 전용 상태 Composable.
