import math
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
APP_MAIN = REPO_ROOT / "app" / "src" / "main"
RES = APP_MAIN / "res"
SVG_DIR = REPO_ROOT / "labs" / "brand-design" / "app" / "svg"
COLORS_KT = (
    REPO_ROOT / "core" / "design-foundation" / "src" / "main" / "java" / "com" / "hancekim" / "mynote"
    / "core" / "designfoundation" / "token" / "MyNoteColors.kt"
)
MOTION_KT = (
    REPO_ROOT / "core" / "design-system" / "src" / "main" / "java" / "com" / "hancekim" / "mynote"
    / "core" / "designsystem" / "motion" / "Motion.kt"
)

ANDROID = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"
SVG = "{http://www.w3.org/2000/svg}"

SAFE_CENTER = (54.0, 54.0)
SAFE_RADIUS = 33.0
SPLASH_LIMIT_MILLIS = 1000
SPLASH_HOLD_LIMIT_MILLIS = 166
LINEAR = "@android:interpolator/linear"
NIGHT = "#0A0F1C"
ROBOT_MARKS = ("#3DDC84", "M65.3,45.828", "M31,63.928")
LAYERS = {
    "background": "launcher-background.svg",
    "foreground": "launcher-foreground.svg",
    "monochrome": "launcher-monochrome.svg",
}


def android(element, name, default=None):
    return element.get(ANDROID + name, default)


def resource_files(ref):
    """`@type/name` 이 가리키는 res 파일들(한정자 폴더 포함)."""
    kind, name = ref.lstrip("@").split("/")
    return sorted(
        path
        for folder in RES.iterdir()
        if folder.is_dir() and folder.name.split("-")[0] == kind
        for path in folder.iterdir()
        if path.stem == name
    )


def resource_root(ref):
    files = resource_files(ref)
    if len(files) != 1:
        raise AssertionError(f"{ref} 파일이 하나가 아니다: {files}")
    return ET.parse(files[0]).getroot()


def colors():
    return {
        color.get("name"): normalize_color(color.text)
        for color in ET.parse(RES / "values" / "colors.xml").getroot().iter("color")
    }


def normalize_color(value):
    value = value.strip().upper()
    if value.startswith("@COLOR/"):
        return colors()[value.split("/")[1].lower()]
    if len(value) == 9 and value.startswith("#FF"):
        return "#" + value[3:]
    return value


def token(name):
    found = re.search(rf"\b{name} = Color\(0xFF([0-9A-Fa-f]{{6}})\)", COLORS_KT.read_text(encoding="utf-8"))
    return "#" + found.group(1).upper()


def theme_items(style_name):
    for path in RES.glob("values*/*.xml"):
        for style in ET.parse(path).getroot().iter("style"):
            if style.get("name") == style_name:
                yield path.parent.name, {item.get("name"): item.text.strip() for item in style.iter("item")}


def app_theme():
    manifest = ET.parse(APP_MAIN / "AndroidManifest.xml").getroot()
    name = android(manifest.find("application"), "theme").split("/")[1]
    themes = list(theme_items(name))
    if [folder for folder, _ in themes] != ["values"]:
        raise AssertionError(f"{name} 는 values 에 하나만 있어야 한다: {themes}")
    return themes[0][1]


def inline(element, attribute):
    for attr in element.findall(AAPT + "attr"):
        if attr.get("name") == attribute:
            return attr[0]
    return None


def vector_parts(vector):
    """VectorDrawable 의 path 를 이름별로 — 경로 글자 · 선 · 칠."""
    parts = {}
    for path in vector.iter("path"):
        gradient = inline(path, "android:fillColor")
        if gradient is not None:
            fill = {
                "x1": float(android(gradient, "startX")),
                "y1": float(android(gradient, "startY")),
                "x2": float(android(gradient, "endX")),
                "y2": float(android(gradient, "endY")),
                "stops": [normalize_color(android(gradient, "startColor")), normalize_color(android(gradient, "endColor"))],
            }
        elif android(path, "fillColor"):
            fill = normalize_color(android(path, "fillColor"))
        else:
            fill = None
        stroke = android(path, "strokeColor")
        parts[android(path, "name")] = {
            "d": " ".join(android(path, "pathData").split()),
            "stroke": normalize_color(stroke) if stroke else None,
            "stroke_width": float(android(path, "strokeWidth", "0")) if stroke else None,
            "fill": fill,
            "fill_alpha": float(android(path, "fillAlpha", "1")),
        }
    return parts


