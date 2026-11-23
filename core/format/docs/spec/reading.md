---
units: [format:note-format, format:json-format]
plans: [009-note-format]
decisions: [NF-D16, NF-D17, NF-D19, NF-D37, DEC-26, DEC-28]
---
## note-format — Entry point and block cache
### Behaviour
- One entry turns an original into blocks. The format comes from the path: a path ending in `.md` (any letter case) is Markdown; any other path, and no path, is JSON. No path means a cache row that carries no path (009-note-format: NF-2).
- Every place that turns an original into blocks goes through this entry: a freshly fetched note, the start-up re-parse, the first sync of a connection, and the replacement of a damaged cache. Format choice is not repeated anywhere else (009-note-format: NF-2).
- The entry never throws. An exception or a stack overflow gives one whole-note box of kind `broken_note`; running out of memory gives one whole-note box of kind `limit_exceeded`. Only that note is affected (009-note-format: NF-2, NF-8).
- Three calls exist: `parse` returns the blocks for drawing, `derive` returns what the cache stores (blocks JSON and the first sentence), and `read` rebuilds blocks from the stored cache.
- `derive` stores the first sentence as the excerpt, computed by the domain rule; the failure boxes from an exception or memory exhaustion have no excerpt (009-note-format: NF-10).
- The block cache is one JSON array. Each block is an object with a `type` and default-valued fields left out: a span is `text` plus `style` (`bold`, `code`, absent for plain), `italic` and `strike` only when true; a list item is `spans`, `checked` and `children` (empty children left out); an ordered list carries `start` only when it is not 1; a table has `header`, `rows` and `align`.
- Field names and the block type names are the storage format. Changing them needs a new parse version so cached rows are rebuilt (009-note-format: NF-9).
- The bytes cached for the eight blocks of format 1 are fixed: a JSON note and a Markdown note that give the same blocks give the same bytes (009-note-format: NF-12).
- The cache may grow new block kinds and fields (table, italic, strike, checkbox state, children, start). The wire shape of JSON notes may not (see `json-format`).
- Cache decoding ignores unknown keys, turns an unknown enum value or a null into the default, and reads an unknown block type as an unknown-type box without throwing. A cached `{"type":"unknown"}` reads as an unknown-type box with no original, so the reader hides "show original".
- The cache is written through a size-capped stream and stops at once past 1 MB (1,048,576 UTF-8 bytes). The note then becomes a whole-note over-limit box, for JSON notes as well as Markdown; exactly 1,048,576 bytes is accepted (009-note-format: NF-8).
- A whole-note box (`broken_note`, `limit_exceeded`) is stored with an empty original, because the original is already in the notes table; `parse` returns the box with the original inside (009-note-format: NF-8).
- Box kinds: `unknown_type` (JSON block with an unknown type), `broken_block` (known type with a missing or wrongly typed field, or no type), `broken_note` (not JSON, or the parse threw), `limit_exceeded` (a limit was passed). The first two cover one block; the last two cover the whole note.
- `read` returns the cached blocks without asking for the original, so the list never reads original text. When the cache is a single whole-note box with an empty original it asks for the original and fills it in; a box that already holds an original is returned as is (009-note-format: NF-8).
- When the cache cannot be decoded, `read` asks for the original and parses it by path. With no original it returns a `broken_note` box holding the cache text. A format problem never removes a note from the list (009-note-format: NF-2).
- The parse version is 2. Any change that alters parse results raises it, so cached rows older than it are rebuilt offline from the stored original and path (009-note-format: NF-9).
- The version is tied to the engine release it was made for. A test compares that release with the one in the version catalogue and fails when only the catalogue was changed (009-note-format: NF-9).
- Re-parsing a JSON original gives the same blocks as decoding its blocks straight into the cache and reading them back; re-reading the 22 repository notes shows no difference (009-note-format: NF-12).
- The 22 fixed pairs of JSON and Markdown copies give the same blocks (the JSON spacer excepted, since Markdown has no spacer), the same first sentences and the same quiz text. Every note under the repository's `notes/` reads with no box and at least one block (009-note-format: NF-11).
- The fixed copies are frozen values and are never compared with the live `notes/` folder; editing a Markdown copy breaks parity on purpose (009-note-format: NF-11).
### Public API
- `NoteFormat` — class with `parse(path, raw)`, `derive(path, raw)` and suspend `read(blocksJson, path, raw)`; an injectable no-argument constructor uses the Markdown converter, and a constructor takes another converter (and limits) for tests.
- `MarkdownConverter` — fun interface: `convert(raw)` turns an in-limit Markdown original into blocks; it may throw.
- `DerivedNote` — the cache value: blocks JSON and the excerpt (null when there is none).
- `PARSER_VERSION` — the parse version number; rows with a lower stored value are re-parsed.
- Test fixtures (`ParityNotes`, `ParityNote`) are shared with the `data` tests; they are not part of the app.
### Guarantees
- Format choice, no-throw behaviour for exceptions, stack overflow and memory exhaustion, and cache replacement: `NoteFormatTest`.
- Cache round trips and bytes, including 32 nested sub-blocks and the limit kinds read back as domain over-limit boxes: `BlockCacheTest`, `NoteDtoSerializationTest`.
- Today's cache bytes and old cache rows stay readable: `CacheCompatTest`.
- Cache cap boundary and the empty original of whole-note boxes: `ResultLimitsTest`.
- Engine release equals the one the parse version was made for: `EngineVersionTest`.
- Fixed parity of 22 notes and the repository notes: `ParityTest`; equivalence of re-parsing with decoding straight into the cache: `ReparseEquivalenceTest`.
### Not covered
- The block model, first-sentence rule and limit values — see `domain`.
- Turning Markdown text into blocks — see `markdown`; reading JSON — see `json-format`; the screening limits — see `limits`.
- Storing the cache, the original and the parse version in a row — see `database:notes`; deciding to re-parse after an upgrade — see `data`.
- Drawing a box and its wording — see the design system and `notes`.
- There is no parse time limit and no partial display of an over-limit note; an over-limit note is one box (009-note-format: NF-8).

