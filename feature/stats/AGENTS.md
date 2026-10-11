# AGENTS.md — :feature:stats

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

통계 · 지난 요약의 자리다(기능 문서 `feature-modules` 모듈 지도). `:feature:notes` 에서 패키지째 옮겨 왔다.

| 패키지 | 무엇 |
|---|---|
| `com.hancekim.mynote.feature.stats` | `StatsViewModel` · 상태 · 인텐트 · 매퍼(`StatsUiMapper`) · 요약 규칙(`SummaryCardRules` · `GeneratedAgo`) · 기록(`StatsTrace`) · 디스패처(`StatsDispatcher.kt`) |
| `com.hancekim.mynote.feature.stats.screen` | 통계 화면 · 지난 요약 화면 · 머리 · 구역 · 요약 카드 · 등록 파일 `StatsDestinations.kt`(`statsDestinations` — 키 `Stats` · `StatsHistory`) |

`:app` 셸이 등록 파일을 묶고(`shell/NotesNavDisplay.kt`), `StatsTrace` 의 구현 `LogStatsTrace` 는 `:app` 의 `di/NotesModule` 이 묶는다.
BR-8 계측(`:app` 의 `TitleFontScreensTest`)이 요약 카드(`SummaryCard` · `SummaryForm` · `rememberStatsFormats`)를 그려서 그 셋은 공개다.

## 경계

| 지킬 것 | 지키는 것 |
|---|---|
| `build.gradle.kts` 는 `hancenote.android.feature` 하나 — `:core:{common, design-system, domain, navigation, ui}` · 공통 의존 · 문구 접두사 `stats_` 를 컨벤션이 넣는다 | `build-logic/AGENTS.md` |
| 다른 기능 모듈을 의존하지 않는다. 둘 이상이 쓰는 것은 `:core:ui`, 다른 기능 화면으로는 목적지 키로 간다 | `notes-tests/test_feature_modules.py` FM-2 |
| 데이터 층(`:core:{data, network, database, format}`) · 모델 구현(`:core:ai`)을 모른다 — `:core:domain` 의 UseCase 만 | FM-3 |
| 문구는 이 모듈만 참조하는 것만 둔다(결정 15) | `lintDebug` · FM-11 |

## 집계는 통계가 맨 위일 때만 돈다

`StatsViewModel` 은 기간·섹션을 `SavedStateHandle` 에 둔다. 집계 Flow 는
`navigator.stack` 의 top 이 `Stats` 일 때만 `flatMapLatest` 로 붙고 아니면 `emptyFlow()` 라 리더에 가 있는 동안
Room 쿼리가 돌지 않는다(debug 에서 `MyNoteStats` 로그로 확인). `SharingStarted.Lazily` 로 바꾸면 한 번 연 뒤
앱이 끝날 때까지 집계가 돈다 — 쓰지 말 것. 집계 · 지난 요약의 `flowOn` 디스패처는 주입한다(`@StatsWork` — 이 모듈의
Hilt 모듈 `StatsDispatcherModule` 하나). 리더 · 퀴즈 진입은
통계 VM 이 목적지 키로 리더를 열고(`StatsIntent.OpenNote` · `RetryQuiz`) 이어 읽을 위치 · 퀴즈 설정은 계약
`FeatureSignals.reportReaderOpened` 로 노트 VM 에 넘긴다 — 노트 VM 이 읽을 수 있는 노트면 퀴즈 VM 에 설정을 요청한다(FM-6 ① ②).
그 두 신호의 시험은 노트 · 퀴즈 VM 을 같이 세워야 해서 `:app` 의 `FeatureSignalsTest` 에 있다.

## 화면

통계 화면(`screen/`)의 2단 여부도 북마크와 같은 `NotesLayout.contentTwoPane`(콘텐츠 폭 ≥ 640dp)이고,
가로 막대·낮은 차트 같은 축소 변형은 `heightClass == Compact` 로 갈린다. 2단 오른쪽 칸 항목 키는 배치마다
다르게 둔다(`side-*`) — 같은 키를 쓰면 `LazyListState` 가 키를 따라가 폴드 전환 때 오른쪽 칸이 아래로 밀린다.

### 요약 카드 · 지난 요약 · 패턴 (통계 2·3단계)

- 카드·패턴의 보임 규칙은 순수 함수 `SummaryCardRules.kt`(`summaryCardOf`·`patternOf`) 와 `GeneratedAgo` 에 있고
  `SummaryCardRulesTest`·`GeneratedAgoTest` 가 지킨다. Composable 안에서 `latest`/`lastFailureAt` 을 다시 비교하지 말 것.
- **카드는 별도 LazyColumn 항목이 아니라 `"segment"` 항목 안(기록 꺼짐 안내 아래, 세그먼트 위)에 있다.** 워커가 끝나 카드가
  나타날 때 별도 항목이면 목록이 첫 보이는 키(`segment`)를 붙잡아 카드가 화면 위로 숨는다(8.2 #64 와 같은 원인).
- 인사이트 Flow 는 `Stats` 가 top 일 때만, 지난 요약 목록은 `StatsHistory` 가 top 일 때만 붙는다(집계와 같은 게이트).
- `StatsHistory` 는 백스택 엔트리라 뒤로가기는 `NavDisplay` pop(5순위)이 받는다. 상단 바는 `←` + `지난 요약`,
  `NotesTopBar(showRefresh = false)`, 플로팅 내비는 그대로 보인다. 행 펼침은 화면 로컬 `rememberSaveable`(키 `weekStart`).
- 초점 표시는 `SummaryCardUi.focusInLegend`(이번 주 기간 && 이번 주 요약 && 오래되지 않음)일 때만 범례에 붙는다.

## 명령

```bash
./gradlew :feature:stats:testDebugUnitTest   # 통계 VM · 매퍼 · 요약 규칙 · 머리 값
./gradlew :app:compileDebugKotlin            # 이 모듈 변경 검증은 :app 컴파일로
```
