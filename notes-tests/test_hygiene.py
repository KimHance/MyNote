import os
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path, PurePosixPath

REPO_ROOT = Path(__file__).resolve().parents[1]
AGENTS = REPO_ROOT / "AGENTS.md"

WALK_SKIPPED = {"build", ".gradle", ".kotlin", "__pycache__"}
NOTE_CONTENT = "notes"
FIXTURE_DIRS = ("notes-tests/fixtures/", "core/format/src/testFixtures/")

HOME_ALLOWED = {".gradle"}
EMAIL_ALLOWED = {"a@b.com", "git@github.com"}

HOME_SHORT = "홈 폴더 줄임 경로"
EMAIL = "이메일"
PATH_NAME = re.compile(r"[\w.-]*", re.ASCII)
PATTERNS = {
    "홈 폴더 절대 경로": re.compile(r"(?<![\w.-])/(?:Users|home|root)/|\b[A-Za-z]:[\\/]+[Uu]sers[\\/]"),
    HOME_SHORT: re.compile(r"(?:(?<![\w~])~|\$HOME\b|\$\{HOME\}|%USERPROFILE%)[\\/]+(?P<name>[^\s\\/`'\")\]]*)"),
    "임시 폴더 경로": re.compile(
        r"(?<![\w.-])(?:/private)?/(?:tmp|var/tmp|var/folders)(?![\w-])"
        r"|\$\{?TMPDIR\b|%TE?MP%|\bAppData[\\/]+Local[\\/]+Temp\b"
    ),
    "GitHub 토큰": re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{82})\b"),
    EMAIL: re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}"),
    "대화 인용": re.compile(
        r"사용자\s*(?:지시|말|요청|발화|결정)?\s*[(（:]\s*[\"“”'‘’「『]"
        r"|사용자가\s*[\"“”'‘’「『][^\"“”'‘’「」『』\n]{1,200}[\"“”'‘’」』]\s*(?:이?라고|고)\s*(?:했|말했)"
    ),
}
CONVERSATION_ID = re.compile(r"대화[^상]{0,4}`?[0-9a-f]{6,}")
RUN_MATERIAL = re.compile(r"<runs>|(?<![\w.-])(?:runs|shots)/")


def violations(text):
    """글에서 위생 위반을 (줄 번호, 갈래) 목록으로 돌려준다."""
    found = []
    for number, line in enumerate(text.splitlines(), 1):
        for label, pattern in PATTERNS.items():
            for match in pattern.finditer(line):
                if label == HOME_SHORT and PATH_NAME.match(match.group("name")).group(0).rstrip(".") in HOME_ALLOWED:
                    continue
                if label == EMAIL and match.group(0) in EMAIL_ALLOWED:
                    continue
                found.append((number, label))
    return found


def is_doc(rel):
    parts = PurePosixPath(rel).parts
    if rel.startswith(FIXTURE_DIRS) or (parts[0] == NOTE_CONTENT and len(parts) > 2):
        return False
    suffix = PurePosixPath(rel).suffix.lower()
    return suffix == ".md" or (parts[0] == "labs" and suffix == ".html")


def all_files(root):
    if (root / ".git").exists():
        result = subprocess.run(
            ["git", "-C", str(root), "ls-files", "-z"],
            capture_output=True,
            check=True,
        )
        return [rel for rel in result.stdout.decode("utf-8").split("\0") if rel]
    found = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames if d not in WALK_SKIPPED)
        found += [Path(dirpath, name).relative_to(root).as_posix() for name in sorted(filenames)]
    return found


def tracked_docs(root):
    """검사 대상 문서의 상대 경로. `.git` 이 없으면 폴더를 걷는다."""
    return [rel for rel in all_files(root) if is_doc(rel) and (root / rel).is_file()]


def scan(root):
    """root 의 검사 대상 문서에서 찾은 위반을 `경로:줄 갈래` 목록으로 돌려준다."""
    hits = []
    for rel in tracked_docs(root):
        text = (root / rel).read_text(encoding="utf-8")
        hits += [f"{rel}:{number} {label}" for number, label in violations(text)]
    return hits


def leftovers(root, pattern):
    """검사 대상 문서에서 pattern 이 든 줄을 `경로:줄` 목록으로 돌려준다."""
    return [
        f"{rel}:{number}"
        for rel in tracked_docs(root)
        for number, line in enumerate((root / rel).read_text(encoding="utf-8").splitlines(), 1)
        if pattern.search(line)
    ]


