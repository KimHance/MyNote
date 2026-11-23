---
units: [format:markdown, format:limits, format:device-note]
plans: [009-note-format, 013-local-notes]
decisions: [NF-D6, NF-D7, NF-D8, NF-D9, NF-D10, NF-D11, NF-D12, NF-D22, NF-D26, NF-D27, LN-D12, DEC-33a]
---
## markdown — Markdown converter
### Behaviour
- Converts a Markdown original (CommonMark with GitHub tables, task items and strike-through; no maths and no highlight) into the block kinds the reader draws. It runs only on text that passed `limits`.
- Front matter is cut first. A leading BOM is dropped and CRLF and lone CR become LF. If the first line is exactly `---` and a later line is exactly `---`, the lines between them are front matter and never become a block. A boundary with spaces around it is not a boundary. With no closing line the whole text is body (009-note-format: NF-1).
- The converter reads only the boundary of front matter, not the lines inside; title and date of a note come from the list entry (009-note-format: NF-1).
- Headings 1 to 6 come from `#` lines and from setext underlines (`===` is level 1, `---` under a paragraph is level 2). Paragraphs are one block each; a blank line makes no block and there is no spacer. `---`, `***` and `___` are dividers (009-note-format: NF-3).
- Fenced code keeps its lines, drops the fence indent, and keeps no language name; an unclosed fence is code to the end, without the last line break. Indented code drops four columns. An HTML block becomes a code block with its original lines and ends at a blank line (009-note-format: NF-3, NF-7).
- A quote (and a GitHub alert) is one quote block of text lines joined with a line break. Content that is not a paragraph is flattened to lines: headings and list items one line each, checkbox items prefixed `[x] ` or `[ ] `, code and HTML lines verbatim, table rows as cells joined by ` | `; dividers, link definitions and empty fences are dropped (009-note-format: NF-3).
- An alert marker such as `[!NOTE]` stays as the first line of the quote text (009-note-format: NF-7).
- Bullet and numbered lists keep their kind. An item's first paragraph is its text; paragraphs, code, quotes, tables and sub-lists after it are the item's sub-blocks. An item whose first child is not a paragraph has empty text and keeps that child as a sub-block (009-note-format: NF-6).
- A numbered list keeps the number of its first item as its start (`3.` gives 3, `9)` gives 9). Task items carry on or off (`[x]` and `[X]` are on), in bullet and numbered lists (009-note-format: NF-6).
- A table has a header row, body rows and one alignment per column from the delimiter row (`:--` left, `:-:` center, `--:` right, otherwise none). A short row is padded with empty cells and extra cells are dropped. `\|` is a literal bar, also inside a code span. A leading `>` in a cell is text (009-note-format: NF-5).
- Text shapes are bold (`**`, `__`), italic (`*`, `_`), strike-through (`~a~`, `~~a~~`) and code, which overlap freely (`***a***` is bold italic). A code span takes only strike-through. Inside a quote the italic mark is still kept (009-note-format: NF-4).
- Code span content is verbatim: no escapes or entities are decoded, a line break inside becomes a space, and one space on both ends is removed when the text is not blank (009-note-format: NF-4).
- A soft line break is a space and spaces before it are dropped. A hard break (two trailing spaces or a backslash) is a line break. A paragraph's outer spaces, tabs and breaks are trimmed, but a no-break space or an ideographic space is text (009-note-format: NF-4).
- Escapes (a backslash before ASCII punctuation) give the character. Entities decode by name or number: decimal up to 7 digits, hex up to 6, names of up to 32 letters and digits; zero, a surrogate or a value past the last code point gives U+FFFD; an unknown name stays as written. Names that stand for two code points (93 of them, such as `&fjlig;`) are decoded from the module's own table (009-note-format: NF-7).
- Syntax the app does not draw becomes text and never a box: a link shows its text (address dropped, inner shapes kept); an image shows its alt text and, with empty alt, no block; an autolink or bare address shows the address; inline HTML stays as written (009-note-format: NF-7).
- Brackets that are not a link stay verbatim with their brackets: a reference with no definition (`a[i]`, `Map[K, V]`), or `[a](b c)` with a plain space or control character in the address outside angle brackets. A no-break space or ideographic space in an address does not break the link (009-note-format: NF-7).
- `$...$`, `$$` blocks and `==...==` return to their source text (a `$$` block is one paragraph `$$ x $$`). Footnotes are not drawn: a footnote definition line becomes a plain text paragraph and `[^n]` stays text. Link definition lines are invisible (009-note-format: NF-7).
- Block depth is counted while walking: list sub-blocks nest one level each; more than 32 gives one whole-note over-limit box holding the original (009-note-format: NF-6, NF-8).
- Table cells are summed over the whole note as header columns times (body rows plus one) before padding; more than 4,096 gives one whole-note over-limit box. The count is per note so many small wide tables cannot sneak past (009-note-format: NF-5, NF-8).
- There is no parse deadline and no partial result: a note is converted whole or becomes one box (009-note-format: NF-8).
### Public API
- None — the converter, the span writer, the flavour and the entity table are internal; the `MarkdownConverter` interface that callers can replace lives in `note-format`.
### Guarantees
- The syntax table, quote flattening, unclosed fences, text shapes, tables, sub-blocks, task items, start numbers and every text-only syntax with zero boxes are asserted against the real engine: `MarkdownConverterTest`.
- CRLF text gives the same blocks as LF text, and the shared front-matter boundary folder gives the same verdict as the index generator: `MarkdownConverterTest`.
- Deep emphasis and brackets read without a box, and the block depth never passes 32: `MarkdownConverterTest`.
### Not covered
- Screening before the engine runs — see `limits`.
- Cache bytes, the first sentence and the choice of Markdown over JSON — see `note-format`.
- Drawing: link opening, images, code colouring, column widths, strike-through announcements, indent depth — see the design system and `notes`.
- Writing rules for authors (no setext, no `---` right after a paragraph, escaping `<` `~` `*` `_` `$` `==` `|`) — see the notes schema document, not code here.
### Unconfirmed
- The 93 two-code-point entity names are a fixed table; no test lists all 93 (only three are asserted).

