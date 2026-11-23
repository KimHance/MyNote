---
units: [app:di, app:build]
plans: [001-reader-foundation, 007-repo-connect, 010-brand, 012-feature-modules, 013-local-notes]
decisions: [BR-D13, FM-D10, FM-D16, FM-D23, DEC-18]
---
## di — Hilt wiring
### Behaviour
- Five Hilt modules in the singleton component and one qualifier, `@ApplicationScope`, complete the graph. `:app` is the only module that sees data, network, database and ai together, so a missing binding fails the `:app` compile, never at runtime.
- The application scope is one singleton coroutine scope on the default dispatcher with a supervisor job; a failing child is logged at error level under the tag `MyNote` and does not cancel its siblings. It is a qualified binding, so a second scope can be added without clashing.
- The system clock and the wall clock are provided as unscoped system implementations.
- The navigator is a singleton on the application scope, starting at Home, so the back stack outlives the Activity and ViewModels and survives rotation; it is lost on process death, there is no save or restore (001-reader-foundation: RD-16).
- The connection seed is null unless the owner and repository build values are both non-blank; otherwise it carries owner, repository, branch and the token, the token null when blank. Release builds always have blank values, so they carry no seed (007-repo-connect: RC-19).
- The template repository is read from the build value by the domain parser; an empty or malformed value gives null and the connect screen shows no template entry (007-repo-connect: RC-26).
- Five seams of feature modules are bound here to their Android implementations: notes messages, editor messages, failure log, quiz auto-advance policy (follows the accessibility setting) and stats trace (writes to the log).
- The model download intents binding is in this module too; see `download`.
- Bindings of repositories, data sources, token holder, quiz generator and insight scheduler come from the four implementation modules and are not repeated here.
### Public API
- `ApplicationScope` — the qualifier of the application coroutine scope.
- `AppModule` — provides the application scope, the clock and the wall clock.
- `NavigationModule` — provides the singleton navigator.
- `RemoteConfigModule` — provides the nullable connection seed and template repository.
- `NotesModule` — binds the five feature seams.
- `DownloadIntentsModule` — binds the model download intents to `AppModelDownloadIntents`.
### Not covered
- The implementations that are bound — `core:ui`, `feature:editor`, `feature:quiz`, `feature:stats`, `ai` modules.
- Parsing rules of the seed and template values — `domain` module; where the build values come from — `app:build`.

## build — Build, manifest and resources
### Behaviour
- Six build-config fields exist: owner, repository, branch, token, template repository and title font name; the default for the first four is blank, so no repository is built into the code and a first launch is the home first-run empty state (007-repo-connect: RC-19) (013-local-notes: LN-1).
- A debug build fills owner, repository, branch and token from the developer's local properties; the token is included only when owner and repository are both set, and a blank branch means the repository's default branch (007-repo-connect: RC-19).
- A release build sets the four to blank even when local properties hold values, so a public APK never carries a seed or token (007-repo-connect: RC-19).
- The template repository comes from the committed root property `template.repo`, set once for both build types; an empty value means no entry, characters outside letters, digits, dot, underscore, slash and hyphen turn it into blank without breaking the build, and the shape check itself is in `domain` (007-repo-connect: RC-26).
- The title font name comes from `-Ptitle.font` in debug builds only (letters, digits and spaces), to produce the not-received case; release always uses the official name (010-brand: BR-8).
- Only arm64-v8a native libraries ship, which cuts the APK from about 85 MB to about 61 MB; an x86_64 emulator gets an APK without the engine library, so the quiz cannot run there.
- The quiz model is not in the APK; it lives in the app's external files folder and is downloaded at run time.
- Release builds shrink and obfuscate with the default optimised rules plus `proguard-rules.pro`; keep rules are added only for what R8 reports missing, and the file keeps source and line attributes and the LiteRT-LM classes (their native code looks members up by name and aborts in release otherwise).
- Release signing is not set in this module; the build convention signs only when all four keystore properties are present in the local properties.
- The manifest declares the launcher `MainActivity` (no recreation on rotation, split screen or keyboard change; resize for the soft keyboard), the exported `ShareReceiverActivity`, and no receiver (013-local-notes: LN-8).
- The manifest declares two optional native libraries, `libvndksupport.so` and `libOpenCL.so`, that the engine's GPU backend needs; without them the engine falls back to the CPU without any error.
- The manifest removes WorkManager's own initializer; `MyNoteApplication` supplies the configuration.
- Cloud backup and device transfer both exclude the two token stores, the model file with its working and plan files, and the whole external files area; nothing else is excluded, so notes, bookmarks and study records are backed up (013-local-notes: LN-15).
- Rules use only exclusions: a single include would drop everything else, and the model in a cloud backup would pass the per-app limit and silently stop the whole backup. Renaming a token store or the model file means renaming it in the rules; the old-style rules file is a copy.
- The launcher icon is adaptive only, three layers (background, foreground, a separate monochrome silhouette), the same for round; there are no per-density bitmaps and no default robot icon (010-brand: BR-1, BR-2).
- The splash icon is an animated drawing whose default state is its end picture; the first 166 ms hold the dark window with same-value keyframes, then the light-up takes 450 ms, 616 ms in all, equal to the theme's declared length; the animators are linear (010-brand: BR-5).
- The window background colour equals the background token, so the first frame after the splash has no white frame (010-brand: BR-4).
- This module sets no resource prefix; its own strings start with `shell_`, because a prefix would rename launcher, theme and backup resources (012-feature-modules: FM-11).
- Connected tests do not uninstall the app: a root Gradle property keeps the APKs after the run, because uninstalling would delete the 2.6 GB model from the external files folder. Only the test package stays installed.
- Instrumented tests of this module run with the real application alive (Hilt, token sync, workers) even when they draw without Hilt ViewModels.
- The statistics evaluation harness runs only with an `evalRound` argument and a model file present. It runs 10 weekly-summary cases and 6 wrong-answer pattern cases on the real model and writes `eval-round<N>.json` under the external files `eval` folder.
- The harness first cancels the daily insight work so the worker cannot open a second engine; it writes the JSON before asserting, and fails below 9 of 10 summaries, below 5 of 6 patterns or with a call p95 over 20 s. `calibrate=true` adds three short-budget calibration runs.
### Public API
- None — build values are read through `BuildConfig` by `di`, and nothing in this unit is a code API.
### Guarantees
- `notes-tests/test_backup_rules.py` proves the notes database is not excluded and the token files and model file stay excluded (013-local-notes: LN-15).
- `notes-tests/test_brand_resources.py` proves the launcher layers, the missing default icon, the window background colour and the splash length against the brand drawings and tokens (010-brand: BR-1, BR-2, BR-4, BR-5).
- `notes-tests/test_feature_modules.py` proves the shell lives in `:app` with its order fixed in one file and that shell strings start with `shell_` (012-feature-modules: FM-5, FM-11).
### Not covered
- The shell and its back order — `app:shell`; the splash exit and font gate — `app:launch`.
- Signing, SDK levels and version numbers — the build convention in `build-logic` and the version catalogue.
- Statistics generation itself — `ai` module; the harness only measures it.
