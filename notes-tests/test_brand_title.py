import functools
import os
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = ("app", "core", "feature")
SKIPPED_DIRS = {"build", ".gradle", ".kotlin"}

APP_TITLE = "MyNote"
TEMPORARY_NAME = "공부 기록"
WORDMARK_SPELLING = re.compile(r"mynote")
# 홈 상단 바와 읽는 중 화면 — 모듈을 옮겨도 따라가게 파일 이름으로 찾는다(feature-modules FM-12)
TITLE_USERS = ("NotesShell.kt", "ConnectReading.kt")
OTHER_TITLES = {
    "notes_settings": "설정",
    "notes_nav_bookmark": "북마크",
    "notes_nav_stats": "통계",
    "notes_stats_history_title": "지난 요약",
}


@functools.cache
def strings_of(path):
    root = ET.parse(path).getroot()
    return {node.get("name"): "".join(node.itertext()) for node in root.iter("string")}


def is_main_strings(path):
    return path.name == "strings.xml" and path.relative_to(REPO_ROOT).parts[-5:-1] == ("src", "main", "res", "values")


def defined_strings(name):
    """모든 모듈의 `strings.xml` 에서 이름으로 찾은 값. 정의는 정확히 하나여야 한다."""
    found = [strings[name] for strings in map(strings_of, filter(is_main_strings, app_sources())) if name in strings]
    if len(found) != 1:
        raise AssertionError(f"{name}: 정의 {len(found)}개")
    return found[0]


def main_source(file_name):
    """모든 모듈의 `src/main` 에서 파일 이름으로 찾은 소스 하나."""
    found = [path for path in app_sources() if path.name == file_name and "main" in path.relative_to(REPO_ROOT).parts]
    if len(found) != 1:
        raise AssertionError(f"{file_name}: {len(found)}개")
    return found[0]


@functools.cache
def app_sources():
    """모든 모듈의 Kotlin 소스와 리소스 XML — 빌드 폴더는 내려가지 않고 걷는다. 한 번 만들어 같이 쓴다."""
    found = []
    for top in SOURCE_ROOTS:
        for directory, children, files in os.walk(REPO_ROOT / top):
            children[:] = sorted(child for child in children if child not in SKIPPED_DIRS)
            rel = Path(directory).relative_to(REPO_ROOT)
            if "src" not in rel.parts:
                continue
            for name in sorted(files):
                if name.endswith(".kt") or (name.endswith(".xml") and "res" in rel.parts):
                    found.append(Path(directory) / name)
    return tuple(found)


class BrandTitleTest(unittest.TestCase):
    def test_BR_7_홈과_읽는_중_화면_제목은_MyNote_다(self):
        self.assertEqual(defined_strings("notes_app_title"), APP_TITLE)
        for file_name in TITLE_USERS:
            with self.subTest(file_name):
                self.assertIn("R.string.notes_app_title", main_source(file_name).read_text(encoding="utf-8"))

    def test_BR_7_앱_소스에_임시_이름이_없다(self):
        sources = list(app_sources())
        self.assertGreaterEqual(len([path for path in sources if is_main_strings(path)]), 2)
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
        for name, value in OTHER_TITLES.items():
            with self.subTest(name):
                self.assertEqual(defined_strings(name), value)
        self.assertEqual(defined_strings("app_name"), APP_TITLE)


if __name__ == "__main__":
    unittest.main()