def svg_parts(name):
    """디자인 원본 SVG 의 path 를 id 별로 — vector_parts 와 같은 모양."""
    root = ET.parse(SVG_DIR / name).getroot()
    gradients = {}
    for gradient in root.iter(SVG + "linearGradient"):
        stops = sorted(gradient.iter(SVG + "stop"), key=lambda stop: float(stop.get("offset")))
        gradients[gradient.get("id")] = {
            "x1": float(gradient.get("x1")),
            "y1": float(gradient.get("y1")),
            "x2": float(gradient.get("x2")),
            "y2": float(gradient.get("y2")),
            "stops": [normalize_color(stop.get("stop-color")) for stop in stops],
        }
    parts = {}
    for path in root.iter(SVG + "path"):
        fill = path.get("fill", "#000000")
        if fill.startswith("url(#"):
            fill = gradients[fill[5:-1]]
        elif fill == "none":
            fill = None
        else:
            fill = normalize_color(fill)
        stroke = path.get("stroke")
        parts[path.get("id")] = {
            "d": " ".join(path.get("d").split()),
            "stroke": normalize_color(stroke) if stroke else None,
            "stroke_width": float(path.get("stroke-width")) if stroke else None,
            "fill": fill,
            "fill_alpha": float(path.get("fill-opacity", "1")),
        }
    return parts


def path_commands(d):
    """절대 좌표 M · L · H · V · A · Z 만 읽는다 — 디자인 원본이 쓰는 명령."""
    tokens = re.findall(r"[A-Za-z]|-?(?:\d+\.?\d*|\.\d+)(?:[eE]-?\d+)?", d)
    commands, index = [], 0
    arity = {"M": 2, "L": 2, "H": 1, "V": 1, "A": 7, "Z": 0}
    while index < len(tokens):
        letter = tokens[index]
        if letter not in arity:
            raise ValueError(f"읽지 못하는 명령 {letter!r}: {d}")
        count = arity[letter]
        commands.append((letter, [float(value) for value in tokens[index + 1:index + 1 + count]]))
        index += 1 + count
    return commands


def endpoints(d):
    points, x, y = [], 0.0, 0.0
    for letter, values in path_commands(d):
        if letter in ("M", "L"):
            x, y = values
        elif letter == "H":
            x = values[0]
        elif letter == "V":
            y = values[0]
        elif letter == "A":
            x, y = values[5:]
        points.append((x, y))
    return points


def arcs(d):
    """경로의 원호를 (중심 x, 중심 y, 반지름, 시작각, 쓸어 간 각) 으로 — SVG 끝점 → 중심 변환."""
    found, x, y = [], 0.0, 0.0
    for letter, values in path_commands(d):
        if letter in ("M", "L"):
            x, y = values
        elif letter == "H":
            x = values[0]
        elif letter == "V":
            y = values[0]
        elif letter == "A":
            rx, ry, _, large, sweep, x2, y2 = values
            if rx != ry:
                raise ValueError(f"원이 아닌 원호: {d}")
            hx, hy = (x - x2) / 2, (y - y2) / 2
            r = max(rx, math.hypot(hx, hy))
            factor = math.sqrt(max(0.0, (r * r - hx * hx - hy * hy) / (hx * hx + hy * hy)))
            if large == sweep:
                factor = -factor
            cx = factor * hy + (x + x2) / 2
            cy = -factor * hx + (y + y2) / 2
            start = math.atan2(y - cy, x - cx)
            span = math.atan2(y2 - cy, x2 - cx) - start
            if sweep and span < 0:
                span += math.tau
            if not sweep and span > 0:
                span -= math.tau
            found.append((cx, cy, r, start, span))
            x, y = x2, y2
    return found


def farthest_on_arcs(part):
    """칠 · 선 바깥 끝을 넣어 원호 위 점들의 안전 원 중심 거리 — (가장 먼 거리, 꼭대기 점 거리)."""
    reach = part["stroke_width"] / 2 if part["stroke"] else 0.0
    far, top = 0.0, None
    for cx, cy, r, start, span in arcs(part["d"]):
        points = [
            (cx + (r + reach) * math.cos(start + span * step / 720), cy + (r + reach) * math.sin(start + span * step / 720))
            for step in range(721)
        ]
        far = max(far, *(math.dist(point, SAFE_CENTER) for point in points))
        highest = min(points, key=lambda point: point[1])
        if top is None or highest[1] < top[1]:
            top = highest
    return far, math.dist(top, SAFE_CENTER)


