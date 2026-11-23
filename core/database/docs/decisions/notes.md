# Decisions

### D-36 · 2026-10 · database:schema
- decision: The log tables arrive through automatic migrations (1 to 2 records, 2 to 3 summaries, 3 to 4 patterns), each adding only its own tables; the notes table is not touched.
- why: Hand-written migration SQL that differs from the entity by a character fails at open time; each stage fixes only the schema it needs.
- rejected: Hand-written SQL migrations; creating tables for later stages in advance.
- evidence: `NotesDatabase` (`autoMigrations`), `NotesDatabaseMigrationTest`, `core/database/AGENTS.md`.

### NF-D29 · 2026-10 · NF-8
- decision: A whole-note box does not put the original back into the block cache; it is read from the original column. The list never reads originals. An original over 1MB has no "show original" and re-conversion skips its row.
- why: So that no row of an input within the limits exceeds the database's one-row read window of 2MB, which would make every note on the home screen vanish.
- rejected: Keeping the original in the block cache.
- evidence: `core/database/src/test/java/com/hancekim/mynote/core/database/NoteRowBudgetTest.kt`; `core/database/src/androidTest/java/com/hancekim/mynote/core/database/NoteDaoTest.kt`

### DEC-2 · 2026-10 · database:schema
- decision: The v4 to v5 schema change (notes table to source_notes, study-record key prefix, quiz_attempts.format) is a hand-written Migration4To5, an exception to the "no hand-written SQL" rule of the database module. The CREATE statements are copied from the createSql of the generated 5.json schema.
- why: Table name and primary key change and data has to be converted, which an automatic migration cannot express.
- rejected: An automatic migration (it cannot rename the table and convert the rows).
- evidence: core/database migration/Migration4To5.kt; androidTest NotesDatabaseMigrationTest runs runMigrationsAndValidate(5, MIGRATION_4_5) from v1 and v4; the database is still at version 5 and schemas 1 to 5 exist.
