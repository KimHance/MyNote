# Decisions

### RD-D3 · 2026-09 · RD-17
- decision: Back is handled in compose order (the later a handler is composed the earlier it receives back): drawer, quiz overlay, swallow during a transition (placed after the `NavDisplay`), token edit, stack pop, the close of a drawer that is still opening, fixed pane fold, then the root exit hint; the drawer counts as engaged by its target value, and the two-press exit is counted in the view model and sent to the screen as a one-shot signal.
- why: The display commits a predictive-back animation before calling `onBack`, so swallowing inside `onBack` leaves screen and stack apart. A drawer that is still opening reports "not open", which let back reach the exit hint and close the app. A press counter held in state is conflated by the state flow and the second press was lost.
- rejected: Swallowing a back during a transition inside `onBack`; deciding the drawer by `isOpen`; counting presses as state; moving handlers into other files or multibindings (the order would no longer be visible in one place).
- evidence: `app` `shell/NotesShell.kt`, `ShellViewModel`, `BackOrderTest` (`FM-10 …`), `ShellViewModelTest` (`FM-9 루트에서 창 안에 두 번 누르면 종료한다`).

### BM-D2 · 2026-10 · app:shell-state, BM-8
- decision: The floating nav selection is derived from the back stack (nearest Home, Bookmarks or Stats under any reader or settings); no ViewModel stores a selected tab.
- why: State held in two places drifts apart; the earlier local remembered selection could disagree with the screen.
- rejected: A stored `selectedTab` in a ViewModel or a saved local selection in the shell.
- evidence: `app/.../shell/ShellUiState.kt` (`selectedTab`).

### RC-D8 · 2026-10 · RC-19
- decision: The build-time repository (`github.*` in local properties) survives only as a debug-build development seed used for the first connection when there is no connection and no earlier disconnect has blocked the seed; no default repository is baked into code; once the user disconnected, the seed is not used again.
- why: Developers do not paste a link every time, public builds must not carry the author's repository, and a restart that revives a disconnected connection means disconnecting did not work.
- rejected: A default repository constant in code, and reusing the seed after a disconnect.
- evidence: `ConnectionUseCaseTest` (`RC-19 끊은 뒤에는 시드를 다시 쓰지 않는다`), `app/build.gradle.kts` (seed only in debug).

### FM-D7 · 2026-10 · FM-7
- decision: Every feature ViewModel is obtained with the Activity as explicit owner; no per-destination ViewModel decorator is used.
- why: A window-class change rebuilds the whole `NavDisplay`; with destination scope the ViewModel would be created anew then, and the shell and the destination would get different instances of the same ViewModel.
- rejected: Per-destination scoped ViewModels through a destination decorator.
- evidence: `app/src/main/java/com/hancekim/mynote/shell/NotesRoute.kt` (`hiltViewModel(viewModelStoreOwner = LocalActivity…)`), `notes-tests/test_feature_modules.py` (`FM-7 기능 ViewModel 은 Activity 를 owner 로 받는다`).

### FM-D11 · 2026-10 · FM-10
- decision: The back-order instrumented test draws a stateless shell with fake state; the two-press time window of step seven is checked by the FM-9 JVM test; no Hilt instrumentation dependency is added.
- why: Library instrumented tests have no app Hilt graph, and it adds no new dependency.
- rejected: A Hilt-based instrumented base that draws the real shell with the real graph.
- evidence: `app/src/androidTest/java/com/hancekim/mynote/shell/BackOrderTest.kt`, `app/src/test/java/com/hancekim/mynote/shell/ShellViewModelTest.kt`.

### FM-D12 · 2026-10 · FM-5
- decision: The registration bundle is used only for the screens of the nine destination keys; the quiz overlay, list panel, top-bar actions, model confirm dialog and back handlers are called directly in one fixed order from one `:app` shell file.
- why: Back priority is composition order; receiving them as an unordered set could compose the quiz overlay (step two) before the swallow (step three).
- rejected: Collecting overlays, panels and handlers as an unordered set (multibinding).
- evidence: `app/src/main/java/com/hancekim/mynote/shell/NotesShell.kt`, `NotesOverlays.kt`, `notes-tests/test_feature_modules.py` (`FM-5 …`).

### FM-D14 · 2026-10 · FM-16
- decision: The snackbar is received by one host at the `:app` root (above the start branch and the window-class branch); each ViewModel holds its own seen signal.
- why: Received inside a feature screen, a failure raised while another screen is on top shows late, and it disappears and reappears during fold and unfold.
- rejected: Receiving the snackbar inside each feature screen.
- evidence: `app/src/main/java/com/hancekim/mynote/shell/NotesRoute.kt`, `core/ui/src/test/java/com/hancekim/mynote/core/ui/element/NotesSignalTest.kt`.

### FM-D17 · 2026-10 · FM-2, FM-5
- decision: The work order places the shell move into `:app` before the feature extractions (stats, connect, settings, quiz).
- why: The fixed-order calls (connect root, reader screen, model confirm dialog, quiz overlay) are direct calls, not registrations; with the shell still in `:feature:notes`, extracting a feature would make notes depend on it (an FM-2 violation); with the shell in `:app` first only imports of `:app` change.
- rejected: Extracting features before moving the shell.
- evidence: `app/src/main/java/com/hancekim/mynote/shell/`, `notes-tests/test_feature_modules.py` (`FM-2 기능 모듈은 서로를 의존하지 않는다`).

### FM-D19 · 2026-10 · FM-10
- decision: With the drawer open, the M3 drawer receives back so its predictive-back stretch stays alive: the shell content is wrapped in a child back dispatcher that is disabled while the modal drawer is open.
- why: A handler at the very end of the content keeps FM-10 step one but removes the M3 animation; the child dispatcher keeps both (official documentation basis, nothing traded).
- rejected: Placing the app's back handler at the very end of the content.
- evidence: `app/src/main/java/com/hancekim/mynote/shell/NotesShell.kt` (`rememberNavigationEventDispatcherOwner`), `BackOrderTest`.

### FM-D20 · 2026-10 · FM-10
- decision: Switching the child dispatcher on and off of FM-D19 is applied in the composition of that same frame, not deferred to an effect.
- why: Deferring leaves a gap within the drawer settle frame: a pop can win right after opening, or no handler receives right after closing so the app can exit.
- rejected: Applying the switch from an effect.
- evidence: `app/src/main/java/com/hancekim/mynote/shell/NotesShell.kt` (the `SideEffect` next to `contentDispatcher`), `BackOrderTest`.

### DEC-37a · 2026-10 · FM-10
- decision: The back-order test runs a stateless shell with fake feature state to check the seven steps in order, as an instrumented test in app. No Hilt instrumentation dependency is added.
- why: The order lives in one shell file and can be proven without the real graph.
- rejected: A Hilt-based instrumented test of the real graph.
- evidence: app androidTest BackOrderTest; shell/NotesShell.kt fixes the order.

### D-24 · 2026-10 · app:shell, BM-16
- decision: The shell decides two columns from window width minus open fixed panel, page margins and rail reserve being at least 640dp and passes a flag to the screen.
- why: The shell already knows the window, panel and rail; screens do not re-measure.
- rejected: Measuring inside the screen with a constraints box, or choosing by width class.
- evidence: `app/.../shell/NotesLayout.kt` (`contentTwoPane`), `NotesLayoutTest`.
