---
units: [app:shell-state, app:shell, app:shell-chrome]
plans: [001-reader-foundation, 003-model-download, 004-bookmarks, 005-stats, 007-repo-connect, 010-brand, 012-feature-modules, 013-local-notes]
decisions: [RD-D3, BM-D2, RC-D8, FM-D7, FM-D11, FM-D12, FM-D14, FM-D17, FM-D19, FM-D20, DEC-37a, D-24]
---
## shell-state — Shell state and start decision
### Behaviour
- The shell ViewModel builds one state: back stack copy, active note id (read from the top reader key), whether the last navigation move has settled, exit hint id, exit request id, connection (null until the first emission), seed-pending flag, text-size scale and model status.
- It holds no note list, quiz or token state; those belong to the feature ViewModels, so the start branch is decided in `shell` by combining shell state and notes state in the same frame (012-feature-modules: FM-7).
- Back pops the navigator. Only a pop that the navigator ignores (nothing to pop) counts toward exit: the first press raises an exit hint, a second press less than 1000 ms later raises an exit request, a press after the window raises the hint again (012-feature-modules: FM-9) (001-reader-foundation: RD-17).
- A pop the navigator drops because a move landed less than 360 ms earlier does nothing here and is not an exit press (001-reader-foundation: RD-16).
- A hint id that the screen has reported as shown reads 0 afterwards, so it is not shown again after recreation; the next press raises a new id (012-feature-modules: FM-9).
- Tab switch dispatches the tab-switch command computed from the current stack through the throttled path; open settings goes through the shared open-settings helper (`ui:signal`).
- Text-size scale and model status are observed lazily; a read failure falls back to the normal scale and to the missing model. Connection is observed eagerly; a read failure falls back to no connection.
- Each distinct connection value after the first is a connection change; the first value and a repeat of the same value clean nothing (007-repo-connect: RC-23).
- On a connection change the stack is replaced by Home alone at once, also when the open reader was a device note; the quiz overlay is dismissed unless old and new are both connections to the same source; last-sync is set to succeeded for a connection and to none otherwise (007-repo-connect: RC-23, RC-17) (012-feature-modules: FM-6).
- At creation the ViewModel consumes the debug seed once; the seed-pending flag turns false when the call ends, whether it succeeded or failed (007-repo-connect: RC-19).
- Start decision: reading while the connection is unknown or the notes are not loaded yet, and while there is no connection and the seed is still pending; every other case is the shell (007-repo-connect: RC-1) (010-brand: BR-K1).
- The notes count that feeds the decision includes repository and device notes; zero notes without a connection is the shell with the home first-run empty state, never a connect screen (013-local-notes: LN-1) (007-repo-connect: RC-1).
- A top screen that draws its own top bar (connect, editor, paste) is reported as owning the chrome; the shell then hides top bar, drawer gestures, floating nav and model pill (007-repo-connect: RC-14).
- The selected tab is the last Home, Bookmarks or Stats key on the stack, Home when none (004-bookmarks: BM-8) (012-feature-modules: FM-K3).
- The reader-end card sends one of four targets: open quiz setup with note id and category, ask to confirm the model download, open settings through the shell, or start the download through settings (012-feature-modules: FM-6).
- Scroll-to-top is allowed only when scrolled and no quiz is open, no chrome-owning screen is on top and no token is being edited.
- An intent handler that throws is logged through the failure log and the state stays as it was (001-reader-foundation: RD-21).
### Public API
- `ShellViewModel` — the shell state holder, taking `ShellIntent`.
- `ShellUiState` — the state described above, with derived flags for top screen kind, `canPop` and `selectedTab`.
- `ShellIntent` — `Back`, `SwitchTab`, `OpenSettings`, `SignalShown`.
- `startOf(shell, notes)` — the start decision from shell and notes state.
- `startDestination(connection, notes, seedPending)` — the pure decision; `StartDestination` (`Reading`, `Shell`), `ConnectionKnown` and `NotesKnown` are its input and output types.
- `screenCleanupOf(previous, next)` — what to clean on a connection change, or null; `ScreenCleanup` carries the two flags.
### Guarantees
- `ConnectStartTest` proves the start decision over the 18 cells of connection by repository notes by device notes, the seed-pending reading, and the four cleanup cases of a confirm (007-repo-connect: RC-23).
- `ShellViewModelTest` proves the double-back exit window and that a stacked connect screen pops while the first-screen Home gives the exit hint.
- `HomeBodyTest`, `FeatureSignalsTest`, `NotesViewModelTest`, `ConnectViewModelTest` and `ConnectIssueTest` live here because they need the shell and a feature together: signals between features (ten paths; the permission re-read is checked by hand), one sync for many subscribers, the hero repository line for confirmed connections only, and equal token-failure branches in settings and the connect screen. `FeatureFixture` builds the shell and feature ViewModels on one navigator and one set of signal holders.
### Not covered
- Note list, search, bookmarks and the reading record — `notes:state`; quiz state — `quiz:session`; token state — `settings:repo`.
- Stack rules and throttling — `navigation:navigator`; the tab-switch command — `navigation:commands`.
- Drawing the reading screen or the shell — `app:shell`.

