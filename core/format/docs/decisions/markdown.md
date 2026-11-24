# Decisions

### NF-D6 · 2026-10 · NF-1
- decision: The note id is the file name, the group is the folder name, and title and date come from the front matter. A `#` heading in the body is a body block, not the title field.
- why: A value has one source, as the group already comes from the folder name; the generator and the app apply the same rule.
- rejected: Taking the title from the first body heading.
- evidence: `core/format/src/main/java/com/hancekim/mynote/core/format/NoteFormat.kt`; `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownConverterTest.kt` (`NF-1 앞머리는 블록이 되지 않는다`)

### NF-D7 · 2026-10 · NF-7, NF-3
- decision: A Markdown note never becomes a raw-text box. Syntax the app cannot draw shows as text (the mapping tables of NF-3 to NF-7). The box is only for a note whose parsing failed or went over a limit.
- why: Any text is valid Markdown; the box only means something when it says "this app could not read it".
- rejected: Turning unsupported syntax into a raw-text box.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownConverterTest.kt` (`NF-7 글자로 보이는 문법 표`)

### NF-D8 · 2026-10 · NF-4, NF-5, NF-6
- decision: Tables are a new block, italic and strike-through are new text shapes, and a list item can hold sub-blocks (sub-lists and the like).
- why: The table is an owner decision. Showing strike-through as plain text would reverse the meaning, and sub-lists are common in study notes.
- rejected: Showing strike-through as plain text.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownConverterTest.kt` (`NF-4 글자 모양 표`, `NF-5 표 블록`, `NF-6 하위 블록`)

### NF-D9 · 2026-10 · NF-7
- decision: HTML is not interpreted: inline HTML stays as its own text, and an HTML block shows its original as a code block.
- why: The app draws no HTML, and text such as `Box<String>` must not disappear.
- rejected: Interpreting or stripping HTML.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownConverterTest.kt` (`NF-7 글자로 보이는 문법 표`)

### NF-D10 · 2026-10 · NF-7
- decision: Links and images show as text only: link text, and alt text for images.
- why: Opening links and fetching images are new features tied to external opening and private-repository authentication. The original stays in the cache, so a later change needs no new download.
- rejected: Opening links and drawing images in this work.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownConverterTest.kt` (`NF-7 글자로 보이는 문법 표`)

### NF-D11 · 2026-10 · NF-8
- decision: Notes are screened by limits before parsing. Container nesting over 32 levels (quote marks and list markers at line start) or an original over 512KB skips the engine and the whole note becomes an over-limit box. Nesting the screen misses is caught again by block depth during conversion and gives the same box. The drawn indent stops at the design limit of three levels.
- why: Engine memory grows quickly with nesting depth, so one 20KB note can exhaust memory, and serialisation overflows the stack at 200 nested levels. Catching exceptions cannot prevent either.
- rejected: Relying on catching exceptions after parsing.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/NoteFormatTest.kt` (`NF-8 한도를 넘는 원문은 엔진을 부르지 않는다`); `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownLimitsTest.kt` (`NF-8 32단은 읽고 33단은 한도 넘음이다`)

### NF-D12 · 2026-10 · NF-8
- decision: Parsing has no time limit; only the size, depth, table-cell and cache limits screen a note.
- why: A time-out box gives different results on different devices; every measured input within the limits finished within 1.3 seconds.
- rejected: A time deadline that turns slow notes into a box.
- evidence: `core/format/src/main/java/com/hancekim/mynote/core/format/MarkdownLimits.kt`; `core/data/src/test/java/com/hancekim/mynote/core/data/repository/MarkdownSyncTest.kt` (`NF-8 한도 안의 큰 입력도 동기화를 멈추지 않는다`)

### NF-D22 · 2026-10 · NF-8
- decision: A line whose prefix (markers and indentation) can hold more than 64 container shares is an over-limit note. One marker or two spaces of indentation is one share; 32 levels of four-space or tab indentation pass.
- why: Nesting made of indentation on lines that cannot end a paragraph is missed by line joining alone.
- rejected: Screening by joined-line nesting alone.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownLimitsTest.kt` (`NF-8 줄 앞부분이 품을 수 있는 컨테이너가 64 를 넘으면 한도 넘음이다`)

