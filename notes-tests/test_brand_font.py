import re
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
DESIGN_DOC = REPO_ROOT / "docs" / "design" / "brand.md"
TYPOGRAPHY_KT = (
    REPO_ROOT / "core" / "design-foundation" / "src" / "main" / "java" / "com" / "hancekim" / "mynote"
    / "core" / "designfoundation" / "token" / "Typography.kt"
)
SOURCE_ROOTS = ("app", "core", "feature")
FONT_SUFFIXES = {".ttf", ".otf", ".ttc"}


def first_table_names(markdown, heading):
    """`heading` 절의 첫 표에서 첫 칸의 백틱 이름들."""
    section = markdown.split(f"\n{heading}\n", 1)[1].split("\n## ", 1)[0]
    rows = []
    for line in section.splitlines():
        if line.startswith("|"):
            rows.append(line)
        elif rows:
            break
    names = []
    for row in rows[2:]:
        first = row.strip("|").split("|")[0]
        names += re.findall(r"`([^`]+)`", first)
    return names


def code_names():
    text = TYPOGRAPHY_KT.read_text(encoding="utf-8")
    found = re.search(r"TitleFontStyleNames: Set<String> = setOf\(([^)]*)\)", text)
    return re.findall(r'"([^"]+)"', found.group(1))


def text_style_fields():
    text = TYPOGRAPHY_KT.read_text(encoding="utf-8")
    holder = re.search(r"data class MyNoteTextStyles\((.*?)\n\)", text, re.DOTALL).group(1)
    return re.findall(r"val (\w+): TextStyle", holder)


class BrandFontTest(unittest.TestCase):
    def test_BR_8_디자인_문서_제목_글꼴_표와_코드_목록이_같다(self):
        """BR-8 디자인 문서 제목 글꼴 표와 코드 목록이 같다"""
        documented = first_table_names(DESIGN_DOC.read_text(encoding="utf-8"), "## 제목 글꼴")
        coded = code_names()
        self.assertTrue(documented)
        self.assertEqual(len(documented), len(set(documented)))
        self.assertEqual(len(coded), len(set(coded)))
        self.assertEqual(sorted(documented), sorted(coded))
        self.assertLessEqual(set(coded), set(text_style_fields()))

    def test_BR_8_앱_소스에_글꼴_파일이_없다(self):
        """BR-8 앱 소스에 글꼴 파일이 없다"""
        for root in SOURCE_ROOTS:
            for path in sorted((REPO_ROOT / root).rglob("*")):
                relative = path.relative_to(REPO_ROOT)
                if "build" in relative.parts or "src" not in relative.parts:
                    continue
                with self.subTest(path=relative.as_posix()):
                    self.assertNotIn(path.suffix.lower(), FONT_SUFFIXES)
                    self.assertFalse(path.is_dir() and path.name.split("-")[0] == "font" and path.parent.name == "res")


if __name__ == "__main__":
    unittest.main()
