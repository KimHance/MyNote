# AGENTS.md — :core:format

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

## 이 모듈이 하는 일

**노트 원문 한 개를 블록으로 바꾼다.** 형식 고르기 · 마크다운 해석 전 한도 · JSON 형식 1 읽기 · 블록 캐시 인코딩이 여기 있다.
순수 JVM 모듈이다(`hancenote.jvm.library` + `kotlin.serialization` 별칭) — `import android.*` 는 컴파일 에러다.
`java-test-fixtures` 는 컨벤션 규칙(모듈은 `hancenote.*` 와 별칭만)의 예외로 직접 적용한다 — 파리티 고정 사본을 이 모듈과 `:core:data` 시험이
클래스패스로 읽는다(`build-logic/AGENTS.md` 모듈 표 아래).
의존은 `:core:domain` 과 마크다운 엔진(`org.jetbrains:markdown`, 판은 `libs.versions.toml`)이고, 이 모듈을 아는 것은 `:core:data` 뿐이다.
network · database · feature 는 모른다. `org.intellij.markdown` import 는 이 모듈 밖에 두지 않는다.

공개 API 는 넷이고 나머지(DTO · `BlockJson` · 캐시 코덱 `BlockCache` · 매퍼 · 한도 · 마크다운 변환기)는 전부 `internal` 이다.

| 이름 | 하는 일 |
|---|---|
| `NoteFormat` | 진입점 — `parse(path, raw)` · `derive(path, raw)` · `read(blocksJson, path) { 원문 }`(suspend). `@Inject constructor()` 라 `@Binds` 가 필요 없다 |
| `MarkdownConverter` | 한도 안의 마크다운 원문 → 도메인 블록. `NoteFormat(markdown)` 으로 바꿔 끼운다 |
| `DerivedNote` | 캐시에 두는 값 — `blocksJson` · `excerpt` |
| `PARSER_VERSION` | 해석 판 번호. 이보다 낮은 판의 캐시 행은 원문에서 다시 만든다 |

## 형식은 진입점 하나가 고른다

- 경로가 `.md`(대소문자 무시)로 끝나면 마크다운, 그 밖이거나 `null` 이면 JSON 이다. `null` 은 경로가 없는 옛 캐시 행이다.
- 원문을 블록으로 바꾸는 네 자리가 모두 `NoteFormat` 을 쓴다 — 새로 받은 노트(`SourceReconciler`) · 앱 시작 재변환
  (`NotesRepositoryImpl.rederiveStale`, 행의 `path`) · 연결 첫 동기화(`ConnectionRepositoryImpl` 이 따로 만드는 reconciler) ·
  캐시 손상 대체(`NoteMapper` → `read`). **형식을 고르는 코드를 다른 곳에 새로 두지 않는다.** 하나만 빠뜨려도 `.md` 가 JSON 으로
  해석된 상자로 현재 판 번호에 굳고, 내용이 바뀔 때까지 다시 변환되지 않는다.
- **세 함수는 어떤 입력에도 던지지 않는다.** `Exception` · `StackOverflowError` 는 노트 전체 상자(`broken_note`)가 된다.
  재변환은 소스 격리 밖에서 모든 소스보다 먼저 돌아서, 한 행이 던지면 그 행이 옛 판으로 남아 동기화가 매번 실패한다.
- **노트 전체 상자(`broken_note` · `limit_exceeded`)는 캐시에 원문을 두지 않는다**(`"raw":""`). 원문은 `raw` 열에 이미 있고,
  캐시에 다시 넣으면 한 행이 원문의 두 배가 된다. `parse` 가 돌려주는 블록에는 원문이 들어 있다.
- `read` 는 원문을 람다로 받아 **노트 전체 상자이거나 캐시를 못 읽을 때만** 부른다 — 목록이 원문 열을 읽지 않게. 상자면 원문으로
  `raw` 를 채우고(옛 캐시처럼 이미 든 상자는 그대로), 캐시를 못 읽으면 원문을 경로로 다시 해석하고, 원문도 없으면 캐시 글자를 든
  노트 전체 상자를 돌려준다. 형식 때문에 노트가 목록에서 사라지는 길은 없다.

## 마크다운 — 한도를 먼저 보고 변환기를 부른다

