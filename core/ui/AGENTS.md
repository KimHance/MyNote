# AGENTS.md — :core:ui

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

**두 기능 이상이 쓰는 화면 조각 · 화면 모델 · 기능 사이 신호 계약의 자리**다(기능 문서 `feature-modules` 결정 3).
기능 모듈은 서로를 의존하지 않으므로(FM-2) 둘 이상이 쓰는 것은 여기를 거친다. 디자인 시스템은 기능 뜻(모델 상태 ·
토큰 입력)을 모르는 자리로 두고, 그 뜻을 아는 공용 조각은 이 모듈이 든다.

## 무엇이 있나

기능 문서 `feature-modules` 의 공용 조각 표 그대로다. 패키지는 `:feature:notes` 에 있던 하위 경로를 그대로 두었다.

| 패키지 | 무엇 | 쓰는 곳 |
|---|---|---|
| (뿌리) | 스낵바 문구 `NotesMessages` · 실패 로그 `FailureLog` 와 그 Android 구현(`:app` 의 `NotesModule` 이 묶는다) | 노트 · 설정 · 퀴즈 · 연결 · 셸 VM |
| `signal/` | 기능 사이 신호 계약 `FeatureSignals` · 마지막 동기화 홀더 `LastSyncHolder`(둘 다 `@Singleton`) · 설정 열기 `openSettings` | 모든 기능 ViewModel · 셸(결정 8 · 13) |
| `frame/` | 셸 틀 계약 `ShellFrame` · `ShellDestinations` · 씬 가장자리 modifier | 셸 · 기능별 등록 파일(FM-4) |
| `shell/` | 화면 상태 홀더 `NotesScrollState` · 상단 바 `NotesTopBar` | 셸이 들고 기능 화면이 받는다 · 셸과 쌓은 연결 화면 |
| `element/` | 토큰 입력 홀더(`TokenDraft` · `NotesDrafts` · `SecretText`) · 토큰 실패 종류와 문구 · 저장소 카드 판정과 마지막 동기화 · 모델 상태 → 카드 · 알약 · CTA 판정 · 일회성 신호 판정 `unseen` · 스낵바 알림 `NoticeUi` · 글자 크기 매핑(`FontScaleUi`) | 연결 · 설정 · 노트 · 퀴즈 · 셸 |
| `screen/` | 공용 버튼(`Primary` · `Secondary` · `Destructive` · `SolidDestructive` · `Quiet`) · 본문 최대 폭 · 모델 상태 색 · 미리보기 틀 | 노트 · 연결 · 설정 · 퀴즈 · 셸 |
| `screen/connect/` | 토큰 입력 칸 · 만드는 법 · 만들기 버튼 · 확인 중 줄 · 알림 상자 · 진행 막대 · 연결 아이콘 · 한글 어절 줄바꿈 글자 · 외부 브라우저 열기 · 대화상자 틀 `ActionDialog` | 연결 · 설정 · 노트 |
| `screen/bookmarks/` | 북마크 시각 라벨 | 노트 · 통계 |

- 외부 브라우저 찾기는 이 모듈 `AndroidManifest.xml` 의 `<queries>` 가 필요하다 — 지우면 `openExternal` 이 조용히 일반 열기로 돌아간다.
- `FeatureSignals` · `LastSyncHolder` · `AndroidNotesMessages` · `AndroidFailureLog` 는 `@Inject` 생성자만 있어 `javax.inject` 애너테이션만 의존한다.
  그래프는 `:app` 의 Hilt 가 만든다 — 그래서 문구 구현은 `@ApplicationContext` 대신 Hilt 가 기본으로 묶는 `Application` 을 받는다.
- 문구는 이 모듈 코드가 쓰는 것과, 기능을 다 떼면 둘 이상이 쓰는 것이다. 후자 중 아직 노트 한 모듈만 쓰는 다섯은 파이썬 시험의 `PLACED_AHEAD` 목록에 있다.
- 탭 종류 · 리더 열기 · 탭 전환 명령은 여기가 아니라 `:core:navigation` 에 있다.

## 경계

| 지킬 것 | 지키는 것 |
|---|---|
| 의존은 `:core:{common, design-system, domain, navigation}` 뿐 — 기능 모듈 · 데이터 층 · 모델 구현을 모른다 | `notes-tests/test_feature_modules.py` FM-2 · FM-3 |
| 문구 접두사는 `ui_`(`build.gradle.kts` 의 `resourcePrefix`) — 이 모듈만 참조하는 문구와 둘 이상의 모듈이 참조하는 문구를 둔다(결정 15). 다른 모듈은 `import com.hancekim.mynote.core.ui.R as UiR` 로 읽는다 | `lintDebug` · FM-11 파이썬 시험 |
| 기능 컨벤션(`hancenote.android.feature`)을 쓰지 않는다 — 그 컨벤션이 이 모듈을 넣으므로 쓰면 자기를 의존한다. Hilt 플러그인도 없다(`javax.inject` 애너테이션만) | FM-1 파이썬 시험 |
