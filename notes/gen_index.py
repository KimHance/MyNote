#!/usr/bin/env python3
"""notes/ 하위 노트 파일들로부터 index.json(평면 카탈로그 + hash)을 생성한다.

규칙
- 노트 파일: notes/<group>/<id>.md (앞머리 + 마크다운) 또는 notes/<group>/<id>.json (옛 형식)
  group = 폴더명. .md 의 id 는 파일 이름, 제목 · 날짜는 앞머리. id 는 두 형식을 통틀어 유일하다.
- 각 항목 hash = 노트 파일 raw 바이트의 sha256(hex). 내용이 바뀌면 hash 가 바뀐다.
  앱은 이 hash 를 "비교만" 하고 직접 생성하지 않는다(서버 드리븐 reconcile).
- path = 레포 루트 기준 상대경로(예: "notes/android/compose-state.md").
  앱은 BASE_URL(레포 raw 루트) + path 로 GET 한다.

검사 규칙은 SCHEMA.md 를 따른다. 마크다운은 앞머리만 보고 본문은 보지 않는다(앱이 어떤 본문도 읽는다).
JSON 은 닫힌 집합을 검증해 앱이 모르는 type/style·필수 필드 누락을 생성 단계에서 막는다(1차 방어선).
하나라도 어기면 index 미생성.

사용법:  python3 notes/gen_index.py    (커밋 때 훅이 돌린다)
"""
import hashlib
import json
import re
import sys
from pathlib import Path

NOTES_DIR = Path(__file__).resolve().parent
INDEX_PATH = NOTES_DIR / "index.json"
NOTE_SUFFIXES = (".json", ".md")

# JSON 형식 1 의 닫힌 집합 — 앱 DTO 의 @SerialName · 공개 도구와 일치해야 한다.
BLOCK_TYPES = {"heading", "paragraph", "code", "quote", "bulletList", "orderedList", "divider", "spacer"}
SPAN_STYLES = {"text", "bold", "code"}
HEADING_LEVELS = {2, 3}
ENVELOPE_KEYS = {"id", "title", "date", "blocks"}

FRONT_MATTER_FENCE = "---"
FRONT_MATTER_KEYS = ("title", "date")
FRONT_MATTER_KEY = re.compile(r"\S+")
DATE_FORMAT = re.compile(r"[0-9]{4}\.[0-9]{2}\.[0-9]{2}")


def validate_note(note: dict, where: str) -> list[str]:
    """노트 1개를 스키마로 검증, 에러 메시지 목록 반환(빈 목록 = 통과)."""
    errs: list[str] = []

    for key in ("id", "title", "date", "blocks"):
        if key not in note:
            errs.append(f"{where}: 필수 키 '{key}' 누락")
    extra = set(note) - ENVELOPE_KEYS - {"schema"}  # schema 버전 필드는 향후 허용
    if extra:
        errs.append(f"{where}: 알 수 없는 최상위 키 {sorted(extra)}")
    if not isinstance(note.get("blocks"), list):
        errs.append(f"{where}: 'blocks' 는 배열이어야 함")
        return errs

    for i, block in enumerate(note["blocks"]):
        errs += _validate_block(block, f"{where} blocks[{i}]")
    return errs


def _validate_block(block: dict, where: str) -> list[str]:
    if not isinstance(block, dict) or "type" not in block:
        return [f"{where}: 'type' 없는 블록"]
    t = block["type"]
    if t not in BLOCK_TYPES:
        return [f"{where}: 미지의 block type '{t}' (허용: {sorted(BLOCK_TYPES)})"]

    errs: list[str] = []
    if t == "heading":
        if block.get("level") not in HEADING_LEVELS:
            errs.append(f"{where}: heading.level 은 {sorted(HEADING_LEVELS)} 중 하나여야 함")
        errs += _validate_spans(block.get("spans"), where)
    elif t in ("paragraph", "quote"):
        errs += _validate_spans(block.get("spans"), where)
    elif t == "code":
        if not isinstance(block.get("code"), str):
            errs.append(f"{where}: code.code(string) 누락")
    elif t in ("bulletList", "orderedList"):
        items = block.get("items")
        if not isinstance(items, list):
            errs.append(f"{where}: {t}.items 는 배열이어야 함")
        else:
            for j, item in enumerate(items):
                errs += _validate_spans(
                    item.get("spans") if isinstance(item, dict) else None,
                    f"{where} items[{j}]",
                )
    return errs


def _validate_spans(spans, where: str) -> list[str]:
    if not isinstance(spans, list) or not spans:
        return [f"{where}: spans(비어있지 않은 배열) 필요"]
    errs: list[str] = []
    for k, span in enumerate(spans):
        if not isinstance(span, dict) or not isinstance(span.get("text"), str):
            errs.append(f"{where} spans[{k}]: text(string) 누락")
            continue
        style = span.get("style", "text")
        if style not in SPAN_STYLES:
            errs.append(f"{where} spans[{k}]: 미지의 style '{style}' (허용: {sorted(SPAN_STYLES)})")
    return errs


def normalize_text(raw: bytes) -> str:
    """노트 바이트를 앱과 같은 글로 읽는다 — UTF-8 · 맨 앞 BOM 뗌 · 줄 끝(CRLF · CR)은 LF."""
    text = raw.decode("utf-8")
    if text.startswith("\ufeff"):
        text = text[1:]
    return text.replace("\r\n", "\n").replace("\r", "\n")


