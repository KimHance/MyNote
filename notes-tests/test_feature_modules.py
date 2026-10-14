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
    "EditorViewModel.kt",
}

# FM-7 — 목적지 스코프 ViewModel 은 창 등급이 바뀌어 NavDisplay 가 다시 만들어질 때 새로 생긴다(결정 7)
ACTIVITY_OWNER = "viewModelStoreOwner"
ENTRY_DECORATOR = "rememberViewModelStoreNavEntryDecorator"
MIN_HILT_CALLS = 8

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
FEATURES = (":feature:notes", ":feature:connect", ":feature:settings", ":feature:quiz", ":feature:stats", ":feature:editor")
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
    ":feature:editor": "editor_",
    CORE_UI: "ui_",
    ":core:design-system": "ds_",
    ":core:ai": "ai_",
}

# FM-1 — 공용 조각 표(기능 문서 feature-modules). 선언 이름 → 사는 모듈. 탭 · 리더 열기 명령은 `:core:navigation`
SHARED_PIECES = {
    CORE_UI: (
        "TokenField",
        "TokenHowTo",
        "TokenCreateButton",
        "VerifyingLine",
        "NoticeBox",
        "monoAnnotated",
        "ProgressBar",
        "ConnectIcons",
        "KeepWordsText",
        "openExternal",
        "TOKEN_PAGE_URL",
        "TokenFailureKind",
        "TokenReason",
        "tokenFailureRes",
        "RepoCardUi",
        "repoCardUi",
        "LastSync",
        "TokenDraft",
        "NotesDrafts",
        "ModelCardUi",
        "modelCardUi",
        "modelPillUi",
        "quizCtaUi",
        "ModelTone.color",
        "ModelPreviewFrame",
        "NotesSignal",
        "unseen",
        "PrimaryButton",
        "SecondaryButton",
        "SolidDestructiveButton",
        "QuietButton",
        "CONTENT_MAX_WIDTH",
        "NotesTopBar",
        "ActionDialog",
        "rememberBookmarkTimeLabel",
        "NotesScrollState",
        "ShellFrame",
        "ShellDestinations",
        "sceneEdgeModifier",
        "NotesMessages",
        "AndroidNotesMessages",
        "FailureLog",
        "AndroidFailureLog",
        "NoticeUi",
        "FontScale.toUi",
        "MyNoteFontScale.toDomain",
    ),
    ":core:navigation": ("openReaderCommand", "switchTabCommand", "NotesTab"),
}
# 외부 브라우저 찾기 — `openExternal` 과 같은 모듈 manifest 에 있어야 한다
BROWSER_QUERY = "android.intent.category.BROWSABLE"

# FM-1 — 모듈 지도의 화면 파일(기능 문서 feature-modules). 파일 이름 → 그 파일이 사는 기능 모듈. 모든 기능 떼기가 끝난 뒤의 자리다
SCREEN_FILES = {
    ":feature:notes": (
        "HomeScreen.kt",
        "BookmarksScreen.kt",
        "ListPanelScreen.kt",
        "NoteReaderScreen.kt",
        "ConnectBand.kt",
        "NotesViewModel.kt",
        "NotesDestinations.kt",
    ),
    ":feature:connect": (
        "ConnectScreen.kt",
        "ConnectReading.kt",
        "ConnectDialogs.kt",
        "ConnectViewModel.kt",
        "ConnectDestinations.kt",
    ),
    ":feature:settings": (
        "SettingsScreen.kt",
        "RepoSection.kt",
        "ModelCard.kt",
        "ModelDialogs.kt",
        "DisconnectDialog.kt",
        "TokenEdit.kt",
        "TokenMask.kt",
        "ConnectionLink.kt",
        "SettingsViewModel.kt",
        "SettingsDestinations.kt",
    ),
    ":feature:quiz": (
        "QuizOverlay.kt",
        "QuizSetupDialog.kt",
        "QuizGeneratingScreen.kt",
        "QuizSolveScreen.kt",
        "QuizResultScreen.kt",
        "QuizFailedScreen.kt",
        "QuizCtaCard.kt",
        "QuizQuestionUi.kt",
        "QuizViewModel.kt",
    ),
    ":feature:stats": (
        "StatsScreen.kt",
        "StatsHistoryScreen.kt",
        "StatsViewModel.kt",
        "StatsDestinations.kt",
    ),
    # local-notes — 편집기 · 붙여넣기 · 가져오기 그릇 · 지우기 확인(기능 모듈 하나 — brief 정하지 않은 것 11)
    ":feature:editor": (
        "EditorScreen.kt",
        "PasteScreen.kt",
        "EditorHost.kt",
        "EditorViewModel.kt",
        "EditorDestinations.kt",
    ),
}
# 노트에 남으면 안 되는 다른 기능의 파일 이름 머리. 연결 띠(`ConnectBand.kt`)는 모듈 지도에서 노트 몫이다
OTHER_FEATURE_HEADS = ("Connect", "Settings", "Quiz", "Stats", "Editor")
NOTES_KEPT = {"ConnectBand.kt"}

