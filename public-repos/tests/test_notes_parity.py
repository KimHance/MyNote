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


SYNTHETIC = {
    "kotlin/flow.json": note("flow", "2026.06.12"),
    "kotlin/alpha.json": note("alpha", "2026.06.12"),
    "kotlin/later.json": note("later", "2026.07.01"),
    "kotlin/short-date.json": note("short-date", "2026.6.2"),
    "android/한글-노트.json": note("한글-노트", "2026.01.05", title="한글 제목 🙂"),
    "android/empty.json": note("empty", "2026.01.05", blocks=[]),
    "가나다/crlf.json": note("crlf", "2025.12.31", newline="\r\n"),
    "zeta/only.json": note("only", "2026.03.03"),
}


class ParityTest(support.TempCase):
    def run_py(self, *args, cwd):
        return subprocess.run(
            [sys.executable, "-I", *args],
            capture_output=True,
            text=True,
            cwd=str(cwd),
            env=self.env,
        )

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

    def test_NT_3_합성_노트_집합에서_gen_index_py_와_출력이_같다(self):
        """NT-3 합성 노트 집합에서 gen_index.py 와 출력이 같다"""
        old, new = self.tmp / "old", self.tmp / "new"
        (old / "notes").mkdir(parents=True)
        shutil.copy(support.GEN_INDEX, old / "notes" / "gen_index.py")
        for rel, text in SYNTHETIC.items():
            for base in (old, new):
                path = base / "notes" / rel
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(text.encode("utf-8"))
        generated = self.run_py("notes/gen_index.py", cwd=old)
        self.assertEqual(generated.returncode, 0, generated.stderr)
        produced = self.run_py(str(support.KIT_TOOL), "index", "--root", str(new), cwd=self.tmp)
        self.assertEqual(produced.returncode, 0, produced.stderr)
        expected = (old / "notes" / "index.json").read_bytes()
        self.assertEqual((new / "notes" / "index.json").read_bytes(), expected)
        index = json.loads(expected)
        self.assertEqual(len(index), 4)
        self.assertEqual([e["id"] for e in index["kotlin"]], ["short-date", "later", "alpha", "flow"])
        crlf = (new / "notes" / "가나다" / "crlf.json").read_bytes()
        self.assertIn(b"\r\n", crlf)
        self.assertEqual(index["가나다"][0]["hash"], hashlib.sha256(crlf).hexdigest())


if __name__ == "__main__":
    unittest.main()
