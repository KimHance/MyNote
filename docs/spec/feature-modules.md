# 기능 모듈 나누기 — 화면을 기능별 모듈로, 경계는 빌드가 지킨다

| 항목 | 값 |
|---|---|
| 기능 | feature-modules |
| 마지막 변경 | 2026-10-12 · 승격 — 결정 19 ~ 24(드로어 디스패처 · 공용 조각 · 낡는 문서 · 공통 액션 예외 · CTA 슬롯) · 열린 질문 1 ~ 4 |
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
3. 다른 기능의 화면으로 간다 → 목적지 키로 이동한다. 이동이 아닌 신호는 안쪽 계약으로 보낸다. 상대 모듈의 코드를 부르지 않는다.
4. ViewModel 을 시험한다 → 시간 · 문구를 주입받으므로 가상 시간 JVM 시험으로 돈다.

### 결정

| # | 날짜 · run | 결정 | 왜 | 누가 | 상태 |
|---|---|---|---|---|---|
| 1 | 2026-10-11 · feature-modules | 기능 모듈은 다섯 — 노트 · 연결(첫 실행 화면) · 설정 · 퀴즈 · 통계 | 사용자 결정. 통계는 이미 자기 ViewModel 이 있고 약 3,400줄이다 | 나 | 정함 |
| 2 | 2026-10-11 · feature-modules | 셸(시작 분기 · Scaffold · 드로어/고정 패널 틀 · 플로팅 내비 · NavDisplay · 오버레이 자리 · 뒤로가기 순서)은 `:app` 으로 | 사용자 결정. 시작 분기와 셸은 여러 기능을 동시에 알아야 하고, 그럴 수 있는 모듈은 모든 기능을 의존하는 `:app` 뿐이다 | 나 | 정함 |
| 3 | 2026-10-11 · feature-modules | 두 기능 이상이 쓰는 화면 조각 · 화면 모델 · 기능 사이 신호 계약은 새 `:core:ui` 에 | 사용자 결정. design-system 은 기능 뜻(모델 상태 · 토큰 입력)을 모르는 자리로 둔다 | 나 | 정함 |
| 4 | 2026-10-11 · feature-modules | ~~순서는 문구 다듬기 → ViewModel 시험 가능하게 → ViewModel 나누기 → 뒤로가기 계측 시험 → …~~ | | 메인 | 뒤집힘 → 6 |
| 5 | 2026-10-11 · feature-modules | 첫 실행 화면 모듈 이름은 `:feature:connect`(코드 이름 그대로) | 사용자 결정 | 나 | 정함 |
| 6 | 2026-10-11 · feature-modules | ~~순서는 … 공용 조각 옮기기 → 통계 · 연결 · 설정 · 퀴즈 차례로 떼기 → 셸 이동~~ | | 메인 | 뒤집힘 → 17 |
| 7 | 2026-10-11 · feature-modules | 기능 ViewModel 은 전부 Activity 를 owner 로 명시해 받는다. 목적지별 ViewModel 데코레이터를 쓰지 않는다 | 창 등급이 바뀌면 NavDisplay 가 통째로 다시 만들어진다. 목적지 스코프면 그때 ViewModel 이 새로 생기고, 셸과 목적지가 같은 ViewModel 을 다른 인스턴스로 받는다 | 메인 | 정함 |
| 8 | 2026-10-11 · feature-modules | 마지막 동기화 값은 `:core:ui` 의 앱 하나뿐인 홀더 — 노트 ViewModel 이 동기화 결과로 쓰고, 셸 ViewModel 이 연결 전이 때 비우며(FM-6 ⑨ 같은 틱), 설정 · 목록 패널이 읽는다 | 동기화를 일으키는 구독을 하나로 지키면서(SY-1) 설정 저장소 칸 · 목록 패널 배지가 같은 값을 본다 | 메인 | 정함 |
| 9 | 2026-10-11 · feature-modules | 공용 버튼 넷 · 본문 최대 폭은 `:core:ui` | 노트 · 연결 · 설정 · 퀴즈가 같이 쓴다. design-system 으로 올리면 DESIGN 13.0 심사와 디자인 문서 변경이 따라온다 | 메인 | 정함 |
| 10 | 2026-10-11 · feature-modules | 제목 글꼴 계측(BR-8)은 `:app` 계측 한 시험으로 옮기고, 그 시험이 그리는 연결 · 통계 화면 진입점을 공개로 넓힌다 | 시험을 기능별로 쪼개면 (클래스, 이름) 쌍이 깨진다(FM-13) | 메인 | 정함 |
| 11 | 2026-10-11 · feature-modules | 뒤로가기 계측은 상태 없는 셸에 가짜 상태를 넣어 그린다. ⑦ 의 두 번 누르기 시간 창은 FM-9 JVM 시험이 본다. Hilt 계측 의존을 더하지 않는다 | 라이브러리 계측에는 앱 Hilt 그래프가 없다. 새 의존 0 | 메인 | 정함 |
| 12 | 2026-10-11 · feature-modules | 등록 묶음은 목적지 키 7개의 화면에만 쓴다. 퀴즈 오버레이 · 목록 패널 · 상단 바 행동 · 모델 확인 대화상자 · 뒤로가기 핸들러는 `:app` 셸 파일 한 곳에서 고정 순서로 직접 부른다 | 뒤로가기 순위 = 컴포즈 순서다. 순서 없는 집합으로 받으면 퀴즈 오버레이(②)가 삼킴(③)보다 먼저 컴포즈될 수 있다 | 메인 | 정함 |
| 13 | 2026-10-11 · feature-modules | 기능 사이 신호 가운데 ViewModel 이 받는 것은 `:core:ui` 의 앱 하나뿐인 계약(상태 흐름 + 증가 id)으로, 화면만 쓰는 값은 `:app` 이 ViewModel 상태를 묶어 넘긴다 | 둘 다 바깥 → 안쪽이다. 컴포지션을 거쳐 넘기면 한 프레임 늦어 읽기 세션 기록 시각이 어긋난다 | 메인 | 정함 |
| 14 | 2026-10-11 · feature-modules | 스낵바는 `:app` 루트(시작 분기 · 창 등급 분기 위) 한 곳이 호스트 하나로 받는다. ViewModel 마다 자기 본 신호를 든다 | 기능 화면 안에서 받으면 다른 화면에 있을 때 난 실패가 늦게 뜨고, 접기 · 펴기 도중 사라졌다 다시 뜬다 | 메인 | 정함 |
| 15 | 2026-10-11 · feature-modules | 문구 자리는 접두사가 아니라 **참조하는 모듈 수**로 정한다 — 하나면 그 모듈, 둘 이상이면 `:core:ui`(공통 액션이면 design-system) | 지금 접두사와 실제 사용처가 어긋난다(퀴즈 CTA 문구를 공용 판정이 쓰고, 설정 · 모델 문구를 목록 패널이 쓴다) | 메인 | 정함 |
| 16 | 2026-10-11 · feature-modules | `:app` 에는 `resourcePrefix` 를 걸지 않고 셸 문구 접두사 `shell_` 은 파이썬 시험이 본다 | lint 는 `:app` 에도 접두사를 강제하지만, 걸면 기존 리소스 17개(앱 이름 · 테마 · 런처 · 백업 규칙 · 브랜드 그림)를 바꿔야 한다 | 메인 | 정함 |
| 17 | 2026-10-11 · feature-modules | 순서는 문구 다듬기(ViewModel 시험 가능하게와 나란히) → 뒤로가기 계측 시험 → ViewModel 나누기 → 목적지 등록 틀 → 모듈 뼈대 → 공용 조각 옮기기 → **셸 이동** → 통계 · 연결 · 설정 · 퀴즈 차례로 떼기 → 문서 | 결정 12 의 고정 순서 호출(연결 루트 · 읽는 중 화면 · 모델 확인 대화상자 · 퀴즈 오버레이)은 등록이 아니라 직접 호출이라, 셸이 `:feature:notes` 에 남은 채 기능을 떼면 노트가 그 기능을 의존하게 된다(FM-2 위반). 셸이 먼저 `:app` 에 가 있으면 기능을 뗄 때 `:app` 의 import 만 바뀐다 | 메인 | 정함 |
| 18 | 2026-10-11 · feature-modules | 등록 함수는 셸 틀 계약 값(틀) 밖으로 기능 자기 상태 · 입구와 결정 13 으로 묶어 넘기는 화면용 값을 인자로 받는다. FM-4 의 '틀 값은 표의 것뿐' 은 틀에 대한 말이다 | 그 값들은 기준 커밋에도 있던 값이고 결정 13 이 정한 길이다. 문서가 두 규칙의 경계를 적지 않아 단위 검수가 구현을 문서 위반으로 읽었다(구현이 맞음) | 메인 | 정함 |
| 19 | 2026-10-12 · feature-modules | 열린 드로어에서는 M3 드로어가 뒤로가기를 받아 예측 뒤로가기 연출(늘림)이 살아 있다 — 셸 내용을 자식 뒤로가기 디스패처로 감싸 모달 드로어가 열려 있을 때 끈다. 우리 핸들러를 내용 맨 끝에 두지 않는다 | 내용 맨 끝 핸들러는 FM-10 ① 을 지키지만 M3 연출을 없앤다. 자식 디스패처는 ① 과 연출을 함께 지킨다(공식 문서 근거 · 맞바꿀 것 없음) | 메인 | 정함 |
| 20 | 2026-10-12 · feature-modules | 결정 19 의 자식 디스패처 켜고 끔은 그 프레임의 컴포지션에 바로 반영한다(효과로 미루지 않는다) | 효과로 미루면 드로어 settle 프레임 안에 틈이 생긴다 — 열린 직후 pop 이 이기거나, 닫힌 직후 받는 핸들러가 없어 앱이 끝날 수 있다 | 메인 | 정함 |
| 21 | 2026-10-12 · feature-modules | 기능을 다 떼면 둘 이상이 쓸 문구 다섯과 ViewModel 시험 가능하게 · 나누기가 만든 공용 조각(스낵바 문구 · 실패 로그 · 스낵바 알림 · 글자 크기 매핑)은 공용 조각 옮기기에서 `:core:ui` 로 옮긴다. 설정만 쓰는 끊기 대화상자 모델은 설정 떼기가 가져간다 | 공용 조각 표가 기준 커밋에서 센 것이라 빠져 있었다. 남겨 두면 뒤 기능 떼기가 노트를 의존해야 해 FM-2 를 깬다 | 메인 | 정함 |
| 22 | 2026-10-12 · feature-modules | 기능을 옮기면 낡는 문서(노트 `AGENTS.md` 의 그 기능 문단 · `app/AGENTS.md` 의 등록 경로 · 디자인 문서의 파일 칸)는 그 기능을 옮기는 단위가 같이 고친다 — 기능 문단은 새 모듈 `AGENTS.md` 로, 나머지는 경로 · 이름만 바꾸고 `## 바뀐 기록` 에 한 줄. 새 지도 전체를 맞추는 것은 문서 단위(FM-15)다 | 옮긴 단위가 무엇이 바뀌었는지 가장 잘 알고, 뒤 단위 검수가 낡은 문서를 근거로 읽지 않는다 | 메인 | 정함 |
| 23 | 2026-10-12 · feature-modules | 화면이 아직 안 쓰는 공통 액션 문구 `ds_action_confirm` · `ds_action_retry` 는 지우지 않는다. FM-11 의 '쓰지 않는 문구 0' 에서 이 둘만 예외이고, 시험의 남겨 둔 목록에 이유와 함께 이름으로 든다 | `DESIGN.md` 공통 문구 표가 정한 공통 액션 어휘다. 화면이 쓰게 되면 시험이 목록에서 지우라고 알린다 | 메인 | 정함 |
| 24 | 2026-10-12 · feature-modules | 리더 끝 CTA 는 퀴즈 기능의 화면이라 노트 등록은 리더 끝 슬롯(노트 id 를 받는다) 하나만 내놓고, `:app` 셸이 그 슬롯에 퀴즈 CTA 카드를 넣는다 | 노트는 퀴즈 모듈을 모른다(FM-2). 셸은 모든 기능을 알므로 결정 13 의 '`:app` 이 묶어 넘기는 값' 과 같은 길이다 | 메인 | 정함 |

