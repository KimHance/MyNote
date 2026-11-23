---
units: [editor:session]
plans: [012-feature-modules, 013-local-notes]
decisions: []
---
## session — Editor session

### Behaviour
- The session is tied to the editor key on the navigator stack: a key that appears opens a session, a key that leaves the stack drops it with its text; at most one editor is on the stack, and opening another replaces it (013-local-notes: LN-8).
- The text lives in the Activity-owned view model and never in saved state, so it survives rotation and fold but not process death, and unsaved text is never written to disk (013-local-notes: LN-10).
- A new note starts empty; imported or shared text starts from the prefilled draft kept in a view-model map under the id the key carries, so the 512KB text never travels in the key (013-local-notes: LN-6).
- Opening an edit loads the note's current text first; while it loads the session ignores edits and cannot be saved; if the note is not a device note or was already deleted, the editor closes quietly with no message (013-local-notes: LN-9).
- Title and group keep one line: a line break becomes a space and the text is cut at 200 code points; the body is not cut (013-local-notes: LN-3).
- Every change re-runs the draft check; saving is possible when the title and body are not blank and the body is at most 524,280 bytes, so the stored original stays within 524,288 bytes with the 8-byte empty front matter (013-local-notes: LN-3).
- The reason shown for a disabled Save is "body too large" before "empty"; nothing is shown while loading or saving; the kilobytes in the too-large reason are counted from the body plus the 8-byte front matter, rounded up, and only when the body is over the limit, so a body just over shows 513 (013-local-notes: LN-3).
- Sizes are UTF-8 bytes, not characters; an unpaired surrogate counts as 3 bytes like the saving rule, and counting stops once the bound is passed (013-local-notes: LN-3, LN-7).
- A session is changed when the draft differs from the one it opened with; for imported text that is the prefilled draft; switching between write and preview is not a change (013-local-notes: LN-10).
- Preview parses the body once per switch to preview, by the same path as saving; editing the title or group in preview keeps the blocks, switching back to write drops them (013-local-notes: LN-3).
- Save writes through the save use case with the default group wording; on success the stack is changed at once so the note's reader takes the editor's place (an edit returns to the reader already below), and the saved text becomes the original so the guard no longer asks (013-local-notes: LN-3, LN-9).
- A failed save logs, keeps the text and shows the notice "노트를 저장하지 못했습니다. 다시 시도해 보세요."; saving is blocked while a save runs, so there is no second save (013-local-notes: LN-3).
- A close request on an unchanged editor leaves at once; on a changed one it opens the leave confirmation first (013-local-notes: LN-10).
- A leave guard on the navigator also asks when a user move would remove or change the changed session's editor from the next stack, such as a tab switch in a wide window; a move that keeps the same editor key, such as stacking a reader over it, is not asked (013-local-notes: LN-10).
- Answering "버리기" sends the move that was held (or the close) at once, past the throttle and the guard; "계속 쓰기" drops it and the held new draft with it (013-local-notes: LN-10).
- Discarding when the next editor has the same key restarts the session from its source (an edit is read again), and the stack is still sent so a reader stacked above comes off (013-local-notes: LN-10).
- Corrections made by the app itself (save, delete, connection change) are not asked and pass the guard.
- The Import request signal opens the import sheet; a request that existed before the view model was created is not replayed (013-local-notes: LN-6).
- Choosing a file reads it by the import rule; Reading shows the progress state; a ready draft opens a prefilled editor, a rejection shows its notice, "nothing chosen" does nothing; cancelling during the read discards the result (013-local-notes: LN-6, LN-7).
- Choosing paste closes the sheet and pushes the paste screen; the pasted text is judged on each change: over 524,288 bytes turns the too-large state on; "다음" with blank or too-large text does nothing and shows no notice (013-local-notes: LN-7).
- A ready import never creates a note: it only opens an editor, and the note exists after Save (013-local-notes: LN-6).
- Rejection notices are five distinct texts: empty, not UTF-8, too large, unreadable, unsupported type; a notice has no undo (013-local-notes: LN-7).
- Shared text arrives through the inbox, also when it was put there before the view model existed; a text share goes through the text import rule, a file share through the file rule, and a share the receiver refused is reported as an unsupported type without reading it (013-local-notes: LN-8).
- A ready share opens the prefilled editor at once on the current stack, replacing an open unchanged editor; over a changed editor it asks first, and a second share arriving during that question keeps only the last (013-local-notes: LN-8).
- A rejected share shows its notice and opens nothing (013-local-notes: LN-8).
- Delete asks once (the dialog state), then on yes removes the note and its bookmarks through the delete use case, takes the note's reader and editor off the stack in one move (merging equal neighbours, home when empty), closes the quiz overlay and shows "노트를 지웠습니다."; no undo (013-local-notes: LN-11).
- A failed delete logs and shows "노트를 지우지 못했습니다. 다시 시도해 보세요."; cancel changes nothing (013-local-notes: LN-11).
- Notices are shown once: the screen reports the id it showed and the view model reads it as seen (012-feature-modules: FM-16).
- The paste text and its too-large flag are cleared when the paste screen leaves the stack (013-local-notes: LN-6).
- An exception in an intent handler is logged and the state stays as it was; texts come from an injected messages interface (012-feature-modules: FM-8).

### Public API
- `EditorViewModel` — Hilt view model with `state` and `onIntent`, shared by the shell through the Activity owner.
- `EditorUiState` — session, leave confirmation, delete-confirm note id, import dialog state, file-pick request id, paste text and flag, notice; derived `guardsExit` and `pasteBlock`.
- `EditorSession` — one session: key, draft, original, check, loading, saving, previewing, preview blocks, body kilobytes; derived `editing`, `dirty`, `canSave`, `saveBlock`.
- `EditorIntent` — new note, edit, field changes, preview, save, close, leave and delete answers, file and paste choices, paste text, signal shown.
- `SaveBlock`, `PasteBlock`, `ImportDialogUi` — the reason kinds and the sheet state.
- `EditorMessages`, `AndroidEditorMessages` — the texts the view model uses (default group, rejection reasons, deleted, save or delete failed); `app` binds them.

### Guarantees
- Share-in on cold and running stacks, replacing, asking before replacing a changed editor, file shares, refused shares and empty shares are proven by `EditorViewModelTest` (013-local-notes: LN-8).
- Leave asking, the prefilled baseline, recreation, discard restarts and the guard on tab switches are proven by `EditorViewModelTest` (013-local-notes: LN-10).
- Save and edit stack results, delete clean-up with the quiz closed, import sheet flow, rejection and cancel are proven by `EditorViewModelTest` (013-local-notes: LN-3, LN-9, LN-11, LN-6, LN-7).
- The reason line numbers (bytes, round-up, 8-byte front matter, unpaired surrogates, early stop) are proven by `Utf8SizeTest` (013-local-notes: LN-3, LN-7).
- A changed editor turns back into a close request, not a pop: instrumented `BackOrderTest` in `app` (013-local-notes: LN-10).

### Not covered
- Blank, size and format rules of saving, importing and the original text of a device note — `domain` and `format`; rejection decisions — `data`.
- Storage of device notes and their bookmarks — `data` and `database`.
- Putting the shared text into the inbox, the share filter and the cold-start stack — `app:share-in`.
- Clipboard: the app never reads it; no export of device notes and no reminder to back up.
- The stack calculations for save, delete and open (`savedNoteCommand`, `withoutNoteCommand`, `openEditorCommand`) — `navigation`.
- Dialog and screen drawing — `host`, `editor-screen`, `paste-screen`.
