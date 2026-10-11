import hashlib
import json
import shutil
import subprocess
import sys
import unittest

import support

KIT_TOOL = support.load_module("kit_tool_for_parity", support.KIT_TOOL)
GEN_INDEX = support.load_module("gen_index_for_parity", support.GEN_INDEX)


def note(note_id, date, title=None, blocks=None, newline="\n"):
    body = json.dumps(
        {
            "id": note_id,
            "title": title or f"제목 {note_id}",
            "date": date,
            "blocks": [{"type": "divider"}] if blocks is None else blocks,
        },
        ensure_ascii=False,
        indent=2,
    )
    return (body + "\n").replace("\n", newline)


def markdown(title, date, body="## 본문\n\n문단\n", newline="\n", bom=False):
    text = f"---\ntitle: {title}\ndate: {date}\n---\n{body}".replace("\n", newline)
    return ("﻿" if bom else "") + text


SYNTHETIC = {
    "kotlin/flow.json": note("flow", "2026.06.12"),
    "kotlin/alpha.json": note("alpha", "2026.06.12"),
    "kotlin/later.json": note("later", "2026.07.01"),
    "kotlin/short-date.json": note("short-date", "2026.6.2"),
    "kotlin/beta.md": markdown("베타", "2026.06.12"),
    "kotlin/newest.md": markdown('"Flow: 따옴표 제목"', "2026.08.01"),
    "android/한글-노트.json": note("한글-노트", "2026.01.05", title="한글 제목 🙂"),
    "android/empty.json": note("empty", "2026.01.05", blocks=[]),
    "android/마크다운-노트.md": markdown("한글 파일 이름", "2026.01.05"),
    "android/no-body.md": markdown("본문 없음", "2026.01.04", body=""),
    "가나다/crlf.json": note("crlf", "2025.12.31", newline="\r\n"),
    "가나다/crlf-md.md": markdown("줄 끝 CRLF", "2025.12.31", newline="\r\n"),
    "가나다/bom-md.md": markdown("'BOM 노트'", "2025.12.30", bom=True),
    "zeta/only.json": note("only", "2026.03.03"),
}

FAILURE_BASE = {
    "g/ok.json": note("ok", "2026.06.12"),
    "g/fine.md": markdown("괜찮음", "2026.06.12"),
}
GOOD_MARKDOWN = "---\ntitle: a\ndate: 2026.10.07\n---\n본문\n"
PARA = [{"type": "paragraph", "spans": [{"text": "x"}]}]
# 두 생성기가 함께 거부하는 깨진 노트 — 마크다운 12종 + JSON 7종(NT-3 ⑦-2)
BROKEN = {
    "앞머리 없음": ("g/bad.md", "# 제목\n본문\n"),
    "닫히지 않음": ("g/bad.md", "---\ntitle: a\ndate: 2026.10.07\n본문\n"),
    "title 없음": ("g/bad.md", "---\ndate: 2026.10.07\n---\n"),
    "date 없음": ("g/bad.md", "---\ntitle: a\n---\n"),
    "date 모양 틀림": ("g/bad.md", "---\ntitle: a\ndate: 2026-10-07\n---\n"),
    "모르는 키": ("g/bad.md", "---\ntitle: a\ndate: 2026.10.07\ntags: x\n---\n"),
    "빈 값": ("g/bad.md", '---\ntitle: ""\ndate: 2026.10.07\n---\n'),
    "같은 키 두 번": ("g/bad.md", "---\ntitle: a\ntitle: b\ndate: 2026.10.07\n---\n"),
    "키: 값 이 아닌 줄": ("g/bad.md", "---\ntitle a\ntitle: a\ndate: 2026.10.07\n---\n"),
    "UTF-8 아님": ("g/bad.md", "---\ntitle: 가\ndate: 2026.10.07\n---\n".encode("cp949")),
    "확장자 .MD": ("g/bad.MD", GOOD_MARKDOWN),
    "같은 id 의 .md 와 .json": ("h/ok.md", GOOD_MARKDOWN),
    "모르는 블록 종류": ("g/bad.json", note("bad", "2026.01.01", blocks=[{"type": "video"}])),
    "모르는 style": (
        "g/bad.json",
        note("bad", "2026.01.01", blocks=[{"type": "paragraph", "spans": [{"text": "x", "style": "italic"}]}]),
    ),
    "level 4": ("g/bad.json", note("bad", "2026.01.01", blocks=[{"type": "heading", "level": 4, "spans": [{"text": "x"}]}])),
    "spans 비어 있음": ("g/bad.json", note("bad", "2026.01.01", blocks=[{"type": "paragraph", "spans": []}])),
    "JSON 깨짐": ("g/bad.json", '{"id": '),
    "id 중복": ("h/bad.json", note("ok", "2026.01.01", blocks=PARA)),
    "필수 키 없음": ("g/bad.json", json.dumps({"id": "bad", "date": "2026.01.01", "blocks": PARA})),
}


