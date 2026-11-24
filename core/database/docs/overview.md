---
id: database
name: Database
aliases: [데이터베이스, 로컬 저장소, 노트 캐시, notes.db]
uses: [data, domain]
units:
  - {id: notes, name: Notes table, aliases: [노트 캐시, 노트 DB, 소스 노트, 기기 노트 저장, source_notes], code: [src/main/java/com/hancekim/mynote/core/database/dao/NoteDao.kt, src/main/java/com/hancekim/mynote/core/database/entity/SourceNoteEntities.kt, src/main/java/com/hancekim/mynote/core/database/datasource/LocalNotesDataSourceImpl.kt, src/main/java/com/hancekim/mynote/core/database/datasource/SourceKindColumn.kt, src/test/java/com/hancekim/mynote/core/database/NoteRowBudgetTest.kt, src/test/java/com/hancekim/mynote/core/database/datasource/SourceKindColumnTest.kt, src/androidTest/java/com/hancekim/mynote/core/database/NoteDaoTest.kt, src/androidTest/java/com/hancekim/mynote/core/database/DeviceNoteDaoTest.kt]}
  - {id: schema, name: Schema and migrations, aliases: [스키마, 마이그레이션, DB 버전, Hilt 묶음], code: [src/main/java/com/hancekim/mynote/core/database/NotesDatabase.kt, src/main/java/com/hancekim/mynote/core/database/migration/, src/main/java/com/hancekim/mynote/core/database/di/, src/androidTest/java/com/hancekim/mynote/core/database/NotesDatabaseMigrationTest.kt]}
  - {id: study-log, name: Study records, aliases: [학습 기록, 읽기 세션, 퀴즈 시도, 북마크 이벤트], code: [src/main/java/com/hancekim/mynote/core/database/dao/StudyLogDao.kt, src/main/java/com/hancekim/mynote/core/database/entity/StudyLogEntities.kt, src/main/java/com/hancekim/mynote/core/database/datasource/StudyLogStoreImpl.kt, src/main/java/com/hancekim/mynote/core/database/datasource/BookmarkEventKindColumn.kt, src/main/java/com/hancekim/mynote/core/database/datasource/QuizFormatColumn.kt, src/test/java/com/hancekim/mynote/core/database/datasource/BookmarkEventKindColumnTest.kt]}
  - {id: insights, name: Summary cache and worker status, aliases: [주간 요약, 패턴 캐시, 워커 상태, 통계 요약 저장], code: [src/main/java/com/hancekim/mynote/core/database/dao/InsightDao.kt, src/main/java/com/hancekim/mynote/core/database/entity/InsightEntities.kt, src/main/java/com/hancekim/mynote/core/database/datasource/InsightStoreImpl.kt, src/main/java/com/hancekim/mynote/core/database/datasource/DataStoreInsightStatusStore.kt, src/main/java/com/hancekim/mynote/core/database/datasource/InsightStatusColumns.kt, src/test/java/com/hancekim/mynote/core/database/datasource/InsightStatusColumnsTest.kt]}
  - {id: bookmarks, name: Bookmark store, aliases: [북마크 저장, 북마크 키 이전, 북마크 키, 북마크 키 바꾸기, 북마크 DataStore], code: [src/main/java/com/hancekim/mynote/core/database/datasource/DataStoreBookmarkStore.kt, src/main/java/com/hancekim/mynote/core/database/datasource/BookmarkKeyMigration.kt, src/test/java/com/hancekim/mynote/core/database/datasource/BookmarkKeyMigrationTest.kt]}
  - {id: prefs, name: Settings, token and connection files, aliases: [설정 저장, 토큰 저장, 글자 크기 저장, 연결 상태 저장, DataStore], code: [src/main/java/com/hancekim/mynote/core/database/datasource/DataStoreSettingsStore.kt, src/main/java/com/hancekim/mynote/core/database/datasource/FontScalePreference.kt, src/main/java/com/hancekim/mynote/core/database/datasource/DataStoreTokenStore.kt, src/main/java/com/hancekim/mynote/core/database/datasource/DataStoreConnectionStateStore.kt, src/test/java/com/hancekim/mynote/core/database/datasource/FontScalePreferenceTest.kt]}
---
## Overview
Owns every on-device store behind the `:core:data` contracts: the Room 3 database `notes.db` (notes of every source, study records, weekly summaries, quiz patterns) and the DataStore files (settings, token, connection state, bookmarks, worker status). It implements those contracts and keeps Room and DataStore types inside the module.
The notes table is the single source of truth for what screens list. It holds the only copy of device notes, so the database is not a disposable cache: it is never rebuilt destructively.
It does not decide what to store or when: sync rules, re-parse, connect flows and statistics maths live in `data`, `domain` and the feature modules. It does not parse note text and does not know the quiz model.
## Structure
- `notes` is the notes table plus its DAO and the `LocalNotesDataSource` implementation that `data` calls for sync, re-parse and device-note writes.
- `schema` declares `NotesDatabase` (version 5), the one hand-written migration and the Hilt modules that bind every store in this module.
- `study-log` and `insights` live in the same database file as `notes` but are unrelated to it; `insights` also owns the worker-status DataStore file.
- `bookmarks` and `prefs` are DataStore files with no link to Room; bookmark keys are note keys, so they follow the key scheme `notes` stores.
- Callers see only the `:core:data` interfaces; every entity, DAO and row type is internal.
```mermaid
flowchart LR
  data[data contracts] --> notes
  data --> study-log
  data --> insights
  data --> bookmarks
  data --> prefs
  schema --> notes
  schema --> study-log
  schema --> insights
```
