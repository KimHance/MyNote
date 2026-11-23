# Decisions

### BR-D13 · 2026-10 · BR-5
- decision: The splash animation holds the dark window up to the platform cap of 166 ms and then lights it; the total is 616 ms (at most 1000 ms; the value's source is the design document).
- why: On a cold start the system's window-opening transition almost covered the dark window, so the lighting could not be seen.
- rejected: Lighting the window at the start of the animation with no hold.
- evidence: `app/src/main/res/values/themes.xml` (animation duration 616); `notes-tests/test_brand_resources.py` (`BR-5 스플래시 움직임은 1000ms 이하이고 테마 길이와 같다`)

### FM-D10 · 2026-10 · app:build, FM-13
- decision: The title-font instrumented test (BR-8) moves into one `:app` instrumented test class, and the entry point of the stats screen it draws is made public.
- why: Splitting the test per feature would break its (class, name) pairs, which FM-13 keeps.
- rejected: Splitting the test across feature modules.
- evidence: `app/src/androidTest/java/com/hancekim/mynote/brand/TitleFontScreensTest.kt`.

### FM-D16 · 2026-10 · FM-11
- decision: `:app` has no `resourcePrefix`; shell strings start with `shell_`, checked by a Python test.
- why: Lint would enforce a prefix on `:app` too, but then 17 existing resources (app name, theme, launcher, backup rules, brand art) would have to be renamed.
- rejected: A `resourcePrefix` on `:app`.
- evidence: `notes-tests/test_feature_modules.py` (`FM-11 셸 문구는 shell_ 로 시작한다`), `app/build.gradle.kts`.

### FM-D23 · 2026-10 · FM-11
- decision: The common action strings `ds_action_confirm` and `ds_action_retry`, not yet used by any screen, are not deleted; they are the only exception to "no unused string" and are named with the reason in the test's kept list.
- why: `DESIGN.md`'s common wording table defines them as the common action vocabulary; when a screen starts to use one, the test tells to remove it from the list.
- rejected: Deleting the unused strings.
- evidence: `notes-tests/test_feature_modules.py` (`UNUSED_KEPT`), `DESIGN.md`.

### DEC-18 · 2026-10 · RC-26
- decision: The template repository address is one build value, template.repo, read from the committed root gradle.properties (also overridable with -P or a user gradle.properties). Empty means no entry to create a repository is shown.
- why: local.properties differs per clone and does not reach release builds; the entry must not exist before the template repository is public.
- rejected: local.properties as the source.
- evidence: app/build.gradle.kts reads gradleProperty("template.repo"); gradle.properties sets it today (the template is still private, so the create screen 404s for non-owners).
