---
units: [common:mvi, common:throttle, common:result]
plans: [001-reader-foundation]
decisions: [RD-D1]
---
## mvi — MVI base
### Behaviour
- A screen state is rebuilt whole. A ViewModel registers the values its `build()` reads; when any registered value changes, `build()` runs again and the whole state is replaced. No code edits part of a state. (001-reader-foundation: RD-21)
- The value that flowed through a registered source is discarded and only used as a signal; `build()` reads each value's current content, so a state is never assembled from a half-old mix and the number of registered values does not change any type.
- `trigger(initial)` registers a writable value that intents set (a query, a selected tab); `Flow.triggerIn(initial, started)` registers a read-only value that comes from outside or is derived. Anything else (a job, a counter, a source flow's input) stays a plain property and never rebuilds.
- A value that `build()` reads but that is not registered does not rebuild the screen. There is no error or log; the screen just stays as it was.
- With no registered value at all, the state is fixed at its first build forever.
- Setting a trigger to a value equal to the current one does not rebuild. A rebuilt state equal to the current one is not emitted again, so the state type needs value equality; a new object with identity equality inside it defeats this.
- Registration works only while the ViewModel is being constructed (property initialisers or `init`). A later registration is ignored without a message, because the list is read once when `state` is first read. (001-reader-foundation: RD-21)
- A `triggerIn` value starts sharing at the line where it is declared and reads its source there, so the values it depends on must be declared above it; declared below, construction fails.
- The first `build()` runs when `state` is first read, not when the ViewModel is created. If a value changes between those two moments, the initial state holds the later value. Concurrent first readers get the same single pipeline.
- `state` is shared while subscribed and stays alive for 5 seconds after the last subscriber leaves, so rotation does not restart it. Re-subscribing inside the 5 seconds does not rebuild; after it, the next subscription rebuilds once.
- Sources registered with `triggerIn` stop with the state at the same 5-second mark, however many derived steps sit between; the waits do not add up. With `SharingStarted.Lazily` a source keeps running from its first subscription until the ViewModel is cleared, which suits Room and DataStore observers and not polling or sensors.
- On subscription each registered value emits its current value at once, so `build()` runs about once per registered value; because the results are equal, subscribers see one state. Putting sorting, filtering or mapping inside `build()` repeats it each time; do it in a `triggerIn` so it runs only when its input changes.
- One rebuild happens per changed value, and an intermediate state is shown on screen when a handler changes several values in turn. Values that must change together belong in one trigger of one type (a single load-state type, not a loading flag plus an error).
- An input and its result are deliberately separate registered values (a search text and its hits): the text shows at once and the result when it arrives; two emissions are expected.
- `build()` never overlaps itself: there is a single collector, even when the main dispatcher runs collectors inline.
- Numeric or toggled trigger values are changed with `update { }`; a read-then-write assignment loses updates under concurrent writes.
- `onIntent` runs the subclass's `handleIntent` inside a cancellation-safe boundary. An exception (but not a cancellation) goes to `onFailure(cause)` and never reaches the caller; the next intent still works. `onFailure` does nothing by default; a subclass may log or show it. (001-reader-foundation: RD-21)
- `launchSafely` launches in the ViewModel's scope with the same boundary: a failure goes to `onFailure`, the scope survives, and a cancellation is not reported as a failure.
- Handlers read trigger values, not `state.value`: before anyone subscribes, `state.value` is only the initial state.
- There is no channel for one-shot effects (snackbars, navigation). Screens use an id counter in the state that a `LaunchedEffect` watches, and the screen sends a "shown" intent afterwards so the id is cleared and a recreated activity does not fire it again.
### Public API
- `MviState` — marker interface for a screen state.
- `MviEvent` — marker interface for a screen intent.
- `MviViewModel<S, E>` — abstract base. Public: `state` (the shared state flow) and `onIntent(event)`. For subclasses: `build()`, `handleIntent(event)`, `onFailure(cause)`, `launchSafely { }`, `trigger(initial)`, `Flow.triggerIn(initial, started)`.
### Guarantees
- Registration, equality, rebuild, lifetime (5 seconds, no stacking, `Lazily`), failure boundary and stream behaviour: `MviViewModelTest`.
- A single pipeline under 48 concurrent first readers, no lost updates under 50,000 concurrent updates, and no overlapping `build()`, both with a plain thread dispatcher and with a single-thread dispatcher that runs inline like the Android main looper: `MviViewModelConcurrencyTest` (it holds the second class, `MviViewModelMainImmediateConcurrencyTest`).
- Tests of a ViewModel must start the collector on an unconfined test dispatcher; a collector launched without one makes `state` read late, so some of these tests fail and the others pass without checking anything.
### Not covered
- What a screen's state contains and how a screen sends intents — each feature module.
- Throttling clicks — see `throttle`.
- Persisting state across process death: nothing here saves or restores state.

## throttle — Throttler and clocks
### Behaviour
- The first call passes. A later call passes only when at least the minimum interval (500 ms by default) has passed since the last call that passed; exactly the interval passes, one millisecond less does not.
- A dropped call does not move the reference point and is not queued or replayed. Under repeated taps one call passes per interval: 20 taps 100 ms apart with a 500 ms interval run 4 times.
- `isIdle()` only asks whether a call would pass; `tryAccept()` asks and, if so, uses up the window; `process { }` runs the action when accepted and says whether it ran; `reset()` forgets the last pass so the next call passes.
- The check and the consume are separate on purpose: a caller that must test more conditions before spending the window (the navigator, whose no-op moves must not open a window) asks first and consumes later; merging them would let a command that changes nothing block the next real one.
- The default clock is monotonic elapsed time since boot, so changing the phone's clock does not distort a window. A `Clock` is a one-method interface and a test passes a lambda to control time.
- The default clock calls the Android system clock, which throws "not mocked" in a JVM unit test; every user that has a unit test injects a clock. This is why the module is an Android library.
- `WallClock` is the separate source of epoch milliseconds for times that are stored (reading sessions, quiz results, bookmark events); its default is the system wall clock. It is never used to measure an interval.
- A throttler has no lock: it is for one thread, in practice the main thread.
### Public API
- `Throttler(minIntervalMillis, clock)` with `isIdle()`, `tryAccept()`, `process(action)`, `reset()`, and `DEFAULT_MIN_INTERVAL_MILLIS` (500).
- `Clock` — `elapsedMillis()`; `SystemClockSource` — the default, backed by the Android elapsed-realtime clock.
- `WallClock` — `nowMillis()`; `SystemWallClockSource` — the default, backed by the system time.
### Guarantees
- Interval boundary, dropped calls not moving the reference, one pass per interval under repeated taps, `process` result and `reset`: `ThrottlerTest`.
### Not covered
- The navigator's own window length and its dropped-move results — see `navigation:navigator`.
- `WallClock` has no test in this module; it is a one-line wrapper.

## result — Cancellation-safe Result
### Behaviour
- `runCatchingCancellable { }` returns a success holding the block's value, or a failure holding what the block threw.
- A `CancellationException` is rethrown, never wrapped. So a cancelled coroutine (for example a replaced `flatMapLatest` inner run) is not turned into a user-visible failure such as "sync failed", and the code after the call does not run once the coroutine is cancelled.
- Everything else that is a `Throwable` is wrapped, including errors such as out-of-memory; there is no filter.
- The function is inline, so suspend calls inside the block keep working.
- Inside coroutines the plain Kotlin `runCatching` is not used, because it swallows cancellation. The failure boundary of `mvi` uses this function.
### Public API
- `runCatchingCancellable(block)` — `Result<R>` as described above.
### Guarantees
- Success value, wrapping of an ordinary exception, rethrow of cancellation, and nothing running after a cancelled block: `RunCatchingCancellableTest`.
### Not covered
- Mapping a failure to what a person sees — the callers (sync, quiz generation, repositories, ViewModels).
