# AGENTS.md — :core:design-system

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈의 자리

`MyNoteTheme`(토큰 배선) + 재사용 위젯/Modifier + 서버드리븐 콘텐츠 렌더러. **순수 UI 레이어**다.

**디자인 시스템의 기준은 루트 `DESIGN.md` 다** — 토큰·창 크기·모션·컴포넌트·문구·접근성 규칙과 디자인 ↔ 코드 차이 표. 화면을 새로 그리기 전에 읽어라.

의존은 다섯 줄뿐이고 각 줄이 의도를 가진다:

| 선언 | 이유 |
|---|---|
| `api(projects.core.designFoundation)` | 토큰 타입(`MyNoteColors`·`MyNoteTextStyles`·`AdaptiveTokens`·`MyNoteWindowClass`)이 **공개 시그니처에 실려 나간다**. `MyNoteTheme.colors` 의 반환형이 그것이므로 `implementation` 으로 바꾸면 feature 가 전부 깨진다 |
| `api(libs.backdrop)` | feature 가 `rememberLayerBackdrop()` 을 부르고 `Backdrop` 을 컴포넌트에 넘긴다. 이 타입도 경계를 넘는다 |
| `implementation(projects.core.common)` | `Throttler` **하나**만 쓴다(`throttledProcess`). 이게 유일한 비-UI 의존이다 |
| `implementation(libs.androidx.compose.foundation)` | compose convention 은 `material3`·`ui`·`ui-tooling-preview` 만 넣는다. `foundation`(`clickable`/`BasicTextField`/`Canvas`/`drawBehind`)은 **이 모듈이 직접 선언**한다 |
| `implementation(libs.androidx.compose.ui.text.google.fonts)` | 제목 글꼴(Gothic A1)을 GMS 글꼴 제공자에서 내려받는다(`font/` — `brand` BR-8). 버전은 BOM 이다 |

`:feature:notes` 는 `:core:design-foundation` 을 자기 `build.gradle.kts` 에 적지 않는다 —
위의 `api` 로 받는다. 토큰이 필요해졌다고 feature 에 의존을 추가하지 말 것.

**토큰의 소유자는 이 모듈이 아니다.** 색/타이포/radius/spacing/`AdaptiveTokens` 는 전부
`:core:design-foundation` 에 있다. 이 모듈은 그것을 CompositionLocal 로 **배선**하고, 위젯에서
**소비**만 한다. 새 색이나 새 텍스트 스타일이 필요하면 여기가 아니라 foundation 의 팔레트·
`myNoteTextStyles()` 를 고친다.

바깥으로는 데이터/도메인/nav3/MVI 를 **모른다**:
- 서버드리븐 콘텐츠는 이 모듈의 UI 모델 `NoteContentBlock`/`NoteContentSpan` 으로만 받는다.
  `:core:data` 의 DTO → UI 모델 매핑은 feature 가 단방향으로 한다.
- 공유 요소 전환은 `Modifier` 슬롯으로만 받는다(`NoteCard(chipModifier/dateModifier/titleModifier)`).
  기본값이 `Modifier` 라 프리뷰가 안 깨진다. `sharedElement` 조립은 feature 가 한다.
- 위젯은 인텐트를 모르고 순수 콜백(`onClick`/`onSelect`/`onQueryChange`)만 받는다.

---

## 테마 — CompositionLocal 네 개, MaterialTheme 은 없다

`MyNoteTheme` 은 `staticCompositionLocalOf` 넷을 제공한다: colors / adaptive / textStyles / fontScale.
앞의 셋은 **기본값이 `error(...)`** 다. `MyNoteTheme { }` 으로 감싸지 않으면 프리뷰가 예외로 죽는다.

다섯째 `LocalTitleFontFamily` 는 반대로 `MyNoteTheme` **바깥**에서 들어온다 — `ProvideTitleFontFamily` 로 `:app` 의
`MainActivity` 가 프로세스마다 한 번 정한 제목 글꼴을 주고, `MyNoteTheme` 이 그것을 `myNoteTextStyles(…, titleFontFamily)` 에 넘긴다.
기본값은 기기 글꼴(`FontFamily.Default`)이라 프리뷰 · 시험은 감쌀 필요가 없다. `textStyles` 의 `remember` 키에 이 글꼴도 들어 있다.

