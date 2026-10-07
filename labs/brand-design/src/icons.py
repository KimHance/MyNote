"""안드로이드 adaptive icon 레이어 (viewBox 0 0 108 108). 전경은 가운데 지름 66 안."""
from brand import *

def _place(inner, size, cx=54, cy=54, vb=100):
    s = size / vb
    return f'<g transform="translate({f2(cx - size/2)} {f2(cy - size/2)}) scale({s:.5f})">{inner}</g>'

# ── 배경 레이어 ──
def icon_bg(d):
    gid = uid('ib')
    if d == 'a':
        return (f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#13203A"/><stop offset="1" stop-color="#070B15"/></linearGradient></defs>'
                f'<rect width="108" height="108" fill="url(#{gid})"/>')
    if d == 'b':
        return f'<rect width="108" height="108" fill="{B["ink"]}"/>'
    if d == 'c':
        return f'<rect width="108" height="108" fill="{C["ink"]}"/>'

# ── 전경 레이어 ── (안전 영역: 중심 54,54 반지름 33)
A_ICON = dict(top=30, width=56, base=112)  # 창 꼭대기 y=30, 기둥은 마스크 아래로 흘러 나간다
def icon_fg(d, mono=False, color=None):
    if d == 'a':
        s = A_ICON['width'] / 76.1; ox = 54 - A_ICON['width'] / 2
        xh = (A_ICON['base'] - A_ICON['top']) / s
        body, defs = a_windows(xh, mono=mono, frame=color if mono else None)
        return (f'<defs>{defs}</defs>' if defs else '') + f'<g transform="translate({f2(ox)} {A_ICON["base"]}) scale({s:.5f})">{body}</g>'
    if d == 'b':
        inner = b_mark('mono', c=color or '#FFFFFF') if mono else b_mark()
        return _place(inner, 58, 54, 54)
    if d == 'c':
        inner = c_mark(red=color or '#FFFFFF') if mono else c_mark()
        return _place(inner, 52, 54, 54)

def icon_full(d, mono=False):
    return icon_bg(d) + icon_fg(d)

# ── 마스크 모양 (보이는 72 영역 기준, 108 좌표) ──
def mask_path(shape):
    if shape == 'square':
        return 'M18 18 H90 V90 H18 Z'
    if shape == 'circle':
        return 'M54 18 A36 36 0 1 1 53.99 18 Z'
    if shape == 'rounded':
        return 'M30 18 H78 Q90 18 90 30 V78 Q90 90 78 90 H30 Q18 90 18 78 V30 Q18 18 30 18 Z'
    if shape == 'squircle':
        # 초타원 근사(n≈4) — 큐빅 네 개
        return ('M54 18 C80.5 18 90 27.5 90 54 C90 80.5 80.5 90 54 90 C27.5 90 18 80.5 18 54 C18 27.5 27.5 18 54 18 Z')
    if shape == 'teardrop':
        return 'M54 18 H78 Q90 18 90 30 V54 A36 36 0 1 1 54 18 Z'

def masked_icon(d, shape='circle', size=48, mono=None):
    """보이는 72dp 영역만 잘라 size px 로."""
    cid = uid('mk')
    body = icon_full(d) if not mono else mono
    return (f'<svg viewBox="18 18 72 72" width="{size}" height="{size}" aria-hidden="true">'
            f'<defs><clipPath id="{cid}"><path d="{mask_path(shape)}"/></clipPath></defs>'
            f'<g clip-path="url(#{cid})">{body}</g></svg>')

def themed_icon(d, shape='circle', size=48, bg='#D3E3FD', fg='#0B2E66'):
    body = f'<rect width="108" height="108" fill="{bg}"/>' + icon_fg(d, mono=True, color=fg)
    return masked_icon(d, shape, size, mono=body)

