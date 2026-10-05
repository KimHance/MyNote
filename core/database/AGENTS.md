# AGENTS.md — :core:database

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

`:core:data` 의 **`LocalNotesDataSource`·`TokenStore`·`SettingsStore`·`BookmarkStore`·`StudyLogStore`·`InsightStore`·
`InsightStatusStore` 를 구현**한다(`:core:database → :core:data`).
노트 캐시(Room 3)와 토큰·설정 저장(DataStore Preferences)이라는 성격이 다른 저장소가 한 모듈에 있는데,
공통점은 "로컬 영속화 구현체" 라는 것뿐이다. 서로 아무 관계가 없다.
`SettingsStore` 계약이 도메인 타입(`FontScale`)으로 말하므로 이 모듈은 `:core:domain` 도 의존한다
(`:core:network` 와 같은 모양이다).

**노트의 SSOT 는 이 모듈의 `source_notes` 테이블이다**(v5 에서 `notes` 를 대신했다). 화면이 보는 Flow 는 원격이 아니라
Room 에서 나온다(`observeCategories()` → `LocalNotesDataSource.observeNotes()` → `NoteDao.observeNotes()`).
원격은 이 테이블을 갱신하는 입력일 뿐이고, 갱신 규칙(리비전 비교·add/update/skip/delete·재변환)은
`core/data/AGENTS.md` 에 있다. 소스 목록은 `sources` 테이블이다(`s0` = 빌드 속성의 GitHub 레포).

## Room 타입을 가두는 방식

`SourceEntity`·`SourceNoteEntity`·`SourceNoteStateRow`·`NoteDao`·`NotesDatabase` 는 **전부 `internal`** 이다. 모듈 밖으로 나가는
타입은 `:core:data` 의 `CachedNote`·`CachedNoteState`·`SourceRecord` 이고, 매핑은 `datasource/LocalNotesDataSourceImpl.kt`
의 private 확장 함수가 전부다. `sources.kind` 의 저장 문자열은 `SourceKindColumn.kt`(`github`·`link`·`local`, enum 이름이 아니다).

그래서 가시성 조합이 조금 특이하다 — **바꾸면 컴파일이 깨진다**:

- `LocalNotesDataSourceImpl` 은 `public class` + `@Inject internal constructor`
  (생성자 파라미터가 internal `NoteDao` 라서). Hilt 는 같은 모듈에서 코드를 생성하므로 문제없다.
- `DatabaseModule` 의 `provideNotesDatabase`/`provideNoteDao` 도 `internal fun`.
- `@Binds` 는 별도의 public `DatabaseBindsModule` 에 있다(`LocalNotesDataSource`, `TokenStore`).
- import 는 **`androidx.room3.*`** 다. `androidx.room.*` 이 아니다. 컨벤션 플러그인
  `hancenote.android.room` 이 `androidx.room3` 플러그인 + KSP + `schemaDirectory("$projectDir/schemas")` 를
  붙인다(이 모듈만 쓴다).

## 건드리면 깨지는 것

- **`applySourceSync` 는 한 트랜잭션이어야 한다.** `@Transaction suspend fun applySourceSync(sourceId, upserts, deleteDocIds)` 가
  upsert 와 delete 를 묶는다. 쪼개면 Room Flow 가 **두 번** 방출되면서 "추가는 됐는데 삭제는 안 된"
  중간 목록이 UI 에 한 프레임 뜬다. `:core:data` 의 "전송 실패는 무반영" 계약이 여기서 물리적으로 완성된다.
  관찰은 `Flow`, 변경은 `suspend` 를 유지한다.
- **삭제는 `WHERE source_id = :sourceId AND doc_id IN (:docIds)` 이고 900개씩 나눠 부른다**(SQLite 바인드 한도 999).
  `source_id` 조건이 "소스는 자기 소유 노트만 지운다" 의 마지막 방어선이다. 계측 `NoteDaoTest` 가 지킨다.
- **`replaceDerived` 는 `blocks_json`·`excerpt`·`parser_version` 만 바꾼다**(재변환). 원문·리비전·처음 본 시각은 건드리지 않는다.
- **본문을 정규화하지 않는다.** `raw` 는 받은 노트 파일 원문, `blocks_json` 은 그것을 변환한 블록 배열 JSON 한 칸(파생 캐시)이다.
  블록 스키마가 바뀌어도 DB 마이그레이션이 필요 없고 `parser_version` 만 올리면 `:core:data` 가 `raw` 에서 다시 만든다.
  블록을 테이블로 펼치려는 리팩터링은 이 트레이드오프를 뒤집는 것이다.
