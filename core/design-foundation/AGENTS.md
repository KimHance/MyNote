# AGENTS.md — :core:design-foundation

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

**토큰의 집.** 색(다크 단일 팔레트)·타이포·radius/spacing·반응형 토큰(`AdaptiveTokens`)이 전부 여기 있고,
`Color(0xFF……)` 리터럴이 허용되는 **유일한 자리**다. 바깥은 값을 다시 만들지 않고 이 이름을 경유한다.

의존은 `androidx.compose.foundation` 하나. 프로젝트 모듈은 아무것도 의존하지 않는 leaf 이고
**`:core:design-system` 을 역으로 의존하지 않는다** — design-system 이 `api(projects.core.designFoundation)`
로 이쪽을 끌어간다. 반대 화살표를 그리면 순환이다.
(`:app` 도 좌표를 선언하지만 실제로 import 하는 곳은 없다. 사실상 소비자는 design-system 하나다.)

## `@Composable` 이 한 개도 없다

여기 있는 건 값(`data class`/`object`)과 순수 함수(`adaptiveTokensFor`, `myNoteTextStyles`,
`fromWidth`/`fromHeight`)뿐이다. 두 가지 결과가 따라온다.

- **테스트가 JUnit4 만으로 돈다.** Robolectric 도 Compose 테스트 룰도 없다(`testImplementation(libs.junit)`
  하나). `Dp`/`TextUnit` 이 inline value class 라 프레임워크를 안 타서 `assertEquals(14.dp, …)` 가 그냥 된다.
  여기에 `Context`·리소스·`@Composable` 을 끌어들이면 그 성질이 깨지고 반응형 로직의 검증이 계측 테스트로 밀린다.
- **그런데 `:core:domain` 처럼 순수 JVM 모듈은 아니다.** `hancenote.android.library.compose` 를 쓴다 —
  `Color`/`Dp`/`TextStyle`/`@Immutable` 이 Compose AAR 에서 오기 때문이다. "순수"의 방향이 domain 과 다르다:
  domain 은 안드로이드를 아예 못 보게 막고, 여기는 **Compose 값 타입까지만** 본다(UI·CompositionLocal·
  컴포저블은 보지 않는다).

## `MyNoteTheme` 은 여기가 아니라 `:core:design-system` 에 있다

`CompositionLocalProvider` 배선과 `MyNoteTheme.colors/textStyles/adaptive/fontScale` 진입점은 design-system 소유다.
그 경계가 뜻하는 것:

- **이 모듈은 값을 만들고, design-system 이 그 값을 컴포지션에 꽂는다.** 그래서 이쪽 함수는 전부
  인자를 받는 순수 함수여야 한다. `LocalXxx.current` 를 읽는 코드를 여기에 넣는 순간 프리뷰와 테스트가
  "MyNoteTheme 으로 감싸세요" 예외로 죽는다(design-system 의 `staticCompositionLocalOf` 기본값이 `error` 다).
- **기존 홀더에 필드를 더하는 건 이 모듈만 고치면 끝난다** — `MyNoteColors` 같은 `data class` 에 색을
  하나 추가하면 `StudyNotesDarkColors` 만 채우면 되고 테마는 홀더를 통째로 들고 있으므로 자동으로 따라온다.
  design-system 을 건드려야 하는 건 **새 종류의 홀더**를 만들어 CompositionLocal 을 하나 더 뚫을 때뿐이다.
- 토큰 홀더는 멤버가 전부 진짜 불변(`Color`/`Dp`/`TextUnit`/`TextStyle`/enum)이라 **`@Immutable`**.
  `MyNoteRadius` 는 `object` + `val` 이라 본래 stable이라서 **어노테이션을 붙이지 않는다.**

## `AdaptiveTokens` — 너비 × 높이 양축

최종 토큰은 **너비 5단계 × 높이 3단계의 곱**이다. `widthTokensFor` / `heightTokensFor` 가 따로 있고
`adaptiveTokensFor` 가 합친다. 새 토큰을 어느 쪽에 넣을지는 "무엇을 정하는 값인가"로 갈린다.

| 축 | 정하는 것 | 토큰 |
|---|---|---|
| **너비** (Compact/Medium/Expanded/Large/ExtraLarge — 600·840·1200·1600dp) | 가로 구조 | `page` `cardGap` `paneWidth` `readingMaxWidth` `cardMinWidth` `useModalDrawer` |
| **높이** (Compact/Medium/Expanded — 480·900dp) | 세로 리듬 | `headerHeight` `padY` `fadeHeight` `heroLogoSize` `heroTitleSize` |

