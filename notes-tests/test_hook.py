import hashlib
import json
import shutil
import unittest

import support
from support import front_matter


class HookTest(support.TempCase):
    def make_repo(self, name, notes):
        """훅을 켠 임시 저장소에 생성기 · 훅 · 노트를 첫 커밋으로 넣는다."""
        root = self.tmp / name
        root.mkdir()
        self.git(root, "init", "-q")
        self.git(root, "config", "core.hooksPath", ".githooks")
        self.git(root, "config", "commit.gpgsign", "false")
        (root / ".githooks").mkdir()
        shutil.copy(support.HOOK, root / ".githooks" / "pre-commit")
        (root / "notes").mkdir()
        shutil.copy(support.GEN_INDEX, root / "notes" / "gen_index.py")
        for rel, content in notes.items():
            self.write(root, rel, content)
        self.git(root, "add", "-A")
        self.git(root, "commit", "-q", "-m", "start")
        return root

    def committed_files(self, root):
        return set(self.git(root, "show", "--name-only", "--format=", "HEAD").stdout.split())

    def committed_index(self, root):
        return json.loads(self.git(root, "show", "HEAD:notes/index.json").stdout)

    def ids(self, index):
        return {group: [e["id"] for e in entries] for group, entries in index.items()}

    def test_NF_14_마크다운을_커밋하면_목록이_같은_커밋에_들어간다(self):
        """NF-14 마크다운을 커밋하면 목록이 같은 커밋에 들어간다"""
        root = self.make_repo("repo", {"notes/kotlin/base.md": front_matter(title="기준")})
        self.assertIn("notes/index.json", self.committed_files(root))

        flow = front_matter(title="흐름", date="2026.07.01")
        self.write(root, "notes/kotlin/flow.md", flow)
        self.git(root, "add", "notes/kotlin/flow.md")
        self.git(root, "commit", "-q", "-m", "add")
        self.assertEqual(self.committed_files(root), {"notes/kotlin/flow.md", "notes/index.json"})
        entry = self.committed_index(root)["kotlin"][0]
        self.assertEqual((entry["id"], entry["title"]), ("flow", "흐름"))
        self.assertEqual(entry["hash"], hashlib.sha256(flow.encode("utf-8")).hexdigest())

        self.write(root, "notes/kotlin/flow.md", front_matter(title="새 흐름", date="2026.07.01"))
        self.git(root, "add", "notes/kotlin/flow.md")
        self.git(root, "commit", "-q", "-m", "edit")
        self.assertEqual(self.committed_files(root), {"notes/kotlin/flow.md", "notes/index.json"})
        self.assertEqual(self.committed_index(root)["kotlin"][0]["title"], "새 흐름")

        self.git(root, "rm", "-q", "notes/kotlin/flow.md")
        self.git(root, "commit", "-q", "-m", "remove")
        self.assertEqual(self.committed_files(root), {"notes/kotlin/flow.md", "notes/index.json"})
        self.assertEqual(self.ids(self.committed_index(root)), {"kotlin": ["base"]})

    def test_NF_14_이름_바꿈도_목록에_반영된다(self):
        """NF-14 이름 바꿈도 목록에 반영된다"""
        root = self.make_repo(
            "repo",
            {"notes/kotlin/base.md": front_matter(title="기준"), "notes/kotlin/move.md": front_matter(title="옮김")},
        )
        steps = [
            ("notes/kotlin/move.md", "archive/move.md", {"kotlin": ["base"]}),
            ("archive/move.md", "notes/android/move.md", {"android": ["move"], "kotlin": ["base"]}),
            ("notes/android/move.md", "notes/cs/move.md", {"cs": ["move"], "kotlin": ["base"]}),
        ]
        for source, target, expected in steps:
            with self.subTest(f"{source} → {target}"):
                (root / target).parent.mkdir(parents=True, exist_ok=True)
                self.git(root, "mv", source, target)
                self.git(root, "commit", "-q", "-m", f"move {target}")
                self.assertIn("notes/index.json", self.committed_files(root))
                self.assertEqual(self.ids(self.committed_index(root)), expected)

    def test_NF_14_앞머리가_틀리면_커밋이_멈춘다(self):
        """NF-14 앞머리가 틀리면 커밋이 멈춘다"""
        cases = {
            "date 없음": ("notes/kotlin/bad.md", "---\ntitle: 날짜 없음\n---\n본문\n"),
            "대문자 확장자": ("notes/kotlin/upper.MD", front_matter()),
        }
        for name, (rel, content) in cases.items():
            with self.subTest(name):
                root = self.make_repo(f"repo-{len(list(self.tmp.iterdir()))}", {"notes/kotlin/base.md": front_matter()})
                head = self.git(root, "rev-parse", "HEAD").stdout
                index = (root / "notes" / "index.json").read_bytes()
                self.write(root, rel, content)
                self.git(root, "add", rel)

                result = self.git(root, "commit", "-q", "-m", "broken", check=False)

                self.assertNotEqual(result.returncode, 0)
                self.assertIn(rel, result.stderr)
                self.assertEqual(self.git(root, "rev-parse", "HEAD").stdout, head)
                self.assertEqual((root / "notes" / "index.json").read_bytes(), index)


if __name__ == "__main__":
    unittest.main()
