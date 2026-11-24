---
units: [editor:editor-screen, editor:paste-screen, editor:host]
plans: [013-local-notes]
decisions: [LN-D6, LN-D7, DEC-38a, DEC-38b]
---
## editor-screen — Editor screen

### Behaviour
- The editor is a stack destination with its own top bar: a close button, the title "새 노트" (new note or imported text) or "노트 고치기" (edit), and the primary "저장" button; the shell hides its own top bar, floating navigation and model pill (`app:shell-state`) (013-local-notes: LN-3, LN-9).
- Below the bar is one scroll: the title field, the group row, for an edit a meta line, the "쓰기" and "미리보기" switch, the reason line, then the body field or the preview (013-local-notes: LN-3).
- The title field is one line with the hint "제목"; the group row has the label "그룹" and a one-line field with the hint "그룹 없음"; an empty group is saved as that default group (013-local-notes: LN-4).
- The body field is plain multi-line text with the hint "여기에 마크다운으로 쓰세요."; it has a minimum height of 240 dp so a press below the text still reaches it (013-local-notes: LN-3).
- There are no formatting buttons, toolbar or autocomplete, and no autosave.
- "저장" is enabled only when the title and the body are not blank and the body is within the size limit, and not while the note is loading or saving; a disabled button is shown dimmed and the reason is the reason line (013-local-notes: LN-3).
- The reason line, under the switch, reads "제목과 본문을 쓰면 저장할 수 있습니다." in the quiet colour when a field is blank; when the body is too large it shows a warning icon and "본문이 nKB로 512KB를 넘습니다. 줄이면 저장할 수 있습니다." in the warning colour, with n from the state; it is absent when saving is possible (013-local-notes: LN-3).
- An edit shows the meta line "이 기기 · YYYY.MM.DD에 만듦" with the device glyph, from the creation date; there is no delete button in the editor; the date never changes on edit (013-local-notes: LN-4, LN-9).
- While an edit's current text is loading the three fields are disabled; the screen shows only the top bar for a key that has no matching session, such as the outgoing editor during a transition (013-local-notes: LN-9).
- A new note or imported text puts the cursor in the title field once per editor key, also after a recreation; an edit does not open the keyboard (013-local-notes: LN-3).
- Pressing "미리보기" hides the keyboard, clears focus and asks for one parse of the body; going back to "쓰기" drops the parsed blocks (013-local-notes: LN-3).
- While the parse is pending the preview shows a progress bar; the result is drawn by the reader's own block renderer (`design-system:blocks`), over-limit box included, under a reader-like header: chip with the group (default group when empty), the date (creation date, else today), the device mark and the trimmed title (013-local-notes: LN-3).
- The close button and the system back both send the same close request, so a changed editor asks before leaving; the screen has no back handler of its own (013-local-notes: LN-10).
- The root takes the safe-drawing insets including the keyboard; only the top bar is fixed and the rest scrolls, capped at the reading width from the Expanded class up.
- In a wide window the editor is the detail pane and the list panel stays usable; choosing another note stacks its reader over the editor and back returns to the editor with the input intact.

### Public API
- `editorDestinations(editor, onIntent, onShellBack)` — registers the editor and the paste destinations for the shell.

### Not covered
- Check rules, limits and saving — `session`.
- Leave and delete confirmations — `host`.
- The Markdown to blocks conversion — `format:markdown` and `ui:mapper`; the block drawing — `design-system:blocks`.
- No UI test in this module draws the editor; the back behaviour of a changed editor is proven in `app:shell` by `BackOrderTest` (013-local-notes: LN-10).

## paste-screen — Paste screen

### Behaviour
- Paste is a stack destination with its own top bar: a back arrow (the shell's back), the title "붙여넣기" and the primary "다음" (013-local-notes: LN-6).
- Below it are the label "가져올 글" and one large monospace field with the hint "여기를 길게 눌러 붙여 넣으세요."; the field fills the remaining height and scrolls inside (013-local-notes: LN-6).
- The cursor goes into the big field once, also after a recreation, so the keyboard opens at once (013-local-notes: LN-6).
- The app never reads the clipboard: there is no paste button or clipboard preview, the person long-presses the field and pastes (013-local-notes: LN-6).
- "다음" is disabled while the text is empty or only whitespace and while it is over 524,288 bytes; the line under the field says "글을 붙여 넣으면 다음으로 갈 수 있습니다." in the first case and shows a warning icon with "512KB가 넘는 글은 가져올 수 없습니다." in the second (013-local-notes: LN-7).
- Pressing "다음" replaces this screen with a prefilled editor, so back from that editor returns to whatever opened the import sheet (013-local-notes: LN-6).
- The pasted text is held by the session and is cleared when the screen leaves the stack, whichever way it leaves (013-local-notes: LN-6).
- The root takes the safe-drawing insets including the keyboard and the column is capped at the reading width from the Expanded class up.

### Public API
- None — `PasteScreen` is internal; it is registered by `editorDestinations` in `editor-screen`.

### Not covered
- Judging the pasted text and building the draft — `session` and `domain:device-notes`.
- Opening the paste screen — `host`.

## host — Import sheet and confirmations

### Behaviour
- The host is composed by the shell (`app:shell`) all the time, above the start branch and the window-class branch, and draws nothing unless a dialog is open; it also owns the system file picker launcher, because a launcher inside a dialog would vanish before its result returns.
- The import sheet is a dialog titled "무엇을 가져올까요?" with the line "가져온 글은 새 노트로 열립니다. 저장해야 노트가 됩니다.", two rows, a share hint and "취소" (013-local-notes: LN-6).
- The rows are "파일 열기" (hint ".md나 .markdown 파일") and "붙여넣기" (hint "다른 앱에서 복사한 글"); the share hint says the app can also be chosen in another app's share menu (013-local-notes: LN-6, LN-8).
- "파일 열기" opens the system document picker for any type; what is accepted is decided afterwards by the file's display name (013-local-notes: LN-6, LN-7).
- Leaving the picker without a file changes nothing: the sheet stays open and no notice shows (013-local-notes: LN-7).
- While a chosen file is read the rows and the hint are replaced by "파일을 읽는 중입니다" and a progress bar, and the only button is "취소"; cancelling stops the read and drops its result, with no editor and no rejection notice (013-local-notes: LN-6).
- "붙여넣기" closes the sheet and pushes the paste screen (013-local-notes: LN-6).
- The leave confirmation reads "저장하지 않고 나갈까요?" with "쓴 내용은 남지 않습니다."; "계속 쓰기" is the quiet button and "버리기" the destructive one; dismissing the dialog means keep writing (013-local-notes: LN-10).
- The delete confirmation reads "이 노트를 지울까요?" with "노트와 북마크를 지웁니다. 학습 기록은 남습니다. 되돌릴 수 없습니다."; "취소" and "지우기"; dismissing means cancel; there is no undo (013-local-notes: LN-11).
- Dialogs are windows of their own on a glass panel with 24 dp padding (20 dp vertical at Compact height); content and buttons scroll together when they do not fit; they do not take part in the shell's back order (`app:shell`) (013-local-notes: LN-K5).

### Public API
- `EditorHost(state, onIntent)` — the always-composed host; the shell passes the editor state and intent handler.

### Not covered
- Deciding when each dialog opens, and what an answer does — `session`.
- Judging a picked file — `domain:device-notes` and `data:device-notes`.
- Who asks for the delete confirmation (the reader's top bar) — `ui:shell` and `app:shell`.
- The glass panel itself — `ui:parts`.
