import json
import re
import shutil
import subprocess
import sys
import unittest
import xml.etree.ElementTree as ElementTree

import support

TEMPLATE = support.PUBLIC / "notes-template"
WORKFLOWS = TEMPLATE / ".github" / "workflows"
README = TEMPLATE / "README.md"
APP_STRINGS = support.REPO_ROOT / "feature" / "notes" / "src" / "main" / "res" / "values" / "strings.xml"
KIT_TOOL = support.load_module("kit_tool_for_template", support.KIT_TOOL)

README_SECTIONS = ["시작하기", "노트 추가", "비공개로 만들었다면", "형식", "문제 해결"]
APP_TERMS = [
    "notes_entry_create",
    "notes_connect_link_label",
    "notes_connect_action",
    "notes_connect_token_title",
    "notes_token_howto_kind",
    "notes_token_howto_scope",
    "notes_token_howto_perm",
]


def template_notes():
    return sorted((TEMPLATE / "notes").glob("*/*.json"))


def used_blocks_and_styles():
    blocks, styles = set(), set()

    def walk_spans(spans):
        for span in spans:
            styles.add(span.get("style", "text"))

    for path in template_notes():
        for block in json.loads(path.read_text(encoding="utf-8"))["blocks"]:
            blocks.add(block["type"])
            walk_spans(block.get("spans", []))
            for item in block.get("items", []):
                walk_spans(item["spans"])
    return blocks, styles


def readme_sections(text):
    sections, current = {}, None
    for line in text.splitlines():
        if line.startswith("## "):
            current = line[3:].strip()
            sections[current] = []
        elif current is not None:
            sections[current].append(line)
    return {name: "\n".join(lines) for name, lines in sections.items()}


def app_string(name):
    root = ElementTree.parse(APP_STRINGS).getroot()
    return next(node.text for node in root.iter("string") if node.get("name") == name)


class TemplateTest(support.TempCase):
    def run_tool(self, *args):
        return subprocess.run(
            [sys.executable, "-I", str(support.KIT_TOOL), *args],
            capture_output=True,
            text=True,
            cwd=str(self.tmp),
            env=self.env,
        )

    def workflow_lines(self):
        files = sorted(WORKFLOWS.glob("*"))
        self.assertEqual(len(files), 1, files)
        return files[0].read_text(encoding="utf-8").splitlines()

    def test_NT_5_템플릿의_index_가_도구_출력과_같다(self):
        """NT-5 템플릿의 index 가 도구 출력과 같다"""
        copy = self.tmp / "template-copy"
        shutil.copytree(TEMPLATE, copy)
        (copy / "notes" / "index.json").unlink()
        checked = self.run_tool("check", "--root", str(copy))
        self.assertEqual(checked.returncode, 0, checked.stdout + checked.stderr)
        made = self.run_tool("index", "--root", str(copy))
        self.assertEqual(made.returncode, 0, made.stdout + made.stderr)
        self.assertEqual(
            (copy / "notes" / "index.json").read_bytes(),
            (TEMPLATE / "notes" / "index.json").read_bytes(),
        )
        self.assertEqual((TEMPLATE / ".mynote-format").read_text(encoding="utf-8"), "1\n")

    def test_NT_5_예시가_8블록_3스타일을_다_쓴다(self):
        """NT-5 예시가 8블록 3스타일을 다 쓴다"""
        blocks, styles = used_blocks_and_styles()
        self.assertEqual(blocks, set(KIT_TOOL.BLOCK_TYPES))
        self.assertEqual(styles, set(KIT_TOOL.SPAN_STYLES))
        titles = [json.loads(path.read_text(encoding="utf-8"))["title"] for path in template_notes()]
        self.assertIn("시작하기", titles)

    def test_NT_5_템플릿에_스크립트가_없다(self):
        """NT-5 템플릿에 스크립트가 없다"""
        found = [path.relative_to(TEMPLATE).as_posix() for path in TEMPLATE.rglob("*") if path.is_file()]
        self.assertTrue(found)
        self.assertEqual([p for p in found if p.endswith((".py", ".sh"))], [])
        self.assertEqual([p for p in found if ".githooks" in p.split("/")], [])
        self.assertFalse((TEMPLATE / ".githooks").exists())

    def test_NT_5_workflow_줄_구조(self):
        """NT-5 workflow 줄 구조"""
        lines = self.workflow_lines()
        text = "\n".join(lines)
        self.assertLessEqual(len(lines), 35)

        kit = [i for i, line in enumerate(lines) if re.fullmatch(r"\s*-?\s*uses:\s*<([A-Z][A-Z0-9_]*)>@v1\s*", line)]
        checkout = [i for i, line in enumerate(lines) if re.fullmatch(r"\s*-?\s*uses:\s*actions/checkout@v\d+\s*", line)]
        self.assertEqual(len(kit), 1)
        self.assertEqual(len(checkout), 1)
        self.assertLess(checkout[0], kit[0])
        placeholder = re.search(r"<[A-Z][A-Z0-9_]*>(?=@v1)", lines[kit[0]]).group(0)
        table = support.table_firsts(support.PLACEHOLDERS.read_text(encoding="utf-8"), "# 자리표시자")
        self.assertIn(placeholder, table)

        self.assertRegex(text, r"(?m)^permissions:\n  contents: write$")
        self.assertRegex(text, r"(?m)^on:\n  push:\n    branches:\n      - '\*\*'$")
        self.assertNotRegex(text, r"(?m)^\s+tags(-ignore)?:")
        self.assertRegex(text, r"(?m)^    paths:\n      - 'notes/\*\*'\n      - '!notes/index\.json'$")
        self.assertRegex(text, r"(?m)^concurrency:\n  group: \S+")
        self.assertNotIn("persist-credentials", text)

    def test_NT_5_README_절이_다_있다(self):
        """NT-5 README 절이 다 있다"""
        text = README.read_text(encoding="utf-8")
        sections = readme_sections(text)
        for name in README_SECTIONS:
            self.assertIn(name, sections)
        self.assertEqual([name for name in sections if name in README_SECTIONS], README_SECTIONS)

        self.assertRegex(sections["시작하기"], r"저장소.*주소.*복사")
        self.assertIn("붙여 넣", sections["시작하기"])
        self.assertIn("Use this template", sections["시작하기"])
        for way in ("웹", "로컬", "플러그인"):
            self.assertIn(f"**{way}**", sections["노트 추가"])
        self.assertRegex(sections["노트 추가"], r"git pull.*한 번")
        self.assertIn("앱", sections["비공개로 만들었다면"])
        self.assertIn("SPEC.md", sections["형식"])
        self.assertIn("지원 앱 형식: `1`", sections["형식"])
        self.assertIn("Actions", sections["문제 해결"])
        self.assertIn("목록이 없습니다", sections["문제 해결"])
        self.assertIn("쓰기 권한", sections["문제 해결"])
        self.assertIn("브랜치 보호 규칙", sections["문제 해결"])

    def test_NT_5_README_용어가_앱_문구와_같다(self):
        """NT-5 README 용어가 앱 문구와 같다"""
        text = README.read_text(encoding="utf-8")
        for name in APP_TERMS:
            value = app_string(name)
            self.assertIn(value, text, f"{name} = {value}")


if __name__ == "__main__":
    unittest.main()
