import itertools
import json
import re
import unittest

import helpers

TOOL = helpers.load_tool()


def table_firsts(text, heading):
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip() == heading)
    values = []
    for line in lines[start + 1:]:
        if line.startswith("#"):
            break
        match = re.match(r"\|\s*`([^`]+)`\s*\|", line)
        if match:
            values.append(match.group(1))
    return values


def table_row(text, heading, first):
    """heading 아래 표에서 첫 칸이 `first` 인 행의 칸 목록."""
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip() == heading)
    for line in lines[start + 1:]:
        if line.startswith("#"):
            break
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if cells[0] == f"`{first}`":
            return cells
    raise AssertionError(f"{heading} 표에 {first} 행이 없습니다")


def first_fence(text, heading, lang):
    """heading 아래 첫 `lang` 코드 블록 — 여는 줄의 백틱 수만큼으로 닫는다."""
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip() == heading)
    body, fence = [], None
    for line in lines[start + 1:]:
        if fence is None:
            match = re.fullmatch(r"(`{3,})" + re.escape(lang), line)
            if match:
                fence = match.group(1)
        elif line == fence:
            return "\n".join(body) + "\n"
        else:
            body.append(line)
    raise AssertionError(f"{heading} 아래에 {lang} 코드 블록이 없습니다")


def date_pattern(shape):
    """`YYYY.MM.DD` 같은 모양을 검사기 정규식 꼴로 — Y · M · D 는 숫자 한 자리."""
    parts = []
    for char, run in itertools.groupby(shape):
        count = len(list(run))
        unit = "[0-9]" if char in "YMD" else re.escape(char)
        parts.append(unit + (f"{{{count}}}" if count > 1 else ""))
    return "".join(parts)


class SpecTest(helpers.ToolCase):
    def setUp(self):
        super().setUp()
        self.text = helpers.SPEC.read_text(encoding="utf-8")

    def test_NT_1_규격의_닫힌_집합이_검사기와_같다(self):
        """NT-1 규격의 닫힌 집합이 검사기와 같다"""
        blocks = table_firsts(self.text, "### 블록 종류")
        styles = table_firsts(self.text, "### 인라인 스타일")
        levels = [int(v) for v in table_firsts(self.text, "### 헤딩 레벨")]
        self.assertEqual(len(blocks), 8)
        self.assertEqual(len(styles), 3)
        self.assertEqual(set(blocks), set(TOOL.BLOCK_TYPES))
        self.assertEqual(set(styles), set(TOOL.SPAN_STYLES))
        self.assertEqual(levels, list(TOOL.HEADING_LEVELS))

    def test_NT_1_규격의_앞머리_규칙이_검사기와_같다(self):
        """NT-1 규격의 앞머리 규칙이 검사기와 같다"""
        self.assertEqual(tuple(table_firsts(self.text, "### 앞머리 키")), TOOL.FRONT_MATTER_KEYS)
        shape = table_row(self.text, "### 앞머리 키", "date")[1]
        self.assertRegex(shape, r"^`[YMD.]+`$")
        self.assertEqual(date_pattern(shape.strip("`")), TOOL.DATE_FORMAT.pattern)
        example = first_fence(self.text, "## 앞머리", "markdown")
        self.assertEqual(example.splitlines()[0], TOOL.FRONT_MATTER_FENCE)
        self.assertIn(f"첫 줄이 정확히 `{TOOL.FRONT_MATTER_FENCE}`", self.text)
        head, _ = TOOL.split_front_matter(example)
        fields, errors = TOOL.front_matter_errors(head)
        self.assertEqual(errors, [])
        self.assertEqual(tuple(fields), TOOL.FRONT_MATTER_KEYS)

    def test_NT_1_규격의_예시_노트가_검사를_통과한다(self):
        """NT-1 규격의 예시 노트가 검사를 통과한다"""
        markdown = first_fence(self.text, "### 예시 마크다운 노트", "markdown")
        old_json = first_fence(self.text, "### 예시 JSON 노트", "json")
        note = json.loads(old_json)
        used_types = {b["type"] for b in note["blocks"]}
        used_styles = {
            s.get("style", "text")
            for b in note["blocks"]
            for s in b.get("spans", [])
        }
        self.assertEqual(used_types, set(TOOL.BLOCK_TYPES))
        self.assertEqual(used_styles, set(TOOL.SPAN_STYLES))

        root = self.make_dir()
        self.write(root, "notes/start/hello-notes.md", markdown)
        self.write(root, "notes/start/hello-old.json", old_json)
        result = self.tool("index", "--root", str(root))
        self.assertEqual(result.returncode, 0, result.stderr)
        made = json.loads((root / "notes" / "index.json").read_bytes())

        index = json.loads(first_fence(self.text, "### 예시 index.json", "json"))
        self.assertEqual(list(index), ["start"])
        for entry in index["start"]:
            self.assertEqual(set(entry), {"id", "title", "date", "path", "hash"})
        for entry in made["start"] + index["start"]:
            del entry["hash"]
        self.assertEqual(made["start"], index["start"])


if __name__ == "__main__":
    unittest.main()
