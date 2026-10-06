import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

KIT_DIR = Path(__file__).resolve().parents[1]
TOOL = KIT_DIR / "tools" / "note_tool.py"
SPEC = KIT_DIR / "SPEC.md"
ACTION = KIT_DIR / "action.yml"

NOTE = {
    "id": "alpha",
    "title": "알파",
    "date": "2026.06.12",
    "blocks": [{"type": "paragraph", "spans": [{"text": "본문"}]}],
}


def load_tool():
    spec = importlib.util.spec_from_file_location("note_tool_under_test", TOOL)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def isolated_env(home, extra=None):
    env = {
        "PATH": os.environ.get("PATH", os.defpath),
        "HOME": str(home),
        "GIT_CONFIG_GLOBAL": os.devnull,
        "GIT_CONFIG_SYSTEM": os.devnull,
        "GIT_CONFIG_NOSYSTEM": "1",
        "GIT_TERMINAL_PROMPT": "0",
        "LC_ALL": "C.UTF-8",
        "PYTHONDONTWRITEBYTECODE": "1",
    }
    if "SYSTEMROOT" in os.environ:
        env["SYSTEMROOT"] = os.environ["SYSTEMROOT"]
    env.update(extra or {})
    return env


def note_json(note_id="alpha", date="2026.06.12", title=None, blocks=None):
    return json.dumps(
        {
            "id": note_id,
            "title": title or note_id,
            "date": date,
            "blocks": blocks if blocks is not None else NOTE["blocks"],
        },
        ensure_ascii=False,
        indent=2,
    ) + "\n"


class ToolCase(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.tmp = Path(self._tmp.name).resolve()
        self.home = self.tmp / "home"
        self.home.mkdir()
        self.env = isolated_env(self.home)

    def make_dir(self, name="repo"):
        path = self.tmp / name
        path.mkdir(parents=True, exist_ok=True)
        return path

    def write(self, root, rel, text):
        path = Path(root) / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(text, bytes):
            path.write_bytes(text)
        else:
            path.write_text(text, encoding="utf-8")
        return path

    def tool(self, *args, cwd=None, env=None):
        merged = dict(self.env)
        merged.update(env or {})
        return subprocess.run(
            [sys.executable, "-I", str(TOOL), *args],
            capture_output=True,
            text=True,
            encoding="utf-8",
            cwd=str(cwd) if cwd else None,
            env=merged,
        )

    def snapshot(self, root):
        return {
            p.relative_to(root).as_posix(): p.read_bytes()
            for p in sorted(Path(root).rglob("*"))
            if p.is_file() and ".git" not in p.relative_to(root).parts
        }

    def git(self, cwd, *args, env=None, check=True):
        merged = dict(self.env)
        merged.update(env or {})
        result = subprocess.run(
            [
                "git",
                "-c", "user.name=Test Author",
                "-c", "user.email=tester",
                "-c", "commit.gpgsign=false",
                *args,
            ],
            capture_output=True,
            text=True,
            encoding="utf-8",
            cwd=str(cwd),
            env=merged,
        )
        if check and result.returncode != 0:
            raise AssertionError(f"git {args} 실패: {result.stderr}")
        return result


class GitWorld(ToolCase):
    def setUp(self):
        super().setUp()
        self.remote = self.tmp / "remote.git"
        self.git(self.tmp, "init", "--bare", "-b", "main", str(self.remote))
        self.seed = self.clone("seed")
        self.write(self.seed, "README.md", "x\n")
        self.git(self.seed, "add", "-A")
        self.git(self.seed, "commit", "-m", "seed")
        self.git(self.seed, "push", "origin", "HEAD:refs/heads/main")

    def clone(self, name):
        path = self.tmp / name
        self.git(self.tmp, "clone", "-q", str(self.remote), str(path))
        return path

    def add_note_and_push(self, clone, note_id, date="2026.06.12", group="g", branch="main"):
        self.write(clone, f"notes/{group}/{note_id}.json", note_json(note_id, date))
        self.git(clone, "add", "-A")
        self.git(clone, "commit", "-m", f"note {note_id}")
        self.git(clone, "push", "origin", f"HEAD:refs/heads/{branch}")

    def remote_show(self, ref, path):
        result = self.git(self.remote, "show", f"{ref}:{path}", check=False)
        return result.stdout if result.returncode == 0 else None

    def remote_log(self, ref):
        return self.git(self.remote, "log", "--format=%H", ref).stdout.split()

    def publish(self, clone, ref="refs/heads/main", extra_env=None, args=()):
        env = {"GITHUB_REF": ref, "GITHUB_WORKSPACE": str(clone), "GITHUB_ACTIONS": "true"}
        env.update(extra_env or {})
        return self.tool("publish", *args, cwd=clone, env=env)