def launcher_icons():
    application = ET.parse(APP_MAIN / "AndroidManifest.xml").getroot().find("application")
    return [android(application, "icon"), android(application, "roundIcon")]


def adaptive_layers(ref):
    root = resource_root(ref)
    if root.tag != "adaptive-icon":
        raise AssertionError(f"{ref} 가 adaptive-icon 이 아니다: {root.tag}")
    return {layer: android(root.find(layer), "drawable") for layer in LAYERS if root.find(layer) is not None}


def animators(animated_vector):
    """AVD 의 움직임을 (대상 이름, 움직임) 으로 — 값 두 개 꼴과 keyframe 꼴을 같은 모양으로 편다."""
    for target in animated_vector.iter("target"):
        animation = inline(target, "android:animation")
        for animator in animation.iter("objectAnimator"):
            offset = int(android(animator, "startOffset", "0"))
            duration = int(android(animator, "duration"))
            holders = animator.findall("propertyValuesHolder")
            if not holders:
                yield android(target, "name"), {
                    "property": android(animator, "propertyName"),
                    "values": [float(android(animator, "valueFrom")), float(android(animator, "valueTo"))],
                    "hold_millis": offset,
                    "end_millis": offset + duration,
                    "interpolator": android(animator, "interpolator"),
                    "repeat": android(animator, "repeatCount", "0"),
                }
                continue
            for holder in holders:
                frames = [(float(android(key, "fraction")), float(android(key, "value")), android(key, "interpolator"))
                          for key in holder.findall("keyframe")]
                held = [fraction for fraction, value, _ in frames if value == frames[0][1]]
                yield android(target, "name"), {
                    "property": android(holder, "propertyName"),
                    "values": [value for _, value, _ in frames],
                    "fractions": [fraction for fraction, _, _ in frames],
                    "hold_millis": offset + round(max(held) * duration),
                    "end_millis": offset + duration,
                    "interpolator": frames[-1][2],
                    "timing": android(animator, "interpolator"),
                    "repeat": android(animator, "repeatCount", "0"),
                }


def cubic(text):
    return [float(value.rstrip("f")) for value in text]