def check_stdin():
    hits = violations(sys.stdin.buffer.read().decode("utf-8"))
    sys.stdout.buffer.write("".join(f"-:{number} {label}\n" for number, label in hits).encode("utf-8"))
    return 1 if hits else 0


PLANTED = {
    "홈 폴더 절대 경로": ["/Users/someone/notes", "/home/someone/notes", "C:\\Users\\someone\\notes"],
    HOME_SHORT: ["~/notes/a.md", "$HOME/notes", "~/.gradle-old/x", "~/.gradle.d/x"],
    "임시 폴더 경로": ["/tmp/build.log", "/private/var/folders/x/T/a", "$TMPDIR/a", "%TEMP%\\a"],
    "GitHub 토큰": ["ghp_" + "A1" * 18, "github_pat_" + "a1B2" * 20 + "c3"],
    EMAIL: ["someone@example.org"],
    "대화 인용": [
        '사용자 지시("이렇게 해 줘")',
        "사용자 말('그만')",
        '사용자 결정("그렇게 가자")',
        '사용자: "그만"',
        '사용자가 "그만" 이라고 했다',
        "사용자가 '그만' 이라고 했다",
    ],
}
ALLOWED = [
    "`~/.gradle/gradle.properties`",
    "| ~/.gradle| ~/.gradle, ~/.gradle. ~/.gradle에 둔다 |",
    "`git@github.com:owner/repo.git` · `<a@b.com>`",
    "힌트 `github_pat_…` · `ghp_xxxx`",
    "`/sdcard/Android/data/app/files/model.bin` · `/data/local/tmp`",
    "https://github.com/octo-learner/notes-template",
    "사용자 결정(장부 D7) · 사용자 지시 · 사용자 말 · 사용자: 앱을 켜는 사람 · 사용자가 '저장' 을 누르고",
]


class HygieneTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tmp = Path(tmp.name).resolve()

    def write(self, root, rel, content):
        path = root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    def run_stdin(self, text):
        result = subprocess.run(
            [sys.executable, "-I", str(Path(__file__).resolve()), "-"],
            input=text.encode("utf-8"),
            capture_output=True,
            cwd=str(self.tmp),
        )
        return result.returncode, result.stdout.decode("utf-8")

    def test_BR_9_추적_문서에_로컬_경로와_시크릿과_대화_인용이_없다(self):
        """BR-9 추적 문서에 로컬 경로와 시크릿과 대화 인용이 없다"""
        self.assertEqual(scan(REPO_ROOT), [])

    def test_BR_9_갈래마다_심은_줄을_잡는다(self):
        """BR-9 갈래마다 심은 줄을 잡는다"""
        for group, (label, lines) in enumerate(PLANTED.items()):
            for index, line in enumerate(lines):
                with self.subTest(label=label, line=line):
                    root = self.tmp / f"planted-{group}-{index}"
                    self.write(root, "docs/planted.md", f"# 문서\n{line}\n")
                    self.assertEqual(scan(root), [f"docs/planted.md:2 {label}"])
        root = self.tmp / "allowed"
        self.write(root, "docs/allowed.md", "\n".join(ALLOWED) + "\n")
        self.assertEqual(scan(root), [])

    def test_BR_9_대상에_레포_문서가_들어_있다(self):
        """BR-9 대상에 레포 문서가 들어 있다"""
        docs = tracked_docs(REPO_ROOT)
        for rel in (
            "AGENTS.md",
            "DECISIONS-TO-REVISIT.md",
            "README.md",
            "labs/brand-design/README.md",
            "notes/SCHEMA.md",
        ):
            self.assertIn(rel, docs)
        for folder in ("docs/spec/", "docs/design/", "core/data/", "public-repos/"):
            self.assertTrue(any(rel.startswith(folder) for rel in docs), folder)
        self.assertTrue(any(rel.startswith("labs/") and rel.endswith(".html") for rel in docs))
        self.assertFalse([rel for rel in docs if rel.startswith(FIXTURE_DIRS)])
        self.assertFalse([rel for rel in docs if rel.startswith(f"{NOTE_CONTENT}/") and rel.count("/") > 1])

        root = self.tmp / "walk"
        for rel in (
            "AGENTS.md",
            "labs/a/index.html",
            "notes/SCHEMA.md",
            "notes/group/note.md",
            "notes-tests/fixtures/front-matter/a.md",
            "core/format/src/testFixtures/resources/a.md",
            "app/build/report.md",
            ".gradle/x.md",
            ".kotlin/x.md",
            "notes-tests/__pycache__/x.md",
            "app/src/main/res/values/strings.xml",
            "docs/page.html",
        ):
            self.write(root, rel, "문서\n")
        self.assertEqual(tracked_docs(root), ["AGENTS.md", "labs/a/index.html", "notes/SCHEMA.md"])

    def test_BR_9_표준_입력으로_준_글도_검사한다(self):
        """BR-9 표준 입력으로 준 글도 검사한다"""
        self.assertEqual(self.run_stdin("제목\n\n본문 " + PLANTED["대화 인용"][0] + "\n"), (1, "-:3 대화 인용\n"))
        self.assertEqual(self.run_stdin("\n".join(ALLOWED) + "\n"), (0, ""))
        for label, lines in PLANTED.items():
            with self.subTest(label):
                self.assertEqual(self.run_stdin(lines[0]), (1, f"-:1 {label}\n"))

    def test_BR_10_루트_AGENTS_에_문서_위생_규칙이_있다(self):
        """BR-10 루트 AGENTS 에 문서 위생 규칙이 있다"""
        text = AGENTS.read_text(encoding="utf-8")
        self.assertEqual(violations(text), [])
        words = ("문서 위생", "로컬 절대 경로", "개인 경로", "대화 인용", "시크릿", "결정 번호", "커밋 본문")
        self.assertTrue([line for line in text.splitlines() if all(word in line for word in words)])
        model = text.split("\n## 모델 파일\n", 1)[1]
        self.assertIn("/sdcard/Android/data/com.hancekim.mynote/files/", model)
        self.assertFalse(re.search(r"^PC\b", model, re.MULTILINE))

    def test_BR_10_검증_명령에_위생_시험이_있다(self):
        """BR-10 검증 명령에 위생 시험이 있다"""
        text = AGENTS.read_text(encoding="utf-8")
        block = text.split("한 번에 검증하려면:", 1)[1].split("```", 2)[1]
        self.assertIn("python3 -I -m unittest discover notes-tests", block)
        here = Path(__file__).resolve()
        self.assertEqual((here.parent.name, here.name[:5]), ("notes-tests", "test_"))
        command = f"python3 {here.parent.name}/{here.name} -"
        self.assertTrue(any(command in line for line in text.splitlines()), command)

    def test_BR_10_디자인_도구_대화_ID_가_없다(self):
        """BR-10 디자인 도구 대화 ID 가 없다"""
        self.assertEqual(leftovers(REPO_ROOT, CONVERSATION_ID), [])
        root = self.tmp / "ids"
        lines = [
            "프로젝트 `0123abcd` · 확인 대화상자 · 대화 상자",
            "대화 `0123abcd`",
            "(대화 0123abcd-4567)",
            "대화 ID `0123abcd`",
            "대화: 0123abc",
        ]
        self.write(root, "docs/a.md", "\n".join(lines) + "\n")
        self.assertEqual(leftovers(root, CONVERSATION_ID), [f"docs/a.md:{n}" for n in (2, 3, 4, 5)])

    def test_BR_10_레포_밖_run_자료_경로가_없다(self):
        """BR-10 레포 밖 run 자료 경로가 없다"""
        self.assertEqual(leftovers(REPO_ROOT, RUN_MATERIAL), [])
        root = self.tmp / "runs"
        lines = [
            "`screenshots/` · `labs/x/r1/` · runs-old/ · 장부 D15",
            "사진(`<runs>/shots/main-check/`)",
            "| `shots/retry-display/card.png` |",
            "runs/brand/brief.md",
        ]
        self.write(root, "docs/a.md", "\n".join(lines) + "\n")
        self.assertEqual(leftovers(root, RUN_MATERIAL), [f"docs/a.md:{n}" for n in (2, 3, 4)])


if __name__ == "__main__":
    if sys.argv[1:] == ["-"]:
        sys.exit(check_stdin())
    unittest.main()