`static`- 이라 값이 바뀌면 fine-grained 무효화가 아니라 **content 서브트리 전체가 재컴포즈**된다.
그래서 `adaptive`/`textStyles` 는 `remember(windowClass, heightClass)` / `remember(fontScale, heightClass)`
로 묶여 있다. 여기에 키를 빼먹거나 매번 새 인스턴스를 만들면 창 크기가 연속으로 변하는
폴더블·분할 화면에서 화면 전체가 프레임마다 다시 컴포즈된다.

**`MaterialTheme` 래퍼가 없다.** 이 모듈 소스 전체에 `MaterialTheme` 참조가 0건이다.
Material3 컴포넌트(`Text`·`Snackbar`·`Icon`·`Scaffold`)는 정상적으로 쓰지만, 그것들이 기본색으로
읽는 `colorScheme` 은 제공되지 않는다 → **색·스타일을 생략하면 머티리얼 기본색이 떠서 테마와 따로 논다.**
`MyNoteSnackbar` 가 존재하는 이유가 정확히 이것이다(M3 기본 스낵바는 `inverseSurface` 계열로 뜬다).
`Surface` 를 쓸 때도 `color` 를 토큰으로 명시해야 한다.

테마 블록의 진입점은 **화면 루트 한 곳**이다. 앱에서는 `NotesScreen` 이
`LocalWindowInfo.current.containerDpSize` 로 너비·높이 클래스를 구해 한 번 감싸고,
프리뷰는 `@Preview` 마다 직접 감싼다. `MyNoteTheme { }` 를 인자 없이 부르면
**Compact × Medium × Normal** 이 적용된다 — 폼팩터를 보려면 프리뷰에서 명시해야 한다.

CompositionLocal 경고는 양쪽에서 끄여 있다: Android Lint 는
`build-logic/.../AndroidCompose.kt` 의 `lint.disable += "ComposeCompositionLocalUsage"`,
ktlint compose-rules 는 `.editorconfig` 의 `ktlint_compose_compositionlocal-allowlist = disabled`.
둘 중 하나만 끄면 다른 쪽에서 막힌다.

---

## 리퀴드 글래스 — 짝이 맞아야 하고, 조립은 `GlassSurface` 안에서만

`backdrop`(`com.kyant.backdrop`) 은 **두 지점이 같은 인스턴스를 봐야** 동작한다:

- 굴절 **대상**에 `Modifier.layerBackdrop(backdrop)` — feature 가 건다.
  현재 위치는 `NotesShell` 의 `NavDisplay` 를 감싼 전용 `Box` 하나다.
- 유리 **표면**에 `drawBackdrop(...)` — 이 모듈의 `GlassSurface` 가 건다.

하나라도 빠지면 **예외도 로그도 없이 그냥 아무것도 안 그려진다.** 내비·최상단 버튼은
그 `Box` **밖**에 있어야 한다(자기 자신을 샘플링하면 안 된다).

`drawBackdrop` 을 직접 부르는 곳은 `GlassSurface.kt` **한 군데뿐**이다. 유리가 필요한 자리는
feature 든 이 모듈이든 `GlassSurface` 를 쓴다(`FloatingNavBar`/`FloatingNavRail`/`ScrollToTopButton`,
feature 의 `QuizSetupDialog`). 효과 체인(`blur → lens → highlight → shadow`)을 바깥에서 다시
조립하지 말 것.

### 파라미터가 무엇을 사는가

`blurRadius` 와 `refractionRatio` 는 **맞바꾸는 값**이다 — 블러가 굴절이 밀어낼 디테일을 먼저
지우므로 블러를 세게 걸면 굴절이 안 보인다. 핸드오프의 20dp 로는 굴절이 전혀 보이지 않아
기본값이 `GLASS_BLUR_RADIUS = 8.dp` 로 낮춰져 있다. 그래서 같은 컴포넌트가 두 역할을 한다:

- **읽을 게 많은 면**(내비 바/레일) — 기본값. 블러로 가독성을 산다. `refractionRatio = LENS_AMOUNT_RATIO(2f)`.
- **작은 물방울**(퀴즈 다이얼로그의 세그먼트) — `blurRadius` 를 죽이고
  `refractionRatio = DROPLET_REFRACTION_RATIO(3.4f)` 로 굴절을 산다. `shadow = null`.

`refractionAmount = radius * refractionRatio` 이므로 **radius 를 바꾸면 굴절 세기도 같이 변한다.**
`tint` 를 주면 `colors.glass` 대신 그 색이 표면색이 된다(비-backdrop 폴백에서는 `bg` 위에 합성).

