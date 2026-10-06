# AGENTS.md — :core:domain

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

**가장 안쪽 레이어.** 모델 + 인터페이스(DIP) + UseCase 뿐이고 프로젝트 모듈은 하나도 의존하지 않는다.
외부 의존도 `kotlinx-coroutines-core` 와 `javax.inject` 둘뿐이다.

## 순수 JVM 이라 규칙이 문서가 아니라 빌드로 강제된다

`hancenote.jvm.library` 를 쓴다 — `com.android.library` 가 아니다. 매니페스트·R 클래스·리소스 머지·
계측 러너가 하나도 붙지 않고, 무엇보다 **`import android.*` 가 `Unresolved reference` 컴파일 에러**다.
"domain 은 순수 Kotlin" 이 지켜지길 바라는 규칙이 아니라 지킬 수밖에 없는 조건이 된다.

여기서 파생되는 것들:

- **코루틴은 `-core`**(`-android` 아님). `Dispatchers.Main` 이 없다. 스레드 선택은 바깥이 한다 —
  `:core:data` 가 `withContext(Dispatchers.Default)`, feature 가 `viewModelScope`.
- **어노테이션을 "안 붙이는" 게 아니라 붙일 수 없다.** `@Serializable`(serialization 플러그인 미적용),
  `@Immutable`/`@Stable`(compose runtime 없음), `R.string`(리소스 없음) 전부 classpath 에 없다.
  직렬화는 `:core:format` · `:core:data` 의 DTO, stability 는 UI 상태, 표시 문구는 `strings.xml` 의 책임이라는 배치가
  실수로 무너질 수 없다. 모델은 평범한 `data class` + `List` 를 그대로 쓴다.
- **Hilt 플러그인이 없다.** `javax.inject.Inject` 만 쓰고 UseCase 는 unscoped 생성자 주입이다.
  `@Binds` 는 구현체를 가진 `:core:data`/`:core:network`/`:core:ai` 의 모듈이 한다.
- **Gradle 태스크 이름이 다르다.** `:core:domain:compileKotlin` / `:core:domain:test` 이고
  `compileDebugKotlin`·`testDebugUnitTest`·`assembleDebug` 는 **존재하지 않는다.**
  루트에서 `./gradlew testDebugUnitTest` 를 돌려도 이 모듈은 조용히 건너뛴다. `./gradlew test` 나 `:core:domain:test` 로 돌린다.

## 인터페이스는 여기가 소유하고 바깥이 구현한다

| 인터페이스 | 하는 일 | 구현 |
|---|---|---|
| `NotesRepository` | `observeCategories(): Flow<List<Category>>` + `suspend sync(): Result<SyncOutcome>` | `NotesRepositoryImpl` (`:core:data`) |
| `TokenRepository` | 저장된 토큰 read/write/observe | `TokenRepositoryImpl` (`:core:data` → DataStore in `:core:database`) |
| `SettingsRepository` | 본문 글자 크기(`FontScale`) · 학습 기록 켜짐 observe/write | `SettingsRepositoryImpl` (`:core:data` → DataStore in `:core:database`) |
| `StudyLogRepository` | 학습 기록 쓰기(세션·시도·북마크 이벤트) · 구간/노트별 관찰 · 오답 조회 · 지우기(요약 캐시 포함) · 365일 정리 | `StudyLogRepositoryImpl` (`:core:data` → Room in `:core:database`) |
| `InsightRepository` | 주간 요약·패턴 캐시(`stamp` 로 거른 관찰) · 워커 상태(성공/실패/포기·`log_epoch`) · 26주 정리 | `InsightRepositoryImpl` (`:core:data` → Room + DataStore `study_insights`) |
| `InsightScheduler` | 워커 예약·취소 · "생성 중" 상태 · 현재 `InsightStamp`(모델 commit + 출력 형식 버전) | `StudyInsightWork` (`:core:ai`) |
| `ConnectProbe` | 후보 목록 + 후보 토큰으로 저장소 · 목록 확인 → `ConnectProbeResult`(Verified · Failed 9종) | `GithubConnectProbe` (`:core:network`) |
| `AppliedTokenSource` | 지금 **실제로 먹은** 토큰 | `GithubTokenHolder` (`:core:network`) |
| `QuizGenerator` | `isAvailable()` + `generate(title, text, count)` | `LiteRtLmQuizGenerator` (`:core:ai`) |

