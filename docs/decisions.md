# Decisions

### NT-D4 · 2026-10 · NT-1
- decision: The format spec's source of truth is `public-repos/notes-kit/SPEC.md`. The template README and the plugin skills point to it and never copy it. The project's own internal spec, generator and hook stay untouched by this work; tests compare the closed sets between the two instead.
- why: Only tracked documents are the app feature documents, and a spec that outsiders read must live in the public repository. Changing the internal scripts at the same time would disturb writing this project's own notes.
- rejected: Copying the spec into the README and skills; replacing the internal spec, generator and hook with the public ones (merging is left for after publication).
- evidence: `public-repos/notes-kit/SPEC.md`; `public-repos/tests/test_schema_parity.py` (`NT-1 규격의 닫힌 집합이 gen_index.py 와 같다`, `NT-1 규격의 문법 표와 한도와 쓰기 규칙이 SCHEMA.md 와 같다`)

### NT-D8 · 2026-10 · NT-2
- decision: The format version is a one-line file `.mynote-format` at the repository root (absent means 1). The tool fails when the file names a version above what it supports and then leaves `index.json` as it was. `index.json` carries no version field.
- why: `index.json` uses group names as its top-level keys, so the app would read a new field as a group.
- rejected: A version field inside `notes/index.json`.
- evidence: `public-repos/notes-kit/tools/note_tool.py`; `public-repos/notes-kit/tests/test_check.py` (`NT-2 더 높은 형식이면 실패하고 index 를 안 쓴다`); `public-repos/notes-template/.mynote-format`

### NT-D11 · 2026-10 · NT-7
- decision: Tests that stand on one public folder alone live inside it (`public-repos/notes-kit/tests`). Tests that look outside the folder, or must hold private names, live in `public-repos/tests`, outside both public folders and for this repository only.
- why: The folder-alone check (NT-7) and the leak check must not break each other; a leak test inside a public folder would either catch itself or put the forbidden strings into the published copy.
- rejected: Keeping every test inside the public folders.
- evidence: `public-repos/tests/test_public_rules.py` (`NT-7 공개 폴더에 비공개 정보가 없다`, `NT-7 notes-kit 을 떼어 내도 시험이 돈다`); `public-repos/notes-kit/tests/`

### NT-D12 · 2026-10 · NT-4
- decision: The reusable action is a thin composite: it hands its inputs to one tool entry, `publish`, which does the check, the index build and the commit and push. Checkout is left to the user's workflow.
- why: Logic spread across shell inside YAML cannot be reached by unit tests; with one entry the whole logic runs against a fake remote, leaving only `action.yml` itself (input passing, paths) for a manual check after publication.
- rejected: Putting the logic in shell steps of `action.yml`.
- evidence: `public-repos/notes-kit/action.yml`; `public-repos/notes-kit/tests/test_publish.py` (`NT-4 action.yml 줄 구조`)

### NT-D14 · 2026-10 · NT-7
- decision: The Python tests run apart from `./gradlew test`. The root verify command lists every Python test folder in one line (`a && b && c`). Tests do not depend on a machine: temporary git repositories ignore global and system git settings and set their own identity, and the tool is loaded by file path.
- why: Tests that nobody runs drift silently (the repository list comparison among them), so they must be in the verify command; and results must not vary with machine git settings.
- rejected: Pulling Python into Gradle; adding CI (the repository has none).
- evidence: root `AGENTS.md` verify block; `public-repos/notes-kit/tests/test_publish.py` (`NT-4 기기의 전역 git 설정과 무관하게 돈다`)

### NT-D16 · 2026-10 · NT-8
- decision: The spec a plugin skill reads is a copy inside the plugin (`${CLAUDE_PLUGIN_ROOT}/SPEC.md`), held byte-equal to the root `SPEC.md` by a test. The source of truth stays in one place.
- why: The plugin cache copies only the plugin folder; relying on a user's repository or a public address would block the first step of reading the spec or need the network. The tool copy follows the same pattern.
- rejected: Reading the spec from the user's repository or from a public repository address.
- evidence: `public-repos/notes-kit/plugins/mynote-notes/SPEC.md`; `public-repos/notes-kit/tests/test_plugin.py`

### NT-D19 · 2026-10 · NT-3
- decision: The template, tool, spec and skills move to Markdown notes (`notes/<group>/<id>.md`, front matter `title` and `date` plus GFM body). The internal `notes/SCHEMA.md` and `notes/gen_index.py` are the source of rules and the public side follows them. Old JSON notes (format 1) stay readable; new notes are written as `.md` only. This overturns the earlier choice of keeping the public format at JSON format 1.
- why: Owner decision. The project's own notes and the app are already Markdown. Cutting JSON would break the byte-equality with the project's generator (NT-3) and drop notes from lists of repositories that still hold them.
- rejected: Dropping JSON support; staying at JSON format 1.
- evidence: `public-repos/notes-kit/tools/note_tool.py`; `public-repos/notes-kit/tests/test_check.py` (`NT-3 마크다운과 JSON 을 한 목록으로 만든다`)

