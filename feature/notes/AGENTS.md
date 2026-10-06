# AGENTS.md — :feature:notes

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

앱의 UI 는 전부 여기 있다. `MainActivity` 는 `NotesRoute()` 하나만 부르고, 그 아래에
ViewModel 하나(+ 통계 `StatsViewModel`) · `NavDisplay` 하나 · `MyNoteTheme` 하나가 있고 그 안에서 화면 11개
(Home · Bookmarks · Stats · StatsHistory · ListPanel · NoteReader · Settings · 퀴즈 4개)가 돈다. "새 화면을 추가한다" 는 대개 새 모듈이
아니라 이 구조 안에 끼우는 일이다.

의존은 컨벤션(`hancenote.android.feature`)이 `:core:common` · `:core:design-system` ·
`:core:domain` · `:core:navigation` + Hilt + lifecycle + coroutines 를 자동으로 넣는다.
`build.gradle.kts` 에 직접 적은 건 `hilt-navigation-compose` 와 `material-icons-core` 둘뿐이다.
`:core:data`/`:core:network`/`:core:database` 는 의존하지 않는다 — 데이터는 `:core:domain` 의
UseCase 와 `QuizGenerator` 인터페이스로만 들어온다.

---

## 상태가 사는 곳이 셋이다 — 이 경계가 이 모듈의 골격이다

| 어디 | 무엇 | 왜 거기 |
|---|---|---|
| `NotesViewModel` | 화면이 그리는 거의 전부 + 퀴즈 진행 + 스낵바 호스트 | 재조립형 MVI 단일 상태 |
| `Navigator`(`:core:navigation`, `@Singleton`) | **백스택** | 소유자가 하나여야 하고, 입구가 하나라 거기서 쓰로틀을 건다 |
| 컴포지션 로컬(`rememberSaveable`) | 드로어 열림 · 고정 패널 펼침 · 모델 확인 다이얼로그 | 목적지가 아니고 레이아웃 상태다. 백스택 엔트리가 아니다 |
| `NotesDrafts`(`NotesScreen` 의 `remember`, 셸 분기 위) | **토큰 입력 draft · 보기 여부**(연결 화면 · 설정 편집기 각각) | 저장하지 않는다 — 상태 복원 번들에 원문이 안 들어가고(결정 16), 접기·펴기·회전에는 셸 분기가 바뀌어도 남는다. `toString` 에 원문이 없다(`TokenDraft`) |
| `ConnectViewModel`(Activity 스코프) | 연결 화면 흐름(확인 중 · 토큰 갈래 · 첫 동기화 · 확인 대화상자). 링크 draft 만 `SavedStateHandle` | 루트와 쌓은 화면이 같은 상태를 쓴다 |

플로팅 내비의 선택은 이 셋 어디에도 없다 — `NotesUiState.selectedTab`(`Reader`·`Settings` 를 건너뛴 가장 위의
`Home`/`Bookmarks`/`Stats`)으로 **백스택에서 파생**한다. 선택 상태를 따로 두면 화면과 어긋난다.

통계 화면만 예외로 ViewModel 이 하나 더 있다 — `stats/StatsViewModel`. **Activity 스코프를 명시해서**
(`hiltViewModel(viewModelStoreOwner = activity)`) 얻고, 기간·섹션은 `SavedStateHandle` 에 둔다. 집계 Flow 는
`navigator.stack` 의 top 이 `Stats` 일 때만 `flatMapLatest` 로 붙고 아니면 `emptyFlow()` 라 리더에 가 있는 동안
Room 쿼리가 돌지 않는다(debug 에서 `MyNoteStats` 로그로 확인). `SharingStarted.Lazily` 로 바꾸면 한 번 연 뒤
앱이 끝날 때까지 집계가 돈다 — 쓰지 말 것. 리더·퀴즈 진입은 `NotesIntent`(`SelectNote(id, resumeAt)`·
`OpenQuizFor`)로 넘긴다.

세 번째를 ViewModel 로 올리려는 충동을 참아라. 드로어/패널은 창 크기에 따라 존재 자체가
사라지는 값이라 상태로 올리면 폼팩터 전환 때 의미가 없어진다. 대신 **뒤로가기는 먹는다**
(아래 참조).

---

## MVI — `build()` 하나가 상태를 통째로 만든다

`NotesViewModel` 은 `:core:common` 의 `MviViewModel<NotesUiState, NotesIntent>` 를 상속한다.
`trigger(initial)` / `Flow.triggerIn(initial)` 로 등록한 값이 하나라도 바뀌면 `build()` 가
다시 돌아 `NotesUiState` 를 새로 만든다. 등록 기준은 하나 — **`build()` 가 그 값을 읽는가.**