## json-format — JSON format 1 reader
### Behaviour
- Reads a JSON note: a root object whose `blocks` array lists the blocks. Only `blocks` is read; title, date and id of the note come from the list entry (009-note-format: NF-12).
- A root that is not an object, a `blocks` value that is not an array, or any JSON syntax error (a trailing comma included) gives one `broken_note` box holding the whole text (009-note-format: NF-12).
- Format 1 has eight block types: heading, paragraph, code, quote, bullet list, ordered list, divider, spacer. A span has `text` and a `style` of text, bold or code. List items have spans only; an ordered list has no start number (009-note-format: NF-12).
- A block that is not an object, or has no string `type`, is a `broken_block` box. A type outside the eight, including `unsupported` and `unknown` sent by a remote, is an `unknown_type` box that keeps the original type name (009-note-format: NF-12).
- A block of a known type with a missing or wrongly typed field is a `broken_block` box; the blocks before and after it are kept.
- A box carries a text salvage and the block's own JSON re-written with a two-space indent. The text is the block's `fallback` string when present, otherwise the joined span text, code and list items (one line per item), otherwise nothing.
- An unknown span style, or a style that is null, a number or a boolean, reads as plain text and the block is kept. A style that is an object or an array makes that one block a `broken_block`.
- An empty `blocks` array is a normal note with no body.
- The set of accepted types is counted from the frozen wire shape, not from the cache shape. Types and fields that exist only in the cache shape (table, italic, start, children) are not read from a JSON original: a table block is an unknown-type box and the extra fields are ignored (009-note-format: NF-12).
- Bad input becomes boxes rather than exceptions; a note with a 100,000-level nested value in one block still gives one block through the entry (009-note-format: NF-2).
### Public API
- None — the reader, the wire shape and the type set are internal; callers go through `note-format`.
### Guarantees
- Every case above is checked in `MyNoteJsonFormatTest`; out-of-format types and fields in `CacheCompatTest`.
### Not covered
- Writing JSON notes: the app never writes them, and the repository tools keep the same closed set in `public-repos`.
- Choosing JSON over Markdown — see `note-format`.
- Adding a new block kind: it goes in the cache shape only, never in the wire shape (format 1 is frozen).