## shell — Shell frame and back order
### Behaviour
- `NotesRoute` obtains the shell, notes, settings, quiz and editor ViewModels, all owned by the Activity, so they survive a window-class change that rebuilds `NavDisplay` (012-feature-modules: FM-7).
- The window class comes from the container width and height; the theme takes the text-size scale from shell state.
- On Android 13 and later without the notification permission, the settings intent that starts the model download first asks for the permission; whatever the answer, the download then starts. Every other intent passes straight through (003-model-download: MD-K13).
- The notifications-enabled flag is re-read on every resume and merged into the settings state; the settings card's turn-on action opens the system notification settings of this app.
- The Activity's start and stop are forwarded to the notes ViewModel as foreground changes.
- An exit request finishes the Activity; an exit hint dismisses the current snackbar, shows `한 번 더 누르면 종료합니다` for a short time and then reports it as shown.
- One snackbar host sits above the start branch and the window-class branch and receives notes errors and infos, the quiz quit undo, and the notices of notes, settings and editor; a notice with an undo shows the undo action; each reports back as shown once it ends, so folding or rotating does not show it again (012-feature-modules: FM-16).
- The quit undo shows a short snackbar; pressing its action sends the quiz undo-exit intent.
- The start branch shows the reading screen or the shell. The editor host (file picker launcher and dialogs) is composed above the branch, so a pick result always has a place to return to (013-local-notes: LN-6).
- The model-download confirm dialog is composed above the branch too; its open state survives rotation; confirming starts the download through the permission step above.
- A window-class change switches between the modal-drawer form and the fixed-panel form and rebuilds `NavDisplay`; the open note's reader scroll, source expansion and the token draft live above that branch and survive it (012-feature-modules: FM-4).
- Back is taken in this order, highest first: open drawer, quiz overlay, swallow during a transition, pop, opening drawer, expanded fixed panel, root exit hint (012-feature-modules: FM-10, FM-K2) (001-reader-foundation: RD-17) (013-local-notes: LN-K5).
- The order is the compose order inside `NotesShell.kt`; moving a handler to another file or to a multibinding set silently reorders it (012-feature-modules: FM-5).
- While a changed editor is on top, back asks the leave confirmation instead of popping; the guard sits after `NavDisplay` so it beats the pop without a predictive preview, and before the swallow so a transition in progress still swallows (013-local-notes: LN-10).
- The scaffold content sits in a child back dispatcher that is off while the drawer is open, so the drawer's own handler receives back; the flag is applied in the same frame, because a one-frame delay lets a pop win right after opening or leaves nobody after closing.
- The drawer counts as engaged while open or opening; an opening drawer is closed by the shell, otherwise back reaches the system and closes the app.
- An expanded fixed panel is folded by back before the root exit hint; root exit is enabled only with nothing to pop, no engaged drawer and no expanded panel.
- In the modal-drawer form the list panel opens from the header menu button and closes when a note is chosen or settings is opened; in the fixed-panel form it starts expanded, the menu button folds it, and the fold state is saved.
- Closing the drawer or folding the panel clears focus and hides the keyboard; the top of the stack changing resets the floating-nav scroll state.
- In the drawer form the header is the scaffold top bar, hidden while the quiz covers the reader and on chrome-owning screens; in the fixed-panel form the list-detail scene draws it. Drawer gestures are off on chrome-owning screens.
- Header title is the settings, bookmarks (`북마크`), statistics (`통계`) or past summaries (`지난 요약`) title, the open note's title, or the app title; back shows on settings and past summaries; refresh shows everywhere except past summaries and device-note readers; a device-note reader shows edit and delete there instead; the bookmark toggle shows while a note is active (013-local-notes: LN-9).
- Scene strategies are empty in the drawer form and one list-detail strategy (`notes-list-detail`) otherwise. Scene slots keep their identity and read live values, because the Activity is not recreated on rotation and a stale capture is a real bug.
- Only `NavDisplay` sits inside the backdrop box; floating nav, pill, scroll-to-top and the quiz overlay are its siblings, otherwise the glass samples itself (012-feature-modules: FM-K8).
- The quiz overlay is composed last, so it takes back before the swallow and the pop; with the fixed panel open it starts after the panel width plus 1 dp.
- Five registrations cover the nine destination keys: notes (Home, Bookmarks, Reader), stats (Stats, StatsHistory), settings (Settings), connect (Connect), editor (Editor, Paste); registering a key twice stops at build time of the bundle (012-feature-modules: FM-4).
- A registered screen gets only frame values from the shell: insets, two-pane flag, stacked-scene flag, scroll holders, token draft, snackbar host. Its own state and entry points, and screen-only values (has connection, shell back, notification settings), are separate parameters of its registration function (012-feature-modules: FM-4).
- A registered screen reads only its own feature's state, so another feature's change does not redraw it; the entry provider is built once for a frame and destination set, and no per-destination ViewModel decorator is used.
- The notes registration exposes a reader-end slot with the note id; the shell fills it with the quiz card (`quiz:cta`) showing the quiz model status (012-feature-modules: FM-6).
- Two-pane content needs window width minus the expanded fixed panel, minus two page paddings, minus the rail reserve to be at least 640 dp; the panel counts only in the fixed-panel form (004-bookmarks: BM-16).
- Content reserves 92 dp plus the navigation inset at the bottom for the floating bar, or only the navigation inset with a rail; in rail form the end padding reserves the rail's right margin, end inset, width and a 12 dp gap, less the page padding (004-bookmarks: BM-17).
- Screen transitions (`navigation:scenes`) and predictive back come from `:core:navigation`; the shell wraps `NavDisplay` in a shared-transition layout and provides its scope (`navigation:shared-elements`), and passes the transitions to `NavDisplay` unchanged (001-reader-foundation: RD-18).
### Public API
- `NotesRoute()` — the Compose root that `MainActivity` calls; everything else in the shell is internal.
### Guarantees
- `BackOrderTest` (instrumented) drives a stateless shell harness: one test per back step, the overlap rule (only the higher step acts), token-edit back, the four editor-guard cases and the window-class change keeping reader scroll, source expansion and token input (012-feature-modules: FM-10, FM-4) (013-local-notes: LN-10).
- `ShellDestinationsTest` proves every one of the nine keys is registered by exactly one registration and none twice.
- `HeaderTest` proves repository-note readers have no edit and delete, device-note readers do, and connect, editor and paste hide the shell chrome.
- `NotesLayoutTest` proves two-pane at 1280 dp with the panel open, one pane with the panel open and two panes when it is folded at 852 dp and 891 by 411 dp, and one pane on a 443 dp Compact portrait phone with the panel open or folded.
### Not covered
- Feature screens and their ViewModels — the six feature modules.
- Token-edit back cancels the edit inside settings, and a dialog receives back in its own window — `settings:repo` and the platform.
- Stack mechanics — `navigation:navigator`; scenes and transitions — `navigation:scenes`; glass components and floating bar drawing — `design-system:glass-nav`.
- Model pill, floating nav, scroll-to-top and reader effects — `app:shell-chrome`.