domain 은 전부 구현을 모른다. 그래프는 `:app` 이 구현 모듈을 다 포함해 완성한다.

`FontScale`(`model/`)은 `:core:design-foundation` 의 `MyNoteFontScale` 과 **일부러 별개 타입**이다 —
design-foundation 은 Compose 를 들고 있어서 domain 이 그걸 의존하면 의존 방향이 뒤집힌다.
둘 사이 매핑은 바깥(UI 레이어)이 한다.

### 연결 흐름 UseCase 둘

`VerifyConnectLinkUseCase`(링크 해석 → 오프라인이면 요청 없이 → probe → 성공 뒤에만 지울 개수 · 토큰 삭제 여부 `ConnectReplacement`)와
`ConnectRepositoryUseCase`(begin → 첫 동기화 → confirm, 실패 · 취소면 discard, `Flow<ConnectProgress>`: Syncing · Connected · Failed). confirm 이 예외로 끝나면 연결 상태를 다시 읽어 확정됐으면 Connected, 아니면 Failed(Server) 로 끝낸다. 이어받기 개수는 `ConnectionRepository.countAdoptionErasable(새 목록 id)` — 새 목록에 없는 노트만. **confirm 이 끝난 뒤에는 discard 를 부르지 않는다**
(확정된 연결의 토큰만 시도 전 값으로 돌아간다). 첫 동기화 실패는 `SyncFailure` 6종으로 나가고 그 밖의 예외는 `Server` 로 감싼다. 시험: `ConnectFlowTest`.

### 토큰 인터페이스가 둘인 이유 (중복이 아니다)

`TokenRepository` = 사용자가 **저장한** 값. `AppliedTokenSource` = 네트워크 클라에 **꽂힌** 값.
`:app` 의 `TokenSync` 가 연결 복구(`RecoverConnectionUseCase`)를 기다린 뒤 저장값을 관찰해 holder 에 밀어넣는다. 폴백은 없다 —
debug 내장 토큰은 시드 연결(`ConsumeSeedUseCase`)이 저장 칸에 쓰고, 저장된 토큰이 이미 있으면 덮어쓰지 않는다.
`ObserveAppliedTokenUseCase` 는 holder 에 실제로 꽂힌 값을 보는 판정용이고, `SyncPendingConnectionUseCase` 가 "저장값 = 적용값" 을 기다린 뒤 첫 동기화를 시작한다.

## UseCase 가 어디까지 하는가

대부분은 위임 한 줄이다(`GetCategories`·`SyncNotes`·`ObserveToken`·`ObserveAppliedToken`·
`ObserveFontScale`·`SetFontScale`, `SaveToken` 은 blank → null 정규화까지). 정렬·필터 같은 도메인 규칙이
생기면 여기로 온다.

실제 규칙을 든 건 **`GenerateQuizUseCase`** 하나다. 화면에서 `noteId` 만 받아 네 가지를 한다:

1. **노트를 찾는다** — `observeCategories().first()` 로 **캐시된 트리**에서 찾는다. 네트워크를 타지 않는다
   (SSOT 는 Room). 못 찾으면 `QuizFailure.Empty`. 즉 호출부가 `Note` 를 들고 오지 않아도 된다.
2. **블록을 평문으로 편다** — 10종 블록의 exhaustive `when`. `Divider`/`Spacer` 는 버리고 코드블록 본문은
   **포함한다**(코드에서도 문제를 낼 수 있게). 리스트는 항목마다 `- `(체크 항목은 `- [x] ` · `- [ ] `, 번호 목록도 같다)를
   붙여 한 줄씩이고 글자가 빈 항목은 줄을 쓰지 않는다. 표는 머리 행부터 행마다 한 줄, 칸 사이 ` | `. 항목의 하위 블록은
   모든 줄을 깊이마다 두 칸 들인다. **취소선 구간은 `~~글~~` 로 남긴다** — 모델이 지운 문장을 사실로 읽지 않게(note-format
   결정 15). 첫 문장(`firstSentence`)은 같은 구간을 모양 없는 글자로 쓴다. **`Unsupported` 는 건진 글자(`text`)만**
   넣고 원문(`raw`, JSON 조각)은 넣지 않는다 — 건진 글자는 상자 안에 보이는 글이라 퀴즈 근거를 노트에서 찾을 수 있다
   (`GenerateQuizUseCaseTest`).