def split_front_matter(text: str):
    """정규화한 글을 (앞머리 줄 목록, 본문)으로 가른다. 경계가 없거나 닫히지 않으면 (None, 글 전체)."""
    lines = text.split("\n")
    if lines[0] != FRONT_MATTER_FENCE:
        return None, text
    for end in range(1, len(lines)):
        if lines[end] == FRONT_MATTER_FENCE:
            return lines[1:end], "\n".join(lines[end + 1:])
    return None, text


def validate_front_matter(head: list[str], where: str) -> tuple[dict[str, str], list[str]]:
    """앞머리 줄을 읽어 (키 → 값, 에러 메시지 목록)을 돌려준다."""
    fields: dict[str, str] = {}
    errs: list[str] = []
    seen: set[str] = set()
    for line_no, line in enumerate(head, start=2):
        key, colon, value = line.partition(":")
        value = _unquote(value)
        at = f"{where}: 앞머리 {line_no}번째 줄"
        if not colon or not FRONT_MATTER_KEY.fullmatch(key):
            errs.append(f"{at} — '키: 값' 이 아님: {line!r}")
        elif key not in FRONT_MATTER_KEYS:
            errs.append(f"{at} — 모르는 키 '{key}' (허용: {sorted(FRONT_MATTER_KEYS)})")
        elif key in seen:
            errs.append(f"{at} — 키 '{key}' 가 두 번 나옴")
        elif not value.strip():
            seen.add(key)
            errs.append(f"{at} — '{key}' 값이 비어 있음")
        else:
            seen.add(key)
            fields[key] = value

    for key in FRONT_MATTER_KEYS:
        if key not in seen:
            errs.append(f"{where}: 앞머리에 '{key}' 없음")
    date = fields.get("date")
    if date is not None and not DATE_FORMAT.fullmatch(date):
        errs.append(f"{where}: 'date' 는 YYYY.MM.DD 여야 함 — {date!r}")
    return fields, errs


def _unquote(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        return value[1:-1]
    return value


def _markdown_note(note_path: Path, raw: bytes, where: str) -> tuple[dict, list[str]]:
    try:
        text = normalize_text(raw)
    except UnicodeDecodeError:
        return {}, [f"{where}: UTF-8 이 아님"]
    head, _ = split_front_matter(text)
    if head is None:
        if text.split("\n", 1)[0] == FRONT_MATTER_FENCE:
            return {}, [f"{where}: 앞머리가 닫히지 않음 — 닫는 '---' 줄이 없음"]
        return {}, [f"{where}: 앞머리 없음 — 첫 줄이 정확히 '---' 여야 함"]
    fields, errs = validate_front_matter(head, where)
    if errs:
        return {}, errs
    return {"id": note_path.stem, "title": fields["title"], "date": fields["date"]}, []


def _json_note(raw: bytes, where: str) -> tuple[dict, list[str]]:
    try:
        note = json.loads(raw)
    except json.JSONDecodeError as e:
        return {}, [f"{where}: JSON 파싱 실패 — {e}"]
    errs = validate_note(note, where)
    if errs:
        return {}, errs
    return {"id": note["id"], "title": note["title"], "date": note["date"]}, []


def main() -> None:
    # group → 노트항목 목록(항목에 group 필드는 없음 — group 은 상위 키)
    groups: dict[str, list[dict]] = {}
    all_errs: list[str] = []
    seen_ids: dict[str, str] = {}
    count = 0

    for note_path in sorted(NOTES_DIR.glob("*/*")):
        if not note_path.is_file():
            continue
        rel = note_path.relative_to(NOTES_DIR.parent).as_posix()  # "notes/<group>/<file>"
        suffix = note_path.suffix
        if suffix not in NOTE_SUFFIXES:
            if suffix.lower() in NOTE_SUFFIXES:
                all_errs.append(f"{rel}: 확장자는 소문자 '{suffix.lower()}' 여야 함")
            continue

        raw = note_path.read_bytes()
        if suffix == ".md":
            note, errs = _markdown_note(note_path, raw, rel)
        else:
            note, errs = _json_note(raw, rel)
        if errs:
            all_errs += errs
            continue

        nid = note["id"]
        if nid in seen_ids:
            all_errs.append(f"{rel}: id '{nid}' 중복 (이미 {seen_ids[nid]})")
            continue
        seen_ids[nid] = rel

        groups.setdefault(note_path.parent.name, []).append({
            **note,
            "path": rel,
            "hash": hashlib.sha256(raw).hexdigest(),
        })
        count += 1

    if all_errs:
        print("✗ 스키마 검증 실패 — index.json 을 생성하지 않음:", file=sys.stderr)
        for e in all_errs:
            print(f"  - {e}", file=sys.stderr)
        sys.exit(1)

    # 결정적 출력: group 키 알파벳순, 그룹 내 date 내림차순 → id
    index = {
        group: sorted(notes, key=lambda n: (_date_key(n["date"]), n["id"]))
        for group, notes in sorted(groups.items())
    }

    INDEX_PATH.write_text(
        json.dumps(index, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"✓ index.json 생성: {count}개 노트 / {len(index)}개 그룹")
    for group, notes in index.items():
        for n in notes:
            print(f"  {group}/{n['id']}  {n['hash'][:12]}…  {n['path']}")


def _date_key(date: str) -> str:
    # "2026.06.12" → 내림차순 정렬용 역키
    return "".join(chr(255 - ord(c)) for c in date)


if __name__ == "__main__":
    main()
