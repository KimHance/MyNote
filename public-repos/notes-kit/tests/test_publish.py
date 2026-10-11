import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import textwrap
import unittest
from unittest import mock

import helpers
from helpers import note_json

HOOK_SCRIPT = textwrap.dedent(
    """\
    import json, os, subprocess, sys
    counter, other, limit = sys.argv[1], sys.argv[2], int(sys.argv[3])
    n = int(open(counter).read()) + 1 if os.path.exists(counter) else 1
    open(counter, "w").write(str(n))
    if n > limit:
        sys.exit(0)
    env = {k: v for k, v in os.environ.items() if k not in ("GIT_DIR", "GIT_WORK_TREE", "GIT_INDEX_FILE", "GIT_PREFIX")}
    def git(*args):
        subprocess.run(["git", "-c", "user.name=Other", "-c", "user.email=other", "-c", "commit.gpgsign=false", *args], cwd=other, env=env, check=True, capture_output=True)
    git("fetch", "-q", "origin")
    git("reset", "-q", "--hard", "origin/main")
    os.makedirs(os.path.join(other, "notes", "g"), exist_ok=True)
    note = {"id": "other%d" % n, "title": "다른 노트 %d" % n, "date": "2026.07.0%d" % n, "blocks": [{"type": "divider"}]}
    open(os.path.join(other, "notes", "g", "other%d.json" % n), "w", encoding="utf-8").write(json.dumps(note, ensure_ascii=False) + "\\n")
    git("add", "-A")
    git("commit", "-q", "-m", "다른 노트 %d" % n)
    git("push", "-q", "origin", "HEAD:refs/heads/main")
    """
)