### NT-D20 · 2026-10 · NT-2
- decision: The format version is raised to 2: the template's `.mynote-format` holds 2 and the tool supports 2. The version means "a tool that knows this version is needed to read this repository"; the tool reads by extension whatever the marker says, so a repository with no marker or 1 still lists its `.md` notes. The action's major version is not tied to the format number.
- why: A new kind of file is a meaning change in the compatibility rule. A tool that scans only `.json` would silently drop `.md` notes from the list; a version check makes it fail loudly instead. The project's own repository has no marker and must still produce the same list.
- rejected: Staying at version 1 for Markdown notes.
- evidence: `public-repos/notes-kit/tests/test_check.py` (`NT-2 형식 2 를 읽는다`, `NT-2 형식 표시와 상관없이 마크다운을 읽는다`); `public-repos/notes-template/.mynote-format`

### NT-D21 · 2026-10 · NT-3
- decision: The tool's Markdown check is the same as the project generator's: it reads only the front matter and never the body. The same note set passes and fails the same files and produces identical `index.json` bytes; error wording may differ. The "slightly stricter" JSON checks stay for JSON notes only.
- why: The app reads any body (undrawn syntax shows as text, over-limit notes become a box). A tool wider or narrower than the generator would make the same repository list differently depending on the tool.
- rejected: A tool that is stricter or looser than the generator on Markdown notes.
- evidence: `public-repos/tests/test_notes_parity.py` (`NT-3 합성 노트 집합에서 gen_index.py 와 출력이 같다`, `NT-3 깨진 노트에서 gen_index.py 와 함께 실패한다`)

### NT-D22 · 2026-10 · NT-1
- decision: The public `SPEC.md` describes Markdown notes as the source of truth (front matter, syntax mapping, text shapes, text-only syntax, limits, writing rules, `index.json`, format version) and keeps old JSON notes as a format-1 section with the closed-set tables. Against the internal `notes/SCHEMA.md` the test compares not sentences but the first column of the tables, the limit numbers and the number of writing rules.
- why: The public spec is in polite Korean and the internal one in plain Korean, so they cannot be byte-equal; the cells and numbers must match whatever the tone, and a change on either side breaks the test.
- rejected: Byte-equal copies; merging the two documents into one (still an open question).
- evidence: `public-repos/tests/test_schema_parity.py` (`NT-1 규격의 문법 표와 한도와 쓰기 규칙이 SCHEMA.md 와 같다`)

### NT-D25 · 2026-10 · NT-7
- decision: Publishing the template and filling the app's `template.repo` value come after the app version that reads Markdown is released. The publish order in `public-repos/README.md` says so first. Publishing itself stays a human act.
- why: An old app reads `.md` notes as JSON, so every note becomes one whole-body box and the template's first impression breaks.
- rejected: Publishing before the Markdown-reading app is out.
- evidence: `public-repos/tests/test_public_rules.py` (`NT-7 공개 순서가 앱 판 배포를 먼저 말한다`)

### NT-D26 · 2026-10 · NT-4
- decision: The first public version of the action is `v1` and carries format 2; the tag in the kit README and docs stays `@v1`. The template's own workflow no longer calls the action (see NT-D31).
- why: Owner decision. Nothing is published yet, so nobody uses a version that knows only format 1.
- rejected: Tagging the first public version `v2` to match the format number.
- evidence: `public-repos/notes-kit/README.md`; `public-repos/notes-kit/action.yml`

### NT-D27 · 2026-10 · NT-3
- decision: The tool does not warn about Markdown bodies over the limits (512KB, table cells 4,096 and so on); it does not look at bodies, as the generator does.
- why: Owner decision. Its verdict must equal the generator's (NT-D21); adding a warning would be decided together with the generator in the note-format work.
- rejected: Warning on over-limit bodies.
- evidence: `public-repos/notes-kit/tools/note_tool.py` (no body reading); `public-repos/notes-kit/tests/test_check.py`

### NT-D29 · 2026-10 · NT-7
- decision: A placeholder is an angle-bracketed token starting with a capital letter and made of capitals, digits and underscores, two characters or more. A one-letter bracket such as `<T>` is not a placeholder.
- why: The public spec must carry over the internal table's inline HTML `<T>` and the writing-rule example `Box\<T>` (NT-1), and the internal `SCHEMA.md` is not changed by this work. Counting one-letter brackets would make NT-1 and NT-7 impossible to pass together.
- rejected: Counting one-letter tokens as placeholders.
- evidence: `public-repos/tests/test_public_rules.py` (`NT-7 자리표시자가 표와 같다`); `public-repos/PLACEHOLDERS.md`