# FM-11 — 정의만 있고 아무도 쓰지 않는 문구 가운데 이유가 있어 두는 것(plan D47). 누가 쓰기 시작하면 시험이 목록에서 지우라고 알린다
UNUSED_KEPT = {
    # DESIGN.md 공통 문구 표의 공통 액션 일곱 가운데 둘 — 디자인 기준 문서가 정한 어휘라 화면이 아직 안 써도 둔다
    "ds_action_confirm",
    "ds_action_retry",
}

# FM-6 — 기능 사이 이동 · 신호가 지나는 타입. 모두 `:core:*` 에 하나씩
SIGNAL_TYPES = (
    "FeatureSignals",
    "ReaderOpened",
    "QuizSetupRequest",
    "openSettings",
    "LastSyncHolder",
    "LastSync",
    "Navigator",
    "NavCommand",
    "MyNoteNavKey",
    "NotesNavKey",
    "SharedTextInbox",
    "SharedText",
)

# FM-11 — 결정 15. 공통 액션 문구는 참조 수와 상관없이 design-system 에 산다
COMMON_ACTION = "ds_action_"
SHARED_STRING_HOMES = (CORE_UI, ":core:design-system")
# 아직 한 모듈이 쓰지만 기능을 다 떼면 둘 이상이 쓰는 문구 — 공용 조각과 같이 `:core:ui` 로 먼저 옮겼다.
# 실제로 둘 이상이 쓰게 되면 시험이 이 목록에서 지우라고 알린다. 연결을 떼며 마지막 셋(`ui_token_*`)이 빠져 지금은 비었다
PLACED_AHEAD = {}
# FM-5 — 셸은 `:app` 에 산다. 결정 12 의 고정 순서 호출(읽는 중 · 모델 확인 대화상자 · 편집기 기능의 대화상자 자리 · 뒤로가기 핸들러 ·
# 목록 패널 · 퀴즈 오버레이 · 상단 바)과 드로어 틀은 `:app` 의 셸 파일 한 곳에서 부른다. 기능 모듈에는 NavDisplay · 드로어 틀이 없다.
# 연결 루트는 local-notes 에서 시작 결과가 아니게 되어(RC-1) 목록에서 빠졌다
SHELL_FILE = "NotesShell.kt"
SHELL_ONLY_CALLS = ("NavDisplay", "ModalNavigationDrawer")
ORDERED_CALLS = (
    "ConnectReadingScreen",
    "EditorHost",
    "ModelConfirmDialog",
    "BackHandler",
    "ListPanelScreen",
    "QuizOverlay",
    "NotesTopBar",
    "ModalNavigationDrawer",
)
# FM-15 — 모듈마다 AGENTS.md 가 있고, 루트 AGENTS.md 의 모듈 지도(그 절의 첫 코드 블록)가 그 모듈 폴더를 든다
ROOT_AGENTS = REPO_ROOT / "AGENTS.md"
MODULE_MAP_HEADING = "## 모듈 지도와 의존 방향"
MIN_MODULES = 18
MULTIBINDING = re.compile(r"@(?:IntoSet|IntoMap|ElementsIntoSet)\b")
ROOT_ROUTE = "NotesRoute"
# FM-11 — 결정 16. `:app` 은 resourcePrefix 를 걸지 않고 셸 문구만 `shell_` 로 시작한다. 셸 문구가 아닌 앱 리소스 이름
APP_NON_SHELL_STRINGS = {"app_name"}
SHELL_PREFIX = "shell_"
STRING_REFERENCE = re.compile(r"\b\w*R\.(?:string|array|plurals)\.(\w+)")
XML_STRING_REFERENCE = re.compile(r"@(?:string|array|plurals)/(\w+)")
# 맨 윗줄 공개 · 모듈 선언 — `private` 는 같은 이름의 파일 안 도우미라 조각이 아니다
DECLARATION = r"^(?:(?:public|internal)\s+)?(?:(?:const|data|enum|sealed|inline|fun)\s+)*(?:fun|val|var|class|interface|object|typealias)\s+(?:<[^>]+>\s+)?"

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


