# Decisions

### LN-D9 · 2026-10 · LN-4
- decision: The group is optional and empty goes to one default group; the default group is stored as the screen wording text itself, and a group with the same name is mixed with repository notes and told apart by the source mark.
- why: The app is single-language, and the stats category snapshot and the summary model input use that value as is.
- rejected: A separate empty-value marker, which would require changing home, stats and summary input.
- evidence: `core/domain/src/main/java/com/hancekim/mynote/core/domain/model/DeviceNote.kt`, `DeviceNotesTest` (`LN-4 그룹 칸을 비우면 기본 그룹이다`).