### 안 하는 것

| 안 한다 | 왜 (한 마디) |
|---|---|
| 사용자가 보는 동작 · 모양 바꾸기 | 범위 밖 — 문구만 `screen-copy` 로 바꾼다 |
| 목적지 키 바꾸기 · 퀴즈를 백스택 목적지로 만들기 | 뒤로가기 순서(FM-10)와 기존 시험이 키에 기대고 있다 |
| 백스택 저장 · 프로세스 사망 복원 | 지금도 없다. 리뷰가 사람에게 넘긴 결정 거리다 |
| `:core:*` 의 데이터 · 도메인 경계 바꾸기 | 화면 모듈만 나눈다. 마지막 동기화도 도메인이 아니라 `:core:ui` 홀더로(결정 8) |
| 기능 문서 RC · SY · MD · NF · BR · NT 의 동작 바꾸기 | 동작 그대로 — 시험만 이름을 지키며 옮긴다 |
| Hilt 계측 기반 · 새 라이브러리 | 결정 11 · 기존 카탈로그 줄만 쓴다 |

## 명세

### 모듈 지도

| 모듈 | 무엇 (나누기 전 `:feature:notes` 안 자리) | 의존하는 것 | 문구 접두사 |
|---|---|---|---|
| `:feature:notes` | 홈 · 북마크 · 목록 패널 내용 · 리더 · 연결 띠(홈 · 목록 패널만 씀) · 동기화 알림 · 학습 기록 입력 (`screen/` 의 홈 · 목록 패널 · 리더 · `bookmarks/` · `mapper/` · 노트 화면 모델) | feature 컨벤션 넷 + `:core:ui` | `notes_` (그대로) |
| `:feature:connect` | 첫 실행 연결 화면 · 쌓은 연결 화면 · 바꾸기 · 받아들이기 대화상자 · 연결 판정 순수 함수(`screen/connect/` 에서 공용 조각을 뺀 나머지 · `element/Connect{Back, Confirm, Entry, LinkCheck}`) | 같음 | `connect_` |
| `:feature:settings` | 설정 화면 · 저장소 칸 · 끊기 대화상자 · 모델 카드 · 모델 대화상자 · 글자 크기 · 학습 기록 켜기 · 토큰 편집 판정(`SettingsScreen` · `RepoSection` · `ModelCard` · `ModelDialogs` · `TokenEdit` · `TokenMask` · 연결 주소 만들기) | 같음 | `settings_` |
| `:feature:quiz` | 퀴즈 설정 · 생성 중 · 풀기 · 결과 · 실패 · 오버레이 · 리더 끝 CTA · 문항 화면 모델 (`quiz/` · `QuizQuestionUi`) | 같음 | `quiz_` |
| `:feature:stats` | 통계 · 지난 요약 (`screen/stats/` · `stats/`) | 같음 | `stats_` |
| `:feature:editor` (local-notes 에서 더함) | 기기 노트 편집기 · 붙여넣기 · 가져오기 그릇 · 나가기 · 지우기 확인 · 공유 받은 글을 편집기로 잇기 (나누기 뒤 새로 — `local-notes` brief 정하지 않은 것 11) | 같음 | `editor_` |
| `:core:ui` (새) | 두 기능 이상이 쓰는 것 — 아래 공용 조각 표 | `:core:{common, design-system, domain, navigation}` | `ui_` |
| `:core:navigation` | 지금 것 + 리더 열기 명령 · 탭 전환 명령 · 탭 종류 (`navigation/OpenReader` · `TabSwitch` · `element/NotesTab`) | 그대로 | — |
| `:app` | 셸 — 시작 분기 · Scaffold · 드로어/고정 패널 틀 · 플로팅 내비 · 맨 위로 · 모델 알약 · 상단 바 행동 · NavDisplay · 오버레이 자리 · 뒤로가기 순서 · 종료 안내 · 스낵바 수집 · 알림 권한 요청 · 테마 · 창 크기 판정 (`shell/` · `NotesRoute` · `element/ConnectStart` · `ScreenCleanup`) | 모든 feature + core | `shell_` (파이썬 시험) |

