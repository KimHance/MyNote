"""MyNote 브랜드 세 방향의 기하. 모든 SVG 조각은 여기서 나온다."""
import json, math, os, itertools
HERE = os.path.dirname(os.path.abspath(__file__))
G = json.load(open(os.path.join(HERE, 'glyphs.json')))
_ids = itertools.count(1)
def uid(p='u'): return f'{p}{next(_ids)}'
def f2(v): return f'{v:.2f}'.rstrip('0').rstrip('.')

# ───────────── 색 ─────────────
A = dict(night='#0A0F1C', facade='#141D30', frame='#6EA8FE', light_top='#F4F8FF', light_bot='#B9D5FF', text='#E6EDF3', text2='#9AA7B4', dim='#2A3A57')
B = dict(hi='#EEF23A', ink='#141519', paper='#F4F5EF', gray='#8A8F98', ink2='#2A2C31')
C = dict(red='#D6402A', ink='#17130F', paper='#EEE7DA', deep='#8E2A1E', ash='#8C857B')

# ───────────── 공통: 꺾인 선을 다각형 윤곽으로 (VectorDrawable 용, 마스크 없이 구멍을 낸다) ─────────────
def _isect(p1, d1, p2, d2):
    det = d1[0] * d2[1] - d1[1] * d2[0]
    if abs(det) < 1e-9: return p2
    t = ((p2[0] - p1[0]) * d2[1] - (p2[1] - p1[1]) * d2[0]) / det
    return (p1[0] + d1[0] * t, p1[1] + d1[1] * t)
def stroke_outline(pts, w):
    """맞물림(miter) 이음 · 평평한 끝으로 획을 다각형으로 바꾼다."""
    h = w / 2
    def offs(sign):
        segs = []
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy); nx, ny = -dy / L * sign, dx / L * sign
            segs.append(((x0 + nx * h, y0 + ny * h), (dx / L, dy / L), (x1 + nx * h, y1 + ny * h)))
        out = [segs[0][0]]
        for a, b in zip(segs, segs[1:]):
            out.append(_isect(a[0], a[1], b[0], b[1]))
        out.append(segs[-1][2])
        return out
    left, right = offs(1), offs(-1)
    poly = left + right[::-1]
    return 'M' + ' L'.join(f'{f2(x)} {f2(y)}' for x, y in poly) + ' Z'

# ───────────── A · 켜 둔 불 ─────────────
# 같은 아치 반지름으로 모든 글자를 짓는다(열주). 선 굵기 T, 엑스하이트 XH.
T = 8.5; XH = 72; R = (76.1 - T) / 4  # m 폭 76.1 = 4R + T
CY = -XH + T / 2 + R                     # 아치 중심 높이(기준선 0, 위가 음수)
def a_m(x=0.0, base=0.0):
    c0, c1, c2 = x + T / 2, x + T / 2 + 2 * R, x + T / 2 + 4 * R
    cy = base + CY
    return (f'M{f2(c0)} {f2(base)} V{f2(cy)} A{f2(R)} {f2(R)} 0 0 1 {f2(c1)} {f2(cy)} V{f2(base)} '
            f'M{f2(c1)} {f2(cy)} A{f2(R)} {f2(R)} 0 0 1 {f2(c2)} {f2(cy)} V{f2(base)}')
def a_pane(x=0.0, base=0.0, g=3.2):
    c1, c2 = x + T / 2 + 2 * R, x + T / 2 + 4 * R
    x0, x1 = c1 + T / 2 + g, c2 - T / 2 - g; r = (x1 - x0) / 2; cy = base + CY
    return f'M{f2(x0)} {f2(base)} V{f2(cy)} A{f2(r)} {f2(r)} 0 0 1 {f2(x1)} {f2(cy)} V{f2(base)} Z'
def a_n(x):
    c0, c1 = x + T / 2, x + T / 2 + 2 * R
    return f'M{f2(c0)} 0 V{f2(CY)} A{f2(R)} {f2(R)} 0 0 1 {f2(c1)} {f2(CY)} V0'
def a_o(x):
    c0, c1 = x + T / 2, x + T / 2 + 2 * R; lo = -T / 2 - R + 1
    return f'M{f2(c0)} {f2(CY)} A{f2(R)} {f2(R)} 0 0 1 {f2(c1)} {f2(CY)} V{f2(lo)} A{f2(R)} {f2(R)} 0 0 1 {f2(c0)} {f2(lo)} Z'
