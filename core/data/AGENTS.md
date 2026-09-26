# AGENTS.md — :core:data

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

**DataSource 계약을 소유하고 reconcile 을 오케스트레이션한다.** 원격도 로컬도 여기서 구현하지 않는다.
`RemoteNotesDataSource`·`LocalNotesDataSource`·`TokenStore` 세 인터페이스를 이 모듈이 정의하고,
`:core:network`/`:core:database` 가 바깥에서 구현한다(DIP). 의존은 `:core:data → :core:domain` + `:core:common` 뿐이고
구현체 모듈은 **컴파일 타임에 모른다** — Hilt 그래프는 `:app` 이 세 모듈을 다 포함해서 완성한다.

## reconcile — 세 모듈이 맞물리는 지점

`NotesRepositoryImpl` 의 `sync()` 가 이 앱의 유일한 쓰기 경로다. 전체가 `withContext(Dispatchers.Default)` +
`runCatchingCancellable` **하나**로 감싸여 있다.

1. `remote.getIndex()` — 원격 카탈로그(`notes/index.json`)를 평탄화된 `List<IndexEntryDto>` 로 받는다.
   각 엔트리에 노트 파일 raw 바이트의 sha256(`hash`)이 들어 있다. **앱은 해시를 비교만 하고 만들지 않는다**
   (생성은 `notes/gen_index.py` + pre-commit 훅).
2. `local.loadHashes()` — 로컬 `id → contentHash` 맵.
3. **본문 재수신 판정은 딱 한 줄이다:** `index.filter { localHashes[it.id] != it.hash }`.
   - 해시 같음 → **스킵**. `getNote` 를 아예 부르지 않는다(대역폭·API 레이트 절약의 핵심).
   - 로컬에 키가 없음 → **add**, 키는 있고 해시가 다름 → **update**.
     이 둘을 가르는 것도 `localHashes[it.id] == null` 하나뿐이다(`SyncOutcome.added` / `updated`).
   - index 에 없는 로컬 id → **delete**(`localHashes.keys - index ids`).
4. 변경분 본문을 `flatMapMerge` 로 **동시** 수신한다. 순차 루프로 되돌리면 테스트
   `변경된 노트를 동시에 받는다` 가 게이트에서 타임아웃으로 죽는다(고의적 변이 감지 장치).
5. `fetched` 와 `deleteIds` 가 둘 다 비면 `applySync` 를 **호출하지 않는다**. 둘 중 하나라도 있으면
   한 번의 `applySync(upserts, deleteIds)` 로 넘긴다 — 원자성은 `:core:database` 의 `@Transaction` 이 보장한다.

### 건드리면 깨지는 계약

- **전부 아니면 전무.** 노트 하나라도 fetch 가 실패하면 예외가 `flatMapMerge` 를 타고 올라와
  `applySync` 에 **도달하지 못한다**. 부분 반영 목록을 보여주면 사용자가 "왜 이 노트가 없지" 로
  오해하므로 의도된 설계다. 개별 fetch 를 `runCatching` 으로 감싸 실패를 건너뛰게 바꾸면 이 계약이 깨진다.
- **`runCatching` 대신 `runCatchingCancellable`**(`:core:common`). 호출부(`NotesViewModel`)가
  `combine(appliedToken, refreshTicks).flatMapLatest { syncFlow(...) }` 이라 토큰이 바뀌거나 새로고침을
  누르면 **이전 sync 가 취소된다**. 평범한 `runCatching` 은 `CancellationException` 까지 삼켜
  그 취소를 "동기화 실패" 스낵바로 둔갑시킨다.
- **`Result` 를 반환한다면 어떤 경로로도 예외를 던지지 않아야 한다.** DB 쓰기 실패까지 같은
  `runCatchingCancellable` 안에 있어야 한다. 밖으로 새면 `stateIn` 의 공유 코루틴이 죽어 상태
  파이프라인이 멈춘다(`로컬 쓰기가 실패해도 예외를 던지지 않고 Result 실패다` 가 이걸 지킨다).
- **`Dispatchers.Default` 를 떼지 말 것.** 호출자는 `viewModelScope`(Main.immediate)이고 `sync()` 안에서
  노트마다 JSON 인코딩이 돈다. 떼면 메인 스레드에서 인코딩한다.

## 경계 교차 타입

| 방향 | 타입 | 규칙 |
|---|---|---|
| 원격 → data | `IndexEntryDto`, `NoteDto`(+`NoteBlockDto` 등) | `:core:data` 소유. Retrofit/OkHttp 타입은 넘어오지 않는다 |
| 로컬 ↔ data | **`CachedNote`** | Room `@Entity`·DAO 는 `:core:database` 안에서 `internal`. 이 모듈은 Entity 를 본 적이 없다 |
| data → 바깥 | `Category`/`Note`/`NoteBlock`(domain), `SyncOutcome` | 매핑은 **단방향**(DTO/`CachedNote` → domain). 역매핑 없음 |

