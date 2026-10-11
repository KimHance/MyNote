# 기능 모듈 나누기 — 화면을 기능별 모듈로, 경계는 빌드가 지킨다

| 항목 | 값 |
|---|---|
| 기능 | feature-modules |
| 마지막 변경 | 2026-10-11 · 처음 — `:feature:notes` 하나를 기능 모듈 다섯 · 공용 화면 자리 · 셸로 나눔(동작 그대로) |
| 디자인 | 없음 |
| 항목 ID | FM-번호 (예 FM-4) |

> 사용자가 보는 동작은 바뀌지 않는다. 이 문서는 **코드가 어디에 사는가와 그 경계를 무엇이 지키는가**만 정한다.
> 화면 문구 다듬기는 같은 작업에서 하지만 받아들임 기준은 `screen-copy` 에 따로 둔다.

## 기획

### 왜

앱의 화면이 전부 `:feature:notes` 한 모듈에 있다(약 1.6만 줄 · 화면 11개). 연결 · 설정 · 퀴즈 · 통계가 같은 모듈이라 경계가 약속뿐이고,
`NotesViewModel` 하나가 의존 27개와 퀴즈 진행 · 토큰 편집 · 모델 받기 · 글자 크기까지 들고 있어 ViewModel 시험이 하나도 없다
(시간 · 문구를 직접 읽어서 JVM 에서 돌지 않는다). 뒤로가기 7단 순서는 셸의 컴포즈 순서에 묶여 있는데 지키는 계측 시험이 없어
파일을 옮기면 조용히 깨진다. 사용자 결정으로 기능별 모듈로 나누고, 나누기 전에 ViewModel 시험과 뒤로가기 시험부터 세운다.

### 누구

| 누가 | 언제 쓰나 |
|---|---|
| 앱 코드를 고치는 사람 · 에이전트 | 기능 하나를 고치거나 화면을 더할 때 — 그 모듈과 셸만 본다 |
| 리뷰어 · 검수 | 경계가 지켜졌는지를 의존 그래프 · 시험 이름으로 확인할 때 |

### 흐름

1. 기능 하나를 고친다 → 그 기능 모듈 안에서 끝난다. 다른 기능 모듈은 보이지 않는다(컴파일이 막는다).
2. 새 화면을 더한다 → 목적지 키는 `:core:navigation`, 화면 등록은 그 기능 모듈. 셸 파일은 손대지 않는다.
3. 다른 기능의 화면으로 간다 → 목적지 키로 이동한다. 상대 모듈의 코드를 부르지 않는다.
4. ViewModel 을 시험한다 → 시간 · 문구를 주입받으므로 가상 시간 JVM 시험으로 돈다.

### 결정

| # | 날짜 · run | 결정 | 왜 | 누가 | 상태 |
|---|---|---|---|---|---|
| 1 | 2026-10-11 · feature-modules | 기능 모듈은 다섯 — 노트 · 연결(첫 실행 화면) · 설정 · 퀴즈 · 통계 | 사용자 결정은 노트 · 설정 · 퀴즈 · 첫 실행 넷. 통계는 이미 자기 ViewModel 이 있고 약 3,400줄이라 다섯째로 뗀다(넷으로 할지는 열린 질문 1) | 메인 | 정함 |
| 2 | 2026-10-11 · feature-modules | 셸(시작 분기 · Scaffold · 드로어/고정 패널 틀 · 플로팅 내비 · NavDisplay · 오버레이 자리 · 뒤로가기 순서)은 `:app` 으로 | 시작 분기와 셸은 여러 기능을 동시에 알아야 하는데 그 일을 할 수 있는 모듈은 모든 기능을 의존하는 `:app` 뿐이다. 공식 Navigation 3 기능 모듈 레시피도 NavDisplay 를 앱 모듈에 둔다(열린 질문 2) | 메인 | 정함 |
| 3 | 2026-10-11 · feature-modules | 두 기능 이상이 쓰는 화면 조각 · 화면 모델은 새 `:core:ui` 에. design-system 의 새 컴포넌트 조건(DESIGN 13.0)에 맞는 일반 위젯이면 design-system 으로 | feature 끼리 직접 의존하지 않으려면 공용물이 안쪽에 있어야 한다. design-system 은 기능 의미(모델 상태 · 토큰 입력)를 모르는 자리라 그대로 둔다(열린 질문 3) | 메인 | 정함 |
| 4 | 2026-10-11 · feature-modules | 순서는 문구 다듬기 → ViewModel 시험 가능하게 → ViewModel 나누기 → 뒤로가기 계측 시험 → 목적지 등록 틀 → 모듈 하나씩 떼기 → 셸 이동 | 시험이 먼저 서야 옮기는 동안의 회귀가 보인다. 문구를 먼저 고치면 한 `strings.xml` 에서 끝나고 모듈로 흩어진 뒤에 같은 일을 다섯 번 하지 않는다 | 메인 | 정함 |

