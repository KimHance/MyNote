import re
import textwrap
import unittest

import support

FENCE = re.compile(r"^( *)(`{3,})markdown\n(.*?)^\1\2$", re.MULTILINE | re.DOTALL)


def example_notes(path):
    """문서의 markdown 펜스 중 앞머리로 시작하는 완전한 노트들."""
    text = path.read_text(encoding="utf-8")
    blocks = [textwrap.dedent(m.group(3)) for m in FENCE.finditer(text)]
    return [b for b in blocks if b.startswith("---\n")]


class DocsTest(support.TempCase):
    def assert_examples_pass(self, path):
        examples = example_notes(path)
        self.assertTrue(examples, f"{path.name} 에 앞머리로 시작하는 예시 노트가 없다")
        notes = {f"example/example-{i}.md": text for i, text in enumerate(examples)}
        root = self.notes_tree("tree", notes)

        result = self.generate(root)

        self.assertEqual(result.returncode, 0, result.stderr)

    def test_NF_15_쓰기_스킬은_JSON_노트_경로를_말하지_않는다(self):
        """NF-15 쓰기 스킬은 JSON 노트 경로를 말하지 않는다"""
        text = support.WRITE_NOTE.read_text(encoding="utf-8")
        description = text.split("\n---\n", 1)[0]
        self.assertIn("notes/<group>/<id>.md", description)
        self.assertNotIn("<id>.json", description)
        self.assertNotIn("notes/<group>/<id>.json", text)
        self.assertNotIn("--no-verify", text)
        self.assertIn("notes/<group>/<id>.md", text)
        for key in ("title:", "date:"):
            self.assertIn(key, text)
        self.assertIn("never touches", text)
        self.assertIn("git push", text)
        self.assertIn("verify it landed", text)

    def test_NF_15_쓰기_스킬의_예시_노트가_생성기를_통과한다(self):
        """NF-15 쓰기 스킬의 예시 노트가 생성기를 통과한다"""
        self.assert_examples_pass(support.WRITE_NOTE)

    def test_NF_15_규격_문서의_예시_노트가_생성기를_통과한다(self):
        """NF-15 규격 문서의 예시 노트가 생성기를 통과한다"""
        self.assert_examples_pass(support.SCHEMA)


if __name__ == "__main__":
    unittest.main()
