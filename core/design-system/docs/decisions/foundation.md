# Decisions

### BR-D4 · 2026-10 · BR-8
- decision: Only the title-style group uses Gothic A1, as a downloadable font. The body stays on the device font, and when the font cannot be had the title styles also use the device font.
- why: Owner decision. The APK does not grow.
- rejected: Changing the body (paragraph, list, table, code) font; putting font files in the APK.
- evidence: `core/design-system/src/main/java/com/hancekim/mynote/core/designsystem/font/TitleFontGate.kt`; `core/design-foundation/src/test/java/com/hancekim/mynote/core/designfoundation/token/TitleFontStylesTest.kt`; `notes-tests/test_brand_font.py` (`BR-8 앱 소스에 글꼴 파일이 없다`)