3. **`take(2_000)` — 본문을 자르는 책임이 여기 있다.** 프롬프트 길이가 곧 prefill 이고 prefill 이 곧 TTFT다
   (온디바이스 2B/GPU 기준 첫 토큰이 본문 길이에 비례해 늘어난다). 컨텍스트 창(`EngineConfig.maxNumTokens`)은
   엔진 레벨이라 늘리려면 재init 10초짜리라서, 실질적으로 조절 가능한 손잡이는 입력 길이뿐이다.
   구현마다 각자 자르면 기준이 흩어지고 어느 값이 먹었는지 추적이 안 된다.
   → **`QuizGenerator` 구현은 받은 텍스트가 이미 잘려 있다고 가정해도 된다**(`:core:ai` 가 그 전제로 쓴다).
4. 평문이 blank 면 `generate` 를 **부르지 않는다**(엔진을 깨우지 않는다).

경계 두 가지:

- **timeout 은 여기 없다.** `NotesViewModel` 의 `withTimeoutOrNull(60초)` 가 감싼다. 그래서 UI 쪽
  `QuizUiState.Failed.Kind` 에는 `Timeout` 이 있는데 `QuizFailure` 에는 없다 — 불일치가 아니라 소유자가 다르다.
- `isAvailable()` 은 UseCase 를 거치지 않는다. feature 가 `QuizGenerator` 를 직접 주입받아 부른다
  (도메인 인터페이스라 경계 위반은 아니다). 모델 파일 존재 여부를 묻는 것뿐이라 감싸도 얻는 게 없다.

## 학습 기록과 통계 — 규칙은 전부 여기 있는 순수 함수다

- `model/ReadSessionTracker` — 열람 세션 상태기계. 시계(`Moment`: 벽시계 + `elapsedRealtime`)를 호출자가 넘긴다.
  30초 미만 버림, 60분 절단, 덮인 동안 일시정지, 같은 노트로 `onTop` 반복은 무시. `NotesViewModel` 이 먹인다.
  읽은 비율 최댓값은 **트래커만** 세션 단위로 들고, top 노트의 마지막 비율을 기억해 보임 재개 세션을 거기서
  시작한다(top 노트가 바뀌면 버린다). 셸은 지금 비율만 보낸다.
- `stats/StatsPeriodRange` — 이번 주(월요일 시작)·이번 달(달 경계로 자른 주)·전체(최근 12개월) 칸과 비교 구간.
  시간대는 넘겨받은 `ZonedDateTime` 의 것. 14일 미만이면 `shortRangeDays`.
- `stats/StudyStatsCalculator` — 숫자 4칸·칸별 합·카테고리 축(현재 노트 ∪ 기간 이벤트, 이름순)·멈춘 노트·
  재도전·오래 안 본 북마크. **SQL 이 아니라 여기서 센다.** 상수는 `StatsThresholds` 한 곳.
- 기록 UseCase 셋(`RecordReadSession`·`RecordQuizAttempt`·`RecordBookmarkEvent`)이 **`study_log_enabled` 를 한 곳에서
  거른다.** 호출부마다 거르지 않는다. `RecordBookmarkEventUseCase` 는 행 id 를 돌려주고 `cancel(id)` 로 되돌린다.
- `stats/InsightPlan` — 워커가 무엇을 만들지(지난주 확정 · 이번 주 첫 요약/재생성 · 패턴) 정하는 순수 판정(PRD D54).
  엔진을 깨우기 전에 돈다. 상수는 `StatsThresholds`.