괄호 안 자리는 나누기 전 경로다. 떼고 난 지금은 모듈마다 자기 패키지 뿌리 아래에 있다 — 예: 연결은 `:feature:connect` 의 `screen/` · `element/`(`screen/connect/` 가 아니다). 지금 자리의 정본은 그 모듈 소스와 `AGENTS.md` 다.

공용 조각 — 기준 커밋에서 패키지 사이 참조를 전수로 세어 두 기능 이상(셸 포함)이 쓰는 것:

| 조각 | 지금 자리 | 쓰는 곳 |
|---|---|---|
| 토큰 입력 칸 · 만드는 법 · 만들기 버튼 · 확인 중 줄 · 알림 상자 · 고정폭 글자 · 진행 막대 | `screen/connect/ConnectParts.kt` 일부 | 연결 · 설정(진행 막대는 노트도) |
| 연결 아이콘 | `screen/connect/ConnectIcons.kt` | 연결 · 설정 · 노트 |
| 한글 어절 줄바꿈 글자 | `screen/connect/KeepWords.kt` | 연결 · 설정 · 노트 |
| 외부 브라우저 열기 · 토큰 페이지 주소 · 모듈 manifest 의 브라우저 찾기 | `screen/connect/ExternalLink.kt` · `AndroidManifest.xml` | 연결 · 설정 |
| 토큰 실패 종류와 문구 | `element/ConnectIssue.kt` 일부 | 연결 · 설정 |
| 저장소 카드 판정 · 마지막 동기화 | `element/RepoCard.kt` | 설정 · 노트(목록 패널 배지) |
| 토큰 입력 홀더 · 비밀 글자 | `element/TokenDraft.kt` | 연결 · 설정 · 셸 |
| 모델 상태 → 카드 · 알약 · CTA 판정 · 상태 색 · 미리보기 틀 | `element/ModelStatusUi.kt` · `screen/ModelCard.kt` 일부 | 설정 · 퀴즈 · 노트 · 셸 |
| 일회성 신호 판정 | `element/NotesSignal.kt` | 모든 ViewModel · 셸 |
| 공용 버튼 넷 · 본문 최대 폭 | `screen/SettingsScreen.kt` 일부 | 노트 · 연결 · 설정 · 퀴즈 |
| 상단 바 | `shell/NotesTopBar.kt` | 셸 · 연결(쌓은 화면) |
| 대화상자 틀(폭 · 모서리 · 여백 · 버튼 줄) | `screen/connect/ConnectDialogs.kt` 일부 | 연결(바꾸기 · 받아들이기) · 설정(끊기) |
| 북마크 시각 라벨 | `screen/bookmarks/BookmarkTimeLabel.kt` | 노트 · 통계 |
| 화면 상태 홀더(스크롤 다섯 · 리더 원문 펼침 · 토큰 draft) 타입 | `shell/NotesScrollState.kt` · `NotesDrafts` | 셸이 들고 기능 화면이 받는다 |
| 셸 틀 계약 타입(ShellFrame · ShellDestinations 별칭 · 씬 가장자리 modifier) | `frame/ShellFrame.kt` | 셸 · 기능별 등록 파일 넷 — 화면 상태 홀더를 가져다 쓰므로 `shell/NotesScrollState.kt` 와 같은 단위에서 옮긴다 |
| 기능 사이 신호 계약 · 마지막 동기화 홀더 | (새) | FM-6 · 결정 8 · 13 |
| 스낵바 문구 `NotesMessages` 와 그 Android 구현 | `NotesMessages.kt` (ViewModel 시험 가능하게가 만듦) | 노트 · 설정 · 퀴즈 ViewModel |
| 실패 로그 `FailureLog` 와 그 Android 구현 | `FailureLog.kt` (ViewModel 시험 가능하게가 만듦) | 노트 · 설정 · 퀴즈 · 연결 · 셸 ViewModel |
| 스낵바 알림 `NoticeUi` | `element/NotesUiState.kt` 일부 | 노트 · 설정 ViewModel · 셸 스낵바 수집 |
| 글자 크기 매핑(`FontScaleUi` 의 두 방향 변환) | `element/FontScaleUi.kt` | 셸 · 설정 ViewModel |

표의 마지막 네 행은 기준 커밋 뒤 ViewModel 시험 가능하게 · ViewModel 나누기가 만들었거나 둘 이상이 쓰게 된 조각이고, 공용 조각 옮기기에서 `:core:ui` 로 갔다(결정 21). 끊기 대화상자 모델(`DisconnectDialogUi`)은 같은 파일에 있었지만 설정만 쓰므로 공용 조각이 아니다 — 설정 떼기가 가져갔다. 이 표와 `notes-tests/test_feature_modules.py` 의 공용 조각 목록은 같은 조각을 든다.

