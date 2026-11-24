---
id: data
name: Data
aliases: [데이터, 데이터 층, core data, 동기화 · 연결 저장소]
uses: [common, domain, format]
units:
  - {id: data, name: Data module root, aliases: [Hilt 묶음, DataModule, Json 바인딩], code: [src/]}
  - {id: contracts, name: Data-source contracts, aliases: [DataSource 계약, 경계 타입, CachedNote, 저장소 인터페이스], code: [src/main/java/com/hancekim/mynote/core/data/datasource/, src/main/java/com/hancekim/mynote/core/data/dto/]}
  - {id: sync, name: Repository-note sync, aliases: [동기화, reconcile, 재변환, 노트 목록, 새로고침, 저장소 노트 변환, 노트 다시 변환, reparse], code: [src/main/java/com/hancekim/mynote/core/data/repository/NotesRepositoryImpl.kt, src/main/java/com/hancekim/mynote/core/data/repository/SourceReconciler.kt, src/main/java/com/hancekim/mynote/core/data/repository/NoteDates.kt, src/main/java/com/hancekim/mynote/core/data/dto/NoteMapper.kt, src/test/java/com/hancekim/mynote/core/data/repository/NotesReconcileTest.kt, src/test/java/com/hancekim/mynote/core/data/repository/NoteFormatSyncTest.kt, src/test/java/com/hancekim/mynote/core/data/repository/MarkdownSyncTest.kt, src/test/java/com/hancekim/mynote/core/data/repository/SyncFailureKindTest.kt, src/test/java/com/hancekim/mynote/core/data/template/]}
  - {id: connection, name: Repository connection, aliases: [저장소 연결, 바꾸기, 끊기, 이어받기, 시작 복구, 연결 정보], code: [src/main/java/com/hancekim/mynote/core/data/connection/, src/main/java/com/hancekim/mynote/core/data/repository/ConnectionRepositoryImpl.kt, src/main/java/com/hancekim/mynote/core/data/repository/ConnectionRecovery.kt, src/test/java/com/hancekim/mynote/core/data/connection/, src/test/java/com/hancekim/mynote/core/data/repository/ConnectionRepositoryTest.kt]}
  - {id: device-notes, name: Device notes, aliases: [기기 노트, 새 노트 저장, 고치기, 지우기, 가져오기 파일 읽기], code: [src/main/java/com/hancekim/mynote/core/data/repository/DeviceNotesRepositoryImpl.kt, src/main/java/com/hancekim/mynote/core/data/datasource/ContentNoteFileReader.kt, src/test/java/com/hancekim/mynote/core/data/repository/DeviceNotesTest.kt, src/test/java/com/hancekim/mynote/core/data/datasource/]}
  - {id: records, name: Pass-through repositories, aliases: [북마크, 학습 기록, 주간 요약, 설정, 토큰 저장, 북마크 저장소, 북마크 기록, BookmarkRepositoryImpl, 북마크 저장], code: [src/main/java/com/hancekim/mynote/core/data/repository/BookmarkRepositoryImpl.kt, src/main/java/com/hancekim/mynote/core/data/repository/StudyLogRepositoryImpl.kt, src/main/java/com/hancekim/mynote/core/data/repository/InsightRepositoryImpl.kt, src/main/java/com/hancekim/mynote/core/data/repository/SettingsRepositoryImpl.kt, src/main/java/com/hancekim/mynote/core/data/repository/TokenRepositoryImpl.kt, src/test/java/com/hancekim/mynote/core/data/repository/BookmarkRepositoryImplTest.kt]}
---
## Overview
Data implements the domain repositories and owns the data-source interfaces that the outer modules implement: `network` implements the note-source adapter, `database` implements the local notes source and every store. It orchestrates repository-note sync (reconcile per source), the repository connection lifecycle (begin, first sync, confirm, discard, disconnect, start recovery) and the write path of device notes.
It implements neither remote nor local storage, knows no Room, Retrofit or DataStore type, and does not parse note bodies itself: every original goes through `NoteFormat` of `format` together with its path. Sorting of notes, sync triggers and every screen are elsewhere.

## Structure
- `contracts` is the boundary: interfaces (`NoteSourceAdapter`, `LocalNotesDataSource`, the stores) and boundary types (`CachedNote`, `SourceEntry`, `IndexNoteDto`). `network` and `database` depend on this module to implement them; this module never sees them at compile time.
- `sync` (`NotesRepositoryImpl` + `SourceReconciler`) writes repository notes; `connection` reuses the same reconciler for a connection attempt's first sync, on an in-memory source.
- `connection` holds `ConnectionRecovery`, which `sync`, every connection entry and the app's token sync wait for once per process before doing anything.
- `device-notes` writes the device source (`local`) through the same local notes source; `sync` re-derives its rows like any other.
- `records` are thin delegations from domain repositories to stores (bookmarks, study log, insights, settings, token).
- `data` binds all repositories in Hilt and provides the one shared `Json`; `app` completes the graph with `network` and `database`.
```mermaid
flowchart LR
  domain[domain use cases] --> sync & connection & devnotes[device-notes] & records
  connection --> sync
  sync & connection & devnotes & records --> contracts
  network[network adapter] -. implements .-> contracts
  database[database stores] -. implements .-> contracts
```
