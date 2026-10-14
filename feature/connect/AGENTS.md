# AGENTS.md — :feature:connect

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

연결 화면(쌓은 자리 하나 — 첫 실행 루트 자리는 걷혔다, `local-notes` 결정 4 · RC-25) · 바꾸기 · 받아들이기 대화상자 · 연결 판정의 자리다(기능 문서 `feature-modules` 모듈 지도). `:feature:notes` 에서 패키지째 옮겨 왔다.

| 패키지 | 무엇 |
|---|---|
| `com.hancekim.mynote.feature.connect.screen` | 연결 화면(`ConnectRoute` · `ConnectScreen`) · 입구 구역 · 링크 칸 · 바꾸기 · 받아들이기 대화상자 · 읽는 중 화면(`ConnectReadingScreen`) · `ConnectViewModel` · 상태 · 인텐트 · 등록 파일 `ConnectDestinations.kt`(`connectDestinations` — 키 `Connect`) |
| `com.hancekim.mynote.feature.connect.element` | 연결 판정 순수 함수 — `connectBackAction` · `confirmUiOf` · `entryVisible` · `connectSectionsOf` · `noticeWays` · `issueOfProbe` · `issueOfSync` · `linkCheckOf` |

`:app` 셸이 읽는 중 화면을 시작 분기에서 직접 부르고(`shell/NotesShell.kt`) 등록 파일을 묶는다(`shell/NotesNavDisplay.kt`).
연결 화면은 시작 결과가 아니다 — 등록 파일의 쌓은 목적지(`NotesNavKey.Connect`)로만 열린다(`local-notes` 결정 4 · RC-1).
남은 연결 조각 — 홈 · 목록 패널의 연결 띠(`ConnectBand`)는 `:feature:notes`, 설정의 끊기 대화상자(`DisconnectDialog`) · 설정 토큰 확인 판정(`tokenFailureOfProbe`) · 연결 주소 만들기(`connectionLinkOf`)는 `:feature:settings` 에 있다.

## 경계

| 지킬 것 | 지키는 것 |
|---|---|
| `build.gradle.kts` 는 `hancenote.android.feature` 하나 — `:core:{common, design-system, domain, navigation, ui}` · 공통 의존 · 문구 접두사 `connect_` 를 컨벤션이 넣는다 | `build-logic/AGENTS.md` |
| 다른 기능 모듈을 의존하지 않는다. 둘 이상이 쓰는 것은 `:core:ui`, 다른 기능 화면으로는 목적지 키로 간다 | `notes-tests/test_feature_modules.py` FM-2 |
| 데이터 층(`:core:{data, network, database, format}`) · 모델 구현(`:core:ai`)을 모른다 — `:core:domain` 의 UseCase 만 | FM-3 |
| 문구는 이 모듈만 참조하는 것만 둔다(결정 15) | `lintDebug` · FM-11 |

## 연결 화면

`ConnectViewModel` 은 Activity 스코프다 — 연결 화면 흐름(확인 중 · 토큰 갈래 · 첫 동기화 · 확인 대화상자)을 들고, 링크 draft 만 `SavedStateHandle` 에 둔다.
연결 화면을 여는 세 길(홈 첫 실행 빈 상태의 `저장소 연결` · 홈 안내 · 설정)이 같은 상태를 쓴다. 토큰 입력 draft 는 저장하지 않고 `:app` 셸의 `NotesDrafts` 가 든다(`feature/notes/AGENTS.md` 상태 표).
연결 VM 시험 가운데 홈 히어로 저장소 줄(`BR-6`)은 노트 상태를 같이 봐야 해서 `:app` 의 같은 이름 `ConnectViewModelTest` 에 있다.

- **연결 화면 자리는 하나**: 쌓은 화면(`NotesNavKey.Connect` entry + 자체 `NotesTopBar`). 첫 실행 루트 자리는 걷혔다(`local-notes` 결정 4 · RC-25). 셸은 `Connect` 가 top 이면 topBar · 플로팅 내비 · 알약 · 맨 위로 · 드로어 제스처를 숨긴다.
  뒤로가기는 엔트리 안 `BackHandler`(NavDisplay 뒤에 컴포즈되어 이긴다) — 판정은 순수 함수 `connectBackAction`.
- **판정 로직은 순수 함수**(`element/` — 시작 분기 `startDestination` · 화면 정리 `screenCleanupOf` 는 `:app` `shell/`)다: `connectBackAction` · `issueOfProbe`/`issueOfSync` · `linkCheckOf` · `confirmUiOf`. 시험 이름에 항목 ID.
- **입구 구역(RC-25) · 목록 없음 상자의 길(RC-9)** 은 `TemplateRepo?`(앱이 제공, `ConnectViewModel` 생성자 → `ConnectUiState.template`)가 있을 때만 그려진다. 보임 · 구역 순서 · 길 판정은 순수 함수 `element/ConnectEntry.kt`(`entryVisible` · `connectSectionsOf` · `noticeWays`)이고, 확인 대화상자는 입력이 아니다.
  위계는 링크 구역 첫째 하나 · 입구 구역은 그 아래 한 단이다 — 같은 무게 · 가로 두 단은 첫 실행 루트와 같이 걷혔다(RC-25). 링크 구역은 늘 첫 자리라 입구가 나타나고 사라져도 칸이 새로 만들어지지 않는다(RC-25 ⑦-2). 입구 버튼은 VM 을 거치지 않고 `openExternal` 로 외부 브라우저만 연다.
  `openExternal` 은 브라우저 패키지(기본 → Chrome → 처음 찾은 것)를 골라 열어 주소를 맡겠다고 한 다른 앱을 거치지 않는다. `openExternal` 은 `:core:ui` 에 있고, 브라우저를 찾는 데 그 모듈 manifest 의 `<queries>` 가 필요하다 — 지우면 조용히 일반 열기로 돌아간다.

## 명령

```bash
./gradlew :feature:connect:testDebugUnitTest   # 연결 판정 · 연결 VM
./gradlew :app:compileDebugKotlin              # 이 모듈 변경 검증은 :app 컴파일로
```