- 경계값은 Material3 의 창 크기 클래스와 같다 — 임의로 고른 수가 아니니 옮기기 전에 그걸 감안한다.
- `fromWidth`/`fromHeight` 는 여기 있지만 **부르는 곳은 한 군데**다. `:app` 셸 `shell/NotesRoute.kt` 의 `NotesScreen` 이
  `LocalWindowInfo.current.containerDpSize` 로 한 번 판정해 `MyNoteTheme` 에 넘기고, 그 아래는 전부
  테마에서 토큰을 읽는다. 화면에서 `BoxWithConstraints` 로 다시 재지 말 것(그건 창이 아니라 부모 제약이다).
- **`null` = "없음"이다.** `readingMaxWidth == null`(너비 Compact/Medium) = 본문 폭 제한 없음,
  `heroLogoSize == null`(높이 Compact) = 히어로 로고를 **그리지 않음**. 소비자가 null 분기를 하게 만든
  설계라 `0.dp` 로 바꾸면 "0 크기로 그리기"가 되어 의미가 사라진다.
- `:core:navigation` 은 이 모듈을 **의존하지 않는다.** 그래서 씬 전략이 `paneWidth` 를 직접 읽을 수 없고
  패널 폭은 `:app` 셸이 `panel` 슬롯 안에서 그린다. 여기에 내비용 값을 더 넣어도 nav 모듈은 못 본다.

### 파생값(레일/바 판정 등)이 화면이 아니라 여기 있는 이유

`useFloatingRail`·`railRightMargin`·`barBottomMargin`·`panelHeaderPadTop`·`noteTitleSize` 는 두 축의
함수라서 계산 프로퍼티로 여기 있다. `useFloatingRail` = 너비 Expanded+ **또는** 높이 Compact
(낮은 창에서 하단 바는 본문을 먹는다).

화면에서 판정하면 안 되는 구체적 이유: 지금 이 술어를 **서로 다른 세 자리가 읽는다** —
`NotesLayout` 이 하단 여백을 예약할지(`FLOATING_BAR_RESERVE`), 같은 파일이 스크롤탑 버튼을 어디 둘지,
`NotesOverlays` 가 레일과 바 중 무엇을 그릴지. 셋이 어긋나면 본문이 바에 가리거나 반대로 빈 여백이 남는다.
소유자가 하나면 어긋날 수가 없다.

순수 함수라서 **`AdaptiveTokensTest` 가 이 계약을 직접 못 박는다** — 높이 경계(480/900), 레일/바 판정
(넓은 창·좁고 높은 창·짧은 창), 모달 드로어는 너비 Compact 만, `readingMaxWidth` 는 Expanded 부터 생김,
`railRightMargin` 하한 14dp, `noteTitleSize == heroTitleSize + 4`, 높이 Compact 에서 히어로 로고 생략.
토큰 값을 고치면 **여기가 먼저 빨개진다.** 의도한 변경이면 테스트를 같이 고친다.

## 타이포는 두 입력을 탄다

`myNoteTextStyles(bodySizeSp, heightClass)` — 한때 `:core:design-system` 에 있었고 `MyNoteHeightClass`
참조가 모듈을 가로지르고 있었다. 정의상 "색/타이포/radius/spacing 토큰"이 이 모듈이라 안으로 들어왔다.

스타일을 변하게 하는 입력이 둘이고, 대상이 겹치지 않는다:

- **사용자 폰트 배율** `MyNoteFontScale`(Normal 16sp / Large 18sp — `NotesViewModel` 의 토글 상태)
  → `body`, `mdHeading2`(+5), `mdHeading3`(+1)
- **높이 클래스** → 짧은 창(높이 Compact)에서 `display` 52→40sp, `screenTitle` 19→17sp

나머지는 고정값이다. 새 스타일을 넣을 때 "짧은 창에서 줄어야 한다"면 `short` 분기를, "본문과 함께
커져야 한다"면 `bodySizeSp` 기반을 쓴다 — 섞으면 둘 다 아닌 값이 된다.