- `onIntent()` 는 상태를 만들지 않는다. trigger 값만 바꾼다.
- 파생 계산(`recentNotes` 정렬, `visibleCategories` 검색 필터)은 `build()` 안이 아니라
  `map`/`combine` + `triggerIn` 으로 빼 둔다. 그래야 재료가 바뀔 때만 계산된다.
- **일회성 신호는 값이 아니라 증가하는 id 로 나간다** — `errorId`/`infoId`/`exitHintId`/
  `exitRequestId`/`quizUndoId`. `StateFlow` 는 conflate 하므로 "횟수"나 "이벤트"를 상태로
  흘리면 한 프레임에 묶여 조용히 씹힌다(0→1→2 가 2 만 보인다). 화면은 `LaunchedEffect(id)`
  로 받아 실행만 한다.
- `SnackbarHostState` 인스턴스는 ViewModel 이 들고 `build()` 는 넘기기만 한다.
- 문구는 ViewModel 이 `getString` 을 직접 부르지 않는다. `NotesMessages`(`@ApplicationContext`)
  가 만들고 VM 은 호출한다. 접근성 판정도 같은 패턴 — `QuizAutoAdvancePolicy`.

---

## 백스택 — `Navigator` 가 소유하고 ViewModel 은 읽어 나른다

`navigator.stack`/`isSettled` 를 `triggerIn` 하고, `backStack`·`activeNoteId`·`canPop`·
`isNavSettled` 는 전부 거기서 파생된다. `rememberNavBackStack` 도, VM 의 `activeNoteId` trigger 도
쓰지 않는다 — 상태가 두 군데가 되면 깨진다.

- 이동은 `navigator.push`/`pop` 으로만. 둘 다 `dispatch` 를 타므로 전환이 끝나기 전에 들어온
  이동은 **버려진다**(`NavResult.Throttled`).
- **`Back` 과 `SwitchTab` 은 다르다.** `Back` 은 pop 하나이고 더 갈 데가 없으면 종료 안내/종료로
  이어진다. 플로팅 내비의 `SwitchTab(Home)` 은 `ReplaceAll([Home])`, `SwitchTab(Bookmarks)` 는
  `[Home]` 에서 `Push(Bookmarks)` / 그 외 `ReplaceAll([Home, Bookmarks])` 이고(`navigation/TabSwitch.kt`),
  이미 그 자리면 `Ignored` 로 아무 일도 없어야 한다. 종료 판단은 화면이 아니라 `pop() == NavResult.Ignored` 가 내린다.
- 두 번 누르기(`EXIT_WINDOW_MILLIS`)는 ViewModel 이 `SystemClock` 으로 센다. 상태로 나가는 건
  `exitHintId`/`exitRequestId` 뿐이고 `finish()` 는 화면이 부른다.
- 사용자 입력이 아닌 정정은 게이트를 우회한다 — 동기화 중 열린 노트가 삭제되면
  `dispatchImmediately(NavCommand.Pop)`.

---

## 뒤로가기 — **컴포즈 순서가 곧 우선순위다**(LIFO, 나중에 컴포즈된 게 이긴다)

파일을 옮기거나 쪼갤 때 가장 쉽게 깨지는 부분이다. 지금 순서는 아래와 같고, 위쪽이 먼저 먹는다.

| 순위 | 핸들러 | 어디서 컴포즈되나 |
|---|---|---|
| 1 | 드로어 닫기 (Material3) | `ModalDrawerSheet(drawerState = …)` — `ModalNavigationDrawer` 가 `content()` **뒤에** 드로어를 컴포즈하므로 자동으로 제일 늦다 |
| 2 | 퀴즈 오버레이 닫기 | `QuizOverlay` (= `NotesOverlays`, NavDisplay 뒤) |
| 3 | 전환 중 삼킴 | `NotesShell` 의 `BackHandler(enabled = !isNavSettled && !drawerEngaged) {}` |
| 4 | `NavDisplay` 의 pop | `NotesNavDisplay` |
| 5 | 여는 중인 드로어 닫기 | `NotesShell` 본문 |
| 6 | 펼친 고정 패널 접기 | `NotesShell` 본문 |
| 7 | 앱 종료 안내 → 종료 | `NotesShell` 본문 (`enabled = !canPop && !drawerEngaged && !collapsiblePanelOpen`) |

각각 그 자리에 있어야 하는 이유:

