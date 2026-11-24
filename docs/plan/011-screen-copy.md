# Screen copy

Source: old feature document `docs/spec/screen-copy.md` (v3) as of commit c8c54350; work done 2026-10-11 … 12.

## Plan
- why: Wording came in from many pieces of work and called one thing by several names (bringing notes in was fetch, receive, import, sync, apply), with translated-sounding, passive, noun-stacked and machine-sounding phrasing; the owner decided to polish all screen text once without changing meaning or behaviour.
- flow: A designer reads all strings and writes the term table, allowed endings, forbidden expressions and the changed-wording table in the design document; development changes `strings.xml` values and what is bound to them, keeping resource names; tests guard tone, forbidden expressions and dropped terms; the changed rows are shown to the owner afterwards, with no approval before applying.
- not doing: No other languages, no layout changes, no edits to note content or repository documents (quoted wording is only aligned), no resource renames (done in 012-feature-modules), no model-only prompts for summaries and patterns, no rename of notification channels (the user's notification settings keep the old name).

## Items
| ID | What | Accept |
|---|---|---|
| SC-1 | One speaking register: the polite formal style; every sentence ending of the in-scope strings is on the design document's allowed-endings list | Python `test_screen_copy.py`: `SC-1 화면 문구의 끝말은 허용 목록에 있다`; no ending outside the list except named exceptions |
| SC-2 | No forbidden expression (exclamation marks, emoji, "click", internal terms, vague error sentences, added translationese, passive, habits) appears in any in-scope string | Python: `SC-2 화면 문구에 쓰지 않는 표현이 없다`; date-range dashes between format slots are not counted |
| SC-3 | The same concept has the same word: no "dropped word" of the design document's term table appears; fetching notes and getting the model use different words | Python: `SC-3 용어 표에서 버린 말이 화면 문구에 없다`; frozen exceptions are named in the term table |
| SC-4 | Translationese, noun stacks and machine-sounding habits are removed; every changed row has before, after, place and reason, and the after text passes SC-1 to SC-3 | Python: `SC-4 바꾼 문구 표의 값이 화면 문구에 있다`; strings not in the table equal the base commit (one-off diff at the time, no test) |
| SC-5 | Meaning is unchanged: a changed string keeps the number, order and kind of its format slots, array item counts are equal, and no string is deleted | Python: `SC-5 바꾼 문구는 형식 자리가 그대로다`; `./gradlew lintDebug` shows no format warning; array item counts and no deletion by the SC-K1 comparison |
| SC-6 | Wording follows the sentence shape of DESIGN 9.4 (state labels and snackbars without a period, notices with one, buttons as short verbs) and one-line places do not get longer | Python: `SC-6 버튼 라벨 한 줄 자리는 글자 수가 늘지 않는다` plus review of the place column |
| SC-7 | Spoken (screen reader) wording is comma-joined facts without screen symbols and does not read the visible text twice | Review of the changed-wording table; manual TalkBack on home card, bookmark row, category row and quiz choice |
| SC-8 | Everything bound to the wording changes together: the template README's seven app terms and quoted sentences, DESIGN.md 9.2 and 9.4, the public spec and its plugin copy, quoted wording in feature documents, the design documents' wording tables | Python `test_screen_copy.py`: `SC-8 템플릿 README 가 인용한 앱 문장이 화면 문구에 있다`; `notes-tests`, `public-repos/tests` and `./gradlew test` pass with unchanged test names |
| SC-9 | Changed wording does not overflow: at width 360dp and system font 1.3x button labels are one line with no ellipsis, at 2.0x and large app text size ellipsis places do not increase against the before-capture | Manual before/after captures per screen with changed wording; `TitleFontScreensTest` (BR-8) passes; the quiz-settings subtitle and a collapsed notification are compared relatively at 1.3x |
| SC-K1 | Resource names and their count do not change within the wording step | Command: name-set comparison with the base commit, difference 0 |
| SC-K2 | App name and screen titles (`MyNote`, settings, bookmarks, statistics, past summaries) stay | `notes-tests/test_brand_title.py` (BR-7) |
| SC-K3 | Token guide words (the GitHub screen's own names) and the notification channel name stay | `public-repos/tests` (NT-5), the frozen list in the design document |
| SC-K4 | The meaning of the common action strings (`ds_action_*`) stays | Review of the changed-wording table |
| SC-K5 | The box shown for a note that could not be read still takes its limit numbers as format slots | Existing note-format tests and SC-5 |
| SC-K6 | The behaviour described in feature documents does not change, only quoted wording | The change line of each edited feature document says quoted wording only |