**`backdrop = null` 이 기본값이고 "효과 없음"** 을 뜻한다 — `clip + background(glassOpaque) + border(glassBorder)`
불투명 폴백으로 떨어진다. 프리뷰가 이 경로를 탄다. 파라미터를 추가할 때 이 분기도 같이 처리할 것.

단계적 저하(굴절 API 33+ / 블러 31+ / 그 아래 불투명)는 **라이브러리가 가른다.** 호출부에서
SDK 로 분기하지 않는다.

`GlassSurface` 는 `content()` 를 **정확히 한 번** 부르고 분기는 `Modifier` 쪽에서만 한다.
분기마다 `content()` 를 부르면 ktlint compose-rules 의 `content-slot-reused` 위반이고
동시에 슬롯 내부 상태가 날아간다.

---

## `PressIndication` — 게으른 노드 생성 함정 (실제 버그였다)

`clickable(interactionSource = null, indication = IndicationNodeFactory)` 는 Compose 가
**첫 터치까지 indication 노드 생성을 미룬다.** 눌림 표현만 하는 인디케이션에는 문제가 없지만,
`PressIndication` 은 `color` 로 **평상시 면까지 직접 그린다** — 그래서 면을 그리는 자리에
`interactionSource = null` 을 쓰면 **버튼 배경이 아예 안 보이고 누를 때만 잠깐 나타난다.**

호출 규약:

| 쓰는 값 | `interactionSource` |
|---|---|
| `color` 를 넘긴다(평상시 면을 그린다) | `remember { MutableInteractionSource() }` **를 명시** |
| `pressColor` 만 / `scale` 만 (평상시 투명) | `null` 로 둬도 된다 |

코드가 이 규약대로 갈려 있다 — `QuizParts` 의 primary 버튼과 `PressIndication` 프리뷰의
채워진 쪽만 `interactionSource` 를 명시하고, `ScrollToTopButton`·테두리형·스케일 전용은 `null` 이다.

부수적으로 알아야 하는 것:

- **면을 `Modifier.background` 로 따로 깔면 안 된다.** 노드의 `draw()` 는
  `scale(factor) { drawSurface(); drawContent() }` 로 면과 내용을 **같이** 줄인다.
  `background` 를 체인 앞에 두면 그것만 안 줄어서 라벨만 쪼그라든 것처럼 보인다.
  면은 `color`/`pressColor` 로 넘겨야 한다. 그래서 `ScrollToTopButton` 에는 `clip`/`background` 가 없다.
- `color`·`pressColor` 둘 다 `Unspecified` 면 `drawSurface` 는 즉시 빠진다(스케일만 남는다).
- `PressIndication` 은 `@Immutable` 이고 `equals`/`hashCode` 를 손으로 구현한다 — 파라미터를
  추가하면 **둘 다 같이 고쳐야 한다.** 안 고치면 색이 바뀌어도 노드가 갱신되지 않는다.
- **`FloatingNav` 은 일부러 `PressIndication` 을 안 쓴다.** 거기는 `selectionPill` 이
  그라디언트·코스틱·rim·스펙큘러까지 그려서 표현이 안 되고, `pressed` 를 `() -> Boolean` 으로
  넘겨 `drawBehind` 안에서 읽으므로 이미 지연 읽기다.

---

## 값을 어느 단계에서 읽는가 — 이 모듈의 일관된 규율

애니메이션 값이 **컴포지션에서 읽히면 매 프레임 리컴포지션이 돈다.** 이 모듈은 세 가지 방식으로
그걸 draw/layout 단계에 가둔다. 새 모션을 추가할 때 같은 형태를 따를 것.

**1. 값을 `() -> Float` 로 노출한다.** `FlipState.rotationX`/`scale` 은 `Float` 이 아니라 람다다.
`Float` 으로 주면 호출부가 컴포지션에서 읽어 버린다 — 람다여야 `Modifier.flip` 의
`graphicsLayer { }` **안에서** 호출되어 draw 단계에 갇힌다. 같은 이유로 `selectionPill`/`NavIcon` 도
`selection: () -> Float`, `pressed: () -> Boolean` 을 받는다.

