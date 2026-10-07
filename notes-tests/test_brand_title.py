import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = ("app", "core", "feature")
SKIPPED_DIRS = {"build", ".gradle", ".kotlin"}
FEATURE_STRINGS = REPO_ROOT / "feature" / "notes" / "src" / "main" / "res" / "values" / "strings.xml"
APP_STRINGS = REPO_ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml"
FEATURE_SOURCES = REPO_ROOT / "feature" / "notes" / "src" / "main" / "java" / "com" / "hancekim" / "mynote" / "feature" / "notes"

APP_TITLE = "MyNote"
TEMPORARY_NAME = "공부 기록"
WORDMARK_SPELLING = re.compile(r"mynote")
TITLE_USERS = ("shell/NotesScenes.kt", "screen/connect/ConnectReading.kt")
OTHER_TITLES = {
    "notes_settings": "설정",
    "notes_nav_bookmark": "북마크",
    "notes_nav_stats": "통계",
    "notes_stats_history_title": "지난 요약",
}


def strings_of(path):
    root = ET.parse(path).getroot()
    return {node.get("name"): "".join(node.itertext()) for node in root.iter("string")}


def app_sources():
    for top in SOURCE_ROOTS:
        for path in (REPO_ROOT / top).rglob("*"):
            rel = path.relative_to(REPO_ROOT)
            if not path.is_file() or SKIPPED_DIRS.intersection(rel.parts) or "src" not in rel.parts:
                continue
            if path.suffix == ".kt" or (path.suffix == ".xml" and "res" in rel.parts):
                yield path


class BrandTitleTest(unittest.TestCase):
    def test_BR_7_홈과_읽는_중_화면_제목은_MyNote_다(self):
        self.assertEqual(strings_of(FEATURE_STRINGS)["notes_app_title"], APP_TITLE)
        for rel in TITLE_USERS:
            with self.subTest(rel):
                self.assertIn("R.string.notes_app_title", (FEATURE_SOURCES / rel).read_text(encoding="utf-8"))

    def test_BR_7_앱_소스에_임시_이름이_없다(self):
        sources = list(app_sources())
        self.assertIn(FEATURE_STRINGS, sources)
        found = [
            str(path.relative_to(REPO_ROOT))
            for path in sources
            if TEMPORARY_NAME in path.read_text(encoding="utf-8")
        ]
        self.assertEqual(found, [])

    def test_BR_7_화면_문구에_소문자_mynote_가_없다(self):
        found = []
        for path in app_sources():
            if path.name != "strings.xml":
                continue
            for name, value in strings_of(path).items():
                if WORDMARK_SPELLING.search(value):
                    found.append(f"{path.relative_to(REPO_ROOT)}:{name}")
        self.assertEqual(found, [])

    def test_BR_7_다른_화면_제목과_런처_이름은_그대로다(self):
        strings = strings_of(FEATURE_STRINGS)
        for name, value in OTHER_TITLES.items():
            with self.subTest(name):
                self.assertEqual(strings[name], value)
        self.assertEqual(strings_of(APP_STRINGS)["app_name"], APP_TITLE)


if __name__ == "__main__":
    unittest.main()
