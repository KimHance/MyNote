---
id: format
name: Note format
aliases: [노트 형식, 마크다운 변환, 형식 엔진, core:format]
uses: [domain]
units:
  - {id: note-format, name: Entry point and block cache, aliases: [형식 고르기, 블록 캐시, 해석 판 번호, 파리티, PARSER_VERSION], code: [src/main/java/com/hancekim/mynote/core/format/NoteFormat.kt, src/main/java/com/hancekim/mynote/core/format/BlockCache.kt, src/main/java/com/hancekim/mynote/core/format/BlockMapper.kt, src/main/java/com/hancekim/mynote/core/format/CacheDto.kt, src/test/java/com/hancekim/mynote/core/format/NoteFormatTest.kt, src/test/java/com/hancekim/mynote/core/format/BlockCacheTest.kt, src/test/java/com/hancekim/mynote/core/format/EngineVersionTest.kt, src/test/java/com/hancekim/mynote/core/format/CacheCompatTest.kt, src/test/java/com/hancekim/mynote/core/format/NoteDtoSerializationTest.kt, src/test/java/com/hancekim/mynote/core/format/ReparseEquivalenceTest.kt, src/test/java/com/hancekim/mynote/core/format/ParityTest.kt, src/test/java/com/hancekim/mynote/core/format/Reads.kt, src/testFixtures/]}
  - {id: json-format, name: JSON format 1 reader, aliases: [JSON 형식, 와이어 DTO, 옛 JSON 노트], code: [src/main/java/com/hancekim/mynote/core/format/MyNoteJsonFormat.kt, src/main/java/com/hancekim/mynote/core/format/WireDto.kt, src/test/java/com/hancekim/mynote/core/format/MyNoteJsonFormatTest.kt]}
  - {id: markdown, name: Markdown converter, aliases: [마크다운, GFM, 앞머리, 글자 모양, 표 변환], code: [src/main/java/com/hancekim/mynote/core/format/GfmMarkdownConverter.kt, src/main/java/com/hancekim/mynote/core/format/MarkdownSpans.kt, src/main/java/com/hancekim/mynote/core/format/NoteMarkdownFlavour.kt, src/main/java/com/hancekim/mynote/core/format/HtmlEntities.kt, src/main/java/com/hancekim/mynote/core/format/MarkdownSource.kt, src/test/java/com/hancekim/mynote/core/format/MarkdownConverterTest.kt]}
  - {id: limits, name: Limits, aliases: [한도, 한도 넘음, 512KB, 32단, 표 칸], code: [src/main/java/com/hancekim/mynote/core/format/MarkdownLimits.kt, src/test/java/com/hancekim/mynote/core/format/MarkdownLimitsTest.kt, src/test/java/com/hancekim/mynote/core/format/ResultLimitsTest.kt]}
  - {id: device-note, name: Device note text rules, aliases: [기기 노트 원문, 가져오기 규칙, 빈 앞머리], code: [src/main/java/com/hancekim/mynote/core/format/DeviceNoteMarkdown.kt, src/test/java/com/hancekim/mynote/core/format/DeviceNoteMarkdownTest.kt]}
---
## Overview
Turns one note original into blocks: it picks the format from the path, checks limits, converts Markdown (JetBrains markdown engine, GFM) or reads JSON format 1, and writes and reads the block cache JSON that sits in the notes table. It also holds the text rules for device notes that share Markdown's front-matter and size code.
It is pure JVM with no Android dependency; only `data` uses it. The engine and its types stay inside the module, and every DTO is internal.
It does not fetch, store or draw notes: caching rows is `database`, deciding when to re-parse is `data`, drawing blocks is the design system and the notes feature. The block model and the limit values belong to `domain`.
## Structure
- `note-format` is the only entry. It picks the format by path, asks `limits` before the engine runs, calls `markdown` or `json-format`, and turns the result into the cache JSON and the first sentence.
- `markdown` splits off front matter and converts the engine tree into blocks; it counts block depth and table cells while walking and reports a whole-note over-limit box.
- `limits` is the screening done on the raw text before any engine call: size and container nesting.
- `json-format` reads the frozen format-1 wire shape into the cache shape; cache shapes may grow, wire shapes may not.
- `device-note` reuses the front-matter split and the byte count to define device-note size bounds, import rules and the stored shape.
```mermaid
flowchart LR
  data[data] --> note-format
  data --> device-note
  note-format --> limits
  note-format --> markdown
  note-format --> json-format
  device-note --> markdown
  device-note --> limits
```
