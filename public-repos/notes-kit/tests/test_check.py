import ast
import hashlib
import json
import shutil
import sys
import unittest

import helpers
from helpers import note_json, note_md


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


def broken_markdown_cases():
    good = "---\ntitle: a\ndate: 2026.10.07\n---\n본문\n"
    return {
        "앞머리 없음": ("notes/g/bad.md", "# 제목\n본문\n"),
        "닫히지 않음": ("notes/g/bad.md", "---\ntitle: a\ndate: 2026.10.07\n본문\n"),
        "title 없음": ("notes/g/bad.md", "---\ndate: 2026.10.07\n---\n"),
        "date 없음": ("notes/g/bad.md", "---\ntitle: a\n---\n"),
        "date 모양 틀림": ("notes/g/bad.md", "---\ntitle: a\ndate: 2026-10-07\n---\n"),
        "모르는 키": ("notes/g/bad.md", "---\ntitle: a\ndate: 2026.10.07\ntags: x\n---\n"),
        "빈 값": ("notes/g/bad.md", '---\ntitle: ""\ndate: 2026.10.07\n---\n'),
        "같은 키 두 번": ("notes/g/bad.md", "---\ntitle: a\ntitle: b\ndate: 2026.10.07\n---\n"),
        "키: 값 이 아닌 줄": ("notes/g/bad.md", "---\ntitle a\ntitle: a\ndate: 2026.10.07\n---\n"),
        "UTF-8 아님": ("notes/g/bad.md", "---\ntitle: 가\ndate: 2026.10.07\n---\n".encode("cp949")),
        "확장자 .MD": ("notes/g/bad.MD", good),
        "같은 id 의 .md 와 .json": ("notes/h/ok.md", good),
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

    def test_NT_2_형식_2_를_읽는다(self):
        """NT-2 형식 2 를 읽는다"""
        root = self.repo_with_index()
        self.write(root, ".mynote-format", "2\n")
        self.assertEqual(helpers.load_tool().read_format(root), 2)
        self.assertEqual(self.tool("check", "--root", str(root)).returncode, 0)
        result = self.tool("index", "--root", str(root))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads((root / "notes" / "index.json").read_bytes())["g"][0]["id"], "ok")

    def test_NT_2_더_높은_형식이면_실패하고_index_를_안_쓴다(self):
        """NT-2 더 높은 형식이면 실패하고 index 를 안 쓴다"""
        root = self.repo_with_index()
        self.write(root, ".mynote-format", "3\n")
        before = self.snapshot(root)
        for command in (["check"], ["index"], ["publish", "--commit", "false"]):
            with self.subTest(command=command):
                result = self.tool(*command, "--root", str(root))
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("형식 3", result.stderr)
                self.assertIn("도구 지원 2", result.stderr)
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


    def test_NT_2_형식_표시와_상관없이_마크다운을_읽는다(self):
        """NT-2 형식 표시와 상관없이 마크다운을 읽는다"""
        for marker in (None, "1\n", "2\n"):
            with self.subTest(marker=marker):
                root = self.make_dir(f"repo-{marker!r}")
                self.write(root, "notes/g/ok.json", note_json("ok"))
                self.write(root, "notes/g/memo.md", note_md("메모", "2026.07.01"))
                if marker is not None:
                    self.write(root, ".mynote-format", marker)
                result = self.tool("index", "--root", str(root))
                self.assertEqual(result.returncode, 0, result.stderr)
                index = json.loads((root / "notes" / "index.json").read_bytes())
                self.assertEqual([e["path"] for e in index["g"]], ["notes/g/memo.md", "notes/g/ok.json"])


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

    def test_NT_3_깨진_마크다운_노트_12종은_각각_파일_경로와_함께_실패한다(self):
        """NT-3 깨진 마크다운 노트 12종은 각각 파일 경로와 함께 실패한다"""
        cases = broken_markdown_cases()
        self.assertEqual(len(cases), 12)
        for name, (rel, text) in cases.items():
            with self.subTest(name):
                root = self.valid_repo()
                self.write(root, "notes/g/fine.md", note_md("괜찮음"))
                self.write(root, rel, text)
                for command in ("check", "index"):
                    result = self.tool(command, "--root", str(root))
                    self.assertNotEqual(result.returncode, 0, f"{command}: {result.stdout}")
                    lines = result.stderr.splitlines()
                    self.assertTrue(lines)
                    self.assertTrue(all(line.startswith(f"{rel}: ") for line in lines), result.stderr)
                    self.assertFalse((root / "notes" / "index.json").exists())
                shutil.rmtree(root)

    def test_NT_3_마크다운과_JSON_을_한_목록으로_만든다(self):
        """NT-3 마크다운과 JSON 을 한 목록으로 만든다"""
        root = self.valid_repo()
        files = {
            "notes/g/quoted.md": note_md('"Flow: 차가운 스트림"', "2026.06.12", "# 본문 제목\n\n문단\n").encode("utf-8"),
            "notes/g/crlf.md": note_md("줄 끝", "2026.07.01").replace("\n", "\r\n").encode("utf-8"),
            "notes/한글/bom.md": b"\xef\xbb\xbf" + note_md("'작은따옴표'", "2026.01.05").encode("utf-8"),
            "notes/한글/empty.md": note_md("빈 본문", "2026.01.05", "").encode("utf-8"),
        }
        for rel, data in files.items():
            self.write(root, rel, data)
        result = self.tool("index", "--root", str(root))
        self.assertEqual(result.returncode, 0, result.stderr)
        index = json.loads((root / "notes" / "index.json").read_bytes())
        self.assertEqual(list(index), ["g", "한글"])
        self.assertEqual(
            [(e["id"], e["title"], e["date"], e["path"]) for e in index["g"]],
            [
                ("crlf", "줄 끝", "2026.07.01", "notes/g/crlf.md"),
                ("ok", "ok", "2026.06.12", "notes/g/ok.json"),
                ("quoted", "Flow: 차가운 스트림", "2026.06.12", "notes/g/quoted.md"),
            ],
        )
        self.assertEqual([(e["id"], e["title"]) for e in index["한글"]], [("bom", "작은따옴표"), ("empty", "빈 본문")])
        entries = {e["path"]: e for group in index.values() for e in group}
        for rel, data in files.items():
            with self.subTest(rel):
                self.assertEqual(entries[rel]["hash"], hashlib.sha256(data).hexdigest())

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
        self.write(root, "notes/g/third.md", note_md("셋째", "2026.07.02"))
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
        self.write(root, "docs/my-notes/kotlin/state.md", note_md("상태", "2026.07.01"))
        result = self.tool("index", "--root", str(root), "--notes-dir", "docs/my-notes")
        self.assertEqual(result.returncode, 0, result.stderr)
        index = json.loads((root / "docs/my-notes/index.json").read_text(encoding="utf-8"))
        self.assertEqual(
            [e["path"] for e in index["kotlin"]],
            ["docs/my-notes/kotlin/state.md", "docs/my-notes/kotlin/flow.json"],
        )
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
        self.write(root, "notes/h/third.md", note_md("셋째", "2026.07.02"))
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