- **`ORDER BY epoch_day IS NULL, epoch_day DESC, first_seen_at DESC, title` 이 UI 의 순서를 결정한다.** `:core:data` 의
  `toCategories` 는 `groupBy` 로 만나는 순서만 유지하므로, 홈의 카테고리 순서·노트 순서는 **이 쿼리에서 파생**된다.
  `date` 문자열은 표시용이고 정렬은 `epoch_day`(못 읽는 날짜는 NULL → 맨 뒤)다. 같은 날짜는 처음 본 시각이 늦은 것이 먼저,
  그것도 같으면 제목(`BINARY`)순이다. `NULLS LAST` 문법 대신 `IS NULL` 을 앞에 둔다.
- **`group` 은 컬럼명이 `group_name` 이다**(`@ColumnInfo`). `group` 은 SQL 예약어라서 그렇다.
- **스키마: `version = 5`, `exportSchema = true`, `autoMigrations = [1→2, 2→3, 3→4]` + 수동 `MIGRATION_4_5`**(`migration/Migration4To5.kt`,
  `DatabaseModule` 의 `.addMigrations`). 4→5 는 아래 "v5" 절.
  `schemas/…NotesDatabase/1.json`–`5.json` 이 다 있어야 KSP 가 자동 마이그레이션을 만들고 테스트가 검증한다. 1→2 는 학습 기록
  테이블 4개와 인덱스, 2→3 은 `weekly_summaries`, 3→4 는 `quiz_patterns` 의 `CREATE` 뿐이고 `notes` 는 건드리지 않는다.
  **3.json 은 v3 엔티티(요약만)로 KSP 를 한 번 돌려 만든 뒤 v4 를 얹었다** — 버전을 한 번에 4 로 올리면 3.json 이 없어
  2→3 자동 마이그레이션을 만들 수 없다. `fallbackToDestructiveMigration` 이
  없어서 Entity 를 고치고 버전만 올리면 **런타임에 DB 를 열다가 죽는다** — 새 테이블·인덱스면 `@AutoMigration`,
  컬럼 변경이면 `Migration` 을 쓰고, 엔티티·버전·스키마 JSON 을 한 커밋에 낸다. 손으로 SQL 을 쓰지 않는다 —
  **예외는 4→5 하나**(아래).
  (노트 캐시라서 destructive 로 가도 되지만, 이제 같은 파일에 학습 기록이 있어 그건 기록을 날리는 결정이다.)
- DB 파일명은 `notes.db`, 빌더 옵션은 `.addMigrations(MIGRATION_4_5)` 하나다.

## 노트 캐시 v5 — `sources`·`source_notes` 와 4→5 수동 마이그레이션

| 테이블 | 키 | 열 |
|---|---|---|
| `sources` | PK `id`(`s0`) | `kind`(`github`·`link`·`local`) · `config_json`(NULL = 빌드 기본값) · `added_at` |
| `source_notes` | PK `note_key`(= `NoteKey` 문자열 `s0/graph`) · UNIQUE(`source_id`, `doc_id`) | `path` · `group_name` · `title` · `date`(표시용) · `epoch_day`(정렬, NULL 허용) · `first_seen_at` · `revision`(index sha256) · `fetched_at` · `parser_version` · `raw`(원문) · `blocks_json`(파생) · `excerpt` · 자리만 둔 열 `kind`·`summary`·`aliases_json`·`source_url`·`tags_json`·`warnings_json` |

- `key` 는 SQL 키워드라 열 이름이 `note_key` 다. FK 는 두지 않는다 — 소유 삭제는 DAO 가 `source_id` 조건으로 명시적으로 한다.
- **자리만 둔 열(`kind`·`summary`·`aliases_json`·`source_url`·`tags_json`·`warnings_json`)은 `CachedNote` 에 없어서 `@Upsert` 가 매번 `NULL` 로 덮는다.** M0 에는 채우는 코드가 없어 문제없지만, 다른 경로(M1 front matter 등)가 이 열을 채우게 되면 그 전에 `CachedNote`·매핑에 열을 더해야 한다 — 아니면 다음 동기화가 지운다.
- **4→5 는 손으로 쓴 `Migration` 이다**("손으로 SQL 을 쓰지 않는다" 의 유일한 예외, `docs/oss/m0/spec-data.md` §5).
  `notes` → `source_notes` 는 PK·열 이름이 바뀌고 값을 변환하며(`'s0/' || id`), 학습 기록 `UPDATE` 는 자동 마이그레이션이 못 만든다.
  `@DeleteTable` 은 `onPostMigrate` 전에 테이블을 지워서 복사할 수 없다. 대신 **`CREATE` 문은 5.json 의 `createSql` 을 그대로 옮겼고**,
  `runMigrationsAndValidate(5, listOf(MIGRATION_4_5))` 가 열·타입·NULL·기본값·인덱스를 5.json 과 대조한다. 엔티티를 고치면 5.json 이
  바뀌므로 이 SQL 도 같이 고쳐야 한다(테스트가 먼저 죽는다).
