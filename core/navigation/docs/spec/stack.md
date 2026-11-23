---
units: [navigation:navigator, navigation:keys, navigation:commands]
plans: [001-reader-foundation, 004-bookmarks, 005-stats, 013-local-notes]
decisions: [RD-D2, BM-D1, LN-D15]
---
## navigator — Navigator
### Behaviour
- The navigator is the only owner of the back stack. Neither Navigation 3 nor a ViewModel holds the stack; ViewModels read it and derive values from it (the open note, whether back can pop). It starts with the root key alone.
- The stack changes only through `dispatch(command)` and `dispatchImmediately(command)`; `push(key)` and `pop()` are thin wrappers of `dispatch`. A command is `Push(key)`, `Pop` or `ReplaceAll(keys)`. `dispatch` answers `Applied`, `Throttled`, `Ignored` or `Guarded`.
- `dispatch` goes in this order: the throttle gate, then computing the next stack, then the leave guard, then applying. The gate comes first so the stack is never changed and then reverted; a change in between would start a transition and leave screen and stack out of step.
- A move that arrives inside the window after the last applied move is dropped, never queued, so tapping fast cannot flip through screens. The window restarts from each applied move. (001-reader-foundation: RD-16)
- The default window is the larger of the scene transition length (340 ms) and the body fade-out plus fade-in (160 + 200 ms), that is 360 ms; it follows the motion numbers, not a copy of them. (001-reader-foundation: RD-16)
- Moves that change nothing are `Ignored` and do not open a window: pushing the key already on top, popping at the root, `ReplaceAll` with an empty list or with the current stack. Because of this two back presses at the root can exit the app; a first press that opened a window would make the second `Throttled`. (001-reader-foundation: RD-16)
- The stack is never empty: a root pop and an empty `ReplaceAll` are refused. The navigation display needs a non-empty stack.
- `Throttled` and `Ignored` are never merged. `Throttled` means "blocked for a moment"; `Ignored` means "nowhere further back", which the shell counts towards exit. Popping at the root during a window answers `Throttled`, so a back press during a transition does not count as an exit press. (001-reader-foundation: RD-16)
- `isSettled` is true at the start, becomes false when a move is applied and true again one window length after the last applied move (a newer move restarts the wait).
- `dispatchImmediately(command)` skips the gate and the guard and returns nothing; it is for moves that are not user input: popping a note that a sync deleted, replacing the stack with the home key when the connection changes, opening settings from a notification. (001-reader-foundation: RD-4)
- An immediate move still opens the window and restarts `isSettled`. If the window was already open it does not move the reference point, so shortly after a correction during a transition the gate may pass while `isSettled` is still false.
- One user action is not split into an immediate move and a `dispatch`: the first opens the window and the second is dropped. The next stack is computed once and sent as one command (see `commands`).
- A leave guard holds one slot: `setLeaveGuard` replaces the current guard and `clearLeaveGuard(guard)` clears it only when that guard is still the one set, so an old ViewModel torn down late does not clear a new guard.
- The guard is asked with the current and next stack, only for `dispatch`. When it blocks, the stack is unchanged, no window opens, and the answer is `Guarded`; the guard itself remembers the blocked stack and asks the person. `Guarded` is not an exit signal. The one user today is the editor's leave confirmation. (013-local-notes: LN-10)
- It is for one thread, the main thread: the throttle and the check-then-act in `dispatch` have no lock. Only the settle timer runs on the scope given at construction.
- The stack lives in memory for the whole process, so it survives rotation and ViewModel loss but not process death. The keys are serializable, but nothing saves or restores the stack.
- The composition root creates one navigator for the app, with an app-wide scope; this module does not use Hilt.
### Public API
- `Navigator(scope, root, minIntervalMillis, clock)` — the owner; `stack`, `isSettled`, `dispatch`, `dispatchImmediately`, `push`, `pop`, `setLeaveGuard`, `clearLeaveGuard`. Interval and clock are constructor parameters so tests can control time.
- `NavCommand` — `Push(key)`, `Pop`, `ReplaceAll(keys)`.
- `NavResult` — `Applied`, `Throttled`, `Ignored`, `Guarded`.
- `LeaveGuard` — one method `blocks(current, next)`; true blocks the move.
### Guarantees
- A move one millisecond before the window ends is blocked and one after it passes, pop during a window, no window for no-op moves, `Throttled` rather than `Ignored` for a pop in a window, immediate moves during a window, and the window restarting from the last move: `NavigatorTest`.
- A guard blocks user moves only, leaves the window closed, is skipped by immediate moves, and an old guard's clear does not remove a newer guard: `NavigatorTest` (`LN-10` test).
### Not covered
- The order of back handling (drawer, quiz, transition swallow, pop, root exit) and the exit counter — `app`.
- Computing which stack a user action leads to — see `commands`.
- What the guard asks and shows — `editor`.
- Drawing the stack — see `scenes`.

