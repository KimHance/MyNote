---
units: [domain:device-notes]
plans: [013-local-notes]
decisions: [LN-D9]
---
## device-notes — Device notes
### Behaviour
- The editor value (a draft) has title, group, body and an optional date. A new note, a note prefilled by an import and a note being edited all use the same shape. A null date means the day of saving; an imported or edited note carries its own date (013-local-notes: LN-3).
- The body is plain Markdown text. A first line of three dashes stays part of the body and is not read as front matter (013-local-notes: LN-3).
- Saving is allowed only when the title is not blank, the body is not blank (spaces, tabs and newlines count as blank) and the body is within the size boundary. The check names each reason separately: title blank, body blank, body too large (013-local-notes: LN-3).
- The size boundary is the reader's original limit (512 KB), judged by the same code the reader uses; the check only counts bytes up to the boundary, so a screen may run it on every keystroke (013-local-notes: LN-3).
- Title and group are one line of at most 200 characters (code points, so an emoji counts one). Each newline (LF, CRLF or CR) becomes one space and counts as a character; anything past 200 is dropped (013-local-notes: LN-3).
- The saved title is that single line trimmed at both ends. The saved group is that single line trimmed at both ends, with case kept, so `Kotlin` and `kotlin` are two groups; an empty group takes the default group (013-local-notes: LN-4).
- The default group name is screen wording, so the caller passes it in; the domain holds no wording and applies the same one-line rule to it as to any group (013-local-notes: LN-4).
- Saving with no note id creates a new note and returns its id; saving with an id updates that device note and returns the same id (013-local-notes: LN-3, LN-9).
- Update keeps the key, the date and the first-seen time; a note that is not a device note, or is missing, fails. Loading for edit returns the current title, group, body and creation date, or nothing for the same two cases (013-local-notes: LN-9).
- Delete removes the note and its bookmarks; study records stay. Closing the reader or the quiz of the deleted note is the screen's work. A note that is not a device note, or is missing, fails (013-local-notes: LN-11).
- A draft that cannot be saved is refused by the repository implementation with an exception that carries the check result; the use case does not check again.
- Preview returns the blocks the reader would draw after saving, parsed off the main thread by the same route and rules as a saved note (013-local-notes: LN-3).
- A note date is shown and parsed as `YYYY.MM.DD`, the shape repository notes use. Surrounding whitespace is trimmed first; a text that is then not exactly that shape or not a real day (`2026.02.30`, `2026-10-04`, `2026.1.4`) parses to nothing (013-local-notes: LN-4).
- Import has three entries and none of them creates a note: shared text and pasted text go through one call, a picked file through another. A result is either a prefilled draft to open in the editor, a rejection with a reason, or dismissed (013-local-notes: LN-6).
- Text import is judged in this order: original over 512 KB (524,288 bytes accepted, 524,289 rejected) is too large, then blank is empty; otherwise the rules (in `format`) fill the draft: line ends and a byte-order mark are tidied, then front-matter title and date, then a first `# ` heading line, then the name hint, with today as the fallback date (013-local-notes: LN-6, LN-7).
- File import checks, in order: no file chosen is dismissed with no message; a display name whose extension is not `.md` or `.markdown` (case ignored) or that is missing is an unsupported type, before any read; then the read. A read that returns too large or not UTF-8 is rejected as such; any other failure while naming or reading, such as lost permission or a vanished file, is unreadable; cancellation is not swallowed (013-local-notes: LN-7).
- The name hint of a file is its display name without the extension (`a.b.Markdown` gives `a.b`); a blank hint is passed as none. For shared text the hint is the share subject, and pasted text has none (013-local-notes: LN-6).
- There are exactly five rejection reasons, each with its own message to the person: empty, not UTF-8, too large, unreadable, unsupported type (013-local-notes: LN-7).
- A file is read as strict UTF-8 and the read stops at the limit plus one byte, so a huge file is never loaded whole.
### Public API
- `DeviceNoteDraft`, `DraftCheck` (`canSave`), `DeviceNoteDraft.check(text)`, `InvalidDraftException` — the editor value and its check.
- `DeviceNoteFields` (`MAX_LENGTH`, `singleLine`, `title`, `group`) and `NoteDates` (`format`, `parse`) — the field and date rules.
- `ImportOutcome` (`Ready`, `Rejected`, `Dismissed`), `ImportRejection`, `FileText` (`Read`, `TooLarge`, `NotUtf8`), `ImportFileTypes` (`accepts`, `stem`).
- `DeviceNotesRepository` — `create`, `update`, `delete`, `load`, `preview`; implemented in `data`.
- `DeviceNoteText` — `fits`, `rawFits`, `imported`; implemented in `format` and provided by `data`.
- `NoteFileReader` — `displayName`, `read`; implemented in `data`.
- `CheckDeviceNoteDraftUseCase`, `SaveDeviceNoteUseCase`, `DeleteDeviceNoteUseCase`, `LoadDeviceNoteUseCase`, `PreviewDeviceNoteUseCase`, `ImportNoteUseCase` (`text`, `file`).
### Guarantees
- The 200-character one-line rule, blank and size checks, the default group, trimming with case kept, the date shape, the origin rule and the accepted file names are covered by `DeviceNoteTest` (size rules there are a fake byte counter).
- The import order, the five rejection reasons, the 524,288/524,289 boundary and "no note exists before saving" are covered by `NoteImportTest` in `data`, which runs the use case against the real rules; the write path by `DeviceNotesTest` in `data`.
### Not covered
- Front-matter reading, the first-heading rule and the byte boundary themselves — `format` (through `DeviceNoteText`).
- Writing the note rows, the key and first-seen time, deleting bookmarks — `data` and `database`.
- Unsaved-changes asking, the editor screen, the share sheet entry and the import sheet — the editor feature module and `app`.
- Not touched by repository flows: sync, resume, change and disconnect never read or write device notes (013-local-notes: LN-12).
