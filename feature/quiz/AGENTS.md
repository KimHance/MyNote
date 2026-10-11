# AGENTS.md — :feature:quiz

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

퀴즈 설정 · 생성 중 · 풀기 · 결과 · 실패 · 오버레이 · 리더 끝 CTA · 문항 화면 모델의 자리다(기능 문서 `feature-modules` 모듈 지도). `:feature:notes` 에서 패키지째 옮겨 왔다.

| 패키지 | 무엇 |
|---|---|
| `com.hancekim.mynote.feature.quiz` | `QuizViewModel` · 상태(`QuizUiState` · `QuizOverlayState`) · 인텐트(`QuizIntent`) · 오버레이(`QuizOverlay`) · 문항 화면 모델(`QuizQuestionUi` · `QuizQuestion.toUi`) · 결과 기록 변환(`completedAttempt`) · 자동 진행 정책(`QuizAutoAdvancePolicy`) |
| `com.hancekim.mynote.feature.quiz.screen` | 화면 — 한 화면을 다 차지하고 `Surface` 로 배경을 갖는다(`QuizGeneratingScreen` · `QuizSolveScreen` · `QuizResultScreen` · `QuizFailedScreen`) |
| `com.hancekim.mynote.feature.quiz.component` | 조각 — 설정 대화상자(`QuizSetupDialog`) · 리더 끝 CTA 카드(`QuizCtaCard`) · 버튼(`QuizParts`) · `QuizOrb`. `QuizSolveScreen` 은 화면이고 `QuizOrb` 는 아니다 |

목적지 키가 없어 등록 파일이 없다. `:app` 셸이 퀴즈 오버레이(`shell/NotesShell.kt` — 결정 12 의 고정 순서, 뒤로가기 ②)와
리더 끝 CTA 카드(노트 등록 파일의 끝 자리 슬롯)를 직접 부르고, `QuizViewModel` 을 Activity owner 로 얻으며(`shell/NotesRoute.kt` · 결정 7),
자동 진행 정책 바인딩은 `:app` 의 `di/NotesModule.kt` 가 묶는다. 그래서 `QuizOverlay` · `QuizCtaCard` · `QuizViewModel` · `QuizUiState` ·
`QuizOverlayState` · `QuizIntent` · `QuizAutoAdvancePolicy` · `AccessibilityAutoAdvancePolicy` 는 공개다. `:app` 의 `BackOrderTest` 가
오버레이 상태를 꾸미려고 `QuizQuestionUi` 를 쓴다.

## 경계

| 지킬 것 | 지키는 것 |
|---|---|
| `build.gradle.kts` 는 `hancenote.android.feature` 하나 — `:core:{common, design-system, domain, navigation, ui}` · 공통 의존 · 문구 접두사 `quiz_` 를 컨벤션이 넣는다 | `build-logic/AGENTS.md` |
| 다른 기능 모듈을 의존하지 않는다. 둘 이상이 쓰는 것은 `:core:ui`, 다른 기능 화면으로는 목적지 키로 간다 | `notes-tests/test_feature_modules.py` FM-2 |
| 데이터 층(`:core:{data, network, database, format}`) · 모델 구현(`:core:ai`)을 모른다 — `:core:domain` 의 UseCase 와 `ModelStatus` 만. 엔진 하나를 통계 워커와 나눠 쓰는 규칙(`sendCancellable` · `tryUse`)은 `core/ai/AGENTS.md` | FM-3 |
| 문구는 이 모듈만 참조하는 것만 둔다(결정 15) — CTA 부제(`ui_quiz_cta_*`)는 공용 판정 `quizCtaUi` 가, 그만두기 스낵바(`ui_quiz_abandoned`)는 `NotesMessages` 구현이 써서 `:core:ui` 에 있다 | `lintDebug` · FM-11 |

## 퀴즈는 내비게이션 목적지가 아니다

진입점은 헤더가 아니라 리더 본문 **맨 끝 CTA**(`QuizCtaCard`)다. 리더는 노트 모듈이라 이 카드를 모르고, `:app` 셸이 리더 끝 슬롯에 넣는다(FM-2). 모델이 없어도 숨기지 않고
흐리게 두고, 누르면 상태에 따라 갈 곳이 갈린다 — 순수 함수 `:app` 의 `shell/ShellFeatures.kt:ctaTarget`(준비됨 → 퀴즈 VM 설정 · 없음 → 받기 확인
대화상자 · 그 밖 → 설정 VM 받기). 알림 권한은 루트가 받기 전에 묻는다.

노트 전체를 읽지 못한 노트에서는 CTA 자리를 아예 그리지 않는다 — 판정(`NoteUi.readable`)은 노트 몫이다(`feature/notes/AGENTS.md` `## 리더 끝 CTA 자리`).

`QuizUiState`(sealed)는 퀴즈 VM 상태 `QuizOverlayState.quiz` 한 칸에 들어가고, `QuizOverlay` 가 리더 **위에 덮인다**.
`Navigator` 를 건드리지 않으므로 백스택에 없고, 뒤로가기는 오버레이가 한 번 받아 `QuizIntent.Dismiss` 를 쏜다.

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

### 기능 사이 신호

- **퀴즈 덮음(FM-6 ⑦)** 은 퀴즈 VM 이 상태를 바꾸는 `setQuiz` 한 자리에서 신호 계약(`:core:ui` 의 `FeatureSignals`)에 같이 쓴다. 노트 VM 이 흐름으로 받아 읽기 세션을 멈춘다.
- **퀴즈 설정 요청(②)** 은 통계 다시 풀기 · 리더 열기가 계약의 `requestQuizSetup` 으로 보내고, **연결이 바뀌면(⑨)** 셸 VM 이 `closeQuiz()` 를 쓴다.
  퀴즈 VM 은 Main.immediate 수집기로 받아 같은 메인 스레드 메시지 안에서 닫는다. 새로 만든 VM 은 지난 요청 · 닫기를 다시 처리하지 않는다.
- **결과 기록의 범주(⑧)** 는 설정을 연 쪽이 넘긴 값 — 퀴즈를 시작할 때의 값이다.

## 시험

퀴즈 VM 시험은 이 모듈의 `FeatureFixture`(노트 모듈 fixture 의 퀴즈 쪽 사본)로 세운다. 기능 사이 신호 가운데 퀴즈 VM 하나만 보는 것은
이 모듈의 `FeatureSignalsTest`, 노트 VM 과 같이 보는 것(⑦ 퀴즈 덮음 · 새 노트 VM 의 지난 리더 열기)은 `:app` 의 같은 이름 `FeatureSignalsTest` 에 있다(FM-13).
채점 · 진행 규칙은 `QuizGradingTest`, 결과 기록 변환은 `QuizAttemptRecordTest`, 가상 시간 규칙(FM-9)은 `QuizViewModelTest` 다.

## 명령

```bash
./gradlew :feature:quiz:testDebugUnitTest   # 채점 · 결과 기록 · 퀴즈 VM (순수 JVM)
./gradlew :app:compileDebugKotlin           # 이 모듈 변경 검증은 :app 컴파일로
# 오버레이 뒤로가기(FM-10 ②)를 그리는 계측 BackOrderTest 는 셸과 같이 :app 에 있다 — app/AGENTS.md
```
