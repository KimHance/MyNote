# Docs system migration

## Plan
- why: The plugin's document system changed (one spec file per feature with a change log became a plan per piece of
  work, per-unit specs inside each module and a decisions file beside each spec file), so this project's documents no
  longer pass its checker and agents no longer know where to look.
- flow: An agent opens `docs/overview.md`, goes to the module map of the code it will touch, reads only that unit's
  spec, follows a tag to the plan when it needs the reason, and proves the documents with `docsys check` and the tests.
- not doing: No change to what the app shows or does and no change to Kotlin behaviour. Module `AGENTS.md` files keep
  their working rules and traps (only references change). `DESIGN.md`, `docs/design/`, `labs/`, `notes/SCHEMA.md` and
  `README.md` keep their roles and language. No new UI tests.

## Items
| ID | What | Accept |
|---|---|---|
| DM-1 | The project carries the document system definition as the plugin's template words it, the generated rule file, the root `AGENTS.md` block and the documents line in each module's `AGENTS.md` | `docsys check` prints errors 0 and warnings 0; `docsys template-diff` reports no missing line |
| DM-2 | The project map lists nineteen modules and each module has a map whose units own every Kotlin file, tests included | `docsys check` reports no `unit`, `code` or `modules` error; `docsys heads` lists nineteen modules |
| DM-3 | Every unit has one spec section, written from the code as it is now, in English, without code blocks | each unit has a section with Behaviour, Public API and Not covered; a sample of bullets per module matches the code |
| DM-4 | The work done so far is rebuilt as plans 001–013 in the order it was done: nine from the old feature documents and four (reader foundation, quiz, bookmarks, statistics) from the git log and the earlier planning notes | thirteen plan files pass `docsys check` |
| DM-5 | Item ids that tests and source comments cite stay resolvable | every id with prefix BR, FM, LN, MD, NF, NT, RC, SC or SY found in tracked Kotlin, Python and Markdown outside the old documents is an item of some plan |
| DM-6 | Decisions keep only choices where alternatives were compared, each aimed at a module, unit or item and filed beside the spec file of its first unit | `docsys check` reports no `orphan`, `decision`, `place` or `pair` error; each decisions file is 300 lines or fewer |
| DM-7 | The old documents are gone and nothing points at them | `docs/spec/` and `DECISIONS-TO-REVISIT.md` do not exist; a search for those paths in tracked files outside `docs/plan/` finds nothing |
| DM-8 | What the owner still has to decide is not lost | the open entries of the old decision record and the open questions of the old feature documents are listed in the root `AGENTS.md` under a not-decided-yet heading |
| DM-9 | The repository's own checks still pass | the `test` command of `docs/docs-system.md` and `./gradlew ktlintCheck` pass; no file under any `src/` folder differs from the base commit |