## limits — Limits
### Behaviour
- All limit values come from the domain limit set: original 512 KB, container nesting 32, block depth 32, table cells 4,096 per note, cache row 1 MB. The module holds no limit numbers of its own beyond the doubled line-prefix cap below, and the box wording takes its numbers from the same set (009-note-format: NF-8).
- Screening runs on the raw text before front matter is cut and before the engine is called, for Markdown originals only. A note that fails it becomes one `limit_exceeded` box holding the original; the engine is never called for it (009-note-format: NF-8).
- Size: more than 524,288 UTF-8 bytes is over; exactly 524,288 passes. It is counted without building a byte array, and a text with more characters than the limit is over at once (009-note-format: NF-8).
- Nesting: containers at the start of each line (`>` quote marks and list markers) are counted across lines the way CommonMark keeps containers open. More than 32 is over; 32 mixed quote and list levels pass and 33 do not (009-note-format: NF-8).
- A list marker is `-`, `+` or `*`, or digits followed by `.` or `)`, then a space, a tab or the end of the line. A tab is four columns, one to four spaces after a marker belong to the item (more than four count as one), and LF, CRLF, CR and a leading BOM end lines or are ignored as in Markdown (009-note-format: NF-8).
- A line whose rest is a divider (three or more of the same `-`, `*` or `_` with spaces between) is not a list marker, as in CommonMark. A text line that continues a paragraph keeps the open containers open, which counts high rather than low (009-note-format: NF-8).
- Line prefix: markers plus indentation columns divided by two over 64 is over, even when the lines would not continue each other. The scan cannot see paragraph state, so this cap bounds the real depth at 64 or less in the cases it misses (009-note-format: NF-8).
- Screening time is proportional to line length: it stops as soon as a cap is passed and decides divider lines once per line, so one 512 KB line of markers is screened quickly (009-note-format: NF-8).
- Known false positive: the scan does not know fenced code, so a code example with 40 `>` marks, a 130-column indent or a 34-level list makes the whole note over-limit. The other choice would let deep nesting reach the engine, so it is kept as the safe side.
- Catching exceptions is not the limit: engine memory grows fast with nesting (a 20 KB note can exhaust it) and serialising very deep sub-lists overflows the stack, so deep input must be stopped before the engine (009-note-format: NF-8).
- Out of memory is a last defence only: an in-limit pathological 512 KB input can need 96 to 128 MB of heap in conversion; if it runs out, that one note becomes an over-limit box and other notes are applied. The box stays until the content or the parse version changes (009-note-format: NF-8).
- Beyond screening, three limits apply after conversion: block depth over 32, table cells over 4,096 and cache over 1 MB. Each gives one whole-note over-limit box. The depth check covers lines the scan lets through, such as ten-digit numbers that the engine does not treat as list markers (009-note-format: NF-8).
- An over-limit box stores no original in the cache and the list never reads originals. A row inside the limits, with its original and its cache, stays under the 2 MB database cursor window (009-note-format: NF-8).
- A note with a very wide table, where a short original would become millions of cells, is stopped by the table-cell count, not by size (009-note-format: NF-8).
### Public API
- None — screening functions are internal and are called only from `note-format`.
### Guarantees
- 32 and 33 levels (spaces after markers, tabs, quote and list mix), 512 KB boundary, CR and BOM, non-container lines, dividers and the line cap: `MarkdownLimitsTest`.
- A 512 KB single line of markers is screened in time proportional to its length: `MarkdownLimitsTest`.
- Wide-table growth, table cells counted per note, cache cap boundary and worst-case cache size for in-limit inputs: `ResultLimitsTest`.
- Over-limit inputs never reach the converter: `NoteFormatTest`.
### Not covered
- How the reader words the box and hides the quiz card for such notes — see `notes`.
- Reader cost of long lists and big tables inside the limits — drawing is not limited here; the table-cell limit is the only guard.
- Storing the box row and the cursor window — see `database:notes`.