**2. `Modifier.Node` 안에 `Animatable` 을 둔다.** `PressIndication`(`DrawModifierNode`)·
`riseIn`(`LayoutModifierNode` + `placeWithLayer`)·`aiShine`(`DrawModifierNode`) 셋은 진행도를
노드 내부 `Animatable` 로 갖고 `onAttach` 의 `coroutineScope` 에서 돌린다. 진행도가 컴포지션에
아예 올라오지 않는다. `riseIn` 이 `graphicsLayer` 가 아니라 `placeWithLayer` 인 이유는
alpha 와 translationY 를 함께 걸어야 해서다.

멈추지 않고 도는 그리기 노드는 **자기 레이어**가 있어야 한다. 레이어 없는 노드의 그리기 무효화는 가장 가까운 부모
레이어를 통째로 다시 기록한다 — 리더 맨 끝 퀴즈 카드라면 세로 스크롤 내용, 곧 노트 본문 전체다(3,000항목 노트에서 손대지
않아도 프레임마다 기록 380ms). 그래서 `aiShine` 은 팩토리가 `graphicsLayer {}` 를 앞에 붙인다. 지우지 말 것.

**3. modifier 인자로 애니메이션 값을 넘기지 않는다.** `Modifier.rotate(v)`/`offset(x)`/`alpha(v)` 는
composition phase 에서 읽힌다. `CategoryRow` 의 caret 이 `graphicsLayer { rotationZ = caretRotation }`
인 것이 그 예다.

draw 안에서 크기에 따라 다시 만들어야 하는 것은 **`size` 를 키로 직접 캐시**한다 —
`PressIndication` 의 `outline`, `aiShine` 의 링 `Path` + sweep `Brush`. 매 프레임 `Path`/`Brush` 를
새로 만들지 말 것.

`FlipState` 는 `AnimatedVisibilityScope.transition` 의 child 두 개(회전=tween, 스케일=spring)를
따로 돌린다. 스펙이 하나가 아닌 이유가 이것이다.

---

## 패키지 경계

- **`theme/`** — `MyNoteTheme` 과 CompositionLocal 배선 **전용**. 여기 있는 것은 테마에 참여한다.
- **`font/`** — 제목 글꼴(`brand` BR-8). `TitleFontGate.kt` 는 순수 판단 — `decideTitleFont(source, giveUp)`(받기와 `giveUp` 을 함께
  시작해 먼저 끝난 쪽 · 제공자 없음 · 실패 · 늦음이면 기기 글꼴, 예외를 밖으로 내지 않는다. 제공자 확인과 받기는 나란히 돈다) ·
  `TitleFontGate`(처음 결정을 들고 다시 받지 않는다) · `awaitFirstFrameAndLamp`(`:app` 이 쓰는 `giveUp` — 첫 앱 프레임 준비와 불 켜짐 끝 중
  늦은 쪽) · `titleFontWaitMillis`(불 켜짐 끝까지 남은 시간 — 실행 요청 시각 기준).
  `GoogleTitleFont.kt` 는 GMS 제공자(인증서 배열 `ds_com_google_android_gms_fonts_certs`)에서 700 하나를 `AndroidFont.typefaceLoader` 로
  직접 받아 `FontFamily(Typeface)` 로 돌려준다 — 이미 받은 글꼴이라 그리는 도중에 바뀌지 않는다. 제공자는 부른 스레드의 루퍼로 답하므로
  따로 둔 `title-font` 루퍼 스레드에서 부른다(메인으로 받으면 첫 컴포지션 뒤로 밀린다). `FontFamily(Font(GoogleFont…))` 를 컴포지션에
  그대로 주면 리졸버가 늦게 받은 글꼴로 다시 그리므로 쓰지 않는다. 첫 프레임 문은 `:app` `MainActivity` 가 한다.
- **`motion/`** — `MyNoteEasing`, `rememberReduceMotion()`. CompositionLocal 로 제공되지도 않고
  테마에 참여하지도 않아서 `theme/` 에서 빠져나온 것들이다. 모션 상수를 `theme/` 에 넣지 말 것.
  `rememberReduceMotion()` 은 `LocalInspectionMode` 를 OR 로 물고 있어 **프리뷰에서는 항상 true** 다.