### 안 하는 것

| 안 한다 | 왜 (한 마디) |
|---|---|
| 사용자가 보는 동작 · 모양 바꾸기 | 범위 밖 — 문구만 `screen-copy` 로 바꾼다 |
| 목적지 키 바꾸기 · 퀴즈를 백스택 목적지로 만들기 | 뒤로가기 순서(FM-10)와 기존 시험이 키에 기대고 있다 |
| 백스택 저장 · 프로세스 사망 복원 | 지금도 없다. 리뷰가 사람에게 넘긴 결정 거리다 |
| `:core:*` 의 데이터 · 도메인 경계 바꾸기 | 화면 모듈만 나눈다 |
| 기능 문서 RC · SY · MD · NF · BR · NT 항목 바꾸기 | 동작 그대로 — 시험만 이름을 지키며 옮긴다 |

## 명세

### 모듈 지도

| 모듈 | 무엇 (지금 자리) | 의존하는 것 | 문구 접두사 |
|---|---|---|---|
| `:feature:notes` | 홈 · 북마크 · 목록 패널 내용 · 리더 · 동기화 알림 · 학습 기록 입력 (`screen/` 의 홈 · 목록 패널 · 리더 · `bookmarks/` · `mapper/` · 노트 화면 모델) | feature 컨벤션 넷 + `:core:ui` | `notes_` (그대로) |
| `:feature:connect` | 첫 실행 연결 화면 · 쌓은 연결 화면 · 연결 대화상자 (`screen/connect/` · 연결 판정 순수 함수) | 같음 | `connect_` |
| `:feature:settings` | 설정 화면 · 저장소 칸 · 모델 카드 · 모델 대화상자 · 글자 크기 · 학습 기록 켜기 (`SettingsScreen` · `RepoSection` · `ModelCard` · `ModelDialogs` · 토큰 편집 판정) | 같음 | `settings_` |
| `:feature:quiz` | 퀴즈 설정 · 생성 중 · 풀기 · 결과 · 실패 · 오버레이 · 리더 끝 CTA (`quiz/`) | 같음 | `quiz_` |
| `:feature:stats` | 통계 · 지난 요약 (`screen/stats/` · `stats/`) | 같음 | `stats_` |
| `:core:ui` (새) | 두 기능 이상이 쓰는 화면 조각 · 화면 모델 — 한글 어절 줄바꿈 글자 · 외부 브라우저 열기 · 토큰 입력 홀더 · 모델 상태 → 카드 · 알약 · CTA 판정 · 기능 사이 신호 계약 ? | `:core:{common, design-system, domain, navigation}` | `ui_` |
| `:app` | 셸 — 시작 분기 · Scaffold · 드로어/고정 패널 틀 · 플로팅 내비 · 맨 위로 · 모델 알약 · 상단 바 틀 · NavDisplay · 오버레이 자리 · 뒤로가기 순서 · 종료 안내 · 테마 · 창 크기 판정 (`shell/` · `NotesRoute`) | 모든 feature + core | `shell_` (컨벤션) |

