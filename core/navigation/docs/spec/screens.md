---
units: [navigation:scenes, navigation:registry, navigation:shared-elements]
plans: [001-reader-foundation, 007-repo-connect, 012-feature-modules]
decisions: [RD-D4, RC-D33]
---
## scenes — Scenes and transitions
### Behaviour
- There is one navigation display. What changes with the form factor is only the list of scene strategies: with the list-detail strategy the screen has a fixed list panel beside one detail pane; with none, Navigation 3's single-pane strategy stacks screens like a stack, as at Compact width. (001-reader-foundation: RD-7)
- The list-detail scene shows only the top entry in its detail pane; the entries below it are its previous entries. When there are none (home alone) the display does not take the back press, so it reaches the app's exit handler.
- The scene key is the one given to the strategy, never derived from the detail entry. Choosing another note therefore does not slide the whole screen: only the body changes inside the scene, fading through — the old body fades and moves out over 160 ms, then the new one fades and moves in over 200 ms, 10 dp vertically. (001-reader-foundation: RD-7)
- The panel opens and closes by expanding and shrinking horizontally from the start with a fade. The scene draws no widths, colours or dividers; the panel, header and expanded state come from the caller as slots.
- The caller keeps the panel and header slot lambdas stable across compositions; a new lambda each time looks like a different strategy and rebuilds the whole scene. The expanded state is read through a function and is not part of that stability, so folding and unfolding the panel does not recreate the strategy.
- When the top entry is the connect screen the strategy returns no scene, so the single-pane layout is used: the connect screen is full width without list panel or header even on a wide window, from the root or on top of other screens. The test compares the entry's content key with the default content key of a connect entry, so giving an entry its own content key silently breaks it. (007-repo-connect: RC-14)
- The editor and the paste screen are not special-cased: on a wide window they sit in the detail pane with the list panel kept, and the shell empties the header slot.
- A push slides the new scene in from the right (1.05 of its width) while it scales from 0.90 to 1, and the old scene scales to 0.93 and fades to half; it takes 340 ms with an ease-out-style curve. A pop is the reverse: the old scene scales back from 0.93 and fades in, the front scene slides out and scales to 0.90. (001-reader-foundation: RD-18)
- Predictive back drives the same pop transition by seeking it; there is no separate predictive-back handler. The predictive spec is the pop spec and ignores the swipe edge; it ends fully off screen at progress 1 because a committed gesture continues the same spec to the end, and a spec that stops short would leave the previous screen showing.
- Back is delivered after the animation is committed. A handler that swallows back during a transition has to be composed after the display (the later one wins) so it still stops a pop whose animation already ran; that wiring is in the app shell.
- The transition numbers are internal to this module so features do not read them and the navigator's throttle window comes from the same source.
- `sceneEdge` draws what an enter transition cannot: the front screen's corner radius (30 dp) and shadow (26 dp) growing with a progress between 0 and 1. The progress is read from the current animated scope; with no scope (a preview) it is a constant 0 and nothing breaks. (001-reader-foundation: RD-18)
- `sceneEdge` is only for Compact width, where screens stack. On wide windows the animated scope in the list-detail scene is the body's, so the progress would mean "body replaced" instead of "scene entered".
### Public API
- `ListDetailSceneStrategy(sceneKey, panel, header, panelExpanded)` — the two-pane strategy.
- `sceneTransitionSpec()`, `scenePopTransitionSpec()`, `scenePredictivePopTransitionSpec()` — the push, pop and predictive-pop transitions for the display.
- `rememberSceneEdgeProgress()` and `Modifier.sceneEdge(progress)` — the corner and shadow effect.
### Guarantees
- Scene key stays the same when the detail changes, one entry in the scene, previous entries follow the stack, a back target exists only when there is something below: `ListDetailSceneStrategyTest`.
- Connect is single pane on a wide window at the root, on top, and not when it is not on top; editor and paste keep the detail pane: `ListDetailSceneStrategyTest` (`RC-14` and `LN-3` tests).
### Not covered
- Transition numbers and the corner effect have no test of their own.
- The panel's width, colours and dividers, the back order and the display itself — `app`; panel content — `notes`.
- Registering screens — see `registry`; the shared-element scope inside the scene — see `shared-elements`.

