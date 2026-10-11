# AGENTS.md — :feature:notes

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

셸을 뺀 앱 화면이 여기 있다. `MainActivity` 가 부르는 `NotesRoute()` 와 셸(`NavDisplay` 하나 · `MyNoteTheme` 하나 · 셸 ViewModel)은
`:app` 의 `shell/` 이고, 셸이 이 모듈의 ViewModel 하나(노트)와 등록 파일 하나를 불러
화면 4개(Home · Bookmarks · ListPanel · NoteReader)를 그린다. 통계 · 지난 요약은 `:feature:stats`(`feature/stats/AGENTS.md`), 연결 화면은 `:feature:connect`(`feature/connect/AGENTS.md`), 설정 화면은 `:feature:settings`(`feature/settings/AGENTS.md`), 퀴즈는 `:feature:quiz`(`feature/quiz/AGENTS.md`)에 있다. "새 화면을 추가한다" 는 대개
새 모듈이 아니라 이 구조 안에 끼우는 일이다.

의존은 컨벤션(`hancenote.android.feature`)이 `:core:common` · `:core:design-system` ·
`:core:domain` · `:core:navigation` · `:core:ui` + Hilt + lifecycle + coroutines + `hilt-navigation-compose` ·
`material-icons-core` 를 자동으로 넣는다. `build.gradle.kts` 는 컨벤션 한 줄과 `namespace` 뿐이다.
`:core:data`/`:core:network`/`:core:database` · `:core:ai` · 다른 기능 모듈은 의존하지 않는다 — 데이터는 `:core:domain` 의
UseCase 로만 들어오고, 둘 이상의 기능이 쓰는 조각은 `:core:ui` 에 있다(FM-2 · FM-3).

---

## 상태가 사는 곳 — 이 경계가 이 모듈의 골격이다

| 어디 | 무엇 | 왜 거기 |
|---|---|---|
| `:app` 의 `shell/ShellViewModel` | 백스택 파생 · 두 번 눌러 종료 · 시작 분기의 연결 · 시드 · 테마 글자 크기 · 알약 모델 상태 · 연결이 바뀔 때 화면 정리 | 여러 기능을 동시에 보는 셸 몫. 노트 목록 · 퀴즈 · 토큰은 들지 않는다 |
| `NotesViewModel` | 노트 목록 · 검색 · 북마크 · 동기화 · 읽기 기록 · 리더 스크롤 요청 | 동기화를 구독하는 곳은 여기 하나다(SY-1 · FM-7) |
| `:feature:settings` 의 `SettingsViewModel` | 토큰 편집 · 글자 크기 바꾸기 · 학습 기록 켜기 · 지우기 · 모델 받기 조작 · 끊기 | 마지막 동기화는 홀더에서 읽기만 한다 |
| `:feature:quiz` 의 `QuizViewModel` | 퀴즈 상태 · 자동 진행 타이머 · 그만두기 되돌리기 · 결과 기록 | 오버레이 · CTA 의 모델 상태 |
| `:core:ui` 의 `signal/`(`FeatureSignals` · `LastSyncHolder`, `@Singleton`) | 기능 사이에서 ViewModel 이 받는 신호 · 마지막 동기화 값 | 결정 8 · 13. 둘 이상의 기능이 보므로 `:core:ui` 에 산다 |
| `Navigator`(`:core:navigation`, `@Singleton`) | **백스택** | 소유자가 하나여야 하고, 입구가 하나라 거기서 쓰로틀을 건다 |
| 컴포지션 로컬(`rememberSaveable`) | 드로어 열림 · 고정 패널 펼침 · 모델 확인 다이얼로그 | 목적지가 아니고 레이아웃 상태다. 백스택 엔트리가 아니다 |
| `NotesDrafts`(`:app` 셸 `NotesScreen` 의 `remember`, 셸 분기 위) | **토큰 입력 draft · 보기 여부**(연결 화면 · 설정 편집기 각각) | 저장하지 않는다 — 상태 복원 번들에 원문이 안 들어가고(결정 16), 접기·펴기·회전에는 셸 분기가 바뀌어도 남는다. `toString` 에 원문이 없다(`TokenDraft`) |

플로팅 내비의 선택은 이 셋 어디에도 없다 — `ShellUiState.selectedTab`(`Reader`·`Settings` 를 건너뛴 가장 위의
`Home`/`Bookmarks`/`Stats`)으로 **백스택에서 파생**한다. 선택 상태를 따로 두면 화면과 어긋난다.

