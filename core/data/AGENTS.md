# AGENTS.md — :core:data

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

**DataSource 계약을 소유하고 reconcile 을 오케스트레이션한다.** 원격도 로컬도 여기서 구현하지 않는다.
`NoteSourceAdapter`·`LocalNotesDataSource`·`TokenStore`·`SettingsStore`·`BookmarkStore`·`StudyLogStore`·`InsightStore` 를
이 모듈이 정의하고, `:core:network`/`:core:database` 가 바깥에서 구현한다(DIP). 의존은 `:core:domain` · `:core:format` ·
`:core:common` 뿐이고 구현체 모듈은 **컴파일 타임에 모른다** — Hilt 그래프는 `:app` 이 세 모듈을 다 포함해서 완성한다.

노트 본문의 **형식 해석은 순수 JVM `:core:format` 이 한다**(`NoteFormat`, `core/format/AGENTS.md`). 이 모듈은 그 진입점을
생성자로 주입받아 부를 뿐 블록 DTO 를 모른다. 전송 계층(Retrofit)은 본문을 해석하지 않고 원문 문자열로 넘긴다 — 그래야 형식 오류를
전송 오류와 가를 수 있다(아래 "실패 두 층"). `:core:format` 은 `implementation` 이라 이 모듈을 의존하는 network · database 에는 보이지 않는다.

## reconcile — 세 모듈이 맞물리는 지점

`NotesRepositoryImpl.sync()` 가 이 앱의 유일한 쓰기 경로다. 전체가 `withContext(Dispatchers.Default)` +
`runCatchingCancellable` **하나**로 감싸여 있다.

1. **재변환(로컬).** `parser_version < PARSER_VERSION` 이고 `raw` 가 있는 행을 원문 · 행의 `path` 로 다시 만들어 한 번에 바꾼다.
   읽는 것은 키 · 경로 · 원문뿐이고(`StaleNote`), 원문이 `NoteLimits.maxCacheBytes` 를 넘는 행은 읽지 않고 건너뛴다 — 커서 창보다
   큰 행 하나가 동기화를 매번 첫 줄에서 멈추지 않게.
   네트워크보다 **먼저** 돌고 따로 커밋되므로 오프라인이어도 반영된다(F4 — 앱을 올리면 노트 파일이 안 바뀌어도 새 변환이 보인다).
   소스 격리 밖이라 `NoteFormat` 이 던지지 않는 것에 기댄다 — 해석이 실패하거나 한도를 넘은 행은 상자로 새 판 번호에 저장된다.
2. **동기화는 소스를 만들지 않는다**(SY-3). `sources()` 중 확정된 GitHub 소스(`config_json` 해석 가능 · 미확정 아님)와 다른 kind 만 돈다.
   연결이 없으면 요청 0회로 빈 `SyncOutcome`. 먼저 `ConnectionRecovery.ensure()` 를 기다린다.
3. **소스마다** `SourceReconciler.reconcile(source, adapter)`. 어댑터는 Hilt `@IntoSet` 으로 모이고 `kind` 로 고른다.
   **어댑터가 없는 kind(`Link`·`Local`)의 소스는 동기화도 삭제도 하지 않는다.** M0 의 소스는 `s0` 하나다.

`reconcile` 한 번:

1. `adapter.list(source)` — 소스 전체 목록(`SourceEntry`: docId·path·revision·category·title?·date?). manifest 어댑터는
   `notes/index.json` 을 평탄화하고 `revision` 은 index 의 sha256 이다. **앱은 해시를 비교만 하고 만들지 않는다**(생성은 `notes/gen_index.py`).
2. `local.loadSourceState(source.id)` — **그 소스 행만** `docId → (revision, hasRaw, firstSeenAt)`.
3. 받을 것 = `revision` 이 다르거나 로컬에 없거나 **`raw` 가 없는**(v4 에서 옮겨 온) 항목. 리비전이 같으면 본문을 받지 않는다.
4. 받을 본문을 `flatMapMerge` 로 **동시** 수신한다. 순차 루프로 되돌리면 테스트 `변경된 노트를 동시에 받는다` 가 타임아웃으로 죽는다(고의적 변이 감지 장치).
5. 받은 원문마다 `NoteFormat.derive(entry.path, raw)` → `blocks_json`(파생 캐시)·`excerpt`. **형식 해석은 던지지 않는다.**
6. 지울 것 = **그 소스의** 로컬 docId − 목록 docId. 둘 다 비면 쓰기를 부르지 않고, 아니면 `applySourceSync(source, upserts, deleteDocIds)` 한 번.
7. `SyncOutcome`: `added`(로컬에 없던 것)·`updated`(리비전이 바뀐 것)·`deleted`(`NoteKey` 문자열). **원문 채우기 재수신(리비전 같음 + `raw` 없음)은 세지 않는다** — 업데이트 직후 "갱신 22개" 스낵바가 뜨지 않게.

