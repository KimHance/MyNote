# :core:data — Claude Code Instructions

DataSource 인터페이스 소유 + reconcile 오케스트레이터. 의존 방향: `:core:data → :core:domain`.

## 하드 룰
- `RemoteNotesDataSource`(원격) / `LocalNotesDataSource`(로컬) / `TokenStore`(토큰) **인터페이스를 이 모듈이 소유**한다.
  구현체(`:core:network`/`:core:database`)는 모른다 — Hilt 가 `:app` 에서 바인딩.
- 토큰: `TokenRepositoryImpl`(domain `TokenRepository` 구현) 이 `TokenStore` 에 위임. 저장 메커니즘(DataStore)은 database 가 책임.
- **SSOT 는 Room.** `observeCategories()` 는 로컬을 `Flow` 로 관찰. `sync()` 는 원격 index 와 로컬
  해시를 비교해 변경분만 본문까지 받아 로컬을 갱신하고 `Result<Unit>` 반환.
- 매핑은 단방향: DTO/`CachedNote` → domain. Entity 는 보지 않는다(데이터 경계 교차는 `CachedNote`).
- `blocksJson` ↔ `List<NoteBlockDto>` 파싱은 이 모듈. `Json` 은 `DataModule` 이 단일 제공.
- 직렬화 모델(DTO)은 순수 유지(Compose 무관). stability 는 UI 레이어가 보증.