**모든 ViewModel 은 Activity 를 owner 로 명시해** (`hiltViewModel(viewModelStoreOwner = activity)`) 얻는다 — 창 등급이 바뀌면
`NavDisplay` 가 통째로 다시 만들어지므로 목적지 스코프면 그때 새로 생기고, 셸과 목적지가 같은 VM 을 다른 인스턴스로 받는다(결정 7).
목적지별 ViewModel 데코레이터를 넣지 않는다. 파이썬 `FM-7 기능 ViewModel 은 Activity 를 owner 로 받는다` 가 지킨다.

통계 ViewModel 의 집계 게이트 · `@StatsWork` · 리더 · 퀴즈 진입(FM-6 ① ②)은 `feature/stats/AGENTS.md` 에 있다.

컴포지션 로컬에 둔 것(드로어 · 패널)을 ViewModel 로 올리려는 충동을 참아라. 드로어/패널은 창 크기에 따라 존재 자체가
사라지는 값이라 상태로 올리면 폼팩터 전환 때 의미가 없어진다. 대신 **뒤로가기는 먹는다**
(아래 참조).

---

## MVI — `build()` 하나가 상태를 통째로 만든다

기능 ViewModel 은 모두 `:core:common` 의 `MviViewModel<S, E>` 를 상속한다(예: `NotesViewModel` 은 `<NotesUiState, NotesIntent>`).
`trigger(initial)` / `Flow.triggerIn(initial)` 로 등록한 값이 하나라도 바뀌면 `build()` 가
다시 돌아 `NotesUiState` 를 새로 만든다. 등록 기준은 하나 — **`build()` 가 그 값을 읽는가.**

- `onIntent()` 는 상태를 만들지 않는다. trigger 값만 바꾼다.
- 파생 계산(`recentNotes` 정렬, `visibleCategories` 검색 필터)은 `build()` 안이 아니라
  `map`/`combine` + `triggerIn` 으로 빼 둔다. 그래야 재료가 바뀔 때만 계산된다.
- **일회성 신호는 값이 아니라 증가하는 id 로 나간다** — `errorId`/`infoId`/`exitHintId`/
  `exitRequestId`/`quizUndoId`. `StateFlow` 는 conflate 하므로 "횟수"나 "이벤트"를 상태로
  흘리면 한 프레임에 묶여 조용히 씹힌다(0→1→2 가 2 만 보인다). 화면은 `LaunchedEffect(id)`
  로 받아 실행만 한다.
- **화면은 스낵바를 다 보여 준 뒤 신호를 낸 VM 에 `SignalShown(signal, id)` 를 보낸다.** VM 은 본 id 를 `build()` 에서 0 으로
  (`notice` 는 null 로) 낸다. 판정은 `:core:ui` 의 `element/NotesSignal.kt` 에 있는 `unseen` 이 한다. 그래서 `configChanges` 밖의 재생성
  (글꼴 · 다크 · 언어)에서 새 컴포지션의 `LaunchedEffect` 가 지난 스낵바와 오래된 `되돌리기` 를 다시 띄우지 않는다.
  `exitRequestId` 는 소비하지 않는다. 종료하면 다시 그릴 화면이 없다.
- **스낵바는 루트 한 곳이 받는다(FM-16).** `SnackbarHostState` 는 `:app` 셸 `NotesScreen` 의 `remember` 하나이고, 네 VM 의 신호를
  받는 `LaunchedEffect` 가 전부 거기(시작 분기 · 창 등급 분기 위)에 있다 — 설정이 맨 위일 때 난 동기화 실패도 바로 뜨고,
  접기 · 펴기에 보이던 것을 다시 띄우지 않는다. 되돌리기는 알림을 낸 VM 의 인텐트로 돌아간다(`NoticeUi<E>`).
  기능 화면 안에 스낵바 수집을 두지 않는다.