### 실패 두 층

| 층 | 무엇 | 처리 |
|---|---|---|
| 전송 | index·본문 수신 실패(오프라인·401·404·5xx), index 해석 실패 | 그 소스의 변경을 **하나도 반영하지 않는다**(`Result` 실패). 지금까지의 "전부 아니면 전무" 는 이 층에만 남는다 |
| 형식 | 본문 JSON 이 깨짐·필드 누락·모르는 type·모르는 style, 마크다운 한도 넘음 · 해석 실패 | 그 노트만 깨진 채로 **반영한다**. 블록은 `Unsupported`, 노트 전체면 상자 하나 |

근거 테스트: `NotesReconcileTest` 의 `본문 전송이 하나라도 실패하면 아무것도 반영하지 않는다`·`형식 오류 노트는 격리하고 나머지는 반영한다`·
`JSON 이 아닌 노트도 목록에 남고 본문은 BrokenNote 다`·`index 를 해석하지 못하면 아무것도 반영하지 않는다`,
`NoteFormatSyncTest` 의 `NF-8 해석 실패는 그 노트만 상자다`·`NF-9 재변환 중 해석이 실패한 행은 상자가 되고 동기화는 성공한다`.

### 건드리면 깨지는 계약

- **소스는 자기 소유 노트만 지운다.** 지울 후보는 `loadSourceState(source.id)` 에서만 나오고 DAO 도 `WHERE source_id = ?` 로 지운다.
  다른 소스(M3 링크 노트·M4 기기 폴더)의 노트를 지우면 `SyncOutcome.deleted` 를 타고 **북마크까지 지워진다**(`NotesViewModel` 이
  지워진 키의 북마크를 정리한다). 회귀 장치: `다른 소스의 노트는 지우지 않는다`·`어댑터가 없는 kind 의 소스는 동기화도 삭제도 하지 않는다`.
- **지우기 보호는 소스 단위.** 그 소스의 목록이 비었는데 그 소스 로컬이 1건 이상이면 지우지 않고 실패한다(`wipesEverything`).
- **`first_seen_at` 은 소스 동기화 한 번에 한 번 잡은 시각이고, 갱신·원문 채우기에도 바꾸지 않는다.** 노트마다 시계를 부르면
  새로 깐 기기에서 같은 날짜 노트 순서가 도착 순서에 따라 갈린다. 정렬은 `:core:database` 쿼리(날짜 → 처음 본 시각 → 제목).
- **`runCatching` 대신 `runCatchingCancellable`**(`:core:common`). 호출부(`NotesViewModel`)가 토큰이 바뀌거나 새로고침을
  누르면 이전 sync 를 취소한다. 평범한 `runCatching` 은 그 취소를 "동기화 실패" 스낵바로 둔갑시킨다.
- **`Result` 를 반환한다면 어떤 경로로도 예외를 던지지 않아야 한다.** DB 쓰기 실패까지 같은 `runCatchingCancellable` 안에
  있어야 한다(`로컬 쓰기가 실패해도 예외를 던지지 않고 Result 실패다`).
- **`Dispatchers.Default` 를 떼지 말 것.** 호출자는 `viewModelScope`(Main.immediate)이고 `sync()` 안에서 노트마다 파싱·인코딩이 돈다.
- 시계는 생성자로 받는다: `internal constructor(…, now)` + `@Inject constructor(…)` 가 `System::currentTimeMillis` 를 넘긴다.

## 연결 — `ConnectionRepositoryImpl` · `ConnectionRecovery`

연결 정보는 `sources.config_json` 한 줄(`connection/ConnectionConfigCodec`: owner·repo·branch·indexPath·isPublic·pending)이다.
스키마 변경 없음. 한 번에 연결 하나.

- **시도** = `begin`(시도 기록 + 시도 전 토큰을 `ConnectionStateStore` 에 적음 → 필요하면 후보 토큰 저장) → `syncPending` → `confirm`/`discard`.
  종류 셋: `New`(다른 저장소 · 첫 연결 — 새 UUID 소스 행을 `"pending":true` 로 만든다, 노트는 한 쿼리로 가려진다),
  `Adopt`(연결 없음 + `config_json` 없는 옛 노트 소유 소스 — 행에 아무것도 쓰지 않고 첫 동기화는 메모리 속 `SourceRecord` 로 돈다),
  `SameRepository`(대소문자 무시로 같은 저장소 — 같은 소스 id 로 설정만 바꾼다). `Adopt`·`SameRepository` 는 확정 전에 저장된 것이 없어서 중단되면 자동으로 옛 상태다.
