import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
DESIGN = REPO_ROOT / "docs" / "design" / "screen-copy.md"
ROWS = REPO_ROOT / "docs" / "design" / "screen-copy-rows.md"
TEMPLATE_README = REPO_ROOT / "public-repos" / "notes-template" / "README.md"
SOURCE_ROOTS = ("app", "core", "feature")
SKIPPED_DIRS = {"build", ".gradle", ".kotlin"}
# 문구를 기능 모듈로 옮겨도(feature-modules FM-11) 검사 밖으로 빠지지 않게 하는 하한 — screen-copy 기준 커밋은 395개
MIN_COPY_COUNT = 390


def strings_files():
    """대상 문구 — 모든 모듈의 `src/main/res/values/strings.xml`(app · core/x · feature/x · 빌드 폴더 제외)."""
    files = []
    for top in SOURCE_ROOTS:
        for path in (REPO_ROOT / top).rglob("strings.xml"):
            rel = path.relative_to(REPO_ROOT)
            if SKIPPED_DIRS.intersection(rel.parts):
                continue
            if rel.parts[-5:-1] == ("src", "main", "res", "values"):
                files.append(path)
    return tuple(sorted(files))


STRINGS = strings_files()
TAG = re.compile(r"<[^>]+>")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
BACKTICK = re.compile(r"`([^`]*)`")
# 문장 자르기 — 디자인 문서 `### 끝말` 의 규칙
SENTENCE_BREAK = re.compile(r"\n|(?<=[.?])\s+")
TRAILING_NON_HANGUL = re.compile(r"[^가-힣?]+$")
# 버튼 · 라벨 · 한 줄 자리 — 길이가 늘면 안 되는 자리(SC-6)
FIXED_PLACES = ("한 줄", "버튼", "라벨", "탭")


def raw_value(text):
    """XML 값의 안드로이드 이스케이프를 풀고(두 글자 `\\n` 은 줄바꿈) 태그를 걷는다. 형식 자리는 그대로다."""
    text = text.replace("\\n", "\n").replace("\\'", "'").replace('\\"', '"')
    return TAG.sub("", text)


def copy_values():
    """대상 문구 전부 — 이름 → 다듬은 값. 배열 항목은 `이름[번호]`."""
    values = {}
    for path in STRINGS:
        root = ET.parse(path).getroot()
        for node in root.iter("string"):
            values[node.get("name")] = raw_value("".join(node.itertext()))
        for node in root.iter("string-array"):
            for index, item in enumerate(node.iter("item")):
                values[f"{node.get('name')}[{index}]"] = raw_value("".join(item.itertext()))
    return values


def cells(line):
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def first_table(path, heading):
    """`heading` 제목 바로 밑의 첫 표 — 머리 칸 이름 → 칸 값 사전의 목록."""
    lines = path.read_text(encoding="utf-8").splitlines()
    start = lines.index(heading)
    rows = []
    header = None
    for line in lines[start + 1 :]:
        if line.startswith("#"):
            break
        if not line.startswith("|"):
            if header:
                break
            continue
        if header is None:
            header = cells(line)
        elif not set(line) <= set("|- "):
            rows.append(dict(zip(header, cells(line))))
    if header is None:
        raise AssertionError(f"{heading} 밑에 표가 없습니다")
    return rows


def ticked(cell):
    return BACKTICK.findall(cell)


def matcher(kind, expression):
    """`꼴` 칸이 글자 · 정규식인 행만 시험이 읽는다. 그 밖은 사람이 읽는 행이다."""
    if kind == "글자":
        return lambda value: expression in value
    if kind == "정규식":
        pattern = re.compile(expression)
        return lambda value: pattern.search(value) is not None
    return None


def sentences(value):
    for piece in SENTENCE_BREAK.split(value):
        piece = TRAILING_NON_HANGUL.sub("", piece.strip())
        if piece:
            yield piece


def changed_rows():
    """바꾼 문구 표의 행 — 리소스 이름 칸이 백틱인 행 전부."""
    rows = []
    for line in ROWS.read_text(encoding="utf-8").splitlines():
        if not line.startswith("| `"):
            continue
        row = cells(line)
        name, place, before, after, length = row[0], row[1], row[2], row[3], row[4]
        rows.append(
            {
                "name": ticked(name)[0],
                "place": place,
                "before": raw_value(ticked(before)[0]),
                "after": raw_value(ticked(after)[0]),
                "length": int(length.replace("−", "-").lstrip("+")),
            }
        )
    return rows


class ScreenCopyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.values = copy_values()

    def test_SC_1_대상_문구를_모든_모듈의_strings_xml_에서_읽는다(self):
        self.assertGreaterEqual(len(self.values), MIN_COPY_COUNT, [str(path.relative_to(REPO_ROOT)) for path in STRINGS])

    def test_SC_1_화면_문구의_끝말은_허용_목록에_있다(self):
        endings = first_table(DESIGN, "### 끝말")
        allow = [matcher(row["꼴"], ticked(row["끝"])[0]) for row in endings if row["판정"] == "허용"]
        block = [matcher(row["꼴"], ticked(row["끝"])[0]) for row in endings if row["판정"] == "막음"]
        exempt = {name for row in first_table(DESIGN, "### 끝말 예외") for name in ticked(row["이름"])}
        self.assertTrue(allow and block and exempt, "디자인 문서 끝말 표를 읽지 못했습니다")
        self.assertEqual(exempt - set(self.values), set(), "끝말 예외에 없는 이름이 있습니다")
        found = []
        for name, value in self.values.items():
            if name in exempt:
                continue
            for sentence in sentences(value):
                if any(rule(sentence) for rule in allow):
                    continue
                if any(rule(sentence) for rule in block):
                    found.append(f"{name}: {sentence}")
        self.assertEqual(found, [])

    def test_SC_2_화면_문구에_쓰지_않는_표현이_없다(self):
        rules = []
        for row in first_table(DESIGN, "### 쓰지 않는 표현"):
            for expression in ticked(row["표현"]):
                rule = matcher(row["꼴"], expression)
                if rule:
                    rules.append((expression, rule))
        self.assertGreaterEqual(len(rules), 27, "디자인 문서 쓰지 않는 표현 표를 다 읽지 못했습니다")
        found = [
            f"{name}: {expression} ← {value}"
            for name, value in self.values.items()
            for expression, rule in rules
            if rule(value)
        ]
        self.assertEqual(found, [])

    def test_SC_3_용어_표에서_버린_말이_화면_문구에_없다(self):
        rules = []
        for row in first_table(DESIGN, "### 용어 표"):
            frozen = set(ticked(row["동결 예외"]))
            self.assertEqual(frozen - set(self.values), set(), f"동결 예외에 없는 이름: {row['동결 예외']}")
            for expression in ticked(row["버린 말"]):
                rule = matcher(row["꼴"], expression)
                if rule:
                    rules.append((expression, rule, frozen))
        self.assertGreaterEqual(len(rules), 15, "디자인 문서 용어 표를 다 읽지 못했습니다")
        found = [
            f"{name}: {expression} ← {value}"
            for name, value in self.values.items()
            for expression, rule, frozen in rules
            if name not in frozen and rule(value)
        ]
        self.assertEqual(found, [])

    def test_SC_4_바꾼_문구_표의_값이_화면_문구에_있다(self):
        rows = changed_rows()
        self.assertEqual(len(rows), 56)
        self.assertEqual(len({row["name"] for row in rows}), len(rows), "같은 이름이 두 행에 있습니다")
        mismatched = [
            f"{row['name']}: {self.values.get(row['name'])!r} ≠ {row['after']!r}"
            for row in rows
            if self.values.get(row["name"]) != row["after"]
        ]
        self.assertEqual(mismatched, [])

    def test_SC_5_바꾼_문구는_형식_자리가_그대로다(self):
        found = [
            f"{row['name']}: {PLACEHOLDER.findall(row['before'])} → {PLACEHOLDER.findall(row['after'])}"
            for row in changed_rows()
            if PLACEHOLDER.findall(row["before"]) != PLACEHOLDER.findall(row["after"])
        ]
        self.assertEqual(found, [])

    def test_SC_6_버튼_라벨_한_줄_자리는_글자_수가_늘지_않는다(self):
        found = []
        for row in changed_rows():
            grown = len(row["after"]) - len(row["before"])
            if grown != row["length"]:
                found.append(f"{row['name']}: 길이 칸 {row['length']} ≠ 실제 {grown}")
            if any(place in row["place"] for place in FIXED_PLACES) and grown > 0:
                found.append(f"{row['name']}: {row['place']} 자리인데 {grown}자 늘었습니다")
        self.assertEqual(found, [])

    def test_SC_8_템플릿_README_가_인용한_앱_문장이_화면_문구에_있다(self):
        quoted = re.findall(r'앱에 "([^"]+)" 가 뜹니다', TEMPLATE_README.read_text(encoding="utf-8"))
        self.assertTrue(quoted, "README 에서 앱 문장 인용을 찾지 못했습니다")
        missing = [text for text in quoted if not any(text in value for value in self.values.values())]
        self.assertEqual(missing, [])


if __name__ == "__main__":
    unittest.main()
