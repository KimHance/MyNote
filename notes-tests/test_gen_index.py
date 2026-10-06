import ast
import hashlib
import json
import sys
import unittest

import support
from support import front_matter

GEN_INDEX = support.load_module("gen_index_under_test", support.GEN_INDEX)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def index_bytes(index):
    return (json.dumps(index, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def json_note(note_id, date, title=None):
    note = {
        "id": note_id,
        "title": title or f"제목 {note_id}",
        "date": date,
        "blocks": [{"type": "paragraph", "spans": [{"text": "본문"}]}],
    }
    return json.dumps(note, ensure_ascii=False, indent=2) + "\n"


class FrontMatterTest(support.TempCase):
    def test_NF_1_앞머리로_목록_항목을_만든다(self):
        """NF-1 앞머리로 목록 항목을 만든다"""
        flow = front_matter(title='"Flow: 차가운 스트림"', body="# 본문 제목\n\n문단\n")
        quoted = "---\ntitle:   '작은따옴표'  \ndate: 2026.06.11\n---\n"
        root = self.notes_tree("tree", {"kotlin/flow.md": flow, "kotlin/quoted.md": quoted})

        result = self.generate(root)

        self.assertEqual(result.returncode, 0, result.stderr)
        expected = {
            "kotlin": [
                {
                    "id": "flow",
                    "title": "Flow: 차가운 스트림",
                    "date": "2026.06.12",
                    "path": "notes/kotlin/flow.md",
                    "hash": sha(flow.encode("utf-8")),
                },
                {
                    "id": "quoted",
                    "title": "작은따옴표",
                    "date": "2026.06.11",
                    "path": "notes/kotlin/quoted.md",
                    "hash": sha(quoted.encode("utf-8")),
                },
            ]
        }
        self.assertEqual((root / "notes" / "index.json").read_bytes(), index_bytes(expected))

    def test_NF_1_앞머리_오류_9종은_각각_파일_경로와_실패한다(self):
        """NF-1 앞머리 오류 9종은 각각 파일 경로와 실패한다"""
        cases = {
            "앞머리 없음": ("# 제목\n본문\n", "앞머리 없음"),
            "닫히지 않음": ("---\ntitle: a\ndate: 2026.10.07\n본문\n", "앞머리가 닫히지 않음"),
            "title 없음": ("---\ndate: 2026.10.07\n---\n", "앞머리에 'title' 없음"),
            "date 없음": ("---\ntitle: a\n---\n", "앞머리에 'date' 없음"),
            "date 모양": ("---\ntitle: a\ndate: 2026-10-07\n---\n", "'date' 는 YYYY.MM.DD 여야 함"),
            "모르는 키": ("---\ntitle: a\ndate: 2026.10.07\ntags: x\n---\n", "모르는 키 'tags'"),
            "빈 값": ('---\ntitle: ""\ndate: 2026.10.07\n---\n', "'title' 값이 비어 있음"),
            "같은 키 두 번": ("---\ntitle: a\ntitle: b\ndate: 2026.10.07\n---\n", "키 'title' 가 두 번 나옴"),
            "빈 줄": ("---\ntitle: a\n\ndate: 2026.10.07\n---\n", "'키: 값' 이 아님"),
            "# 줄": ("---\n# 메모\ntitle: a\ndate: 2026.10.07\n---\n", "'키: 값' 이 아님"),
            "콜론 없는 줄": ("---\ntitle a\ntitle: a\ndate: 2026.10.07\n---\n", "'키: 값' 이 아님"),
        }
        for name, (content, reason) in cases.items():
            with self.subTest(name):
                root = self.notes_tree(
                    f"tree-{len(list(self.tmp.iterdir()))}",
                    {"kotlin/ok.md": front_matter(), "kotlin/bad.md": content},
                    index=support.PRIOR_INDEX,
                )

                result = self.generate(root)

                self.assertNotEqual(result.returncode, 0)
                errors = [line for line in result.stderr.splitlines() if line.startswith("  - ")]
                self.assertEqual(len(errors), 1, result.stderr)
                self.assertIn("notes/kotlin/bad.md", errors[0])
                self.assertIn(reason, errors[0])
                self.assertEqual((root / "notes" / "index.json").read_bytes(), support.PRIOR_INDEX)

    def test_NF_1_CRLF_와_BOM_도_같은_항목이다(self):
        """NF-1 CRLF 와 BOM 도 같은 항목이다"""
        lf = front_matter(title="줄 끝", body="# 본문\n\n문단\n")
        variants = {
            "lf": lf.encode("utf-8"),
            "crlf": lf.replace("\n", "\r\n").encode("utf-8"),
            "cr": lf.replace("\n", "\r").encode("utf-8"),
            "bom": b"\xef\xbb\xbf" + lf.encode("utf-8"),
            "bom-crlf": b"\xef\xbb\xbf" + lf.replace("\n", "\r\n").encode("utf-8"),
        }
        root = self.notes_tree("tree", {f"g/{name}.md": data for name, data in variants.items()})

        result = self.generate(root)

        self.assertEqual(result.returncode, 0, result.stderr)
        entries = {e["id"]: e for e in json.loads((root / "notes" / "index.json").read_bytes())["g"]}
        self.assertEqual(set(entries), set(variants))
        for name, data in variants.items():
            with self.subTest(name):
                entry = entries[name]
                self.assertEqual((entry["title"], entry["date"]), ("줄 끝", "2026.06.12"))
                self.assertEqual(entry["path"], f"notes/g/{name}.md")
                self.assertEqual(entry["hash"], sha(data))
        self.assertEqual(len({e["hash"] for e in entries.values()}), len(variants))

    def test_NF_1_앞머리_경계_묶음(self):
        """NF-1 앞머리 경계 묶음"""
        bundle = support.FRONT_MATTER_BUNDLE
        cases = json.loads((bundle / "cases.json").read_text(encoding="utf-8"))
        self.assertEqual({c["file"] for c in cases}, {p.name for p in bundle.glob("*.md")})
        self.assertTrue(any(c["frontMatter"] for c in cases))
        self.assertTrue(any(not c["frontMatter"] for c in cases))

        for case in cases:
            with self.subTest(case["file"]):
                raw = (bundle / case["file"]).read_bytes()
                text = GEN_INDEX.normalize_text(raw)
                head, body = GEN_INDEX.split_front_matter(text)
                self.assertEqual(head is not None, case["frontMatter"])
                self.assertEqual(body, case["body"])

                root = self.notes_tree(case["file"], {f"g/{case['file']}": raw})
                result = self.generate(root)
                rel = f"notes/g/{case['file']}"
                if case["frontMatter"]:
                    self.assertNotIn(f"{rel}: 앞머리 없음", result.stderr)
                    self.assertNotIn(f"{rel}: 앞머리가 닫히지 않음", result.stderr)
                else:
                    reason = "앞머리가 닫히지 않음" if text.split("\n", 1)[0] == "---" else "앞머리 없음"
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn(f"{rel}: {reason}", result.stderr)


class GeneratorTest(support.TempCase):
    def test_NF_13_마크다운과_JSON_을_한_목록으로_만든다(self):
        """NF-13 마크다운과 JSON 을 한 목록으로 만든다"""
        notes = {
            "kotlin/a.json": json_note("a", "2026.06.12"),
            "kotlin/b.md": front_matter(title="비", date="2026.07.01"),
            "kotlin/c.md": front_matter(title="씨", date="2026.06.12"),
            "android/d.md": front_matter(title="디", date="2026.01.05"),
        }
        root = self.notes_tree("tree", notes)

        result = self.generate(root)

        self.assertEqual(result.returncode, 0, result.stderr)

        def entry(rel, note_id, title, date):
            return {
                "id": note_id,
                "title": title,
                "date": date,
                "path": f"notes/{rel}",
                "hash": sha(notes[rel].encode("utf-8")),
            }

        expected = {
            "android": [entry("android/d.md", "d", "디", "2026.01.05")],
            "kotlin": [
                entry("kotlin/b.md", "b", "비", "2026.07.01"),
                entry("kotlin/a.json", "a", "제목 a", "2026.06.12"),
                entry("kotlin/c.md", "c", "씨", "2026.06.12"),
            ],
        }
        self.assertEqual((root / "notes" / "index.json").read_bytes(), index_bytes(expected))

    def test_NF_13_두_형식에_같은_id_면_실패한다(self):
        """NF-13 두 형식에 같은 id 면 실패한다"""
        cases = {
            "json 과 md": {"kotlin/flow.json": json_note("flow", "2026.06.12"), "android/flow.md": front_matter()},
            "json id 와 md 파일 이름": {"kotlin/other.json": json_note("flow", "2026.06.12"), "cs/flow.md": front_matter()},
            "md 둘": {"kotlin/flow.md": front_matter(), "android/flow.md": front_matter()},
        }
        for name, notes in cases.items():
            with self.subTest(name):
                root = self.notes_tree(f"tree-{len(list(self.tmp.iterdir()))}", notes, index=support.PRIOR_INDEX)

                result = self.generate(root)

                self.assertNotEqual(result.returncode, 0)
                self.assertIn("id 'flow' 중복", result.stderr)
                for rel in notes:
                    self.assertIn(f"notes/{rel}", result.stderr)
                self.assertEqual((root / "notes" / "index.json").read_bytes(), support.PRIOR_INDEX)

    def test_NF_13_소문자가_아닌_확장자는_실패한다(self):
        """NF-13 소문자가 아닌 확장자는 실패한다"""
        cases = {
            "x.MD": front_matter(),
            "x.Md": front_matter(),
            "y.JSON": json_note("y", "2026.06.12"),
            "y.Json": json_note("y", "2026.06.12"),
        }
        for name, content in cases.items():
            with self.subTest(name):
                root = self.notes_tree(
                    f"tree-{len(list(self.tmp.iterdir()))}",
                    {"kotlin/ok.md": front_matter(), f"kotlin/{name}": content},
                    index=support.PRIOR_INDEX,
                )

                result = self.generate(root)

                self.assertNotEqual(result.returncode, 0)
                self.assertIn(f"notes/kotlin/{name}: 확장자는 소문자", result.stderr)
                self.assertEqual((root / "notes" / "index.json").read_bytes(), support.PRIOR_INDEX)

    def test_NF_13_표준_라이브러리만_쓴다(self):
        """NF-13 표준 라이브러리만 쓴다"""
        tree = ast.parse(support.GEN_INDEX.read_text(encoding="utf-8"))
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