### 셸 틀 계약

등록된 목적지 화면은 셸에게서 아래 값만 받는다. 값의 모양(State · 람다 · 묶음)은 구현이 정한다.

| 값 | 왜 셸이 주나 |
|---|---|
| 위 · 아래 · 끝 여백 | 알약 예약 · 플로팅 바 · 레일 예약이 셸의 계산이다 |
| 두 단 여부 | 펴진 고정 패널을 빼고 셸이 잰다 |
| 쌓이는 씬 여부(씬 가장자리 그림) | 넓은 창에서는 켜지 않는다 |
| 화면 상태 홀더(스크롤 · 원문 펼침 · 토큰 draft) | 창 등급 분기 위에 있어야 접기 · 펴기에 남는다 |
| 스낵바 호스트 | 결정 14 |

기능 자기 상태 · 입구, 그리고 결정 13 으로 `:app` 이 묶어 넘기는 화면용 값(연결 있음 · 셸 뒤로 · 알림 설정 열기 같은 것)은 이 틀 밖의 등록 함수 인자다 — 틀에 넣지 않고, 이 표의 '이 값만' 에 들지 않는다(결정 18). 리더 끝 CTA 도 틀 밖이다 — 노트 등록은 리더 끝 슬롯(노트 id 를 받는다) 하나만 내놓고 `:app` 셸이 거기에 퀴즈 CTA 카드를 넣는다. 노트는 퀴즈 모듈을 모른다(결정 24).

모든 등록 화면은 등록 틀이 한꺼번에 공유 전환 스코프로 감싼다(기능이 빠뜨려도 전환이 꺼지지 않게).
연결 화면의 등록은 기본 내용 키를 바꾸지 않는다(넓은 창 전체 폭 판정 RC-14 가 그 키로 알아본다).

### ViewModel 경계

지금 `NotesViewModel` 이 든 상태 묶음이 어디로 가나. ConnectViewModel · StatsViewModel 은 그대로다.

| 상태 묶음 (지금 `NotesUiState` 칸) | 갈 곳 |
|---|---|
| 백스택 파생(`backStack` · `activeNoteId` · `canPop` · `isNavSettled` · `selectedTab`) · 종료(`exitHintId` · `exitRequestId` · 두 번 누르기 창) · 시작 분기(`connection` · `seedPending`) · 연결이 바뀔 때 화면 정리 · 테마에 넣을 글자 크기 | 셸 ViewModel (`:app`) |
| 범주 · 검색 · 펼침 · 최근 · `notesLoaded` · 북마크와 해제 되돌리기 · 동기화(`isLoading` · 실패 · 반영 알림 · `awaitingNotes` · 새로고침) · 마지막 동기화 쓰기 · 리더 스크롤 요청 · 학습 기록 입력(보임 · 읽은 비율 · 퀴즈 덮음) | `NotesViewModel` (`:feature:notes`) |
| 토큰(`hasToken` · `maskedToken` · `tokenEdit` · 지우기 되돌리기) · 글자 크기 바꾸기 · 학습 기록 켜기 · 지우기 · 모델 받기 조작 · 알림 꺼짐 · 끊기 대화상자 · 저장소 칸 | `SettingsViewModel` (`:feature:settings`) |
| 퀴즈 상태 · 자동 진행 타이머 · 그만두기 되돌리기 · 생성 작업 · 퀴즈 결과 기록 | `QuizViewModel` (`:feature:quiz`) |
| 모델 상태 | 읽는 곳(셸 알약 · 설정 카드 · 퀴즈 CTA · 통계)마다 UseCase 로 읽는다 |

시작 분기는 셸 상태(연결 · 시드)와 노트 상태(`notesLoaded` · 노트 수)를 `:app` 이 같은 프레임에 묶어 판정한다. 셸 ViewModel 은 노트 목록을 따로 읽지 않는다.

### 문구 자리

결정 15 의 규칙으로 정한다. **표와 결정 15 가 어긋나면 결정 15 가 먼저다** — 표는 큰 묶음을 미리 본 것이고, 문구마다의 자리는 그 문구를 참조하는 모듈 수가 정한다(FM-11 시험).

기준 커밋의 이름으로 본 큰 묶음(옮기기 전 이름의 기록이다):

| 지금 이름 | 갈 곳 |
|---|---|
| `notes_stats_*` | `:feature:stats` `stats_*` |
| `notes_quiz_*` (CTA 문구 `notes_quiz_cta_*` 는 공용 판정이 써서 빼고 · 스낵바 문구도 빼고) | `:feature:quiz` `quiz_*` |
| `notes_settings_*` · `notes_repo_*` · 모델 카드 · 모델 대화상자 · 끊기 대화상자 문구 (목록 패널이 쓰는 것과 스낵바 문구는 빼고) | `:feature:settings` `settings_*` |
| `notes_connect_*` · `notes_entry_*` · `notes_link_*` · 바꾸기 · 받아들이기 대화상자 문구 | `:feature:connect` `connect_*` |
| `notes_nav_*` · `notes_pill_*` · `notes_exit_hint` · 셸만 쓰는 `notes_action_*` | `:app` `shell_*` |
| 둘 이상이 쓰는 것 — `notes_app_title` · `notes_token_howto_*` · `notes_quiz_cta_*` · 목록 패널과 셸이 쓰는 `notes_settings` · 모델 상태 판정이 쓰는 `notes_model_*` · 스낵바 문구 열다섯(아래 표) 등 | `:core:ui` `ui_*` |
| 나머지(`notes_home_*` · `notes_bookmark_*` · `notes_panel_*`)와 목록 패널만 쓰는 배지(`notes_settings_token_check_badge` · `notes_model_badge_*`) | `:feature:notes` 그대로 |

공용 조각 옮기기 뒤 지금 이름으로 본, 위 표만 봐서는 자리를 틀리기 쉬운 문구:

| 지금 이름 | 자리 | 왜 |
|---|---|---|
| 스낵바 문구 열다섯 — `ui_sync_failed` · `ui_sync_applied` · `ui_sync_changed` · `ui_sync_deleted` · `ui_bookmark_added` · `ui_bookmark_removed` · `ui_quiz_abandoned` · `ui_settings_token_saved` · `ui_settings_token_cleared` · `ui_settings_studylog_cleared` · `ui_model_deleted` · `ui_model_paused_snack` · `ui_model_cancelled_snack` · `ui_model_discarded_snack` · `ui_repo_disconnect_failed` | `:core:ui` | 문구를 고르는 구현(`NotesMessages` 의 Android 구현)이 `:core:ui` 에 있어 참조하는 모듈이 `:core:ui` 하나다. 접두사로는 노트 · 퀴즈 · 설정 몫으로 보이지만 결정 15 가 먼저다 |
| 앞당겨 옮긴 다섯 — `ui_settings`(목록 패널 · 셸 상단 바) · `ui_model_percent`(셸 알약 · 목록 패널 · 설정 모델 카드) · `ui_token_browser_missing` · `ui_token_classic_warn` · `ui_token_verifying`(연결 · 설정) | `:core:ui` | 공용 조각 옮기기 때는 `:feature:notes` 한 모듈만 참조해 파이썬 시험의 앞당긴 목록(`PLACED_AHEAD`)에 두었다. 셸 · 연결 · 설정을 떼며 다섯 모두 둘 이상이 참조하게 되어 그 목록은 이제 비었다. 목록에 다시 이름을 넣으면 시험이 오타 · 지운 문구 · 참조 0 을 잡는다 |
| `ui_action_refresh` | `:core:ui` | 상단 바(`:core:ui`)와 홈 빈 화면(노트)이 쓴다 |
| `notes_settings_token_check_badge` · `notes_model_badge_missing` · `notes_model_badge_failed` | `:feature:notes` | 목록 패널 한 곳만 쓴다 — 설정 · 모델 문구처럼 보여도 옮기지 않는다 |

