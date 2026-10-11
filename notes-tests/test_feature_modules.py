import re
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = ("app", "core", "feature")
SKIPPED_DIRS = {"build", ".gradle", ".kotlin"}

# FM-8 — ViewModel 이 시간 · 문구 · 로그를 직접 읽으면 JVM 시험에서 가상 시간으로 만들 수 없다
DIRECT_READS = ("SystemClock", "System.currentTimeMillis", "android.util.Log", "@ApplicationContext", "getString(")
EXPECTED_VIEW_MODELS = {
    "NotesViewModel.kt",
    "ShellViewModel.kt",
    "SettingsViewModel.kt",
    "QuizViewModel.kt",
    "StatsViewModel.kt",
    "ConnectViewModel.kt",
}

# FM-7 — 목적지 스코프 ViewModel 은 창 등급이 바뀌어 NavDisplay 가 다시 만들어질 때 새로 생긴다(결정 7)
ACTIVITY_OWNER = "viewModelStoreOwner"
ENTRY_DECORATOR = "rememberViewModelStoreNavEntryDecorator"
MIN_HILT_CALLS = 7

# FM-7 · SY-1 — 동기화 흐름은 차갑다. 구독마다 동기화가 돈다
SYNC_PARAM = re.compile(r"\w+\s*:\s*ObserveSyncRunsUseCase\b")

# FM-7 — ViewModel 경계 표. 파일 → 그 안에 없어야 할 이름
OTHER_FEATURE_STATE = {
    "NotesViewModel.kt": (
        "QuizUiState",
        "TokenEditState",
        "StartModelDownloadUseCase",
        "CancelModelDownloadUseCase",
        "DiscardPartialModelUseCase",
        "DeleteModelUseCase",
    ),
    "ShellViewModel.kt": (
        "GetCategoriesUseCase",
        "QuizUiState",
        "TokenEditState",
        "ObserveAppliedTokenUseCase",
    ),
}


def kotlin_main_sources():
    for top in SOURCE_ROOTS:
        for path in (REPO_ROOT / top).rglob("*.kt"):
            rel = path.relative_to(REPO_ROOT)
            if SKIPPED_DIRS.intersection(rel.parts) or "main" not in rel.parts:
                continue
            yield path


def call_arguments(text, name):
    """`name(` 호출마다 짝 괄호까지의 인자 글자."""
    for match in re.finditer(re.escape(name) + r"\s*\(", text):
        depth, start = 0, match.end() - 1
        for index in range(start, len(text)):
            if text[index] == "(":
                depth += 1
            elif text[index] == ")":
                depth -= 1
                if depth == 0:
                    yield text[start + 1 : index]
                    break


def view_model_sources():
    for top in SOURCE_ROOTS:
        for path in (REPO_ROOT / top).rglob("*ViewModel.kt"):
            rel = path.relative_to(REPO_ROOT)
            if SKIPPED_DIRS.intersection(rel.parts) or "main" not in rel.parts:
                continue
            yield path


class FeatureModulesTest(unittest.TestCase):
    def test_FM_8_ViewModel_은_시간과_문구를_직접_읽지_않는다(self):
        sources = list(view_model_sources())
        self.assertTrue(EXPECTED_VIEW_MODELS <= {path.name for path in sources})
        found = [
            f"{path.relative_to(REPO_ROOT)}: {word}"
            for path in sources
            for word in DIRECT_READS
            if word in path.read_text(encoding="utf-8")
        ]
        self.assertEqual(found, [])

    def test_FM_7_기능_ViewModel_은_Activity_를_owner_로_받는다(self):
        calls, missing, decorators = 0, [], []
        for path in kotlin_main_sources():
            text = path.read_text(encoding="utf-8")
            for arguments in call_arguments(text, "hiltViewModel"):
                calls += 1
                if ACTIVITY_OWNER not in arguments:
                    missing.append(str(path.relative_to(REPO_ROOT)))
            if ENTRY_DECORATOR in text:
                decorators.append(str(path.relative_to(REPO_ROOT)))
        self.assertGreaterEqual(calls, MIN_HILT_CALLS)
        self.assertEqual(missing, [])
        self.assertEqual(decorators, [])

    def test_FM_7_동기화_UseCase_를_받는_클래스는_NotesViewModel_하나다(self):
        holders = [
            path.name
            for path in kotlin_main_sources()
            if SYNC_PARAM.search(path.read_text(encoding="utf-8"))
        ]
        self.assertEqual(holders, ["NotesViewModel.kt"])

    def test_FM_7_ViewModel_은_자기_기능의_상태만_든다(self):
        sources = {path.name: path for path in view_model_sources()}
        found = [
            f"{name}: {word}"
            for name, words in OTHER_FEATURE_STATE.items()
            for word in words
            if word in sources[name].read_text(encoding="utf-8")
        ]
        self.assertEqual(found, [])


if __name__ == "__main__":
    unittest.main()
