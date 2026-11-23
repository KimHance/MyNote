# Decisions

### RD-D4 · 2026-09 · RD-7
- decision: The app has one `NavDisplay`; the layout differs only by the scene strategy (none at Compact so screens stack, a list-detail strategy from Medium up with a fixed pane and one detail pane), and the scene key stays fixed so changing the open note fades only the body.
- why: Layout is chosen by adding or removing a strategy, so screens carry no form-factor branches; a stable scene key avoids rebuilding the whole scene when only the note changes. Slots passed to the strategy are remembered so a new lambda each composition is not read as a new strategy.
- rejected: Branching in the screens by window size; one display per form factor.
- evidence: `core/navigation` `ListDetailScene`, `ListDetailSceneStrategyTest` (`상세가 바뀌어도 씬 키는 그대로다`), `NotesLayoutTest`.

### RC-D33 · 2026-10 · RC-14
- decision: When the connect screen is on top of the back stack it is one full-width column on wide windows (Medium and up), not a list plus detail scene; when it is not on top the scene is unchanged.
- why: Two panes would squeeze the connect screen to half width with a list showing behind it.
- rejected: Letting the connect screen split into list and detail panes.
- evidence: `core/navigation/.../scene/ListDetailSceneStrategyTest` (`RC-14 연결 화면은 넓은 창에서도 한 칸이다`, `RC-14 연결 화면이 맨 위가 아니면 장면은 그대로다`).
