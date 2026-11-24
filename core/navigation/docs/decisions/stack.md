# Decisions

### RD-D2 · 2026-09 · RD-16, navigation
- decision: One `Navigator` owns the back stack and every user move goes through its `dispatch`, which drops a move that arrives within the transition window; a dropped move is `Throttled`, a pop at the root is `Ignored`, and no-op moves do not open the window.
- why: Fast taps changed the stack faster than the animation and cut transitions and shared elements; with the stack touched from many places there was no single spot to stop it. The window is taken from the scene motion so lengthening a motion lengthens the throttle. Separating the two results keeps a throttled back during a transition from being counted as an exit.
- rejected: Queueing moves (every tap would still arrive, defeating the point); changing the stack first and reverting (the display starts a transition and screen and stack disagree); deriving the stack from a view-model `activeNoteId` or keeping it in `rememberNavBackStack` (two owners of state).
- evidence: `core/navigation` `Navigator`, `NavigatorTest` (`전환 중 루트에서 pop 하면 Ignored 가 아니라 Throttled 다`), `SceneMotion`.

### BM-D1 · 2026-10 · navigation:commands, BM-7
- decision: The Bookmarks screen is its own back-stack destination pushed onto Home, so the stack is Home then Bookmarks; the Home tab always replaces the stack with Home alone, and the Bookmarks tab replaces any deeper stack with Home then Bookmarks.
- why: Back becomes an ordinary pop to Home with no special case, and the navigation library supplies the transition, predictive back and per-destination scroll state. A single pop from a stack of readers could not reach Home.
- rejected: Replacing the whole stack with Bookmarks as the only root (back would be ignored there, leaking into the exit hint and needing a tab branch in the back handler); using the old single-pop close action for the Home tab.
- evidence: `core/navigation/.../TabSwitch.kt`, `TabSwitchTest`.

### LN-D15 · 2026-10 · LN-10
- decision: Every user move that removes a changed editor from the stack goes through the leave confirmation, not only back and close but also wide-window tab switches and stack replacement; moves that keep the editor (reader or settings stacked on top) do not ask; a move after Discard is not asked again.
- why: Written text is not dropped without asking (same intent as the share-over-share rule of LN-8).
- rejected: Confirming only on back and close.
- evidence: `core/navigation/src/main/java/com/hancekim/mynote/core/navigation/Navigator.kt`, `core/navigation/src/test/java/com/hancekim/mynote/core/navigation/NavigatorTest.kt`, `EditorViewModelTest` (`LN-10 바뀐 편집기를 떼는 탭 전환은 먼저 묻는다`).
