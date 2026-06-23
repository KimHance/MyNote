# :build-logic Module

## Responsibility
Composite build containing all Gradle convention plugins. Centralizes build configuration so module `build.gradle.kts` files stay minimal and consistent.

## Structure
```
build-logic/
  convention/
    src/main/java/
      AndroidApplicationConventionPlugin.kt
      AndroidApplicationComposeConventionPlugin.kt
      AndroidLibraryConventionPlugin.kt
      AndroidLibraryComposeConventionPlugin.kt
      AndroidFeatureConventionPlugin.kt
      AndroidRoomConventionPlugin.kt
      AndroidCircuitConventionPlugin.kt
      AndroidTestConventionPlugin.kt
      HiltConventionPlugin.kt
      JvmLibraryConventionPlugin.kt
      com/hance/convention/
        KotlinAndroid.kt          ← shared Kotlin/Android config
        AndroidCompose.kt         ← shared Compose config
        Circuit.kt                ← Circuit KSP setup
        BillboardFlavor.kt        ← prod/demo flavor definitions
        ProjectExtensions.kt      ← libs accessor extension
```

## Convention Plugin Reference
| Plugin ID | Applied by | Adds |
|-----------|-----------|------|
| `hance.android.application` | `:app` | AGP application, Kotlin, desugar |
| `hance.android.application.compose` | `:app` | Compose compiler, tooling |
| `hance.android.library` | all library modules | AGP library, Kotlin, flavors |
| `hance.android.library.compose` | Compose libraries | Compose compiler |
| `hance.android.feature` | `:feature:*` | library + KSP + design-system + circuit + domain + lifecycle + coroutines + Timber |
| `hance.android.hilt` | any Hilt module | Hilt plugin, kapt/ksp, dependencies |
| `hance.android.room` | Room modules | Room plugin, schema export dir |
| `hance.circuit` | Circuit screens | KSP + parcelize + codegen arg `hilt` |
| `hance.jvm.library` | pure Kotlin modules | kotlin-jvm |
| `hance.android.test` | `:benchmark` | android.test plugin |

## Product Flavors
Defined in `BillboardFlavor.kt`:
- `prod` — production (default)
- `demo` — demo app, applicationIdSuffix `.demo`

`configureFlavors()` is called in `AndroidLibraryConventionPlugin` and `AndroidApplicationConventionPlugin` — all modules automatically have `prod`/`demo` variants.

## Rules
- All AGP/Kotlin/Compose/Hilt versions are defined in `/gradle/libs.versions.toml` — update versions there, not here.
- When adding a new convention plugin: register it in `build.gradle.kts` `gradlePlugin { plugins { } }` block, then add an alias in `libs.versions.toml` `[plugins]` section.
- Java/Kotlin target version is JVM 17 — do not change.
- `validatePlugins { enableStricterValidation = true; failOnWarning = true }` is enforced — all plugins must pass strict validation.
