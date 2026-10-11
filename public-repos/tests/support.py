import importlib.util
import os
import re
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PUBLIC = REPO_ROOT / "public-repos"
KIT = PUBLIC / "notes-kit"
KIT_TOOL = KIT / "tools" / "note_tool.py"
KIT_SPEC = KIT / "SPEC.md"
GEN_INDEX = REPO_ROOT / "notes" / "gen_index.py"
SCHEMA = REPO_ROOT / "notes" / "SCHEMA.md"
FRONT_MATTER_BUNDLE = REPO_ROOT / "notes-tests" / "fixtures" / "front-matter"
PLACEHOLDERS = PUBLIC / "PLACEHOLDERS.md"

OWNER = "KimHance"
REPO_NAME = "MyNote"

SKIPPED_DIRS = {"__pycache__", ".git"}


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def public_files():
    found = []
    for path in sorted(PUBLIC.rglob("*")):
        rel = path.relative_to(PUBLIC)
        if rel.parts[0] == "tests" or SKIPPED_DIRS & set(rel.parts) or not path.is_file():
            continue
        try:
            text = path.read_bytes().decode("utf-8")
        except UnicodeDecodeError:
            continue
        found.append((rel.as_posix(), text))
    return found


def table_firsts(text, heading):
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip() == heading)
    values = []
    for line in lines[start + 1:]:
        if line.startswith("#"):
            break
        match = re.match(r"\|\s*`([^`]+)`\s*\|", line)
        if match:
            values.append(match.group(1))
    return values


def section(text, heading):
    """heading 줄 아래부터 같은 단계 이상의 다음 제목 앞까지의 줄 목록."""
    lines = text.splitlines()
    start = lines.index(heading)
    level = len(heading) - len(heading.lstrip("#"))
    body = []
    for line in lines[start + 1:]:
        hashes = len(line) - len(line.lstrip("#"))
        if 0 < hashes <= level and line[hashes:hashes + 1] == " ":
            break
        body.append(line)
    return body


def first_table_cells(text, heading):
    """heading 바로 아래 첫 표의 본문 행 첫 칸 — 칸 글자 전체를 앞뒤 공백만 떼고."""
    cells, seen_table = [], False
    for line in section(text, heading):
        if not line.startswith("|"):
            if seen_table:
                break
            continue
        seen_table = True
        first = re.split(r"(?<!\\)\|", line.strip().strip("|"))[0].strip()
        if not set(first) <= set("-: "):
            cells.append(first)
    return cells[1:]


def clean_env(home):
    env = {
        "PATH": os.environ.get("PATH", os.defpath),
        "HOME": str(home),
        "GIT_CONFIG_GLOBAL": os.devnull,
        "GIT_CONFIG_NOSYSTEM": "1",
        "PYTHONDONTWRITEBYTECODE": "1",
    }
    if "SYSTEMROOT" in os.environ:
        env["SYSTEMROOT"] = os.environ["SYSTEMROOT"]
    return env


class TempCase(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.tmp = Path(self._tmp.name).resolve()
        self.env = clean_env(self.tmp)