def calls_in(text, name):
    """`name(` 호출 수 — 선언(`fun name(`)과 이름이 더 긴 것(`NotesNavDisplay(`)은 빼고."""
    return len(re.findall(r"(?<![\w.])(?<!fun )" + re.escape(name) + r"\s*\(", text))


def view_model_sources():
    return [path for path in kotlin_main_sources() if path.name.endswith("ViewModel.kt")]


def module_of(path):
    """소스 · 리소스 경로 → 그 모듈 경로(`core/ui/src/…` → `:core:ui`)."""
    parts = path.relative_to(REPO_ROOT).parts
    return ":" + ":".join(parts[: parts.index("src")])


def declared_in(name):
    """`main` 소스에서 이름을 맨 윗줄에 (`private` 가 아니게) 선언한 모듈들. `Receiver.name` 은 그 받는 쪽의 확장으로 찾는다."""
    receiver, _, simple = name.rpartition(".")
    owner = re.escape(receiver) + r"\." if receiver else r"(?:[\w<>?, ]+\.)?"
    pattern = re.compile(DECLARATION + owner + re.escape(simple) + r"(?![\w.])", re.MULTILINE)
    return [module_of(path) for path in kotlin_main_sources() if pattern.search(path.read_text(encoding="utf-8"))]


@functools.cache
def main_files():
    """모든 모듈 `src/main` 아래 파일 — 빌드 폴더는 내려가지 않는다."""
    found = []
    for top in SOURCE_ROOTS:
        for directory, children, files in os.walk(REPO_ROOT / top):
            children[:] = sorted(child for child in children if child not in SKIPPED_DIRS)
            rel = Path(directory).relative_to(REPO_ROOT).parts
            if "src" in rel and rel[rel.index("src") + 1 :][:1] == ("main",):
                found += [Path(directory) / name for name in sorted(files)]
    return tuple(found)


def string_definitions():
    """문구 이름 → 정의한 모듈들(`src/main/res/values/strings.xml`)."""
    defined = {}
    for path in main_files():
        if path.name == "strings.xml" and path.parent.name == "values":
            for node in ET.parse(path).getroot():
                defined.setdefault(node.get("name"), []).append(module_of(path))
    return defined