- **`component/`** — 재사용 위젯 + `Modifier` 확장(`fadingEdges`/`horizontalScrollFade`/`riseIn`/`aiShine`/`flip`) +
  상태 홀더(`FloatingNavScrollState`) + 아이콘. 화면 전용 위젯은 여기가 아니라 feature 에 둔다.
  `NoteCard(bookmarked = true)` 는 `BookmarkRibbon`(12 × 17dp, `text2`)을 카드 `TopEnd` 에 `offset(-16, -1)` 로 건다 —
  카드 테두리 1dp 를 덮어야 해서 `Column` 의 `clip` **바깥** `Box` 에 그린다. 메타 줄 end 패딩 24dp 는 북마크 여부와
  무관하게 항상 있어 켜고 꺼도 칩 말줄임 위치가 안 변한다. 리본은 desc 가 없고 카드 노드의 `stateDescription` 이 `ds_bookmarked` 다.
  `BookmarkGlyph`(12dp) 는 `NoteRow` 전용으로 남아 있다.
- **`chart/`** — 통계 차트 Canvas 5종(`TimeBars`·`CategoryDonut`·`CategoryStackBar`·`HBarRow`·`DivergingBars`).
  등장 애니메이션은 `progress: () -> Float` 하나를 받아 **그리기 람다 안에서** 읽는다(`rememberChartProgress(key)` 가
  만든다, 감속 모션이면 처음부터 1). 칸마다 지연·곡선은 `ChartMotion` 한 곳. 글자는 `TextMeasurer` 로 데이터가 바뀔
  때만 잰다. 색은 `chartAccent2/3`·`chartFuture`(foundation) + 기존 토큰. `NumberedRow`·`SectionLabel` 은
  `component/` 에 있다(북마크의 `BookmarkRow` 와 아직 통합 전).
- **`block/`** — 서버드리븐 콘텐츠의 UI 모델(`NoteContentBlock` 닫힌 sealed 집합)과 렌더러(`NoteContent`).
  블록 종류를 늘리려면 `core/format/AGENTS.md` '새 블록 · 필드를 더할 때' 를 따른다 — 마크다운 변환 · format 캐시 DTO ·
  domain · feature 매퍼 · 이 sealed 집합 · `NoteContent` 의 `when` 이 **한 세트로** 움직인다. JSON 형식 1 은 늘리지 않는다.
  `Unsupported(kind, salvaged, raw)` 만 와이어에 없는 **앱 내부 블록**이다 — 그리지 못한 블록·노트를 버리지 않고
  `UnsupportedBlock` 점선 상자로 그린다. `kind` 는 domain `UnsupportedReason` 을 옮긴 UI sealed 종류(이 모듈은 domain 을 모른다) —
  한도 넘음 `LimitExceeded(maxDepth, maxKilobytes, maxTableCells)` 는 문구 숫자를 들고 온다. 숫자를 이 모듈 소스에 다시 적지 않는다.
  `UnsupportedBlock` 은 종류를 `when` 으로 가르니 종류가 늘면 컴파일이 잡는다.
  빈 `salvaged`·`raw` 는 feature 매퍼가 `null` 로 바꿔 넘긴다(`raw == null` 이면 원문 보기 버튼이 없다). 펼침·2단은
  `UnsupportedBlock` 안이 아니라 **`NoteContentState`**(블록 index 집합 둘, `Saver` 로 저장)에 있고 `NoteContent(state = …)` 로
  받는다 — 블록 안 `rememberSaveable` 은 창 등급이 바뀌어 `NavDisplay` 가 다시 만들어질 때 키를 명시해도 사라진다(관문 2 S1).
  그래서 feature 셸이 리더 스크롤과 같은 자리·같은 키로 들고 내려 준다(`NotesScrollState.readerContent`, `NoteContentStateTest`).
  기본값 `rememberNoteContentState()` 는 프리뷰·다른 호출부용이다. 원문 자르기(앞 40줄 → 최대 200줄·10,000자)는 순수 함수 `sliceRaw`
  (`RawSlice.kt`, `RawSliceTest`)다. 원문은 **줄마다 `Text` 하나**로 그린다 — 한 `Text` 에 줄마다 `ParagraphStyle` 을
  걸면 복사한 글자에서 줄바꿈이 사라진다(`SelectionContainer` 는 `Text` 사이를 `'\n'` 으로 잇는다). 대신 패널 `Column` 을
  `mergeDescendants` 로 묶어 TalkBack 에는 한 노드다. 치수·문구는 `DESIGN.md` §8.6.
  리더는 지연 없는 `verticalScroll` Column 이라 블록 · 표 칸 · 하위 블록을 노트를 열 때 전부 재고, 그래서 렌더 셋은 측정을
  한 번에 끝낸다. 글자 구간(`NoteSpans.kt`)은 한 함수에서 모양을 겹치고 취소선 구간을 낭독 글자로 감싼다. 표(`NoteTable.kt`)는
  `Layout` 하나 — 칸마다 고유 폭 둘을 읽고 고정 폭으로 한 번만 잰다(`BoxWithConstraints`·`SubcomposeLayout`·`IntrinsicSize`
  금지). 열 폭은 순수 함수 `tableColumnWidths`(`TableColumnWidthsTest`), 선 · 머리 바탕은 측정이 상태에 남긴 자리를 그리기
  단계에서만 읽는다(`ScrollState.maxValue` 와 같은 길). 목록(`NoteList.kt`)은 기호 칸을 상수가 아니라 기호 글자를 재서 정하고
  항목을 첫 기준선으로 맞춘다. 4단부터 목록은 `hangLeft` 로 부모 기호 칸만큼 당겨 3단 기호 자리에 선다. 하위 블록은
  `NoteBlockContent` 를 다시 부른다. 하위 블록에는 상자가 오지 않는다(feature 매퍼가 건진 글자로 바꾼다). 와도 원문 보기 없이 그린다.

