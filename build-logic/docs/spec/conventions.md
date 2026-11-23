---
units: [build-logic:plugins, build-logic:shared-config, build-logic:signing, build-logic:test-inputs, build-logic:ktlint]
plans: [008-note-template, 012-feature-modules]
decisions: [NT-D15, FM-D15, DEC-19]
---
## plugins — Convention plugins
### Behaviour
- Nine plugin ids are registered: `hancenote.android.application`, `.application.compose`, `.library`, `.library.compose`, `.feature`, `.hilt`, `.room`, `hancenote.jvm.library` and `hancenote.ktlint`. A module applies them through the version-catalog alias of the same name with dots turned to dashes (`hancenote-android-library`).
- `hancenote.ktlint` has no catalog alias; only other plugins apply it, so a module never declares it.
- Application plugin: applies the Android application plugin and ktlint, then the shared Android and Kotlin setup; sets `targetSdk` from the catalog and the AndroidJUnit runner, turns off dependency info in both the APK and the bundle, and wires release signing.
- Dependency info is off because its encrypted block would make F-Droid reject the APK and distribution is a GitHub release APK; with it off, the release task graph has no dependency-report tasks.
- Application compose plugin applies the application plugin itself plus the Compose compiler plugin and the shared Compose setup. A module that lists both application plugins is redundant, not wrong.
- Library plugin: Android library plugin, ktlint, shared Android and Kotlin setup, the AndroidJUnit runner and the repo test inputs. Library compose plugin applies the library plugin, the Compose compiler plugin and the shared Compose setup.
- Feature plugin: applies the library compose plugin and the Hilt plugin, sets `resourcePrefix` to the module name plus an underscore (`quiz` gives `quiz_`), and adds the shared dependencies, so a feature module has no `dependencies` block. (012-feature-modules: FM-11)
- Feature plugin dependencies: `:core:common`, `:core:design-system`, `:core:domain`, `:core:navigation` and `:core:ui`; lifecycle runtime, runtime-compose and viewmodel-compose; coroutines for Android; Hilt navigation compose; core Material icons; and JUnit with coroutines-test for unit tests.
- A feature module never depends on another feature module; the feature plugin adds none. A shared piece goes to `:core:ui`. The Python module-boundary tests read this plugin's source to compute what a feature module can reach. (012-feature-modules: FM-2)
- Feature plugin names project modules as strings, so renaming or moving a core module also means editing this file.
- Hilt plugin: applies KSP and the Hilt Gradle plugin, adds the Hilt runtime and puts the Hilt compiler on KSP.
- Room plugin: applies Room 3 and KSP, writes schemas to the module's `schemas` folder, adds the Room runtime and puts the Room compiler on KSP.
- JVM library plugin: Kotlin JVM plugin and ktlint, Java 17, JUnit 4 for tests, no tests found is not a failure, repo test inputs. It applies no Hilt; modules using it (`domain`, `format`) use plain constructor injection.
- Because that plugin is not an Android library, `import android.*` in those modules is a compile error: the purity rule is enforced by the build.
- The Kotlin Android plugin is never applied: Kotlin is built into the Android Gradle Plugin version in use. The Kotlin JVM plugin is applied only by the JVM library plugin.
- Plugin checks are strict: plugin validation runs with stricter rules and fails on any warning.
- A change to a plugin takes effect on the next Gradle call, which compiles the convention project first; there is no publish step.
### Public API
- `hancenote.*` plugin ids — as listed above; module build files use the catalog aliases.
- Plugin classes `AndroidApplicationConventionPlugin`, `AndroidApplicationComposeConventionPlugin`, `AndroidLibraryConventionPlugin`, `AndroidLibraryComposeConventionPlugin`, `AndroidFeatureConventionPlugin`, `HiltConventionPlugin`, `AndroidRoomConventionPlugin`, `JvmLibraryConventionPlugin` — public classes behind the ids; nothing calls them directly.
### Guarantees
- The module boundary rules that read this source (no feature-to-feature dependency, no data layer in features) are checked by the Python suite in `notes-tests/test_feature_modules.py`, not by Gradle.
### Not covered
- SDK levels, Java target, desugaring, Compose dependencies and the version catalog accessor — see `shared-config`.
- Release signing — see `signing`. Test input folders — see `test-inputs`. Ktlint setup — see `ktlint`.
- Dependencies a module adds for itself (Room entities, Retrofit, serialization plugin alias): declared in that module's own build file.
- `:core:format` applies the Gradle core `java-test-fixtures` plugin directly. This is a deliberate exception: no convention is made for a thing one module uses.
- Resource prefixes of non-feature modules (`ds_`, `ui_`, `ai_`) are written in those modules' build files; the `:app` module has none. Android Lint enforces a prefix only in modules that declare one, and the Python test FM-11 rejects a library with resources and no prefix. (012-feature-modules: FM-11)

