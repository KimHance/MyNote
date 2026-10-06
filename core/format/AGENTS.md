# AGENTS.md — :core:format

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

**노트 원문 한 개를 블록으로 바꾼다.** 형식 고르기 · 마크다운 해석 전 한도 · JSON 형식 1 읽기 · 블록 캐시 인코딩이 여기 있다.
순수 JVM 모듈이다(`hancenote.jvm.library` + `kotlin.serialization` 별칭) — `import android.*` 는 컴파일 에러다.
의존은 `:core:domain` 하나이고, 이 모듈을 아는 것은 `:core:data` 뿐이다. network · database · feature 는 모른다.

공개 API 는 넷이고 나머지(DTO · `BlockJson` · 매퍼 · 한도)는 전부 `internal` 이다.

| 이름 | 하는 일 |
|---|---|
| `NoteFormat` | 진입점 — `parse(path, raw)` · `derive(path, raw)` · `read(blocksJson, path, raw)`. `@Inject constructor()` 라 `@Binds` 가 필요 없다 |
| `MarkdownConverter` | 한도 안의 마크다운 원문 → 도메인 블록. `NoteFormat(markdown)` 으로 바꿔 끼운다 |
| `DerivedNote` | 캐시에 두는 값 — `blocksJson` · `excerpt` |
| `PARSER_VERSION` | 해석 판 번호. 이보다 낮은 판의 캐시 행은 원문에서 다시 만든다 |

## 형식은 진입점 하나가 고른다

- 경로가 `.md`(대소문자 무시)로 끝나면 마크다운, 그 밖이거나 `null` 이면 JSON 이다. `null` 은 경로가 없는 옛 캐시 행이다.
- 원문을 블록으로 바꾸는 네 자리가 모두 `NoteFormat` 을 쓴다 — 새로 받은 노트(`SourceReconciler`) · 앱 시작 재변환
  (`NotesRepositoryImpl.rederiveStale`, 행의 `path`) · 연결 첫 동기화(`ConnectionRepositoryImpl` 이 따로 만드는 reconciler) ·
  캐시 손상 대체(`NoteMapper` → `read`). **형식을 고르는 코드를 다른 곳에 새로 두지 않는다.** 하나만 빠뜨려도 `.md` 가 JSON 으로
  해석된 상자로 현재 판 번호에 굳고, 내용이 바뀔 때까지 다시 변환되지 않는다.
- **세 함수는 어떤 입력에도 던지지 않는다.** `Exception` · `StackOverflowError` 는 원문 전체를 든 노트 전체 상자(`broken_note`)가 된다.
  재변환은 소스 격리 밖에서 모든 소스보다 먼저 돌아서, 한 행이 던지면 그 행이 옛 판으로 남아 동기화가 매번 실패한다.
- `read` 는 캐시를 못 읽으면 원문을 경로로 다시 해석하고, 원문도 없으면 캐시 글자를 든 노트 전체 상자를 돌려준다.
  형식 때문에 노트가 목록에서 사라지는 길은 없다.

## 마크다운 — 한도를 먼저 보고 변환기를 부른다

1. `exceedsMarkdownLimits(raw)` 가 참이면 변환기를 부르지 않고 `Unsupported(limit_exceeded, raw = 원문)` 하나로 둔다.
   - 크기: UTF-8 524,288 바이트 초과(할당 없이 센다).
   - 중첩: 줄 앞 컨테이너(`>` · 목록 표시) 32단 초과. CommonMark 의 컨테이너 잇기를 따라 줄마다 센다 — 탭은 4칸, 표시 뒤 공백
     1–4칸, 줄 끝 LF · CRLF · CR, 맨 앞 BOM. 이어지는 글 줄(lazy)은 열린 컨테이너를 닫지 않고 둔다(크게 세는 쪽).
   - 줄 상한: 줄 하나의 앞부분이 품을 수 있는 컨테이너 수(표시 수 + 공백 칸 ÷ 2)가 64 를 넘어도 한도 넘음이다. 줄 잇기는 문단
     상태를 모르므로 이어지는 글로 끊어 보인 중첩을 놓칠 수 있다. 이 상한이 그 경우에도 실제 깊이를 64 이하로 묶는다.
   - 한도는 블록 깊이 32 를 보장하지 않는다. 문단을 끊지 못하는 줄로 이은 중첩은 실제 깊이 64 까지 한도를 지난다 —
     변환기가 블록 깊이 32 를 따로 지킨다.
   - 예외를 잡는 것으로 한도를 대신하지 않는다. 엔진 메모리는 중첩에 따라 빠르게 늘어 20KB 노트 하나로 바닥나고, 직렬화는
     하위 목록 200단에서 스택이 넘친다(note-format 결정 11).
