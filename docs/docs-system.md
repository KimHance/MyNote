# Document system

What documents this project keeps, where they live, what their frontmatter holds and how to write them.
The tables are read by `docsys.py check`; the prose is for people and agents. Change a table to change the rule for this project.

## Reading
- Do not read everything. Start at the module map (`<module>/docs/overview.md`), find the unit you are about to touch, and open only that unit's spec.
- A spec file's head (`units` · `plans` · `decisions`) is a summary of its body. The hance plugin's office mode keeps it (`sync`), reads all heads at once (`heads`) and verifies them (`check`); outside office mode, keep the head in step with the sentences you change.
- Read a unit's spec before changing its code: it holds rules the code alone does not show.
- The reasons behind a spec file are in the decisions file of the same name (`spec/login.md` → `decisions/login.md`), when there is one.
- A tag at the end of a spec sentence, `(<plan>: <item>)`, names the plan it came from. Follow it only when you need the reason.
- When a document and the code disagree, do not pick one silently — tell the user what differs.
- `.claude/rules/docs.md` and the block in `AGENTS.md` are generated from the Documents table below (`docsys.py init`); do not edit them by hand.
- Each module folder's `AGENTS.md` carries one line saying which documents the module has (`sync` writes it; the rest of the file is yours).

## Project
| key | value |
|---|---|
| modules | `app build-logic core/* feature/*` |
| sources | `.kt` |
| test | `./gradlew test && python3 -I -m unittest discover notes-tests && python3 -I -m unittest discover public-repos/tests && python3 -I -m unittest discover public-repos/notes-kit/tests` |
| ui-test | `ANDROID_SERIAL=<device serial> ./gradlew :core:design-system:connectedDebugAndroidTest && ANDROID_SERIAL=<device serial> ./gradlew :app:connectedDebugAndroidTest` |

- `modules` — the folders that are modules, as globs relative to the project root, space-separated (`src/*`, `packages/* apps/*`, `src`).
- `sources` — source file extensions, space-separated (`.js .ts`). Every source file in a module belongs to one of its units.
- `test` — the command that runs the tests.
- `?` = not filled in yet (`check` fails until it is). `-` = there is none.
- Optional row `ignore` — file-name patterns that need no unit (`index.ts *.d.ts`).
- Optional row `ui-test` — the command that runs the UI tests: tests that drive the real screen the way a person would
  (open it, press, type, read what is shown). A project with a screen people use has this row; the tool is whatever fits
  this project's platform. May be the same command as `test`.

## Documents
| doc | path | max lines | role |
|---|---|---|---|
| project | docs/overview.md | 60 | what this is, its modules, how it ships |
| module | <module>/docs/overview.md | 60 | what the module owns, its units |
| plan | docs/plan/<NNN>-<work>.md | 60 | one piece of work: why · flow · not doing + the items it builds |
| spec | <module>/docs/spec/<group>.md | 300 | how each unit behaves now |
| decisions | <module>/docs/decisions/<group>.md | 300 | choices where alternatives were compared — one file per spec file, same name |

Any other document a project keeps (design notes, research, release checklists, runbooks, data appendices) is outside this
system: it lives wherever the project puts it, `check` does not read it, and a document here may point to it by path.

The project doc and each module doc open with frontmatter (`---` … `---`) and have `## Overview`.
A module doc also has `## Structure` — how its units connect.

## Fields
| doc | field | required | points to | meaning |
|---|---|---|---|---|
| project | id | yes | - | project id, kebab-case |
| project | status | yes | - | active · paused · done |
| project | name | no | - | display name |
| project | modules | no | module | module ids that have a module doc |
| module | id | yes | - | unique in the project |
| module | name | yes | - | display name |
| module | aliases | no | - | other names, any language |
| module | units | no | unit | the module's parts, one `{id: …, name: …}` per line |
| module | uses | no | module | other modules this one depends on |
| unit | id | yes | - | unique in the module; the sub-folder of the same name holds its code |
| unit | name | no | - | display name |
| unit | aliases | no | - | other names, any language |
| unit | code | no | code | files or folders (ending `/`) relative to the module — only when not the folder of the same name |
| spec | units | no | unit | filled by sync — the units this file's sections describe |
| spec | plans | no | plan | filled by sync — the plans its sentences cite |
| spec | decisions | no | decision | filled by sync — the decisions in the file of the same name, then others whose title names one of its units |

`unit` rows are the keys of one entry in a module's `units` list. A meaning written as `a · b · c` lists the allowed values.
`points to` names what a field's values are: `module` (a module id), `unit` (`module:unit`, or a unit id of the same module),
`code` (a path), `plan` (a plan file name without `.md`, number included), `decision` (a decision id); `-` = not a link. Fields filled by sync are never written by hand.

Fields can differ per project. A field that points to something says so in `points to`. Add a project-wide field as a row here;
a field only one document uses goes in that document's `fields:`, with its meaning. `heads` and `check` read this table,
so nothing else needs to change when a field is added.

