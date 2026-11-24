---
id: mynote
name: MyNote
status: active
modules: [app, build-logic, ai, common, data, database, design-foundation, design-system, domain, format, navigation, network, ui, connect, editor, notes, quiz, settings, stats]
---

## Overview

MyNote is an Android app for reading Markdown study notes. Notes come from two places: repository notes, fetched from
a GitHub repository the person connects and cached on the device (read-only in the app), and device notes, written in
the app or imported (share, `.md` file, paste), whose only original is the app database. Connecting a repository is
optional; the first run opens on an empty home. An on-device language model (LiteRT-LM) turns a note into a
four-choice quiz and writes short study summaries; the 2.6 GB model is not in the APK and is downloaded at run time.
Core flow: home (note cards, bookmarks, statistics) → list panel → reader → quiz at the end of a note; settings hold
the repository, the token, the text size and the model card.

## Modules

| module | folder | owns |
|---|---|---|
| `app` | `app` | composition root (Hilt), the shell: start branch, scaffold, `NavDisplay`, overlay slots, back order |
| `notes` | `feature/notes` | home, bookmarks, list panel, reader, connection strip |
| `connect` | `feature/connect` | repository connect screen, change and take-over dialogs |
| `settings` | `feature/settings` | settings screen, repository row, model card, disconnect dialog |
| `quiz` | `feature/quiz` | quiz overlay and the call-to-action card at the end of the reader |
| `stats` | `feature/stats` | statistics and past summaries |
| `editor` | `feature/editor` | device-note editor, paste, import, delete confirm, share-in session |
| `ui` | `core/ui` | screen pieces two or more features use, the shell frame contract, signals between features |
| `ai` | `core/ai` | LiteRT-LM quiz generation, the statistics worker, the model download |
| `network` | `core/network` | GitHub Contents API |
| `database` | `core/database` | Room 3 note cache, study records, summary cache, DataStore |
| `data` | `core/data` | data-source interfaces, repositories, reconcile |
| `format` | `core/format` | pure JVM: source text → blocks (format choice, limits, Markdown, JSON format 1, block cache) |
| `domain` | `core/domain` | pure JVM: models, repository interfaces, use cases |
| `navigation` | `core/navigation` | Navigation 3 contract: keys, navigator, scenes, registry |
| `design-system` | `core/design-system` | components, block renderers, charts, theme, motion |
| `design-foundation` | `core/design-foundation` | tokens (colour, radius, type, adaptive window classes) |
| `common` | `core/common` | MVI base, result type, throttler |
| `build-logic` | `build-logic` | `hancenote.*` convention plugins |

Dependencies always point from outside to inside; interfaces are owned by the inner module and implemented by the
outer one. Features depend only on `domain` use cases and never on each other; the shell that puts features on one
screen lives only in `app`. `domain` and `format` are pure JVM, so an Android import there is a compile error.

## Outside the modules

- `AGENTS.md` (root and one per module, Korean) — working rules, commands and traps of the project and of each module.
- `DESIGN.md` and `docs/design/` — the design system and per-feature design notes (Korean); `labs/` holds design drafts.
- `notes/` — the note content the app reads from GitHub (format: `notes/SCHEMA.md`); `notes-tests/` holds Python tests that pin rules no single module owns: screen wording (plan 011), module boundaries and resource prefixes (012), document hygiene and brand resources (010), backup rules (013), the generator and hook (009).
- `public-repos/` — originals of the public notes template and its kit (spec, tool, plugin); its tests pin the public spec copies to `notes/SCHEMA.md`, limit numbers included (plan 008).
- `gradle/libs.versions.toml` and `settings.gradle.kts` — the source of truth for versions and modules.
- Source comments and test names cite plan items by id (`LN-13`, `FM-10`); the plan that holds an id is found with `docsys.py cites <id>`.

## How it ships

- `./gradlew :app:assembleDebug` builds the debug APK; the full check is `./gradlew ktlintCheck :app:assembleDebug test lintDebug` plus the three Python test folders. Instrumented tests run per module on a connected device.
- Not published to a store. The quiz model is fetched by the app (WorkManager with a foreground notification).
- Note content ships by `git push` of `notes/`: the app reads the remote, and a pre-commit hook regenerates `notes/index.json`.
