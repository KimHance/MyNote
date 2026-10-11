# AGENTS.md — :feature:settings

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

설정 화면 · 저장소 칸 · 끊기 대화상자 · 모델 카드 · 모델 대화상자 · 글자 크기 · 학습 기록 켜기 · 토큰 편집 판정의 자리다(기능 문서 `feature-modules` 모듈 지도). `:feature:notes` 에서 패키지째 옮겨 왔다.

| 패키지 | 무엇 |
|---|---|
| `com.hancekim.mynote.feature.settings` | `SettingsViewModel` · 상태(`SettingsUiState`) · 인텐트(`SettingsIntent`) · 등록 파일 `SettingsDestinations.kt`(`settingsDestinations` — 키 `Settings`) |
| `com.hancekim.mynote.feature.settings.screen` | 설정 화면(`SettingsScreen`) · 저장소 칸 · 토큰 편집기(`RepoSection`) · 끊기 대화상자(`DisconnectDialog`) · 모델 카드(`ModelCard`) · 모델 대화상자(`ModelConfirmDialog` · 지우기 확인) |
| `com.hancekim.mynote.feature.settings.element` | 토큰 편집 판정 순수 함수 — `reduceTokenEdit`(RC-18 전이표 + D37 · D39) · `maskToken` · `maskedTokenTail` · `connectionLinkOf` · `tokenFailureOfProbe`(설정 토큰 확인) · 끊기 대화상자 모델 `DisconnectDialogUi` |

`:app` 셸이 등록 파일을 묶고(`shell/NotesNavDisplay.kt`), 리더 끝 CTA 의 모델 확인 대화상자(`ModelConfirmDialog`)를 직접 부른다(`shell/NotesShell.kt` — 결정 12 의 고정 순서).
그래서 `ModelConfirmDialog` · `SettingsViewModel` · `SettingsUiState` · `SettingsIntent` 는 공개다. `:app` 의 `BackOrderTest` 가 토큰 편집 상태를 꾸미려고 `TokenEditState` 를 쓴다.

## 경계

| 지킬 것 | 지키는 것 |
|---|---|
| `build.gradle.kts` 는 `hancenote.android.feature` 하나 — `:core:{common, design-system, domain, navigation, ui}` · 공통 의존 · 문구 접두사 `settings_` 를 컨벤션이 넣는다 | `build-logic/AGENTS.md` |
| 다른 기능 모듈을 의존하지 않는다. 둘 이상이 쓰는 것은 `:core:ui`, 다른 기능 화면으로는 목적지 키로 간다 | `notes-tests/test_feature_modules.py` FM-2 |
| 데이터 층(`:core:{data, network, database, format}`) · 모델 구현(`:core:ai`)을 모른다 — `:core:domain` 의 UseCase 만 | FM-3 |
| 문구는 이 모듈만 참조하는 것만 둔다(결정 15) | `lintDebug` · FM-11 |

## 설정 화면

`SettingsViewModel` 은 Activity 스코프다(결정 7) — 토큰 편집 · 글자 크기 바꾸기 · 학습 기록 켜기 · 지우기 · 모델 받기 조작 · 끊기를 들고,
마지막 동기화는 `:core:ui` 의 `LastSyncHolder` 에서 읽기만 한다. 토큰 입력 draft 는 저장하지 않고 `:app` 셸의 `NotesDrafts` 가 든다(`feature/notes/AGENTS.md` 상태 표).

- **설정 열기(FM-6 ④)** 는 `Navigator.openSettings(signals)` — 설정 VM 이 신호를 받아 토큰 편집을 정리한다. 새로 만든 VM 은 지난 설정 열기를 다시 처리하지 않는다.
- **연결 화면 열기(③)** 는 설정 VM 이 토큰 확인 중 잠금을 신호 계약에 동기로 쓰고, 노트 VM 이 그 값을 읽어 막는다.
- **토큰 편집 중 뒤로가기는 편집 취소다** — `RepoSection` 의 엔트리 안 `BackHandler` 가 `NavDisplay` 뒤에 컴포즈되어 pop 보다 먼저 받는다.
  7단 밖이고 `:app` 의 `BackOrderTest`(`FM-10 토큰 편집 중 뒤로가기는 편집을 취소한다`)가 지킨다. 토큰 편집 중에는 셸이 맨 위로를 숨긴다(⑩).
- **토큰 확인 실패 갈래** 는 `tokenFailureOfProbe` 가 확인 실패 아홉 갈래를 직접 대응한다(연결 모듈의 `issueOfProbe` 를 부르지 않는다 — FM-2).
  두 판정이 갈라지지 않음은 `:app` 의 `ConnectIssueTest`(`RC-18 설정 편집기와 연결 화면의 토큰 실패 갈래가 같다`)가 지킨다.
- 모델 상태 → 카드 판정(`modelCardUi`) · 상태 색 · 공용 버튼은 `:core:ui` 다. `ModelStatus` 가 `:core:domain` 에 있는 까닭은 `feature/notes/AGENTS.md` `## 퀴즈 모델 상태`.

### 토큰 원문은 ViewModel 밖으로 나가지 않는다

`SettingsUiState` 에 있는 건 `maskedToken`(`ghp_••••••••a3F9`)뿐이다. 입력 중인 draft 와 보기/숨기기는
`NotesDrafts`(저장 안 되는 홀더)다. 비밀값을 상태로 올리면 로그·스크린샷 테스트·
`toString()` 에 흘러나온다. `maskToken()` 은 **길이도 드러내지 않는다** — 원본이 20자든 200자든
가린 결과가 같다.

되돌리기용 이전 토큰은 `SettingsViewModel` 의 **메모리 필드**다. 디스크에 백업을 남기지 않는다.

## 시험

설정 VM 시험은 이 모듈의 `FeatureFixture`(노트 모듈 fixture 의 설정 쪽 사본)로 세운다. 기능 사이 신호 가운데 설정 VM 하나만 보는 것은
이 모듈의 `FeatureSignalsTest`, 노트 VM 과 같이 보는 것(③ 연결 확인 중 · ⑪ 학습 기록 지우기)은 `:app` 의 같은 이름 `FeatureSignalsTest` 에 있다(FM-13).

## 명령

```bash
./gradlew :feature:settings:testDebugUnitTest   # 토큰 편집 판정 · 설정 VM
./gradlew :app:compileDebugKotlin               # 이 모듈 변경 검증은 :app 컴파일로
```
