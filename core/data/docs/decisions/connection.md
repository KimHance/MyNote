# Decisions

### SY-D10 · 2026-10 · SY-1
- decision: Confirming a connection does not restart sync, and neither does the token rollback when a failed attempt is discarded; every connection change is check, first sync (before confirm), confirm.
- why: A duplicate request right after confirm or discard eats the unauthenticated 60 requests per hour.
- rejected: Re-running sync after confirm or after the rollback to refresh the list.
- evidence: `ConnectionRepositoryTest` (`SY-1 확정 때 옛 토큰을 지워도 동기화는 다시 시작되지 않는다`, `SY-1 실패 폐기로 토큰을 되돌려도 동기화는 다시 시작되지 않는다`), `ConnectionUseCaseTest` (`SY-1 확정 뒤 동기화는 다시 시작되지 않는다`).

### RC-D4 · 2026-10 · RC-14, RC-15
- decision: One connection at a time; changing to another repository disconnects the old one.
- why: Several repositories stay a non-goal; the several-source structure exists for link notes and device folders.
- rejected: Connecting several repositories at once.
- evidence: `ConnectionRepositoryImpl.kt`, `ConnectionRepositoryTest`.

### RC-D5 · 2026-10 · RC-15, RC-16, RC-17
- decision: Changing or disconnecting removes the old connection's notes and those notes' bookmarks, and keeps study records (reading, quiz, statistics); a same-repository branch or location change deletes nothing; bookmarks are removed on the source side with the notes.
- why: The deletion range closes around one disconnected source, matching "a source deletes only its own notes"; note keys carry no repository coordinate, so leaving old keys would let a same article id in a new repository attach to an old bookmark.
- rejected: Keeping the old bookmark keys, and deleting bookmarks from the screen layer.
- evidence: `ConnectionRepositoryTest` (`RC-15 성공하면 옛 소스의 노트와 북마크만 지운다`, `RC-17 끊으면 그 소스의 노트 북마크 토큰만 지운다`), `NoteDaoTest`.

### RC-D6 · 2026-10 · RC-15
- decision: Changing the connection is confirmed only after the new repository's check and first sync succeed; before that the old notes stay visible and new notes do not mix in; a failure leaves old connection, notes and token as they were.
- why: One wrongly pasted link must not erase the notes on this device.
- rejected: Switching the connection first and syncing afterwards.
- evidence: `ConnectionRepositoryTest` (`RC-15 새 저장소 동기화가 실패하면 옛 연결이 남는다`, `RC-15 확정 중 구독자는 새 노트와 옛 노트를 같이 보지 않는다`).

### RC-D9 · 2026-10 · RC-19
- decision: On a device that used the app before the update, the first connected repository adopts the existing repository notes and bookmarks (same article id keeps its bookmark, only notes missing from the new repository are removed after a successful first sync).
- why: Old notes carry no record of which repository they came from; removing the code default is otherwise a path that deletes the bookmarks of the author's own device.
- rejected: Hiding or discarding old notes at first connect.
- evidence: `ConnectionRepositoryTest` (`RC-19 시드 없는 옛 노트는 처음 연결하는 저장소가 이어받는다`, `RC-19 이어받기 중 죽어도 옛 노트와 북마크는 남는다`).

### RC-D19 · 2026-10 · RC-15, RC-17, RC-19
- decision: Unconfirmed marker, adoption, disconnect marker and the pre-attempt token live in existing storage with no schema change: before confirm the new connection carries an unconfirmed flag inside the connection info and confirm removes it and deletes the old connection in one local transaction; unconfirmed notes are filtered in one place; the pre-attempt token has its own storage slot restored at start; disconnect goes marker, notes, bookmarks, token and finishes the remaining steps at start.
- why: The notes store (Room) and bookmarks, token and markers (DataStore) cannot share one transaction, and after a crash the user may see only "old state" or "fully disconnected".
- rejected: A database schema bump and a single cross-store transaction.
- evidence: `ConnectionRecovery.kt`, `ConnectionStateStore.kt`, `ConnectionRepositoryTest` (`RC-15 확정 전 중단은 옛 연결로 돌아간다`, `RC-17 끊기가 중간에 죽어도 재시작이 남은 단계를 마친다`).