- 문구는 ViewModel 이 `getString` 을 직접 부르지 않는다. 인터페이스 `NotesMessages` 를 주입받아 부르고,
  `getString` 은 구현 `AndroidNotesMessages`(앱 `Application`)에만 있다. 접근성 판정(`:feature:quiz` 의 `QuizAutoAdvancePolicy`) ·
  실패 로그(`FailureLog`)도 같은 패턴이고 구현은 `:app` 의 `NotesModule` 이 묶는다.
  `NotesMessages` · `FailureLog` 와 그 구현, 알림 `NoticeUi`, 글자 크기 매핑(`FontScaleUi`)은 여러 기능의 VM 이 같이 써서 `:core:ui` 에 있다.
  이 모듈 안에는 Hilt 모듈이 없다 — 통계 디스패처 `@StatsWork` 의 모듈은 통계 패키지와 같이 `:feature:stats` 로 갔다.
  시각은 `:core:common` 의 `Clock`(간격) · `WallClock`(기록 시각)을 주입받는다 — ViewModel 이 JVM 시험에서
  가상 시간으로 만들어지게 하려는 것이다(FM-8 · `NotesViewModelTest`).

---

## 기능 사이 신호 — 이동은 목적지 키로, 그 밖은 계약으로

다른 기능의 화면으로 가는 것은 `Navigator` 에 목적지 키로 한다. 이동이 아닌 신호 가운데 ViewModel 이 받는 것은
`signal/FeatureSignals`(상태 흐름 + 증가 id)로만 보낸다. 컴포지션을 거쳐 나르면 한 프레임 늦는다(결정 13).
화면만 쓰는 값(오버레이의 노트 제목 · 목록 패널의 설정 배지 · 맨 위로의 토큰 편집 중)은 셸이 VM 상태를 묶어 넘긴다(`:app` 의 `shell/ShellFeatures.kt`).

- 받는 VM 은 만들어질 때의 id 를 이미 본 것으로 잡는다. 계약은 싱글턴이라 Activity 가 다시 열려도 옛 요청이 남아 있다
  (`FM-6 새로 만든 … ViewModel 은 지난 … 다시 처리하지 않는다`). 학습 기록 지우기는 노트 VM 이 해제할 때의 횟수와 되돌릴 때의 횟수를 비교한다.
- **퀴즈 덮음(⑦)** 은 퀴즈 VM 이 상태를 바꾸는 `setQuiz` 한 자리에서 같이 쓴다. 노트 VM 은 흐름으로 받아 읽기 세션을 멈춘다.
- **연결이 바뀌면(⑨)** 셸 VM 의 `onConnectionChanged` 한 호출이 스택 `[Home]` · 계약 `closeQuiz()` · `LastSyncHolder` 를 동기로 쓴다.
  퀴즈 VM 은 Main.immediate 수집기로 받아 같은 메인 스레드 메시지 안에서 닫는다.
- **설정 열기(④)** 는 `Navigator.openSettings(signals)` — 설정 VM 이 토큰 편집을 정리하게 알린 뒤 push 한다.
- **연결 화면 열기(③)** 는 설정 VM 이 토큰 확인 중 잠금을 계약에 동기로 쓰고, 노트 VM 이 그 값을 읽어 막는다.
- **마지막 동기화** 는 `LastSyncHolder` 하나 — 노트 VM 이 쓰고(셸 VM 은 연결 전이 때 비운다) 설정이 읽는다. 동기화 UseCase 를
  다른 VM 이 받으면 구독마다 동기화가 돈다(파이썬 `FM-7 동기화 UseCase 를 받는 클래스는 NotesViewModel 하나다`).

시험은 `FeatureFixture`(시험 소스)가 VM 들을 같은 Navigator · 계약 · 홀더로 세우고 `FeatureSignalsTest` 가 열 갈래를 본다.

---

## 백스택 — `Navigator` 가 소유하고 ViewModel 은 읽어 나른다

셸 VM 이 `navigator.stack`/`isSettled` 를 `triggerIn` 하고, `backStack`·`activeNoteId`·`canPop`·
`isNavSettled` 는 전부 거기서 파생된다. `rememberNavBackStack` 도, VM 의 `activeNoteId` trigger 도
쓰지 않는다 — 상태가 두 군데가 되면 깨진다.

- 이동은 `navigator.push`/`pop` 으로만. 둘 다 `dispatch` 를 타므로 전환이 끝나기 전에 들어온
  이동은 **버려진다**(`NavResult.Throttled`).