1. `exceedsMarkdownLimits(raw)` 가 참이면 변환기를 부르지 않고 `Unsupported(limit_exceeded, raw = 원문)` 하나로 둔다.
   한도 값은 domain `NoteLimits.Default` 다 — 화면 매퍼가 같은 값을 상자 문구 숫자로 쓰므로 여기에 숫자를 다시 적지 않는다.
   - 크기: UTF-8 `maxBytes`(524,288 바이트) 초과(할당 없이 센다).
   - 중첩: 줄 앞 컨테이너(`>` · 목록 표시) `maxDepth`(32단) 초과. CommonMark 의 컨테이너 잇기를 따라 줄마다 센다 — 탭은 4칸, 표시 뒤 공백
     1–4칸, 줄 끝 LF · CRLF · CR, 맨 앞 BOM. 이어지는 글 줄(lazy)은 열린 컨테이너를 닫지 않고 둔다(크게 세는 쪽). 줄 나머지가
     구분선(`- - -` · `* * *` 처럼 같은 글자 셋 이상과 공백)이면 목록 표시로 세지 않는다 — CommonMark 도 구분선이 먼저다.
   - 줄 상한: 줄 하나의 앞부분이 품을 수 있는 컨테이너 수(표시 수 + 공백 칸 ÷ 2)가 64 를 넘어도 한도 넘음이다. 줄 잇기는 문단
     상태를 모르므로 이어지는 글로 끊어 보인 중첩을 놓칠 수 있다. 이 상한이 그 경우에도 실제 깊이를 64 이하로 묶는다.
   - 한도는 블록 깊이 32 를 보장하지 않는다. 문단을 끊지 못하는 줄로 이은 중첩은 실제 깊이 64 까지 한도를 지난다 —
     변환기가 블록 깊이 32 를 따로 지킨다.
   - 예외를 잡는 것으로 한도를 대신하지 않는다. 엔진 메모리는 중첩에 따라 빠르게 늘어 20KB 노트 하나로 바닥나고, 직렬화는
     하위 목록 200단에서 스택이 넘친다(note-format 결정 11).
2. 한도 안이면 `MarkdownConverter.convert(raw)` → 도메인 블록 → 캐시 DTO. `@Inject constructor()` 가 끼우는 것은 `GfmMarkdownConverter` 다.

### 결과 한도 — 캐시 한 행이 커서 창에 들어가게

원문 한도만으로는 결과 크기가 묶이지 않는다. 짧은 줄이 많은 노트는 캐시 JSON 이 원문의 13–15배가 되고(제목 · 문단 한 글자씩),
모자란 표 칸을 채우면 10KB 원문이 칸 200만 개가 된다. Room 은 한 행을 커서 창(2MB) 하나로 읽어서, 창보다 큰 행이 하나라도
있으면 목록 질의가 던지고 홈의 노트가 전부 사라진다. 그래서 변환 결과에도 한도를 두고, 넘으면 노트 전체가 한도 넘음 상자다.

- 표 칸: 변환기가 노트 전체 표 칸 수(머리 열 × (본문 행 + 1))를 더해 가다 `maxTableCells` 를 넘으면 멈춘다. 모자란 칸을 채우기
  전에 센다. 값은 리더 그리기 비용으로 골랐다 — 표는 Layout 하나로 다 그려서 디버그 빌드에서 3×300 칸 2.7초 · 3×2,000 칸 6.4초 ·
  3×6,000 칸 15초에 ANR 이다. 노트 전체로 세는 것은 메모리 때문이다 — 표 하나마다 세면 작고 넓은 표 여러 개로 같은 증폭이 난다.
- 캐시 크기: `BlockCache.encode(blocks, maxBytes)` 가 상한 있는 스트림으로 쓰다 `maxCacheBytes` 를 넘으면 그 자리에서 멈춘다.
  JSON 노트도 같다. 다 쓴 뒤 재면 수 MB 문자열을 만든 뒤 버린다.
- 값은 실측으로 골랐다 — 레포 노트 22개의 캐시는 원문의 1.27–1.93배(합 1.41배), 실제 노트를 이어 붙인 483KB 는 1.41배, 짧은 한글
  문단은 1.89배라 원문 상한 512KB 의 보통 노트는 1MB 안이다. 최악 행 = 원문 512KB + 캐시 1MB + 나머지 열로 창 2MB 에 500KB 남짓
  남는다(`:core:database` 계측 `NF-8 한도 안 최악 크기 행도 …`).

