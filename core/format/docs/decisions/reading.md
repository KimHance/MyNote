# Decisions

### NF-D16 · 2026-10 · NF-12
- decision: JSON originals are read only as the closed set of format 1. The cache shape is a superset and the two shapes are kept apart.
- why: If the JSON reader accepted a new block or field the moment the cache gained it, the closed set would leak.
- rejected: One shared shape for the JSON wire format and the cache.
- evidence: `core/format/src/main/java/com/hancekim/mynote/core/format/WireDto.kt`, `CacheDto.kt`; `core/format/src/test/java/com/hancekim/mynote/core/format/CacheCompatTest.kt` (`NF-12 JSON 원문의 형식 1 밖 type 과 필드는 지금 판과 같게 읽힌다`)

### NF-D17 · 2026-10 · NF-2
- decision: One entry point (path and original) picks the format, every place that turns an original into blocks uses it, and it never throws on any input.
- why: Several places choosing the format would let one miss and freeze a `.md` note as JSON; the app-start re-conversion runs outside source isolation, so one row's exception would block sync forever.
- rejected: Letting each call site pick the format.
- evidence: `core/format/src/main/java/com/hancekim/mynote/core/format/NoteFormat.kt`; `core/format/src/test/java/com/hancekim/mynote/core/format/NoteFormatTest.kt` (`NF-2 진입점은 어떤 입력에도 던지지 않는다`)

### NF-D19 · 2026-10 · NF-11
- decision: The parity material (22 JSON notes before the move, 22 Markdown notes after it, and the reference list) is kept as fixed copies. The byte comparison with the live `notes/` was done once at the migration commit.
- why: A test that compares live notes with fixed values becomes false from the next note written.
- rejected: Comparing against the live `notes/` on every run.
- evidence: `core/format/src/testFixtures/resources/parity`; `core/format/src/test/java/com/hancekim/mynote/core/format/ParityTest.kt`

### NF-D37 · 2026-10 · NF-2, NF-8
- decision: As the entry point's last defence, an out-of-memory error becomes that one note's over-limit box. The limit check always runs first and exceptions never stand in for a limit. The 512KB original limit stays. The box stays until the content changes or the parse version rises.
- why: One pathological 512KB input within the limits builds engine and block trees using 96 to 128MB of heap (measured on a desktop JVM; the emulator's growth limit is 192MB). If not caught, that note makes every sync fail. Parsing it again would use the same memory.
- rejected: Lowering the 512KB original limit (left for a measurement on a device); relying on exceptions alone instead of limits.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/NoteFormatTest.kt` (`NF-2 메모리가 모자라면 그 노트만 한도 넘음 상자다`); `core/data/src/test/java/com/hancekim/mynote/core/data/repository/NoteFormatSyncTest.kt`

### DEC-26 · 2026-10 · NF-12
- decision: The DTOs that read JSON notes (WireDto.kt) are frozen at the closed set of format 1. A separate cache DTO (CacheDto.kt) stored in Room carries what Markdown adds (italic, strike, children, check state, start number, tables). A new JSON block means a new format 2, never an extension of format 1.
- why: One shared DTO would let each new block widen the JSON closed set and silently change what old apps read.
- rejected: One DTO for wire and cache.
- evidence: WireDto.kt, CacheDto.kt and BlockCache in core/format; CacheCompatTest and NoteDtoSerializationTest pin the cache bytes of the eight old blocks.

### DEC-28 · 2026-10 · format:note-format
- decision: Only core/format applies the java-test-fixtures plugin directly (an exception to "modules apply hancenote.* plugins only"). The parity fixtures (22 JSON, 22 Markdown, reference list) sit in its testFixtures and the data module's tests read them from the classpath.
- why: A convention plugin for a single user is overkill, and a file path shared by two modules is invisible to Gradle as an input.
- rejected: A new convention plugin; reading the files by path from the other module.
- evidence: core/format/build.gradle.kts applies java-test-fixtures; core/format/src/testFixtures ParityNotes.kt; it is the only such module. If a second module needs fixtures, promote it to a convention plugin.