### NF-D26 · 2026-10 · NF-7
- decision: Footnotes, both the definition line (`[^n]: ...`) and the reference in the body, show as original text.
- why: The engine drops or keeps the same syntax depending on the number of words, which would lose characters of notes in other repositories.
- rejected: Leaving the engine's behaviour, where the same syntax can vanish or stay.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownConverterTest.kt` (`NF-7 글자로 보이는 문법 표`)

### NF-D27 · 2026-10 · NF-8
- decision: The limit screen also counts leading spaces and markers on lines inside fenced code (a known false positive).
- why: If the screen knew fences, input where the screen and the engine disagree would let deep nesting reach the engine (NF-D11).
- rejected: A fence-aware screen.
- evidence: `core/format/src/test/java/com/hancekim/mynote/core/format/MarkdownLimitsTest.kt`

### LN-D12 · 2026-10 · LN-3
- decision: A device note's raw text is stored as an empty front matter (two `---` lines, 8 bytes) plus the body, so the body size boundary is 524,280 bytes and loading a note for editing strips the first 8 bytes.
- why: A body whose first line is `---` stays a body, and the reader reads it through the same entry as repository notes (NF-2).
- rejected: Adding a blank line only to bodies whose first line is `---`, which makes the way back ambiguous when the body starts with a blank line and `---`.
- evidence: `core/format/src/main/java/com/hancekim/mynote/core/format/DeviceNoteMarkdown.kt`, `core/format/src/test/java/com/hancekim/mynote/core/format/DeviceNoteMarkdownTest.kt`, `feature/editor/src/test/java/com/hancekim/mynote/feature/editor/Utf8SizeTest.kt`.

### DEC-33a · 2026-10 · NF-5
- decision: Tables are drawn as a full cell grid (outer border, 1dp lines between all cells, header on the second surface colour).
- why: Picked from two design candidates (grid and horizontal rules); no further reason is recorded; the known weakness is that inline code in a header cell blends into the header colour.
- rejected: Horizontal rules only (candidate B).
- evidence: NoteBlockMapper handles tables in core/ui; docs/design/note-format.md section on the table lists the grid values and records B as not chosen.

### NT-D4 · 2026-10 · NT-1
- decision: The format spec's source of truth is `public-repos/notes-kit/SPEC.md`. The template README and the plugin skills point to it and never copy it. The project's own internal spec, generator and hook stay untouched by this work; tests compare the closed sets between the two instead.
- why: Only tracked documents are the app feature documents, and a spec that outsiders read must live in the public repository. Changing the internal scripts at the same time would disturb writing this project's own notes.
- rejected: Copying the spec into the README and skills; replacing the internal spec, generator and hook with the public ones (merging is left for after publication).
- evidence: `public-repos/notes-kit/SPEC.md`; `public-repos/tests/test_notes_parity.py` (`NT-1 규격의 닫힌 집합이 gen_index.py 와 같다`), `public-repos/tests/test_schema_parity.py` (`NT-1 규격의 문법 표와 한도와 쓰기 규칙이 SCHEMA.md 와 같다`)

### NT-D22 · 2026-10 · NT-1
- decision: The public `SPEC.md` describes Markdown notes as the source of truth (front matter, syntax mapping, text shapes, text-only syntax, limits, writing rules, `index.json`, format version) and keeps old JSON notes as a format-1 section with the closed-set tables. Against the internal `notes/SCHEMA.md` the test compares not sentences but the first column of the tables, the limit numbers and the number of writing rules.
- why: The public spec is in polite Korean and the internal one in plain Korean, so they cannot be byte-equal; the cells and numbers must match whatever the tone, and a change on either side breaks the test.
- rejected: Byte-equal copies; merging the two documents into one (still an open question).
- evidence: `public-repos/tests/test_schema_parity.py` (`NT-1 규격의 문법 표와 한도와 쓰기 규칙이 SCHEMA.md 와 같다`)
