"""워드마크 · 락업 · 에셋 파일. 모든 함수는 (svg 조각, bbox(x, y, w, h)) 를 돌려준다."""
from brand import *
from icons import *

class Art:
    def __init__(self, body, x, y, w, h, defs=''):
        self.body, self.x, self.y, self.w, self.h, self.defs = body, x, y, w, h, defs
    def at(self, x, y, s=1.0):
        """bbox 왼쪽 위를 (x, y) 에 놓는다."""
        return f'<g transform="translate({f2(x - self.x * s)} {f2(y - self.y * s)}) scale({s:.5f})">{self.body}</g>'
    def svg(self, pad=0, bg=None, width=None, extra_defs=''):
        vb = f'{f2(self.x - pad)} {f2(self.y - pad)} {f2(self.w + 2 * pad)} {f2(self.h + 2 * pad)}'
        wa = f' width="{width}"' if width else ''
        bgr = f'<rect x="{f2(self.x - pad)}" y="{f2(self.y - pad)}" width="{f2(self.w + 2 * pad)}" height="{f2(self.h + 2 * pad)}" fill="{bg}"/>' if bg else ''
        defs = self.defs + extra_defs
        return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}"{wa}>' + (f'<defs>{defs}</defs>' if defs else '') + bgr + self.body + '</svg>'

def recolor(g, fill):
    return g.replace('fill="#000"', f'fill="{fill}"')

# ───────── A ─────────
def a_wordmark(color=True, lit=True, frame=None, pane_fill=None):
    d, pane, w = a_wordmark_parts()
    d = d.replace('-96', '-90')
    fr = frame or (A['frame'] if color else '#000')
    defs = ''
    if color:
        gid = uid('aw')
        defs = (f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="0" y1="-72" x2="0" y2="0">'
                f'<stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="{A["light_bot"]}"/></linearGradient>')
        pf = f'url(#{gid})'
    else:
        pf = pane_fill or fr
    body = f'<path d="{d}" fill="none" stroke="{fr}" stroke-width="{T}"/>' + (f'<path d="{pane}" fill="{pf}"/>' if lit else '')
    return Art(body, 0, -90, w, 114, defs)
def a_symbol(color=True, frame=None):
    body, defs = a_windows(A_MARK_XH, mono=not color, frame=frame or (None if color else '#000'))
    return Art(body, 0, -A_MARK_XH, 76.1, A_MARK_XH, defs)
def a_korean(size=40, weight=300, fill=None):
    g, w = set_text('gothic300' if weight == 300 else 'gothic500', '공부 기록', size, tracking=40)
    return Art(recolor(g, fill or A['text']), 0, -size * 0.86, w, size * 1.04)
def a_lockup_h(on_dark=True):
    sym = a_symbol(); wm = a_wordmark(lit=False, frame=A['text'] if on_dark else A['night'])
    s = 72 / A_MARK_XH  # 창 높이 = 워드마크 엑스하이트+α
    sw = 76.1 * s
    body = sym.at(0, -72 * 1.0 - 0, s) if False else f'<g transform="translate(0 0) scale({s:.5f})">{sym.body}</g>'
    body += wm.at(sw + 34, -90)
    return Art(body, 0, -90, sw + 34 + wm.w, 114, sym.defs + wm.defs)
def a_lockup_v(on_dark=True):
    sym = a_symbol(); wm = a_wordmark(lit=False, frame=A['text'] if on_dark else A['night'])
    ko = a_korean(30, fill=A['text2'] if on_dark else '#4A5568')
    W = wm.w; ss = 1.25
    sx = (W - 76.1 * ss) / 2
    body = f'<g transform="translate({f2(sx)} {f2(A_MARK_XH * ss)}) scale({ss})">{sym.body}</g>'
    y = A_MARK_XH * ss + 44
    body += wm.at(0, y)
    y2 = y + 114 + 30
    body += ko.at((W - ko.w) / 2, y2)
    return Art(body, 0, 0, W, y2 + ko.h, sym.defs + wm.defs)

# ───────── B ─────────
B_WM_SIZE = 100
def b_wordmark(on_dark=True, band=True):
    g1, w1 = set_text('archivo', 'My', B_WM_SIZE, tracking=-12, kern={('M', 'y'): -18})
    gap = 26 if band else 9
    g2, w2 = set_text('archivo', 'Note', B_WM_SIZE, tracking=-12, x=w1 + gap, kern={('N', 'o'): -14, ('t', 'e'): -6})
    my = B['paper'] if on_dark else B['ink']
    body = ''
    x0, x1 = w1 + gap - 22, w1 + gap + w2 + 20
    if band:
        body += f'<path d="{b_band(x0, x1, -66, 9, k=26)}" fill="{B["hi"]}"/>'
        note = B['ink']
    else:
        note = my
    body += recolor(g1, my) + recolor(g2, note)
    return Art(body, -2, -78, x1 + 4, 104)