def error_paths(lines, known):
    """오류 줄이 가리키는 노트 경로 — 줄이 알려진 경로 + `:` 또는 공백으로 시작하면 그 경로."""
    found = set()
    for line in lines:
        line = line.removeprefix("  - ")
        found |= {rel for rel in known if line.startswith((f"{rel}:", f"{rel} "))}
    return found


class ParityTest(support.TempCase):
    def run_py(self, *args, cwd):
        return subprocess.run(
            [sys.executable, "-I", *args],
            capture_output=True,
            text=True,
            cwd=str(cwd),
            env=self.env,
        )

    def tree(self, name, files, with_generator=False):
        root = self.tmp / name
        (root / "notes").mkdir(parents=True)
        if with_generator:
            shutil.copy(support.GEN_INDEX, root / "notes" / "gen_index.py")
        for rel, content in files.items():
            path = root / "notes" / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content if isinstance(content, bytes) else content.encode("utf-8"))
        return root

    def generate(self, root):
        return self.run_py("notes/gen_index.py", cwd=root)

    def tool_index(self, root):
        return self.run_py(str(support.KIT_TOOL), "index", "--root", str(root), cwd=self.tmp)

    def test_NT_1_규격의_닫힌_집합이_gen_index_py_와_같다(self):
        """NT-1 규격의 닫힌 집합이 gen_index.py 와 같다"""
        text = support.KIT_SPEC.read_text(encoding="utf-8")
        blocks = set(support.table_firsts(text, "### 블록 종류"))
        styles = set(support.table_firsts(text, "### 인라인 스타일"))
        levels = {int(v) for v in support.table_firsts(text, "### 헤딩 레벨")}
        self.assertEqual(blocks, set(GEN_INDEX.BLOCK_TYPES))
        self.assertEqual(styles, set(GEN_INDEX.SPAN_STYLES))
        self.assertEqual(levels, set(GEN_INDEX.HEADING_LEVELS))
        self.assertEqual(set(KIT_TOOL.BLOCK_TYPES), set(GEN_INDEX.BLOCK_TYPES))
        self.assertEqual(set(KIT_TOOL.SPAN_STYLES), set(GEN_INDEX.SPAN_STYLES))
        self.assertEqual(set(KIT_TOOL.HEADING_LEVELS), set(GEN_INDEX.HEADING_LEVELS))
        self.assertEqual(
            set(KIT_TOOL.ENVELOPE_KEYS | KIT_TOOL.OPTIONAL_KEYS),
            set(GEN_INDEX.ENVELOPE_KEYS) | {"schema"},
        )

    def test_NT_1_규격의_앞머리_규칙이_gen_index_py_와_같다(self):
        """NT-1 규격의 앞머리 규칙이 gen_index.py 와 같다"""
        text = support.KIT_SPEC.read_text(encoding="utf-8")
        self.assertEqual(tuple(support.table_firsts(text, "### 앞머리 키")), GEN_INDEX.FRONT_MATTER_KEYS)
        self.assertIn("| `date` | `YYYY.MM.DD` |", text)
        self.assertEqual(KIT_TOOL.DATE_FORMAT.pattern, GEN_INDEX.DATE_FORMAT.pattern)
        self.assertEqual(KIT_TOOL.FRONT_MATTER_KEYS, GEN_INDEX.FRONT_MATTER_KEYS)
        self.assertEqual(KIT_TOOL.FRONT_MATTER_KEY.pattern, GEN_INDEX.FRONT_MATTER_KEY.pattern)
        self.assertEqual(KIT_TOOL.FRONT_MATTER_FENCE, GEN_INDEX.FRONT_MATTER_FENCE)
        self.assertEqual(set(KIT_TOOL.NOTE_SUFFIXES), set(GEN_INDEX.NOTE_SUFFIXES))

    def test_NT_3_합성_노트_집합에서_gen_index_py_와_출력이_같다(self):
        """NT-3 합성 노트 집합에서 gen_index.py 와 출력이 같다"""
        old = self.tree("old", SYNTHETIC, with_generator=True)
        new = self.tree("new", SYNTHETIC)
        generated = self.generate(old)
        self.assertEqual(generated.returncode, 0, generated.stderr)
        produced = self.tool_index(new)
        self.assertEqual(produced.returncode, 0, produced.stderr)
        expected = (old / "notes" / "index.json").read_bytes()
        self.assertEqual((new / "notes" / "index.json").read_bytes(), expected)
        index = json.loads(expected)
        self.assertEqual(len(index), 4)
        self.assertEqual(
            [e["id"] for e in index["kotlin"]],
            ["short-date", "newest", "later", "alpha", "beta", "flow"],
        )
        self.assertEqual(index["kotlin"][1]["title"], "Flow: 따옴표 제목")
        self.assertEqual({e["path"].rsplit(".", 1)[1] for e in index["android"]}, {"json", "md"})
        crlf = (new / "notes" / "가나다" / "crlf-md.md").read_bytes()
        self.assertIn(b"\r\n", crlf)
        entries = {e["id"]: e for e in index["가나다"]}
        self.assertEqual(entries["crlf-md"]["hash"], hashlib.sha256(crlf).hexdigest())
        self.assertEqual(entries["bom-md"]["title"], "BOM 노트")

    def test_NT_3_깨진_노트에서_gen_index_py_와_함께_실패한다(self):
        """NT-3 깨진 노트에서 gen_index.py 와 함께 실패한다"""
        self.assertEqual(len(BROKEN), 19)
        for number, (name, (rel, content)) in enumerate(BROKEN.items()):
            with self.subTest(name):
                files = {**FAILURE_BASE, rel: content}
                old = self.tree(f"old-{number}", files, with_generator=True)
                new = self.tree(f"new-{number}", files)
                known = {f"notes/{r}" for r in files}

                generated = self.generate(old)
                produced = self.tool_index(new)

                self.assertNotEqual(generated.returncode, 0, generated.stdout)
                self.assertNotEqual(produced.returncode, 0, produced.stdout)
                self.assertFalse((old / "notes" / "index.json").exists())
                self.assertFalse((new / "notes" / "index.json").exists())
                expected = error_paths(generated.stderr.splitlines(), known)
                self.assertEqual(expected, {f"notes/{rel}"})
                self.assertEqual(error_paths(produced.stderr.splitlines(), known), expected)

    def test_NT_3_레포_노트에서_커밋된_index_json_과_바이트가_같다(self):
        """NT-3 레포 노트에서 커밋된 index.json 과 바이트가 같다"""
        committed = (support.REPO_ROOT / "notes" / "index.json").read_bytes()
        copy = self.tmp / "repo-copy"
        shutil.copytree(
            support.REPO_ROOT / "notes",
            copy / "notes",
            ignore=shutil.ignore_patterns("index.json", "__pycache__"),
        )
        self.assertFalse((copy / "notes" / "index.json").exists())
        self.assertFalse((copy / ".mynote-format").exists())

        produced = self.tool_index(copy)

        self.assertEqual(produced.returncode, 0, produced.stderr)
        self.assertEqual((copy / "notes" / "index.json").read_bytes(), committed)
        paths = [e["path"] for group in json.loads(committed).values() for e in group]
        self.assertTrue(any(path.endswith(".md") for path in paths))

    def test_NT_3_앞머리_경계_묶음에서_판정이_같다(self):
        """NT-3 앞머리 경계 묶음에서 판정이 같다"""
        bundle = support.FRONT_MATTER_BUNDLE
        cases = json.loads((bundle / "cases.json").read_text(encoding="utf-8"))
        self.assertEqual({c["file"] for c in cases}, {p.name for p in bundle.glob("*.md")})
        self.assertTrue(any(c["frontMatter"] for c in cases))
        self.assertTrue(any(not c["frontMatter"] for c in cases))
        boundary = ("앞머리가 없습니다", "앞머리가 닫히지 않았습니다")

        for number, case in enumerate(cases):
            with self.subTest(case["file"]):
                raw = (bundle / case["file"]).read_bytes()
                files = {f"g/{case['file']}": raw}
                old = self.tree(f"old-{number}", files, with_generator=True)
                new = self.tree(f"new-{number}", files)
                rel = f"notes/g/{case['file']}"

                generated = self.generate(old)
                produced = self.tool_index(new)

                said = [line.removeprefix(f"{rel}: ") for line in produced.stderr.splitlines()]
                if case["frontMatter"]:
                    self.assertFalse([line for line in said if line.startswith(boundary)], produced.stderr)
                else:
                    text = KIT_TOOL.normalize_text(raw)
                    reason = boundary[1] if text.split("\n", 1)[0] == "---" else boundary[0]
                    self.assertNotEqual(produced.returncode, 0)
                    self.assertTrue(any(line.startswith(reason) for line in said), produced.stderr)
                self.assertEqual(produced.returncode == 0, generated.returncode == 0, produced.stderr + generated.stderr)


if __name__ == "__main__":
    unittest.main()