- 4→5 가 하는 일(한 트랜잭션 — Room 이 `BEGIN EXCLUSIVE` 로 감싸고 실패하면 `ROLLBACK`, DB 는 v4 그대로):
  `sources` 에 `s0` → `notes` 를 `source_notes` 로 **복사**(캐시를 버리지 않아 업데이트 직후 오프라인이어도 보인다. `raw` NULL·
  `parser_version` 0 이라 다음 동기화가 원문을 한 번 채운다, `revision` = 옛 `contentHash`, `epoch_day` = `julianday` 로 계산) →
  `DROP TABLE notes` → 학습 기록 3테이블 `note_id` 에 `s0/` 접두(`instr(note_id, '/') = 0` 인 것만) → `quiz_attempts.format` 추가.
- **옮긴 행의 `first_seen_at` = 마이그레이션 시각(ms) − `rowid`.** v4 의 `ORDER BY date DESC` 는 동률을 rowid 순으로 냈다(실측).
  rowid 가 작을수록 처음 본 시각이 늦어져 v5 쿼리가 같은 날짜 노트 순서를 그대로 낸다. SQLite 보장이 아니라 실측이라
  계측 테스트 `같은 날짜 노트의 v4 순서가 v5 에서도 같다` 가 기기의 실제 SQLite 로 확인한다.
- 커밋된 `main` 은 v1 이라 실제 기기에는 1→2→3→4(자동)→5(수동)가 이어 도는 경로도 있다 — `v1 에서 v5 까지 한 번에 올라간다`.
- 다운그레이드는 지원하지 않는다. v5 DB 가 있는 기기에 v4 빌드를 깔면 첫 DB 접근에서 죽는다(`appVersionCode` 가 1 로 고정이라
  워크트리 빌드끼리도 덮어 깔린다).

## 북마크 저장 (DataStore `bookmarks`)

- `DataStoreBookmarkStore` — 키 이름 = 노트 키(`s0/graph`), 값 = 추가 시각 `Long`.
- **`BookmarkKeyMigration`(`DataMigration`)** 이 `/` 없는 옛 키를 `s0/<키>` 로 옮긴다. 같은 `s0/` 키가 이미 있으면 그쪽을 남긴다.
  `Long` 이 아닌 값·이미 접두가 있는 키는 그대로. 멱등이라 옛 키로 된 백업이 복원돼도 다시 돈다.
- DataStore 는 마이그레이션이 끝나 새 파일을 쓴 뒤에 첫 값을 낸다. 실패하면 파일을 바꾸지 않고 `data` Flow 로 던지고 다음 접근에서
  다시 시도한다 — 그 사이 화면은 `NotesViewModel` 의 `catch` 로 북마크가 빈다(손실 없음).
- Room 과 DataStore 는 한 트랜잭션이 아니다. 둘 다 원자적·멱등이라 한쪽만 끝난 상태에서도 다음 실행이 맞춘다.

## 학습 기록 (Room, v2)

통계(PRD 4.9)의 원시 기록이다. `entity/StudyLogEntities.kt` 의 넷과 `dao/StudyLogDao.kt` 가 전부 `internal` 이고,
밖으로는 `:core:data` 의 `StudyLogStore` 계약(도메인 모델 `ReadSession`·`QuizAttempt`·`BookmarkEvent` 로 말한다)만
나간다. 구현은 `datasource/StudyLogStoreImpl.kt`.

