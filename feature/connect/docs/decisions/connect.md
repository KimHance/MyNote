# Decisions

### RC-D53 · 2026-10 · RC-25
- decision: `저장소 만들기` only opens the GitHub template-creation address in an external browser; the app neither creates repositories nor signs in to GitHub, and the user copies the new address back into the link field; there is no clipboard reading, paste button or return deep link.
- why: Owner decision. GitHub has no path that returns to the app after creation, and reading the clipboard is a new read the user does not know about.
- rejected: Creating the repository in the app, a clipboard auto-read, a paste button and a return deep link.
- evidence: `ConnectEntryTest`, `ConnectViewModelTest` (`RC-25 입구가 여는 주소는 생성 주소다`).

### RC-D57 · 2026-10 · RC-9
- decision: The RC-9 box carries a line saying that a repository created just now may need a moment and should be connected again shortly; the app does not retry on its own.
- why: A repository just created from the template can answer as if the list does not exist while GitHub fills the files; an automatic retry would eat the unauthenticated 60 per hour.
- rejected: Automatic retrying of the check.
- evidence: `ConnectEntryTest`, `ConnectProbeTest` (`RC-9 무인증 저장소는 열리고 목록이 없으면 지원하지 않는 형식이다`).

### RC-D59 · 2026-10 · RC-25
- decision: The link section has no section title that appears and disappears with the entry section; the headline, guidance text and field serve as its title, and the entry section has a same-level title of its own.
- why: A title that attaches and detaches with the entry makes the screen jump when `연결` is pressed.
- rejected: A link-section title shown only while the entry section is shown.
- evidence: `ConnectEntryTest`, `feature/connect/.../element/ConnectEntry.kt`.

### RC-D63 · 2026-10 · RC-25
- decision: The entry section's hierarchy is one for every place the connect screen opens: the link section first, the entry section second below it; the side-by-side two-column layout and the first-run-root hierarchy are removed.
- why: The same-weight layout was meant for a person without a repository on the first-run root; that screen no longer exists, and a person who opens the connect screen now has a repository to paste.
- rejected: Same-weight link and entry sections (decisions 55, 58 and 60 of the old document) and a two-column layout.
- evidence: `ConnectEntryTest` (`RC-25 연결 화면의 세 길은 모두 링크 첫째이고 두 단이 없다`).

### DEC-21a · 2026-10 · RC-25
- decision: The entry button that opens the template is named "저장소 만들기" (and the guide button "형식 안내 보기").
- why: "새로 만들기" read as "create a note".
- rejected: "새로 만들기".
- evidence: connect_entry_create in feature/connect strings.xml; "새로 만들기" appears in no strings.xml.
