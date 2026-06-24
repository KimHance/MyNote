# 노트 JSON 스키마

이 문서는 `notes/` 노트 파일의 **계약**이다. 데스크탑 write-note 도구와 손편집 모두 이 스키마를 따른다.

> **단일 출처는 앱 DTO** (`:core:data` 의 `NoteBlockDto` / `NoteSpanDto` / `NoteDto`).
> 이 문서/`gen_index.py` 검증은 그 DTO 를 사람이 읽게 옮긴 것일 뿐이다. DTO 가 바뀌면 여기도 같이 바꾼다.
>
> **닫힌 집합 원칙:** 콘텐츠는 서버 드리븐이지만 **블록 타입·인라인 스타일 집합은 앱 버전에 바인딩**된다.
> 앱이 모르는 `type`/`style` 은 렌더할 수 없다 → 새 컴포넌트는 **앱 업데이트**가 선행돼야 한다.
> `gen_index.py` 가 생성 단계에서 미지의 값을 거부하므로, 깨진 노트는 `index.json` 에 실리지 않는다(1차 방어선).

---

## 1. 디렉터리 / 파일 배치

```
notes/
├── index.json              # gen_index.py 가 생성(직접 편집 금지)
├── SCHEMA.md               # 이 문서
├── gen_index.py            # 생성기
└── <group>/<id>.json       # 노트 1개 = 파일 1개. 폴더명 = 카테고리(group)
```

- `group` 은 **폴더명에서 파생**한다 — 노트 파일 안에 `group` 필드를 두지 않는다(중복 금지).
- 파일명은 보통 `<id>.json` (권장). `id` 는 전역 유일해야 한다.

## 2. 노트 envelope

```jsonc
{
  "id": "compose-state",          // string, 전역 유일
  "title": "Compose 상태 관리 정리", // string
  "date": "2026.06.12",           // string, "YYYY.MM.DD"
  "blocks": [ /* Block[] */ ]      // 아래 블록들의 배열(순서 = 렌더 순서)
}
```

- 알 수 없는 추가 키는 앱이 무시한다(`ignoreUnknownKeys = true`). 향후 `"schema": 1` 같은 버전 필드를
  비파괴적으로 더할 수 있다(지금은 불필요).

## 3. Block — discriminator `"type"`

각 블록은 `"type"` 으로 구분되는 닫힌 집합. **허용 type 6종**:

| type | 필드 | 의미 |
|---|---|---|
| `heading` | `level` (int, 2 또는 3), `spans` (Span[]) | 섹션 제목 (H2/H3) |
| `paragraph` | `spans` (Span[]) | 문단 |
| `code` | `code` (string, 멀티라인 가능) | 코드 블록(원문 그대로) |
| `quote` | `spans` (Span[]) | 인용 |
| `bulletList` | `items` (ListItem[]) | 글머리 목록 |
| `orderedList` | `items` (ListItem[]) | 번호 목록 |

```jsonc
{ "type": "heading", "level": 2, "spans": [ { "text": "제목" } ] }
{ "type": "paragraph", "spans": [ { "text": "본문 " }, { "text": "강조", "style": "bold" } ] }
{ "type": "code", "code": "val x = 1\nprintln(x)" }
{ "type": "quote", "spans": [ { "text": "인용문" } ] }
{ "type": "bulletList", "items": [ { "spans": [ { "text": "항목" } ] } ] }
{ "type": "orderedList", "items": [ { "spans": [ { "text": "1번" } ] } ] }
```

- `ListItem` = `{ "spans": Span[] }`.

## 4. Span (인라인) — `style` 닫힌 집합

```jsonc
{ "text": "문자열", "style": "bold" }   // style 생략 시 "text"
```

| style | 의미 |
|---|---|
| `text` | 일반(기본값, 생략 가능) |
| `bold` | 굵게 |
| `code` | 인라인 코드 |

- `heading`/`paragraph`/`quote` 의 `spans`, 그리고 각 `ListItem.spans` 에 쓰인다.
- `code` **블록**은 spans 가 아니라 `code` 문자열을 그대로 받는다(스타일 적용 안 함).

## 5. index.json 형태 (생성물)

`gen_index.py` 가 생성한다(직접 편집 금지). **group 키 → 노트 항목 배열**인 JSON 오브젝트:

```jsonc
{
  "android": [
    { "id": "nav3", "title": "…", "date": "2026.06.18",
      "path": "notes/android/nav3.json", "hash": "8970…" },
    …
  ],
  "kotlin": [ … ],
  "cs": [ … ]
}
```

- `group` 은 **상위 키**이므로 항목엔 group 필드가 없다(폴더명 = group).
- 각 항목 `hash` = **노트 파일 raw 바이트의 `sha256`(hex)**. 앱은 이 값을 **비교만** 한다
  (같으면 skip, 다르면 재fetch). 앱은 hash 를 만들지 않는다.
- 항목 필드: `id` · `title` · `date` · `path`(레포 루트 기준) · `hash`.
- 앱 매핑: 와이어 DTO `IndexNoteDto`(group 없음) → 원격 구현체가 group 을 펼쳐 `IndexEntryDto`(group 포함)로
  평탄화 → reconcile.

## 6. 노트 추가/수정 절차

1. `notes/<group>/<id>.json` 을 이 스키마대로 작성/수정.
2. `python3 notes/gen_index.py` 실행 → `index.json`(+hash) 재생성. 검증 실패 시 어느 노트가 왜 틀렸는지 출력.
3. 커밋/푸시(레포 루트 `notes/`). 앱은 startup 에 `notes/index.json` 을 받아 reconcile 한다.
