# 아키텍처 & 모듈 경계

## 모듈 그래프 (→ = 의존)

```
:app
  → :core:design-foundation, :core:design-system, :core:domain, :feature:notes

:feature:notes
  → :core:design-system, :core:domain      (feature convention 이 자동 주입)

:core:domain
  → api(:core:data)                          (모델 + Repository 인터페이스 전파)

:core:design-system → api(:core:design-foundation)
:core:data — 모델 + Repository + 더미 구현 + Hilt 모듈
```

## 하드 룰
- **feature 모듈은 `:core:data` 를 직접 의존하지 않는다.** `:core:domain` UseCase 를 경유한다(데이터 모델은 domain 의 `api(:core:data)` 로 transitive).
- 역방향 의존 금지: `:core:design-foundation` 은 `:core:design-system` 을 의존하지 않는다.
- feature ↔ feature 직접 의존 금지.
- 버전은 `/gradle/libs.versions.toml` 에만. `build.gradle.kts` 에 하드코딩 금지.

## 프레젠테이션: MVI
- 상태: `XxxUiState` (`@Immutable`), 단일 소스. 컬렉션은 `ImmutableList`/`ImmutableMap`.
- 인텐트: `sealed interface XxxIntent` (UI → ViewModel 단방향).
- ViewModel: `@HiltViewModel`, 단일 진입점 `fun onIntent(intent: XxxIntent)`. 내부 `MutableStateFlow` 들을 `combine` → `stateIn` 으로 `uiState` 노출.
- 화면 Composable 은 콜백을 `onIntent: (XxxIntent) -> Unit` **하나만** 받는다. design-system 의 재사용 컴포넌트는 MVI 를 모르고 순수 콜백(`onClick` 등)을 받는다.

### Route / Screen 분리 (필수)
- **`XxxRoute`** = ViewModel 주입(`hiltViewModel()`) + 상태 수집(`collectAsStateWithLifecycle`)만. ViewModel 의존 때문에 프리뷰 불가. `XxxScreen` 을 호출만 한다.
- **`XxxScreen`** = stateless. `uiState` + `onIntent: (Intent) -> Unit`(+ 필요한 host state)만 받는다. **테마 블록(`MyNoteTheme { … }`)은 Screen 에서 시작**한다.
- **Screen 에는 항상 `@Preview`** — ViewModel 없이 더미 `uiState` + `onIntent = {}` 로 렌더. 폼팩터별로 `@Preview(widthDp = …)` 를 여러 개 둔다.

```kotlin
@Composable fun XxxRoute(vm: XxxViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    XxxScreen(uiState, onIntent = vm::onIntent)
}
@Composable fun XxxScreen(uiState: XxxUiState, onIntent: (XxxIntent) -> Unit) {
    MyNoteTheme(...) { /* 화면 */ }
}
@Preview @Composable private fun XxxScreenPreview() = XxxScreen(previewUiState, onIntent = {})
```

## 내비게이션: Navigation 3 + Adaptive
- `NavDisplay` + `rememberNavBackStack` + `entryProvider { entry<Key> { } }`. `entry` 는 `EntryProviderScope` 멤버(별도 import 불필요).
- `NavKey` 는 `@Serializable`.
- 목록-상세는 Material3 `ListDetailSceneStrategy`(Medium+ 2-pane). Compact 는 `ModalNavigationDrawer`(핸드오프 모달 드로어).
- ViewModel 주입: `hiltViewModel()` (`androidx.hilt.lifecycle.viewmodel.compose`).

## Convention Plugins (항상 사용, 수동 설정 금지)
| alias | 용도 |
|---|---|
| `billboard.android.application` (+ `.compose`) | `:app` |
| `billboard.android.library` (+ `.compose`) | core 라이브러리 |
| `billboard.android.feature` | `:feature:*` (`:core:design-system` + `:core:domain` + lifecycle + coroutines 자동) |
| `billboard.android.hilt` | Hilt 모듈 |

> AGP 9 기준: Kotlin 빌트인이라 `org.jetbrains.kotlin.android` 적용 금지. `LibraryExtension` 은 `com.android.build.api.dsl.*`(newDsl). flavor/Circuit/Room convention 은 미사용.