## shell-chrome — Floating chrome and reader effects
### Behaviour
- The floating navigation has three tabs, Home (`홈`), Bookmarks (`북마크`), Stats (`통계`); the selected tab comes from the stack and pressing one sends the tab-switch intent (001-reader-foundation: RD-15).
- It is a bottom bar on tall narrow windows and a right rail where the adaptive tokens ask for a rail (001-reader-foundation: RD-15).
- The navigation is hidden while scrolled down (its scroll state), while a quiz is open, while settings is on top and while a chrome-owning screen is on top.
- Scroll-to-top follows the rule of `shell-state`; pressing it shows the navigation again and scrolls the active scroller to the top (001-reader-foundation: RD-14).
- The model pill is visible while settings and chrome-owning screens are not on top and the status is busy, or the status became ready within the last 3.2 s after being busy, or the status is failed and that failure has not been seen (003-model-download: MD-K12).
- A failure counts as seen once settings, or a screen that owns the chrome (connect, editor, paste), is on top while the status is failed; the flag resets when the status leaves failed.
- A ready pill appears only on a transition from a busy state to ready; the first observed status, even ready at app start or after recreation, shows nothing (003-model-download: MD-K12).
- The pill is hidden while the navigation is scrolled away or a quiz is open, fades in and out, and pressing it opens settings.
- While the pill is visible the content top padding grows by 54 dp over 300 ms; the pill state is computed once and handed to both the overlay and the padding.
- While a note is active, the share of it seen is sent to notes as reader progress: scroll plus viewport over total, rounded down to a whole percent, 100 when the content fits, nothing before the reader is measured; only changed values are sent (005-stats: ST-2).
- A reader scroll request from the notes state waits up to 2 s for the requested note's reader to be measured, then scrolls to the requested fraction; on timeout it does nothing.
### Public API
- None — all declarations are internal to `:app`.
### Guarantees
- `ModelPillTest` proves a busy-to-ready transition raises the ready pill and a first-seen ready or non-ready status does not.
- `ReaderScrollEffectsTest` proves the percentage: none before measurement, 100 when it fits, one viewport at the top, 100 at the end and rounded down in between.
### Not covered
- The glass bar, rail, pill and button drawing — `design-system:glass-nav`; which status text the pill shows — `ui:model-status`.
- The tab-switch rules — `navigation:commands`.
- What happens to the progress after it is sent — `notes:state` (feeds the tracker), `domain:study-log` (keeps the largest share) and `database:study-log` (stores it).
