import re
import unittest
from collections import Counter

import support

# (SCHEMA.md 제목, SPEC.md 제목) — 말투가 달라 문장은 견주지 않고 표 첫 칸 · 수만 견준다(결정 22)
TABLES = [
    ("## 3. 본문 — 문법과 블록", "## 본문 — 문법과 블록"),
    ("### 글자 모양", "### 글자 모양"),
    ("### 글자로만 보이는 문법", "### 글자로만 보이는 문법"),
]
LIMITS = ("## 4. 한도", "## 한도")
RULES = ("## 5. 쓰기 규칙", "## 쓰기 규칙")
NUMBER = re.compile(r"\d[\d,]*")
RULE_ITEM = re.compile(r"^\d+\. ")


def limit_numbers(text, heading):
    """한도 절 전체(목록과 문단)의 정수 — 쉼표를 떼고 나온 횟수까지."""
    return Counter(int(n.replace(",", "")) for line in support.section(text, heading) for n in NUMBER.findall(line))


def rule_count(text, heading):
    return sum(1 for line in support.section(text, heading) if RULE_ITEM.match(line))


class SchemaParityTest(unittest.TestCase):
    def test_NT_1_규격의_문법_표와_한도와_쓰기_규칙이_SCHEMA_md_와_같다(self):
        """NT-1 규격의 문법 표와 한도와 쓰기 규칙이 SCHEMA.md 와 같다"""
        schema = support.SCHEMA.read_text(encoding="utf-8")
        spec = support.KIT_SPEC.read_text(encoding="utf-8")
        for schema_heading, spec_heading in TABLES:
            with self.subTest(spec_heading):
                expected = support.first_table_cells(schema, schema_heading)
                self.assertTrue(expected)
                self.assertEqual(support.first_table_cells(spec, spec_heading), expected)

        expected_limits = limit_numbers(schema, LIMITS[0])
        self.assertIn(524288, expected_limits)
        self.assertIn(130, expected_limits)
        self.assertEqual(limit_numbers(spec, LIMITS[1]), expected_limits)

        expected_rules = rule_count(schema, RULES[0])
        self.assertGreater(expected_rules, 0)
        self.assertEqual(rule_count(spec, RULES[1]), expected_rules)

        self.assertIn("인라인 HTML `<T>` · `<br>`", support.first_table_cells(spec, TABLES[2][1]))
        self.assertIn("`Box\\<T>`", spec)


if __name__ == "__main__":
    unittest.main()