### RC-D20 · 2026-10 · RC-15
- decision: The confirmation dialog for a change appears after the check succeeded, and the number of notes to delete is counted then, so a mistyped link never shows a deletion warning.
- why: Showing "N notes will be deleted" before a wrong link is rejected would only scare the user.
- rejected: Asking for confirmation before the check.
- evidence: `ConnectFlowTest` (`RC-15 틀린 링크에는 삭제 확인이 안 뜬다`), `ConnectConfirmTest`.

### RC-D28 · 2026-10 · RC-15
- decision: When changing from a private to a public repository the old token is deleted at confirm; conditions are a repository change, public by the unauthenticated check and no candidate token; a public connection whose token was entered for the limit and a same-repository change keep the token.
- why: It follows "a public repository stores no token"; a public connection with a leftover token would confuse the card as "public plus token".
- rejected: Keeping the old token after switching to a public repository.
- evidence: `ConnectionRepositoryTest` (`RC-15 공개 저장소로 바꾸면 확정 때 옛 토큰을 지운다`, `RC-15 같은 저장소 변경은 확정 때 토큰을 지우지 않는다`, `RC-15 비공개로 바꾸면 토큰을 지우지 않는다`).

### RC-D29 · 2026-10 · RC-19
- decision: Adoption and same-repository change write nothing before confirm (neither connection info nor the unconfirmed flag); everything is written once at confirm, and the first sync runs on an in-memory connection.
- why: With no write window a crash can only leave the state as it was.
- rejected: Writing an unconfirmed connection first and clearing the connection info to go back on failure.
- evidence: `ConnectionRepositoryTest` (`RC-19 이어받기 첫 동기화가 실패하면 연결 없음과 옛 노트로 돌아간다`).

### RC-D34 · 2026-10 · RC-16
- decision: The statistics list does not show deleted notes; their study record rows stay in storage and totals and charts are unchanged; there is no design for deleted notes.
- why: A to-do list such as "notes you stopped reading" with an entry that cannot be opened is confusing, and it needs no domain change.
- rejected: Showing deleted notes in the statistics list with a marker.
- evidence: `ConnectionRepositoryTest`, `NoteDaoTest` (record rows survive removal).

### DEC-9 · 2026-10 · RC-12, RC-23
- decision: The connected repository (owner, repo, branch, list location, public flag, canonical name) and the "not yet confirmed" marker with its attempt id live in one JSON column, sources.config_json, not in new Room columns. The notes query hides unconfirmed sources with a LIKE test on the marker.
- why: A schema bump to v6 raises the downgrade risk and needs another manual migration. Hiding unconfirmed notes in exactly one query guarantees old and new notes never show together at the moment of confirmation.
- rejected: Real columns for pending and attempt id (schema v6).
- evidence: ConnectionConfigCodec.PENDING_MARKER used by NoteDao.observeNotes; confirmation is one Room transaction (promote); NoteDaoTest checks the filter.

### DEC-10 · 2026-10 · RC-6, RC-17
- decision: The rest of the connection state (token before the attempt, disconnect marker, used connection ids, bookmark clean-up list) lives in the DataStore connection_state, not in Room. Disconnect runs in a fixed order (marker, notes in Room, bookmark prefix, token) and ConnectionRecovery finishes the remaining steps at start-up.
- why: Tokens and bookmarks already live in DataStore and two stores cannot share one transaction, so order plus resume stands in for atomicity.
- rejected: Putting the state into Room (schema bump) or a single transaction across both stores.
- evidence: ConnectionRepositoryImpl.disconnect and ConnectionRecovery in core/data; DataStoreConnectionStateStore in core/database; ConnectionRepositoryTest uses JVM fakes. The DataStore implementation itself has no instrumented test.

### DEC-36a · 2026-10 · RC-6
- decision: Only the token saved before the attempt is excluded from backup, in its own file (connection_attempt_token), not the whole connection_state store. The used connection ids stay in the backup.
- why: Used connection ids must survive a restore so an id is never reused (RC decision 17).
- rejected: Excluding all of connection_state from backup.
- evidence: backup_rules.xml and data_extraction_rules.xml exclude datastore/connection_attempt_token.preferences_pb; notes-tests test_backup_rules.py; DataStoreConnectionStateStore uses a separate attemptTokenDataStore.
