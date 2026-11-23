# Decisions

### BM-D5 · 2026-10 · BM-18
- decision: When a sync reports deleted notes, their bookmarks are deleted too; the display still joins bookmarks with existing notes.
- why: With no screen able to remove an orphan, orphans would stay forever; the join stays because a hidden note is not in the deleted list.
- rejected: Never deleting and letting a returning note revive its bookmark.
- evidence: `NotesViewModel` (`afterSyncRun`), `BookmarkMapperTest` "LN-11 노트 없는 북마크 키는 보이지 않는다".

### BM-D15 · 2026-10 · BM-18
- decision: Bookmark cleanup after a sync runs in the notes ViewModel's sync-success branch as its own safe launch, not inside the sync use case or repository.
- why: A failed bookmark write must not turn into a "sync failed" message, and the domain module cannot use the cancellation-safe wrapper.
- rejected: Putting the cleanup inside `SyncNotesUseCase` or `NotesRepositoryImpl`.
- evidence: `NotesViewModel` (`afterSyncRun`).

### DEC-36c · 2026-10 · SY-1
- decision: The notes view model starts its sync flow with SharingStarted.Lazily. If the flow ends with an exception it does not run again while the view model lives; sync failures arrive as Result values, so only a repository read error ends it.
- why: Under screen re-subscription the flow used to be revived, so sync reran on every return to the screen.
- rejected: A while-subscribed sharing policy that restarts on re-subscription.
- evidence: NotesViewModel in feature/notes (sync trigger with SharingStarted.Lazily); FM-K10 in the old feature-modules document.