# ── 가짜 앱 아이콘 (다른 앱들) ──
FAKES = [
    ('날씨', '#3B82F6', '<circle cx="47" cy="50" r="13" fill="#FDE68A"/><path d="M40 66 a10 10 0 0 1 4-19 a13 13 0 0 1 25 3 a8 8 0 0 1 1 16 Z" fill="#fff"/>'),
    ('지도', '#E8F0E3', '<path d="M54 30 a14 14 0 0 1 14 14 c0 11 -14 26 -14 26 s-14 -15 -14 -26 a14 14 0 0 1 14 -14Z" fill="#16A34A"/><circle cx="54" cy="44" r="5" fill="#E8F0E3"/>'),
    ('카메라', '#2B2F36', '<rect x="33" y="40" width="42" height="30" rx="7" fill="#E5E7EB"/><circle cx="54" cy="55" r="9" fill="#2B2F36"/><circle cx="54" cy="55" r="5" fill="#93C5FD"/>'),
    ('메일', '#F05A3C', '<rect x="34" y="39" width="40" height="30" rx="4" fill="#fff"/><path d="M35 41 L54 56 L73 41" fill="none" stroke="#F05A3C" stroke-width="3.5" stroke-linejoin="round"/>'),
    ('음악', '#7C3AED', '<path d="M47 66 V40 L66 36 V60" fill="none" stroke="#fff" stroke-width="4" stroke-linejoin="round"/><circle cx="43" cy="66" r="5.5" fill="#fff"/><circle cx="62" cy="61" r="5.5" fill="#fff"/>'),
    ('캘린더', '#FFFFFF', '<rect x="34" y="36" width="40" height="38" rx="6" fill="#EEF2FF"/><rect x="34" y="36" width="40" height="10" rx="5" fill="#4F46E5"/><text x="54" y="69" font-size="19" font-weight="700" text-anchor="middle" fill="#312E81" font-family="Noto Sans KR, sans-serif">7</text>'),
    ('은행', '#0F766E', '<rect x="34" y="42" width="40" height="26" rx="5" fill="#CCFBF1"/><rect x="34" y="48" width="40" height="5" fill="#0F766E"/><rect x="40" y="58" width="10" height="4" rx="2" fill="#0F766E"/>'),
    ('사진', '#FFFFFF', '<path d="M54 54 V34 a10 10 0 0 1 0 20Z" fill="#F43F5E"/><path d="M54 54 H74 a10 10 0 0 1 -20 0Z" fill="#F59E0B"/><path d="M54 54 V74 a10 10 0 0 1 0 -20Z" fill="#22C55E"/><path d="M54 54 H34 a10 10 0 0 1 20 0Z" fill="#3B82F6"/>'),
    ('계산기', '#F97316', '<path d="M44 46 h12 M50 40 v12 M60 46 h10 M43 62 l8 8 M51 62 l-8 8 M60 64 h10 M60 70 h10" stroke="#fff" stroke-width="3.6" stroke-linecap="round"/>'),
    ('메시지', '#22C55E', '<path d="M36 50 c0-9 8-15 18-15 s18 6 18 15 -8 15 -18 15 c-3 0-5 0-8-1 l-8 5 2-7 c-3-3-4-7-4-12Z" fill="#fff"/>'),
    ('시계', '#111827', '<circle cx="54" cy="54" r="19" fill="none" stroke="#fff" stroke-width="3.5"/><path d="M54 43 V54 L62 59" fill="none" stroke="#FBBF24" stroke-width="3.5" stroke-linecap="round"/>'),
    ('파일', '#FACC15', '<path d="M36 44 a4 4 0 0 1 4-4 h10 l4 5 h14 a4 4 0 0 1 4 4 v17 a4 4 0 0 1 -4 4 H40 a4 4 0 0 1 -4 -4Z" fill="#fff"/>'),
]
def fake_icon(i, shape='circle', size=48):
    name, bg, glyph = FAKES[i]
    body = f'<rect width="108" height="108" fill="{bg}"/>{glyph}'
    return masked_icon(None, shape, size, mono=body), name
FAKES_MONO = [
    '<circle cx="54" cy="54" r="15" fill="none" stroke="F" stroke-width="4"/><path d="M54 33v-5M54 80v-5M33 54h-5M80 54h-5" stroke="F" stroke-width="4" stroke-linecap="round"/>',
    '<path d="M54 30 a14 14 0 0 1 14 14 c0 11 -14 26 -14 26 s-14 -15 -14 -26 a14 14 0 0 1 14 -14Z" fill="F"/><circle cx="54" cy="44" r="5" fill="B"/>',
    '<rect x="33" y="40" width="42" height="30" rx="7" fill="F"/><circle cx="54" cy="55" r="8" fill="B"/>',
    '<rect x="34" y="39" width="40" height="30" rx="4" fill="F"/><path d="M36 42 L54 56 L72 42" fill="none" stroke="B" stroke-width="3.5" stroke-linejoin="round"/>',
    '<path d="M47 66 V40 L66 36 V60" fill="none" stroke="F" stroke-width="4" stroke-linejoin="round"/><circle cx="43" cy="66" r="5.5" fill="F"/><circle cx="62" cy="61" r="5.5" fill="F"/>',
    '<rect x="34" y="36" width="40" height="38" rx="6" fill="none" stroke="F" stroke-width="4"/><path d="M34 47h40" stroke="F" stroke-width="4"/><rect x="42" y="54" width="8" height="8" rx="1.5" fill="F"/>',
    '<rect x="34" y="42" width="40" height="26" rx="5" fill="F"/><rect x="34" y="48" width="40" height="5" fill="B"/>',
    '<path d="M54 54 V34 a10 10 0 0 1 0 20Z M54 54 H74 a10 10 0 0 1 -20 0Z M54 54 V74 a10 10 0 0 1 0 -20Z M54 54 H34 a10 10 0 0 1 20 0Z" fill="F"/>',
    '<path d="M44 46 h12 M50 40 v12 M60 46 h10 M43 62 l8 8 M51 62 l-8 8 M60 64 h10 M60 70 h10" stroke="F" stroke-width="3.6" stroke-linecap="round"/>',
    '<path d="M36 50 c0-9 8-15 18-15 s18 6 18 15 -8 15 -18 15 c-3 0-5 0-8-1 l-8 5 2-7 c-3-3-4-7-4-12Z" fill="F"/>',
    '<circle cx="54" cy="54" r="19" fill="none" stroke="F" stroke-width="3.5"/><path d="M54 43 V54 L62 59" fill="none" stroke="F" stroke-width="3.5" stroke-linecap="round"/>',
    '<path d="M36 44 a4 4 0 0 1 4-4 h10 l4 5 h14 a4 4 0 0 1 4 4 v17 a4 4 0 0 1 -4 4 H40 a4 4 0 0 1 -4 -4Z" fill="F"/>',
]
def fake_themed(i, shape='circle', size=48, bg='#D3E3FD', fg='#0B2E66'):
    name = FAKES[i][0]
    g = FAKES_MONO[i].replace('"F"', f'"{fg}"').replace('"B"', f'"{bg}"')
    body = f'<rect width="108" height="108" fill="{bg}"/>{g}'
    return masked_icon(None, shape, size, mono=body), name
