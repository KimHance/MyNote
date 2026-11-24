---
units: [data:connection]
plans: [006-sync, 007-repo-connect, 013-local-notes]
decisions: [SY-D10, RC-D4, RC-D5, RC-D6, RC-D9, RC-D19, RC-D20, RC-D28, RC-D29, RC-D34, DEC-10, DEC-36a]
---
## connection — Repository connection
### Behaviour
- A connection is one JSON value on its source row: owner, repo, branch, list location, public flag, plus a pending flag and the id of the attempt that confirmed it; there is one connection at a time and no schema change (007-repo-connect: RC-12).
- A stored value that is blank, unreadable, or missing owner, repo or list location reads as no connection; unknown keys are ignored (007-repo-connect: RC-12).
- The confirmed connection is the first GitHub source whose stored value decodes and is not pending; the device source has no stored value and is never a connection (007-repo-connect: RC-12).
- Observing the connection first waits for start recovery, then follows the source rows and emits connected (source id and connection) or none, without repeats.
- "Attempting" is true from `begin` until `confirm` or `discard` ends, in memory only.
- `begin` sorts an attempt into one of three kinds: same repository (the confirmed connection's owner and repo, ignoring case) keeps that source id (007-repo-connect: RC-14).
- New: another repository while connected, or a first connection with nothing to adopt; it gets a new source id and a source row marked pending, which normal sync skips (007-repo-connect: RC-15).
- Adopt: no confirmed connection and a GitHub source without a stored connection that holds at least one note; nothing is written to its row before confirmation (007-repo-connect: RC-19).
- Device notes are never adopted and never make a first connection an adoption (007-repo-connect: RC-19) (013-local-notes: LN-12).
- `begin` records the attempt (target source, a fresh attempt id, the token before it, whether a candidate token came with it) before saving that candidate token; a `begin` that finds "attempting" still true first runs recovery to clear the leftover attempt (007-repo-connect: RC-6).
- New source ids come from the generator; a blank id, an id with `/`, an id used before (kept across deletes), an existing source id, `s0` and `local` are skipped; after 16 rejected tries `begin` fails (013-local-notes: LN-5).
- The first sync of an attempt reconciles an in-memory source with the target connection under the attempt's source id; for adopt and same-repository attempts it also removes the bookmarks of notes it deleted.
- That first sync uses the same reconcile rules as ordinary sync and returns a failed result instead of throwing.
- `confirm` runs to the end even when cancelled and always clears "attempting"; the stored value gets the attempt id (007-repo-connect: RC-15).
- Confirming a new connection writes the replaced GitHub source ids to the bookmark sweep list, promotes the pending source while deleting the replaced sources' notes and rows in one transaction, then removes their bookmarks by prefix and clears the list (007-repo-connect: RC-15).
- A subscriber never sees old and new repository notes together, because promotion is one transaction (007-repo-connect: RC-15).
- Confirming a change to a public repository removes the old token, unless the token was a candidate of this attempt (a token typed after a rate limit stays), then waits up to 5 seconds for the applied token to become empty so sync is not started twice (007-repo-connect: RC-15) (006-sync: SY-1).
- A same-repository change or a change to a private repository keeps the token; confirming a same-repository or adopt attempt only rewrites the stored value (007-repo-connect: RC-14, RC-15).
- Confirmation clears the attempt record and the seed block (007-repo-connect: RC-19).
- `discard` runs to the end even when cancelled: it restores the token held before the attempt (waiting up to 5 seconds for it to be applied), deletes a new attempt's source only while it is still pending, and clears the attempt (007-repo-connect: RC-6) (006-sync: SY-1).
- Disconnect order is marker (which also blocks the build seed), notes and source row in one transaction, bookmarks by prefix, token, marker cleared; the seed block stays until a later connection is confirmed (007-repo-connect: RC-17, RC-19).
- If deleting the notes fails, the marker and seed block are cleared when the row still exists, bookmarks and token stay, and the result is a failure (007-repo-connect: RC-17).
- Disconnect without a connection succeeds and does nothing; disconnect never touches the device source or other sources (013-local-notes: LN-12).
- Changing or disconnecting never touches study records or settings (007-repo-connect: RC-16).
- The change warning counts the confirmed source's notes and bookmarks (or, with no connection, the adoptable source's); device notes and their bookmarks are not counted (007-repo-connect: RC-15) (013-local-notes: LN-12).
- The adoption warning counts only the adoptable notes missing from the new list (doc ids the check already read, no extra request) and their bookmarks (007-repo-connect: RC-15).
- The seed is eligible only with no confirmed connection and no seed block (007-repo-connect: RC-19).
- Start recovery runs once per process (concurrent callers wait for the one run) before the connection is observed, counted, begun, synced or disconnected, and before ordinary sync.
- Recovery step 1 finishes a disconnect: with a marker whose row is gone it removes that prefix's bookmarks, clears the token and the marker; with the row still present it clears marker and seed block, since nothing was deleted (007-repo-connect: RC-17).
- Recovery step 2 deletes pending GitHub sources, step 3 deletes notes whose source row is missing (007-repo-connect: RC-15, RC-17).
- Recovery step 4 restores the pre-attempt token when the attempt's source row does not carry that attempt's id (not confirmed) and the token differs, then clears the attempt; a confirmed attempt keeps its token (007-repo-connect: RC-6).
- Recovery step 5 removes bookmarks of swept source ids whose rows are gone, then clears the sweep list (007-repo-connect: RC-15).
- Recovery never deletes device notes (013-local-notes: LN-12).
### Public API
- `ConnectionRepositoryImpl` — domain `ConnectionRepository`: `observeConnection`, `observeAttempting`, `countErasable`, `countAdoptionErasable`, `isSeedEligible`, `begin`, `syncPending`, `confirm`, `discard`, `disconnect`, `recover`.
- `ConnectionRecovery` — singleton start recovery: `ensure()` (once per process) and `run()` (one pass).
- `ConnectionConfigCodec` — `encode(connection, pending, attemptId)`, `decode(json)` giving `DecodedConfig` or null, and `PENDING_MARKER`, the exact pending text that the note list query of `database:notes` matches to hide a pending source's notes.
- `DecodedConfig` — decoded connection, pending flag and attempt id.
### Guarantees
- Restart keeps the connection and sync uses it over the seed; canonical name stored; same-repository change deletes nothing; failed or aborted change keeps the old state; confirm after promotion survives cancellation; recovery restores tokens and finishes a disconnect; seed not revived after disconnect; confirm or rollback does not restart sync: `ConnectionRepositoryTest`.
- Round trip of the stored value, unreadable value as no connection, the pending marker matching the encoding, case-insensitive same repository: `ConnectionConfigCodecTest`.
- Device notes survive change, disconnect, a crashed disconnect and seed connection, and are not counted: `DeviceNotesTest`.
### Not covered
- Reading a link and checking the repository before `begin` — `domain:connect-link`, `domain:connection` and `network:probe`.
- The order begin, token apply, first sync, confirm or discard, the seed consumption and turning an unexpected exception into a kind — `domain:connection`.
- Applying the token to HTTP requests — `network:token` and the app's token sync (`app:startup`).
- Dialog wording — `connect:confirm`; the start screen choice and the screen clean-up (closing the reader) on a connection change — `app:shell-state`.
- No several repositories at once, no token encryption or expiry, no keeping the last failure kind across restarts, no re-attaching old bookmarks after reconnecting the same repository.
### Unconfirmed
- If the process dies after promotion and before the old token is removed, the old private token stays; this narrow gap has no test.