def b_symbol(mode='color', c=None):
    return Art(b_mark(mode, c=c), 4, 10, 92, 84)
def b_korean(size=40, fill=None, band=False):
    g, w = set_text('bhs', '공부 기록', size, tracking=-10)
    body = ''
    if band:
        body += f'<path d="{b_band(-size*0.22, w + size*0.24, -size*0.94, size*0.18, k=size*0.3)}" fill="{B["hi"]}"/>'
    return Art(body + recolor(g, fill or B['paper']), 0, -size * 0.86, w, size * 1.04)
def b_lockup_h(on_dark=True):
    sym = b_symbol(); wm = b_wordmark(on_dark, band=False)
    s = 1.12
    body = sym.at(0, -78 + 2, s) + wm.at(sym.w * s + 26, -78)
    return Art(body, 0, -78, sym.w * s + 26 + wm.w, 104)
def b_lockup_v(on_dark=True):
    sym = b_symbol(); wm = b_wordmark(on_dark, band=False)
    ko = b_korean(34, fill=B['paper'] if on_dark else B['ink'])
    W = wm.w; s = 2.0
    body = sym.at((W - sym.w * s) / 2, 0, s)
    y = sym.h * s + 30
    body += wm.at(0, y)
    y2 = y + wm.h + 22
    body += ko.at((W - ko.w) / 2, y2)
    return Art(body, 0, 0, W, y2 + ko.h)

# ───────── C ─────────
def c_wordmark(fill=None, dot=None, size=100):
    fill = fill or C['paper']; dot = dot or C['red']
    g, w = set_text('hahmlet', 'MyNote', size, tracking=6, kern={('M', 'y'): -34, ('N', 'o'): -12, ('t', 'e'): 4})
    # o 의 속공간 한가운데에 回 의 안 사각 — 인주
    f = G['hahmlet']; sc = size / 1000
    ox = 0
    for ch, nxt in zip('MyNote', 'yNote '):
        if ch == 'o': break
        ox += f['glyphs'][ch]['adv'] * sc + 6 * size / 1000
        ox += {('M', 'y'): -34, ('N', 'o'): -12}.get((ch, nxt), 0) * size / 1000
    cx = ox + f['glyphs']['o']['adv'] * sc / 2 - 0.2; cy = -f['xh'] * sc / 2 - 0.6
    q = size * 0.072
    body = recolor(g, fill) + f'<rect x="{f2(cx - q/2)}" y="{f2(cy - q/2)}" width="{f2(q)}" height="{f2(q)}" fill="{dot}"/>'
    return Art(body, -4, -78, w + 8, 104)
def c_symbol(red=None):
    return Art(c_mark(red=red), 5, 5, 90, 90)
def c_korean(size=40, fill=None, weight=600):
    g, w = set_text('hahmlet' if weight == 600 else 'hahmlet500', '공부 기록', size, tracking=-10)
    return Art(recolor(g, fill or C['paper']), 0, -size * 0.86, w, size * 1.04)
def c_korean_vertical(size=40, fill=None):
    """세로쓰기 — 글자를 위에서 아래로."""
    f = G['hahmlet']; sc = size / 1000; parts = []; y = 0
    for ch in '공부기록':
        g = f['glyphs'][ch]
        parts.append(f'<path transform="translate(0 {f2(y + size * 0.86)}) scale({sc:.5f})" d="{g["d"]}"/>')
        y += size * (1.08 if ch != '부' else 1.42)
    return Art(f'<g fill="{fill or C["paper"]}">' + ''.join(parts) + '</g>', 0, 0, size * 0.93, y)
def c_lockup_h(fill=None):
    wm = c_wordmark(fill); sym = c_symbol()
    s = 0.86  # 도장 = 대문자 높이보다 조금 크게, 기준선 아래로 살짝 내려 찍는다(낙관)
    body = wm.at(0, -78) + sym.at(wm.w + 22, -64, s)
    return Art(body, -4, -78, wm.w + 22 + sym.w * s, 104)
def c_lockup_v(fill=None):
    wm = c_wordmark(fill); sym = c_symbol(); ko = c_korean(30, fill=fill or C['paper'], weight=500)
    W = wm.w; s = 1.9
    body = sym.at((W - sym.w * s) / 2, 0, s)
    y = sym.h * s + 40
    body += wm.at(0, y)
    y2 = y + wm.h + 18
    body += ko.at((W - ko.w) / 2 - 4, y2)
    return Art(body, -4, 0, W, y2 + ko.h)
def c_lockup_seal_vertical(fill=None):
    """낙관 — 세로로 쓴 '공부 기록' 아래 도장."""
    ko = c_korean_vertical(56, fill=fill); sym = c_symbol()
    s = 0.62
    body = ko.at(0, 0) + sym.at((ko.w - sym.w * s) / 2, ko.h + 18, s)
    return Art(body, 0, 0, ko.w, ko.h + 18 + sym.h * s)
