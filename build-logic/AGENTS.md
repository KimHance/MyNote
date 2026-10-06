# AGENTS.md — build-logic

이 파일은 빌드 설정을 건드리는 에이전트를 위한 안내다.

---

## 이게 뭔가

`build-logic` 은 **별도의 composite build** 다. 루트 `settings.gradle.kts` 의
`pluginManagement { includeBuild("build-logic") }` 로 끼워지고, 안에는 `:convention`
서브프로젝트 하나만 있다. 그 프로젝트가 Gradle 컨벤션 플러그인 **9개**를 만들어 낸다.

효과는 **모듈 `build.gradle.kts` 가 거의 비어 있다**는 것이다. `core/common/build.gradle.kts` 전부:

```kotlin
plugins { alias(libs.plugins.hancenote.android.library) }
android { namespace = "com.hancekim.mynote.core.common" }
dependencies {
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`compileSdk`·`minSdk`·JVM 17·desugaring·ktlint·테스트 옵션이 한 줄도 없다. 전부
`hancenote.android.library` 안에 있다. `feature/notes/build.gradle.kts` 는 더 짧다 —
`hancenote.android.feature` 한 줄이 Compose·Hilt·KSP 와 `:core:{common,design-system,domain,navigation}`
+ lifecycle + coroutines 까지 다 끌고 온다.

> **모듈 파일에 설정을 추가하려는 충동이 들면 먼저 여기 컨벤션에 있는지 확인한다.**
> 두 벌로 선언하면 버전 올릴 때 한쪽만 고쳐질 자리가 생긴다 — 커밋 `22815da` 가
> `design-system` 의 `ui-tooling-preview` 와 `:app` 의 `activity-compose` 중복을 그래서 지웠다.

플러그인 소스는 `build-logic/convention/src/main/java/` 에 있다(디렉터리 이름이 `java` 지만
내용은 Kotlin 이다). 고치면 **다음 Gradle 호출에서 `:build-logic:convention:compileKotlin`
이 먼저 돌고 곧바로 쓰인다** — 따로 publish 하는 단계는 없다.

## 자주 쓰는 명령

전부 **리포 루트**에서 실행한다. `build-logic` 안으로 `cd` 하지 않고 태스크 경로에
`:build-logic:` 접두사를 붙인다.

| 목적 | 명령 |
|---|---|
| 컨벤션 코드 자체 검증 | `./gradlew :build-logic:convention:validatePlugins` |
| 가장 빠른 전체 검증 | `./gradlew :app:compileDebugKotlin` |
| 유닛 테스트(Android 모듈) | `./gradlew testDebugUnitTest` |
| JVM 모듈까지 포함 | `./gradlew test testDebugUnitTest` |
| 단일 테스트 클래스 | `./gradlew :core:common:testDebugUnitTest --tests '*ThrottlerTest'` |
| 단일 테스트 메서드(한글 이름 그대로 됨) | `./gradlew :core:common:testDebugUnitTest --tests '*ThrottlerTest.첫 호출은 통과한다'` |
| 포맷 검사 / 자동 교정 | `./gradlew ktlintCheck` · `./gradlew ktlintFormat` |
| 한 모듈·한 소스셋만 | `./gradlew :core:design-system:ktlintMainSourceSetCheck` |
| Android Lint | `./gradlew lintDebug` → `<module>/build/reports/lint-results-debug.txt` |
| APK | `./gradlew :app:assembleDebug` · `./gradlew :app:assembleRelease` |
| Compose 안정성 리포트 | `./gradlew :core:design-system:compileDebugKotlin -PcomposeMetrics` |
| 경고를 에러로 | `./gradlew <task> -PwarningsAsErrors=true` |
| 캐시가 의심되면 | `--no-build-cache` / `--no-configuration-cache` |

**`./gradlew testDebugUnitTest` 는 `:core:domain` 을 테스트하지 않는다.** 그 모듈은 순수 JVM
이라 테스트 태스크 이름이 `test` 다(`:core:domain:testDebugUnitTest` 는 **존재하지 않는다** —
실행하면 태스크를 못 찾아 실패한다). 지금은 domain 에 테스트가 없어서 티가 안 나지만
넣는 순간 조용히 빠진다. 그래서 위 표에 `test testDebugUnitTest` 를 같이 적어 뒀다.

`--tests` 에 오타를 내면 `No tests found for given includes: [...]` 로 **실패**한다
(뒤에 나오는 `failOnNoDiscoveredTests = false` 가 이 경우까지 삼키지는 않는다).

## 컨벤션 플러그인 지도

id 는 전부 `hancenote.*`, 카탈로그 alias 는 `hancenote-*` 다. 커밋 `c982101` 이전에는
alias(`billboard-*`)·id(`hance.*`)·group·패키지가 네 갈래로 갈려 있었다. 지금은
**모듈 파일의 alias 를 그대로 grep 하면 구현 클래스가 나온다.**

| id | 적용 대상 | 스스로 적용/추가하는 것 |
|---|---|---|
| `hancenote.android.application` | `:app` | `com.android.application` · **`hancenote.ktlint`** · 공통 Kotlin/Android · `targetSdk` · `dependenciesInfo` 끄기 · release 서명 |
| `hancenote.android.application.compose` | `:app` | ↑ **+** `org.jetbrains.kotlin.plugin.compose` · 공통 Compose 설정 |
| `hancenote.android.library` | core 라이브러리 | `com.android.library` · **`hancenote.ktlint`** · 공통 Kotlin/Android · 계측 러너 |
| `hancenote.android.library.compose` | Compose 쓰는 라이브러리 | ↑ **+** Compose 컴파일러 · 공통 Compose 설정 |
| `hancenote.android.feature` | `:feature:*` | `library.compose` **+ `android.hilt`** · `:core:{common,design-system,domain,navigation}` · lifecycle(runtime/runtimeCompose/viewModelCompose) · `kotlinx-coroutines-android` |
| `hancenote.android.hilt` | Hilt 쓰는 모듈 | `com.google.devtools.ksp` · `com.google.dagger.hilt.android` · `hilt-android` + `ksp(hilt-android-compiler)` |
| `hancenote.android.room` | `:core:database` | `androidx.room3` · KSP · `schemaDirectory("$projectDir/schemas")` · room3 runtime + compiler |
| `hancenote.jvm.library` | `:core:domain` · `:core:format` | `org.jetbrains.kotlin.jvm` · **`hancenote.ktlint`** · Java 17 · `useJUnit()` |
| `hancenote.ktlint` | (다른 컨벤션이 자동 적용) | `org.jlleitschuh.gradle.ktlint` · ktlint 버전 고정 · `ktlintRuleset(compose-rules)` |

두 가지 주의:

- **`hancenote.ktlint` 는 `libs.versions.toml` `[plugins]` 에 alias 가 없다.** 다른 컨벤션이
  문자열 id 로만 적용하기 때문이다. 모듈에서 직접 선언하지 않는다.
- `*.compose` 컨벤션은 자기 짝(`application`/`library`)을 스스로 적용한다. 그래서
  `app/build.gradle.kts` 가 `hancenote.android.application` 과
  `hancenote.android.application.compose` 를 **둘 다** 적은 건 중복이다(해롭진 않다).

### 모듈 → 어떤 컨벤션을 쓰는가

| 모듈 | 컨벤션 | 추가로 선언한 것 |
|---|---|---|
| `:app` | `android.application` + `.compose` + `android.hilt` | `buildConfig = true`, `ndk.abiFilters = arm64-v8a`, release R8/shrink |
| `:core:common` | `android.library` | — |
| `:core:ai` | `android.library` + `android.hilt` + `kotlin-serialization` | — |
| `:core:data` | `android.library` + `android.hilt` + `kotlin-serialization` | — |
| `:core:database` | `android.library` + `android.room` + `android.hilt` | — |
| `:core:network` | `android.library` + `android.hilt` + `kotlin-serialization` | — |
| `:core:design-foundation` | `android.library.compose` | — |
| `:core:design-system` | `android.library.compose` | `resourcePrefix = "ds_"` |
| `:core:navigation` | `android.library.compose` + `kotlin-serialization` | — |
| `:core:domain` | **`jvm.library`** | — |
| `:core:format` | **`jvm.library`** + `kotlin-serialization` | — |
| `:feature:notes` | **`android.feature`** (Compose·Hilt 포함) | `resourcePrefix = "notes_"` |

## 헬퍼가 실제로 켜는 것

`build-logic/convention/src/main/java/com/hancenote/convention/` 에 다섯 파일뿐이다(`RepoTestInputs.kt` 는 아래 "레포 폴더 시험 입력" 절).

**`KotlinAndroid.kt` → `configureKotlinAndroid()`** (application/library 공통)
`compileSdk`·`minSdk`(카탈로그에서) · Java 17 source/target · **core library desugaring**
(`isCoreLibraryDesugaringEnabled` + `coreLibraryDesugaring(desugar_jdk_libs)`) ·
`testOptions.unitTests.isIncludeAndroidResources = true` · `failOnNoDiscoveredTests = false`.

> `failOnNoDiscoveredTests = false` 가 없으면 **테스트가 아예 없는 모듈**(`:core:database`,
> `:core:network`)의 `testDebugUnitTest` 가 Gradle 9 에서 실패한다. `jvm.library` 에도 같은 줄이 있다.

**`configureKotlin()`** (Android/JVM 공통) — `jvmTarget = JVM_17`,
`allWarningsAsErrors` 를 `warningsAsErrors` Gradle 속성으로 제어. **툴체인 선언은 없다** —
Gradle 을 돌리는 JDK 로 컴파일하고 타깃만 17 로 맞춘다.

**`AndroidCompose.kt` → `configureAndroidCompose()`**
`buildFeatures.compose = true` · Compose BOM(`implementation` + `androidTestImplementation`) ·
`activity-compose`/`material3`/`ui`/`ui-tooling-preview`/`kotlinx-immutable` ·
`debugImplementation` 으로 `ui-tooling`+`ui-test-manifest` · `androidTestImplementation(ui-test-junit4)` ·
**`lintChecks(compose-lint-checks)`**(Slack) · `lint.disable += "ComposeCompositionLocalUsage"` ·
`includeSourceInformation = true` · `composeMetrics` 속성이 있을 때만 리포트 출력.

> `commonExtension.buildFeatures.compose = true` 는 **한 줄이어야 한다.** 예전에는 실제 확장
> 타입으로 좁히는 `when` 이 있었는데, Application/Library 가 아닌 확장이 들어오면 어느 분기도
> 안 타고 Compose 가 꺼진 채 빌드돼 `@Composable` 을 못 찾는 엉뚱한 에러가 났다(`32aac30`).

**`ProjectExtensions.kt`** — `Project.libs` 버전 카탈로그 접근자. 컨벤션 안에서 버전/좌표를
읽는 유일한 경로다(`libs.findVersion("minSdk")`, `libs.findLibrary("hilt.android")`).

**`SigningConfig.kt`** — 아래 별도 절.

## Gradle 속성 스위치 — 값을 줘야 하는 것과 아닌 것

같은 "속성으로 켜는 기능"인데 판정 방식이 다르다. 헷갈리면 조용히 안 켜진다.

| 속성 | 판정 | 켜는 법 |
|---|---|---|
| `composeMetrics` | `providers.gradleProperty(...).isPresent` | `-PcomposeMetrics` (값 불필요) |
| `warningsAsErrors` | `.orNull.toBoolean()` | **`-PwarningsAsErrors=true`** — `-PwarningsAsErrors` 만 주면 빈 문자열 → `false` |

실측: `-PwarningsAsErrors` 단독은 통과, `-PwarningsAsErrors=true` 는
`e: warnings found and -Werror specified` 로 실패한다.

**지금 `-PwarningsAsErrors=true` 는 전체가 통과하지 않는다.** `:core:common` 테스트 소스에
`ExperimentalCoroutinesApi` opt-in 경고 3건이 있다. CI 에 걸려면 먼저 그걸 정리해야 한다.

`-PcomposeMetrics` 를 주면 `<module>/build/compose-reports/` 에
`*-composables.txt`/`*-composables.csv`/`*-classes.txt` 가 생긴다. **항상 켜 두지 않는다** —
모든 빌드가 안정성 보고서를 쓰느라 느려진다(`877a67e`).

## 레포 폴더 시험 입력 — 속성으로 켠 모듈만

`hancenote.android.library`·`hancenote.jvm.library` 가 `configureRepoTestInputs()` 를 부른다. 모듈 `build.gradle.kts` 가
`extra["<속성>"] = true` 를 **plugins 블록 아래 아무 데나** 한 줄 두면(`afterEvaluate` 에서 읽는다) 그 모듈의 모든 `Test` 태스크가
아래 루트 폴더를 상대 경로 입력으로 안다 — 폴더의 한 글자만 바뀌어도 시험이 UP-TO-DATE 로 건너뛰지 않고 다시 돈다.

| 속성 | 폴더 | 켠 모듈 |
|---|---|---|
| `notesTemplateInput` | `public-repos/notes-template` | `:core:data` |
| `notesInput` | `notes` · `notes-tests/fixtures/front-matter` | `:core:format` |

**요청한 모듈만 켠다**(결정 15 · D18): 모든 모듈에 걸면 폴더 한 글자에 전 모듈 시험이 돈다. 폴더가 없어도 실패하지 않는다(빈 입력).
폴더를 더하려면 `RepoTestInputs.kt` 의 표에 한 줄을 더한다.

## 캐시 (루트 `gradle.properties`)

```properties
org.gradle.caching=true
org.gradle.configuration-cache=true
```

- **빌드 캐시** — 태스크 출력을 `~/.gradle/caches/build-cache-1` 에 입력 해시로 저장한다.
  `build/` 가 날아가도 입력이 같으면 다시 안 돌린다. 커밋 `aff86c7` 실측(clean 후
  `ktlintCheck + assembleDebug + testDebugUnitTest + lintDebug`): **37.9초 → 1.8초**, 334개 태스크가
  캐시에서 복원됐다. clean 직후와 브랜치를 갈아탔다 돌아올 때가 제일 크다.
- **설정 캐시** — 모듈 11개라 절약 자체는 작다(0.4초 → 0.1초). 켜 둔 값은 성능보다
  **지금 통과한다는 사실을 고정하는 것**이다. 설정 단계에서 `Project` 를 붙잡는 코드를
  컨벤션에 넣으면 그 커밋에서 바로 빌드가 실패한다. 나중에 켜려면 원인이 쌓인 뒤라 찾기 어렵다.
  → **컨벤션을 고칠 때 설정 단계에서 `Project`/`Task` 를 캡처하지 않는다.**
- 이상하면 `--no-build-cache` / `--no-configuration-cache` 로 끄고 비교한다.

`build-logic/gradle.properties` 는 따로 `parallel`·`caching`·`configureondemand` 를 켠다.

## `dependenciesInfo` — 끈다

application 컨벤션이 `dependenciesInfo.includeInApk = false` · `includeInBundle = false` 를 넣는다. AGP 는 release 빌드에서
의존성 목록을 Google Play 키로 암호화해 **APK 서명 블록**과 AAB 에 싣는데, F-Droid 는 그 블록을 알 수 없는 바이너리로 보고
거부하고 배포는 GitHub Releases APK 라 읽을 주체도 없다. 블록 메서드(`dependenciesInfo { }`) 대신 getter + 대입으로 쓴다
(AGP 9 주의사항과 같은 모양).

확인은 태스크 그래프로 한다 — `./gradlew :app:assembleRelease :app:bundleRelease --dry-run` 에
`collectReleaseDependencies`·`sdkReleaseDependencyData`·`configureReleaseDependencies` 가 **없어야** 한다(끄기 전에는 셋 다 있다).
APK 로는 확인할 수 없다: debug 빌드는 원래 이 블록을 만들지 않고, 서명 키가 없는 release 는 `-unsigned.apk` 라 서명 블록 자체가 없다.

## release 서명 — 침묵과 경고를 구분한다

`SigningConfig.kt` 는 루트 `local.properties` 에서 `keystore.path`, `keystore.password`,
`key.alias`, `key.password` **4개**를 읽는다(파일은 gitignore).

| 상황 | 동작 |
|---|---|
| 4개 다 없음 | **조용히** 서명 없이 진행 (키를 안 가진 개발자 — 의도된 침묵) |
| 일부만 있음 | `release 서명 건너뜀 — local.properties 에 <빠진 키> 없음` 경고 |
| 경로는 있는데 파일이 없음 | `release 서명 건너뜀 — 키스토어 파일 없음: <경로>` 경고 |
| 4개 + 파일 정상 | `signingConfigs["release"]` 생성 후 release 빌드타입에 연결 |

값이 **비어 있는 것도 없는 것으로** 센다. 이 구분이 왜 필요한가: `key.alias` 를 `keyAlias`
로 잘못 쓰면 예전에는 `BUILD SUCCESSFUL` 로 끝나고 서명 없는 APK 가 나왔다. 그걸 아는 건
설치/업로드 순간(`INSTALL_PARSE_FAILED_NO_CERTIFICATES`)이고, 그때는 빌드가 성공했다고
말했기 때문에 Gradle 설정을 의심하지 않는다(`3aeb34b`).

서명이 안 붙으면 산출물 이름이 `app/build/outputs/apk/release/app-release-unsigned.apk` 가
된다 — **파일명으로 확인하는 게 제일 빠르다.**

## `resourcePrefix` — 무엇이 강제하는가

`:core:design-system`(`ds_`)과 `:feature:notes`(`notes_`)에만 걸려 있다. 리소스가 있는
라이브러리 모듈이 그 둘뿐이다(각각 `res/values/strings.xml`). 컨벤션이 자동으로 걸지 않는다 —
**리소스를 갖는 라이브러리 모듈을 새로 만들면 직접 적어 준다.**

이유: 라이브러리 리소스는 이름이 겹치면 조용히 덮인다(`android.nonTransitiveRClass=true` 로
R 클래스는 분리돼 있지만 병합된 리소스 이름 공간은 하나다). 접두사를 걸면 어느 모듈이 준
리소스인지 이름만 봐도 안다.

**강제하는 주체는 Android Lint 다 — 컴파일이 아니다.** 실측: 접두사 없는 `<string>` 을 넣고
`:core:design-system:assembleDebug` → **통과**. `:core:design-system:lintDebug` → 실패:

```
Error: Resource named 'zz_probe' does not start with the project's resource prefix 'ds_';
rename to 'ds_zz_probe' ? [ResourceName]
```

즉 리소스를 추가했으면 `assemble` 만으로는 모른다. `lintDebug` 를 돌려야 한다.

## ktlint 와 `.editorconfig`

`hancenote.ktlint` 가 하는 일은 두 줄뿐이다 — ktlint 버전을 카탈로그(`ktlint = "1.8.0"`)로
고정하고 `ktlintRuleset` 에 `io.nlopez.compose.rules:ktlint`(0.6.7)를 넣는다. Gradle 플러그인은
`org.jlleitschuh.gradle.ktlint` 14.2.0.

규칙 설정은 전부 **리포 루트 `.editorconfig`** 다. 끈 것과 이유:

| 설정 | 왜 |
|---|---|
| `ktlint_function_naming_ignore_when_annotated_with = Composable` | `@Composable` 은 PascalCase 가 정상 |
| `ktlint_standard_function-signature = disabled` | 120자에 들어가면 시그니처를 한 줄로 합쳐 버려 선언이 읽기 나빠진다 |
| `ktlint_standard_class-signature = disabled` + `ktlint_standard_annotation = disabled` | `class Foo @Inject constructor(...)` 를 한 줄로 유지하려면 **둘 다** 꺼야 한다 |
| `ktlint_compose_compositionlocal-allowlist = disabled` | 테마 토큰 전파(`MyNoteTheme.colors` 등)는 CompositionLocal 의 정당한 용례. Android Lint 쪽 같은 검사(`ComposeCompositionLocalUsage`)는 `AndroidCompose.kt` 에서 끈다 |

### 안 쓰는 import 는 아무도 안 잡는다 (실측)

이건 모르면 계속 쌓인다. 커밋 `5a92d9e` 가 14개를 손으로 지웠다.

정확한 사정은 이렇다 — ktlint 1.8.0 표준 룰셋에 `NoUnusedImportsRule`(`no-unused-imports`)은
**있다**. 다만 그 클래스가 `Rule.OnlyWhenEnabledInEditorconfig` 를 구현해서
**`.editorconfig` 에서 명시로 켜지 않으면 돌지 않는다**. (커밋 메시지의 "표준 룰셋에 없다"는
설명은 부정확하다. 관측 — 아무도 안 잡는다 — 은 맞다.)

실측 3건:

1. 안 쓰는 import 를 넣고 `./gradlew :core:common:ktlintMainSourceSetCheck` → **통과**.
2. 같은 상태로 `./gradlew :core:common:compileDebugKotlin --rerun-tasks` → **경고조차 없다**.
3. `.editorconfig` 의 **`[*.{kt,kts}]` 섹션 안**에 `ktlint_standard_no-unused-imports = enabled`
   를 넣으면 → `Unused import` 로 잡힌다.

> **함정:** 3번을 파일 **맨 아래**에 붙이면 `[*.{md,json,yml,yaml}]` 섹션에 들어가 조용히
> 무효다(이것도 실측). `.editorconfig` 에 Kotlin 규칙을 추가할 땐 섹션 위치를 확인한다.

정렬(`import-ordering`)은 잡는데 미사용은 통과한다. `by` 위임처럼 이름이 본문에 안 나타나는
import(`getValue`/`setValue`)는 켜도 오탐 위험이 있으니 지울 때 주의한다.

### pre-commit 훅

`.githooks/pre-commit` 이 **스테이징된 `.kt`/`.kts` 파일만** 검사한다
(`./gradlew ktlintCheck -PinternalKtlintGitFilter="<개행 구분 경로 목록>"`). 위반이면 커밋 중단.
노트를 건드렸으면 `notes/gen_index.py` 도 돌려 `index.json` 을 같은 커밋에 넣는다.
설치는 머신당 1회: `git config core.hooksPath .githooks`. `--no-verify` 는 쓰지 않는다.

## AGP 9 주의사항

- **`org.jetbrains.kotlin.android` 를 적용하지 않는다.** AGP 9 는 Kotlin 이 빌트인이다 —
  카탈로그 `[plugins]` 에 그 alias 자체가 없고, `hancenote.android.library` 는
  `com.android.library` + ktlint 만 적용하는데도 `compileDebugKotlin` 이 정상 동작한다.
  Kotlin 플러그인을 명시로 붙이는 곳은 **비Android 모듈뿐**이다(`jvm.library` → `org.jetbrains.kotlin.jvm`).
- **`CommonExtension` 은 타입 파라미터가 없어졌다**(`CommonExtension<*,*,*,*,*,*>` → `CommonExtension`).
  헬퍼 시그니처가 그래서 `configureKotlinAndroid(commonExtension: CommonExtension)` 이다.
- **블록 메서드는 사라졌고 getter 는 남아 있다.**
  ```kotlin
  commonExtension.defaultConfig.minSdk = 32      // O
  commonExtension.defaultConfig { minSdk = 32 }  // X — 블록은 구체 extension 에만
  ```
- Gradle 10 에서 제거되는 `val x: String? by project` 대신 `providers.gradleProperty("x")` 를 쓴다
  (컨벤션이 이미 그렇게 한다).
- 공식 마이그레이션 지침은 Android CLI 의 `agp-9-upgrade` 스킬.

## 순수 JVM 모듈 — `hancenote.jvm.library`

`:core:domain` 과 `:core:format`(+ `kotlin-serialization` 별칭)이 쓴다. Hilt 컨벤션은 Android 플러그인이라 붙지 않으므로
두 모듈은 `javax.inject` 생성자 주입만 쓴다. 왜 Android 라이브러리가 아닌가:

1. 매니페스트·R 클래스·리소스 머지·계측 러너가 전부 딸려오는데 **하나도 안 쓴다.**
2. 더 중요한 건 **`import android.*` 가 컴파일 에러로 막힌다**는 것이다. "domain 은 순수
   Kotlin" 이라는 아키텍처 룰이 문서가 아니라 **빌드로 강제된다.**

실측 확인(`android.util.Log` 를 `:core:domain` 에 넣고 `:core:domain:compileKotlin`):

```
e: .../ZzAndroidProbe.kt:3:8 Unresolved reference 'android'.
```

그래서 domain 은 코루틴도 `kotlinx-coroutines-core`(안드로이드 디스패처 없는 쪽)를 쓴다.
`:core:domain` 에 안드로이드 타입이 필요해졌다면 **컨벤션을 바꾸는 게 아니라 설계가 틀린 것**이다.

## 버전과 좌표는 카탈로그에만

`/gradle/libs.versions.toml` 하나가 전부다. `build-logic/settings.gradle.kts` 가
`from(files("../gradle/libs.versions.toml"))` 로 **같은 파일**을 읽으므로, Gradle 플러그인
클래스패스(AGP·KGP·KSP·Hilt·Room3·ktlint-gradle)의 버전도 거기 있다.

지금 지켜지고 있다(확인): `app`·`core/*`·`feature/*` 의 모든 `build.gradle.kts` 에
버전 문자열이나 `"group:name:version"` 형태의 하드코딩이 **0건**이다 — `libs.*` 와
`projects.*` 만 쓴다. 이 상태를 유지한다.

- 앱 버전도 카탈로그다: `appVersionCode`/`appVersionName`.
- 루트 `settings.gradle.kts` 는 `repositoriesMode = FAIL_ON_PROJECT_REPOS` 라
  **모듈에서 `repositories { }` 를 선언할 수 없다.** 저장소는 settings 두 곳(루트,
  `build-logic/settings.gradle.kts`)에만 있고, `build-logic` 쪽에 `gradlePluginPortal()` 이
  있는 건 `ktlint-gradle` 때문이다.
- `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` 덕에 모듈은 `projects.core.designFoundation`
  같은 접근자를 쓴다. **컨벤션 플러그인 안에서는 못 쓴다** — `AndroidFeatureConventionPlugin`
  이 `project(":core:common")` 처럼 문자열을 쓰는 이유다. 모듈 이름을 바꾸면 그 문자열도 같이 고친다.

## 바꾸는 절차

**새 컨벤션 플러그인 추가**
1. `build-logic/convention/src/main/java/XxxConventionPlugin.kt` 작성(패키지 없는 최상위 클래스).
2. `build-logic/convention/build.gradle.kts` 의 `gradlePlugin { plugins { register(...) } }` 에
   `id = "hancenote.xxx"` 로 등록.
3. **모듈이 직접 적용할 플러그인이면** `libs.versions.toml` `[plugins]` 에 `hancenote-xxx` alias 추가.
   다른 컨벤션이 문자열로 적용할 거면 불필요(`hancenote.ktlint` 가 그 경우).
4. 새 Gradle 플러그인에 의존하면 `convention/build.gradle.kts` `dependencies` 에 `libs.*-gradle-plugin` 추가.
5. `./gradlew :build-logic:convention:validatePlugins` → 그다음 `./gradlew :app:compileDebugKotlin`.

`validatePlugins` 는 `enableStricterValidation = true`, `failOnWarning = true` 다. 경고 하나도 못 넘어간다.

**버전 올리기** — `libs.versions.toml` 만 고치고 `./gradlew :app:compileDebugKotlin` +
`./gradlew testDebugUnitTest` + `./gradlew lintDebug`. 컨벤션 파일은 보통 손댈 필요가 없다.

## 지금 상태 (참고값)

- AGP 9.4.1 · Gradle 9.8.0(wrapper) · Kotlin 2.4.20 · KSP 2.3.12 · Hilt 2.60.1 · Room3 3.0.3
- `compileSdk` 37 · `targetSdk` 37 · `minSdk` 32 · Java/JVM 타깃 17(build-logic 자신도 17)
- 모듈 12개 = `:app` + core 10(`common`, `ai`, `data`, `database`, `design-foundation`,
  `design-system`, `domain`, `format`, `navigation`, `network`) + feature 1(`notes`)
- 테스트가 있는 모듈: `app` 을 뺀 전부(core 10 · `feature/notes`).
- 없는 것(과거에 있었으나 제거됨): Circuit convention · android test(benchmark) convention ·
  product flavor(prod/demo). 되살릴 땐 카탈로그 alias 부터 다시 추가해야 한다.

## 이 문서의 주장을 다시 확인하는 법

문장을 고칠 일이 생기면 추측하지 말고 돌려 본다.

```bash
# 컨벤션이 실제로 넣어 주는 의존/설정
./gradlew :core:design-system:dependencies --configuration debugCompileClasspath

# 어떤 플러그인 id 가 만들어지는지
ls build-logic/convention/build/pluginDescriptors/

# 모듈에 실제로 있는 태스크 이름
./gradlew :core:domain:tasks --all

# ktlint 규칙 하나가 기본으로 도는지 (안 쓰는 import 를 잠깐 넣어 보고 되돌린다)
./gradlew :core:common:ktlintMainSourceSetCheck
```