```yaml
owner: payments-team
fields:
  owner: the team that answers for this module
```

## Plan
| rule | value |
|---|---|
| sections | Plan · Items |
| optional sections | Assumed · Copy |
| plan bullets | why · flow · not doing |
| item columns | ID · What · Accept |
| item id | `^[A-Z]{2,4}-[A-Z]?[0-9]+$` |

One piece of work per file, named `<NNN>-<work>.md`: `NNN` is three digits, one more than the highest number already in the
folder (the first plan is `001`), so the files list in the order the work was done. `<work>` is a short kebab-case name.
`## Plan` has one bullet per plan key (`- why: …`).
`## Items` is a table — one row per behaviour, an id of the pattern above (unique across all plans), and an Accept that a test can check.
The optional letter after the dash keeps the ids the old feature documents and the tests already use: `K` for an invariant (`LN-K1`).
Keep it short: one line of Accept per item — the detail belongs in the tests and in the unit's spec.
The plan file stays after the work is done, and it says what was actually built: when the work ends up different from the
plan (an item dropped, added or changed), correct the Items before the work is closed.
`## Assumed` (optional) lists what was decided without asking the user, one bullet each — what was unclear and what was chosen.

## Spec
| rule | value |
|---|---|
| section title | `## <unit id> — <name>` |
| headings | Behaviour · Public API · Guarantees · Not covered · Unconfirmed |
| required headings | Behaviour · Public API · Not covered |

How each unit behaves now — not how it got there. A module with units keeps their specs in its `docs/spec/` folder:
any file name and grouping, sub-folders allowed; a full file is split inside the same folder. A single `<module>/docs/spec.md` is not allowed.
Every unit of the module has exactly one section, titled as above, and under it only these headings:

- `### Behaviour` — what it does now, in the present tense; behaviour, not implementation.
- `### Public API` — the public names, one line each; only names the code actually declares public.
- `### Guarantees` (optional) — what callers may rely on; may name the test that proves it.
- `### Not covered` — what is out of scope, and common misreadings; point to the other unit by its id.
- `### Unconfirmed` (optional) — anything not verified in the code.

When code changes how a unit behaves, edit that sentence in the same commit — rewrite it in place, never append history.
Write absence explicitly ("no retry", "none"): readers invent what is not stated.

A bullet may end with a tag naming the plan item(s) behind it: `(<plan>: <item id>)`, where `<plan>` is the plan file name
without `.md`, number included — ``- `^` is evaluated left to right (003-left-assoc: LA-1)``. Several items of one plan:
`(002-power-operator: POW-2, POW-4)`; items of two plans: two tags, `(002-power-operator: POW-2) (003-left-assoc: LA-1)`.
The tag names the item behind the current sentence; it is not a history list. When a later work rewrites the sentence, replace
the tag with the new plan and item. A bullet without a tag is fine (for example, behaviour written down from existing code that no
plan introduced). A tag must name a plan file that exists and items that are in it. A bare `(POW-2)` without the plan is an error;
parentheses holding anything else, such as only decision ids (`(ADR-12)`), are ordinary text.

```markdown
## parse — Parser
### Behaviour
- Reads one expression per line; `^` is evaluated right to left (002-power-operator: POW-2).
- A bad line stops the run; no recovery.
### Public API
- `parse(text)` — the expression tree for `text`; raises `ParseError` on bad input.
### Not covered
- Evaluating the tree — see `eval`.
```

## Decisions
| rule | value |
|---|---|
| bullets | decision · why · rejected · evidence |

Title of each entry: `### <id> · <YYYY-MM> · <target>[, <target>]`. A target is a module id, `module:unit` or a plan item ID, and it must exist.
The id is unique across the whole project.

Where an entry goes: in the decisions file named after the spec file it is about — a decision about a unit described in
`<module>/docs/spec/login.md` goes in `<module>/docs/decisions/login.md` (sub-folders mirror the spec folder). Create the file with
the first decision; no spec file of that name → no decisions file. A decision about units of several spec files goes with the
first one; the other spec files reach it through their `decisions` head.
Only what belongs to no unit — the whole project, how it ships, a project without modules — goes in `docs/decisions.md`.

## Rules
- Documents are written in English (fewer tokens for the agents that read them); names may carry `aliases` in any language.
- Docs hold intent, reasons and behaviour — not how the code does it.
- No code blocks in documents: a reader who sees a snippet believes it has read the code. Say it in words and name the file;
  names (`ParseError`) and input → result examples (`2^3` → `8`) inside a sentence are fine. Only `mermaid` diagrams may be fenced.
- An item that changes what a person sees or operates is proven by a UI test, not only by a test of the logic under it.
- Write a decision only when alternatives were compared. Overturned → delete it.
- A doc at its line limit: move the oldest settled part out; do not raise the limit.
