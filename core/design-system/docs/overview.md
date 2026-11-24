---
id: design-system
name: Design system
aliases: [디자인 시스템, 디자인시스템, ds, 리퀴드 글래스, 공용 위젯]
uses: [design-foundation, common]
units:
  - {id: theme, name: Theme and motion, aliases: [테마, 모션, 감속 모션, 이징, 애니메이션 줄이기, MyNoteTheme, reduce motion], code: [src/main/java/com/hancekim/mynote/core/designsystem/theme/, src/main/java/com/hancekim/mynote/core/designsystem/motion/]}
  - {id: font, name: Title font gate, aliases: [제목 글꼴, Gothic A1, 글꼴 문], code: [src/main/java/com/hancekim/mynote/core/designsystem/font/, src/test/java/com/hancekim/mynote/core/designsystem/font/]}
  - {id: glass-nav, name: Glass surface and floating navigation, aliases: [유리, 리퀴드 글래스, 플로팅 내비, 하단 바, 레일, 맨 위로 버튼, 상태 알약, 스크롤 숨김, 탭 바 선택 알약, FloatingNav], code: [src/main/java/com/hancekim/mynote/core/designsystem/component/GlassSurface.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/FloatingNav.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/FloatingNavScrollState.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/ScrollToTopButton.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/FloatingStatusPill.kt, src/test/java/com/hancekim/mynote/core/designsystem/component/FloatingNavScrollStateTest.kt]}
  - {id: modifiers, name: Press, rise, flip and fade modifiers, aliases: [눌림 표현, 샤인, 떠오름, 뒤집기, 가장자리 페이드, 연타 막기, 버튼 누름 효과, 등장 애니메이션, PressIndication, riseIn], code: [src/main/java/com/hancekim/mynote/core/designsystem/component/PressIndication.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/AiShine.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/RiseIn.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/Flip.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/FadingEdges.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/ThrottledProcess.kt]}
  - {id: widgets, name: Cards, rows and small widgets, aliases: [노트 카드, 노트 행, 범주 행, 검색창, 칩, 스낵바, 북마크 리본, 이 기기 표시, 번호 행, 섹션 라벨, 검색 입력, NoteCard, 카테고리 칩], code: [src/main/java/com/hancekim/mynote/core/designsystem/component/, src/androidTest/java/com/hancekim/mynote/core/designsystem/component/]}
  - {id: brand, name: Hero, logo and icons, aliases: [히어로, 로고, 워드마크, 아이콘], code: [src/main/java/com/hancekim/mynote/core/designsystem/component/MyNoteHero.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/MyNoteLogo.kt, src/main/java/com/hancekim/mynote/core/designsystem/component/MyNoteIcons.kt, src/test/java/com/hancekim/mynote/core/designsystem/component/MyNoteIconsTest.kt, src/androidTest/java/com/hancekim/mynote/core/designsystem/component/MyNoteHeroTest.kt]}
  - {id: blocks, name: Note body blocks, aliases: [본문 블록, 노트 본문, 서버드리븐 콘텐츠, 표, 목록, 체크박스, 점선 상자, 원문 보기, 표 열 너비, 코드 블록, 인용, NoteContent], code: [src/main/java/com/hancekim/mynote/core/designsystem/block/, src/test/java/com/hancekim/mynote/core/designsystem/block/, src/androidTest/java/com/hancekim/mynote/core/designsystem/block/]}
  - {id: charts, name: Statistics charts, aliases: [차트, 통계 차트, 막대, 도넛], code: [src/main/java/com/hancekim/mynote/core/designsystem/chart/]}
---
## Overview
The design system is the pure UI layer every screen draws with: the theme wiring, the liquid-glass surface and floating navigation, reusable widgets and modifiers, the hero and icons, the note body renderer and the statistics charts. It reads tokens (colours, text styles, adaptive sizes) and does not own them.
It knows nothing of data, domain, navigation or MVI: widgets take plain values and callbacks, note content arrives as its own UI model, shared-element transitions arrive as `Modifier` slots. Values, rules per component and the design to code difference table live in the root `DESIGN.md`; the specs here state behaviour and traps only.
It does not decide when to show things (window class, scroll thresholds, which screen has a nav, what a chart measures); callers do.
## Structure
- `theme` supplies the colours, adaptive tokens, text styles and font scale that every other unit reads, and `rememberReduceMotion` for the units that animate. There is no Material theme.
- `font` decides the title font once per process; the app calls it before building its first screen and hands the result to `theme`.
- `glass-nav` holds the only place that draws the backdrop effect (`GlassSurface`); the bar, rail, scroll-to-top button and status pill are built on it. The app shell owns the `layerBackdrop` box those surfaces sample.
- `modifiers` is the shared motion and press feedback layer; `glass-nav` and `blocks` use `PressIndication`, and `blocks` uses the horizontal scroll fade for tables.
- `widgets` is the catch-all for loose files of `component/`: cards, rows, chips, field, snackbar and the source marks.
- `brand` draws the home hero, the logo vectors and the icon set; `widgets`, `glass-nav` and `blocks` take their icons from it.
- `blocks` renders `NoteContentBlock` lists; feature mappers translate domain blocks into this model one way.
- `charts` are five Canvas charts that draw from a progress function the caller creates with `rememberChartProgress`.
```mermaid
flowchart LR
  glass-nav --> theme
  widgets --> theme
  brand --> theme
  blocks --> theme
  charts --> theme
  glass-nav --> modifiers
  glass-nav --> brand
  widgets --> brand
  blocks --> modifiers
  blocks --> brand
```