---

## 하드 룰 (지금 코드에서 실제로 지켜지고 있다 — 깨뜨리지 말 것)

- **`Color(0x...)` 리터럴 0건.** 색은 전부 `MyNoteTheme.colors`. 팔레트 정의는 foundation 에만.
  (예외는 `@Preview(backgroundColor = ...)` 뿐 — 애노테이션 인자는 상수만 받아 토큰을 못 쓴다.
  `0xFF0E1217` = `bg`, `0xFF161C24` = `surface` 를 **손으로 베낀 값**이라 팔레트를 바꾸면 같이 고쳐야 한다.)
- **`MaterialTheme` 참조 0건.** 위의 "테마" 절 참조.
- **컬렉션 파라미터는 `ImmutableList`** — `NoteContent(blocks)`, `FloatingNavBar(items)`,
  `NoteContentBlock.BulletList(items: ImmutableList<NoteContentListItem>)`. `List<T>` 금지.
- **UI 를 그리는 Composable 은 같은 파일에 `@Preview` 동반**, `MyNoteTheme { }` 로 감싸고 다크 배경 지정.
  프리뷰가 없는 파일은 **UI 를 emit 하지 않는 것**들뿐이다(`NoteContentBlock`=데이터,
  `FloatingNavScrollState`=상태 홀더, `MyNoteIcons`·`MyNoteLogo`=`ImageVector`, `Motion`, `MyNoteTheme`).
- **로고 그림(`MyNoteLogo` 의 마크 · 워드마크)은 tint 하지 않는다.** 마크는 창틀(`accent`)과 빛(로고 전용 주광 그라디언트,
  foundation `MyNoteBrandColors`)이 다른 색이라 tint 가 빛을 한 색으로 지운다. 색을 인자로 받아 `remember(색)` 으로 만든다.
  크기는 `MyNoteHero` 가 `heroLogoSize`(없으면 마크 없음)와 `heroTitleSize` 값을 dp 로 읽은 x높이로 정한다 — 글자 배율로 안 커진다.
- **여러 상태를 갖는 컴포넌트는 `private class …PreviewParameterProvider`** 로 상태를 주입한다
  (`FloatingNav` hidden, `NoteRow`/`CategoryRow` active/expanded, `SearchField` 빈/입력, `MyNoteSnackbar` 액션 유무).
- **클릭 가능한 `Box`/`Text` 는 `role`** 을 준다(`Role.Button`/`Tab`/`RadioButton`), 장식 아이콘은
  `contentDescription = null`. `FloatingNav` 은 선택됐을 때 라벨이 보이므로 아이콘 설명을 뺀다.

---

## 문구 (다국어 없음)

- `resourcePrefix = "ds_"` 가 Gradle 에서 **강제**된다. 라이브러리 리소스는 앱에 병합될 때
  이름이 겹치면 조용히 덮이므로, 접두사를 안 붙이면 빌드가 막히게 해 둔 것이다.
- `strings.xml` 에는 **여러 feature 가 공유하는 액션 문구**(확인/취소/저장/닫기/다시 시도/되돌리기)와
  이 모듈 컴포넌트가 쓰는 문구(`ds_scroll_to_top`·`ds_search_hint`·`UnsupportedBlock` 의 `ds_unsupported_*` 13개·본문 블록 낭독 `ds_block_*` 4개)만 둔다.
  feature 는 `import com.hancekim.mynote.core.designsystem.R as DsR` 로 참조한다.