2. 한도 안이면 `MarkdownConverter.convert(raw)` → 도메인 블록 → 캐시 DTO.
   - `@Inject constructor()` 가 끼우는 것은 지금 `PlaceholderMarkdown` 이다. 지금 판이 `.md` 원문을 JSON 으로 읽어 만들던 것과 같은
     원문 전체 상자를 내므로 `PARSER_VERSION` 을 올리지 않아도 캐시가 어긋나지 않는다.
   - 실제 엔진(`md-parser` 단위)은 `PlaceholderMarkdown` 자리를 바꾸고 판 번호를 그때 한 번 올린다. 앞머리 떼기 · 줄 끝 바꾸기도
     변환기 몫이다 — 한도는 원문 그대로를 본다.

## JSON — 와이어와 캐시는 모양을 따로 둔다

| | 와이어 `WireBlockDto` (`WireDto.kt`) | 캐시 `NoteBlockDto` (`CacheDto.kt`) |
|---|---|---|
| 무엇 | 원격 JSON 원문 — 형식 1 | `blocks_json` 열 — 파생 캐시 |
| 집합 | 블록 8종 · 모양 3종. **동결** | 와이어 + `unknown` · `unsupported`. 새 블록 · 필드는 여기에만 |

- `MyNoteJsonFormat.parse(raw)` 는 던지지 않는다. 루트가 객체가 아니거나 `blocks` 가 배열이 아니면(JSON 문법 오류 · 끝 쉼표 포함)
  `broken_note` 하나. 아니면 블록마다:
  - 객체가 아니거나 `type` 이 문자열이 아님 → `broken_block`
  - `type` 이 `WIRE_TYPES` 밖 → `unknown_type`(원래 type 을 `original` 에). 원격이 `unsupported` · `unknown` 을 보내도 여기 걸린다
  - 와이어 디코드 실패(필드 누락 · 타입 오류) → `broken_block`. 성공하면 캐시 DTO 로 옮긴다
  - `text` = 블록의 `fallback`, 없으면 `spans` 글자 이음 · `code` · `items` 글자(항목마다 줄), 없으면 `null`
  - `raw` = 그 블록 JSON 을 두 칸 들여쓰기로 다시 쓴 것
- envelope 은 `blocks` 하나만 읽는다. 캐시의 제목 · 날짜는 목록(index)의 값이다.
- **`WIRE_TYPES` 는 와이어 sealed 에서 센다.** 캐시에 새 type 을 더해도 따라 늘지 않는다 — 그래서 JSON 원문의 형식 1 밖 type ·
  필드(`table` · `italic` · `start` · `children`)는 지금 판과 같게 읽힌다(`CacheCompatTest` 다섯 줄).
- `BlockJson` 에 `ignoreUnknownKeys` · `coerceInputValues` — 모르는 enum · `null` · 숫자 · 불리언 `style` 은 `text` 가 된다.
  트리 디코드라 객체 · 배열 `style` 은 그 블록이 `broken_block` 이 된다. 이 `Json` 은 DI 밖 내부 인스턴스다.