- **삼킴은 `NavDisplay` 호출 뒤에 둔다.** `NavDisplay` 는 예측 뒤로가기 애니메이션을 **이미 커밋한
  뒤** `onBack` 을 부른다. 앞에 두고 버리면 화면은 닫혔는데 스택은 그대로라 어긋난다.
  `activity-compose` 의 `BackHandler` 는 `NavigationEventDispatcher` 가 있으면 거기 등록되므로
  nav3 핸들러와 같은 LIFO 를 탄다.
- **드로어 판정은 `isOpen` 이 아니라 `drawerEngaged = isOpen || targetValue == Open` 이다.**
  `isOpen` 은 `currentValue` 이고 그 값은 `AnchoredDraggable` 이 애니메이션이 끝난 뒤에 넣는다.
  여는 동안은 `false` 인데 Material3 의 핸들러도 `enabled = drawerState.isOpen` 이라 같이 꺼져 있다 —
  그 틈으로 뒤로가기가 종료 안내로 샌다.
- **여는 중(`drawerOpening`)은 우리가 받아 닫는다.** 비활성만 시키면 그 구간에 아무도 받지 않아
  시스템으로 넘어가 앱이 그냥 꺼진다.
- **펼친 고정 패널도 뒤로가기를 먹는다.** 백스택 엔트리가 아니지만 Compact 의 드로어와 대칭이다.
  빠뜨리면 홈에서 패널을 펴 둔 채 뒤로가기 했을 때 종료 안내가 먼저 먹는다.

---

## `shell/` — 호스트를 일 단위로 쪼갠 것

`NotesScreen` → `NotesShell` 하나만 부르고 그 안에서 조립된다. 한 파일이 한 가지만 한다:
드로어·패널 상태와 뒤로가기 순서(`NotesShell`) / 인셋 산수(`NotesLayout`) / 스크롤과 "맨 위로"
판정(`NotesScrollState`) / 씬 슬롯과 전략(`NotesScenes`) / `NavDisplay`(`NotesNavDisplay`) /
플로팅 내비·맨 위로·퀴즈·모델 알약(`NotesOverlays`·`ModelPill`) / 크롬(`NotesTopBar`).

쪼개거나 옮길 때 깨지는 것:

- **`layerBackdrop` Box 안에는 `NotesNavDisplay` 만.** 그 Box 가 유리의 샘플링 대상이므로
  오버레이(내비·맨 위로·퀴즈)가 안에 들어가면 유리가 자기를 샘플링한다. 그래서 `NotesOverlays` 는
  그 Box 의 **형제**다. 반대로 `layerBackdrop` 을 `SharedTransitionLayout` 에 직접 걸면 레이어가
  비어 효과가 통째로 사라진다.
- **씬 슬롯(`panel`/`header`/`panelExpanded`)은 `remember` 로 정체성을 고정한다.** 매 컴포지션
  새 람다가 들어가면 `sceneStrategies` 가 달라진 것으로 보여 씬이 통째로 재생성된다. 안의 값은
  `rememberUpdatedState` 로 읽는다(`rememberNotesPanelSlot`/`rememberNotesHeaderSlot` 참조).
  manifest 에 `configChanges` 가 켜져 있어 Activity 가 재생성되지 않으므로 낡은 캡처는 실제 버그다.
- `nestedScroll(navScroll.nestedScrollConnection)` 은 `layerBackdrop` Box **바깥**의 Box 에 걸려 있다 —
  홈 그리드든 리더든 스크롤이 플로팅 내비 숨김 판정으로 올라와야 한다. 노트가 바뀌면
  `navScroll.reset()` 으로 다시 보여 준다.
- `NotesScrollState` 의 리더 스크롤은 `key(activeNoteId) { rememberScrollState() }` 다 — 노트마다
  스크롤 위치가 따로다. 리더 본문의 원문 펼침 상태(`readerContent` = design-system `NoteContentState`)도 같은 자리·같은 키다.
  **창 등급이 바뀌면(접기·펴기·회전) 셸이 `if (useDrawer) ModalNavigationDrawer(content = scaffold) else scaffold()` 의 다른
  가지로 옮겨 `NavDisplay` 가 통째로 다시 만들어진다** — 항목 안의 `remember`/`rememberSaveable` 은 키를 명시해도 그때 사라진다.
  창 등급을 넘어 남아야 하는 리더 상태는 여기(셸, 그 분기 위)에 둔다(관문 2 S1).

---

## 폼팩터 — 분기하는 곳은 딱 두 군데다