- **확정(`New`)** = `LocalNotesDataSource.promote` 한 Room 트랜잭션: 새 소스 pending 제거 + 다른 GitHub 소스 노트·행 삭제. 그 뒤 북마크 접두 삭제.
  Room 과 DataStore 는 못 묶으므로 지울 id 를 먼저 `bookmarkSweep` 에 적고 시작 복구가 행이 사라진 id 만 마저 지운다.
- **끊기** = 표식(`markDisconnect`, 시드 차단 겸용) → 노트·행(한 트랜잭션) → 북마크 접두 → 토큰 → 표식 해제. 시드 차단은 새 연결이 확정될 때 푼다.
  노트 삭제가 실패하면 표식을 되돌리고 북마크·토큰은 그대로다.
- **시작 복구** `ConnectionRecovery.ensure()`(싱글턴, 한 번): 끊기 표식 마무리 → pending 소스 폐기 → 확정 안 된 시도의 토큰을 시도 전 값으로(시도 대상 행의 `config_json` 이 목표와 다르면) → 북마크 청소.
  앱에 시동 필드가 없다 — `NotesRepositoryImpl.sync` · `ConnectionRepositoryImpl` 의 모든 진입 · `:app` `TokenSync` 가 먼저 기다린다.
- 연결 식별자는 `ConnectionIdGenerator`(UUID)이고 쓴 id 는 DataStore 에 쌓여 겹치면 다시 뽑는다.
- 시드는 domain 의 `ConsumeSeedUseCase` 가 소비한다(`ConnectionSeed?` 를 주입받아 `begin` → 첫 동기화 → `confirm`, 실패면 `discard`).

## 형식 — `:core:format` 을 부르는 네 자리

해석 규칙 · 와이어와 캐시 DTO · 한도 · 상자 사유는 `core/format/AGENTS.md` 에 있다. 이 모듈이 지킬 것은 **원문을 블록으로 바꾸는 자리가
모두 `NoteFormat` 에 경로를 함께 넘기는 것** 하나다.

| 자리 | 부르는 것 | 경로 |
|---|---|---|
| 새로 받은 노트 | `SourceReconciler.derive` → `derive(path, raw)` | 목록 항목의 `path` |
| 앱 시작 재변환 | `NotesRepositoryImpl.rederiveStale` → `derive(path, raw)` | 캐시 행의 `path`(옛 행은 `null` = JSON) |
| 연결 첫 동기화 | `ConnectionRepositoryImpl` 이 따로 만든 `SourceReconciler` | 목록 항목의 `path` |
| 화면이 읽는 길 · 캐시 손상 대체 | `NoteMapper.toDomainNote` → `read(blocksJson, path) { loadRaw(key, maxCacheBytes) }` | 캐시 행의 `path` |

`NoteFormat` 은 `@Inject constructor()` 라 `DataModule` 에 바인딩이 없다. 두 Repository 의 `internal constructor` 가 그것을 받으므로
시험은 `NoteFormat(markdown)` 으로 가짜 변환기를 넣는다(`NoteFormatSyncTest`).

## 경계 교차 타입

| 방향 | 타입 | 규칙 |
|---|---|---|
| 원격 → data | `SourceEntry`, 원문 `String`, `IndexNoteDto` | `:core:data` 소유. Retrofit/OkHttp 타입은 넘어오지 않는다 |
| 로컬 ↔ data | **`CachedNote`**·`CachedNoteState`·`SourceRecord`, 읽기 투영 `ListedNote`(목록)·`StaleNote`(재변환), 쓰기 `RederivedNote` | Room `@Entity`·DAO 는 `:core:database` 안에서 `internal`. 이 모듈은 Entity 를 본 적이 없다 |
| data → 바깥 | `Category`/`Note`/`NoteBlock`(domain), `SyncOutcome` | 매핑은 **단방향**(`ListedNote` → domain, `NoteMapper`). `Note.id` = 행의 `key` = `NoteKey` 문자열(`s0/graph`) |