시험 · 템플릿 README · 문서가 이름으로 묶은 문구(지금 이름 — 두 시험 모두 모든 모듈의 `strings.xml` 에서 이름으로 찾는다, FM-12): `public-repos/tests/test_template.py` 의 앱 용어 일곱(`connect_entry_create` · `connect_link_label` · `connect_action` · `connect_token_title` · `ui_token_howto_*` 셋) ·
`notes-tests/test_brand_title.py` 의 제목 다섯(`ui_app_title` · `ui_settings` · `shell_nav_bookmark` · `shell_nav_stats` · `shell_stats_history_title`) ·
`model-download` MD-8 확인 칸의 리소스 이름 둘 · 디자인 문서 문구표의 리소스 이름 · Kotlin 단위 시험의 `R.string` 참조.

### 동작

| ID | 동작 | 받아들임 기준 | 확인 |
|---|---|---|---|
| FM-1 | 화면은 모듈 지도대로 다섯 기능 모듈에 산다 | `settings.gradle.kts` 에 `:feature:{notes, connect, settings, quiz, stats}` 와 `:core:ui` 가 있다. 모듈 지도의 화면 파일이 그 모듈 `src/main` 에만 있고 `:feature:notes` 에 연결 · 설정 · 퀴즈 · 통계 화면 파일이 0개다. 공용 조각 표의 조각이 `:core:ui`(탭 · 리더 열기 명령은 `:core:navigation`)에 있다 | 파이썬: `FM-1 모듈 지도의 모듈이 모두 등록돼 있다` · `FM-1 공용 조각은 core:ui 와 core:navigation 에 산다` · `FM-1 화면은 모듈 지도대로 산다`(화면 파일 자리 — 마지막 기능 떼기인 퀴즈 떼기가 더했다) |
| FM-2 | 기능 모듈은 서로를 의존하지 않는다 | 다섯 기능 모듈과 `:core:ui` 의 선언 의존과 컨벤션이 넣는 의존을 닫힘으로 따라가 `:feature:*` 가 0개다. 교차 확인: 각 기능 모듈의 `dependencies --configuration debugRuntimeClasspath` 출력에 `project ':feature:` 가 0줄이고, 같은 명령을 `:app` 에 돌리면 다섯 줄 모두 나온다(헛통과 아님) | 파이썬: `FM-2 기능 모듈은 서로를 의존하지 않는다` · 명령: `./gradlew :feature:<모듈>:dependencies --configuration debugRuntimeClasspath \| grep "project ':feature:"` 0줄 · `:app` 은 5종 |
| FM-3 | 기능 모듈 · `:core:ui` 는 데이터 층 · 모델 구현을 모른다 | 같은 닫힘에 `:core:data` · `:core:network` · `:core:database` · `:core:ai` · `:core:format` 이 0개다 | 파이썬: `FM-3 기능 모듈은 데이터 층을 모른다` |
| FM-4 | 목적지 화면은 그 기능 모듈이 등록하고, 셸은 등록 묶음을 셸 틀 계약의 값만 넘겨 그린다 | `NotesNavKey` 의 키 7개마다 등록된 화면이 정확히 하나다. 셸 파일에서 목적지 화면 Composable(`HomeScreen(` · `BookmarksScreen(` · `NoteReaderScreen(` · `SettingsScreen(` · `StatsRoute(` · `StatsHistoryRoute(` · `ConnectRoute(`)을 부르는 줄이 0이다 — 이 0 에는 결정 12 의 고정 순서 호출(퀴즈 오버레이 · 목록 패널 · 상단 바 행동 · 모델 확인 대화상자 · 연결 루트 · 뒤로가기 핸들러)이 **들지 않는다**. 등록 화면이 셸에게서 받는 틀 값은 셸 틀 계약 표의 것뿐이고, 기능 자기 상태 · 입구와 결정 13 으로 묶어 넘기는 값은 틀 밖 등록 함수 인자다(결정 18). 창 등급이 바뀌어도(접기 · 펴기) 리더 스크롤 · 원문 펼침 · 토큰 draft 가 남는다 | JVM: `FM-4 모든 목적지 키에 화면이 하나씩 등록된다` · 계측: `FM-4 창 등급이 바뀌어도 리더 스크롤과 원문 펼침과 토큰 입력이 남는다` · 리뷰 grep |
| FM-5 | 셸은 `:app` 에 살고 순서를 한 파일이 고정한다 | `NavDisplay(` 호출 · `ModalNavigationDrawer(` · 플로팅 내비 배치 · 뒤로가기 7단의 핸들러가 `:app` 에만 있고 `feature/` 아래 0건이다. 결정 12 의 고정 순서 호출이 `:app` 셸 파일 한 곳에 있고, 오버레이 · 패널 · 핸들러를 순서 없는 집합(멀티바인딩)으로 받지 않는다. `layerBackdrop` 상자 안에는 NavDisplay 하나이고 기능이 주는 오버레이도 그 형제 자리에서 그린다. `MainActivity` 가 부르는 루트 화면이 `:app` 에 있다 | 리뷰 grep: `feature/` 아래 `NavDisplay(` · `ModalNavigationDrawer(` 0건 · 오버레이 · 핸들러 타입의 `@IntoSet` 0건 · FM-10 |
| FM-6 | 기능 사이 이동은 목적지 키로만, 이동이 아닌 신호는 결정 13 의 계약으로만 | 아래 열 갈래가 지금과 같게 일어나고, 그 신호가 지나는 타입이 `:core:*` 에 있다 — ① 통계 → 리더 열기(이어 읽을 위치 포함) ② 통계 다시 풀기 → 리더를 열고 퀴즈 설정(전체를 읽을 수 없는 노트면 리더만) ③ 홈 띠 · 목록 패널 · 설정 → 연결 화면, 연결 확인 중이면 막힘 ④ 설정 열기 → 토큰 편집 정리 ⑤ 리더 끝 CTA → 퀴즈 설정, 모델이 없으면 → 모델 받기 확인 대화상자 → 알림 권한 요청 → 받기 시작 ⑥ 앱이 다시 보일 때마다 알림 허용을 다시 읽어 설정 카드 · CTA 가 같은 값을 본다 ⑦ 퀴즈가 리더를 덮는 동안 읽기 세션이 멈춘다(같은 틱 — 컴포지션을 거치지 않는다) ⑧ 퀴즈가 노트의 읽을 수 있음 · 제목 · 범주를 쓴다 — 퀴즈 결과 기록의 범주는 퀴즈를 시작할 때의 값이다 ⑨ 연결이 바뀌면 스택 `[Home]` · 퀴즈 닫기 · 마지막 동기화 비우기가 같은 틱에 ⑩ 토큰 편집 중에는 맨 위로가 숨는다 ⑪ 설정에서 학습 기록을 지우면 노트의 북마크 해제 되돌리기가 지운 기록의 이벤트 id 를 버린다(되돌리기가 지운 기록을 건드리지 않는다) | JVM: `FM-6 통계에서 노트를 열면 리더가 열린다` · `FM-6 통계 다시 풀기는 리더를 열고 퀴즈 설정을 띄운다` · `FM-6 연결 확인 중에는 연결 화면을 열지 않는다` · `FM-6 설정을 열면 토큰 편집을 정리한다` · `FM-6 모델이 없을 때 CTA 는 받기 확인을 띄운다` · `FM-6 퀴즈가 리더를 덮는 동안 읽기 세션이 멈춘다` · `FM-6 연결이 바뀌면 스택과 퀴즈와 마지막 동기화를 같이 정리한다` · `FM-6 토큰 편집 중에는 맨 위로를 숨긴다` · `FM-6 학습 기록을 지운 뒤 북마크 되돌리기는 기록을 건드리지 않는다`(위 FM-6 이름은 `FeatureSignalsTest`) · ⑨ 는 `ConnectStartTest` 의 RC-23 넷(`RC-23 확정되면 종류와 무관하게 스택은 홈 하나다` · `RC-23 같은 저장소 변경에서는 노트가 지워지지 않아 퀴즈를 닫지 않는다` · `RC-23 끊으면 연결 화면 루트다` · `RC-23 앱을 켠 첫 연결 상태와 같은 연결의 되풀이는 화면을 정리하지 않는다`) · 수동(에뮬레이터: ⑤ 권한 요청 · ⑥ 알림 껐다 켜고 돌아오기) |
| FM-7 | ViewModel 은 자기 기능의 상태만 들고, 모두 Activity 를 owner 로 받는다 | ViewModel 경계 표대로다 — `:feature:notes` 에서 `QuizUiState` · `TokenEditState` · 모델 받기 · 취소 · 지우기 UseCase 참조 0건, 셸 ViewModel 이 노트 목록 · 퀴즈 · 토큰 상태를 들지 않는다. 기능 · 셸의 모든 `hiltViewModel(` 호출이 Activity owner 를 명시하고 목적지별 ViewModel 데코레이터가 0건이다. 동기화 UseCase(`ObserveSyncRunsUseCase`)를 생성자로 받는 클래스가 `NotesViewModel` 하나다 | 파이썬: `FM-7 기능 ViewModel 은 Activity 를 owner 로 받는다` · `FM-7 동기화 UseCase 를 받는 클래스는 NotesViewModel 하나다` · JVM: `FM-7 동기화는 구독이 여럿이어도 한 번 돈다` · 컴파일(FM-2 가 막는다) |
| FM-8 | ViewModel 은 시간 · 문구를 주입받는다 | 모든 `*ViewModel.kt`(`StatsViewModel` 의 기록 시각 포함)에서 `SystemClock` · `System.currentTimeMillis` · `android.util.Log` · `@ApplicationContext` · `getString(` 0건. 모든 ViewModel 이 JVM 단위 시험에서 가상 시간으로 만들어진다 | 파이썬: `FM-8 ViewModel 은 시간과 문구를 직접 읽지 않는다` |
| FM-9 | ViewModel 시험이 시간에 기대는 규칙을 지킨다 | 가상 시간 JVM 시험이 있다 — 루트에서 뒤로가기 두 번(창 안 = 종료 요청 · 창 밖 = 안내 다시) · 퀴즈 자동 진행(늦게 온 타이머가 문항을 건너뛰지 않음) · 퀴즈 그만두기 되돌리기(창 안 복귀 · 창 밖 사라짐) · 토큰 지우기 되돌리기 · 북마크 해제 되돌리기 · 본 신호는 다시 나오지 않음 | JVM: `FM-9 루트에서 창 안에 두 번 누르면 종료한다` · `FM-9 창이 지나면 다시 안내한다` · `FM-9 늦게 온 자동 진행은 문항을 건너뛰지 않는다` · `FM-9 그만둔 퀴즈는 창 안에서만 되돌린다` · `FM-9 지운 토큰을 되돌린다` · `FM-9 해제한 북마크를 되돌린다` · `FM-9 본 신호는 다시 나오지 않는다` |
| FM-10 | 뒤로가기 7단 순서를 계측 시험이 지킨다 | 에뮬레이터에서 상태 없는 셸에 가짜 상태를 넣고(결정 11), 단마다 앞 단이 없는 상태에서 뒤로가기 한 번 → 그 단의 결과만 일어난다: ① 열린 드로어가 닫힌다 ② 퀴즈 오버레이(설정 · 풀기)가 닫힌다 ③ 전환 중 뒤로가기는 아무 일도 없다 — 예측 뒤로가기 제스처 도중 포함 ④ 쌓인 화면이 pop 된다 ⑤ 여는 중인 드로어가 닫힌다 ⑥ 펼친 고정 패널이 접힌다(넓은 창) ⑦ 루트에서 뒤로가기 인텐트가 나가고, 종료 요청 id 가 오르면 주입한 종료 콜백이 불린다(창 안 · 밖 판정은 FM-9). 두 단이 겹치면 순위가 높은 쪽만 일어난다(① + ④ · ② + ④ · ⑥ + ⑦). 7단 밖: 설정 토큰 편집 중 뒤로가기는 편집 취소이고 pop 이 아니다. 시험 몸통은 ViewModel 나누기 · 등록 틀 · 셸 이동 뒤에도 바뀌지 않고 같은 이름으로 통과한다 | 계측: `FM-10 열린 드로어가 먼저 닫힌다` 외 단마다 하나 · `FM-10 예측 뒤로가기 도중 전환 중이면 삼킨다` · `FM-10 겹치면 순위가 높은 쪽만 일어난다` · `FM-10 토큰 편집 중 뒤로가기는 편집을 취소한다` · 수동(연결 화면 엔트리 뒤로가기 · 대화상자 뒤로가기 — 앱 Hilt 그래프가 있어야 그린다) |
| FM-11 | 문구는 참조하는 모듈 자리에 그 접두사로 산다 | 결정 15 대로다 — 문구마다 그것을 참조(`R.string`)하는 모듈이 하나면 그 모듈, 둘 이상이면 `:core:ui`(또는 design-system)에 정의돼 있다(공통 액션 `ds_action_*` 는 참조 수와 상관없이 design-system). 모든 라이브러리 기능 모듈 · `:core:ui` 에 `resourcePrefix` 가 있고(lint 가 접두사를 강제) `lintDebug` 에 리소스 이름 경고 0. 접두사를 빼먹은 새 라이브러리 모듈은 lint 가 못 보므로 파이썬 시험이 막는다. `:app` 은 결정 16 대로 셸 문구만 `shell_` 로 시작한다. 모든 `strings.xml` 을 합쳐 같은 이름이 두 번 정의된 문구 0 · 정의만 있고 아무도 안 쓰는 문구 0 — 예외는 `DESIGN.md` 공통 액션 표의 `ds_action_confirm` · `ds_action_retry` 둘뿐이다(결정 23) | `./gradlew lintDebug` · 파이썬: `FM-11 라이브러리 모듈마다 문구 접두사가 있다` · `FM-11 문구는 참조하는 모듈 자리에 산다` · `FM-11 셸 문구는 shell_ 로 시작한다` · `FM-11 문구 이름은 모듈을 넘어 겹치지 않는다` · `FM-11 쓰지 않는 문구가 없다` |
| FM-12 | 시험 · README · 문서에 묶인 문구가 같이 옮겨진다 | 문구를 이름으로 찾는 파이썬 시험(`test_brand_title` · `test_template` · `test_screen_copy`)이 특정 `strings.xml` 경로 · 소스 뿌리가 아니라 모든 모듈의 `strings.xml` · 소스에서 이름으로 찾는다. 리소스 이름을 글자로 든 문서(디자인 `screen-copy-rows.md` 의 리소스 이름 칸 · 디자인 `screen-copy.md` 의 끝말 예외 · 동결 예외 이름)도 옮긴 이름을 따른다. 문구 자리 아래 목록의 시험이 새 이름으로 통과하고 시험 이름은 그대로다. 레포 문서 · 시험 · 코드에 옮긴 옛 이름이 0건이다(`## 바뀐 기록` 줄 제외) | `python3 -I -m unittest discover notes-tests` · `python3 -I -m unittest discover public-repos/tests` · 리뷰 grep |
| FM-13 | 시험은 이름을 지키며 옮겨지고 수가 줄지 않는다 | 기준 커밋(`9d6f0fd`)의 `:feature:notes` 시험 209쌍(단위 207 · 계측 2 · 이번 작업 자료로 고정한 목록)의 (단순 클래스 이름, 시험 이름)이 나눈 뒤 어느 모듈에든 모두 있다. 시험 대상이 두 모듈로 갈린 시험 클래스는 같은 단순 이름의 클래스로 두 모듈에 나눠 두어도 된다(시험 이름은 그대로 · 클래스를 지우거나 시험 이름을 바꾸지 않는다). 단위는 각 모듈 `testDebugUnitTest` 결과 XML, 계측은 소스의 시험 이름으로 센다. 모듈별 시험 수의 합 ≥ 209 + 이번에 새로 쓴 FM 시험 수 | 명령: 기준 목록과 나중 쌍의 차집합 0 |
| FM-14 | 전체 검증이 통과한다 | 루트 `AGENTS.md` 의 한 번에 검증 명령이 모두 통과한다 — `ktlintCheck` · `:app:assembleDebug` · `test` · `lintDebug` · 파이썬 셋 · 계측(시험이 있는 모듈마다 차례로). 그리고 `:app:assembleRelease`(축소 · 난독화 빌드)가 통과한다 | 명령 |
| FM-15 | 문서가 새 모듈을 안다 | 새 모듈마다 `AGENTS.md` 가 있다. 루트 `AGENTS.md` 의 모듈 지도 · `build-logic/AGENTS.md` 의 모듈 → 컨벤션 표(`:core:ai` 의 `ai_` 포함) · `app/AGENTS.md`(셸) · `feature/notes/AGENTS.md`(남은 것만) · `core/navigation/AGENTS.md`(등록 · 옮겨 온 명령)가 새 지도와 맞다. 계측 명령 줄에 새 모듈이 있고 빈 줄이 없다 | 파이썬: `FM-15 모듈마다 AGENTS.md 가 있다` · `test_hygiene` 통과 · 리뷰 |
| FM-16 | 스낵바는 루트 한 곳에서 받는다 | `:app` 루트(시작 분기 · 창 등급 분기 위)에 스낵바 호스트가 하나다. 설정이 맨 위일 때 난 동기화 실패가 그 자리에서 뜨고, 스낵바 도중 접기 · 펴기를 해도 다 본 신호가 다시 뜨지 않는다. 되돌리기는 신호를 낸 기능에게 돌아간다 | 기존 `NotesSignalTest` · FM-9 · 수동(Fold 에뮬레이터: 설정에서 새로고침 실패 · 스낵바 도중 접기) |