| 테이블 | 키 · 인덱스 | 메모 |
|---|---|---|
| `read_sessions` | id 자동 · `started_at`·`note_id` | 열람 세션. 카테고리는 기록 시점 스냅샷. `note_id` = `NoteKey` 문자열(v5) |
| `quiz_attempts` | id 자동 · `finished_at`·`note_id` | 결과 화면까지 간 시도만. `format`(v5, `llm`·`card`, 기본 `llm` — `QuizFormatColumn`) |
| `quiz_answers` | PK(`attempt_id`, `position`) · FK → `quiz_attempts` CASCADE | 선택지는 JSON 이 아니라 `choice_0..3` 고정 칸 |
| `bookmark_events` | id 자동 · `at` | `kind` 는 enum 이름이 아니라 `"added"`/`"removed"` 고정 문자열(`BookmarkEventKindColumn.kt`) |

- **집계(`SUM`/`GROUP BY` 로 칸 만들기)는 SQL 에 두지 않는다.** DAO 는 구간 필터·노트별 최신 세션·첫 기록 시각만
  한다. 날·주·달 칸은 기기 시간대 기준이라 `:core:domain` 의 `StudyStatsCalculator` 가 Kotlin 으로 계산한다.
- `clear()`·`prune()` 은 `@Transaction` 이고, 답을 **먼저 지운 뒤** 시도를 지운다 — FK CASCADE 에만 기대지 않는다.
  **`clear()` 는 요약·패턴 캐시 두 테이블도 같은 트랜잭션에서 지운다**(PRD D43 — 기록을 지웠는데 파생 문장이 남으면 안 된다).
- 보존은 두 기준이다(D44 갱신): `prune(before = 365일)` 은 네 테이블, `deleteAnswersBefore(before = 90일)` 은 **`quiz_answers`
  만**(문항·보기 텍스트). 시도 행(정답률·재도전 숫자)은 365일 남는다. 패턴은 최근 28일 오답만 보므로 기능 손해가 없다.
- 오답 조회 `wrongAnswers(since, limit)` 는 `quiz_answers` ⋈ `quiz_attempts` 에서 `picked_index != answer_index`, 최신순.
  `note_id` 도 싣는다 — 패턴 근거 말뭉치에 노트 제목을 더하려고.
  고른 보기·정답 보기 텍스트는 `WrongAnswerRow.choice(i)` 로 고정 칸에서 꺼낸다.

## 주간 요약·패턴 캐시 (Room, v3·v4)

| 테이블 | 키 | 메모 |
|---|---|---|
| `weekly_summaries`(v3) | PK `week_start`(월요일 epochDay) | 같은 주는 `@Upsert` 로 덮어쓴다. `model_commit`·`schema_version` 열 |
| `quiz_patterns`(v4) | PK `week_start` | 태그는 `tag_0` NOT NULL + `tag_1`/`tag_2` NULL 고정 칸 |

- **관찰 쿼리가 `model_commit = :commit AND schema_version = :version` 으로 거른다.** 모델을 바꾸면 옛 문장이 섞이지 않는다.
  현재 값은 `:core:ai` 가 `InsightScheduler.stamp` 로 내보내고 도메인 UseCase 가 넘긴다(이 모듈은 모델을 모른다).
- "최신" 은 `ORDER BY week_start DESC, generated_at DESC` — 지난주 확정이 이번 주 요약보다 늦게 만들어져도 카드는 이번 주를 본다.
- 26주 정리: `InsightDao.prune(before)` 가 두 테이블에서 `week_start < before` 를 지운다(앱 시작 + 워커 저장 직후).

## 워커 상태 (DataStore `study_insights`)

`DataStoreInsightStatusStore` — 키 6개: `last_result`(`success`/`failure`/`skipped`, `InsightStatusColumns.kt` 의 고정 문자열)
· `last_success_at` · `last_failure_at` · `last_failure_kind`(`parse`/`engine`/`timeout`) · `last_skipped_at` · `log_epoch`.
- `markSkipped` 는 실패 키를 건드리지 않는다(포기는 실패가 아니다, PRD D60). `markFailure(summaryFailed = false)`(패턴만 실패)는
  `last_failure_at` 을 쓰지 않는다 — 카드 문구가 "이번 주 요약을 만들지 못했습니다" 라서.
- `clear()` 는 `log_epoch` 을 **남기고** 나머지를 지운다. 기록 지우기가 epoch 을 먼저 올리고 지우므로, 그 순간 돌던 워커는
  저장 직전 epoch 비교에서 결과를 버린다.
- 시도 + 답은 `insertAttempt(attempt, answers)` 한 트랜잭션으로 넣는다(행 id 를 받아 답에 채운다).