## registry — Destination registry
### Behaviour
- A feature exposes its screens as a `DestinationRegistration`: a function that receives the shell's frame value and registers screens. The frame type is a generic parameter, so this module knows nothing of its shape and no feature or design-system type enters here.
- `destinationProvider(frame, registrations, fallback)` builds the Navigation 3 entry provider from all registrations once.
- Registration opens only `destination<Key> { screen }`, not Navigation 3's own scope. Every screen is wrapped in the shared-transition scope, so a feature cannot forget it and turn off the home-to-reader transition; and a feature cannot choose a content key or metadata, because the wide-window rule recognises the connect screen by its default content key. (012-feature-modules: FM-4) (007-repo-connect: RC-14)
- The wrapping is done at registration, once. Wrapping again each time an entry is built would create a new lambda each time and break entry equality.
- Registrations run once when the provider is built, so the frame value must be read inside the screen; read during registration it would be fixed to the value of that moment.
- Registering the same destination twice stops the build of the provider with an illegal-argument failure; a destination nobody registered goes to `fallback`, whose default throws an illegal-state error naming the key. Each key of the notes set is therefore registered by exactly one feature. (012-feature-modules: FM-4)
- Registrations are a plain list, not a Hilt multibinding set: a registration receives composition values from the shell, and the back-order instrumented test draws the shell without Hilt. Calls whose order matters (overlays, panels, back handlers) are direct calls in the shell, not registrations.
- No view-model decorator is passed to the display. With one, a window class change would create the destination's ViewModel again.
### Public API
- `DestinationRegistration<F>` — `DestinationScope.(frame: F) -> Unit`.
- `DestinationScope` — `destination(clazz, content)` and `destination<K>(content)`.
- `destinationProvider(frame, registrations, fallback)` — the entry provider for the display.
### Guarantees
- A registered connect screen keeps the default content key and stays single pane on a wide window; a double registration stops the build; an unregistered key reaches the fallback; the frame value is passed to registrations: `DestinationsTest`.
### Not covered
- What a frame contains, and drawing the shell around destinations — `ui` (frame contract) and `app`.
- That every key has a screen: the shell test `ShellDestinationsTest` in `app`.
- Shared-element behaviour itself — see `shared-elements`.

## shared-elements — Shared elements
### Behaviour
- Navigation 3's animated-content scope throws when it is read outside the display, as in a preview. So screens never read it: the module keeps its own scope that may be absent, and that local is internal so a screen cannot read it by mistake. This is enforced by visibility, not by discipline.
- `ProvideNavAnimatedScope` provides the scope: one already provided inside wins, otherwise Navigation 3's. The two-pane scene provides its body scope, so on a wide window the body cross-fade is the base of a shared element and at Compact width the display's scene transition is.
- It reads Navigation 3's scope, so it is called only inside an entry; the registry already wraps every destination. In a preview it throws.
- `navSharedElement(key)` and `navSharedBounds(key)` are modifiers; without the shared-transition scope or the animated scope they return the modifier unchanged, so previews and screens outside the display still draw.
- `LocalSharedTransitionScope` may be absent; the app shell sets it by wrapping the display in a shared-transition layout.
- The meaning of the key strings (chip, date and title of a note) is the feature's; this module only wires. The chip, date and title of a card travel into the reader header and back. (001-reader-foundation: RD-13)
### Public API
- `LocalSharedTransitionScope` — the shared-transition scope, or none.
- `ProvideNavAnimatedScope { }` — provides the animated scope to its content.
- `Modifier.navSharedElement(key)` and `Modifier.navSharedBounds(key)`.
### Not covered
- Key naming and which elements are shared — `notes`.
- Wrapping the display in a shared-transition layout — `app`.
- There is no unit test of this unit; it needs a Compose host.