## device-note — Device note text rules
### Behaviour
- A device note is stored as a path `<doc id>.md` and an original made of an empty front matter (two `---` lines, 8 bytes) followed by the body. The empty front matter keeps a body that starts with `---` from being read as front matter (013-local-notes: LN-3).
- Reading the body back from a stored original removes exactly that prefix; any body round trips, including empty ones, ones starting with `---` and ones starting with a BOM (013-local-notes: LN-3).
- A body fits when its UTF-8 size is at most 524,280 bytes, so the stored original is at most 524,288 bytes, the same count and limit the reader uses. A stored original fits when it is at most 524,288 bytes (013-local-notes: LN-3, LN-7).
- A body that fits but breaks another limit (for example 33 nested quote marks) is saved and shown as a whole-note over-limit box (009-note-format: NF-8).
- The import rule turns pasted, shared or file text into a draft of title, body and date. A BOM is dropped and CRLF and CR become LF; the front-matter boundary is the same code as in `markdown` and agrees with the shared boundary folder (013-local-notes: LN-6).
- Title: the front-matter `title` when not blank; else the first non-blank line if it is a level-1 `# ` heading; else the name hint (a file or share title); else empty. A `# ` heading with a closing run of `#` after a space has it removed, inline marks stay, and `# #`, `#text`, `## text` and setext are not title headings (013-local-notes: LN-6).
- A front-matter value is what follows the first colon, trimmed, with one pair of matching quotes removed; the first of a repeated key wins; keys other than `title` and `date` are ignored (013-local-notes: LN-6).
- Body: after a heading title, the text after that line without leading blank lines; after front matter, the text after the closing line without leading blank lines; otherwise the whole text. A heading that follows a front-matter title stays in the body (013-local-notes: LN-6).
- Date: the front-matter `date` when it is a real day in `YYYY.MM.DD`; any other shape (`2026-10-14`) or impossible day (`2026.02.30`) gives the day the import happens (013-local-notes: LN-6).
- The title is cut to 200 characters by the domain title rule. Text with no front matter and no heading is accepted as it is; foreign Markdown never fails the import (013-local-notes: LN-6).
### Public API
- `DeviceNoteMarkdown` — implements the domain device-note text rules: `fits(body)`, `rawFits(text)` and `imported(text, nameHint, today)` giving a draft; takes the limit set (default limits).
- `deviceNotePath(docId)` — the stored path, `<docId>.md`.
- `deviceNoteRaw(body)` — the stored original: empty front matter plus body.
- `deviceNoteBody(raw)` — the body of a stored original.
### Guarantees
- The import table (22 cases), CRLF, CR and BOM equivalence, the boundary folder, the 524,280 and 524,288 byte bounds, `---` bodies and round trips: `DeviceNoteMarkdownTest`.
### Not covered
- Picking a file, decoding it as UTF-8 and the five rejection reasons — see `editor`; the editor's title and group length rules — see `domain`.
- Writing or reading the row — see `database:notes`; creating the key, group and date of a new note — see `data`.
