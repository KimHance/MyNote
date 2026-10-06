import ast
import hashlib
import json
import shutil
import sys
import unittest

import helpers
from helpers import note_json


def broken_cases():
    para = [{"type": "paragraph", "spans": [{"text": "x"}]}]
    return {
        "모르는 블록 종류": ("notes/g/bad.json", note_json("bad", blocks=[{"type": "video"}])),
        "모르는 style": (
            "notes/g/bad.json",
            note_json("bad", blocks=[{"type": "paragraph", "spans": [{"text": "x", "style": "italic"}]}]),
        ),
        "level 4": (
            "notes/g/bad.json",
            note_json("bad", blocks=[{"type": "heading", "level": 4, "spans": [{"text": "x"}]}]),
        ),
        "spans 비어 있음": (
            "notes/g/bad.json",
            note_json("bad", blocks=[{"type": "paragraph", "spans": []}]),
        ),
        "JSON 깨짐": ("notes/g/bad.json", '{"id": '),
        "id 중복": ("notes/h/bad.json", note_json("ok", blocks=para)),
        "필수 키 없음": (
            "notes/g/bad.json",
            json.dumps({"id": "bad", "date": "2026.01.01", "blocks": para}),
        ),
        "최상위가 객체가 아님": ("notes/g/bad.json", "[1, 2]"),
        "date 가 문자열이 아님": (
            "notes/g/bad.json",
            json.dumps({"id": "bad", "title": "t", "date": 20260101, "blocks": para}),
        ),
    }


class FormatTest(helpers.ToolCase):
    def repo_with_index(self):
        root = self.make_dir()
        self.write(root, "notes/g/ok.json", note_json("ok"))
        self.write(root, "notes/index.json", "{}\n")
        return root

    def test_NT_2_형식_표시가_없으면_1_이다(self):
        """NT-2 형식 표시가 없으면 1 이다"""
        root = self.repo_with_index()
        self.assertEqual(helpers.load_tool().read_format(root), 1)
        self.assertEqual(self.tool("check", "--root", str(root)).returncode, 0)
        self.write(root, ".mynote-format", "1\n")
        self.assertEqual(helpers.load_tool().read_format(root), 1)
        self.assertEqual(self.tool("index", "--root", str(root)).returncode, 0)

    def test_NT_2_더_높은_형식이면_실패하고_index_를_안_쓴다(self):
        """NT-2 더 높은 형식이면 실패하고 index 를 안 쓴다"""
        root = self.repo_with_index()
        self.write(root, ".mynote-format", "2\n")
        before = self.snapshot(root)
        for command in (["check"], ["index"], ["publish", "--commit", "false"]):
            with self.subTest(command=command):
                result = self.tool(*command, "--root", str(root))
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("형식 2", result.stderr)
                self.assertIn("도구 지원 1", result.stderr)
                self.assertEqual(self.snapshot(root), before)

    def test_NT_2_정수가_아니면_실패한다(self):
        """NT-2 정수가 아니면 실패한다"""
        root = self.repo_with_index()
        before = self.snapshot(root)
        for value in ("abc\n", "1.0\n", "0\n", "-1\n", "\n", "1\n2\n", "１\n"):
            with self.subTest(value=value):
                self.write(root, ".mynote-format", value)
                result = self.tool("index", "--root", str(root))
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(".mynote-format", result.stderr)
                self.assertEqual(self.snapshot(root)["notes/index.json"], before["notes/index.json"])


