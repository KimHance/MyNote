import functools
import os
import re
import unittest
import xml.etree.ElementTree as ET
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


# FM-1 · FM-2 · FM-3 · FM-11 — 모듈 그래프는 Gradle 을 돌리지 않고 settings · 모듈 build 파일 · 카탈로그 · 컨벤션 소스에서 읽는다
SETTINGS = REPO_ROOT / "settings.gradle.kts"
CATALOG = REPO_ROOT / "gradle" / "libs.versions.toml"
CONVENTION = REPO_ROOT / "build-logic" / "convention"
FEATURES = (":feature:notes", ":feature:connect", ":feature:settings", ":feature:quiz", ":feature:stats")
CORE_UI = ":core:ui"
CORE_UI_DEPENDS = {":core:common", ":core:design-system", ":core:domain", ":core:navigation"}
DATA_LAYER = (":core:data", ":core:network", ":core:database", ":core:ai", ":core:format")
FEATURE_CONVENTION = "hancenote.android.feature"
ANDROID_LIBRARY = "hancenote.android.library"
# 문구 접두사 — 모듈 지도 · plan D7. `:app` 은 결정 16 대로 걸지 않는다
PREFIXES = {
    ":feature:notes": "notes_",
    ":feature:connect": "connect_",
    ":feature:settings": "settings_",
    ":feature:quiz": "quiz_",
    ":feature:stats": "stats_",
    CORE_UI: "ui_",
    ":core:design-system": "ds_",
    ":core:ai": "ai_",
}

INCLUDE = re.compile(r'include\(\s*"(:[\w:-]+)"\s*\)')
PROJECT_PATH = re.compile(r'project\(\s*"(:[\w:-]+)"\s*\)')
PROJECT_ACCESSOR = re.compile(r"\bprojects((?:\.\w+)+)")
PLUGIN_ALIAS = re.compile(r"alias\(\s*libs\.plugins\.([\w.]+)\s*\)")
PLUGIN_APPLY = re.compile(r'apply\(\s*(?:plugin\s*=\s*)?"(hancenote\.[\w.]+)"\s*\)')
CATALOG_PLUGIN = re.compile(r'^([\w-]+)\s*=\s*\{\s*id\s*=\s*"([^"]+)"', re.MULTILINE)
REGISTERED = re.compile(r'id\s*=\s*"([^"]+)"\s*\n\s*implementationClass\s*=\s*"(\w+)"')
RESOURCE_PREFIX = re.compile(r'resourcePrefix\s*=\s*"([^"]*)"')


def module_dir(path):
    return REPO_ROOT.joinpath(*path.strip(":").split(":"))


def accessor_of(path):
    """`:core:design-system` → `core.designSystem` — 타입 안전 프로젝트 접근자 이름."""

    def camel(part):
        head, *rest = part.split("-")
        return head + "".join(word[:1].upper() + word[1:] for word in rest)

    return ".".join(camel(part) for part in path.strip(":").split(":"))


def convention_sources():
    """컨벤션 플러그인 id → 그 소스 글자."""
    registered = REGISTERED.findall((CONVENTION / "build.gradle.kts").read_text(encoding="utf-8"))
    return {
        plugin_id: (CONVENTION / "src" / "main" / "java" / f"{name}.kt").read_text(encoding="utf-8")
        for plugin_id, name in registered
    }


def catalog_plugins():
    """카탈로그 `[plugins]` 별칭 접근자(`hancenote.android.feature`) → 플러그인 id."""
    text = CATALOG.read_text(encoding="utf-8")
    section = text.split("[plugins]", 1)[1].split("\n[", 1)[0]
    return {key.replace("-", "."): plugin_id for key, plugin_id in CATALOG_PLUGIN.findall(section)}


def module_graph():
    """settings 에 든 모듈마다 — 선언 의존 + 컨벤션이 넣는 의존 · 적용된 컨벤션 id · 문구 접두사."""
    paths = INCLUDE.findall(SETTINGS.read_text(encoding="utf-8"))
    by_accessor = {accessor_of(path): path for path in paths}
    aliases = catalog_plugins()
    conventions = convention_sources()
    graph = {}
    for path in paths:
        directory = module_dir(path)
        text = (directory / "build.gradle.kts").read_text(encoding="utf-8")
        unknown = [name for name in PROJECT_ACCESSOR.findall(text) if name.strip(".") not in by_accessor]
        if unknown:
            raise AssertionError(f"{path}: settings 에 없는 접근자 {unknown}")
        depends = {by_accessor[name.strip(".")] for name in PROJECT_ACCESSOR.findall(text)}
        depends |= set(PROJECT_PATH.findall(text))
        plugins, queue = set(), [aliases[alias] for alias in PLUGIN_ALIAS.findall(text)]
        prefix = next(iter(RESOURCE_PREFIX.findall(text)), None)
        while queue:
            plugin_id = queue.pop()
            if plugin_id in plugins:
                continue
            plugins.add(plugin_id)
            source = conventions.get(plugin_id)
            if source is None:
                continue
            depends |= set(PROJECT_PATH.findall(source))
            queue.extend(PLUGIN_APPLY.findall(source))
            if prefix is None:
                found = RESOURCE_PREFIX.findall(source)
                prefix = found[0].replace("${name}", directory.name) if found else None
        graph[path] = {"depends": depends, "plugins": plugins, "prefix": prefix, "directory": directory}
    return graph