판정은 `NotesScreen` 이 `containerDpSize` 로 **너비와 높이 둘 다** 보고 `MyNoteTheme` 에 넣는다.
그 뒤 화면 코드는 분기하지 않는다. 갈리는 건:

1. `sceneStrategies` — Compact(`useModalDrawer`)면 **빈 리스트**라 기본 `SinglePaneSceneStrategy` 로
   떨어져 홈/리더가 별개 씬 레이어로 쌓인다. Medium+ 면 `ListDetailSceneStrategy` 로 고정 패널 +
   상세 한 칸이 되고 **헤더도 씬 안**(상세 칸 위)으로 들어간다. 리스트에서 넣고 빼는 것만으로
   레이아웃이 갈린다.
2. 목록을 `ModalNavigationDrawer` 로 감쌀지(`useDrawer`) 여부.

인셋은 이 갈림 때문에 두 번 밀리기 쉽다:

- `ListPanelScreen(applyStatusInset = true)` 는 **고정 패널일 때만**. 드로어에서는
  `ModalDrawerSheet` 가 이미 처리한다.
- 넓은 창은 헤더가 씬 안이라 `Scaffold` 의 topBar 가 밀어 주지 않는다 — 플로팅 레일은
  `NotesLayout.headerOffset`(= `headerHeight + statusBar`)을 직접 뺀다. Compact 는 0 이다.
- `Scaffold(contentWindowInsets = WindowInsets(0))` 이므로 하단 여백은 `contentBottomPadding` 으로
  명시 전달한다(하단 바가 뜨는 형태면 `FLOATING_BAR_RESERVE` 를 더한다). 레일이 뜨는 형태면 오른쪽도
  `contentEndPadding`(`FloatingNavDefaults.RailWidth` + 간격 − `page`)으로 예약해 홈 그리드와 북마크 화면이
  레일 왼쪽에서 끝나게 한다 — 북마크 행의 해제 버튼이 레일 아래 깔리면 누를 수 없다.
- 홈 그리드는 `GridCells.Adaptive(minSize = adaptive.cardMinWidth)`. 고정 열 수 금지.
- 북마크 화면의 2단 여부(`twoPane`)는 화면이 `BoxWithConstraints` 로 재지 않고 **셸 `NotesLayout` 이
  계산**한다 — `창 폭 − 펴진 고정 패널 − page − contentEndPadding − page ≥ 640dp`(`bookmarksTwoPane`).
  패널을 접으면 2단으로 바뀌는 이유가 이것이다. 가로 폰의 큰 카드 축소는 단 수가 아니라 `heightClass == Compact` 다.

북마크 화면(`screen/bookmarks/`)은 홈형 그리드가 아니다 — `bookmarks[0]` 하나가 큰 카드(`FeaturedBookmarkCard`),
나머지가 `02` 부터 번호 행(`BookmarkRow`)이다. `LazyColumn` 의 큰 카드 키는 `"featured"` 로 **고정**이라 맨 앞을 해제하면
카드가 움직이지 않고 내용만 `Crossfade` 로 다음 노트로 바뀐다(승격). 큰 카드의 발췌 `NoteUi.excerpt` 는 저장하지 않고
`:core:domain` 의 `Note.firstSentence()` 로 `NotesUiMapper` 가 파생한다. 홈 카드의 북마크 표기는 design-system
`NoteCard(bookmarked = …)` 의 모서리 리본이고, 목록 패널 행만 12dp 글리프를 유지한다.

플로팅 내비의 3칸이 전부 `SwitchTab` 으로 배선돼 있다. 통계는 북마크와 같은 규칙이다 — `[Home]` 에서 push,
그 밖에서 `ReplaceAll([Home, Stats])`, 이미 top 이면 `Ignored`(`navigation/TabSwitch.kt`, `TabSwitchTest`).
통계 화면(`screen/stats/`)의 2단 여부도 북마크와 같은 `NotesLayout.contentTwoPane`(콘텐츠 폭 ≥ 640dp)이고,
가로 막대·낮은 차트 같은 축소 변형은 `heightClass == Compact` 로 갈린다. 2단 오른쪽 칸 항목 키는 배치마다
다르게 둔다(`side-*`) — 같은 키를 쓰면 `LazyListState` 가 키를 따라가 폴드 전환 때 오른쪽 칸이 아래로 밀린다.

### 요약 카드 · 지난 요약 · 패턴 (통계 2·3단계)

- 카드·패턴의 보임 규칙은 순수 함수 `stats/SummaryCardRules.kt`(`summaryCardOf`·`patternOf`) 와 `stats/GeneratedAgo` 에 있고
  `SummaryCardRulesTest`·`GeneratedAgoTest` 가 지킨다. Composable 안에서 `latest`/`lastFailureAt` 을 다시 비교하지 말 것.