- `stats/WeeklyDigest(Builder)` — 요약 입력의 **모든 숫자**(그 주·지난주 같은 구간·차이·카테고리별 분·정답·`top`/`least`)를
  여기서 만든다. 모델은 숫자를 계산하지 않고 이 값만 옮긴다 — `:core:ai` 가 출력 숫자를 이 집합과 대조해 환각을 버린다.
- `stats/WeeklyHistory` — 지난 요약 화면의 주 목록(최대 52주, 원시 기록에서 숫자 재계산, D64).
- `ClearStudyLogUseCase` 순서: 워커 취소 → `log_epoch` +1 → 기록·캐시 삭제 → 상태 키 삭제 → 조건(`insightsWanted`)이면 재예약.
  `PruneStudyLogUseCase` 는 365일 기록 + **90일 `quiz_answers`**(문항 텍스트, D44 갱신) + 26주 요약·패턴을 같이 정리한다.
- `WeeklyDigest.trend`(more/less/same, `change.minutes` 부호)·`titles`(stalled + reread) — 요약 문장에 숫자를 쓰지 않으므로
  방향은 말로, 제목 속 숫자는 가드에서 지우고 본다(`:core:ai` 검수 B1).
- 테스트(`:core:domain:test`): `NoteKeyTest`·`GenerateQuizUseCaseTest`(NF-10 평문)·`NoteExcerptTest`·`NoteLimitsTest`·`ReadSessionTrackerTest`·`StatsPeriodRangeTest`(DST·윤년·시간대 포함)·
  `StudyStatsCalculatorTest`·`StudyLogUseCaseTest`(지우기 순서·재예약·26주 정리 포함)·`InsightPlanTest`·`WeeklyDigestTest`·
  `WeeklyHistoryTest`.

## `QuizFailure` 의 메시지는 개발자용이다

`sealed class QuizFailure : Exception` — `Parse`/`Empty`/`Engine`. 메시지는 전부 영어 진단 문구다
(`"parse: no JSON array in output"`, `"engine failed to load or run"`). **화면 문구가 아니다.**

경로: `:core:ai` 가 실패를 만들고 → `NotesViewModel.toFailKind()` 가 `QuizUiState.Failed.Kind` 로 바꾸고
→ `QuizFailedScreen` 이 `Kind` → `R.string.notes_quiz_failed_*` 로 그린다.

여기에 한국어 안내 문장을 쓰면 안 되는 이유 셋:

1. **같은 문장이 두 벌이 된다.** `strings.xml` 을 고쳐도 도메인 쪽은 안 따라오고, 어느 쪽이 화면에
   보이는지는 호출 경로에 달린다.
2. **도메인이 표시 문구를 알게 된다.** 로케일·번역·리소스는 UI 의 관심사다. 도메인이 그걸 들면
   문구를 바꾸려고 가장 안쪽 레이어를 건드리게 된다.
3. **애초에 불가능하다.** 순수 JVM 이라 `R.string` 에 닿을 수 없다 — 남는 건 번역 불가능한
   하드코딩 문자열뿐이다.

그리고 화면 분기는 **타입으로** 한다. 메시지 문자열로 갈랐다면 문구를 다듬는 순간 화면이 조용히 깨진다.
새 실패 종류를 추가할 때도 문자열이 아니라 서브타입을 추가하고, `toFailKind()` + `Kind` + `strings.xml`
세 자리를 같이 고친다(`toFailKind` 의 `else` 가 `Engine` 이라 **컴파일러가 안 잡아 준다**).

## 모델

- `Category`/`Note`/`NoteBlock`/`NoteListItem`/`NoteSpan`/`SpanStyle`/`TableCell`/`TableAlign`/`NoteLimits` + `QuizQuestion` +
  `SyncOutcome` + `NoteKey`/`SourceId`/`SourceKind`.
