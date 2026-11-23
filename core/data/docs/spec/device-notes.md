---
units: [data:device-notes]
plans: [013-local-notes]
decisions: [LN-D2, LN-D3]
---
## device-notes — Device notes
### Behaviour
- Device notes live in the same notes table as repository notes under the device source `local` (kind local); the app database is their only copy and they reach the screen through the same note list (013-local-notes: LN-14).
- Saving refuses a draft that fails the domain save check (blank title, blank body, stored original over 524,288 bytes) with a failed result carrying the check; nothing is written (013-local-notes: LN-3).
- A new note gets a random UUID doc id, the path `<doc id>.md` and the key `local/<doc id>`; the key never collides with a repository note key and is returned to the caller (013-local-notes: LN-5).
- The stored original is an empty front matter (8 bytes) followed by the body, so a body whose first line is `---` stays body and the body limit is 524,280 bytes (013-local-notes: LN-3).
- Blocks and the excerpt are derived from that original and path by the same Markdown rules as repository notes; a body over another limit is stored as a box (013-local-notes: LN-3, LN-14).
- Title and group are stored as one line of at most 200 characters; an empty group becomes the default group name the caller passes, and a group with the same name as a repository group is the same group (013-local-notes: LN-3, LN-4).
- The date is the imported date when the draft has one, else today in the device time zone, stored as `YYYY.MM.DD`; the first-seen time is the save time (013-local-notes: LN-4).
- The first save creates the device source row in the same transaction when it is missing.
- Editing accepts only a device key; it rewrites title, group, original and derived columns and keeps key, date and first-seen time, so bookmarks and study records stay attached (013-local-notes: LN-4, LN-5, LN-9).
- Editing a note that no longer exists, or a repository note, gives a failed result; repository notes cannot be edited or deleted, also at the storage level (013-local-notes: LN-9).
- An edit that lands while a re-derive is running keeps the new body: re-deriving only rewrites rows still below the current parse version (013-local-notes: LN-9).
- Deleting removes the note, then the bookmarks under its key; if the note was already gone the bookmarks are still removed and the result is a failure (013-local-notes: LN-11).
- If removing the note throws, the bookmarks stay; a crash between the two steps leaves a bookmark without a note, which the bookmarks screen hides (013-local-notes: LN-11).
- Deleting never touches study records (013-local-notes: LN-11).
- Loading for edit returns nothing for a non-device key, a missing note or an original over 1 MB; otherwise title, group, body without the empty front matter, and date.
- Preview parses the body exactly as the reader will after saving (013-local-notes: LN-3).
- Sync, start recovery, change, disconnect and the seed connection never touch device notes (013-local-notes: LN-12).
- Reading a picked file: the display name is the provider's name, else the last address segment; reading stops at one byte over the limit and reports too large; the rest of the file is never read (013-local-notes: LN-7).
- Text is decoded as strict UTF-8 with no replacement characters; malformed or truncated input is reported as not UTF-8; a leading byte-order mark is kept in the text (013-local-notes: LN-7).
- An unreadable or vanished file throws; file reading runs off the main thread (013-local-notes: LN-6, LN-7).
### Public API
- `DeviceNotesRepositoryImpl` — domain `DeviceNotesRepository`: `create(draft, defaultGroup)` gives the new key, `update(noteId, draft, defaultGroup)`, `delete(noteId)`, `load(noteId)` gives a draft or null, `preview(body)` gives blocks.
- `ContentNoteFileReader` — domain `NoteFileReader` over `content://` addresses: `displayName(uri)` and `read(uri, maxBytes)` giving read text, too large or not UTF-8.
### Guarantees
- Save, size boundary, `---` first line, grouping, keys, edit keeping key, date and bookmarks, overlap with re-derive, delete, untouched by sync and connection flows, same reading features, previous-version data read as before: `DeviceNotesTest`.
- Import routes share one rule, five distinct rejection reasons, 512 KB boundary (524,288 accepted, 524,289 rejected), reading stops at 524,289 bytes, nothing exists before Save: `NoteImportTest` (013-local-notes: LN-6, LN-7).
### Not covered
- The import rule (front matter, heading, name hint, rejection order) — domain `ImportNoteUseCase` and `format`.
- The save check, field normalising and default group wording — domain and `editor`.
- Edit and delete buttons, confirmations and closing the reader — `editor`, `notes`.
- No export, no upload to a repository, no turning a repository note into a device note.