def closure(graph, start):
    """`start` 가 닿는 모든 모듈(api · implementation · 시험 의존을 가리지 않는다 — 보수적으로)."""
    seen, queue = set(), list(graph[start]["depends"])
    while queue:
        path = queue.pop()
        if path in seen:
            continue
        seen.add(path)
        queue.extend(graph[path]["depends"])
    return seen


def resource_names(directory):
    """모듈 `src/main/res` 의 리소스 이름 — values 는 항목 이름, 나머지는 파일 이름."""
    names = []
    for path in sorted((directory / "src" / "main" / "res").glob("*/*")):
        if path.parent.name.startswith("values"):
            root = ET.parse(path).getroot()
            names += [node.get("name") for node in root if node.get("name")]
        else:
            names.append(path.name.split(".", 1)[0])
    return names


@functools.cache
def kotlin_main_sources():
    """모든 모듈 `main` 의 Kotlin 소스 — 빌드 폴더는 내려가지 않고 걷는다. 한 번 만들어 같이 쓴다."""
    found = []
    for top in SOURCE_ROOTS:
        for directory, children, files in os.walk(REPO_ROOT / top):
            children[:] = sorted(child for child in children if child not in SKIPPED_DIRS)
            if "main" in Path(directory).relative_to(REPO_ROOT).parts:
                found += [Path(directory) / name for name in sorted(files) if name.endswith(".kt")]
    return tuple(found)


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
    return [path for path in kotlin_main_sources() if path.name.endswith("ViewModel.kt")]


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


    def test_FM_1_모듈_지도의_모듈이_모두_등록돼_있다(self):
        graph = module_graph()
        self.assertEqual(sorted(path for path in graph if path.startswith(":feature:")), sorted(FEATURES))
        self.assertIn(CORE_UI, graph)
        for path in FEATURES:
            with self.subTest(path):
                self.assertIn(FEATURE_CONVENTION, graph[path]["plugins"])
        self.assertNotIn(FEATURE_CONVENTION, graph[CORE_UI]["plugins"])
        self.assertIn(ANDROID_LIBRARY, graph[CORE_UI]["plugins"])
        self.assertTrue(CORE_UI_DEPENDS <= graph[CORE_UI]["depends"], graph[CORE_UI]["depends"])

    def test_FM_2_기능_모듈은_서로를_의존하지_않는다(self):
        graph = module_graph()
        # 헛통과 막이 — 판정이 컨벤션이 넣는 의존과 접근자 이름을 실제로 읽는지, 기능 의존을 알아보는지 먼저 본다
        self.assertTrue(set(FEATURES) <= closure(graph, ":app"), "`:app` 이 기능 모듈 다섯을 다 보지 못했습니다")
        found = []
        for path in (*FEATURES, CORE_UI):
            reached = closure(graph, path)
            if path != CORE_UI:
                self.assertIn(CORE_UI, reached, f"{path}: 컨벤션이 넣는 `:core:ui` 를 읽지 못했습니다")
                self.assertIn(":core:design-foundation", reached, f"{path}: 닫힘이 접근자 의존을 따라가지 못했습니다")
            found += [f"{path} → {other}" for other in sorted(reached) if other.startswith(":feature:")]
        self.assertEqual(found, [])

    def test_FM_3_기능_모듈은_데이터_층을_모른다(self):
        graph = module_graph()
        self.assertTrue(set(DATA_LAYER) <= closure(graph, ":app"), "`:app` 이 데이터 층을 다 보지 못했습니다")
        found = [
            f"{path} → {other}"
            for path in (*FEATURES, CORE_UI)
            for other in sorted(closure(graph, path))
            if other in DATA_LAYER
        ]
        self.assertEqual(found, [])

    def test_FM_11_라이브러리_모듈마다_문구_접두사가_있다(self):
        graph = module_graph()
        libraries = [path for path, module in graph.items() if ANDROID_LIBRARY in module["plugins"]]
        self.assertTrue(set(PREFIXES) <= set(libraries), libraries)
        missing, wrong, unprefixed = [], [], []
        for path in libraries:
            module = graph[path]
            has_res = (module["directory"] / "src" / "main" / "res").is_dir()
            expected = PREFIXES.get(path)
            if expected is not None and module["prefix"] != expected:
                wrong.append(f"{path}: {module['prefix']!r} ≠ {expected!r}")
            elif expected is None and has_res and not module["prefix"]:
                missing.append(path)
            if module["prefix"] and has_res:
                unprefixed += [
                    f"{path}: {name}" for name in resource_names(module["directory"]) if not name.startswith(module["prefix"])
                ]
        self.assertEqual(missing, [], "리소스가 있는데 resourcePrefix 가 없는 라이브러리 모듈")
        self.assertEqual(wrong, [])
        self.assertEqual(unprefixed, [])
        self.assertNotIn(ANDROID_LIBRARY, graph[":app"]["plugins"])

if __name__ == "__main__":
    unittest.main()
