# `:build-logic`

모든 Gradle convention plugin 을 담는 composite build. 각 모듈의 `build.gradle.kts` 는
alias 한 줄씩만 적고 실제 설정은 전부 여기 모은다.

## 구조

```
build-logic/
  settings.gradle.kts                 ← google/mavenCentral + gradlePluginPortal(ktlint-gradle 용)
  convention/
    build.gradle.kts                  ← 플러그인 등록 + 클래스패스 의존성
    src/main/java/
      AndroidApplicationConventionPlugin.kt
      AndroidApplicationComposeConventionPlugin.kt
      AndroidLibraryConventionPlugin.kt
      AndroidLibraryComposeConventionPlugin.kt
      AndroidFeatureConventionPlugin.kt
      AndroidRoomConventionPlugin.kt
      HiltConventionPlugin.kt
      KtlintConventionPlugin.kt
      com/hance/convention/
        KotlinAndroid.kt              ← 공통 Kotlin/Android 설정(compileSdk·minSdk·JVM 17·desugar)
        AndroidCompose.kt             ← 공통 Compose 설정(BOM·material3·compose-lints)
        SigningConfig.kt              ← local.properties 기반 release 서명(키 없으면 조용히 skip)
        ProjectExtensions.kt          ← `Project.libs` 버전 카탈로그 접근자
```

## Convention Plugin

| Plugin ID | 적용 대상 | 하는 일 |
|---|---|---|
| `hance.android.application` | `:app` | AGP application · 공통 Kotlin/Android · `targetSdk` · release 서명 · **`hance.ktlint` 자동 적용** |
| `hance.android.application.compose` | `:app` | `hance.android.application` + Compose 컴파일러 + 공통 Compose 의존성 |
| `hance.android.library` | 모든 core 라이브러리 | AGP library · 공통 Kotlin/Android · 계측 러너 · **`hance.ktlint` 자동 적용** |
| `hance.android.library.compose` | Compose 쓰는 라이브러리 | `hance.android.library` + Compose 컴파일러 + 공통 Compose 의존성 |
| `hance.android.feature` | `:feature:*` | `library.compose` + `:core:common`·`:core:design-system`·`:core:domain` + lifecycle · coroutines |
| `hance.android.hilt` | Hilt 쓰는 모듈 | KSP · Hilt 플러그인 · `hilt-android` + 컴파일러 |
| `hance.android.room` | `:core:database` | `androidx.room3` · KSP · 스키마 export 디렉터리 · room 런타임/컴파일러 |
| `hance.ktlint` | (자동) | ktlint 버전 고정 + `ktlintRuleset(compose-rules)` |

> `hance.ktlint` 는 application/library convention 이 직접 적용하므로 **모듈에서 따로 선언하지 않는다.**
> 그래서 `libs.versions.toml` 에도 alias 가 없다(문자열 id 로만 적용).

## 공통 설정이 실제로 켜는 것

**`KotlinAndroid.configureKotlinAndroid()`** — `compileSdk`/`minSdk`(카탈로그) · JVM 17 ·
core library desugaring · `testOptions.unitTests.isIncludeAndroidResources` ·
`failOnNoDiscoveredTests = false`(테스트 없는 모듈이 `test` 태스크를 실패시키지 않도록).

**`configureKotlin()`** — `jvmTarget = 17`, `allWarningsAsErrors` 를 `warningsAsErrors` Gradle 속성으로 제어.

**`AndroidCompose.configureAndroidCompose()`** — `buildFeatures.compose` · Compose BOM ·
`activity-compose`/`material3`/`ui`/`ui-tooling-preview`/`kotlinx-immutable` ·
디버그 전용 tooling · **Slack compose-lints(`lintChecks`)** · `ComposeCompositionLocalUsage` 비활성.

## 룰

- **버전은 `/gradle/libs.versions.toml` 에만.** 여기엔 하드코딩하지 않는다. 접근은 `Project.libs`(`ProjectExtensions.kt`).
- 새 convention plugin 추가 시: 클래스 작성 → `convention/build.gradle.kts` 의 `gradlePlugin { plugins { } }` 에 등록
  → 모듈이 직접 적용할 플러그인이면 `libs.versions.toml` `[plugins]` 에 alias 추가(다른 convention 이 적용할 거면 불필요).
- JVM 타깃은 17 고정.
- `validatePlugins { enableStricterValidation = true; failOnWarning = true }` 가 켜져 있어 strict 검증을 통과해야 한다.

### AGP 9 주의사항

- **`org.jetbrains.kotlin.android` 를 적용하지 않는다.** AGP 9 는 Kotlin 이 빌트인이고, 이 플러그인은 새 DSL 과 비호환이다.
- `CommonExtension` 은 **타입 파라미터가 없어졌다**(`CommonExtension<*,*,*,*,*,*>` → `CommonExtension`).
- `CommonExtension` 의 **블록 메서드는 사라졌지만 getter 는 남아 있다.** Application/Library 를 한 번에 설정할 땐 getter 로 접근한다:
  ```kotlin
  commonExtension.defaultConfig.minSdk = …        // O
  commonExtension.defaultConfig { minSdk = … }    // X — 블록 메서드는 구체 extension 에만
  ```
- 제거된 API(`applicationVariants`·`variantFilter`·`sdkDirectory` 등)는 `androidComponents` 로 대체한다.
- Gradle 10 에서 제거되는 `val x: String? by project` 위임 대신 `providers.gradleProperty("x")` 를 쓴다.

> 공식 마이그레이션 지침은 Android CLI 의 `agp-9-upgrade` 스킬(`android skills add agp-9-upgrade`) 참조.

## 없는 것 (과거에 있었으나 제거됨)

Circuit convention · JVM library convention · android test(benchmark) convention · product flavor(prod/demo).
관련 카탈로그 항목도 함께 정리됐으니 되살릴 땐 alias 부터 다시 추가해야 한다.
