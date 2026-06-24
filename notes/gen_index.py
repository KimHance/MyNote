#!/usr/bin/env python3
"""notes/ 하위 노트 파일들로부터 index.json(평면 카탈로그 + hash)을 생성한다.

규칙
- 노트 파일: notes/<group>/<id>.json  (group = 폴더명)
- 각 항목 hash = 노트 파일 raw 바이트의 sha256(hex). 내용이 바뀌면 hash 가 바뀐다.
  앱은 이 hash 를 "비교만" 하고 직접 생성하지 않는다(서버 드리븐 reconcile).
- path = 레포 루트 기준 상대경로(예: "notes/android/compose-state.json").
  앱은 BASE_URL(레포 raw 루트) + path 로 GET 한다.

스키마(SCHEMA.md)는 앱 DTO(:core:data NoteBlockDto/NoteSpanDto)와 1:1. 닫힌 집합을 검증해
앱이 모르는 type/style·필수 필드 누락을 생성 단계에서 막는다(1차 방어선). 하나라도 어기면 index 미생성.

사용법:  python3 notes/gen_index.py    (노트를 추가/수정한 뒤 매번 실행)
"""
import hashlib
import json
import sys
from pathlib import Path

NOTES_DIR = Path(__file__).resolve().parent
INDEX_PATH = NOTES_DIR / "index.json"

# 닫힌 집합 — 앱 DTO 의 @SerialName 과 일치해야 한다(바뀌면 SCHEMA.md·DTO 동시 갱신).
BLOCK_TYPES = {"heading", "paragraph", "code", "quote", "bulletList", "orderedList"}
SPAN_STYLES = {"text", "bold", "code"}
HEADING_LEVELS = {2, 3}
ENVELOPE_KEYS = {"id", "title", "date", "blocks"}


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


def main() -> None:
    # group → 노트항목 목록(항목에 group 필드는 없음 — group 은 상위 키)
    groups: dict[str, list[dict]] = {}
    all_errs: list[str] = []
    seen_ids: dict[str, str] = {}
    count = 0

    for note_path in sorted(NOTES_DIR.glob("*/*.json")):
        raw = note_path.read_bytes()
        rel = note_path.relative_to(NOTES_DIR.parent).as_posix()  # "notes/<group>/<file>"
        try:
            note = json.loads(raw)
        except json.JSONDecodeError as e:
            all_errs.append(f"{rel}: JSON 파싱 실패 — {e}")
            continue

        errs = validate_note(note, rel)
        if errs:
            all_errs += errs
            continue

        nid = note["id"]
        if nid in seen_ids:
            all_errs.append(f"{rel}: id '{nid}' 중복 (이미 {seen_ids[nid]})")
            continue
        seen_ids[nid] = rel

        groups.setdefault(note_path.parent.name, []).append({
            "id": nid,
            "title": note["title"],
            "date": note["date"],
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
