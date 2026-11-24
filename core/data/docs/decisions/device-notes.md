# Decisions

### LN-D2 · 2026-10 · LN-9
- decision: Repository notes are not edited in the app (read-only as before); writing is for device notes only.
- why: Editing repository notes would bring the conflicts of uploading to the repository (stage 3) in now.
- rejected: Letting the app edit and delete repository notes.
- evidence: `core/data/src/test/java/com/hancekim/mynote/core/data/repository/DeviceNotesTest.kt` (`LN-9 저장소 노트에는 고치기와 지우기가 없다`).

### LN-D3 · 2026-10 · LN-6
- decision: Import has three routes: sharing text from another app, opening a `.md` file through the system file picker, and pasting; fetching a web address is out of scope.
- why: These three work without permissions or network.
- rejected: Fetching and scraping the body from a web address.
- evidence: `core/data/src/main/java/com/hancekim/mynote/core/data/datasource/ContentNoteFileReader.kt`, `feature/editor/src/main/java/com/hancekim/mynote/feature/editor/screen/PasteScreen.kt`.
