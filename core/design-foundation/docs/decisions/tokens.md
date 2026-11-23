# Decisions

### RD-D5 · 2026-09 · RD-6, design-foundation
- decision: Layout tokens are the product of five width classes and three height classes, the floating navigation is a right rail from the Expanded width class up or on short windows, and the card grid fills the width from a minimum card width.
- why: Width alone cannot describe a window that is wide but short (a phone on its side): vertical rhythm and the navigation shape follow height, and a bottom bar would eat the body of a short window. A fixed column count makes cards jump on folding, split screen and free windows where width changes continuously.
- rejected: Width classes only; a fixed number of grid columns.
- evidence: `core/design-foundation` `AdaptiveTokens`, `AdaptiveTokensTest` (`짧은 창은 너비가 좁아도 레일을 쓴다`).

### BR-D15 · 2026-10 · BR-8
- decision: The title styles' line height stays at the design value, and a slightly bigger line box on the device font is accepted; line counts stay the same in both fonts.
- why: The font does not change within a process, so the screen does not jump; raising the line height to fit the device font would make Gothic A1 titles loose.
- rejected: Raising the line height to fit the device font.
- evidence: `core/design-foundation/src/main/java/com/hancekim/mynote/core/designfoundation/token/Typography.kt`; `core/design-foundation/src/test/java/com/hancekim/mynote/core/designfoundation/token/TitleFontStylesTest.kt`; `app/src/androidTest/java/com/hancekim/mynote/brand/TitleFontScreensTest.kt`