## shared-config — Shared module setup
### Behaviour
- Android modules get `compileSdk` and `minSdk` from the catalog entries of those names (`targetSdk` only for the application); no module build file states an SDK level.
- Java source and target are 17 and Kotlin `jvmTarget` is 17 for Android and JVM modules alike; core library desugaring, with the catalog's desugar library, is on for Android modules only.
- No JDK toolchain is declared: code compiles with the JDK that runs Gradle and only the target is fixed at 17.
- Unit tests in Android modules include Android resources; every `Test` task has the no-tests-found failure turned off, so a module without tests does not break the root test run on Gradle 9.
- All Kotlin warnings become errors only when the Gradle property `warningsAsErrors` is `true` (letter case ignored); passing `-PwarningsAsErrors` with no value is false.
- Compose setup turns Compose on with a single plain assignment on the common extension; it is not narrowed by extension type, because a `when` over the extension type would leave Compose off for an unexpected type and the build would then fail with a misleading "composable not found" error.
- Compose setup adds the Compose BOM to both `implementation` and `androidTestImplementation`, then activity-compose, Material 3, UI, UI tooling preview and the immutable-collections library; UI tooling and the test manifest in debug builds; the UI test library in androidTest.
- It adds Slack's Compose lint checks, turns off the Android Lint check `ComposeCompositionLocalUsage` (theme tokens travel by composition local on purpose), and keeps source information in Compose output.
- Compose stability reports are written to the module's `build/compose-reports/` only when the property `composeMetrics` exists, with any value; they are off by default because writing them slows every build.
- `Project.libs` is the only way convention code reads a version or coordinate from the catalog; the catalog file is the root `gradle/libs.versions.toml`, the same file this build's settings read for the Gradle plugin classpath.
### Public API
- `Project.libs` — the `libs` version catalog of the project.
### Guarantees
- A version or coordinate is not hard-coded in any module build file; moving a version means editing the catalog only.
### Not covered
- Which plugin calls what — see `plugins`.
- Hard-coded numbers such as the Java and `jvmTarget` level 17 are written in the helper source, not in the catalog.
### Unconfirmed
- Whether a build with `-PwarningsAsErrors=true` passes today: the owner's module notes say three opt-in warnings in `:core:common` tests stop it; this was not re-run for this document.

## signing — Release signing
### Behaviour
- Release signing reads four keys from the root `local.properties`: `keystore.path`, `keystore.password`, `key.alias`, `key.password`. A key with a blank value counts as missing; a missing file counts as no keys.
- With none of the four present, the build continues silently and the release APK is unsigned. This is the normal case for a developer without the key.
- With some but not all present, the build continues with a warning that names the missing keys, and the release APK is unsigned.
- With all four present but no file at the keystore path, the build continues with a warning that names the path, and the release APK is unsigned.
- With all four present and the file found, a `release` signing config is created and attached to the release build type.
- No case fails the build. The silent and the warning cases are split because otherwise a misspelt key would end in a successful build with an unsigned APK that fails only at install or upload.
- An unsigned release is recognised by its file name: `app-release-unsigned.apk`.
### Public API
- None — the function is internal to the convention project and is called only by the application plugin.
### Not covered
- Where the keystore comes from and any CI secrets — outside the repository.
- Applying the config to the app module — see `plugins` (application plugin).

## test-inputs — Repo folders as test inputs
### Behaviour
- A module opts in by setting an extra property in its build file (anywhere after the plugins block, since it is read after evaluation); only the property's presence counts, not its value.
- `notesTemplateInput` adds the `public-repos/notes-template` folder, `notesInput` adds `notes` and `notes-tests/fixtures/front-matter`, and `catalogInput` adds only `gradle/libs.versions.toml`.
- Each opted-in module's `Test` tasks treat those paths as inputs with relative path sensitivity, so a one-character change in a folder re-runs the tests instead of reporting them up to date.
- Only `:core:data` (template input) and `:core:format` (notes and catalog inputs) opt in; all other modules are unaffected on purpose, so one edit in `notes/` does not rerun every module's tests.
- A missing folder is an empty input, not an error. `__pycache__` folders are excluded everywhere, because Python tests write them into `notes/` and would otherwise make JVM tests rerun after every Python run.
- Adding a folder means adding one row to the table in `RepoTestInputs.kt`; a single file in a folder is selected with an include pattern.
- The template input exists so the test that feeds the template's example notes through the app's sync reruns when an example note changes. (008-note-template: NT-6)
### Public API
- `Project.configureRepoTestInputs()` — wires the inputs for the properties the module set; called by the library and JVM library plugins.
### Not covered
- The tests that read those folders — modules `data` and `format`.

## ktlint — Ktlint convention
### Behaviour
- Applies the ktlint Gradle plugin, pins the ktlint version to the catalog entry `ktlint`, and adds the Compose rules ruleset to the ktlint ruleset configuration.
- The application, library and JVM library plugins apply it, so every module is checked; a module build file never applies it.
- Rule settings are not here: they are the root `.editorconfig` (maximum line 120, trailing commas allowed, composable function names exempt from naming, function-signature, class-signature and annotation rules off, the Compose composition-local allowlist rule off).
- Unused imports are reported by nobody: the ktlint rule exists but is opt-in and the `.editorconfig` does not enable it. Enabling it must be done inside the Kotlin section of that file; the same line placed under the markdown and JSON section is silently ignored.
- The pre-commit hook runs ktlint on staged Kotlin files only, and stops the commit on a violation.
### Public API
- `KtlintConventionPlugin` — the plugin class behind the id `hancenote.ktlint`; other plugins apply it by id and it has no catalog alias.
### Not covered
- Formatting rules themselves — the root `.editorconfig`.
- The commit hook script — `.githooks/pre-commit`, outside any module.
