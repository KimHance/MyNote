import importlib.util
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
GEN_INDEX = REPO_ROOT / "notes" / "gen_index.py"
HOOK = REPO_ROOT / ".githooks" / "pre-commit"
SCHEMA = REPO_ROOT / "notes" / "SCHEMA.md"
WRITE_NOTE = REPO_ROOT / ".claude" / "skills" / "write-note" / "SKILL.md"
FRONT_MATTER_BUNDLE = Path(__file__).resolve().parent / "fixtures" / "front-matter"

PRIOR_INDEX = b'{"prior": []}\n'


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def clean_env(home):
    env = {
        "PATH": os.environ.get("PATH", os.defpath),
        "HOME": str(home),
        "GIT_CONFIG_GLOBAL": os.devnull,
        "GIT_CONFIG_NOSYSTEM": "1",
        "GIT_TERMINAL_PROMPT": "0",
        "GIT_AUTHOR_NAME": "notes-tests",
        "GIT_AUTHOR_EMAIL": "notes-tests@example.invalid",
        "GIT_COMMITTER_NAME": "notes-tests",
        "GIT_COMMITTER_EMAIL": "notes-tests@example.invalid",
        "LC_ALL": "C.UTF-8",
        "PYTHONDONTWRITEBYTECODE": "1",
    }
    if "SYSTEMROOT" in os.environ:
        env["SYSTEMROOT"] = os.environ["SYSTEMROOT"]
    return env


def front_matter(title="제목", date="2026.06.12", body="본문\n"):
    return f"---\ntitle: {title}\ndate: {date}\n---\n{body}"


class TempCase(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.tmp = Path(self._tmp.name).resolve()
        self.home = self.tmp / "home"
        self.home.mkdir()
        self.env = clean_env(self.home)

    def write(self, root, rel, content):
        path = Path(root) / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content if isinstance(content, bytes) else content.encode("utf-8"))
        return path

    def notes_tree(self, name, notes, index=None):
        """생성기 사본과 노트(경로 → 내용)를 든 저장소 루트를 만든다."""
        root = self.tmp / name
        (root / "notes").mkdir(parents=True)
        (root / "notes" / "gen_index.py").write_bytes(GEN_INDEX.read_bytes())
        if index is not None:
            (root / "notes" / "index.json").write_bytes(index)
        for rel, content in notes.items():
            self.write(root / "notes", rel, content)
        return root

    def generate(self, root):
        return subprocess.run(
            [sys.executable, "-I", "notes/gen_index.py"],
            capture_output=True,
            text=True,
            cwd=str(root),
            env=self.env,
        )

    def git(self, root, *args, check=True):
        result = subprocess.run(
            ["git", *args],
            capture_output=True,
            text=True,
            cwd=str(root),
            env=self.env,
        )
        if check:
            self.assertEqual(result.returncode, 0, result.stderr)
        return result
