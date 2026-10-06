import json
import re
import unittest
from pathlib import Path

from helpers import KIT_DIR, SPEC, TOOL

MARKETPLACE = KIT_DIR / ".claude-plugin" / "marketplace.json"
PLUGIN_DIR = KIT_DIR / "plugins" / "mynote-notes"
PLUGIN_TOOL = PLUGIN_DIR / "scripts" / "note_tool.py"
PLUGIN_SPEC = PLUGIN_DIR / "SPEC.md"
TOOL_CALL = '${CLAUDE_PLUGIN_ROOT}/scripts/note_tool.py'
SPEC_PATH = '${CLAUDE_PLUGIN_ROOT}/SPEC.md'


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def skill_text(name):
    return (PLUGIN_DIR / "skills" / name / "SKILL.md").read_text(encoding="utf-8")


def skill_head(text):
    match = re.match(r"---\n(.*?)\n---\n", text, re.DOTALL)
    if not match:
        return {}
    head = {}
    for line in match.group(1).splitlines():
        key, sep, value = line.partition(":")
        if sep and not line.startswith(" "):
            head[key.strip()] = value.strip()
    return head


class PluginTest(unittest.TestCase):
    def test_NT_8_마켓플레이스_JSON_구조(self):
        """NT-8 마켓플레이스 JSON 구조"""
        market = read_json(MARKETPLACE)
        self.assertIsInstance(market["name"], str)
        self.assertTrue(market["name"])
        self.assertIsInstance(market["owner"]["name"], str)
        self.assertTrue(market["owner"]["name"])
        self.assertEqual([p["name"] for p in market["plugins"]], ["mynote-notes"])
        source = market["plugins"][0]["source"]
        self.assertTrue(source.startswith("./"))
        self.assertNotIn("..", source)
        folder = KIT_DIR / source
        self.assertTrue(folder.is_dir())
        manifest = read_json(folder / ".claude-plugin" / "plugin.json")
        self.assertEqual(manifest["name"], market["plugins"][0]["name"])
        self.assertTrue(manifest["description"])
        for skill in ("write-note", "check-notes"):
            self.assertTrue((folder / "skills" / skill / "SKILL.md").is_file())

    def test_NT_8_플러그인의_도구는_루트_도구와_바이트가_같다(self):
        """NT-8 플러그인의 도구는 루트 도구와 바이트가 같다"""
        self.assertFalse(PLUGIN_TOOL.is_symlink())
        self.assertEqual(PLUGIN_TOOL.read_bytes(), TOOL.read_bytes())

    def test_NT_8_플러그인의_규격은_루트_규격과_바이트가_같다(self):
        """NT-8 플러그인의 규격은 루트 규격과 바이트가 같다"""
        self.assertFalse(PLUGIN_SPEC.is_symlink())
        self.assertEqual(PLUGIN_SPEC.read_bytes(), SPEC.read_bytes())

    def test_NT_8_스킬_머리_정보(self):
        """NT-8 스킬 머리 정보"""
        for name in ("write-note", "check-notes"):
            with self.subTest(name):
                text = skill_text(name)
                head = skill_head(text)
                self.assertEqual(head.get("name"), name)
                self.assertTrue(head.get("description"))
                self.assertNotIn(str(KIT_DIR), text)
        self.assertIn(TOOL_CALL, skill_text("check-notes"))
        for name in ("write-note", "check-notes"):
            for path in re.findall(r"\S*note_tool\.py", skill_text(name)):
                with self.subTest(path):
                    self.assertTrue(path.rstrip('"').endswith(TOOL_CALL))

    def test_NT_9_스킬이_index_를_만지지_않는다고_말한다(self):
        """NT-9 스킬이 index 를 만지지 않는다고 말한다"""
        text = skill_text("write-note")
        self.assertIn("`index.json`", text)
        self.assertRegex(text, r"index\.json.{0,40}(만들지도 고치지도 않습니다|스테이징하지 않습니다)")
        self.assertNotRegex(text, r"index\.json.{0,30}(를|을) (쓴다|씁니다|만듭니다|고칩니다|커밋합니다)")
        self.assertNotIn("--no-verify", text)
        self.assertIn("Action", text)
        self.assertIn("git pull --rebase", text)
        self.assertRegex(text, r"미리보기")
        self.assertRegex(text, r"\.github/workflows")
        for private in ("gen_index", "pre-commit", ".githooks", "SCHEMA.md", "MyNote", "core.hooksPath"):
            with self.subTest(private):
                self.assertNotIn(private, text)

    def test_NT_9_스킬이_push_확인을_요구한다(self):
        """NT-9 스킬이 push 확인을 요구한다"""
        text = skill_text("write-note")
        self.assertIn("git push", text)
        self.assertRegex(text, r"push 가 성공해야 끝")
        self.assertRegex(text, r"push 가 안 됐으면 안 됐다고")

    def test_NT_9_스킬이_규격을_가리키고_복사하지_않는다(self):
        """NT-9 스킬이 규격을 가리키고 복사하지 않는다"""
        text = skill_text("write-note")
        self.assertIn(SPEC_PATH, text)
        self.assertEqual(text.count("SPEC.md"), text.count(SPEC_PATH))
        for kind in ("orderedList", "divider", "spacer"):
            with self.subTest(kind):
                self.assertNotIn(kind, text)
        self.assertNotRegex(text, r"\|\s*`(heading|paragraph|code|quote|bulletList|orderedList|divider|spacer)`\s*\|")

    def test_NT_10_스킬이_도구를_부르고_파일을_고치지_않는다(self):
        """NT-10 스킬이 도구를 부르고 파일을 고치지 않는다"""
        text = skill_text("check-notes")
        self.assertIn(f'python3 "{TOOL_CALL}" check', text)
        self.assertIn("파일 경로: 이유", text)
        self.assertIn("파이썬이 없으면", text)
        self.assertRegex(text, r"파일을 고치지 않습니다")
        self.assertIn(SPEC_PATH, text)
        self.assertEqual(text.count("SPEC.md"), text.count(SPEC_PATH))
        self.assertNotIn("--no-verify", text)


if __name__ == "__main__":
    unittest.main()