## keys — Destination keys
### Behaviour
- Every destination is a serializable key under the sealed `MyNoteNavKey`. It is sealed so other modules cannot add one and every `when` over keys is checked for completeness; a new feature's key is added here.
- `NotesNavKey` has nine keys: `Home`, `Bookmarks`, `Stats`, `StatsHistory`, `Reader(noteId)`, `Settings`, `Connect`, `Editor(noteId, draftId)` and `Paste`.
- `Editor` has three shapes with one type: a new note (both fields null), editing a stored note (`noteId`), and an editor prefilled from an import (`draftId`). The text of an import is never in the key; the editor's ViewModel holds it. There is at most one editor in a stack. (013-local-notes: LN-3, LN-8, LN-9)
- `Paste` is the paste screen of import; its next step replaces it with a prefilled editor. (013-local-notes: LN-6)
- The quiz is not a destination: it is an overlay and has no key.
- `activeNoteId()` of a stack answers only when the top is a `Reader`; with a reader below another screen it answers nothing. `isSettingsTop()` says whether the top is `Settings`.
- No feature holds a key of its own; no one serializes keys today (no saved back stack).
### Public API
- `MyNoteNavKey` — sealed interface of all keys.
- `NotesNavKey` — the nine keys above.
- `List<MyNoteNavKey>.activeNoteId()` and `.isSettingsTop()`.
### Guarantees
- The open note exists only when a reader is on top, and bookmarks, stats, history and connect tops are neither a note nor settings: `NotesNavKeyTest`.
### Not covered
- Which screen a key shows — the feature module that registers it (see `registry`).
- Stack rules (what can follow what) — see `navigator` and `commands`.

## commands — Stack commands
### Behaviour
- Each function takes the current stack and returns one `NavCommand`, so one user action is one `dispatch` and one throttle window. Two commands in a row would lose the second.
- `openReaderCommand(noteId)`: if the top is not `Settings` it pushes the reader. If settings is on top, it takes settings off and puts the reader in its place; when the entry below settings is already that reader, it only takes settings off. (001-reader-foundation: RD-11)
- `switchTabCommand(tab)` for the floating navigation (`NotesTab`: Home, Bookmarks, Stats): Home replaces the stack with the home key from anywhere (pressing it on home alone changes nothing). Bookmarks or Stats pushes the tab onto a stack that is exactly the home key; from anywhere else it replaces the stack with home plus the tab, so an open reader or the other tab is dropped. The same tab pressed on top gives the stack it already has and is ignored without opening a window. (001-reader-foundation: RD-15) (004-bookmarks: BM-7) (005-stats: ST-8)
- `openEditorCommand(editor)`: removes every editor already in the stack and, if the top then is `Paste`, the paste screen too; when nothing was removed it pushes. Otherwise it replaces the stack with the rest plus the new editor (home when the rest is empty). So a cold share starts from home plus the editor, a running app pushes over the current stack, a second share replaces the first editor, and the paste screen's next step replaces paste, so back returns to where paste was opened. (013-local-notes: LN-6, LN-8)
- `savedNoteCommand(noteId)`: removes the editors from the stack and, unless the entry now on top is already that note's reader, puts that reader on top. A new note ends on its reader in the editor's place; an edit goes back to the reader below. Back returns to the screen that opened the editor. (013-local-notes: LN-3, LN-9)
- `withoutNoteCommand(noteId)`: removes that note's readers and the editors editing it, merges neighbours that became the same key, and falls back to home when nothing is left. It always answers `ReplaceAll`; when nothing changed the navigator ignores it. Closing the quiz overlay is the caller's job through a signal in `ui`. (013-local-notes: LN-11)
- `isEditorTop()` says whether the top is an editor; `editorKey()` gives the one editor in the stack, if any.
### Public API
- `NotesTab` — `Home`, `Bookmarks`, `Stats`.
- `List<MyNoteNavKey>.openReaderCommand(noteId)`, `.switchTabCommand(tab)`, `.openEditorCommand(editor)`, `.savedNoteCommand(noteId)`, `.withoutNoteCommand(noteId)`, `.isEditorTop()`, `.editorKey()`.
### Guarantees
- Settings replaced by the reader, kept bookmarks below, same-note below, other-note below, not-on-top: `OpenReaderTest`.
- Each tab from home alone, from reader, from the other tab, ignoring a tab already on top, home from anywhere, and returning from history: `TabSwitchTest`.
- Share from cold and warm, share over share, paste replaced by editor, save, edit-save, delete and neighbour merging, editor-on-top test: `EditorCommandsTest`.
### Not covered
- Sending the command and the throttle — see `navigator`.
- Deleting the note itself and closing the quiz — `data`, `quiz` and `ui`.
- The wide-window layout of editor and paste — see `scenes`.