class CheckTest(helpers.ToolCase):
    def valid_repo(self):
        root = self.make_dir()
        self.write(root, "notes/g/ok.json", note_json("ok"))
        return root

    def test_NT_3_깨진_노트_9종은_각각_파일_경로와_함께_실패한다(self):
        """NT-3 깨진 노트 9종은 각각 파일 경로와 함께 실패한다"""
        cases = broken_cases()
        self.assertEqual(len(cases), 9)
        for name, (rel, text) in cases.items():
            with self.subTest(name):
                root = self.valid_repo()
                self.write(root, rel, text)
                for command in ("check", "index"):
                    result = self.tool(command, "--root", str(root))
                    self.assertNotEqual(result.returncode, 0, f"{command}: {result.stdout}")
                    lines = result.stderr.splitlines()
                    self.assertTrue(
                        any(line.startswith(f"{rel}: ") for line in lines), result.stderr
                    )
                    self.assertFalse(any(line.startswith("notes/g/ok.json") for line in lines))
                shutil.rmtree(root)

    def test_NT_3_오류가_있으면_index_를_쓰지_않는다(self):
        """NT-3 오류가 있으면 index 를 쓰지 않는다"""
        root = self.valid_repo()
        self.write(root, "notes/g/bad.json", "{")
        result = self.tool("index", "--root", str(root))
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((root / "notes" / "index.json").exists())
        self.write(root, "notes/index.json", "이전 내용\n")
        result = self.tool("index", "--root", str(root))
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((root / "notes" / "index.json").read_text(encoding="utf-8"), "이전 내용\n")

    def test_NT_3_index_외에는_아무것도_쓰지_않는다(self):
        """NT-3 index 외에는 아무것도 쓰지 않는다"""
        root = self.valid_repo()
        self.write(root, "notes/g/second.json", note_json("second", "2026.07.01"))
        self.write(root, "notes/SCHEMA.md", "그대로\n")
        self.write(root, ".mynote-format", "1\n")
        self.write(root, "other/file.txt", "그대로\n")
        before = self.snapshot(root)
        result = self.tool("index", "--root", str(root))
        self.assertEqual(result.returncode, 0, result.stderr)
        after = self.snapshot(root)
        self.assertEqual(set(after) - set(before), {"notes/index.json"})
        for rel, data in before.items():
            self.assertEqual(after[rel], data, rel)

    def test_NT_3_notes_dir_이_달라도_path_는_레포_루트_기준이다(self):
        """NT-3 notes-dir 이 달라도 path 는 레포 루트 기준이다"""
        root = self.make_dir()
        self.write(root, "docs/my-notes/kotlin/flow.json", note_json("flow"))
        result = self.tool("index", "--root", str(root), "--notes-dir", "docs/my-notes")
        self.assertEqual(result.returncode, 0, result.stderr)
        index = json.loads((root / "docs/my-notes/index.json").read_text(encoding="utf-8"))
        self.assertEqual(index["kotlin"][0]["path"], "docs/my-notes/kotlin/flow.json")
        self.assertFalse((root / "notes").exists())
        for bad in ("/abs", "/".join(["a", "..", "b"]), "."):
            with self.subTest(bad):
                self.assertNotEqual(
                    self.tool("check", "--root", str(root), "--notes-dir", bad).returncode, 0
                )

    def test_NT_3_두_번_돌려도_같다(self):
        """NT-3 두 번 돌려도 같다"""
        root = self.valid_repo()
        self.write(root, "notes/h/second.json", note_json("second", "2026.07.01"))
        self.assertEqual(self.tool("index", "--root", str(root)).returncode, 0)
        first = self.snapshot(root)
        self.assertEqual(self.tool("index", "--root", str(root)).returncode, 0)
        self.assertEqual(self.snapshot(root), first)
        index = json.loads(first["notes/index.json"])
        self.assertEqual(
            index["g"][0]["hash"], hashlib.sha256(first["notes/g/ok.json"]).hexdigest()
        )

    def test_NT_3_표준_라이브러리만_쓴다(self):
        """NT-3 표준 라이브러리만 쓴다"""
        tree = ast.parse(helpers.TOOL.read_text(encoding="utf-8"))
        imported = set()
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                imported |= {alias.name.split(".")[0] for alias in node.names}
            elif isinstance(node, ast.ImportFrom):
                self.assertEqual(node.level, 0)
                imported.add(node.module.split(".")[0])
        self.assertTrue(imported)
        self.assertEqual(imported - set(sys.stdlib_module_names), set())


if __name__ == "__main__":
    unittest.main()