## 마크다운 변환 — `GfmMarkdownConverter`

| 단계 | 파일 | 무엇 |
|---|---|---|
| 본문 고르기 | `MarkdownSource.kt` | 맨 앞 BOM 떼기 · 줄 끝(CRLF · CR) → LF · 앞머리(첫 줄 정확히 `---` ~ 다음 정확히 `---`) 떼기. 경계가 없거나 닫히지 않으면 글 전체가 본문 |
| 엔진 | `NoteMarkdownFlavour.kt` | GFM 에서 수식 · 형광 파서를 뺀 flavour(`$` · `==` 가 글자로 남는다). 취소선은 같은 규칙의 자체 구분자 파서 |
| 블록 | `GfmMarkdownConverter.kt` | 트리를 블록으로. 깊이를 세며 내려가고 32(`NoteLimits.maxDepth`)를 넘으면 노트 전체가 `LimitExceeded` 상자 하나(원문을 든다) |
| 글자 | `MarkdownSpans.kt` | 인라인 노드를 구간으로. 스택으로 걷는다(깊은 강조에도 스택이 안 넘침). 이스케이프 · 엔티티를 직접 푼다 |

문법이 무엇으로 가는지는 `notes/SCHEMA.md` 3절과 note-format NF-3 ~ NF-7 이 정본이다.

**엔진 판을 올리면 `PARSER_VERSION` 도 올린다.** 파리티가 통과해도 이미 캐시된 노트는 내용이 바뀔 때까지 옛 엔진 결과로,
새로 받은 노트만 새 결과로 갈린다. `PARSER_VERSION` 옆 `PARSER_ENGINE` 이 그 판이 기준으로 삼은 엔진 판이고, `EngineVersionTest` 가
카탈로그(`gradle/libs.versions.toml`, 시험 입력 `catalogInput`)의 엔진 판과 다르면 실패한다 — 카탈로그만 고치면 빨강이다.

엔진 판을 올리거나 변환을 고칠 때 알아 둘 엔진 사정:

- 엔진은 앞머리를 모르고 CR 을 줄 끝으로 보지 않는다 — 그래서 엔진 밖에서 정리한다. 한도는 정리하기 전 원문 그대로를 본다.
- `MarkdownParser` 는 `assertionsEnabled = false` 로 만든다. 켜 두면 내부 단정이 실패할 때 던진다.
- 엔진의 `StrikeThroughDelimiterParser` 는 문단의 첫 구분자가 홑물결 여는 쪽이면 짝을 보지 않는다(`~a~` 가 글자로 남음). 자체 파서가 이 자리를 맡는다.
- 엔진은 목록 표시가 언제나 문단을 끊게 두고(`2)` · 빈 항목도), 10자리 이상 번호는 목록 표시로 보지 않는다. 해석 전 한도의 스캐너는
  그런 번호를 목록 표시로 세므로, 그 줄로 이은 들여쓰기 중첩은 한도를 지나 블록 깊이 64 까지 갈 수 있다 — 변환기의 깊이 지킴이 그 자리다
  (`NF-8 변환 결과의 블록 깊이는 32 를 넘지 않는다`).
- 엔티티는 엔진의 HTML 출력용 변환기를 쓰지 않는다(평문 `"` 를 `&quot;` 로 되돌린다). 엔진의 엔티티 표는 코드 포인트 하나라
  `&fjlig;` · `&NotEqualTilde;` 처럼 둘로 된 HTML5 이름 93개는 `HtmlEntities.kt` 표가 먼저 본다. 코드 구간 글자는 원문 범위를 그대로 쓴다.
- 문단 · 표 칸 · 목록 안 코드에는 컨테이너 들여쓰기(`WHITE_SPACE`) · 인용 표시(`BLOCK_QUOTE`) 토큰이 섞인다. 줄바꿈(`EOL`) 뒤 들여쓰기는 버린다.
  표 칸 맨 앞 `>` 도 엔진은 인용 표시 토큰으로 준다 — 표 칸에서는 글자로 쓴다(`>=` 가 `=` 가 되지 않게).