### 안 바뀔 것

| 무엇 | 확인 |
|---|---|
| 기능 문서 `repo-connect` · `sync` · `model-download` · `note-format` · `brand` · `note-template` 의 모든 동작과 그 시험(이름 그대로 · 옮긴 모듈에서 통과) | `test` · 계측 · 파이썬 |
| 뒤로가기 7단 순서(`feature/notes/AGENTS.md` 표) · 대화상자는 자기 창이 뒤로가기를 받는다 | FM-10 · 수동 |
| 플로팅 내비 선택이 백스택에서 파생된다 · 탭 전환 규칙 | 기존 `TabSwitchTest` · `OpenReaderTest` |
| 접기 · 펴기 · 회전에서 리더 스크롤 · 원문 펼침 · 토큰 입력 draft · 퀴즈 진행 · 통계 기간이 남는다 | FM-4 계측 · 수동(Fold 에뮬레이터: 퀴즈 풀기 · 통계 각각 접고 펴기) |
| 홈 ↔ 리더 공유 요소 전환 | 수동(카드 → 리더 → 뒤로) |
| 연결 화면은 넓은 창에서도 목록 칸 · 헤더 없이 전체 폭(RC-14) | 기존 RC-14 시험 |
| 토큰 원문이 상태 · 저장 번들 · `toString` 에 나오지 않는다 | 기존 `TokenDraftTest` · `TokenMaskTest` · `NotesDraftsTest` |
| `layerBackdrop` 상자 안에는 NavDisplay 만 | FM-5 · 수동(유리 내비 · 알약이 자기를 비추지 않음) |
| 통계 집계 · 인사이트는 그 화면이 맨 위일 때만 돈다 | 수동(debug `MyNoteStats` 로그) |
| 동기화는 앱이 살아 있는 동안 화면 재구독으로 다시 돌지 않는다(SY-1) | FM-7 · 기존 SY 시험 |
| 일회성 신호(스낵바 · 되돌리기)가 재생성 뒤 다시 뜨지 않는다 | 기존 `NotesSignalTest` · FM-9 · FM-16 |
| 템플릿 저장소가 있을 때만 입구 구역이 보인다(RC-25) | 기존 RC-25 시험 |
| 외부 링크는 브라우저 앱으로 연다(모듈 manifest 의 브라우저 찾기가 옮긴 모듈에 남는다) | 기존 `ExternalLinkTest` · 수동(토큰 만들기 버튼) |

