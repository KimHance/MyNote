# 아키텍처 & 모듈 경계

## 모듈 그래프 (→ = 의존)

```
:app   → :core:{design-foundation, design-system, domain, data, network, database}, :feature:notes
         (composition root: 구현체 모듈을 모두 포함해 Hilt 그래프를 완성)

:feature:notes → :core:design-system, :core:domain      (feature convention 이 자동 주입)

:core:domain   — 가장 안쪽. 모델(Category/Note/NoteBlock) + 인터페이스(NotesRepository/TokenRepository) + UseCase. 의존 없음.
:core:data     → :core:domain   (도메인 인터페이스 구현 + Remote/Local/Token DataSource 인터페이스 소유)
:core:network  → :core:data     (RemoteNotesDataSource 구현 = Contents API, AuthInterceptor + GithubTokenHolder)
:core:database → :core:data     (Local/Token DataSource 구현 = Room 노트캐시 + DataStore 토큰)
:core:design-system → api(:core:design-foundation)
```

> **DIP 핵심:** 인터페이스는 안쪽(domain/data)이 소유하고, 바깥(network/database)이 구현한다.
> 의존 화살표는 항상 바깥→안쪽. `:app` 만 구현체 모듈을 알고 Hilt 로 바인딩한다. **SSOT 는 Room.**

## 하드 룰
- **feature 모듈은 `:core:data`/`:core:network`/`:core:database` 를 직접 의존하지 않는다.** `:core:domain` UseCase 만 경유(도메인 모델은 domain 소유).
- **`:core:network`/`:core:database` 는 `:core:data` 의 DataSource 인터페이스를 구현**하고, `:core:data` 는 그 구현체를 모른다.
- Room `@Entity`·Retrofit·DataStore 타입은 각 모듈 밖으로 새지 않는다(경계 교차 타입: `CachedNote` 등 data 소유 타입).
- 역방향 의존 금지: `:core:design-foundation` 은 `:core:design-system` 을 의존하지 않는다. feature ↔ feature 직접 의존 금지.
- 버전은 `/gradle/libs.versions.toml` 에만. `build.gradle.kts` 에 하드코딩 금지(의존성도 `libs.*` alias).

## 프레젠테이션: MVI
- 상태: `XxxUiState` (`@Immutable`), 단일 소스. 컬렉션은 `ImmutableList`/`ImmutableMap`.
- 인텐트: `sealed interface XxxIntent` (UI → ViewModel 단방향).
- ViewModel: `@HiltViewModel`, 단일 진입점 `fun onIntent(intent)`. 상태 생산은 **Molecule** — `MoleculeViewModel<Event, Model>` 을 상속해 `@Composable present(events)` 프레젠터에서 선언적으로 조립한다(`combine`/`stateIn` 대신 `remember`/`collectAsState`/`LaunchedEffect`). `onIntent` → `take(intent)` → events Flow. 상태 노출은 `models: StateFlow`.
- **상태홀더(예: `SnackbarHostState`)도 프레젠터(`present`)가 소유**하고 발동까지 담당한다(Circuit/Molecule 철학). UI 는 `SnackbarHost(uiState.snackbarHostState)` 렌더만.
- 화면 Composable 은 콜백을 `onIntent: (XxxIntent) -> Unit` **하나만** 받는다. design-system 의 재사용 컴포넌트는 MVI 를 모르고 순수 콜백(`onClick` 등)을 받는다.

### Route / Screen 분리 (필수)
- **`XxxRoute`** = ViewModel 주입(`hiltViewModel()`) + 상태 수집(`collectAsStateWithLifecycle`)만. ViewModel 의존 때문에 프리뷰 불가. `XxxScreen` 을 호출만 한다.
- **`XxxScreen`** = stateless. `uiState` + `onIntent: (Intent) -> Unit`(+ 필요한 host state)만 받는다. **테마 블록(`MyNoteTheme { … }`)은 Screen 에서 시작**한다.
- **Screen 에는 항상 `@Preview`** — ViewModel 없이 더미 `uiState` + `onIntent = {}` 로 렌더. 폼팩터별로 `@Preview(widthDp = …)` 를 여러 개 둔다.

```kotlin
@Composable fun XxxRoute(vm: XxxViewModel = hiltViewModel()) {
    val uiState by vm.models.collectAsStateWithLifecycle()   // Molecule: models: StateFlow
    XxxScreen(uiState, onIntent = vm::onIntent)
}
@Composable fun XxxScreen(uiState: XxxUiState, onIntent: (XxxIntent) -> Unit) {
    MyNoteTheme(...) { /* 화면 */ }
}
@Preview @Composable private fun XxxScreenPreview() = XxxScreen(previewUiState, onIntent = {})
```

## 내비게이션 / 레이아웃: 상태 기반 + 접이식 패널
- 화면 전환은 **단일 상태(`activeNote`)** 가 구동한다 — `activeNote == null` 이면 홈, 아니면 리더. (Nav3
  `NavDisplay`/`ListDetailSceneStrategy` 는 사용하지 않는다. 수동 토글이 필요한 push 패널과 맞지 않아 제거함.)
- 폼팩터(`containerDpSize`)는 **목록 패널 표현만** 가른다: Compact = `ModalNavigationDrawer`(화면 위 오버레이),
  Medium+ = `Row` + `AnimatedVisibility`(가로 expand/shrink) 접이식 사이드 패널(옆에서 미는 push).
- **두 폼팩터 모두 ☰ 토글이 항상 살아 있다.** 패널 펼침 상태는 `rememberSaveable` 로 보존.
- ViewModel 주입: `hiltViewModel()` (`androidx.hilt.lifecycle.viewmodel.compose`).

## Convention Plugins (항상 사용, 수동 설정 금지)
| alias | 용도 |
|---|---|
| `billboard.android.application` (+ `.compose`) | `:app` |
| `billboard.android.library` (+ `.compose`) | core 라이브러리 |
| `billboard.android.feature` | `:feature:*` (`:core:design-system` + `:core:domain` + lifecycle + coroutines 자동) |
| `billboard.android.hilt` | Hilt 모듈 |

> AGP 9 기준: Kotlin 빌트인이라 `org.jetbrains.kotlin.android` 적용 금지. `LibraryExtension` 은 `com.android.build.api.dsl.*`(newDsl). flavor/Circuit/Room convention 은 미사용.