- 문단 앞뒤는 공백 · 탭 · 줄바꿈만 뗀다(전각 공백 · NBSP 는 글자). 줄바꿈 앞 공백도 떼고 줄바꿈은 공백 하나다(CommonMark).
- 인용은 글 줄로 펴므로 인용 안 체크 항목은 그 줄 앞에 `[x] ` · `[ ] ` 를 남긴다(켬 · 끔이 사라지지 않게).
- 각주는 그리지 않는다. 엔진은 한 낱말 정의 `[^n]: 설명` 만 링크 정의로 읽어 감추고 여러 낱말은 문단으로 남겨, 같은 문법이 두 결과였다.
  `[^..]` 라벨은 정의가 있어도 참조로 보지 않고, 각주 정의 줄은 원문 글자 문단으로 둔다(note-format 결정 7 '그리지 못하는 문법은 글자로').
- 표 정렬 노드는 없다 — 구분 행 글자를 읽는다. 머리 칸 수를 넘는 칸은 `CELL` 이 아니라 구분자 토큰으로 붙는다. 표 칸 코드 구간의 `\|` 는 엔진이 남긴다.
- `<a@b.com>` 은 자동 링크 노드가 아니라 `<` · `EMAIL_AUTOLINK` · `>` 토큰 셋으로 온다.
- 엔진은 정의가 없어도 `[..]` 를 참조 링크 노드로 만들고(`a[i]` · `Map[K, V]`), 목적지에 공백이 든 `[a](b c)` 도 링크로 읽는다.
  변환기가 엔진의 `LinkMap` 으로 정의를 모아, 링크로 성립하지 않는 노드는 괄호까지 원문 글자로 둔다(CommonMark 와 같다).

## JSON — 와이어와 캐시는 모양을 따로 둔다

| | 와이어 `WireBlockDto` (`WireDto.kt`) | 캐시 `NoteBlockDto` (`CacheDto.kt`) |
|---|---|---|
| 무엇 | 원격 JSON 원문 — 형식 1 | `blocks_json` 열 — 파생 캐시 |
| 집합 | 블록 8종 · 모양 3종. **동결** | 와이어 + `unknown` · `unsupported` · `table` + 기본값 필드(`italic` · `strike` · `checked` · `children` · `start`). 새 블록 · 필드는 여기에만 |

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
| `limit_exceeded` | `LimitExceeded` | 한도(원문 크기 · 줄 앞 중첩 · 블록 깊이 · 표 칸 · 캐시 크기)를 넘었다 |

## 새 블록 · 필드를 더할 때

`when` 이 exhaustive 라서 컴파일러가 잡아 주는 자리와 안 잡아 주는 자리가 섞여 있다. **컴파일러가 안 잡는 것: `PARSER_VERSION`, 문서.**

1. `CacheDto.kt` — `NoteBlockDto` 에 `@SerialName` 서브타입 · 기본값 있는 필드. `WireDto.kt` 는 건드리지 않는다(형식 1 동결)
2. `core/domain/.../NoteBlock.kt` — 도메인 대응
3. `BlockMapper.kt` — 캐시 → 도메인 · 도메인 → 캐시(컴파일 에러로 잡힌다). 마크다운에서 나오게 하려면 `GfmMarkdownConverter.kt` 도
4. `:core:design-system` 의 `NoteContentBlock` + 렌더러, `:feature:notes` 의 `NoteBlockMapper`(컴파일 에러로 잡힌다)
5. **`PARSER_VERSION` 을 올린다.** 해석 결과가 바뀌는 모든 변경이 해당한다. 안 올리면 캐시된 노트가 다음 내용 변경 전까지 옛 결과로 남는다
6. `CacheCompatTest` 가 그대로 통과해야 한다 — 기본값 필드는 지금 바이트를 바꾸지 않는다
7. JSON 형식은 동결이라 `notes/gen_index.py` 의 JSON 닫힌 집합과 `notes/SCHEMA.md` 의 JSON 절은 바꾸지 않는다

필드 없는 블록은 `data object`(`Divider` · `Spacer`)로 둔다 — `{"type":"divider"}` 만으로 라운드트립된다.

캐시 모양(필드 이름이 저장 형식이다 — 바꾸면 `PARSER_VERSION` 을 올려 다시 만든다):