- **파라미터 기본값에 문자열 리터럴을 두지 않는다.** `SearchField(placeholder: String? = null)` 처럼
  `null` 기본값 + `stringResource` 폴백으로 받는다 — 기본값에 리터럴을 두면 리소스를 우회한다.
- 코드의 한글 리터럴은 프리뷰 더미 데이터뿐이다. 말투는 합니다체 하나로 통일한다.

---

## 테스트

계측 테스트는 `androidTest` 의 둘이다. `NoteContentSemanticsTest` 는 본문 블록이 시맨틱 트리에 싣는 것(취소선 낭독 글자 ·
체크 상태 설명 · 표 행 · 열 수와 칸 정보 · 머리 칸 제목 · 제목 블록 제목)을, `MyNoteHeroTest`(BR-6)는 히어로의 긴 저장소 이름 한 줄 ·
높이 같음 · 워드마크 제목(heading) · 저장소 줄 전체 글자 · 높이 Compact 마크 없음 · 워드마크가 글자 배율로 안 커짐을 Compose 시험으로 본다.
`MyNoteHeroTest` 는 `captureToImage` 사진을 시험 APK 의 외부 파일 폴더 `hero/` 에 남긴다(`adb pull` 로 꺼낸다).
**`./gradlew test` 에는 들지 않는다.**
기기 하나만 붙여 `ANDROID_SERIAL=<기기 serial> ./gradlew :core:design-system:connectedDebugAndroidTest` 로 돌린다.
라이브러리 시험이라 앱을 지우지 않지만, 같은 기기에서 계측 시험을 동시에 돌리면 시험 APK 재설치로 서로 죽는다 — 차례로 돌린다.

단위 테스트는 `FloatingNavScrollStateTest`·`RawSliceTest`(원문 자르기 `sliceRaw`)·`NoteContentStateTest`(펼침 상태와 저장)·
`TableColumnWidthsTest`(표 열 폭)·`NoteListMarkerTest`(글머리 · 들여쓰기 멈춤)·`NoteSpansTest`(취소선 낭독 · 구간 모양)·
`TitleFontGateTest`(제목 글꼴 문 — 가짜 글꼴 출처와 가상 시간, 그래서 `kotlinx-coroutines-test` 시험 의존이 있다)인데, 가능한 이유는
같다 — 판정을 Compose 밖 순수 함수·클래스로 뺐다. `FloatingNavScrollStateTest` 가 가능한 이유는
`FloatingNavScrollState` 가 **Compose 를 모르고 px 두 개만 받는 순수 클래스**이고
`rememberFloatingNavScrollState()` 가 density 변환만 담당하도록 갈라 뒀기 때문이다.
숨김/표시 판정 같은 로직을 추가할 때는 이 형태(순수 상태 홀더 + `remember…` 어댑터)를 따르면
테스트가 붙는다. Composable 안에 판정 로직을 직접 쓰면 검증할 방법이 없다.

`FloatingNavScrollState` 의 실제 규칙 세 가지: `6.dp` 미만 이동은 떨림으로 무시, 상단 `48.dp`
안전지대에서는 숨지 않음, 누적 오프셋은 0 아래로 안 내려감. 노트를 바꿀 때 `reset()` 을
불러야 기준점까지 되돌아간다(`NotesShell` 의 `LaunchedEffect(uiState.activeNoteId)`).

---

## 라이선스

| 무엇 | 라이선스 | 어디에 |
|---|---|---|
| Gothic A1(제목 글꼴) | SIL Open Font License 1.1 | APK 에 들지 않는다 — 기기의 GMS 글꼴 제공자가 받아 둔 것을 쓴다 |
| `androidx.compose.ui:ui-text-google-fonts` | Apache License 2.0 | 라이브러리 |

앱 안에 오픈소스 고지 화면을 만들면 둘 다 싣는다.

---

## 검증

```bash
./gradlew :core:design-system:compileDebugKotlin
./gradlew :core:design-system:testDebugUnitTest
ANDROID_SERIAL=<기기 serial> ./gradlew :core:design-system:connectedDebugAndroidTest   # 기기 필요 — 테스트 절
./gradlew ktlintFormat --continue && ./gradlew ktlintCheck && ./gradlew lintDebug
```
