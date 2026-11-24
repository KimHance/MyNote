# Decisions

### D-23 · 2026-10 · BM-11
- decision: The added-time text is built by a composable from the added time with `java.time`; "today" is fixed once when the screen enters composition and not updated live.
- why: Resources and the "today" test belong to the screen; the ViewModel only hands over the time, and midnight refreshes on the next visit.
- rejected: Building the strings in the ViewModel through the messages interface (which is for snackbars only).
- evidence: `core/ui/.../screen/bookmarks/BookmarkTimeLabel.kt`, `BookmarkTimeKindTest`.

### RC-D46 · 2026-10 · RC-8
- decision: `토큰 만들기` opens GitHub's token-creation address without a prefilled query, and the guidance does not add `(Contents)`; without a browser a snackbar says so.
- why: The prefilled query could not be verified on the real GitHub page (login required).
- rejected: A prefilled name, permission and expiry link.
- evidence: `ExternalLinkTest`, `core/ui/.../screen/connect/ExternalLink.kt`.

### RC-D49 · 2026-10 · RC-24
- decision: Korean word-boundary line breaking applies only to the text drawn new for the connect work (connect screen, repository card, confirm dialog, home band, empty state, and the notification hint on the settings model card), only to drawn text; accessibility text and content descriptions stay original; the app-wide text style is unchanged.
- why: It prevents regressions on other screens while keeping reading aloud unaffected.
- rejected: Applying it app-wide through the text style.
- evidence: `core/ui/.../screen/connect/KeepWords.kt`, `KeepWordsTest` (`D50 한글 음절 사이에만 결합자를 넣어 어절 중간에서 꺾이지 않게 한다`).

### RC-D62 · 2026-10 · RC-8, RC-25
- decision: External links (`토큰 만들기`, `저장소 만들기`, `형식 안내 보기`) open in a browser app (default browser, then Chrome, then the first browser found), not through other apps that claim the address; without a browser a plain open, and without that a snackbar.
- why: All three pages need web login and other apps (such as the GitHub app) do not have the token or repository creation screens; the buttons promise "opens in an external browser".
- rejected: A plain view intent that lets any claiming app handle the address.
- evidence: `core/ui/.../screen/connect/ExternalLink.kt`, `ExternalLinkTest`.

### FM-D9 · 2026-10 · ui:parts, FM-1
- decision: The shared buttons (five today: primary, secondary, destructive, solid destructive, quiet) and the maximum body width live in `:core:ui`.
- why: Notes, connect, settings and quiz use them together.
- rejected: Raising them to `:core:design-system`, which would bring the DESIGN 13.0 review and a design document change.
- evidence: `core/ui/src/main/java/com/hancekim/mynote/core/ui/screen/` (shared parts), `DESIGN.md`.

### DEC-15 · 2026-10 · ui:parts
- decision: Korean word-level line breaking is done only for the text drawn by the connect feature (connect screen, repository card, confirm dialogs, home band, empty state, and the notification hint on the settings model card): a WORD JOINER (U+2060) is inserted between Hangul syllables at draw time. Accessibility text keeps the original string. The rest of the app still breaks per syllable.
- why: Compose LineBreak.WordBreak.Phrase had no effect on Hangul in measurements, and breaking mid-word read badly in design review.
- rejected: The Compose Phrase word-break setting; applying the joiner app-wide or in a design-system text style (left open).
- evidence: KeepWords.kt in core/ui screen/connect (WORD_JOINER) with KeepWordsTest.

### FM-D3 · 2026-10 · FM-1, ui
- decision: Screen pieces, screen models and cross-feature signal contracts that two or more features use go into a new `:core:ui` module (owner decision).
- why: The design system stays a place that does not know feature meaning such as model state or token input.
- rejected: Putting them into `:core:design-system`.
- evidence: `core/ui/` (`signal/FeatureSignals.kt`, `frame/ShellFrame.kt`, `mapper/NoteBlockMapper.kt`), `notes-tests/test_feature_modules.py` (`FM-1 공용 조각은 core:ui 와 core:navigation 에 산다`).

### FM-D21 · 2026-10 · FM-1, ui
- decision: Five strings used by two or more features and the shared pieces created by the ViewModel split (snackbar wording, failure log, snackbar notice, font-size mapping) move to `:core:ui` in the shared-pieces step; the disconnect dialog model, used only by settings, goes with the settings extraction.
- why: The shared-pieces table was counted at the base commit and missed them; leaving them would make later feature extractions depend on notes (an FM-2 violation).
- rejected: Leaving them in `:feature:notes`.
- evidence: `core/ui/src/main/java/com/hancekim/mynote/core/ui/NotesMessages.kt`, `FailureLog.kt`, `notes-tests/test_feature_modules.py` (`PLACED_AHEAD` now empty).
