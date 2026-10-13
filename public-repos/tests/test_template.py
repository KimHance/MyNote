import os
import re
import shutil
import subprocess
import sys
import unittest
import xml.etree.ElementTree as ElementTree
from pathlib import Path

import support

TEMPLATE = support.PUBLIC / "notes-template"
WORKFLOWS = TEMPLATE / ".github" / "workflows"
README = TEMPLATE / "README.md"
TEMPLATE_TOOL_REL = "tools/note_tool.py"
# 앱 문구는 모듈을 옮겨도 따라가게 모든 모듈의 strings.xml 에서 이름으로 찾는다(feature-modules FM-12)
APP_MODULES = ("app", "core", "feature")
SKIPPED_DIRS = {"build", ".gradle", ".kotlin"}
KIT_TOOL = support.load_module("kit_tool_for_template", support.KIT_TOOL)

README_SECTIONS = ["시작하기", "노트 추가", "비공개로 만들었다면", "형식", "문제 해결"]
APP_TERMS = [
    "connect_entry_create",
    "connect_link_label",
    "connect_action",
    "connect_token_title",
    "ui_token_howto_kind",
    "ui_token_howto_scope",
    "ui_token_howto_perm",
]


def template_files():
    return sorted(
        path.relative_to(TEMPLATE / "notes").as_posix()
        for path in (TEMPLATE / "notes").rglob("*")
        if path.is_file()
    )


def readme_sections(text):
    """`## ` 절 이름 → 본문. 코드 블록 안의 `## ` 줄은 절이 아니다."""
    sections, current, fenced = {}, None, False
    for line in text.splitlines():
        if line.startswith("```"):
            fenced = not fenced
        if line.startswith("## ") and not fenced:
            current = line[3:].strip()
            sections[current] = []
        elif current is not None:
            sections[current].append(line)
    return {name: "\n".join(lines) for name, lines in sections.items()}


def app_strings_files():
    """모든 모듈의 `src/main/res/values/strings.xml` — 빌드 폴더는 내려가지 않고 걷는다."""
    for top in APP_MODULES:
        for directory, children, files in os.walk(support.REPO_ROOT / top):
            children[:] = sorted(child for child in children if child not in SKIPPED_DIRS)
            path = Path(directory) / "strings.xml"
            if "strings.xml" in files and path.relative_to(support.REPO_ROOT).parts[-5:-1] == ("src", "main", "res", "values"):
                yield path


def app_string(name):
    found = [
        node.text
        for path in app_strings_files()
        for node in ElementTree.parse(path).getroot().iter("string")
        if node.get("name") == name
    ]
    if len(found) != 1:
        raise AssertionError(f"{name}: 정의 {len(found)}개")
    return found[0]


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
        self.assertEqual((TEMPLATE / ".mynote-format").read_text(encoding="utf-8"), "2\n")

    def test_NT_5_예시는_마크다운_노트_둘이다(self):
        """NT-5 예시는 마크다운 노트 둘이다"""
        self.assertEqual(
            template_files(),
            ["index.json", "start/getting-started.md", "start/note-format.md"],
        )
        self.assertEqual([p for p in template_files() if p.endswith(".json") and p != "index.json"], [])
        self.assertEqual((TEMPLATE / ".mynote-format").read_text(encoding="utf-8"), "2\n")
        groups, count = KIT_TOOL.collect(TEMPLATE, "notes")
        self.assertEqual(count, 2)
        self.assertEqual(
            [(e["id"], e["title"], e["date"]) for e in groups["start"]],
            [("getting-started", "시작하기", "2026.10.06"), ("note-format", "노트 형식 둘러보기", "2026.10.06")],
        )

    def test_NT_5_템플릿의_스크립트는_도구_사본_하나다(self):
        """NT-5 템플릿의 스크립트는 도구 사본 하나다"""
        found = [
            path.relative_to(TEMPLATE).as_posix()
            for path in TEMPLATE.rglob("*")
            if path.is_file() and not support.SKIPPED_DIRS & set(path.relative_to(TEMPLATE).parts)
        ]
        self.assertTrue(found)
        self.assertEqual([p for p in found if p.endswith((".py", ".sh"))], [TEMPLATE_TOOL_REL])
        self.assertEqual([p for p in found if ".githooks" in p.split("/")], [])
        self.assertFalse((TEMPLATE / ".githooks").exists())

    def test_NT_5_템플릿의_도구_규격_사본이_정본과_같다(self):
        """NT-5 템플릿의 도구 · 규격 사본이 정본과 같다"""
        self.assertEqual((TEMPLATE / TEMPLATE_TOOL_REL).read_bytes(), support.KIT_TOOL.read_bytes())
        self.assertEqual((TEMPLATE / "SPEC.md").read_bytes(), support.KIT_SPEC.read_bytes())

    def test_NT_5_workflow_줄_구조(self):
        """NT-5 workflow 줄 구조"""
        lines = self.workflow_lines()
        text = "\n".join(lines)
        self.assertLessEqual(len(lines), 35)

        run = [i for i, line in enumerate(lines) if re.fullmatch(rf"\s*run:\s*python3 {re.escape(TEMPLATE_TOOL_REL)} publish\s*", line)]
        uses = [i for i, line in enumerate(lines) if re.match(r"\s*-?\s*uses:", line)]
        checkout = [i for i, line in enumerate(lines) if re.fullmatch(r"\s*-?\s*uses:\s*actions/checkout@v\d+\s*", line)]
        self.assertEqual(len(run), 1)
        self.assertEqual(len(checkout), 1)
        self.assertEqual(uses, checkout)
        self.assertLess(checkout[0], run[0])

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
        for way in ("웹", "로컬"):
            self.assertIn(f"**{way}**", sections["노트 추가"])
        self.assertRegex(sections["노트 추가"], r"git pull.*한 번")
        self.assertIn("`notes/<그룹>/<id>.md`", sections["노트 추가"])
        for key in KIT_TOOL.FRONT_MATTER_KEYS:
            self.assertIn(f"`{key}`", sections["노트 추가"])
        self.assertNotRegex(text, r"<id>\.json")
        self.assertIn("앱", sections["비공개로 만들었다면"])
        self.assertIn("[`SPEC.md`](SPEC.md)", sections["형식"])
        self.assertNotRegex(text, r"<[A-Z][A-Z0-9_]+>")
        self.assertIn("지원 앱 형식: `2`", sections["형식"])
        self.assertIn("글자로 보이", sections["형식"])
        self.assertIn("마크다운을 읽는 앱 판", sections["형식"])
        self.assertIn("Actions", sections["문제 해결"])
        self.assertIn("앞머리", sections["문제 해결"])
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
