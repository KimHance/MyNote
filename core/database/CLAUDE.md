# :core:database — Claude Code Instructions

Room 3 로컬 캐시 모듈. **노트 SSOT 의 물리 저장소**. 의존 방향: `:core:database → :core:data`.

## 하드 룰
- **Room 타입(`@Entity`/DAO/`RoomDatabase`)은 이 모듈 밖으로 내보내지 않는다.** 경계 교차 타입은
  `:core:data` 의 `CachedNote`. Entity↔CachedNote 매핑은 이 모듈의 `datasource` 패키지에서.
- **본문은 정규화하지 않는다.** `NoteEntity.blocksJson` 에 원본 JSON 문자열로 보관(스키마 변동 대비,
  마이그레이션 부담 최소화). 블록 파싱은 `:core:data` Mapper 책임.
- Entity 는 불변(`val` 만). DAO: **관찰은 `Flow`, 변경은 `suspend`**.
- `@Database(exportSchema = true)`. 스키마 변경 시 `core/database/schemas/` 산출물을 함께 관리하고
  버전을 올린다(마이그레이션은 스키마 확정 후 작성).
- import 는 `androidx.room3.*` (room3 alpha). `androidx.room.*` 아님.

## 토큰 저장 (DataStore)
- 노트 캐시(Room) 외에 **토큰 로컬 저장**(`DataStoreTokenStore`, DataStore Preferences)도 구현 — `:core:data` 의 `TokenStore` 인터페이스 구현.
- DataStore 타입도 모듈 밖으로 안 나간다(반환 `String?`/`Flow<String?>`). 현재 **평문**(앱 전용 저장소) — 암호화-at-rest 는 후속(임플만 스왑).

## 컨벤션
- 플러그인: `billboard.android.library` + `billboard.android.room` + `billboard.android.hilt`.
- DI: `DatabaseModule` 이 `NotesDatabase`/`NoteDao` 를 `@Provides`, `LocalNotesDataSource`·`TokenStore` 구현을 `@Binds`.