- **캐시 바이트를 지킨다.** 구분자 `type` · 기본값 생략 · 같은 serial name 이라 지금 8종 블록의 `blocks_json` 바이트가 그대로다.
  `CacheCompatTest` 가 그 문자열을 고정한다. 깨지면 이미 깐 기기의 캐시와 어긋난다. 옛 캐시의 `{"type":"unknown"}` 은 원문 없는
  `UnknownType`(UI 는 "원문 보기" 를 숨긴다)으로 읽힌다.

## 상자 사유

| 캐시 | 도메인 | 언제 |
|---|---|---|
| `unknown_type` | `UnknownType` | JSON 블록의 type 을 모른다 |
| `broken_block` | `BrokenBlock` | 아는 type 인데 필드 누락 · 타입 오류, type 없음 |
| `broken_note` | `BrokenNote` | JSON 이 아니다 · 해석이 던졌다 |
| `limit_exceeded` | `BrokenNote` | 마크다운이 해석 전 한도를 넘었다. 도메인 종류는 `note-model` 단위가 더한다 — 더하면 `BlockMapper.kt` 의 이 줄을 바꾼다 |

## 새 블록 · 필드를 더할 때

`when` 이 exhaustive 라서 컴파일러가 잡아 주는 자리와 안 잡아 주는 자리가 섞여 있다. **컴파일러가 안 잡는 것: `PARSER_VERSION`, 문서.**

1. `CacheDto.kt` — `NoteBlockDto` 에 `@SerialName` 서브타입 · 기본값 있는 필드. `WireDto.kt` 는 건드리지 않는다(형식 1 동결)
2. `core/domain/.../NoteBlock.kt` — 도메인 대응
3. `BlockMapper.kt` — 캐시 → 도메인 · 도메인 → 캐시(컴파일 에러로 잡힌다)
4. `:core:design-system` 의 `NoteContentBlock` + 렌더러, `:feature:notes` 의 `NoteBlockMapper`(컴파일 에러로 잡힌다)
5. **`PARSER_VERSION` 을 올린다.** 해석 결과가 바뀌는 모든 변경이 해당한다. 안 올리면 캐시된 노트가 다음 내용 변경 전까지 옛 결과로 남는다
6. `CacheCompatTest` 가 그대로 통과해야 한다 — 기본값 필드는 지금 바이트를 바꾸지 않는다
7. JSON 형식은 동결이라 `notes/gen_index.py` 의 JSON 닫힌 집합과 `notes/SCHEMA.md` 의 JSON 절은 바꾸지 않는다

필드 없는 블록은 `data object`(`Divider` · `Spacer`)로 둔다 — `{"type":"divider"}` 만으로 라운드트립된다.

## 시험

JVM 모듈이라 태스크가 `:core:format:test` 다(`testDebugUnitTest` 없음). 전부 JUnit4.

| 시험 | 무엇 |
|---|---|
| `MyNoteJsonFormatTest` · `NoteDtoSerializationTest` · `ReparseEquivalenceTest` | `:core:data` 에서 옮긴 형식 시험 — 이름 · 단정 그대로 |
| `NoteFormatTest` | NF-2 형식 고르기 · 던지지 않음 · 캐시 손상 대체, NF-8 엔진 호출 0 · 해석 실패 상자 |
| `MarkdownLimitsTest` | NF-8 32단 / 33단 · 512KB · CR · BOM · 컨테이너가 아닌 줄 · 줄 상한 |
| `CacheCompatTest` | NF-12 형식 1 밖 type · 필드 · 캐시 바이트 · 옛 캐시 행 |

동기화를 지나는 시험(연결 첫 동기화 · 캐시 손상 행 · 한도 넘은 노트 격리 · 재변환 실패 행)은 `:core:data` 의 `NoteFormatSyncTest` 다.
`ReparseEquivalenceTest` 의 레포 노트 시험은 루트 `notes/` 를 읽기만 하고 태스크 입력으로 선언하지 않았다 — 노트만 바뀌었으면 `--rerun`.
