#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path, PurePosixPath

SUPPORTED_FORMAT = 1
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


def collect(root, notes_dir):
    root = Path(root)
    groups = {}
    problems = []
    seen = {}
    count = 0
    for note_path in sorted((root / notes_dir).glob("*/*.json")):
        if not note_path.is_file():
            continue
        raw = note_path.read_bytes()
        rel = note_path.relative_to(root).as_posix()
        try:
            note = json.loads(raw)
        except (ValueError, RecursionError) as e:
            problems.append((rel, f"JSON 을 읽을 수 없습니다 — {e}"))
            continue
        errors = note_errors(note)
        if errors:
            problems += [(rel, e) for e in errors]
            continue
        if note["id"] in seen:
            problems.append((rel, f"id '{note['id']}' 중복 (이미 {seen[note['id']]})"))
            continue
        seen[note["id"]] = rel
        groups.setdefault(note_path.parent.name, []).append(
            {
                "id": note["id"],
                "title": note["title"],
                "date": note["date"],
                "path": rel,
                "hash": hashlib.sha256(raw).hexdigest(),
            }
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
