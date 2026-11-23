# Sync contract

Source: old feature document `docs/spec/sync.md` as of commit c8c54350 (source-ownership rules from the public-safety work of 2026-10-03, connection-aware sync from the repo-connect work of 2026-10-05) and the owner's earlier planning notes (untracked).

## Plan
- why: Sync read one repository fixed at build time and reported every failure as the same sentence; once the repository is connected inside the app, sync must follow the saved connection, stay quiet without one, tell failure causes apart and delete only what its own source owns.
- flow: App start, refresh, a changed token or a connection attempt starts sync; without a connection it ends with nothing to do; with one it reads that connection's list, fetches only changed notes and adds, updates or deletes that connection's notes; on failure the cache stays and the cause kind reaches the screen.
- not doing: No several repositories at once, no folder walking without a list file, no login or token refresh, no automatic retry or periodic background sync, no conditional requests (304) to save quota, no keeping the last failure kind across restarts.

## Items
| ID | What | Accept |
|---|---|---|
| SY-1 | Sync runs at app start, on refresh, on a changed applied token and on a connection attempt; a running sync is cancelled by a new one (cancel is not a failure); confirming a connection and rolling a failed attempt's token back do not restart sync | `ConnectionRepositoryTest` and `ConnectionUseCaseTest`: connection plus token saved together start one sync batch, zero extra requests after confirm or after the rollback |
| SY-2 | Requests are built from the saved connection (repository, branch, list location); a list item's `path` is relative to the repository root | `GithubManifestAdapterTest`: connection `a/b`, `dev`, `docs/index.json` sends `contents/docs/index.json?ref=dev`, item `notes/x/y.json` is fetched without `docs/` |
| SY-3 | Without a connection sync sends no request and ends as nothing to do, not success or failure; it never creates or revives a source; cached notes stay readable | `ConnectionRepositoryTest`, `DeviceNotesTest`, `GithubManifestAdapterTest`: no connection and 3 cached notes give 0 requests, no error, 3 notes kept, no source row appears |
| SY-4 | Sync adds, updates and deletes only notes of the connected source; another source's note with the same doc id is neither deleted nor changed | `NotesReconcileTest` and the instrumented note DAO test: same doc id in another source survives |
| SY-5 | Failures are told apart as no internet, token rejected (401), not accessible (404, or 403 that is not a limit), rate limit, server (5xx, timeout) and bad list format; the cache is unchanged; a non-kind exception in the connect flow's first sync becomes the server kind; the last kind reaches the settings card | `SyncFailureMappingTest`, `SyncFailureKindTest`, `ConnectionUseCaseTest`: each response maps to its kind; 429, 403 with remaining 0 and 403 with `retry-after` are limits, other 403 is not accessible |
| SY-6 | Sync right after connecting is silent; only a refresh the user pressed shows the loaded-notes snackbar | Manual: connect shows no snackbar, pressed refresh with changes shows one; no automated test |
| SY-K1 | A format error is isolated per note: an unknown block or missing field only boxes that block, a non-JSON file stays listed with a broken-note body, the rest is applied | `NotesReconcileTest`: isolates a format-error note, a non-JSON note stays listed as `BrokenNote` |
| SY-K2 | If any body transfer fails nothing is applied; an unreadable list applies nothing | `NotesReconcileTest`: one failed body applies nothing, an unparseable index applies nothing |
| SY-K3 | Delete protection: an empty list for a source that still has notes deletes nothing and fails | `NotesReconcileTest`: empty source index with remaining notes fails without deleting |
| SY-K4 | An equal hash skips the body, a different hash refetches and updates, a note missing from the list is deleted | `NotesReconcileTest`: same revision not refetched, different revision refetched, absent note deleted |
| SY-K5 | Refetching bodies to fill raw text after an update is not counted as a change | `NotesReconcileTest`: an old row without raw text refetches once and is not counted |
| SY-K6 | A source whose kind has no adapter is neither synced nor deleted | `NotesReconcileTest`: unknown-kind source is left alone |
| SY-K7 | A note that is open and gets deleted closes at once | Manual on an emulator; no automated test |
