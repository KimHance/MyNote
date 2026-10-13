#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
from pathlib import Path, PurePosixPath

SUPPORTED_FORMAT = 2
FORMAT_FILE = ".mynote-format"
DEFAULT_NOTES_DIR = "notes"
INDEX_NAME = "index.json"
BRANCH_PREFIX = "refs/heads/"
BOT_NAME = "github-actions[bot]"
BOT_EMAIL = "github-actions[bot]@users.noreply.github.com"
BLOCK_TYPES = frozenset(
    {"heading", "paragraph", "code", "quote", "bulletList", "orderedList", "divider", "spacer"}
)
SPAN_STYLES = frozenset({"text", "bold", "code"})
HEADING_LEVELS = (2, 3)
ENVELOPE_KEYS = frozenset({"id", "title", "date", "blocks"})
OPTIONAL_KEYS = frozenset({"schema"})
NOTE_SUFFIXES = (".json", ".md")
FRONT_MATTER_FENCE = "---"
FRONT_MATTER_KEYS = ("title", "date")
FRONT_MATTER_KEY = re.compile(r"\S+")
DATE_FORMAT = re.compile(r"[0-9]{4}\.[0-9]{2}\.[0-9]{2}")
FALSE_WORDS = frozenset({"false", "0", "no", "off"})


class Failure(Exception):
    def __init__(self, problems):
        super().__init__("; ".join(render_problem(p) for p in problems))
        self.problems = problems


def render_problem(problem):
    path, reason = problem
    return f"{path}: {reason}" if path else reason


def read_format(root):
    path = Path(root) / FORMAT_FILE
    if not path.is_file():
        return 1
    try:
        text = path.read_text(encoding="utf-8").strip()
    except UnicodeDecodeError:
        text = ""
    if not (text.isascii() and text.isdigit()) or int(text) < 1:
        raise Failure([(FORMAT_FILE, f"정수(1 이상)가 아닙니다: '{text}'")])
    version = int(text)
    if version > SUPPORTED_FORMAT:
        raise Failure(
            [(FORMAT_FILE, f"형식 {version} 은 읽을 수 없습니다 (도구 지원 {SUPPORTED_FORMAT})")]
        )
    return version


def clean_notes_dir(notes_dir):
    posix = PurePosixPath(str(notes_dir).replace("\\", "/"))
    if posix.is_absolute() or ".." in posix.parts or posix == PurePosixPath("."):
        raise Failure([(None, f"notes-dir 는 저장소 안의 상대 경로여야 합니다: '{notes_dir}'")])
    return posix.as_posix()


def spans_errors(spans, where):
    if not isinstance(spans, list) or not spans:
        return [f"{where}: spans(비어 있지 않은 배열)가 필요합니다"]
    errors = []
    for k, span in enumerate(spans):
        if not isinstance(span, dict) or not isinstance(span.get("text"), str):
            errors.append(f"{where} spans[{k}]: text(문자열)가 없습니다")
            continue
        style = span.get("style", "text")
        if not isinstance(style, str) or style not in SPAN_STYLES:
            errors.append(f"{where} spans[{k}]: 모르는 style '{style}' (허용: {sorted(SPAN_STYLES)})")
    return errors


def block_errors(block, where):
    if not isinstance(block, dict) or "type" not in block:
        return [f"{where}: 'type' 이 없는 블록입니다"]
    kind = block["type"]
    if not isinstance(kind, str) or kind not in BLOCK_TYPES:
        return [f"{where}: 모르는 block type '{kind}' (허용: {sorted(BLOCK_TYPES)})"]
    errors = []
    if kind == "heading":
        level = block.get("level")
        if type(level) is not int or level not in HEADING_LEVELS:
            errors.append(f"{where}: heading.level 은 {list(HEADING_LEVELS)} 중 하나여야 합니다")
        errors += spans_errors(block.get("spans"), where)
    elif kind in ("paragraph", "quote"):
        errors += spans_errors(block.get("spans"), where)
    elif kind == "code":
        if not isinstance(block.get("code"), str):
            errors.append(f"{where}: code.code(문자열)가 없습니다")
    elif kind in ("bulletList", "orderedList"):
        items = block.get("items")
        if not isinstance(items, list):
            errors.append(f"{where}: {kind}.items 는 배열이어야 합니다")
        else:
            for j, item in enumerate(items):
                spans = item.get("spans") if isinstance(item, dict) else None
                errors += spans_errors(spans, f"{where} items[{j}]")
    return errors


def note_errors(note):
    if not isinstance(note, dict):
        return ["최상위가 객체가 아닙니다"]
    errors = []
    for key in ("id", "title", "date", "blocks"):
        if key not in note:
            errors.append(f"필수 키 '{key}' 가 없습니다")
    for key in ("id", "title", "date"):
        if key in note and not isinstance(note[key], str):
            errors.append(f"'{key}' 는 문자열이어야 합니다")
    extra = set(note) - ENVELOPE_KEYS - OPTIONAL_KEYS
    if extra:
        errors.append(f"알 수 없는 최상위 키 {sorted(extra)}")
    blocks = note.get("blocks")
    if "blocks" in note and not isinstance(blocks, list):
        errors.append("'blocks' 는 배열이어야 합니다")
    if isinstance(blocks, list):
        for i, block in enumerate(blocks):
            errors += block_errors(block, f"blocks[{i}]")
    return errors