- **`Back` 과 `SwitchTab` 은 다르다.** `Back` 은 pop 하나이고 더 갈 데가 없으면 종료 안내/종료로
  이어진다. 플로팅 내비의 `SwitchTab(Home)` 은 `ReplaceAll([Home])`, `SwitchTab(Bookmarks)` 는
  `[Home]` 에서 `Push(Bookmarks)` / 그 외 `ReplaceAll([Home, Bookmarks])` 이고(`:core:navigation` 의 `TabSwitch.kt`),
  이미 그 자리면 `Ignored` 로 아무 일도 없어야 한다. 종료 판단은 화면이 아니라 `pop() == NavResult.Ignored` 가 내린다.
- 두 번 누르기(`EXIT_WINDOW_MILLIS`)는 셸 VM 이 주입받은 `Clock`(단조 시계)으로 센다. 상태로 나가는 건
  `exitHintId`/`exitRequestId` 뿐이고 `finish()` 는 화면이 부른다.
- 사용자 입력이 아닌 정정은 게이트를 우회한다 — 동기화 중 열린 노트가 삭제되면
  `dispatchImmediately(NavCommand.Pop)`.
- **설정이 맨 위일 때 노트 · 퀴즈 열기는 스택을 한 번에 바꾼다**(`:core:navigation` 의 `OpenReader.kt` 에 있는 `openReaderCommand`).
  설정을 걷고 그 자리에 리더를 둔다. pop 과 push 를 나눠 부르면 앞의 변경이 쓰로틀 창을 열어 push 가 버려진다.

---

## 셸은 `:app` 에 있다

시작 분기 · Scaffold · 드로어/고정 패널 틀 · `NavDisplay` · 오버레이 자리 · 뒤로가기 7단 순서 · 루트 스낵바 · 모델 알약 · 셸 ViewModel 은
`:app` 의 `shell/` 이다(기능 문서 `feature-modules` 결정 2 · FM-5). 뒤로가기 순위 표와 셸 파일 나눔은 `app/AGENTS.md` 에 있다.
이 모듈은 셸이 부르는 것 — 등록 파일(`notesDestinations`) ·
목록 패널(`ListPanelScreen` · `ListPanelChrome`) ·
ViewModel 하나 — 만 공개로 내놓고 셸을 import 하지 않는다. 연결 루트 · 읽는 중 화면은 `:feature:connect`, 모델 확인 대화상자(`ModelConfirmDialog`)는 `:feature:settings`, 퀴즈 오버레이(`QuizOverlay`) · 리더 끝 CTA 카드(`QuizCtaCard`)는 `:feature:quiz` 에 있다.

엔트리 안 핸들러(7단 밖)는 이 모듈에 없다 — 설정의 토큰 편집 취소는 `:feature:settings`, 쌓은 연결 화면의 뒤로는 `:feature:connect`.

---

## 폼팩터 — 분기하는 곳은 딱 두 군데다

판정은 `:app` 셸의 `NotesScreen` 이 `containerDpSize` 로 **너비와 높이 둘 다** 보고 `MyNoteTheme` 에 넣는다.
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
  `contentEndPadding`(`FloatingNavDefaults.RailWidth` + 간격 − `page`)으로 예약해 홈 그리드 · 북마크 · 통계 · 리더가
  레일 왼쪽에서 끝나게 한다. 북마크 행의 해제 버튼이 레일 아래 깔리면 누를 수 없기 때문이다. 리더는 가운데 정렬한 본문 칸 바깥에서
  예약하므로 넓은 창에서는 본문 폭이 그대로다.
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
그 밖에서 `ReplaceAll([Home, Stats])`, 이미 top 이면 `Ignored`(`:core:navigation` 의 `TabSwitch.kt`, `TabSwitchTest`).
통계 화면의 2단 · 요약 카드 · 지난 요약 · 패턴은 `feature/stats/AGENTS.md` 에 있다.

### 학습 기록 배선 (통계 0단계)

