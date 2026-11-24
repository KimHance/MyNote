---
id: design-foundation
name: Design foundation
aliases: [디자인 파운데이션, 디자인 토큰, 토큰, design tokens]
uses: []
units:
  - {id: palette, name: Colours and radius, aliases: [색, 팔레트, 반경, 브랜드 색, StudyNotesDarkColors, 색상 값, 색 추가, 램프 색, MyNoteColors], code: [src/main/java/com/hancekim/mynote/core/designfoundation/token/MyNoteColors.kt, src/main/java/com/hancekim/mynote/core/designfoundation/token/MyNoteBrandColors.kt, src/main/java/com/hancekim/mynote/core/designfoundation/token/MyNoteRadius.kt]}
  - {id: typography, name: Typography, aliases: [타이포, 글자 스타일, 제목 글꼴, 폰트 배율, 본문 글자 크기, 줄 높이, text styles, MyNoteFontScale], code: [src/main/java/com/hancekim/mynote/core/designfoundation/token/Typography.kt, src/test/java/com/hancekim/mynote/core/designfoundation/token/]}
  - {id: adaptive, name: Adaptive tokens, aliases: [반응형 토큰, 창 크기 등급, 너비 높이 클래스, 레일 바 판정], code: [src/main/java/com/hancekim/mynote/core/designfoundation/window/, src/test/java/com/hancekim/mynote/core/designfoundation/window/]}
---
## Overview
Design foundation is the house of the design tokens: the single dark colour palette, the brand lamp colours, corner radii, the 19 text styles, and the window-size classes with the responsive tokens derived from them. It is the only place where colour literals are allowed; everything else reads these names.
It holds values and pure functions only, with no composables, no composition locals, no resources. The theme entry point that puts these values into the composition lives in the design system. The values themselves are listed in the root `DESIGN.md`; the specs here state the rules and do not repeat the tables.
## Structure
- `palette`, `typography` and `adaptive` do not depend on each other except that `typography` takes the height class from `adaptive`.
- The design system's theme calls `adaptive` and `typography` once per window and holds the results; screens read tokens from the theme. The app shell is the one place that measures the window and picks the classes.
- It has no project dependencies, and the design system depends on it, never the other way. The navigation module does not depend on it either.
```mermaid
flowchart LR
  typography --> adaptive
  palette
```
