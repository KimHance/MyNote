# MyNote — Claude Code Instructions

## Language Rules
- 대화 응답은 한국어.
- 코드 주석은 한국어.
- 문서/설정 파일(CLAUDE.md, 룰 등)은 한국어 허용(사용자 선호).

---

## Project Overview

GitHub raw JSON 을 읽어 "블로그 글처럼" 읽는 **읽기 전용** 안드로이드 학습 노트 앱.
앱에서 write 없음. 모든 콘텐츠는 원격 JSON(현재는 로컬 더미). 다크 단일 테마, 멀티 폼팩터(반응형).

**Package root:** `com.hancekim.mynote`
**SDK:** compileSdk 37 · targetSdk 36 · minSdk 32
**Base:** `Billboard 프로젝트` 의 Gradle/convention plugin 을 가져와 각색(Circuit·flavor 제거, Nav3·MVI 채택).

---

## Rules
@.claude/rules/01-architecture.md
@.claude/rules/02-compose-state.md
@.claude/rules/03-di-hilt.md
@.claude/rules/04-design-system.md
@.claude/rules/05-error-handling.md
@.claude/rules/06-testing.md

---

## Module Reference

| Module | Role |
|--------|------|
| `:app` | Application(Hilt), MainActivity, edge-to-edge, `NotesRoute` 호스트 |
| `:core:design-foundation` | 색/타이포/radius/spacing 토큰, `AdaptiveTokens`(5단계), 마크다운 파서 |
| `:core:design-system` | `MyNoteTheme`(토큰 CompositionLocal), 공용 컴포넌트, 마크다운 렌더 |
| `:core:data` | `Note`/`Category` 모델(순수 직렬화), `NotesRepository` + 더미 구현, Hilt 모듈 |
| `:core:domain` | `GetCategoriesUseCase` 등 UseCase. `api(:core:data)` |
| `:feature:notes` | 3화면(Home·Reader·ListPanel), `NotesViewModel`(MVI), Nav3 그래프 |

---

## Tech Stack

| Layer | Tech |
|---|---|
| Language / UI | Kotlin · Jetpack Compose · Material3 |
| Navigation | **Navigation 3** (`NavDisplay`) + **Material3 adaptive** (`ListDetailSceneStrategy`) |
| Presentation | **MVI** — `UiState` + `Intent` + ViewModel `onIntent()`, Hilt `@HiltViewModel` |
| DI | Hilt |
| Serialization | kotlinx.serialization |
| Async | Coroutines / Flow |
| Collections | kotlinx-collections-immutable |
| Build | AGP 9.2.1 · Gradle 9.4.1 · convention plugins (`:build-logic`) |

---

## Common Commands

| Task | Command |
|---|---|
| Compile (검증) | `./gradlew :app:compileDebugKotlin` |
| Unit tests | `./gradlew :core:design-foundation:testDebugUnitTest` |
| Build APK | `./gradlew :app:assembleDebug` |

> 깃 작업은 사용자가 직접 수행(현재 git 미초기화). 임의 커밋 금지.
