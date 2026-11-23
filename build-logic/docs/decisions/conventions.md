# Decisions

### NT-D15 · 2026-10 · NT-6
- decision: Telling Gradle that the template folder is an input of the JVM test that reads it is a general rule in the convention plugin, switched on only by the module that asks (today `:core:data`), not a manual setting in a module's build file.
- why: Without it, editing only the template and running `./gradlew test` skips the test as up to date and it passes with no count. Manual per-module settings are forbidden by the project, and wiring it into every module would rerun all module tests for a one-letter template change.
- rejected: A manual input in `core/data/build.gradle.kts`; declaring the folder as input of every module's tests.
- evidence: `build-logic/convention/src/main/java/com/hancenote/convention/RepoTestInputs.kt` (`notesTemplateInput`); `core/data/src/test/java/com/hancekim/mynote/core/data/template/TemplateNotesTest.kt`

### FM-D15 · 2026-10 · FM-11
- decision: The home of a string is decided by how many modules reference it (one: that module, two or more: `:core:ui`, common actions: design-system), not by its prefix.
- why: Prefix and real use disagree (the quiz CTA wording is used by a shared decision, settings and model wording by the list panel).
- rejected: Placing strings by their old prefix.
- evidence: `notes-tests/test_feature_modules.py` (`FM-11 문구는 참조하는 모듈 자리에 산다`).

### DEC-19 · 2026-10 · NT-6, NT-7
- decision: Python tests of the public assets run with python3 unittest from the one-line verification command in the root AGENTS.md, not from Gradle. Only the check that template examples are read by the app parser (NT-6) is a JVM test, and only the data module takes the template folder as test input.
- why: The repository has no CI and Gradle should not gain a Python dependency; if every module took the template folder as input, one template character would rerun every test.
- rejected: Wiring the Python suites into ./gradlew test; giving all modules the template folder as input.
- evidence: Root AGENTS.md verification command; core/data TemplateNotesTest ("NT-6 ..."); no .github directory in the repository.
