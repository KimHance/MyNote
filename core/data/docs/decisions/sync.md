# Decisions

### SY-D1 · 2026-10 · SY-K1
- decision: Format errors are contained per note: an unknown block type or missing field only turns that block into a box with `원문 보기`, a file that is not JSON stays listed with the body as one box, and an unknown inline style reads as plain text.
- why: One bad note must not stop the whole sync, and content may be published before the app learns a new block.
- rejected: Failing the whole sync on the first note that cannot be read.
- evidence: `core/data/.../repository/NotesReconcileTest` (`형식 오류 노트는 격리하고 나머지는 반영한다`, `JSON 이 아닌 노트도 목록에 남고 본문은 BrokenNote 다`).

### SY-D2 · 2026-10 · SY-K2
- decision: A transfer error is atomic per source: if the list or any body cannot be fetched, none of that source's changes is applied.
- why: Deleting based on a half-received list would remove notes that are fine.
- rejected: Applying whatever was received and deleting the rest.
- evidence: `NotesReconcileTest` (`본문 전송이 하나라도 실패하면 아무것도 반영하지 않는다`, `index 를 해석하지 못하면 아무것도 반영하지 않는다`).

### SY-D4 · 2026-10 · SY-K3
- decision: Delete protection: when a source's list is empty but the source still has notes, nothing is deleted and the sync fails.
- why: A temporarily empty answer must not wipe everything.
- rejected: Also asking for confirmation when 50 percent or more disappear at once (deferred to the roadmap, still open).
- evidence: `NotesReconcileTest` (`소스 index 가 비었는데 그 소스 노트가 남아 있으면 지우지 않고 실패한다`).

### SY-D6 · 2026-10 · SY-3
- decision: A sync without a connection is not a failure: no snackbar, no error, and it never creates or revives a source.
- why: A failure over the connect screen on first run would be wrong, and a sync that revives a connection after the user disconnected means disconnecting did not work.
- rejected: Reporting a failure when there is no connection, and a sync that recreates the source on its own.
- evidence: `ConnectionRepositoryTest` (`SY-3 연결이 없으면 요청도 오류도 없다`, `SY-3 동기화는 소스를 만들지 않는다`), `DeviceNotesTest`.

### NT-D28 · 2026-10 · NT-6
- decision: The check that the template examples hold no text-only syntax runs in two places: raw lines (outside fenced code and code spans) for links, images, HTML and autolinks, and interpreted text (outside code shapes) for math, highlight, footnotes and alerts. The examples never write a `$` or `==` outside code shapes, escaped or not.
- why: Links, images and HTML vanish when interpreted (a link becomes plain text, an image alt text, HTML a code block), so only the raw source shows them; math, highlight, footnotes and alerts stay as text, so the interpreted text shows them. An escaped dollar sign, as the writing rules ask, comes back as a plain dollar sign and would look like a trace.
- rejected: Checking the interpreted text only.
- evidence: `core/data/src/test/java/com/hancekim/mynote/core/data/template/TemplateNotesTest.kt` (`NT-6 템플릿 예시에 글자로만 보이는 문법이 없다`)

### DEC-1 · 2026-10 · SY-K1
- decision: Note body parsing is not done inside the network layer. The GitHub manifest adapter hands back the body as a raw string; parsing happens on the data side (today through the single entry point of the format module), so a format error stays a per-block or per-note problem and never becomes a transport error.
- why: With parsing inside the Retrofit converter, a format error threw the same exception as a transfer failure and hit the rule "one failed body means nothing is applied". One unknown inline style then stopped sync on every device.
- rejected: Parsing in the network converter, throwing on malformed notes.
- evidence: GithubManifestAdapter in core/network returns a String body; NoteFormat in core/format is the only parser entry; core/data tests NotesReconcileTest ("format-error note is isolated, the rest is applied") and MyNoteJsonFormatTest.

### DEC-25 · 2026-10 · NF-2
- decision: Note format interpretation lives in the pure JVM module core/format behind one entry point (NoteFormat parse, derive, read with path and raw text). The path extension picks the format: .md goes to the JetBrains markdown parser with GFM, anything else to JSON format 1. The entry never throws. Only core/data depends on core/format.
- why: The engine dependency must not leak into data or feature modules, and four call sites (sync, reconversion, connection check, cache read) choosing the format separately would let one of them miss Markdown.
- rejected: Format choice and parsing kept inside core/data, or chosen separately at each call site.
- evidence: core/format NoteFormat.kt; core/data AGENTS and module dependency checks; JSON notes are still read.

### DEC-36b · 2026-10 · SY-4
- decision: The guard between disconnect and a running sync checks only that the source row still exists before writing; it does not compare the source settings. Start-up recovery removes notes whose source row is gone.
- why: Comparing settings would block the first sync of a new connection, which runs with the new settings.
- rejected: Comparing the full source configuration before applying.
- evidence: SourceReconciler.reconcile returns an empty outcome when applySourceSync reports the source is gone; ConnectionRecovery.
