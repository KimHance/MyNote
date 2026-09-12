| 포맷/린트 | `./gradlew ktlintFormat --continue` · `./gradlew ktlintCheck` · `./gradlew lintDebug` |
# MyNote — Claude Code Instructions

## Language Rules
- 대화 응답은 한국어.
- 코드 주석은 한국어.
- 문서/설정 파일(CLAUDE.md, 룰 등)은 한국어 허용(사용자 선호).

---

## Project Overview

GitHub 의 JSON 노트를 읽어 "블로그 글처럼" 보는 **읽기 전용** 안드로이드 학습 노트 앱.
앱에서 write 없음. 콘텐츠는 **서버드리븐**: 레포 루트 `notes/` 의 `index.json`(group→노트, 해시) +
노트별 JSON 을 **GitHub Contents API**(private 레포 + 토큰)로 받아 **Room 에 캐싱**하고 해시로 reconcile.
다크 단일 테마, 멀티 폼팩터(반응형). 토큰은 런타임 입력(release) / `local.properties`(debug).

**Package root:** `com.hancekim.mynote`
**SDK:** compileSdk 37 · targetSdk 36 · minSdk 32
**Base:** `Billboard 프로젝트` 의 Gradle/convention plugin 을 가져와 각색(Circuit·flavor 제거 완료, 전체 재조립형 MVI 채택).

---

## Rules
@.claude/rules/01-architecture.md
@.claude/rules/02-compose-state.md
@.claude/rules/03-di-hilt.md
@.claude/rules/04-design-system.md
@.claude/rules/05-error-handling.md
@.claude/rules/06-testing.md
@.claude/rules/07-notes-authoring.md

---

## Module Reference

| Module | Role |
|--------|------|
| `:app` | Application(Hilt), MainActivity, edge-to-edge, `NotesRoute` 호스트. 레포좌표 BuildConfig·앱스코프·`TokenSync` 제공 |
| `:core:common` | MVI base(`MviViewModel`/`MviState`/`MviEvent`). leaf — Compose·다른 core 모듈 의존 없음. 사용법은 해당 패키지 `AGENTS.md` |
| `:core:design-foundation` | 색/타이포/radius/spacing 토큰, `AdaptiveTokens`(너비 5단계 × 높이 3단계). (leaf, 파서 없음) |
| `:core:design-system` | `MyNoteTheme`(토큰 CompositionLocal), 공용 컴포넌트(+`FloatingNavBar`/`Rail`·`fadingEdges`), `NoteContent` 블록 렌더(UI 모델 `NoteContentBlock`) |
| `:core:domain` | 모델(`Category`/`Note`/`NoteBlock`), `NotesRepository`/`TokenRepository` 인터페이스(DIP 소유), UseCase |
| `:core:navigation` | **내비게이션 계약** — 백스택 키(`MyNoteNavKey`, sealed)·씬/전략·전환 모션·공유 요소 배선. 테마/도메인 의존 없음 |
| `:core:data` | DataSource 인터페이스(Remote/Local/Token), DTO·Mapper, `NotesRepositoryImpl`(reconcile)·`TokenRepositoryImpl`, Hilt |
| `:core:network` | GitHub Contents API(Retrofit), `RemoteNotesDataSourceImpl`, `AuthInterceptor`+`GithubTokenHolder`(클라 토큰 주입) |
| `:core:database` | Room3 노트 캐시(`NoteEntity`/DAO/DB), `LocalNotesDataSourceImpl`, DataStore 토큰 저장(`DataStoreTokenStore`) |
| `:feature:notes` | 3화면(Home·Reader·ListPanel), `NotesViewModel`(`MviViewModel` 상속), 노트 전용 공유요소 키, 토큰 입력 다이얼로그 |

---

## Tech Stack

| Layer | Tech |
|---|---|
| Language / UI | Kotlin · Jetpack Compose · Material3 |
| Navigation | **Navigation 3** — `NavDisplay` 1개 + 커스텀 `Scene`/`SceneStrategy`. 백스택은 `activeNoteId` 에서 파생(SSOT 는 ViewModel). Compact=씬 스택+모달 드로어 · Medium+=고정 패널 씬. 예측 뒤로가기 + 공유 요소 |
| Presentation | **전체 재조립형 MVI** — `:core:common` 의 `MviViewModel<S, E>`(`trigger`/`triggerIn` → `build()` → `state`), Hilt `@HiltViewModel` |
| DI | Hilt |
| Network | Retrofit · OkHttp(`AuthInterceptor`) · GitHub Contents API |
| Local cache | **Room 3**(`androidx.room3`, KSP) — 노트 SSOT |
| Local prefs | **DataStore Preferences** — 토큰 저장 |
| Serialization | kotlinx.serialization (polymorphic `NoteBlockDto`, discriminator `type`) |
| Async | Coroutines / Flow |
| Collections | kotlinx-collections-immutable |
| Liquid glass | **backdrop 2.0.1**(`io.github.kyant0`) — 플로팅 내비 유리 표면. 블러 + **굴절(lens, AGSL)** + 스펙큘러 하이라이트 |
| Build | AGP 9.4.0 · Gradle 9.6 · Kotlin 2.4.20 · convention plugins (`:build-logic`) |
| Lint / Format | ktlint 1.8.0 + compose-rules 0.6.6(`hance.ktlint`) · Slack compose-lints 1.6.0(Android Lint) · `.editorconfig` |
| Release | R8 minify + resource shrinking(`app/proguard-rules.pro`) — release 3.2MB, missing_rules 없음 |

---

## Common Commands

| Task | Command |
|---|---|
| Compile (검증) | `./gradlew :app:compileDebugKotlin` |
| Unit tests | `./gradlew testDebugUnitTest`(전 모듈) · `./gradlew :core:common:testDebugUnitTest`(MVI base) |
| Build APK | `./gradlew :app:assembleDebug` · `./gradlew :app:assembleRelease`(R8) |
| 포맷/린트 | `./gradlew ktlintCheck` · `./gradlew ktlintFormat` · `./gradlew lintDebug` |
| 노트 index 재생성 | `python3 notes/gen_index.py` (노트 수정 후 — 스키마 검증 + hash) |

> 깃 작업은 사용자가 직접 수행. **임의 커밋 금지**(요청 시에만). 커밋은 사용자 신원으로, AI 공동저자 푸터 없이.
> pre-commit 훅(`.githooks/pre-commit`)이 **스테이징된 Kotlin 파일만** ktlint 검사하고, 노트를 건드렸으면 `index.json` 을 재생성한다.
> 설치는 머신당 1회: `git config core.hooksPath .githooks`
> 토큰은 `local.properties`(gitignore)에만 — debug 전용 내장, release 는 런타임 입력.
