import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = ("app", "core", "feature")
SKIPPED_DIRS = {"build", ".gradle", ".kotlin"}

# FM-8 — ViewModel 이 시간 · 문구 · 로그를 직접 읽으면 JVM 시험에서 가상 시간으로 만들 수 없다
DIRECT_READS = ("SystemClock", "System.currentTimeMillis", "android.util.Log", "@ApplicationContext", "getString(")
EXPECTED_VIEW_MODELS = {"NotesViewModel.kt", "StatsViewModel.kt", "ConnectViewModel.kt"}


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


if __name__ == "__main__":
    unittest.main()