### ViewModel 경계

지금 `NotesViewModel` 이 든 상태 묶음이 어디로 가나. ConnectViewModel · StatsViewModel 은 그대로다.

| 상태 묶음 (지금 `NotesUiState` 칸) | 갈 곳 |
|---|---|
| 백스택 파생(`backStack` · `activeNoteId` · `canPop` · `isNavSettled` · `selectedTab`) · 종료(`exitHintId` · `exitRequestId` · 두 번 누르기 창) · 시작 분기(`connection` · `notesLoaded` · `seedPending`) · 연결이 바뀔 때 화면 정리 · 테마에 넣을 글자 크기 · 스낵바 호스트 | 셸 ViewModel (`:app`) |
| 범주 · 검색 · 펼침 · 최근 · 북마크와 해제 되돌리기 · 동기화(`isLoading` · 실패 · 반영 알림 · `lastSync` · `awaitingNotes` · 새로고침) · 리더 스크롤 요청 · 학습 기록 입력(보임 · 읽은 비율) | `NotesViewModel` (`:feature:notes`) |
| 토큰(`hasToken` · `maskedToken` · `tokenEdit` · 지우기 되돌리기) · 글자 크기 바꾸기 · 학습 기록 켜기 · 지우기 · 모델 받기 조작 · 알림 꺼짐 · 끊기 대화상자 · 저장소 칸 | `SettingsViewModel` (`:feature:settings`) |
| 퀴즈 상태 · 자동 진행 타이머 · 그만두기 되돌리기 · 생성 작업 · 퀴즈 결과 기록 | `QuizViewModel` (`:feature:quiz`) |
| 모델 상태 | 읽는 곳(셸 알약 · 설정 카드 · 퀴즈 CTA · 통계)마다 UseCase 로 읽는다 |

### 문구 자리

| 지금 이름 | 갈 곳 |
|---|---|
| `notes_stats_*` | `:feature:stats` `stats_*` |
| `notes_quiz_*` | `:feature:quiz` `quiz_*` |
| `notes_settings_*` · `notes_repo_*` · 모델 카드 · 대화상자 문구 | `:feature:settings` `settings_*` |
| `notes_connect_*` · `notes_entry_*` · `notes_link_*` · `notes_dialog_*` | `:feature:connect` `connect_*` |
| `notes_nav_*` · `notes_pill_*` · `notes_exit_hint` · `notes_action_*` 중 상단 바 | `:app` `shell_*` |
| 두 모듈 이상이 쓰는 문구(`notes_app_title` · `notes_token_howto_*` · 모델 상태 라벨 등) | `:core:ui` `ui_*` |
| 나머지(`notes_home_*` · `notes_bookmark_*` · `notes_panel_*` · `notes_sync_*`) | `:feature:notes` 그대로 |

시험 · 템플릿 README · 문서가 이름이나 경로로 묶은 문구: `public-repos/tests/test_template.py` 의 앱 용어 일곱(`notes_entry_create` · `notes_connect_*` 셋 · `notes_token_howto_*` 셋) ·
`notes-tests/test_brand_title.py` 의 제목 다섯(`notes_app_title` · `notes_settings` · `notes_nav_bookmark` · `notes_nav_stats` · `notes_stats_history_title`)과 그 파일 경로 ·
`model-download` MD-8 확인 칸의 리소스 이름 둘 · 디자인 문서 문구표의 리소스 이름 · Kotlin 단위 시험의 `R.string` 단언.

### 동작