| 자리 | 캐시 JSON |
|---|---|
| 구간 | `{"text":…,"style":"bold","italic":true,"strike":true}` — 기본값(`text` · `false`)은 생략 |
| 목록 항목 | `{"spans":[…],"checked":true,"children":[블록…]}` — `checked` 없음 = 체크 항목 아님, `children` 빈 목록은 생략 |
| 번호 목록 | `{"type":"orderedList","items":[…],"start":3}` — `start` 1 은 생략 |
| 표 | `{"type":"table","header":[{"spans":[…]}],"rows":[[{"spans":[…]}]],"align":["left","center","right","none"]}` |

## 시험

JVM 모듈이라 태스크가 `:core:format:test` 다(`testDebugUnitTest` 없음). 전부 JUnit4. `read` 가 suspend 라 시험은 원문을 미리 쥔
같은 이름의 확장(`Reads.kt`)으로 부른다.

| 시험 | 무엇 |
|---|---|
| `MyNoteJsonFormatTest` · `NoteDtoSerializationTest` · `ReparseEquivalenceTest` | `:core:data` 에서 옮긴 형식 시험 — 이름 · 단정 그대로. 레포 노트 시험의 입력은 JSON 고정 사본 22개 |
| `NoteFormatTest` | NF-2 형식 고르기 · 던지지 않음 · 캐시 손상 대체, NF-8 엔진 호출 0 · 해석 실패 상자 |
| `MarkdownLimitsTest` | NF-8 해석 전 한도 — 32단 / 33단 · 512KB · CR · BOM · 컨테이너가 아닌 줄 · 줄 상한 |
| `MarkdownConverterTest` | 실제 엔진으로 NF-1 앞머리 · 줄 끝 · 경계 묶음, NF-3 ~ NF-7 대응 표, NF-8 32단 / 33단 · 512KB · 블록 깊이 지킴 · 큰 입력 |
| `ParityTest` | NF-11 — 고정 사본 22쌍의 블록 · 첫 문장 · 퀴즈 평문, JSON 사본 해시 = 기준 목록, 레포 `notes/` 상자 0 |
| `CacheCompatTest` | NF-12 형식 1 밖 type · 필드 · 캐시 바이트 · 옛 캐시 행 |
| `EngineVersionTest` | NF-9 카탈로그의 엔진 판 = `PARSER_ENGINE`(엔진만 올리고 판 번호를 잊지 않게) |
| `BlockCacheTest` | NF-4 · 5 · 6 새 모양의 캐시 왕복 · 바이트, 32단 하위 블록, NF-8 한도 넘음 종류 · 거르기가 도메인 한도 값을 씀, NF-12 변환기 길의 지금 블록 바이트 |
| `ResultLimitsTest` | NF-8 결과 한도 — 넓은 표 증폭 · 표 칸 누적 · 캐시 상한 경계(JSON · 마크다운) · 한도 안 병적 입력의 캐시 크기 · 노트 전체 상자의 원문 자리 |

동기화를 지나는 시험은 `:core:data` 의 `NoteFormatSyncTest`(가짜 변환기) · `MarkdownSyncTest`(실제 엔진)다.

### 시험 자료

| 자리 | 무엇 | 누가 읽나 |
|---|---|---|
| `src/testFixtures/resources/parity/` | `index.json`(기준 커밋 `d15883f` 의 `notes/index.json` 바이트) · `json/<그룹>/<id>.json`(같은 커밋의 노트 바이트) · `md/<그룹>/<id>.md`(그 JSON 을 옮긴 마크다운) | `ParityNotes`(`src/testFixtures`) — 이 모듈 시험과 `:core:data` 시험(`testFixtures(projects.core.format)`), 파이썬 시험은 경로로 |
| 루트 `notes-tests/fixtures/front-matter/` | 앞머리 경계 묶음 — 생성기 시험과 같은 파일 | `MarkdownConverterTest` |
| 루트 `notes/` | 레포의 지금 노트 | `ParityTest` |

- 사본은 고정 값이다. 살아 있는 `notes/` 와 비교하지 않는다 — 다음 노트 쓰기부터 시험이 거짓이 된다(note-format 결정 19).
- 마크다운 사본을 고치면 파리티가 깨진다. 사본은 노트 이전이 그대로 복사해 쓰는 원본이기도 하다.
- 루트 폴더 둘은 `extra["notesInput"] = true`, 카탈로그는 `extra["catalogInput"] = true` 로 `Test` 태스크 입력이다
  (`build-logic/AGENTS.md` "레포 폴더 시험 입력"). 한 글자만 바뀌어도 다시 돈다.