## 열린 질문

| # | 무엇 | 지금 기본값 | 날짜 · run |
|---|---|---|---|
| 1 | 실기기 수동 확인 — 연결 화면 엔트리 뒤로가기 · 대화상자 뒤로가기 · Fold 접고 펴기 뒤 드로어 뒤로가기 · FM-6 ⑤ 알림 권한 요청 · ⑥ 알림 껐다 켜고 돌아오기 · FM-16 Fold(설정에서 새로고침 실패 · 스낵바 도중 접기)는 셸 이동 뒤 에뮬레이터에서 모두 확인했다. 남은 것은 퀴즈 풀기 · 결과 · 풀기 도중 접고 펴기다 — 에뮬레이터가 퀴즈 모델을 돌리지 못한다(GPU 초기화 실패 · CPU 경로는 지원하지 않는 명령으로 멈춤). 풀기 오버레이가 뒤로가기로 닫히는 것은 계측 FM-10 ② 가 덮는다 | 실기기에서 한 번 본다 | 2026-10-12 · feature-modules |
| 2 | 상태를 통째로 받는 자리 — 셸 본문 · 내비 표시 · 헤더 · 패널 슬롯 · 설정 엔트리가 기능 상태 묶음을 통째로 읽고, 모델 받기 진행이 셸 · 설정 · 퀴즈에 따로 실린다. 셸 본문 · 오버레이 묶음을 기능마다 자기 상태만 받게 다시 짤지, 그 범위 | 그대로 — 회귀가 아니고 사용자가 보는 동작은 같다. 후속 | 2026-10-12 · feature-modules |
| 3 | 통계 지난 요약의 행 펼침이 접고 펴면 닫힌다 — 이 작업 전부터 있던 동작이다 | 그대로 — 후속(통계) | 2026-10-12 · feature-modules |
| 4 | 나누기 전부터 공개였던 설정 선언(연결 주소 만들기 · 토큰 가리기 · 토큰 편집 줄이기 등)을 모듈 안으로 좁힐지 · 시험 고정물(`FeatureFixture`)이 노트 · `:app` · 설정 세 벌로 갈려 서로 어긋날 위험 | 좁히기는 문서 단위가 판단 · 고정물 합치기는 후속 | 2026-10-12 · feature-modules |