### NF-D4 · 2026-10 · NF-13
- decision: The notes list (`notes/index.json` with path, hash, title, date) stays as it is and the generator learns to read `.md`. Repositories that have no list and are scanned by folder are out of scope.
- why: The scope of the work is the parsing engine; changing the sync structure as well would raise the risk.
- rejected: Reading a folder directly without a list.
- evidence: `notes/gen_index.py`; `notes-tests/test_gen_index.py` (`NF-13 마크다운과 JSON 을 한 목록으로 만든다`)

### NF-D24 · 2026-10 · NF-15
- decision: Writing rule 5 asks writers not to start a continuation line with a list marker. The engine is not changed.
- why: The engine ends a paragraph at `2)` or an empty-item line, which differs from CommonMark. Writers can avoid it, so it is not a reason to change the engine.
- rejected: Patching the engine to follow CommonMark here.
- evidence: `notes/SCHEMA.md` writing rules; `notes-tests/test_docs.py`

### BR-D11 · 2026-10 · BR-9
- decision: This repository's own coordinates (owner and repository name) are not on the document hygiene test's deny list.
- why: Inside the repository its own name is no secret, and the public folders are checked separately by NT-7.
- rejected: Putting the repository's own coordinates on the deny list.
- evidence: `notes-tests/test_hygiene.py`; `public-repos/tests/test_public_rules.py` (`NT-7 공개 폴더에 비공개 정보가 없다`)

### SC-D6 · 2026-10 · SC-9
- decision: The overflow check is absolute at width 360dp and font 1.3x (buttons one line, no ellipsis) and relative at 2.0x and large app text size (ellipsis places do not increase against the before-capture).
- why: Zero ellipsis at 2.0x cannot be kept even with the old wording, and the maximum system font scale of Android 14 is above 1.3x.
- rejected: Zero ellipsis and one-line buttons at every scale including 2.0x.
- evidence: `app/src/androidTest/java/com/hancekim/mynote/brand/TitleFontScreensTest.kt`, `docs/design/screen-copy.md`.

### SC-D7 · 2026-10 · SC-9
- decision: At 1.3x, the one-line places that start with text that changes, such as a note title (the quiz settings subtitle), and the collapsed notification are compared relatively against the before-capture instead of requiring zero ellipsis; the screen shape of that place stays as it is.
- why: Both could not keep zero ellipsis at 1.3x even before the wording change, and wording cannot fix it; owner decision.
- rejected: Holding the absolute zero-ellipsis rule for those two places, or changing their screen shape (ellipsize only the title, allow two lines) inside this work.
- evidence: `docs/design/screen-copy.md` (overflow section); quiz module strings `quiz_*`.

### FM-D3 · 2026-10 · FM-1, ui
- decision: Screen pieces, screen models and cross-feature signal contracts that two or more features use go into a new `:core:ui` module (owner decision).
- why: The design system stays a place that does not know feature meaning such as model state or token input.
- rejected: Putting them into `:core:design-system`.
- evidence: `core/ui/` (`signal/FeatureSignals.kt`, `frame/ShellFrame.kt`, `mapper/NoteBlockMapper.kt`), `notes-tests/test_feature_modules.py` (`FM-1 공용 조각은 core:ui 와 core:navigation 에 산다`).

### FM-D21 · 2026-10 · FM-1, ui
- decision: Five strings used by two or more features and the shared pieces created by the ViewModel split (snackbar wording, failure log, snackbar notice, font-size mapping) move to `:core:ui` in the shared-pieces step; the disconnect dialog model, used only by settings, goes with the settings extraction.
- why: The shared-pieces table was counted at the base commit and missed them; leaving them would make later feature extractions depend on notes (an FM-2 violation).
- rejected: Leaving them in `:feature:notes`.
- evidence: `core/ui/src/main/java/com/hancekim/mynote/core/ui/NotesMessages.kt`, `FailureLog.kt`, `notes-tests/test_feature_modules.py` (`PLACED_AHEAD` now empty).

### DEC-20 · 2026-10 · NT-8
- decision: The plugin folder carries its own copies of SPEC.md and note_tool.py and the skills read them through the plugin root; the canonical copies are notes-kit/SPEC.md and notes-kit/tools/note_tool.py. A test pins the copies byte-for-byte to the canonical ones, so editing the canonical file without the copy fails the build.
- why: The plugin cache copies only the plugin folder, and the user's notes repository has no spec.
- rejected: Pointing the plugin at the repository-root files.
- evidence: public-repos/notes-kit/plugins/mynote-notes (SPEC.md, scripts/note_tool.py); public-repos/tests; root AGENTS.md section on public assets.
