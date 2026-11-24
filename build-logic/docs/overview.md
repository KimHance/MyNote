---
id: build-logic
name: Build logic
aliases: [빌드 로직, 컨벤션 플러그인, hancenote 플러그인, convention plugins]
uses: []
units:
  - {id: plugins, name: Convention plugins, aliases: [컨벤션 플러그인, hancenote.android.library, hancenote.android.feature, feature 모듈 의존, 모듈 의존 자동 추가, 새 모듈 만들기], code: [convention/src/main/java/]}
  - {id: shared-config, name: Shared module setup, aliases: [공통 설정, 컴파일 SDK, Compose 설정, 버전 카탈로그 접근자], code: [convention/src/main/java/com/hancenote/convention/KotlinAndroid.kt, convention/src/main/java/com/hancenote/convention/AndroidCompose.kt, convention/src/main/java/com/hancenote/convention/ProjectExtensions.kt]}
  - {id: signing, name: Release signing, aliases: [릴리스 서명, 키스토어], code: [convention/src/main/java/com/hancenote/convention/SigningConfig.kt]}
  - {id: test-inputs, name: Repo folders as test inputs, aliases: [레포 폴더 시험 입력, 시험 입력], code: [convention/src/main/java/com/hancenote/convention/RepoTestInputs.kt]}
  - {id: ktlint, name: Ktlint convention, aliases: [ktlint, 포맷 검사, 코드 스타일], code: [convention/src/main/java/KtlintConventionPlugin.kt]}
---
## Overview
Build logic is a separate composite build holding one Gradle project, `convention`, that registers the nine `hancenote.*` convention plugins. A module's `build.gradle.kts` names one plugin and its namespace; SDK levels, Java 17, desugaring, ktlint, Compose, Hilt and the feature wiring come from the plugin.
It owns how modules are configured, not what they contain. Versions and coordinates are not here: they live in the root version catalog, which this build reads from the same file.
It has no Kotlin tests of its own; the plugins are exercised by every module build, and module-boundary rules that read this source are tested in the Python suite.
## Structure
- `plugins` is the entry: each plugin class applies the others it builds on and calls the helpers.
- `shared-config` is called by the application and library plugins (Android and Kotlin settings) and by the Compose plugins; the JVM plugin calls only its Kotlin part.
- `signing` is called by the application plugin only; `test-inputs` by the library and JVM library plugins.
- `ktlint` is applied by the application, library and JVM library plugins, never by a module directly.
- The feature plugin is the one place that names other project modules, as strings, because typesafe project accessors do not exist inside a plugin.
```mermaid
flowchart LR
  plugins --> shared-config
  plugins --> signing
  plugins --> test-inputs
  plugins --> ktlint
```