class BrandResourcesTest(unittest.TestCase):
    def assertSameParts(self, vector_ref, svg_name):
        drawn = vector_parts(resource_root(vector_ref))
        source = svg_parts(svg_name)
        self.assertEqual(sorted(drawn), sorted(source), f"{vector_ref} ⇄ {svg_name} 이름")
        for name, part in source.items():
            with self.subTest(drawable=vector_ref, part=name):
                mine = drawn[name]
                self.assertEqual(mine["d"], part["d"])
                self.assertEqual(mine["stroke"], part["stroke"])
                self.assertEqual(mine["stroke_width"], part["stroke_width"])
                self.assertEqual(mine["fill"], part["fill"])
                self.assertEqual(mine["fill_alpha"], part["fill_alpha"])
        return drawn

    def test_BR_1_런처_아이콘은_세_층이고_monochrome_은_전용_그림이다(self):
        """BR-1 런처 아이콘은 세 층이고 monochrome 은 전용 그림이다"""
        self.assertEqual(launcher_icons(), ["@mipmap/ic_launcher", "@mipmap/ic_launcher_round"])
        for ref in launcher_icons():
            with self.subTest(icon=ref):
                self.assertEqual([path.parent.name for path in resource_files(ref)], ["mipmap-anydpi"])
                layers = adaptive_layers(ref)
                self.assertEqual(sorted(layers), sorted(LAYERS))
                self.assertNotEqual(layers["monochrome"], layers["foreground"])
                for layer, drawable in layers.items():
                    drawn = self.assertSameParts(drawable, LAYERS[layer])
                    if layer == "foreground":
                        self.assertEqual(drawn["frame"]["stroke"], token("accent"))

    def test_BR_1_아치_꼭대기가_안전_원_안이다(self):
        """BR-1 아치 꼭대기가 안전 원 안이다"""
        for ref in launcher_icons():
            for layer in ("foreground", "monochrome"):
                for name, part in vector_parts(resource_root(adaptive_layers(ref)[layer])).items():
                    with self.subTest(icon=ref, layer=layer, part=name):
                        self.assertTrue(arcs(part["d"]))
                        far, top = farthest_on_arcs(part)
                        self.assertLess(top, SAFE_RADIUS)
                        self.assertLess(far, SAFE_RADIUS)
        frame = vector_parts(resource_root(adaptive_layers(launcher_icons()[0])["foreground"]))["frame"]
        self.assertEqual([round(value, 1) for value in farthest_on_arcs(frame)], [30.6, 27.0])

    def test_BR_2_런처_리소스에_기본_로봇_아이콘이_없다(self):
        """BR-2 런처 리소스에 기본 로봇 아이콘이 없다"""
        for path in sorted(RES.rglob("*")):
            if not path.is_file():
                continue
            with self.subTest(path=path.relative_to(REPO_ROOT).as_posix()):
                self.assertFalse(path.name.startswith("ic_launcher") and path.suffix in (".webp", ".png"))
                if path.suffix == ".xml":
                    text = path.read_text(encoding="utf-8").upper()
                    for mark in ROBOT_MARKS:
                        self.assertNotIn(mark.upper(), text)
        self.assertEqual(sorted(folder.name for folder in RES.glob("mipmap*")), ["mipmap-anydpi"])

    def test_BR_4_테마_창_바탕은_앱_바탕색이다(self):
        """BR-4 테마 창 바탕은 앱 바탕색이다"""
        theme = app_theme()
        self.assertEqual(theme["android:windowBackground"], "@color/window_background")
        self.assertEqual(normalize_color(theme["android:windowBackground"]), token("bg"))
        self.assertEqual(theme["android:windowSplashScreenBackground"], "@color/brand_night")
        self.assertEqual(normalize_color(theme["android:windowSplashScreenBackground"]), NIGHT)
        self.assertNotIn("android:windowSplashScreenIconBackgroundColor", theme)
        self.assertNotIn("android:windowSplashScreenBehavior", theme)

    def test_BR_5_스플래시_움직임은_1000ms_이하이고_테마_길이와_같다(self):
        """BR-5 스플래시 움직임은 1000ms 이하이고 테마 길이와 같다"""
        theme = app_theme()
        icon = resource_root(theme["android:windowSplashScreenAnimatedIcon"])
        self.assertEqual(icon.tag, "animated-vector")
        vector = inline(icon, "android:drawable")
        drawn = self.assertSameParts(theme["android:windowSplashScreenAnimatedIcon"], "splash-icon.svg")
        self.assertEqual(drawn["frame"]["stroke"], token("accent"))

        groups = {android(group, "name"): group for group in vector.iter("group")}
        paths = {android(path, "name"): path for path in vector.iter("path")}
        ends, holds = [], []
        mine = re.search(r"MyNoteEasing: Easing = CubicBezierEasing\(([^)]*)\)", MOTION_KT.read_text(encoding="utf-8"))
        for target, motion in animators(icon):
            with self.subTest(target=target, property=motion["property"]):
                self.assertEqual(motion["repeat"], "0")
                element = groups.get(target, paths.get(target))
                self.assertEqual(float(android(element, motion["property"], "1")), motion["values"][-1])
                self.assertNotEqual(motion["values"][0], motion["values"][-1])
                if "fractions" in motion:
                    self.assertEqual(motion["fractions"][0], 0)
                    self.assertEqual(motion["fractions"][-1], 1)
                    # 빠지면 기본 AccelerateDecelerate 가 keyframe 시각과 곡선을 함께 휜다
                    self.assertEqual(motion["timing"], LINEAR)
                easing = resource_root(motion["interpolator"])
                control = [android(easing, key) for key in ("controlX1", "controlY1", "controlX2", "controlY2")]
                self.assertEqual(cubic(control), cubic(value.strip() for value in mine.group(1).split(",")))
                ends.append(motion["end_millis"])
                holds.append(motion["hold_millis"])
        self.assertEqual(sorted(groups), ["lamp_group"])
        self.assertTrue(ends)
        self.assertLessEqual(max(ends), SPLASH_LIMIT_MILLIS)
        self.assertEqual(max(ends), int(theme["android:windowSplashScreenAnimationDuration"]))
        self.assertEqual(len(set(holds)), 1)
        self.assertLessEqual(holds[0], SPLASH_HOLD_LIMIT_MILLIS)

        xs, ys = zip(*endpoints(drawn["lamp"]["d"]))
        group = groups["lamp_group"]
        self.assertAlmostEqual(float(android(group, "pivotX")), (min(xs) + max(xs)) / 2, places=3)
        self.assertAlmostEqual(float(android(group, "pivotY")), max(ys), places=3)


if __name__ == "__main__":
    unittest.main()