크기가 아니라 글꼴을 바꾸는 셋째 인자 `titleFontFamily`(기본 기기 글꼴)는 제목 글꼴 스타일 넷(`TitleFontStyleNames` —
noteTitle · title · sectionTitle · screenTitle, 정본은 `docs/design/brand.md` 의 제목 글꼴 표)에만 간다(`brand` BR-8). 넷은 줄 높이와
`LineHeightStyle(Center, Trim.None)` 을 고정한다 — Gothic A1 은 줄 상자가 줄 높이 그대로다. 기기 글꼴은 한글이 대체 글꼴로 그려져
그 높이(약 1.45em)가 줄 높이보다 크면 그만큼 남는다(에뮬레이터 실측: `title` 한 줄 34.9 · Gothic A1 31.9dp). 줄 수는 같다. 글꼴 값은 `:core:design-system` 이 받아 넘긴다(이 모듈은 받는 법을 모른다). `TitleFontStylesTest` 가 목록 밖
스타일이 글꼴 인자와 무관하게 그대로인지 본다. 목록에 스타일을 더하거나 빼면 디자인 문서 표도 같이 고친다(`notes-tests/test_brand_font.py`).

- **한 화면 전용 타이포 스케일을 그 화면에 두지 않는다.** 퀴즈가 자기 스케일 11종을 들고 있다가
  여기로 합쳐졌다. 화면 하나가 디자인 시스템을 우회하면 나머지 화면과 조용히 어긋나고, 같은 값이
  두 이름으로 존재하게 된다(합칠 때 `failTitle` 은 `sectionTitle` 과 값이 같아 그대로 흡수됐다).
- **히어로/노트 제목 크기만 타이포가 아니라 `AdaptiveTokens` 의 `TextUnit` 이다**
  (`heroTitleSize`/`noteTitleSize`). 화면이 `textStyles.noteTitle.resized(adaptive.noteTitleSize)`
  로 합쳐 쓴다 — `copy(fontSize = …)` 만 하면 고정된 sp 줄 높이를 그대로 받아 큰 크기에서 줄이 붙는다. `resized` 가 줄 높이를 같은 배율로 맞춘다. 이름 붙은 스타일은 타이포에, 폼팩터에 따라 연속적으로 변하는 크기는 반응형 토큰에 —
  라는 분담이다. `heroTitleSize` 는 히어로 워드마크 그림의 x높이로도 읽힌다(design-system `MyNoteHero` 가 값을 dp 로 —
  글자 배율 무시). 값을 바꾸면 워드마크 크기가 같이 바뀐다.

## 색

`MyNoteColors` 는 다크 단일 팔레트(`StudyNotesDarkColors`) 하나뿐이다. 라이트 테마가 없다는 전제 덕에
알파가 섞인 값(`glass` `scrim` `navItem*`)을 그대로 안전하게 쓴다 — 대안 팔레트를 추가하면 그 전제가 깨진다.

- **유리 표면은 색 하나가 아니라 묶음이다.** `glass`(내비) / `glassPanel`(다이얼로그 — 블러 세기가 달라
  따로 있다) / `glassOpaque`(`GlassSurface` 에 `backdrop` 이 없을 때의 불투명 폴백) / `glassBorder`,
  그리고 선택 알약용 `navItemSelectedTop`·`Bottom`·`Rim`·`Specular`·`Caustic`·`Pressed` 6종.
- `correct`/`wrong` 은 **색만으로 말하지 않는다**는 전제로 만들어졌다(아이콘·글자와 함께 쓴다).
  `warn`/`warnText` 는 실패가 아니라 "요청보다 적게 만들어짐" 안내라서 빨강이 아니다.
- **로고 전용 색은 팔레트 밖이다** — `token/MyNoteBrandColors.kt` 의 `object MyNoteBrandColors`(`LampTop` · `LampBottom`).
  히어로 마크의 빛 그라디언트만 읽는다. UI 면 · 글자 · 상태에 쓰지 않고, Kotlin 이 읽는 색만 둔다(밤 · 아이콘 바탕은 `:app`
  색 리소스). `MyNoteColors` 에 넣으면 토큰처럼 읽혀 화면에 새어 나간다.
- **안 쓰는 토큰은 남기지 않는다.** 팔레트가 유일한 출처라서 쓰이지 않는 이름도 "있는 값"처럼 읽히고,
  다음 사람이 그걸 근거로 쓴다(과거에 `navItemSelected`·`scrimTop`·`gesturePill` 을 정리한 이유).