## 바뀐 기록

| 날짜 | run | 항목 | 무엇이 |
|---|---|---|---|
| 2026-10-11 | feature-modules | FM-1 ~ FM-15 | 처음 |
| 2026-10-11 | feature-modules · 검토 1 | FM-1 · FM-2 · FM-4 ~ FM-8 · FM-10 ~ FM-16 | 실현성 검토 반영: 공용 조각 전수 표 · 셸 틀 계약 · 고정 순서 호출(결정 12) · 신호 열 갈래 · VM Activity owner(결정 7) · 동기화 구독 구조 시험 · `notesLoaded` 는 노트 VM · 문구 자리를 참조 모듈 수로(결정 15) · `:app` 접두사 근거(결정 16) · FM-2 확인 명령 형식 · FM-13 단순 클래스 이름 · release 빌드 · FM-10 ⑦ 기준과 7단 밖 핸들러 · FM-16 추가. 열린 질문 1 ~ 4 는 사용자 결정으로 닫음(결정 1 · 2 · 3 · 5). 결정 4 를 6 으로 뒤집음(뒤로가기 계측을 VM 나누기 앞으로) |
| 2026-10-11 | feature-modules · 재검토 2 | FM-13 · (결정) · (공용 조각 표) | 결정 6 을 17 로 뒤집음(셸 이동을 공용 조각 옮기기 바로 뒤로) · 공용 조각 표에 대화상자 틀 · FM-13 에 두 모듈로 갈린 시험 클래스를 같은 이름으로 나눠 두는 허용 |
| 2026-10-11 | feature-modules · 단위 검수 반영 | FM-4 · FM-6 · FM-12 · 결정 8 · 결정 18 · (셸 틀 계약 · 공용 조각 표) | 단위 검수(vm-split · nav-registry · copy-apply)가 구현이 맞고 문서가 좁다고 짚은 것: 틀 밖 등록 인자(결정 18 · 결정 13) · 공용 조각 표에 셸 틀 계약 타입(화면 상태 홀더와 같은 단위) · 결정 8 에 셸 ViewModel 의 연결 전이 비우기 · FM-6 ⑧ 범주는 시작 때 값 · ⑪ 학습 기록 지우기와 북마크 되돌리기 · ⑨ 확인 칸을 실제 시험으로 · FM-12 이름으로 찾는 대상에 문구 시험 · 디자인 문서의 리소스 이름. 보이는 동작은 그대로 |
| 2026-10-12 | feature-modules · 공용 조각 옮기기 검수 반영 | FM-1 · (공용 조각 표 · 문구 자리) | 공용 조각 옮기기 명세 검수 의도와 메인 후속(공용 조각 · 문구 자리 · FM-1 시험 이름)을 표에 맞춤: 공용 조각 표에 ViewModel 시험 가능하게 · 나누기가 만든 넷(스낵바 문구 · 실패 로그 · 스낵바 알림 · 글자 크기 매핑) · 끊기 대화상자 모델은 설정 몫 · 문구 자리는 결정 15 가 표보다 먼저 · 스낵바 문구 열다섯은 `:core:ui` · 목록 패널 배지는 노트 · 앞당겨 옮긴 다섯 · 없는 이름 한 줄 지움 · FM-1 확인 칸에 모듈 몫 시험 둘. 뒤 기능 떼기가 표를 보고 거꾸로 옮기지 않게. 보이는 동작은 그대로 |
| 2026-10-12 | feature-modules · 승격 | FM-1 · FM-11 · 결정 19 ~ 24 · 열린 질문 1 ~ 4 · (모듈 지도 · 공용 조각 표 · 셸 틀 계약 · 문구 자리) | run 끝 승격: 드로어 예측 뒤로가기는 자식 디스패처로 M3 연출 유지 · 켜고 끔은 그 프레임에(결정 19 · 20) · 공용 조각 다섯 문구와 셋 조각은 공용 조각 옮기기에서(결정 21) · 옮기면 낡는 문서는 그 단위가(결정 22) · 공통 액션 둘은 FM-11 예외(결정 23 · 받아들임 문장에도) · 리더 끝 CTA 는 노트가 슬롯만 내놓고 셸이 퀴즈 카드를 넣음(결정 24 · 셸 틀 계약 끝 문단). 모듈 지도 괄호는 나누기 전 자리라고 적음 · 앞당긴 목록이 비었음 · 묶인 문구 목록을 지금 이름으로 · FM-1 화면 자리 시험이 이제 있음. 실기기 수동 · 상태 통째로 받는 자리 · 지난 요약 펼침 · 설정 선언 좁히기와 시험 고정물을 열린 질문으로. 보이는 동작은 그대로 |
