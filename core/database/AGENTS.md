# AGENTS.md — :core:database

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

`:core:data` 의 **`LocalNotesDataSource` 와 `TokenStore` 를 구현**한다(`:core:database → :core:data`).
노트 캐시(Room 3)와 토큰 저장(DataStore Preferences)이라는 성격이 다른 두 저장소가 한 모듈에 있는데,
공통점은 "로컬 영속화 구현체" 라는 것뿐이다. 서로 아무 관계가 없다.

**노트의 SSOT 는 이 모듈의 `notes` 테이블이다.** 화면이 보는 Flow 는 원격이 아니라 Room 에서 나온다
(`observeCategories()` → `LocalNotesDataSource.observeNotes()` → `NoteDao.observeNotes()`).
원격은 이 테이블을 갱신하는 입력일 뿐이고, 갱신 규칙(해시 비교·add/update/skip/delete)은
`core/data/AGENTS.md` 에 있다.

## Room 타입을 가두는 방식

`NoteEntity`·`NoteHashRow`·`NoteDao`·`NotesDatabase` 는 **전부 `internal`** 이다. 모듈 밖으로 나가는 타입은
`:core:data` 의 `CachedNote` 하나이고, Entity ↔ `CachedNote` 매핑은 `datasource/LocalNotesDataSourceImpl.kt`
의 private 확장 함수 두 개가 전부다.

그래서 가시성 조합이 조금 특이하다 — **바꾸면 컴파일이 깨진다**:

- `LocalNotesDataSourceImpl` 은 `public class` + `@Inject internal constructor`
  (생성자 파라미터가 internal `NoteDao` 라서). Hilt 는 같은 모듈에서 코드를 생성하므로 문제없다.
- `DatabaseModule` 의 `provideNotesDatabase`/`provideNoteDao` 도 `internal fun`.
- `@Binds` 는 별도의 public `DatabaseBindsModule` 에 있다(`LocalNotesDataSource`, `TokenStore`).
- import 는 **`androidx.room3.*`** 다. `androidx.room.*` 이 아니다. 컨벤션 플러그인
  `hancenote.android.room` 이 `androidx.room3` 플러그인 + KSP + `schemaDirectory("$projectDir/schemas")` 를
  붙인다(이 모듈만 쓴다).

## 건드리면 깨지는 것

- **`applySync` 는 한 트랜잭션이어야 한다.** `@Transaction suspend fun applySync(upserts, deleteIds)` 가
  upsert 와 delete 를 묶는다. 쪼개면 Room Flow 가 **두 번** 방출되면서 "추가는 됐는데 삭제는 안 된"
  중간 목록이 UI 에 한 프레임 뜬다. `:core:data` 의 "전부 아니면 전무" 계약이 여기서 물리적으로 완성된다.
  관찰은 `Flow`, 변경은 `suspend` 를 유지한다.
- **본문을 정규화하지 않는다.** `NoteEntity.blocksJson` 은 블록 배열 JSON 문자열 한 칸이다. 블록 스키마가
  바뀌어도 DB 마이그레이션이 필요 없게 하려는 의도이고, 파싱은 `:core:data` 의 Mapper 책임이다.
  블록을 테이블로 펼치려는 리팩터링은 이 트레이드오프를 뒤집는 것이다.
- **`ORDER BY date DESC` 가 UI 의 순서를 결정한다.** `:core:data` 의 `toCategories` 는 `groupBy` 로 만나는
  순서만 유지하므로, 홈의 카테고리 순서·노트 순서는 **이 쿼리에서 파생**된다. 그리고 `date` 는 TEXT 이고
  `YYYY.MM.DD`(0 패딩)라서 사전순 정렬이 우연히 시간순과 일치한다 — 날짜 포맷을 바꾸면 정렬이 조용히 망가진다.
- **`group` 은 컬럼명이 `group_name` 이다**(`@ColumnInfo`). `group` 은 SQL 예약어라서 그렇다.
- `deleteByIds` 는 `WHERE id IN (:ids)` 다. Room 이 자동으로 쪼개 주지 않으므로 한 번에 지울 id 가
  SQLite 바인드 변수 한계(999)를 넘길 규모가 되면 청크가 필요하다. 지금 노트 수에선 안 걸린다.
- **스키마: `version = 1`, `exportSchema = true`, 마이그레이션 없음.** `schemas/…NotesDatabase/1.json` 이
  커밋돼 있다. `fallbackToDestructiveMigration` 도 안 걸려 있어서, Entity 를 고치고 버전만 올리면
  **런타임에 DB 를 열다가 죽는다.** 필드를 바꿀 땐 버전 + `Migration` + 새 스키마 JSON 을 같이 낸다.
  (캐시라서 destructive 로 가도 되지만, 그건 명시적으로 선택해야 하는 결정이다.)
- DB 파일명은 `notes.db`, 빌더에 아무 옵션이 없다.

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

## 테스트

이 모듈에는 테스트가 없다(컨벤션이 `failOnNoDiscoveredTests(false)` 로 풀어 뒀다).
DAO 동작은 `:core:data` 의 `NotesReconcileTest` 가 fake `LocalNotesDataSource` 로 대체 검증한다 —
즉 **`applySync` 의 트랜잭션성 자체는 어떤 테스트도 지켜 주지 않는다.** 여기를 고칠 땐 직접 확인한다.