- **블록은 10종이다** — 형식 1 의 8종 + `Table`(머리 칸 · 본문 행 · 열 정렬) + `Unsupported`. 새 모양은 기본값 있는 필드라 지금 8종의
  생성자 호출이 그대로다: `NoteSpan.italic` · `strike`(굵게 · 코드는 지금 `style` — 겹치면 `style = Bold, italic = true`, 코드에는
  취소선만 붙는다), `NoteListItem.checked`(`null` = 체크 항목 아님) · `children`(항목 글 아래 블록), `OrderedList.start`.
  인용 안 기울임도 모델에는 기울임 그대로 두고 세워 그리기는 렌더가 한다. 새 블록 · 필드는 마크다운에서만 나온다(JSON 형식 1 동결).
- **`NoteLimits` 가 해석 전 한도 값의 한 자리다** — `Default` = 32단 · 512KB(`maxBytes` 524,288). `:core:format` 의 거르기와
  `:feature:notes` 의 화면 매퍼가 같은 값을 읽는다 — 상자 문구의 두 숫자가 여기서 온다(NF-8 ⑨). 바꾸면 이미 거른 노트는
  다음 재변환(`PARSER_VERSION`)까지 옛 판정이다.
- **`Note.id` 는 `String` 이고 값이 `NoteKey` 문자열이다** — `"<sourceId>/<docId>"`, 기존 노트는 `s0/graph`. 북마크·학습 기록·
  `SyncOutcome.deleted`·내비 키가 전부 이 문자열로 맞물린다. 소비자는 id 를 불투명하게 다룬다(쪼개거나 파싱하지 않는다).
  `NoteKey.parse` 는 **첫 `/`** 에서 자른다 — `SourceId` 에는 `/` 가 없고 `docId` 에는 있을 수 있다(M1 의 경로 기반 id).
  `s0` 는 레포 좌표를 담지 않는다 — 노트 레포를 옮겨도 키가 바뀌지 않게(`NoteKeyTest`).
- **`NoteBlock.Unsupported(reason, type, text, raw)`** 는 앱 내부 전용 블록이다(와이어에 없는 타입). `reason` =
  `UnknownType`(앱이 모르는 type)·`BrokenBlock`(아는 type 인데 필드 누락·타입 오류, type 없음)·`BrokenNote`(노트 파일 전체를
  못 읽음 — 이때 `blocks` 는 이 블록 하나)·`LimitExceeded`(마크다운이 해석 전 한도를 넘어 읽지 않음 — `BrokenNote` 처럼 노트 전체
  상자 하나이고 첫 문장 · 퀴즈 카드가 없다). `text` = 건진 글자(없으면 `null`), `raw` = 원문 보기용(빈 문자열 = 원문 없음, v4 에서
  옮긴 캐시의 옛 `unknown`). 만드는 규칙은 `core/format/AGENTS.md`.
- **`NoteBlock` 은 같은 모양이 세 번 나온다:** `NoteBlockDto`(`:core:format`, `@Serializable`, 모듈 밖에 안 보임) →
  **`NoteBlock`(여기)** → `NoteContentBlock`(`:core:design-system`, `@Immutable`). 이 중복이 경계의 값이다 —
  DTO 가 UI 까지 새지 않고, UI stability 가 도메인을 오염시키지 않는다.
  블록 타입을 하나 추가하면 exhaustive `when` 이 있는 자리(`:core:format` 의 `BlockMapper`, `NoteBlockMapper`,
  `GenerateQuizUseCase.toPlainText`)는 컴파일 에러로 잡히지만 **`:core:format` 의 `PARSER_VERSION` 과 문서는 컴파일러가
  잡아 주지 않는다.** JSON 형식 1 은 동결이라 새 블록은 마크다운에서만 나온다(`core/format/AGENTS.md`).
- **`SyncOutcome` 은 값이 아니라 판단까지 준다** — `changed`(added+updated)·`hasChanges`. 스낵바를 띄울지
  (`hasChanges`)와 문구(`added`/`updated`/`deleted`)를 같은 자리에서 못 세면 "변경 없음"인데 안내가 뜬다.
  `deleted` 는 개수가 아니라 **`NoteKey` 문자열 목록**이다 — 삭제된 노트를 보고 있으면 화면을 옮기고 그 노트의 북마크를 지워야 해서
  호출부가 키를 쓴다. 업데이트 직후 원문을 채우려고 다시 받은 노트는 `updated` 에 세지 않는다(`:core:data`).