## 토큰 저장 (DataStore)

- `DataStoreTokenStore` 가 `:core:data` 의 `TokenStore` 를 구현한다. DataStore 타입도 모듈 밖으로
  나가지 않는다 — 노출은 `String?` / `Flow<String?>` 뿐이다.
- `preferencesDataStore(name = "github_token")` 은 **`Context` 확장 프로퍼티**(파일 최상위 private)다.
  같은 이름으로 두 번 만들면 DataStore 가 런타임에 중복 인스턴스로 던지므로 이 파일 밖에서 만들지 않는다.
- **빈 문자열은 토큰 없음으로 정규화한다** — 읽을 때도(`?.ifBlank { null }`), 쓸 때도(`null` 이면 키 제거).
  이 불변식은 `SaveTokenUseCase` 와 `:core:network` 의 `GithubTokenHolder` 에서도 반복된다.
- **현재 평문 저장**이다(앱 전용 디렉터리). 암호화-at-rest 로 가려면 이 클래스만 스왑하면 되도록
  인터페이스가 좁게 잡혀 있다.
- 토큰이 저장된 뒤 실제 HTTP 요청에 붙기까지의 배선은 `core/network/AGENTS.md` 의 토큰 흐름을 본다.

## 설정 저장 (DataStore)

- `DataStoreSettingsStore` 가 `SettingsStore` 를 구현한다 — `preferencesDataStore(name = "settings")`,
  키는 `font_scale`·`study_log_enabled`(Boolean, 없으면 `true`) 둘. 토큰과 **다른 파일**이라 서로 영향이 없다.
- **저장 형식은 enum 이름이 아니라 `datasource/FontScalePreference.kt` 의 고정 문자열**(`"normal"`/`"large"`)이다.
  `FontScale.name` 을 그대로 쓰면 enum 을 리네임하는 순간 기존 사용자의 저장값이 조용히 무효가 된다.
- **매핑은 던지지 않는다** — 키가 없거나 모르는 값이면 `FontScale.Default`(=`Normal`). 다운그레이드로
  모르는 값이 남아 있어도 기본 크기로 뜬다(단, DataStore 파일 자체가 깨지면 `data` Flow 가 던진다 —
  토큰 쪽과 같은 노출이다).
- 이 매핑은 `FontScalePreferenceTest` 가 지킨다.

## 테스트

JVM 은 `FontScalePreferenceTest`·`BookmarkEventKindColumnTest`·`InsightStatusColumnsTest`·`SourceKindColumnTest`(저장 형식 매핑)와
`BookmarkKeyMigrationTest`(옛 키 접두·충돌·멱등) 다섯이다.

**마이그레이션은 계측 테스트다** — `src/androidTest/…/NotesDatabaseMigrationTest.kt`, 실행은
`./gradlew :core:database:connectedDebugAndroidTest`(에뮬레이터·기기 필요, 루트 `test` 에는 안 들어간다).
room3-testing 의 `MigrationTestHelper` 는 Android 변형이 `Instrumentation` 을 요구해 JVM 단위 테스트로는 못 돈다.
`1.json` 으로 만든 DB 에 노트를 넣고 `runMigrationsAndValidate(2)` 로 올려 노트 보존·새 테이블·인덱스·CASCADE 를 본다.
스키마 JSON 은 Room Gradle 플러그인이 테스트 APK assets 에 자동으로 넣는다. 헬퍼가 DB 파일을 지우지 않으므로
`@Before` 에서 `deleteDatabase` 한다. v2→v3→v4(기록 보존 + 요약 upsert 덮어쓰기 + 패턴), v4→v5(노트 이동·`s0` 소스·
학습 기록 접두와 답·요약·패턴 보존·CASCADE·`epoch_day`·같은 날짜 순서 보존), v1→v5 한 번에 케이스가 있다.
`NoteDaoTest`(in-memory Room) 가 `applySourceSync` 의 한 번 반영·900개 넘는 삭제 청크·다른 소스의 같은 docId 보존·정렬·소스 상태를 본다.
이 모듈의 계측 테스트는 라이브러리 테스트 APK 만 깔고 앱(`com.hancekim.mynote`)의 DB 를 건드리지 않는다.
`DataStoreBookmarkStore` 자체(Context 가 필요한 부분)에는 테스트가 없다 — 이전 로직은 `BookmarkKeyMigration` 으로 떼어 JVM 에서 본다.
