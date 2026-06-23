# Compose & 상태

## Stability (필수)
- 컴포저블 파라미터·State 의 컬렉션은 **`ImmutableList`/`ImmutableMap`/`persistentListOf()`** (kotlinx-collections-immutable). **`List<T>` 금지.**

### `@Immutable` vs `@Stable` — 의미로 구분 (헷갈리지 말 것)
- **`@Immutable`** = 생성 후 **모든 프로퍼티가 deeply 불변**. "이 애는 절대 안 바뀐다"고 못 박는 것.
- **`@Stable`** = 바뀔 수 있지만 변경이 **우리 통제 안**에 있고(외부에서 혼자 안 날뜀), 변경 시 Compose 에 통지된다.
- **판단 기준:** 내부에 `List`(읽기 전용 인터페이스일 뿐 불변 보장 X — 실제 `MutableList` 일 수 있음) 같은 값이 **하나라도** 있으면 deeply immutable 이 아니므로 **`@Stable`**. 멤버가 전부 `ImmutableList`/원시타입/`Color`/`Dp`/`TextStyle` 처럼 진짜 불변뿐이면 **`@Immutable`**.
  - 예: `NotesUiState` 는 `Category(notes: List<Note>)` 를 품으므로 **`@Stable`**. `RecentNote`(`Note` = `val String` 만)·`MdBlock`(ImmutableList)·디자인 토큰은 **`@Immutable`**.
- **`object`**(싱글톤 + `val` 상수)는 본래 stable — `@Immutable`/`@Stable` 을 붙이지 않는다.

### 직렬화 모델
- **`:core:data` 의 `@Serializable` 모델(`Note`/`Category`)은 순수하게 유지**(Compose 의존 없음, `List` 사용). stability 는 이를 담는 UI 레이어(`UiState` `@Stable`, `RecentNote`/`MdBlock` `@Immutable`)에서 보증한다.

## 테마 접근
- 색/타이포/간격은 **`MyNoteTheme.colors` / `MyNoteTheme.textStyles` / `MyNoteTheme.adaptive`** 로만. `MaterialTheme` 을 직접 테마 용도로 쓰지 않는다.
- (Material3 컴포넌트 자체 — `Scaffold`/`TopAppBar`/`Text` 등 — 사용은 정상. 색은 토큰으로 지정.)

## 윈도우 크기 / 반응형
- 폼팩터 판정은 **`LocalWindowInfo.current.containerDpSize`** 사용(이미 `Dp` 단위, density 변환 불필요).
- `BoxWithConstraints` 로 윈도우 크기를 재지 않는다 — subcomposition 비용이 있고, 그건 "윈도우"가 아니라 "부모 제약"이다.
- 단계별 토큰은 `AdaptiveTokens`(5단계). 간격/폭/열수를 하드코딩하지 않는다.

## Recomposition 규율
- **애니메이션 값은 draw phase 로 격리(deferred read).** `Modifier.graphicsLayer { rotationZ = animatedValue }` 처럼 **람다 안**에서 읽는다.
  - **금지:** `Modifier.rotate(value)` / `offset(x)` / `alpha(v)` 처럼 애니메이션 값을 **인자로** 받는 modifier — composition phase 에서 읽혀 매 프레임 recomposition 을 유발한다. `graphicsLayer { rotationZ = … / translationX = … / alpha = … }` 로 대체.
  - 예(caret 회전): `Modifier.graphicsLayer { rotationZ = caretRotation }` (O) / `Modifier.rotate(caretRotation)` (X).
- density 의존 계산 등 recomposition 간 불변값은 `remember(key) { }` 로 캐싱(예: `remember(note.md) { MarkdownParser.parse(...) }`).
- `by animateFloatAsState(...)` 는 `import androidx.compose.runtime.getValue` 필요.

## 시스템바 인셋 (edge-to-edge)
- `Scaffold` + `TopAppBar` + `ModalDrawerSheet` 가 인셋을 프레임워크 차원에서 처리한다. 화면에서 `statusBarsPadding`/`navigationBarsPadding` 을 수동으로 더하지 않는다.
- 상태바 아이콘 명암은 `MainActivity` 의 `enableEdgeToEdge(SystemBarStyle.dark(...))` 로 고정(다크 단일 테마).