def a_e(x):
    c0, c1 = x + T / 2, x + T / 2 + 2 * R; lo = -T / 2 - R + 1; bar = -38.5
    cx = (c0 + c1) / 2; th = math.radians(28)
    ex, ey = cx + R * math.cos(th), lo + R * math.sin(th)
    return (f'M{f2(c0 - T/2 + 0.01)} {f2(bar)} H{f2(c1)} V{f2(CY)} A{f2(R)} {f2(R)} 0 0 0 {f2(c0)} {f2(CY)} '
            f'V{f2(lo)} A{f2(R)} {f2(R)} 0 0 0 {f2(ex)} {f2(ey)}')
def a_t(x):
    s = x + 11.5
    return f'M{f2(s)} -96 V0 M{f2(x)} {f2(-XH + T/2)} H{f2(x + 27)}'
def a_y(x):
    c0, c1 = x + T / 2, x + T / 2 + 2 * R; lo = -T / 2 - R
    return f'M{f2(c0)} {-XH} V{f2(lo)} A{f2(R)} {f2(R)} 0 0 0 {f2(c1)} {f2(lo)} M{f2(c1)} {-XH} V24'
W_N = 2 * R + T
def a_wordmark_parts():
    """mynote 를 (획 경로, 창 경로, 폭) 으로."""
    x = 0.0; d = []
    d.append(a_m(x)); pane = a_pane(x); x += 4 * R + T + 10.5
    d.append(a_y(x)); x += W_N + 10.5
    d.append(a_n(x)); x += W_N + 9.5
    d.append(a_o(x)); x += W_N + 7
    d.append(a_t(x)); x += 27 + 5.5
    d.append(a_e(x)); x += W_N
    return ' '.join(d), pane, x
def a_windows(xh, mono=False, frame=None, light=None, lit=True, gid=None):
    """m 의 두 창을 엑스하이트 xh 로. 기준선 0, 왼쪽 0. 폭 4R+T(=76.1). (svg 조각, defs)"""
    cy = -xh + T / 2 + R
    c0, c1, c2 = T / 2, T / 2 + 2 * R, T / 2 + 4 * R
    d = (f'M{f2(c0)} 0 V{f2(cy)} A{f2(R)} {f2(R)} 0 0 1 {f2(c1)} {f2(cy)} V0 '
         f'M{f2(c1)} {f2(cy)} A{f2(R)} {f2(R)} 0 0 1 {f2(c2)} {f2(cy)} V0')
    g = 3.2; x0, x1 = c1 + T / 2 + g, c2 - T / 2 - g; r = (x1 - x0) / 2
    pane = f'M{f2(x0)} 0 V{f2(cy)} A{f2(r)} {f2(r)} 0 0 1 {f2(x1)} {f2(cy)} V0 Z'
    fr = frame or A['frame']
    if mono:
        return f'<path d="{d}" fill="none" stroke="{fr}" stroke-width="{T}"/>' + (f'<path d="{pane}" fill="{fr}"/>' if lit else ''), ''
    gid = gid or uid('ag')
    # 빛: 위가 가장 밝고 아래로 주광 파랑 — 두 정지점, 사용자 좌표(VectorDrawable 로 그대로 옮긴다)
    defs = (f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="0" y1="{f2(-xh)}" x2="0" y2="0">'
            f'<stop offset="0" stop-color="{light or A["light_top"]}"/><stop offset="1" stop-color="{A["light_bot"]}"/></linearGradient>')
    body = f'<path d="{d}" fill="none" stroke="{fr}" stroke-width="{T}"/>' + (f'<path d="{pane}" fill="url(#{gid})"/>' if lit else '')
    return body, defs
A_MARK_XH = 92  # 로고마크의 창 높이(폭 76.1 대비 1.21)
def a_mark(mode='color', ink=None, light=None, box=100):
    """viewBox 0 0 100 100 안에 긴 창 두 개."""
    s = 0.86; w = 76.1 * s; h = A_MARK_XH * s
    ox, oy = (100 - w) / 2, (100 + h) / 2
    body, defs = a_windows(A_MARK_XH, mono=(mode == 'mono'), frame=ink, light=light)
    return (f'<defs>{defs}</defs>' if defs else '') + f'<g transform="translate({f2(ox)} {f2(oy)}) scale({s})">{body}</g>'