| ID | 동작 | 받아들임 기준 | 확인 |
|---|---|---|---|
| FM-1 | 화면은 모듈 지도대로 다섯 기능 모듈에 산다 | `settings.gradle.kts` 에 `:feature:{notes, connect, settings, quiz, stats}` 와 `:core:ui` 가 있다. 모듈 지도의 화면 파일이 그 모듈 `src/main` 에만 있고 `:feature:notes` 에 연결 · 설정 · 퀴즈 · 통계 화면 파일이 0개다 | 파이썬: `FM-1 화면은 모듈 지도대로 산다` |
| FM-2 | 기능 모듈은 서로를 의존하지 않는다 | 다섯 기능 모듈과 `:core:ui` 의 `debugCompileClasspath` · `debugRuntimeClasspath` 에 `:feature:*` 프로젝트가 0개다 | 파이썬: `FM-2 기능 모듈은 서로를 의존하지 않는다` · 명령: `./gradlew :feature:<모듈>:dependencies --configuration debugRuntimeClasspath` 에 `project :feature:` 0줄 |
| FM-3 | 기능 모듈 · `:core:ui` 는 데이터 층 · 모델 구현을 모른다 | 같은 클래스패스에 `:core:data` · `:core:network` · `:core:database` · `:core:ai` · `:core:format` 이 0개다 | 파이썬: `FM-3 기능 모듈은 데이터 층을 모른다` |
| FM-4 | 목적지 화면은 그 기능 모듈이 등록하고 셸은 등록 묶음을 그린다 | `NotesNavKey` 의 키 7개마다 등록된 화면이 정확히 하나다. 셸 파일에서 기능 화면 Composable 을 부르는 줄이 0이다. 새 키를 더할 때 셸 파일을 고치지 않는다 | JVM: `FM-4 모든 목적지 키에 화면이 하나씩 등록된다` · 리뷰 grep: 셸 파일의 `HomeScreen(` · `SettingsScreen(` · `StatsRoute(` · `ConnectRoute(` · `NoteReaderScreen(` · `BookmarksScreen(` 0건 |
| FM-5 | 셸은 `:app` 에 산다 | `NavDisplay(` 호출 · `ModalNavigationDrawer` · 플로팅 내비 배치 · 뒤로가기 순서의 핸들러가 `:app` 에만 있고 `feature/` 아래 0건이다. `MainActivity` 가 부르는 루트 화면이 `:app` 에 있다 | 리뷰 grep: `feature/` 아래 `NavDisplay(` · `ModalNavigationDrawer(` 0건 · `:app:compileDebugKotlin` |
| FM-6 | 기능 사이 이동은 목적지 키로만, 이동이 아닌 신호는 안쪽 계약으로만 | 통계 → 리더 열기 · 다시 풀기, 홈 · 설정 → 연결 화면, 리더 → 퀴즈 설정, 퀴즈가 리더를 덮는 동안 읽기 세션 멈춤, 연결이 바뀔 때 스택 `[Home]` · 퀴즈 닫기가 지금과 같게 일어난다. 그 신호가 지나는 타입이 `:core:*` 에 있다 | JVM: `FM-6 통계에서 노트를 열면 리더가 열린다` · `FM-6 통계 다시 풀기는 리더를 열고 퀴즈 설정을 띄운다` · `FM-6 퀴즈가 리더를 덮는 동안 읽기 세션이 멈춘다` · 기존 `ScreenCleanup` 시험 |
| FM-7 | ViewModel 은 자기 기능의 상태만 든다 | ViewModel 경계 표대로다 — `:feature:notes` 에서 `QuizUiState` · `TokenEditState` · `ModelStatus` 조작 UseCase(받기 · 취소 · 지우기) 참조 0건, 셸 ViewModel 이 화면 목록 · 퀴즈 · 토큰 상태를 들지 않는다. 동기화를 시작하는 구독은 앱 전체에 하나다 | 컴파일(FM-2 가 막는다) · 리뷰 grep · JVM: `FM-7 동기화는 구독이 여럿이어도 한 번 돈다` |
| FM-8 | ViewModel 은 시간 · 문구를 주입받는다 | 모든 `*ViewModel.kt` 에서 `SystemClock` · `System.currentTimeMillis` · `@ApplicationContext` · `getString(` 0건. 모든 ViewModel 이 JVM 단위 시험에서 가상 시간으로 만들어진다 | 리뷰 grep · 파이썬: `FM-8 ViewModel 은 시간과 문구를 직접 읽지 않는다` |
| FM-9 | ViewModel 시험이 시간에 기대는 규칙을 지킨다 | 가상 시간 JVM 시험이 있다 — 루트에서 뒤로가기 두 번(창 안 = 종료 요청 · 창 밖 = 안내 다시) · 퀴즈 자동 진행(늦게 온 타이머가 문항을 건너뛰지 않음) · 퀴즈 그만두기 되돌리기(창 안 복귀 · 창 밖 사라짐) · 토큰 지우기 되돌리기 · 북마크 해제 되돌리기 · 본 신호는 다시 나오지 않음 | JVM: `FM-9 루트에서 창 안에 두 번 누르면 종료한다` · `FM-9 창이 지나면 다시 안내한다` · `FM-9 늦게 온 자동 진행은 문항을 건너뛰지 않는다` · `FM-9 그만둔 퀴즈는 창 안에서만 되돌린다` · `FM-9 지운 토큰을 되돌린다` · `FM-9 해제한 북마크를 되돌린다` · `FM-9 본 신호는 다시 나오지 않는다` |
| FM-10 | 뒤로가기 7단 순서를 계측 시험이 지킨다 | 에뮬레이터에서 단마다 앞 단이 없는 상태를 만들고 뒤로가기 한 번 → 그 단의 결과만 일어난다: ① 열린 드로어가 닫힌다 ② 퀴즈 오버레이가 닫힌다 ③ 전환 중 뒤로가기는 아무 일도 없다 ④ 쌓인 화면이 pop 된다 ⑤ 여는 중인 드로어가 닫힌다 ⑥ 펼친 고정 패널이 접힌다(넓은 창) ⑦ 루트에서 종료 안내 → 창 안 두 번째에 종료. 그리고 두 단이 겹치면 순위가 높은 쪽만 일어난다(① + ④ · ② + ④ · ⑥ + ⑦) | 계측: `FM-10 열린 드로어가 먼저 닫힌다` 외 단마다 하나 · `FM-10 겹치면 순위가 높은 쪽만 일어난다` — 셸을 옮기기 전에 세우고 옮긴 뒤 같은 이름으로 통과 |
| FM-11 | 문구는 그 화면의 모듈에 그 접두사로 산다 | 문구 자리 표대로다. 모든 라이브러리 기능 모듈 · `:core:ui` 에 `resourcePrefix` 가 있고 `lintDebug` 에 리소스 이름 경고 0. 모든 `strings.xml` 을 합쳐 같은 이름이 두 번 정의된 문구 0. 정의만 있고 아무도 안 쓰는 문구 0 | `./gradlew lintDebug` · 파이썬: `FM-11 문구 이름은 모듈을 넘어 겹치지 않는다` · `FM-11 쓰지 않는 문구가 없다` |
| FM-12 | 시험 · README · 문서에 묶인 문구가 같이 옮겨진다 | 문구 자리 아래 목록의 시험이 새 이름 · 새 경로로 통과하고 시험 이름은 그대로다. 레포 문서 · 시험 · 코드에 옮긴 옛 이름(`notes_` + 옮긴 묶음)이 0건이다(`## 바뀐 기록` 줄 제외) | `python3 -I -m unittest discover notes-tests` · `python3 -I -m unittest discover public-repos/tests` · 리뷰 grep |
| FM-13 | 시험은 이름을 지키며 옮겨지고 수가 줄지 않는다 | 기준 커밋(`9d6f0fd`)의 `:feature:notes` 시험(단위 207 · 계측 2)의 (클래스 이름, 시험 이름) 쌍이 나눈 뒤 어느 모듈에든 모두 있다. 모듈별 시험 수의 합 ≥ 209 + 이번에 새로 쓴 FM 시험 수 | 명령: 기준 · 나중의 JUnit 결과 XML 에서 쌍 비교(차집합 0) |
| FM-14 | 전체 검증이 통과한다 | 루트 `AGENTS.md` 의 한 번에 검증 명령이 모두 통과한다 — `ktlintCheck` · `:app:assembleDebug` · `test` · `lintDebug` · 파이썬 셋 · 계측(시험이 있는 모듈마다 차례로) | 명령 |
| FM-15 | 문서가 새 모듈을 안다 | 새 모듈마다 `AGENTS.md` 가 있다. 루트 `AGENTS.md` 의 모듈 지도 · `build-logic/AGENTS.md` 의 모듈 → 컨벤션 표 · `app/AGENTS.md`(셸) · `feature/notes/AGENTS.md`(남은 것만)가 새 지도와 맞다. 계측 명령 줄에 새 모듈이 있다 | 파이썬: `FM-15 모듈마다 AGENTS.md 가 있다` · `test_hygiene` 통과 · 리뷰 |