`NotesViewModel` 이 `:core:domain` 의 `ReadSessionTracker` 를 들고 네 입력을 먹인다 — `init` 의 수집기 둘
(스택 top 의 `activeNoteId`, 계약의 퀴즈 덮음 `quizCoversReader`)과 인텐트 둘(`ForegroundChanged` = `:app` `NotesRoute` 의 라이프사이클
옵저버가 `ON_START`/`ON_STOP` 에 보냄, `ReaderProgress` = `:app` 셸 `ReaderScrollEffects.kt` 가 **지금 본 비율이 바뀔
때마다** 보냄). **세션 안 최댓값은 트래커만 든다** — 셸이 최댓값을 들면 `ON_STOP`→`ON_START` 로 열린 새 세션이
0% 로 저장된다(셸 효과는 다시 시작되지 않아 같은 비율을 다시 보내지 않는다). 그래서 트래커가 top 노트의 마지막
비율을 기억해 새 세션을 거기서 시작한다(PRD D39, 검수 blocker #1). 닫힌 세션·퀴즈 결과·북마크 이벤트는 `launchSafely { withContext(NonCancellable) { … } }` 로 쓴다 —
`ON_STOP` 직후 스코프가 취소돼도 한 행은 끝까지 간다. 퀴즈 결과는 퀴즈 VM 이 쓰고 범주는 설정을 연 쪽이 넘긴다. 변환은 순수 함수
`:feature:quiz` 의 `QuizAttemptRecord.kt` 에 있는 `completedAttempt()` 이고 `QuizAttemptRecordTest` 가 "결과까지 간 것만" 을 지킨다.
북마크 화면 해제의 되돌리기는 `added` 를 새로 쓰지 않고 그 해제 이벤트 행을 지운다(`removedEventId`).

---

## 리더 끝 CTA 자리 — 노트는 슬롯만 내놓는다

퀴즈 진입점은 리더 본문 **맨 끝 CTA** 다. CTA 카드 · 퀴즈 오버레이 · 퀴즈 VM 은 `:feature:quiz`(`feature/quiz/AGENTS.md`)에 있고,
노트는 퀴즈 모듈을 모른다(FM-2). `NoteReaderScreen` 은 끝 자리 슬롯(`endCta`)을, 등록 파일 `notesDestinations` 는 노트 id 를 받는
슬롯(`readerEndCta`)을 받을 뿐이고, `:app` 셸이 그 슬롯에 `QuizCtaCard` 를 넣는다.

**노트 전체를 읽지 못한 노트(`NoteBlock.Unsupported` 의 `BrokenNote` · `LimitExceeded` 가 있는 노트)는 그 자리를
아예 그리지 않는다.** 퀴즈에 넣을 평문이 없어서다. 판정은 `NotesUiMapper` 가 파생하는 `NoteUi.readable` 하나이고(`NotesUiMapperTest`),
통계의 "다시 풀기"(`OpenQuizFor`)도 같은 값으로 리더만 열고 퀴즈 설정을 띄우지 않는다(띄우면 `GenerateQuizUseCase` 가
빈 평문으로 `QuizFailure.Empty`). 블록 몇 개만 못 그린 노트(`UnknownType`·`BrokenBlock`)는 평소대로다. 그 블록은
`NoteBlockMapper` 가 빈 `text`·`raw` 를 `null` 로 바꿔 design-system `NoteContentBlock.Unsupported` 로 넘긴다(`NoteBlockMapperTest`).

---

## 패키지 배치가 뜻하는 것

- `element/`(상태·인텐트) · `mapper/`(도메인 → UI 단방향) · `navigation/`(노트 전용 공유 요소 키) ·
  `screen/`(본 화면 3개) · 루트(등록 파일 · UI 모델 · VM). 호스트(`shell/`)는 `:app` 에 있다.
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
- **프로덕션 트리에 `MyNoteTheme { }` 는 `:app` 의 `shell/NotesRoute.kt` 한 군데뿐이다.** 그래서 안쪽 화면들은
  테마를 감싸지 않고, 대신 **각 화면의 `@Preview` 가 스스로 `MyNoteTheme` 을 두르고 폼팩터를
  넘긴다**(`windowClass`/`heightClass`). 화면을 추가하면 프리뷰도 같은 방식으로 붙인다.
- 화면 단위는 `Surface(color = …)` 로 배경을 갖는다. `Surface` 는 `propagateMinConstraints = true`
  라 자식에 `fillMaxSize` 를 또 붙일 필요가 없다.
- **본문 블록 견본은 `src/debug` 에 있다** — `src/debug/.../screen/NoteFormatSample.kt`. 리더 화면(`NoteReaderScreen`) 그대로에
  견본 노트를 얹은 프리뷰 하나와 상태 14개(글자 모양 · 하위 블록 · 체크 · 알림 · 한도 넘음 · 표를 compact · 852 · 큰 글자로)다. 릴리스에는
  들어가지 않는다. 기능 문서 확인 칸의 `견본` 이 이것이다. debug 앱을 깔고 PreviewActivity 로 상태 번호(0–13, `samples` 순서)를
  골라 연다. 852 상태를 실제 리더 칸 폭으로 보려면 `wm density` 로 화면 폭을 맞추고 끝나면 되돌린다.

  ```bash
  adb -s <기기 serial> shell am start -n com.hancekim.mynote/androidx.compose.ui.tooling.PreviewActivity \
    --es composable com.hancekim.mynote.feature.notes.screen.NoteFormatSampleKt.NoteFormatSamplePreview \
    --es parameterProviderClassName com.hancekim.mynote.feature.notes.screen.NoteFormatSampleProvider \
    --ei parameterProviderIndex 0
  ```

---

## 명령

```bash
./gradlew :feature:notes:testDebugUnitTest   # 노트 VM · 매퍼 · 홈 판정 (순수 JVM)
./gradlew :app:compileDebugKotlin            # 이 모듈 변경 검증은 :app 컴파일로
# 이 모듈 화면을 그리는 계측(BR-8 TitleFontScreensTest · 뒤로가기 BackOrderTest)은 셸과 같이 :app 으로 갔다 — app/AGENTS.md
```

---

## 퀴즈 모델 상태

설정 모델 카드 · 토큰 편집은 `:feature:settings`(`feature/settings/AGENTS.md`)다. 아래는 모델 상태를 읽는 모든 기능(셸 알약 · 설정 카드 · 퀴즈 CTA)에 같다.

### `ModelStatus` 는 `:core:domain` 에 있다 — `:core:ai` 가 아니다

받는 쪽 구현은 `:core:ai` 인데 **feature 는 `:core:ai` 를 의존하지 않는다**(feature 컨벤션이
넣어 주는 건 core/{common, design-system, domain, navigation, ui} 뿐). 그래서 상태 타입과
`QuizModelSource` 계약은 `:core:domain` 이 소유하고 `:core:ai` 의 `ModelDownloads` 가 구현한다.
`QuizGenerator` 와 같은 DIP 다. **`:core:ai` 에 public 을 만들어도 여기서는 영원히 안 보인다.**

화면이 쓰는 입구는 UseCase 다 — `ObserveModelStatusUseCase` · `StartModelDownloadUseCase` ·
`CancelModelDownloadUseCase` · `DiscardPartialModelUseCase` · `DeleteModelUseCase`.

### 실패 이유 문자열은 `ModelFailure` 하나에서만 온다

`ModelStatus.Failed(reason)` 의 `reason` 은 WorkManager output data 를 타고 오는 **문자열**이라
타입이 지켜 주지 않는다. `:core:domain` 의 `ModelFailure.NO_SPACE` 를 양쪽이 같이 본다.
UI 에 `"no-space"` 를 다시 적으면 값이 어긋나는 순간 **공간 부족인데 "잠시 뒤 다시 시도하세요"
로 조용히 떨어진다.** 실제로 한 번 그렇게 만들었다가 고쳤다.

### 퀴즈를 쓸 수 있는지는 모델 상태 하나에서 파생된다

예전엔 `flow { emit(quizGenerator.isAvailable()) }` 로 앱 시작에 한 번만 읽었다. 그래서 다 받아도
앱을 다시 켜야 퀴즈가 켜졌다. 지금은 어느 기능도 `QuizGenerator.isAvailable()` 을 부르지 않는다 —
읽는 ViewModel(셸 · 설정 · 퀴즈 · 통계)이 각자 `ObserveModelStatusUseCase` 로 `ModelStatus` 를 받고, `ModelDownloads.status()` 가
WorkManager 와 설치 파일을 계속 흘린다(`Ready` = 설치된 파일이 있음). 리더 끝 CTA 는 `Ready` 가 아니어도 숨지 않고 상태에 맞는
부제 · 동작으로 남는다(`quizCtaUi`, 아래). 이 모듈은 그 UseCase 를 주입받지 않는다 — 목록 패널의 설정 배지 · 진행 막대가 쓰는
모델 상태는 셸 ViewModel 이 읽어 목록 패널 크롬 값(`ListPanelChrome.modelStatus`, `:app` 의 `shell/ShellFeatures.kt`)으로 넘긴다.

### 상태 → 화면 매핑은 순수 함수다

`:core:ui` 의 `element/ModelStatusUi.kt` 에 있는 `modelCardUi()`(9개 상태 → 점·라벨·막대·버튼·알림 꺼짐 안내 칸), `modelPillUi()`(상태 → 알약 라벨·톤·
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

---

## 저장소 연결 — 시작 분기 · 연결 화면 · 연결 띠

- **시작 분기는 `:app` 셸의 `NotesStart`(`shell/NotesShell.kt`)가 한다.** `shell/ShellUiState.kt:startOf(셸, 노트)`(순수 함수 `shell/ConnectStart.kt:startDestination`)가 셸의 연결 · 시드와 노트의 읽음 · 노트 수를 같은 프레임에 묶어 읽는 중 / 연결 루트 / `NotesShell` 을 가른다.
  연결 루트는 셸 밖이라 인셋을 직접 갖고 스낵바는 루트 호스트를 받는다. 루트의 뒤로가기는 콜백(`onShellBack`)으로 셸 VM 에 간다. `NotesExitEffects`(셸 파일의 `internal`)는 `NotesRoute` 가 분기 위에서
  한 번 부른다. 분기마다 두면 연결 루트와 셸을 오갈 때 종료 안내가 다시 뜬다. 연결을 읽기 전(`connection == null`) · 노트를 읽기 전(`notesLoaded`) · 시드 소비 전(`seedPending`)에는 연결 화면도 홈도 그리지 않는다.
- **화면 정리는 연결 상태 전이가 한다.** `:app` 의 `ShellViewModel.onConnectionChanged` 가 (이전 → 다음 연결) 을 `shell/ScreenCleanup.kt:screenCleanupOf` 에 넘겨 스택 `[Home]` · 퀴즈 닫기 · 마지막 동기화를 같은 틱에 정한다(FM-6 ⑨). 시드 · 확정 · 끊기 어느 길이든 한 곳이고, 화면이 따로 discard 를 부르지 않는다.
- **연결 화면(첫 실행 루트 · 쌓은 화면 · 입구 구역 · 확인 대화상자)은 `:feature:connect` 다** — `feature/connect/AGENTS.md`. 이 모듈에는 홈 · 목록 패널의 연결 띠(`screen/connect/ConnectBand.kt`)만 남았다. 설정의 저장소 카드 · 끊기 대화상자 · 토큰 편집은 `:feature:settings` 다.
- **판정 로직은 순수 함수**(`element/` — 시작 분기 둘 `startDestination` · `screenCleanupOf` 는 `:app` `shell/`, 연결 화면 판정은 `:feature:connect`, 토큰 편집 판정은 `:feature:settings`)다: `startDestination` · `screenCleanupOf` · `repoCardUi`(RC-13 · `:core:ui`) · `homeBodyOf`(RC-21). 시험 이름에 항목 ID.
- **홈 히어로의 저장소 줄은 확정된 연결의 정식 이름 그대로다**(`NotesUiState.heroRepoName` = `connectedRepo?.fullName`, BR-6). 바꾸는 중의
  대상(확인 · 첫 동기화)은 `ConnectViewModel` 에만 있어 홈에 안 보이고, 확정되면 `observeConnection` 이 새 이름을 흘린다. 연결이 없으면 줄이 없다.
  히어로 그림 · 배치는 design-system `MyNoteHero` 이고 제목 이름은 `ui_app_title`(MyNote — 상단 바 · 읽는 중 화면과 같은 문자열)이다.
- 동기화 트리거는 `ObserveSyncRunsUseCase` 다. 생성자로 받는 클래스는 `NotesViewModel` 하나다. 연결 직후 동기화는 `announce` 가 꺼져 스낵바가 없고(SY-6), 새로고침에만 반영 알림이 뜬다.
  VM 의 `sync` 는 `triggerIn(…, started = SharingStarted.Lazily)` 다. 기본값(`WhileSubscribed(0)`)이면 `state` 구독이 5초 넘게
  끊겼다 돌아올 때 상류가 새로 시작해 동기화가 다시 돈다. SY-1 위반이고, 오프라인이면 돌아올 때마다 실패 스낵바가 뜬다.
  `Lazily` 라서 동기화 중에 화면을 떠나도 동기화는 마저 돌고 뒤처리(`afterSyncRun`)도 한다.
