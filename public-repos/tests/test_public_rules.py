import re
import shutil
import subprocess
import sys
import unittest

import support

PRIVATE_PATTERNS = {
    "소유자 이름": re.compile(re.escape(support.OWNER)),
    "소유자/저장소 좌표": re.compile(re.escape(f"{support.OWNER}/{support.REPO_NAME}"), re.IGNORECASE),
    "로컬 절대 경로": re.compile(r"/Users/"),
    "토큰(fine-grained)": re.compile(r"github_pat_[A-Za-z0-9_]{20,}"),
    "토큰(classic)": re.compile(r"ghp_[A-Za-z0-9]{20,}"),
    "이메일": re.compile(r"[\w.+-]+@[\w-]+\.[\w.-]+"),
}
OUTSIDE_PATH = re.compile(r"\.\./")
PLACEHOLDER = re.compile(r"<[A-Z][A-Z0-9_]*>")
TABLE_PLACEHOLDER = re.compile(r"^\|\s*`(<[A-Z][A-Z0-9_]*>)`\s*\|", re.MULTILINE)


def hits(pattern):
    found = []
    for rel, text in support.public_files():
        for number, line in enumerate(text.splitlines(), 1):
            if pattern.search(line):
                found.append(f"{rel}:{number}")
    return found


class PublicRulesTest(support.TempCase):
    def test_NT_7_공개_폴더에_비공개_정보가_없다(self):
        """NT-7 공개 폴더에 비공개 정보가 없다"""
        scanned = [rel for rel, _ in support.public_files()]
        self.assertIn("notes-kit/tools/note_tool.py", scanned)
        self.assertTrue(any(r.startswith("notes-kit/tests/test_") for r in scanned))
        for label, pattern in PRIVATE_PATTERNS.items():
            with self.subTest(label):
                self.assertEqual(hits(pattern), [])

    def test_NT_7_폴더_밖을_가리키지_않는다(self):
        """NT-7 폴더 밖을 가리키지 않는다"""
        self.assertEqual(hits(OUTSIDE_PATH), [])

    def test_NT_7_자리표시자가_표와_같다(self):
        """NT-7 자리표시자가 표와 같다"""
        table_text = support.PLACEHOLDERS.read_text(encoding="utf-8")
        table = set(TABLE_PLACEHOLDER.findall(table_text))
        self.assertTrue(table)
        used = set()
        for rel, text in support.public_files():
            if rel != "PLACEHOLDERS.md":
                used |= set(PLACEHOLDER.findall(text))
        self.assertEqual(used, table)
        self.assertEqual(set(PLACEHOLDER.findall(table_text)) - table, set())

    def test_NT_7_notes_kit_을_떼어_내도_시험이_돈다(self):
        """NT-7 notes-kit 을 떼어 내도 시험이 돈다"""
        copy = self.tmp / "detached" / "kit"
        shutil.copytree(support.KIT, copy, ignore=shutil.ignore_patterns("__pycache__"))
        outside = self.tmp / "elsewhere"
        outside.mkdir()
        result = subprocess.run(
            [sys.executable, "-I", "-m", "unittest", "discover", str(copy / "tests")],
            capture_output=True,
            text=True,
            cwd=str(outside),
            env=self.env,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertRegex(result.stderr, r"Ran \d+ tests")


if __name__ == "__main__":
    unittest.main()