### 안 바뀔 것

| 무엇 | 확인 |
|---|---|
| 기능 문서 `repo-connect` · `sync` · `model-download` · `note-format` · `brand` · `note-template` 의 모든 항목과 그 시험(이름 그대로 · 옮긴 모듈에서 통과) | `test` · 계측 · 파이썬 |
| 뒤로가기 7단 순서(`feature/notes/AGENTS.md` 표) | FM-10 |
| 플로팅 내비 선택이 백스택에서 파생된다 · 탭 전환 규칙 | 기존 `TabSwitchTest` · `OpenReaderTest` |
| 접기 · 펴기 · 회전에서 리더 스크롤 · 원문 펼침 · 토큰 입력 draft · 퀴즈 진행 · 통계 기간이 남는다 | 수동(Fold 에뮬레이터: 리더 · 설정 토큰 편집 · 퀴즈 풀기 · 통계 각각 접고 펴기) |
| 토큰 원문이 상태 · 저장 번들 · `toString` 에 나오지 않는다 | 기존 `TokenDraftTest` · `TokenMaskTest` · `NotesDraftsTest` |
| `layerBackdrop` 상자 안에는 NavDisplay 만 | 수동(유리 내비 · 알약이 자기를 비추지 않음) · 리뷰 |
| 통계 집계 · 인사이트는 그 화면이 맨 위일 때만 돈다 | 수동(debug `MyNoteStats` 로그) |
| 동기화는 앱이 살아 있는 동안 화면 재구독으로 다시 돌지 않는다(SY-1) | FM-7 · 기존 SY 시험 |
| 일회성 신호(스낵바 · 되돌리기)가 재생성 뒤 다시 뜨지 않는다 | 기존 `NotesSignalTest` · FM-9 |
| 템플릿 저장소가 있을 때만 입구 구역이 보인다(RC-25) | 기존 RC-25 시험 |

## 열린 질문

| # | 무엇 | 지금 기본값 | 날짜 · run |
|---|---|---|---|
| 1 | 기능 모듈 수 — 사용자 결정의 넷(통계는 노트 안) 또는 다섯(통계 따로) | 다섯(결정 1) | 2026-10-11 · feature-modules |
| 2 | 셸 자리 — `:app` 또는 `:feature:notes` 가 호스트로 남기 | `:app`(결정 2). `app/AGENTS.md` 의 화면 없음 원칙이 바뀐다 | 2026-10-11 · feature-modules |
| 3 | 공용 화면 자리 — 새 `:core:ui` 또는 design-system 에 올리기 | 새 `:core:ui`(결정 3) | 2026-10-11 · feature-modules |
| 4 | 첫 실행 모듈 이름 — `:feature:connect`(코드 이름) 또는 `:feature:intro` | `:feature:connect` | 2026-10-11 · feature-modules |

## 바뀐 기록

| 날짜 | run | 항목 | 무엇이 |
|---|---|---|---|
| 2026-10-11 | feature-modules | FM-1 ~ FM-15 | 처음 |