class PublishTest(helpers.GitWorld):
    def first_publish(self, ref="refs/heads/main"):
        work = self.clone("work")
        self.add_note_and_push(work, "alpha")
        result = self.publish(work, ref)
        self.assertEqual(result.returncode, 0, result.stderr)
        return work

    def index_at(self, ref="main"):
        return json.loads(self.remote_show(ref, "notes/index.json"))

    def install_hook(self, work, limit):
        script = self.tmp / "hook_push.py"
        script.write_text(HOOK_SCRIPT, encoding="utf-8")
        other = self.clone("other")
        hook = work / ".git" / "hooks" / "pre-push"
        hook.write_text(
            f'#!/bin/sh\nexec "{sys.executable}" -I "{script}" "{self.tmp / "hook_count"}" "{other}" {limit}\n',
            encoding="utf-8",
        )
        hook.chmod(hook.stat().st_mode | stat.S_IXUSR)

    def hook_count(self):
        return int((self.tmp / "hook_count").read_text())

    def test_NT_4_새_노트를_올리면_index_갱신_커밋이_하나_생긴다(self):
        """NT-4 새 노트를 올리면 index 갱신 커밋이 하나 생긴다"""
        work = self.clone("work")
        self.add_note_and_push(work, "alpha")
        before = self.remote_log("main")
        self.assertIsNone(self.remote_show("main", "notes/index.json"))
        result = self.publish(work)
        self.assertEqual(result.returncode, 0, result.stderr)
        after = self.remote_log("main")
        self.assertEqual(len(after), len(before) + 1)
        self.assertEqual(after[1:], before)
        note_bytes = self.git(work, "show", "HEAD~1:notes/g/alpha.json").stdout.encode("utf-8")
        entry = self.index_at()["g"][0]
        self.assertEqual(entry["hash"], hashlib.sha256(note_bytes).hexdigest())
        self.assertEqual(entry["path"], "notes/g/alpha.json")
        author = self.git(self.remote, "log", "-1", "--format=%an <%ae>", "main").stdout.strip()
        self.assertEqual(author, "github-actions[bot] <github-actions[bot]@users.noreply.github.com>")
        self.assertEqual(self.git(work, "rev-parse", "HEAD").stdout, self.git(self.remote, "rev-parse", "main").stdout)

    def test_NT_4_마크다운_노트를_올려도_index_갱신_커밋이_하나_생긴다(self):
        """NT-4 마크다운 노트를 올려도 index 갱신 커밋이 하나 생긴다"""
        work = self.clone("work")
        self.add_note_and_push(work, "alpha", markdown=True)
        before = self.remote_log("main")
        result = self.publish(work)
        self.assertEqual(result.returncode, 0, result.stderr)
        after = self.remote_log("main")
        self.assertEqual(len(after), len(before) + 1)
        self.assertEqual(after[1:], before)
        changed = self.git(
            self.remote, "diff-tree", "--no-commit-id", "--name-only", "-r", "main"
        ).stdout.split()
        self.assertEqual(changed, ["notes/index.json"])
        note_bytes = (work / "notes" / "g" / "alpha.md").read_bytes()
        entry = self.index_at()["g"][0]
        self.assertEqual(entry["path"], "notes/g/alpha.md")
        self.assertEqual((entry["id"], entry["title"]), ("alpha", "alpha"))
        self.assertEqual(entry["hash"], hashlib.sha256(note_bytes).hexdigest())

    def test_NT_4_봇_커밋은_index_만_바꾼다(self):
        """NT-4 봇 커밋은 index 만 바꾼다"""
        work = self.clone("work")
        self.add_note_and_push(work, "alpha")
        self.write(work, "notes/g/untracked-extra.json", note_json("extra"))
        self.git(work, "add", "notes/g/untracked-extra.json")
        result = self.publish(work)
        self.assertEqual(result.returncode, 0, result.stderr)
        changed = self.git(
            self.remote, "diff-tree", "--no-commit-id", "--name-only", "-r", "main"
        ).stdout.split()
        self.assertEqual(changed, ["notes/index.json"])
        staged = self.git(work, "diff", "--cached", "--name-only").stdout.split()
        self.assertEqual(staged, ["notes/g/untracked-extra.json"])

    def test_NT_4_바뀐_게_없으면_커밋하지_않는다(self):
        """NT-4 바뀐 게 없으면 커밋하지 않는다"""
        work = self.first_publish()
        before = self.remote_log("main")
        result = self.publish(work)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.remote_log("main"), before)
        self.assertEqual(self.git(work, "status", "--porcelain").stdout, "")

    def test_NT_4_깨진_노트는_커밋_없이_실패한다(self):
        """NT-4 깨진 노트는 커밋 없이 실패한다"""
        work = self.first_publish()
        good_index = self.remote_show("main", "notes/index.json")
        tip = self.remote_log("main")
        self.write(work, "notes/g/broken.json", '{"id": "broken"')
        self.git(work, "add", "-A")
        self.git(work, "commit", "-m", "broken")
        self.git(work, "push", "origin", "HEAD:refs/heads/main")
        tip = self.remote_log("main")
        result = self.publish(work)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("notes/g/broken.json: ", result.stderr)
        self.assertIn("::error file=notes/g/broken.json::", result.stdout)
        self.assertEqual(self.remote_log("main"), tip)
        self.assertEqual(self.remote_show("main", "notes/index.json"), good_index)
        self.assertEqual(self.git(work, "rev-parse", "HEAD").stdout, self.git(self.remote, "rev-parse", "main").stdout)

        self.git(work, "rm", "-q", "notes/g/broken.json")
        self.write(work, "notes/g/no-date.md", "---\ntitle: 날짜 없음\n---\n본문\n")
        self.git(work, "add", "-A")
        self.git(work, "commit", "-m", "broken markdown")
        self.git(work, "push", "origin", "HEAD:refs/heads/main")
        tip = self.remote_log("main")
        result = self.publish(work)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("notes/g/no-date.md: ", result.stderr)
        self.assertIn("::error file=notes/g/no-date.md::", result.stdout)
        self.assertEqual(self.remote_log("main"), tip)
        self.assertEqual(self.remote_show("main", "notes/index.json"), good_index)

    def test_NT_4_commit_을_끄면_커밋하지_않는다(self):
        """NT-4 commit 을 끄면 커밋하지 않는다"""
        work = self.clone("work")
        self.add_note_and_push(work, "alpha")
        before = self.remote_log("main")
        local_before = self.git(work, "rev-parse", "HEAD").stdout
        result = self.publish(work, extra_env={"COMMIT": "false"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue((work / "notes" / "index.json").is_file())
        self.assertEqual(self.remote_log("main"), before)
        self.assertEqual(self.git(work, "rev-parse", "HEAD").stdout, local_before)
        self.assertEqual(self.git(work, "status", "--porcelain").stdout.strip(), "?? notes/index.json")

    def test_NT_4_다른_브랜치에서도_그_브랜치에_맞춘다(self):
        """NT-4 다른 브랜치에서도 그 브랜치에 맞춘다"""
        work = self.clone("work")
        self.git(work, "checkout", "-q", "-b", "feature/notes-1")
        self.add_note_and_push(work, "alpha", branch="feature/notes-1")
        main_before = self.remote_log("main")
        result = self.publish(work, "refs/heads/feature/notes-1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.remote_log("main"), main_before)
        self.assertEqual(len(self.remote_log("feature/notes-1")), len(main_before) + 2)
        self.assertEqual(self.index_at("feature/notes-1")["g"][0]["id"], "alpha")

    def test_NT_4_그_사이_원격이_앞서가도_재시도로_올라간다(self):
        """NT-4 그 사이 원격이 앞서가도 재시도로 올라간다"""
        work = self.clone("work")
        self.add_note_and_push(work, "alpha")
        self.install_hook(work, limit=1)
        result = self.publish(work)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.hook_count(), 2)
        ids = {e["id"] for e in self.index_at()["g"]}
        self.assertEqual(ids, {"alpha", "other1"})
        changed = self.git(
            self.remote, "diff-tree", "--no-commit-id", "--name-only", "-r", "main"
        ).stdout.split()
        self.assertEqual(changed, ["notes/index.json"])
        self.assertEqual(self.git(work, "status", "--porcelain").stdout, "")

    def test_NT_4_재시도도_거절되면_실패한다(self):
        """NT-4 재시도도 거절되면 실패한다"""
        work = self.clone("work")
        self.add_note_and_push(work, "alpha")
        self.install_hook(work, limit=99)
        result = self.publish(work)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.hook_count(), 2)
        self.assertIsNone(self.remote_show("main", "notes/index.json"))
        self.assertIn("재시도", result.stderr)

    def test_NT_4_태그_push_는_커밋_없이_끝난다(self):
        """NT-4 태그 push 는 커밋 없이 끝난다"""
        work = self.clone("work")
        self.add_note_and_push(work, "alpha")
        before = self.remote_log("main")
        local_before = self.git(work, "rev-parse", "HEAD").stdout
        for ref in ("refs/tags/v1.0.0", "", "refs/pull/1/merge"):
            with self.subTest(ref=ref):
                result = self.publish(work, ref)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(len(result.stdout.strip().splitlines()), 1)
                self.assertEqual(self.remote_log("main"), before)
                self.assertEqual(self.git(work, "rev-parse", "HEAD").stdout, local_before)
                self.assertFalse((work / "notes" / "index.json").exists())

    def test_NT_4_기기의_전역_git_설정과_무관하게_돈다(self):
        """NT-4 기기의 전역 git 설정과 무관하게 돈다"""
        hostile_home = self.tmp / "hostile-home"
        hooks = hostile_home / "hooks"
        hooks.mkdir(parents=True)
        for name in ("pre-commit", "commit-msg"):
            hook = hooks / name
            hook.write_text("#!/bin/sh\nexit 1\n", encoding="utf-8")
            hook.chmod(0o755)
        config = hostile_home / "gitconfig"
        config.write_text(
            f"[core]\n\thooksPath = {hooks}\n[commit]\n\tgpgsign = true\n"
            "[gpg]\n\tprogram = false\n[user]\n\tuseConfigOnly = true\n",
            encoding="utf-8",
        )
        hostile = {"HOME": str(hostile_home), "GIT_CONFIG_GLOBAL": str(config), "GIT_CONFIG_NOSYSTEM": "1"}
        with mock.patch.dict(os.environ, hostile):
            probe = self.make_dir("probe")
            subprocess.run(["git", "init", "-q", str(probe)], check=True, env=os.environ)
            (probe / "f").write_text("x", encoding="utf-8")
            subprocess.run(["git", "-C", str(probe), "add", "f"], check=True, env=os.environ)
            control = subprocess.run(
                ["git", "-C", str(probe), "commit", "-q", "-m", "x"],
                capture_output=True,
                env=os.environ,
            )
            self.assertNotEqual(control.returncode, 0)
            fresh = self.clone("hostile-work")
            self.add_note_and_push(fresh, "alpha")
            result = self.publish(fresh)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(self.index_at()["g"][0]["id"], "alpha")
        stripped = dict(self.env)
        stripped.update(hostile)
        fresh2 = self.clone("hostile-work-2")
        self.add_note_and_push(fresh2, "beta", group="h")
        direct = subprocess.run(
            [sys.executable, "-I", str(helpers.TOOL), "publish", "--root", str(fresh2)],
            capture_output=True,
            text=True,
            env={**stripped, "GITHUB_REF": "refs/heads/main"},
        )
        self.assertEqual(direct.returncode, 0, direct.stderr)
        self.assertEqual({e["id"] for g in self.index_at().values() for e in g}, {"alpha", "beta"})


class ActionYamlTest(unittest.TestCase):
    def setUp(self):
        self.lines = helpers.ACTION.read_text(encoding="utf-8").splitlines()

    def block(self, header):
        start = self.lines.index(header)
        out = []
        for line in self.lines[start + 1:]:
            if line and not line.startswith(" "):
                break
            out.append(line)
        return out

    def test_NT_4_action_yml_줄_구조(self):
        """NT-4 action.yml 줄 구조"""
        self.assertIn("  using: composite", self.lines)
        inputs = self.block("inputs:")
        names = [m.group(1) for m in (re.match(r"^  ([\w-]+):$", l) for l in inputs) if m]
        self.assertEqual(names, ["notes-dir", "commit"])
        self.assertIn("    default: notes", inputs)
        self.assertIn('    default: "true"', inputs)
        steps = self.block("  steps:")
        self.assertEqual(len([l for l in steps if l.startswith("    - ")]), 1)
        runs = [l for l in steps if l.strip().startswith("run:")]
        self.assertEqual(len(runs), 1)
        self.assertIn('"$GITHUB_ACTION_PATH/tools/note_tool.py" publish', runs[0])
        self.assertNotIn("${{", runs[0])
        self.assertIn("        NOTES_DIR: ${{ inputs.notes-dir }}", steps)
        self.assertIn("        COMMIT: ${{ inputs.commit }}", steps)
        text = "\n".join(self.lines)
        self.assertNotIn("checkout", text)
        self.assertEqual([l for l in self.lines if re.match(r"^\s*-?\s*uses:", l)], [])


if __name__ == "__main__":
    unittest.main()