# ───────────── B · 형광펜 ─────────────
NIB_W, NIB_ANG = 27, 26
def nib_poly(p0, p1, w=NIB_W, ang=NIB_ANG):
    dx, dy = w / 2 * math.cos(math.radians(ang)), -w / 2 * math.sin(math.radians(ang))
    pts = [(p0[0] - dx, p0[1] - dy), (p0[0] + dx, p0[1] + dy), (p1[0] + dx, p1[1] + dy), (p1[0] - dx, p1[1] - dy)]
    cx = sum(p[0] for p in pts) / 4; cy = sum(p[1] for p in pts) / 4
    pts.sort(key=lambda p: math.atan2(p[1] - cy, p[0] - cx))
    return 'M' + ' L'.join(f'{f2(x)} {f2(y)}' for x, y in pts) + ' Z'
B_STROKES = [((21, 82), (21, 24)), ((21, 24), (50, 70)), ((50, 70), (79, 24)), ((79, 24), (79, 82))]
def b_mark(mode='color', c=None, alpha=0.8):
    c = c or B['hi']
    a = 1 if mode == 'mono' else alpha
    return ''.join(f'<path d="{nib_poly(p, q)}" fill="{c}" fill-opacity="{a}"/>' for p, q in B_STROKES)
def b_band(x0, x1, y0, y1, k=None):
    """끌 끝 띠 — 양 끝이 펜촉 각도로 잘린 평행사변형."""
    k = (y1 - y0) / math.tan(math.radians(90 - (90 - NIB_ANG) * 0.5)) if k is None else k
    return f'M{f2(x0 + k)} {f2(y0)} H{f2(x1)} L{f2(x1 - k)} {f2(y1)} H{f2(x0)} Z'

# ───────────── C · 회독 ─────────────
SEAL_EDGE = 'M12 7 Q50 5.6 88 7.2 Q93.4 7.6 93.2 13 Q94.4 50 92.8 87 Q92.6 93 87 93.2 Q50 94.6 13.2 92.9 Q7.2 92.6 7 87.4 Q5.7 50 6.9 12.6 Q7.3 7.2 12 7 Z'
C_W = 9.5
C_OUTER = [(24, 99), (24, 24), (76, 24), (76, 76), (43, 76)]
C_INNER = 'M41.5 41.5 H58.5 V58.5 H41.5 Z'
def c_holes():
    # 바깥 획: 아래 가장자리에서 들어온다 → 도장 밖으로 나가는 부분은 가장자리 모양이 자른다(evenodd 대신 따로 자름)
    outer = stroke_outline(C_OUTER, C_W)
    # 안쪽 사각 고리
    a0, a1 = 41.5 - C_W / 2, 58.5 + C_W / 2; b0, b1 = 41.5 + C_W / 2, 58.5 - C_W / 2
    ring = f'M{f2(a0)} {f2(a0)} H{f2(a1)} V{f2(a1)} H{f2(a0)} Z M{f2(b0)} {f2(b0)} V{f2(b1)} H{f2(b1)} V{f2(b0)} Z'
    return outer, ring
def c_seal_path():
    """도장 한 덩어리 — 가장자리에서 획을 파낸 단일 경로(evenodd). 들어오는 획은 아래 가장자리를 뚫는다."""
    outer, ring = c_holes()
    return SEAL_EDGE, outer, ring
def c_mark(mode='color', red=None, paper=None):
    red = red or C['red']
    edge, outer, ring = c_seal_path()
    cid = uid('cc')
    # SVG 에서는 clipPath + evenodd 로 그린다(VectorDrawable 은 group clip-path 로 같은 것을 한다)
    return (f'<defs><clipPath id="{cid}"><path d="{edge}"/></clipPath></defs>'
            f'<g clip-path="url(#{cid})"><path fill-rule="evenodd" fill="{red}" d="M0 0 H100 V100 H0 Z {outer} {ring}"/></g>')

# ───────────── 글꼴 윤곽 조판 ─────────────
def set_text(key, text, size, tracking=0, kern=None, x=0, y=0, fill='#000'):
    """size = em 크기(px 단위계). tracking = em 의 1/1000. kern = {(a,b): 1/1000em}. 반환 (svg, 폭)."""
    f = G[key]; sc = size / f['upm']; parts = []; cx = x
    kern = kern or {}
    for i, ch in enumerate(text):
        if ch == ' ':
            cx += size * 0.28; continue
        g = f['glyphs'][ch]
        parts.append(f'<path transform="translate({f2(cx)} {f2(y)}) scale({sc:.5f})" d="{g["d"]}"/>')
        cx += g['adv'] * sc + tracking * size / 1000
        if i + 1 < len(text): cx += kern.get((ch, text[i + 1]), 0) * size / 1000
    cx -= tracking * size / 1000
    return f'<g fill="{fill}">' + ''.join(parts) + '</g>', cx - x