`CachedNote.blocksJson` 은 **블록 배열을 통째로 담은 JSON 문자열**이다. 노트 envelope 의 `id`/`title`/`date` 는
파싱은 되지만 **버려지고**, 캐시에 들어가는 제목·날짜는 **index 의 값**이다(`fetch()` 가 `entry.title`/`entry.date` 를 쓴다).
즉 목록/헤더에 보이는 제목이 안 맞으면 노트 파일이 아니라 `index.json` 을 봐야 한다.
(그래도 `NoteDto` 는 `id`/`title`/`date` 가 non-null 무기본값이라 노트 파일에서 키가 빠지면 **디코드가 실패해 sync 전체가 실패**한다.)

## 새 블록 타입을 추가할 때

`NoteBlockDto` 는 `@Serializable sealed interface` + `@SerialName` **discriminator `type`** 다. kotlinx 의 기본
polymorphic 디코더는 **모르는 discriminator 를 만나면 던진다** — `ignoreUnknownKeys = true` 는 알 수 없는
*필드*만 무시하고 알 수 없는 *type* 은 구제하지 않는다. 위의 "전부 아니면 전무" 와 합쳐지면:
**앱이 모르는 블록 타입 하나가 원격에 올라가면 그 노트 하나가 아니라 동기화 전체가 실패하고 캐시는 통째로 멈춘다.**
그래서 앱 업데이트가 콘텐츠보다 먼저 나가야 한다(`notes/SCHEMA.md` 첫 문단이 같은 말을 한다).

한 타입을 추가하려면 `when` 이 exhaustive 라서 컴파일러가 잡아 주는 자리 + 안 잡아 주는 자리가 섞여 있다.
**컴파일러가 안 잡는 것: 파이썬 검증기와 문서.**

1. `dto/NotesDto.kt` — `NoteBlockDto` 에 `@SerialName("…")` 서브타입 추가
2. `core/domain/.../NoteBlock.kt` — 도메인 대응 추가
3. `dto/NoteMapper.kt` — DTO → domain (컴파일 에러로 잡힌다)
4. `:core:design-system` 의 `NoteContentBlock` + 렌더러, `:feature:notes` 의 `NoteBlockMapper`(컴파일 에러로 잡힌다)
5. **`notes/gen_index.py` 의 `BLOCK_TYPES`** 와 **`notes/SCHEMA.md` 의 허용 type 표** — 안 고치면 훅이 노트를 거부한다
6. `NoteDtoSerializationTest` 에 라운드트립 케이스

필드 없는 블록은 `data object`(`Divider`/`Spacer`)로 둔다 — `{"type":"divider"}` 만으로 라운드트립된다.

## 그 외 알아둘 것

- **`Json` 은 `DataModule` 이 단독으로 제공한다**(`ignoreUnknownKeys = true`). `:core:network` 의 Retrofit
  컨버터도 이 인스턴스를 주입받는다. 다른 모듈에서 또 `@Provides Json` 하면 Hilt 중복 바인딩으로 빌드가 깨진다.
  (`:core:ai` 의 `QuizJsonParser` 는 DI 밖의 자체 `Json` 이라 무관하다.)
- **`toCategories` 는 조용히 버린다.** `blocksJson` 디코드가 실패한 노트는 `runCatching{}.getOrNull()` 로
  스킵되고, 한 그룹의 노트가 전부 실패하면 그 `Category` 자체가 사라진다. "노트가 안 보인다"는 제보의
  유력한 용의자이고 로그도 남지 않는다.
- **`observeCategories()` 는 Room Flow 가 방출될 때마다 전 노트의 `blocksJson` 을 다시 디코드한다.**
  그래서 `flowOn(Dispatchers.Default)` 가 붙어 있다. 노트 수가 늘면 여기가 먼저 아프다.
- 카테고리/노트의 **정렬은 이 모듈에 없다.** `groupBy` 가 만나는 순서를 유지할 뿐이고 그 순서는
  DAO 의 `ORDER BY date DESC` 에서 온다.
- **토큰은 통과만 한다.** `TokenRepositoryImpl` 은 domain `TokenRepository` → data `TokenStore` 로
  위임하는 얇은 껍데기다. 저장 메커니즘(DataStore)은 `:core:database`, HTTP 헤더 주입은 `:core:network`.
- DTO 는 순수하게 유지한다 — Compose 의존 없음, `List` 사용. stability(`@Stable`/`ImmutableList`)는 UI 레이어 책임.

## 테스트

`src/test/` 에 두 개, 둘 다 JUnit4 + `runTest`(`kotlin("test")` 금지).

- `NotesReconcileTest` — fake Remote/Local 로 add/skip/update/delete + 부분 실패 무반영 + index 실패 +
  로컬 쓰기 실패 + 동시 수신. fake 는 `failOn`(실패 주입)과 `gate`(`MutableStateFlow` 배리어로 동시성 강제)를 갖는다.
  reconcile 을 손대면 **여기부터** 고친다.
- `NoteDtoSerializationTest` — polymorphic discriminator, `style` 생략 시 `TEXT` 기본값, `divider`/`spacer` 라운드트립.