def normalize_text(raw):
    text = raw.decode("utf-8")
    if text.startswith("\ufeff"):
        text = text[1:]
    return text.replace("\r\n", "\n").replace("\r", "\n")


def split_front_matter(text):
    lines = text.split("\n")
    if lines[0] != FRONT_MATTER_FENCE:
        return None, text
    for end in range(1, len(lines)):
        if lines[end] == FRONT_MATTER_FENCE:
            return lines[1:end], "\n".join(lines[end + 1:])
    return None, text


def unquote(value):
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        return value[1:-1]
    return value


def front_matter_errors(head):
    fields = {}
    errors = []
    seen = set()
    for line_no, line in enumerate(head, start=2):
        key, colon, value = line.partition(":")
        value = unquote(value)
        at = f"앞머리 {line_no}번째 줄"
        if not colon or not FRONT_MATTER_KEY.fullmatch(key):
            errors.append(f"{at} — '키: 값' 이 아닙니다: {line!r}")
        elif key not in FRONT_MATTER_KEYS:
            errors.append(f"{at} — 모르는 키 '{key}' (허용: {sorted(FRONT_MATTER_KEYS)})")
        elif key in seen:
            errors.append(f"{at} — 키 '{key}' 가 두 번 나옵니다")
        elif not value.strip():
            seen.add(key)
            errors.append(f"{at} — '{key}' 값이 비어 있습니다")
        else:
            seen.add(key)
            fields[key] = value
    for key in FRONT_MATTER_KEYS:
        if key not in seen:
            errors.append(f"앞머리에 '{key}' 가 없습니다")
    date = fields.get("date")
    if date is not None and not DATE_FORMAT.fullmatch(date):
        errors.append(f"'date' 는 YYYY.MM.DD 여야 합니다 — {date!r}")
    return fields, errors


def markdown_note(note_path, raw):
    try:
        text = normalize_text(raw)
    except UnicodeDecodeError:
        return None, ["UTF-8 이 아닙니다"]
    head, _ = split_front_matter(text)
    if head is None:
        if text.split("\n", 1)[0] == FRONT_MATTER_FENCE:
            return None, ["앞머리가 닫히지 않았습니다 — 닫는 '---' 줄이 없습니다"]
        return None, ["앞머리가 없습니다 — 첫 줄이 정확히 '---' 여야 합니다"]
    fields, errors = front_matter_errors(head)
    if errors:
        return None, errors
    return {"id": note_path.stem, "title": fields["title"], "date": fields["date"]}, []


def json_note(raw):
    try:
        note = json.loads(raw)
    except (ValueError, RecursionError) as e:
        return None, [f"JSON 을 읽을 수 없습니다 — {e}"]
    errors = note_errors(note)
    if errors:
        return None, errors
    return {"id": note["id"], "title": note["title"], "date": note["date"]}, []


def collect(root, notes_dir):
    root = Path(root)
    groups = {}
    problems = []
    seen = {}
    count = 0
    for note_path in sorted((root / notes_dir).glob("*/*")):
        if not note_path.is_file():
            continue
        rel = note_path.relative_to(root).as_posix()
        suffix = note_path.suffix
        if suffix not in NOTE_SUFFIXES:
            if suffix.lower() in NOTE_SUFFIXES:
                problems.append((rel, f"확장자는 소문자 '{suffix.lower()}' 여야 합니다"))
            continue
        raw = note_path.read_bytes()
        if suffix == ".md":
            note, errors = markdown_note(note_path, raw)
        else:
            note, errors = json_note(raw)
        if errors:
            problems += [(rel, e) for e in errors]
            continue
        if note["id"] in seen:
            problems.append((rel, f"id '{note['id']}' 중복 (이미 {seen[note['id']]})"))
            continue
        seen[note["id"]] = rel
        groups.setdefault(note_path.parent.name, []).append(
            {**note, "path": rel, "hash": hashlib.sha256(raw).hexdigest()}
        )
        count += 1
    if problems:
        raise Failure(problems)
    return groups, count


def date_key(date):
    return tuple(-ord(c) for c in date)


