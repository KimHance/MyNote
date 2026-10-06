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


def first_json_fence(text, heading):
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip() == heading)
    body, inside = [], False
    for line in lines[start + 1:]:
        if line.startswith("```json") and not inside:
            inside = True
        elif line.startswith("```") and inside:
            return "\n".join(body)
        elif inside:
            body.append(line)
    raise AssertionError(f"{heading} 아래에 json 코드 블록이 없습니다")


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

    def test_NT_1_규격의_예시_노트가_검사를_통과한다(self):
        """NT-1 규격의 예시 노트가 검사를 통과한다"""
        note = json.loads(first_json_fence(self.text, "### 예시 노트"))
        used_types = {b["type"] for b in note["blocks"]}
        used_styles = {
            s.get("style", "text")
            for b in note["blocks"]
            for s in b.get("spans", [])
        }
        self.assertEqual(used_types, set(TOOL.BLOCK_TYPES))
        self.assertEqual(used_styles, set(TOOL.SPAN_STYLES))
        root = self.make_dir()
        self.write(root, "notes/start/hello-notes.json", json.dumps(note, ensure_ascii=False, indent=2) + "\n")
        result = self.tool("check", "--root", str(root))
        self.assertEqual(result.returncode, 0, result.stderr)
        index = json.loads(first_json_fence(self.text, "### 예시 index.json"))
        self.assertEqual(list(index), ["start"])
        self.assertEqual(
            set(index["start"][0]), {"id", "title", "date", "path", "hash"}
        )


if __name__ == "__main__":
    unittest.main()