- **카드는 별도 LazyColumn 항목이 아니라 `"segment"` 항목 안(기록 꺼짐 안내 아래, 세그먼트 위)에 있다.** 워커가 끝나 카드가
  나타날 때 별도 항목이면 목록이 첫 보이는 키(`segment`)를 붙잡아 카드가 화면 위로 숨는다(8.2 #64 와 같은 원인).
- 인사이트 Flow 는 `Stats` 가 top 일 때만, 지난 요약 목록은 `StatsHistory` 가 top 일 때만 붙는다(집계와 같은 게이트).
- `StatsHistory` 는 백스택 엔트리라 뒤로가기는 `NavDisplay` pop(5순위)이 받는다. 상단 바는 `←` + `지난 요약`,
  `NotesTopBar(showRefresh = false)`, 플로팅 내비는 그대로 보인다. 행 펼침은 화면 로컬 `rememberSaveable`(키 `weekStart`).
- 초점 표시는 `SummaryCardUi.focusInLegend`(이번 주 기간 && 이번 주 요약 && 오래되지 않음)일 때만 범례에 붙는다.

### 학습 기록 배선 (통계 0단계)

`NotesViewModel` 이 `:core:domain` 의 `ReadSessionTracker` 를 들고 네 입력을 먹인다 — `init` 의 수집기 둘
(스택 top 의 `activeNoteId`, 퀴즈 `coversReader`)과 인텐트 둘(`ForegroundChanged` = `NotesRoute` 의 라이프사이클
옵저버가 `ON_START`/`ON_STOP` 에 보냄, `ReaderProgress` = 셸 `ReaderScrollEffects.kt` 가 **지금 본 비율이 바뀔
때마다** 보냄). **세션 안 최댓값은 트래커만 든다** — 셸이 최댓값을 들면 `ON_STOP`→`ON_START` 로 열린 새 세션이
0% 로 저장된다(셸 효과는 다시 시작되지 않아 같은 비율을 다시 보내지 않는다). 그래서 트래커가 top 노트의 마지막
비율을 기억해 새 세션을 거기서 시작한다(PRD D39, 검수 blocker #1). 닫힌 세션·퀴즈 결과·북마크 이벤트는 `launchSafely { withContext(NonCancellable) { … } }` 로 쓴다 —
`ON_STOP` 직후 스코프가 취소돼도 한 행은 끝까지 간다. 퀴즈 결과 → `QuizAttempt` 변환은 순수 함수
`quiz/QuizAttemptRecord.kt` 의 `completedAttempt()` 이고 `QuizAttemptRecordTest` 가 "결과까지 간 것만" 을 지킨다.
북마크 화면 해제의 되돌리기는 `added` 를 새로 쓰지 않고 그 해제 이벤트 행을 지운다(`removedEventId`).

---

## 퀴즈 — 내비게이션 목적지가 아니다

진입점은 헤더가 아니라 리더 본문 **맨 끝 CTA**(`QuizCtaCard`)다. 모델이 없어도 숨기지 않고
흐리게 두고, 누르면 왜 안 되는지 스낵바로 말해 준다(`NotesShell` 의 `onOpenQuiz`).

**예외 하나 — 노트 전체를 읽지 못한 노트(`NoteBlock.Unsupported` 의 `BrokenNote` · `LimitExceeded` 가 있는 노트)는 CTA 를
아예 그리지 않는다.** 퀴즈에 넣을 평문이 없어서다. 판정은 `NotesUiMapper` 가 파생하는 `NoteUi.readable` 하나이고(`NotesUiMapperTest`),
통계의 "다시 풀기"(`OpenQuizFor`)도 같은 값으로 리더만 열고 퀴즈 설정을 띄우지 않는다(띄우면 `GenerateQuizUseCase` 가
빈 평문으로 `QuizFailure.Empty`). 블록 몇 개만 못 그린 노트(`UnknownType`·`BrokenBlock`)는 평소대로다. 그 블록은
`NoteBlockMapper` 가 빈 `text`·`raw` 를 `null` 로 바꿔 design-system `NoteContentBlock.Unsupported` 로 넘긴다(`NoteBlockMapperTest`).

`QuizUiState`(sealed)는 `NotesUiState.quiz` 한 칸에 들어가고, `QuizOverlay` 가 리더 **위에 덮인다**.
`Navigator` 를 건드리지 않으므로 백스택에 없고, 뒤로가기는 오버레이가 한 번 받아 `DismissQuiz` 를 쏜다.

- `coversReader` 인 상태(Loading/Solving/Result/Failed)는 `matchParentSize()` 로 콘텐츠 영역을 덮고,
  Compact 에서는 `Scaffold` 의 topBar 도 같이 숨긴다. 넓은 창에서 패널이 펴져 있으면
  `startInset = paneWidth + 1.dp` 로 **목록 패널은 남겨 둔다.**
- **채점·진행 규칙은 `QuizUiState.Solving` 의 순수 함수** `pick()`/`advance()` 다. 아무 일도 없을
  때는 `this` 를 그대로 돌려줘서 ViewModel 이 "변화 없음"을 `===` 로 알아본다. 이 규칙이 상태로
  내려와 있는 덕에 `QuizGradingTest` 가 코루틴도 화면도 없이 검증한다(처음엔 고른 답이
  `rememberSaveable` 에 있어서 고르지 않은 문제에 "오답" 이 뜨는 버그가 있었다).
- **자동 진행 타이머는 ViewModel 에 있다.** 화면의 `LaunchedEffect` 에 두면 중복 발행되고, 늦게 온
  타이머가 문항을 건너뛴다 — 그래서 `NextQuestion(fromIndex)` 로 "내가 띄운 타이머가 아직 그 문항을
  보고 있나" 를 확인한다. TalkBack(터치 탐색)이면 `QuizAutoAdvancePolicy` 가 자동 진행을 끄고
  화면이 버튼을 대신 띄운다.
- **생성 중 연출 타이머는 반대로 화면(`QuizGeneratingScreen`)에 있다.** 진행률을 알 수 없는 연출이라
  1초에 한 번씩 앱 상태를 재조립할 이유가 없다. 대신 마지막 슬롯은 끝까지 채우지 않는다 —
  거짓 진행률이 된다.
- `AUTO_ADVANCE_MILLIS` 는 `QuizUiState` 에만 있다. 타이머(VM)와 링(화면)이 같은 값을 봐야 한다 —
  예전에 양쪽에 복사돼 있어 링과 실제 전환이 어긋났다.
- 그만두기는 **되돌릴 수 있다.** VM 이 `abandonedSolving` 을 `UNDO_WINDOW_MILLIS` 동안 들고,
  `quizUndoId` 로 스낵바 액션을 띄운다. 생성은 `quizJob` 으로 중복 실행을 막고
  `GENERATE_TIMEOUT_MILLIS` 로 끊어 `Kind.Timeout` 으로 떨어뜨린다.
- 선택지 행은 `key(state.index, index)` 로 감싼다. 없으면 행이 슬롯째 재사용되고 안의
  `animateColorAsState` 가 이전 문제의 정답/오답 색에서 중립색으로 애니메이션해 한 번 번쩍인다.
- 무한 애니메이션은 전부 `rememberReduceMotion()` 을 **실제로 반영**해야 한다(판정만 하고 버리면
  접근성 설정이 무시되고 프리뷰에서도 계속 돈다).

---

## 패키지 배치가 뜻하는 것

- `quiz/screen/` 은 화면(한 화면을 다 차지하고 `Surface` 로 배경을 갖는다), `quiz/component/` 는
  조각이다. `QuizSolveScreen` 은 화면이고 `QuizOrb` 는 아니다.
- `element/`(상태·인텐트) · `mapper/`(도메인 → UI 단방향) · `navigation/`(노트 전용 공유 요소 키) ·
  `shell/`(호스트) · `screen/`(본 화면 3개) · 루트(진입점·UI 모델·VM·문구).
- 같은 패키지라 공짜로 보이던 참조가 import 로 드러나는 게 이 배치의 실익이다. 새 파일을 넣을 때
  "이게 화면인가 조각인가" 를 먼저 답하고 자리를 정하라.
- **목적지 키·씬·모션·공유요소 배선은 여기 없다 — `:core:navigation` 에 있다.** 여기는 쓰는 쪽이다.
  키(`chip-/date-/title-{noteId}`)만 `rememberNoteSharedModifiers` 가 정한다.
- 재사용 위젯은 여기 만들지 않는다. `:core:design-system` 으로 올리고, 공유 요소가 필요하면
  `Modifier` 슬롯으로 주입받게 한다(`NoteCard(chipModifier=…)`).

---

## 문구 · 프리뷰

- 화면 문구는 `src/main/res/values/strings.xml`(`resourcePrefix = "notes_"`). 코드에 한글 리터럴
  금지 — 예외는 프리뷰 더미와 개발자용 에러 메시지뿐이다. 공통 문구(취소·저장·닫기·되돌리기)는
  design-system 의 `ds_*`.
- 말투는 **합니다체** — 서술 `~합니다`/`~습니다`, 권유·지시 `~하세요`. 다국어는 하지 않는다.
  리소스로 모으는 이유는 번역이 아니라 한 곳에 모으는 것이다.
- **프로덕션 트리에 `MyNoteTheme { }` 는 `NotesRoute.kt` 한 군데뿐이다.** 그래서 안쪽 화면들은
  테마를 감싸지 않고, 대신 **각 화면의 `@Preview` 가 스스로 `MyNoteTheme` 을 두르고 폼팩터를
  넘긴다**(`windowClass`/`heightClass`). 화면을 추가하면 프리뷰도 같은 방식으로 붙인다.
- 화면 단위는 `Surface(color = …)` 로 배경을 갖는다. `Surface` 는 `propagateMinConstraints = true`
  라 자식에 `fillMaxSize` 를 또 붙일 필요가 없다.

---

## 명령

```bash
./gradlew :feature:notes:testDebugUnitTest   # QuizGradingTest (순수 JVM, 13건)
./gradlew :app:compileDebugKotlin            # 이 모듈 변경 검증은 :app 컴파일로
```

---

## 설정 화면과 퀴즈 모델 상태

### 토큰 원문은 ViewModel 밖으로 나가지 않는다

`NotesUiState` 에 있는 건 `maskedToken`(`ghp_••••••••a3F9`)뿐이다. 입력 중인 draft 와 보기/숨기기는
`NotesDrafts`(저장 안 되는 홀더)다. 비밀값을 상태로 올리면 로그·스크린샷 테스트·
`toString()` 에 흘러나온다. `maskToken()` 은 **길이도 드러내지 않는다** — 원본이 20자든 200자든
가린 결과가 같다.

되돌리기용 이전 토큰은 `NotesViewModel` 의 **메모리 필드**다. 디스크에 백업을 남기지 않는다.

### `ModelStatus` 는 `:core:domain` 에 있다 — `:core:ai` 가 아니다

받는 쪽 구현은 `:core:ai` 인데 **feature 는 `:core:ai` 를 의존하지 않는다**(feature 컨벤션이
넣어 주는 건 core/{common, design-system, domain, navigation} 뿐). 그래서 상태 타입과
`QuizModelSource` 계약은 `:core:domain` 이 소유하고 `:core:ai` 의 `ModelDownloads` 가 구현한다.
`QuizGenerator` 와 같은 DIP 다. **`:core:ai` 에 public 을 만들어도 여기서는 영원히 안 보인다.**

화면이 쓰는 입구는 UseCase 다 — `ObserveModelStatusUseCase` · `StartModelDownloadUseCase` ·
`CancelModelDownloadUseCase` · `DiscardPartialModelUseCase` · `DeleteModelUseCase`.

### 실패 이유 문자열은 `ModelFailure` 하나에서만 온다

`ModelStatus.Failed(reason)` 의 `reason` 은 WorkManager output data 를 타고 오는 **문자열**이라
타입이 지켜 주지 않는다. `:core:domain` 의 `ModelFailure.NO_SPACE` 를 양쪽이 같이 본다.
UI 에 `"no-space"` 를 다시 적으면 값이 어긋나는 순간 **공간 부족인데 "잠시 뒤 다시 시도해 주세요"
로 조용히 떨어진다.** 실제로 한 번 그렇게 만들었다가 고쳤다.

### `isQuizAvailable` 은 더 이상 한 번 읽고 마는 값이 아니다

예전엔 `flow { emit(quizGenerator.isAvailable()) }` 로 앱 시작에 한 번만 읽었다. 그래서 다 받아도
앱을 다시 켜야 퀴즈가 켜졌다. 지금은 `modelStatus == Ready` 에서 파생되고, `ModelDownloads.status()`
가 WorkManager 를 계속 흘린다.

### 상태 → 화면 매핑은 순수 함수다

`element/ModelStatusUi.kt` 의 `modelCardUi()`(9개 상태 → 점·라벨·막대·버튼·알림 꺼짐 안내 칸), `modelPillUi()`(상태 → 알약 라벨·톤·
보조 %·링)와 `quizCtaUi()`(상태 → CTA 부제·칩·동작)는 `@StringRes` id 를 돌려준다. Context 없이 단위 테스트가 된다.
Composable 안에서 `when (status)` 를 새로 쓰지 말고 여기에 넣어라.

---

## 사이드 시트 — `LazyColumn` 은 반드시 `weight(1f)` 박스 안에

```
Column(fillMaxSize) {
    Header                       // 고정
    Box(Modifier.weight(1f)) { LazyColumn(...) }   // ← 여기서만 늘어난다
    SettingsRow(...)             // 고정
}
```

`weight` 를 빼면 카테고리가 많을 때 **설정 줄이 화면 밖으로 밀려난다.** 목록과 설정 줄 사이에
선을 넣지 않는다 — 대신 아래에 더 있을 때만 하단 페이드가 켜진다.

페이드는 새로 만들지 않고 design-system 의 `Modifier.fadingEdges(top, bottom)` 에
`animateDpAsState` 로 0↔16/52dp 를 먹인다. `listState.canScrollBackward/Forward` 가 조건이다.

## 상단 알약은 콘텐츠를 가리지 않는다

`ModelPill` 이 보이는 동안 홈·리더의 스크롤 `contentTopPadding` 이 +54dp 된다(300ms).
`NotesShell` 이 `rememberModelPillVisible()` 을 한 번 계산해 **오버레이와 패딩 양쪽에 같은 값**을
넘긴다. 두 군데서 따로 계산하면 알약이 뜨는데 패딩이 안 밀리는 순간이 생긴다.

---

## 저장소 연결 — 시작 분기 · 연결 화면 · 설정 카드

- **시작 분기는 `NotesScreen` 이 한다.** `NotesUiState.start`(순수 함수 `element/ConnectStart.kt:startDestination`)가 읽는 중 / 연결 루트 / `NotesShell` 을 가른다.
  연결 루트는 셸 밖이라 `NotesExitEffects`(셸 파일의 `internal`)와 스낵바 · 인셋을 직접 갖는다. 연결을 읽기 전(`connection == null`) · 노트를 읽기 전(`notesLoaded`) · 시드 소비 전(`seedPending`)에는 연결 화면도 홈도 그리지 않는다.
- **화면 정리는 연결 상태 전이가 한다.** `NotesViewModel.onConnectionChanged` 가 (이전 → 다음 연결) 을 `element/ScreenCleanup.kt:screenCleanupOf` 에 넘겨 스택 `[Home]` · 퀴즈 닫기를 정한다. 시드 · 확정 · 끊기 어느 길이든 한 곳이고, 화면이 따로 discard 를 부르지 않는다.
- **연결 화면 자리는 둘**: 첫 실행 루트(`ConnectPlace.Root`) · 쌓은 화면(`ConnectPlace.Stacked`, `NotesNavKey.Connect` entry + 자체 `NotesTopBar`). 셸은 `Connect` 가 top 이면 topBar · 플로팅 내비 · 알약 · 맨 위로 · 드로어 제스처를 숨긴다.
  뒤로가기는 엔트리 안 `BackHandler`(NavDisplay 뒤에 컴포즈되어 이긴다) — 판정은 순수 함수 `connectBackAction`.
- **판정 로직은 순수 함수**(`element/`)다: `startDestination` · `screenCleanupOf` · `connectBackAction` · `repoCardUi`(RC-13) · `reduceTokenEdit`(RC-18 전이표 + D37 · D39) · `homeBodyOf`(RC-21) · `issueOfProbe`/`issueOfSync` · `linkCheckOf` · `confirmUiOf`. 시험 이름에 항목 ID.
- **입구 구역(RC-25) · 목록 없음 상자의 길(RC-9)** 은 `TemplateRepo?`(앱이 제공, `ConnectViewModel` 생성자 → `ConnectUiState.template`)가 있을 때만 그려진다. 보임·위계·두 단·길 판정은 순수 함수 `element/ConnectEntry.kt`(`entryVisible` · `entryEmphasis` · `twoColumnsOf` · `noticeWays`)이고, 확인 대화상자는 입력이 아니다.
  링크 구역(안내·칸·알림·토큰·`연결`)은 `movableContentOf` 한 벌이라 가로 두 단 ↔ 한 단에서 칸이 새로 만들어지지 않는다 — **이 구역을 `Row`/`Column` 에 두 번 쓰지 말 것**(포커스·키보드가 날아간다). 입구 버튼은 VM 을 거치지 않고 `openExternal` 로 외부 브라우저만 연다.
- 동기화 트리거는 `ObserveSyncRunsUseCase` 다. 연결 직후 동기화는 `announce` 가 꺼져 스낵바가 없고(SY-6), 새로고침에만 반영 알림이 뜬다.