`CachedNote` 는 `key`·`sourceId`·`docId`·`path`·`group`·`title`·`date`·`epochDay`·`firstSeenAt`·`revision`·`fetchedAt`·`parserVersion`·
`raw`·`blocksJson`·`excerpt` 다. v5 의 3라운드 열(`kind`·`summary`·`aliases_json`·`source_url`)과 `tags_json`·`warnings_json` 은
M0 에 쓰는 코드가 없어 여기에 없다 — 쓰는 단계에서 더한다.

## 새 블록 타입을 추가할 때

`core/format/AGENTS.md` "새 블록 · 필드를 더할 때" 를 따른다. 이 모듈에서 고칠 것은 없다 — 블록 DTO 를 모르기 때문이다.

## 그 외 알아둘 것

- **`Json` 바인딩은 `DataModule` 하나에 있다**(`NotesJson`: `ignoreUnknownKeys`·`coerceInputValues`). 이 모듈 안에서 주입받는 곳은 없고,
  주입받는 곳은 `:core:network` 의 Retrofit 컨버터 둘(목록 index · 저장소 확인 응답)뿐이다. 다른 모듈에서 또 `@Provides Json` 하면
  Hilt 중복 바인딩으로 빌드가 깨진다.
  (`:core:ai` 의 `QuizJsonParser` 와 `:core:format` 의 `BlockJson` 은 DI 밖의 자체 `Json` 이라 무관하다.)
- **`observeCategories()` 는 Room Flow 가 방출될 때마다 전 노트의 `blocks_json` 을 다시 디코드한다.** 그래서 `flowOn(Dispatchers.Default)`
  가 붙어 있다. 목록은 원문 열을 읽지 않는다(`ListedNote`) — 원문은 노트 전체 상자이거나 캐시를 못 읽은 노트만 `loadRaw` 로 한 행씩,
  `maxCacheBytes` 안일 때만 읽는다(넘으면 상자에 원문 보기가 없다). `excerpt` 열은 동기화가 채우지만 아직 화면이 읽지 않는다.
- 카테고리/노트의 **정렬은 이 모듈에 없다.** `groupBy` 가 만나는 순서를 유지할 뿐이고 그 순서는 DAO 쿼리에서 온다.
- **토큰은 통과만 한다.** `TokenRepositoryImpl` 은 domain `TokenRepository` → data `TokenStore` 로 위임하는 얇은 껍데기다.
- DTO 는 순수하게 유지한다 — Compose 의존 없음, `List` 사용. stability(`@Stable`/`ImmutableList`)는 UI 레이어 책임.

## 테스트

`src/test/` 의 시험은 전부 JUnit4 + `runTest`(`kotlin("test")` 금지). 형식 시험은 `:core:format` 에 있다.

- `NotesReconcileTest` — fake 어댑터·fake 로컬로 add/skip/update/delete, 전송 실패 무반영, 형식 격리, 모르는 style, 원문·버전 저장,
  재변환(원격 0회), 원문 채우기(세지 않음), 자기 소유 삭제, 소스 단위 가드, 처음 본 시각, epochDay, excerpt, 캐시 손상 대체, 동시 수신.
  fake 어댑터는 `failOn`(실패 주입)과 `gate`(`MutableStateFlow` 배리어로 동시성 강제)를 갖는다. reconcile 을 손대면 **여기부터** 고친다.
- `NoteFormatSyncTest` — 가짜 마크다운 변환기로 네 자리가 경로로 고르는지(연결 첫 동기화 · 캐시 손상 `.md` 행), 한도 넘은 노트와 해석이
  실패한 노트가 그 노트만 상자가 되는지, 재변환이 JSON 행을 그대로 두고 실패 행을 상자로 저장하는지(NF-2 · NF-8 · NF-9).
- `MarkdownSyncTest` — 실제 엔진(`NoteFormat()`)으로 한도 안의 큰 입력이 동기화를 멈추지 않는지(NF-8), 옛 판이 상자로 굳힌 `.md` 행이
  네트워크 없이 다시 만들어지는지(NF-9), 이 레포 노트 22개를 `.md` 로 옮긴 목록으로 동기화하면 추가 0 · 삭제 0 · 갱신 22 이고 북마크가
  그대로인지(NF-16). 노트 사본은 `:core:format` 의 공유 시험 자료(`testFixtures(projects.core.format)` → `ParityNotes`)를 읽는다.
- `TemplateNotesTest` — 템플릿 JSON 예시가 `NoteFormat` 에서 강등 0 · 동기화로 전부 받아진다(NT-6).
- `ConnectionRepositoryTest` · `ConnectionConfigCodecTest` · `SyncFailureKindTest` · `BookmarkRepositoryImplTest`.