def render_index(groups):
    index = {
        group: sorted(notes, key=lambda n: (date_key(n["date"]), n["id"]))
        for group, notes in sorted(groups.items())
    }
    return (json.dumps(index, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def run_check(root, notes_dir):
    read_format(root)
    return collect(root, notes_dir)


def run_index(root, notes_dir):
    groups, count = run_check(root, notes_dir)
    path = Path(root) / notes_dir / INDEX_NAME
    data = render_index(groups)
    if not path.is_file() or path.read_bytes() != data:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return path, count, len(groups)


def git(root, *args):
    return subprocess.run(
        ["git", "-C", str(root), *args], capture_output=True, text=True, encoding="utf-8"
    )


def git_failure(result, what):
    detail = (result.stderr or result.stdout or "").strip()
    return Failure([(None, f"{what} 실패: {detail}")])


def commit_index(root, rel_index):
    status = git(root, "status", "--porcelain", "--", rel_index)
    if status.returncode != 0:
        raise git_failure(status, "git status")
    if not status.stdout.strip():
        return False
    added = git(root, "add", "--", rel_index)
    if added.returncode != 0:
        raise git_failure(added, "git add")
    committed = git(
        root,
        "-c", f"user.name={BOT_NAME}",
        "-c", f"user.email={BOT_EMAIL}",
        "-c", "commit.gpgsign=false",
        "-c", f"core.hooksPath={os.devnull}",
        "commit", "-m", f"chore: {rel_index} 갱신", "--", rel_index,
    )
    if committed.returncode != 0:
        raise git_failure(committed, "git commit")
    return True


def push_branch(root, branch):
    return git(root, "push", "origin", f"HEAD:{BRANCH_PREFIX}{branch}")


def run_publish(root, notes_dir, commit, environ):
    ref = environ.get("GITHUB_REF", "")
    if commit and not ref.startswith(BRANCH_PREFIX):
        print(f"브랜치가 아닌 ref('{ref}')라 커밋 · push 없이 끝냅니다.")
        return 0
    branch = ref[len(BRANCH_PREFIX):]
    rel_index = f"{notes_dir}/{INDEX_NAME}"
    path, count, group_count = run_index(root, notes_dir)
    print(f"✓ {rel_index} 생성: 노트 {count}개 / 그룹 {group_count}개")
    if not commit:
        return 0
    if not commit_index(root, rel_index):
        print("바뀐 것이 없어 커밋하지 않습니다.")
        return 0
    pushed = push_branch(root, branch)
    if pushed.returncode == 0:
        print(f"✓ {rel_index} 갱신 커밋을 '{branch}' 에 올렸습니다.")
        return 0
    print("push 가 거절되어 원격을 받아 index 를 다시 만듭니다.", file=sys.stderr)
    fetched = git(root, "fetch", "origin", f"{BRANCH_PREFIX}{branch}")
    if fetched.returncode != 0:
        raise git_failure(fetched, "git fetch")
    reset = git(root, "reset", "--keep", "FETCH_HEAD")
    if reset.returncode != 0:
        raise git_failure(reset, "git reset")
    run_index(root, notes_dir)
    if not commit_index(root, rel_index):
        print("원격에 이미 같은 index 가 있어 커밋하지 않습니다.")
        return 0
    pushed = push_branch(root, branch)
    if pushed.returncode != 0:
        raise git_failure(pushed, "재시도 git push")
    print(f"✓ 재시도 끝에 {rel_index} 갱신 커밋을 '{branch}' 에 올렸습니다.")
    return 0


def annotation_text(text):
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def report(failure, environ):
    for path, reason in failure.problems:
        print(render_problem((path, reason)), file=sys.stderr)
        if environ.get("GITHUB_ACTIONS") == "true":
            where = f"file={annotation_text(path)}::" if path else "::"
            print(f"::error {where}{annotation_text(reason)}")


def want_commit(value):
    return str(value).strip().lower() not in FALSE_WORDS


def build_parser(environ):
    parser = argparse.ArgumentParser(prog="note_tool.py", description="노트 검사 · index 생성 도구")
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument(
        "--root", default=environ.get("GITHUB_WORKSPACE") or os.getcwd(), help="저장소 루트"
    )
    common.add_argument(
        "--notes-dir",
        default=environ.get("NOTES_DIR") or DEFAULT_NOTES_DIR,
        help="저장소 루트 기준 노트 폴더",
    )
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("check", parents=[common], help="노트를 검사합니다")
    sub.add_parser("index", parents=[common], help="검사를 통과하면 index.json 을 씁니다")
    publish = sub.add_parser(
        "publish", parents=[common], help="검사 · 생성 · 바뀐 때만 커밋 · push"
    )
    publish.add_argument(
        "--commit",
        default=environ.get("COMMIT") or "true",
        help="false 면 파일만 쓰고 커밋하지 않습니다",
    )
    return parser


def main(argv=None, environ=None):
    environ = os.environ if environ is None else environ
    args = build_parser(environ).parse_args(argv)
    try:
        notes_dir = clean_notes_dir(args.notes_dir)
        root = Path(args.root)
        if args.command == "check":
            _, count = run_check(root, notes_dir)
            print(f"✓ 노트 {count}개가 규격에 맞습니다")
            return 0
        if args.command == "index":
            _, count, group_count = run_index(root, notes_dir)
            print(f"✓ {notes_dir}/{INDEX_NAME} 생성: 노트 {count}개 / 그룹 {group_count}개")
            return 0
        return run_publish(root, notes_dir, want_commit(args.commit), environ)
    except Failure as failure:
        report(failure, environ)
        return 1


if __name__ == "__main__":
    sys.exit(main())