def string_references():
    """문구 이름 → 그것을 참조하는 모듈들 — `main` 의 Kotlin `R.string.…`(별칭 `UiR` · `DsR` 포함)과 XML `@string/…`."""
    found = {}
    for path in main_files():
        if path.suffix == ".kt":
            names = STRING_REFERENCE.findall(path.read_text(encoding="utf-8"))
        elif path.suffix == ".xml" and path.name != "strings.xml":
            names = XML_STRING_REFERENCE.findall(path.read_text(encoding="utf-8"))
        else:
            continue
        for name in names:
            found.setdefault(name, set()).add(module_of(path))
    return found


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


    def test_FM_5_셸은_app_에_살고_순서를_한_파일이_고정한다(self):
        sources = {path: path.read_text(encoding="utf-8") for path in kotlin_main_sources()}
        app = {path: text for path, text in sources.items() if module_of(path) == ":app"}
        features = {path: text for path, text in sources.items() if module_of(path).startswith(":feature:")}
        # 헛통과 막이 — 셸 쪽에서 호출을 실제로 알아보는지 먼저 본다
        for name in (*SHELL_ONLY_CALLS, *ORDERED_CALLS):
            self.assertGreater(sum(calls_in(text, name) for text in app.values()), 0, f"`:app` 에서 `{name}(` 을 찾지 못했습니다")
        in_features = [
            f"{path.relative_to(REPO_ROOT)}: {name}("
            for path, text in features.items()
            for name in SHELL_ONLY_CALLS
            if calls_in(text, name)
        ]
        self.assertEqual(in_features, [])
        ordered = sorted({path.name for path, text in app.items() for name in ORDERED_CALLS if calls_in(text, name)})
        self.assertEqual(ordered, [SHELL_FILE])
        bound = [str(path.relative_to(REPO_ROOT)) for path, text in {**app, **features}.items() if MULTIBINDING.search(text)]
        self.assertEqual(bound, [])
        self.assertEqual(declared_in(ROOT_ROUTE), [":app"])
        activity = next(text for path, text in app.items() if path.name == "MainActivity.kt")
        self.assertGreater(calls_in(activity, ROOT_ROUTE), 0)

    def test_FM_11_셸_문구는_shell__로_시작한다(self):
        defined = string_definitions()
        app = {name for name, homes in defined.items() if ":app" in homes}
        self.assertTrue(APP_NON_SHELL_STRINGS <= app, "`:app` 문구 파일을 읽지 못했습니다")
        self.assertTrue(any(name.startswith(SHELL_PREFIX) for name in app), "셸 문구가 하나도 없습니다")
        self.assertEqual(sorted(name for name in app - APP_NON_SHELL_STRINGS if not name.startswith(SHELL_PREFIX)), [])
        self.assertEqual(
            sorted(f"{name}: {homes}" for name, homes in defined.items() if name.startswith(SHELL_PREFIX) and homes != [":app"]),
            [],
        )

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
        self.assertTrue(set(FEATURES) <= closure(graph, ":app"), "`:app` 이 기능 모듈을 다 보지 못했습니다")
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

    def test_FM_1_공용_조각은_core_ui_와_core_navigation_에_산다(self):
        wrong = [
            f"{name}: {declared_in(name)}"
            for home, names in SHARED_PIECES.items()
            for name in names
            if declared_in(name) != [home]
        ]
        self.assertEqual(wrong, [])
        manifests = {module_of(path) for path in main_files() if path.name == "AndroidManifest.xml" and BROWSER_QUERY in path.read_text(encoding="utf-8")}
        self.assertEqual(manifests, {CORE_UI}, "외부 브라우저 찾기 `<queries>` 는 `openExternal` 이 사는 모듈에 하나")

    def test_FM_1_화면은_모듈_지도대로_산다(self):
        sources = [path for path in kotlin_main_sources() if module_of(path).startswith(":feature:")]
        homes = {}
        for path in sources:
            homes.setdefault(path.name, set()).add(module_of(path))
        # 헛통과 막이 — 모듈 지도의 화면 파일을 하나도 빠짐없이 찾는지, 기능 모듈을 다 보는지 먼저 본다
        self.assertEqual({module_of(path) for path in sources}, set(FEATURES))
        wrong = [
            f"{name}: {sorted(homes.get(name, ()))} — {home} 에만 있어야 한다"
            for home, names in SCREEN_FILES.items()
            for name in names
            if homes.get(name) != {home}
        ]
        self.assertEqual(wrong, [])
        left = sorted(
            str(path.relative_to(REPO_ROOT))
            for path in sources
            if module_of(path) == ":feature:notes"
            and path.name.startswith(OTHER_FEATURE_HEADS)
            and path.name not in NOTES_KEPT
        )
        self.assertEqual(left, [], "노트에 연결 · 설정 · 퀴즈 · 통계 화면 파일이 남았습니다")

    def test_FM_6_기능_사이_신호_타입은_core_에_있다(self):
        wrong = [f"{name}: {declared_in(name)}" for name in SIGNAL_TYPES if len(declared_in(name)) != 1 or not declared_in(name)[0].startswith(":core:")]
        self.assertEqual(wrong, [])

    def test_FM_11_문구는_참조하는_모듈_자리에_산다(self):
        defined, references = string_definitions(), string_references()
        # 헛통과 막이 — 별칭 참조(`UiR`)와 둘 이상이 쓰는 문구를 실제로 알아보는지 먼저 본다
        self.assertGreaterEqual(len(references.get("ui_app_title", ())), 2, "둘 이상이 쓰는 문구를 알아보지 못했습니다")
        self.assertIn(":app", references.get("app_name", ()), "manifest 의 문구 참조를 읽지 못했습니다")
        # 앞당긴 목록의 헛통과 막이 — 오타 · 지운 문구 · 아무도 안 쓰는 문구는 아래 반복이 보지 못한다(plan D49)
        wrong = [f"{name}: 앞당긴 목록에 있지만 정의가 없다" for name in PLACED_AHEAD if name not in defined]
        wrong += [f"{name}: 앞당긴 목록에 있지만 아무도 쓰지 않는다" for name in PLACED_AHEAD if name in defined and not references.get(name)]
        for name, homes in sorted(defined.items()):
            users = references.get(name, set())
            if name in PLACED_AHEAD:
                if len(users) > 1:
                    wrong.append(f"{name}: 이제 둘 이상이 쓴다 — PLACED_AHEAD 에서 지우세요")
                elif set(homes) != {CORE_UI}:
                    wrong.append(f"{name}: 정의 {homes} — 앞당겨 옮긴 문구는 `:core:ui` 에")
                continue
            if name.startswith(COMMON_ACTION):
                expected = {":core:design-system"}
            elif len(users) == 1:
                expected = users
            elif len(users) > 1:
                expected = set(SHARED_STRING_HOMES)
            else:
                continue
            if not set(homes) <= expected:
                wrong.append(f"{name}: 정의 {homes} · 참조 {sorted(users)}")
        self.assertEqual(wrong, [])

    def test_FM_11_쓰지_않는_문구가_없다(self):
        defined, references = string_definitions(), string_references()
        # 헛통과 막이 — 배열 참조(`R.array`)와 manifest 참조를 실제로 알아보는지 먼저 본다
        self.assertTrue(references.get("quiz_generating_steps"), "배열 문구 참조를 읽지 못했습니다")
        self.assertIn(":app", references.get("app_name", ()), "manifest 의 문구 참조를 읽지 못했습니다")
        unused = sorted(name for name in defined if not references.get(name) and name not in UNUSED_KEPT)
        self.assertEqual(unused, [], "정의만 있고 아무도 쓰지 않는 문구 — 지우거나 이유를 적어 UNUSED_KEPT 에")
        stale = [f"{name}: 정의가 없다" for name in sorted(UNUSED_KEPT) if name not in defined]
        stale += [f"{name}: 이제 쓰인다 — UNUSED_KEPT 에서 지우세요" for name in sorted(UNUSED_KEPT) if references.get(name)]
        self.assertEqual(stale, [])

    def test_FM_11_문구_이름은_모듈을_넘어_겹치지_않는다(self):
        defined = string_definitions()
        self.assertGreaterEqual(len({home for homes in defined.values() for home in homes}), 4, "문구 파일을 다 찾지 못했습니다")
        self.assertEqual({name: homes for name, homes in defined.items() if len(homes) > 1}, {})

    def test_FM_15_모듈마다_AGENTS_md_가_있다(self):
        paths = INCLUDE.findall(SETTINGS.read_text(encoding="utf-8"))
        # 헛통과 막이 — settings 의 include 를 못 읽으면 빈 목록이라 아래가 모두 통과한다
        self.assertGreaterEqual(len(paths), MIN_MODULES, "settings.gradle.kts 의 include 를 다 읽지 못했습니다")
        root = ROOT_AGENTS.read_text(encoding="utf-8")
        section = root.split(MODULE_MAP_HEADING, 1)[1] if MODULE_MAP_HEADING in root else ""
        blocks = section.split("```")
        self.assertGreaterEqual(len(blocks), 3, "루트 AGENTS.md 의 모듈 지도 코드 블록을 찾지 못했습니다")
        module_map = blocks[1]
        missing_file = [path for path in paths if not (module_dir(path) / "AGENTS.md").is_file()]
        missing_map = []
        for path in paths:
            folder = path.strip(":").replace(":", "/")
            if not re.search(rf"(?<![\w/-]){re.escape(folder)}(?![\w-])", module_map):
                missing_map.append(path)
        self.assertEqual(missing_file, [], "AGENTS.md 가 없는 모듈")
        self.assertEqual(missing_map, [], "루트 AGENTS.md 모듈 지도에 없는 모듈")

if __name__ == "__main__":
    unittest.main()
