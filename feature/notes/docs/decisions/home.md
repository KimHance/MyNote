# Decisions

### RC-D44 · 2026-10 · RC-21
- decision: The home connect-hint band sits on top of the home grid in Compact and on top of the list column (below the search field, scrolling with the list) in Medium and up; the detail column keeps showing recent notes, with no empty-detail "pick a note" design; a connected home that is loading shows the receiving empty state instead of a centred spinner.
- why: The empty-detail design of the mock differs from the existing Medium-and-up home, and the home structure stays untouched.
- rejected: The mock's empty detail column and a spinner while loading.
- evidence: `HomeBodyTest`, `CategoryLoadTest`, `feature/notes/.../screen/HomeEmpty.kt`.

### RC-D52 · 2026-10 · RC-21
- decision: The receiving state is judged by signals, not time: from the confirm until the new connection's note list is first emitted (and during the first sync); a first emission is judged at once, an empty repository goes straight to no notes.
- why: Confirm was emitted before the notes, so a no-notes frame flashed at confirm (reproduced on a device); a one-second timer flashes again on a late emission.
- rejected: A one-second grace timer.
- evidence: `CategoryLoadTest` (`RC-21 확정 직후 노트가 늦게 와도 노트 없음이 비치지 않는다`, `RC-21 빈 저장소는 첫 방출 즉시 노트 없음이다`).

### BR-D3 · 2026-10 · BR-6
- decision: The home hero is the A mark, the `mynote` wordmark and the connected owner/repo; the home bar title and the reading screen title are MyNote. No title field is added to the notes list (`index.json`).
- why: Owner decision. The list's top-level keys are group names, so an added field could break the sync of old apps.
- rejected: Adding a repository title field to `notes/index.json` to use as the app title.
- evidence: `core/design-system/src/main/java/com/hancekim/mynote/core/designsystem/component/MyNoteHero.kt`; `feature/notes/src/test/java/com/hancekim/mynote/feature/notes/element/HeroRepoTest.kt`

### LN-D4 · 2026-10 · LN-1
- decision: The first run shows the home (empty state with the New note and Import paths) instead of the connect screen; connecting a repository is an optional entry (owner decision).
- why: A start that needs a repository created before any note shows is hard.
- rejected: Keeping the connect screen as the first screen for no-connection and zero notes.
- evidence: `feature/notes/src/main/java/com/hancekim/mynote/feature/notes/element/HomeBody.kt`, `feature/notes/src/test/java/com/hancekim/mynote/feature/notes/element/HomeBodyTest.kt`.

### LN-D8 · 2026-10 · LN-5
- decision: The source mark shows only on device notes; repository notes keep their look.
- why: The screens of people who already use repositories are not disturbed.
- rejected: Marking the source on every note, repository notes included.
- evidence: `core/data/src/test/java/com/hancekim/mynote/core/data/repository/DeviceNotesTest.kt` (`LN-5 출처 표시는 기기 노트에만 있다`).

### LN-D13 · 2026-10 · LN-2
- decision: The two entries of a home with notes (New note, Import) are two labelled secondary buttons and also show in the connected syncing and no-notes states.
- why: Writing is now the main path, and an unlabelled icon is not found by a first-time user.
- rejected: Unlabelled icon entries.
- evidence: `feature/notes/src/main/java/com/hancekim/mynote/feature/notes/element/HomeEntry.kt`, `HomeBodyTest` (`LN-2 두 입구는 홈 상태별 표대로 보인다`).

### DEC-16 · 2026-10 · RC-21
- decision: After a connection changes, the home screen stays in the receiving state until the first emission of the notes list for the new source, then decides list or empty at once.
- why: Right after confirmation the "repository has no notes" text flashed before notes arrived; a one-second timer flashed again on a late emission.
- rejected: A fixed one-second timer before showing the empty state.
- evidence: CategoryLoad (loaded, settling) driven by NotesViewModel in feature/notes; no timer in the view model.
