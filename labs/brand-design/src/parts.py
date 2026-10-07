"""보드에 들어가는 그림 조각 — 구성도 · 아이콘 묶음 · 폰 화면 · 배너."""
from system import *
import math

def svgw(inner, vb, width=None, cls='', label=None, style=''):
    a = f' role="img" aria-label="{label}"' if label else ' aria-hidden="true"'
    w = f' width="{width}"' if width else ''
    c = f' class="{cls}"' if cls else ''
    s = f' style="{style}"' if style else ''
    return f'<svg viewBox="{vb}"{w}{c}{s}{a} xmlns="http://www.w3.org/2000/svg">{inner}</svg>'

def mark_svg(d, width=None, label=None, mode='color', cls=''):
    inner = {'a': lambda: a_mark(mode, ink='#E6EDF3' if mode == 'mono' else None),
             'b': lambda: b_mark(mode, c='#141519' if mode == 'mono' else None),
             'c': lambda: c_mark()}[d]()
    if mode == 'light':
        inner = {'a': lambda: a_mark('color', ink='#0A0F1C', light='#6EA8FE').replace(A['light_bot'], '#6EA8FE'),
                 'b': lambda: b_mark('color', c=B['ink'], alpha=0.78),
                 'c': lambda: c_mark()}[d]()
    if mode == 'ink':  # 브랜드 색 바탕 위
        inner = {'a': lambda: a_mark('mono', ink='#0A0F1C'),
                 'b': lambda: b_mark('color', c=B['ink'], alpha=0.8),
                 'c': lambda: c_mark(red=C['ink'])}[d]()
    return svgw(inner, '0 0 100 100', width, cls, label)

# ───────── 구성도 ─────────
def construct_a():
    s = 3.0; ox = 40; base = 322
    xh = A_MARK_XH
    body, defs = a_windows(xh)
    cy = -xh + T / 2 + R
    c = '#3D5A86'
    g = [f'<defs>{defs}</defs>']
    # 안내선: 기준선 · 아치 시작선 · 꼭대기
    for y, t in [(0, '기준선'), (cy, '아치 시작'), (-xh, '꼭대기')]:
        Y = base + y * s
        g.append(f'<line x1="20" x2="330" y1="{f2(Y)}" y2="{f2(Y)}" stroke="{c}" stroke-width=".8" stroke-dasharray="3 4"/>')
        g.append(f'<text x="336" y="{f2(Y + 3.5)}" font-size="10" fill="#8FA6CC">{t}</text>')
    g.append(f'<g transform="translate({f2(ox)} {base}) scale({s})">{body}</g>')
    # 반지름 원 · 중심선
    for cx in (T / 2 + R, T / 2 + 3 * R):
        X = ox + cx * s; Y = base + cy * s
        g.append(f'<circle cx="{f2(X)}" cy="{f2(Y)}" r="{f2(R * s)}" fill="none" stroke="#9CC4FF" stroke-width=".8"/>')
        g.append(f'<circle cx="{f2(X)}" cy="{f2(Y)}" r="2.2" fill="#9CC4FF"/>')
    X = ox + (T / 2 + R) * s; Y = base + cy * s
    g.append(f'<line x1="{f2(X)}" y1="{f2(Y)}" x2="{f2(X - R * s * .7071)}" y2="{f2(Y - R * s * .7071)}" stroke="#9CC4FF" stroke-width=".8"/>')
    g.append(f'<text x="{f2(X - 30)}" y="{f2(Y + 18)}" font-size="10" fill="#B9D5FF">R 16.9</text>')
    # 획 굵기
    x0 = ox; y0 = base - 40
    g.append(f'<line x1="{f2(x0)}" x2="{f2(x0 + T * s)}" y1="{y0}" y2="{y0}" stroke="#F4F8FF" stroke-width="1"/>')
    g.append(f'<text x="{f2(x0 + T * s + 6)}" y="{y0 + 3.5}" font-size="10" fill="#F4F8FF">T 8.5</text>')
    # 빛 간격
    xr = ox + (T / 2 + 4 * R + T / 2) * s
    g.append(f'<line x1="{f2(xr - T * s - 3.2 * s)}" x2="{f2(xr - T * s)}" y1="{f2(base - 120)}" y2="{f2(base - 120)}" stroke="#F4F8FF" stroke-width="1"/>')
    g.append(f'<text x="{f2(xr + 8)}" y="{f2(base - 116)}" font-size="10" fill="#F4F8FF">빛 ↔ 창틀 3.2</text>')
    return svgw(''.join(g), '0 0 400 360', label='켜 둔 불 마크 구성도')

def construct_b():
    s = 3.2; g = []
    for i, (p, q) in enumerate(B_STROKES):
        g.append(f'<path d="{nib_poly(p, q)}" transform="scale({s})" fill="{B["ink"]}" fill-opacity=".16" stroke="{B["ink"]}" stroke-width=".35"/>')
    # 펜촉 각도 표시: 각 획의 시작점에 펜촉 선분
    for i, (p, q) in enumerate(B_STROKES):
        for pt in (p, q):
            dx, dy = NIB_W / 2 * math.cos(math.radians(NIB_ANG)), -NIB_W / 2 * math.sin(math.radians(NIB_ANG))
            g.append(f'<line x1="{f2((pt[0] - dx) * s)}" y1="{f2((pt[1] - dy) * s)}" x2="{f2((pt[0] + dx) * s)}" y2="{f2((pt[1] + dy) * s)}" stroke="#141519" stroke-width="1.1"/>')
        mx, my = (p[0] + q[0]) / 2 * s, (p[1] + q[1]) / 2 * s
        g.append(f'<circle cx="{f2(mx)}" cy="{f2(my)}" r="9" fill="#141519"/><text x="{f2(mx)}" y="{f2(my + 3.6)}" text-anchor="middle" font-size="10.5" font-weight="700" fill="{B["hi"]}">{i + 1}</text>')
    # 각도 호
    cx, cy = 21 * s, 82 * s + 40
    g.append(f'<line x1="{f2(cx - 50)}" y1="{f2(cy)}" x2="{f2(cx + 60)}" y2="{f2(cy)}" stroke="#141519" stroke-width=".8" stroke-dasharray="3 3"/>')
    ex, ey = cx + 60 * math.cos(math.radians(NIB_ANG)), cy - 60 * math.sin(math.radians(NIB_ANG))
    g.append(f'<line x1="{f2(cx)}" y1="{f2(cy)}" x2="{f2(ex)}" y2="{f2(ey)}" stroke="#141519" stroke-width="1.1"/>')
    g.append(f'<path d="M{f2(cx + 34)} {f2(cy)} A34 34 0 0 0 {f2(cx + 34 * math.cos(math.radians(NIB_ANG)))} {f2(cy - 34 * math.sin(math.radians(NIB_ANG)))}" fill="none" stroke="#141519" stroke-width=".8"/>')
    g.append(f'<text x="{f2(cx + 66)}" y="{f2(cy - 12)}" font-size="11" fill="#141519">펜촉 26° · 폭 27</text>')
    return svgw(''.join(g), '-10 20 340 340', label='형광펜 마크 구성도')

def construct_c():
    s = 3.2; g = []
    # 격자
    for i in range(0, 101, 4):
        w = .9 if i % 20 == 0 else .35
        g.append(f'<line x1="{i * s}" x2="{i * s}" y1="0" y2="{100 * s}" stroke="#5A4A3C" stroke-width="{w}"/>')
        g.append(f'<line y1="{i * s}" y2="{i * s}" x1="0" x2="{100 * s}" stroke="#5A4A3C" stroke-width="{w}"/>')
    g.append(f'<g transform="scale({s})" opacity=".92">{c_mark()}</g>')
    # 획 중심선과 방향
    pts = C_OUTER
    d = 'M' + ' L'.join(f'{f2(x * s)} {f2(min(y, 96) * s)}' for x, y in pts)
    g.append(f'<path d="{d}" fill="none" stroke="#17130F" stroke-width="1.2" stroke-dasharray="5 4"/>')
    g.append(f'<path d="M{41.5 * s} {41.5 * s} H{58.5 * s} V{58.5 * s} H{41.5 * s} Z" fill="none" stroke="#17130F" stroke-width="1.2" stroke-dasharray="5 4"/>')
    ax, ay = 24 * s, 88 * s
    g.append(f'<path d="M{ax - 7} {ay} L{ax} {ay - 12} L{ax + 7} {ay}" fill="none" stroke="#17130F" stroke-width="1.6"/>')
    g.append(f'<text x="{ax + 12}" y="{ay + 4}" font-size="11" fill="#17130F">입구</text>')
    g.append(f'<text x="{76 * s + 10}" y="{20 * s}" font-size="11" fill="#EEE7DA">획 9.5</text>')
    return svgw(''.join(g), '-4 -4 336 336', label='회독 마크 구성도')

# ───────── 아이콘 묶음 ─────────
def layer_svg(inner, width=None, guides=True, checker=False):
    gd = ''
    if guides:
        gd = ('<rect x="18" y="18" width="72" height="72" fill="none" stroke="#FF4FD8" stroke-width=".45" stroke-dasharray="1.6 1.6"/>'
              '<circle cx="54" cy="54" r="33" fill="none" stroke="#3EE6FF" stroke-width=".45"/>')
    bg = ''
    if checker:
        bg = ('<defs><pattern id="chk" width="6" height="6" patternUnits="userSpaceOnUse"><rect width="6" height="6" fill="#20252c"/>'
              '<rect width="3" height="3" fill="#2a3039"/><rect x="3" y="3" width="3" height="3" fill="#2a3039"/></pattern></defs><rect width="108" height="108" fill="url(#chk)"/>')
    return svgw(bg + inner + gd, '0 0 108 108', width)

# ───────── 폰 ─────────
def phone(screen_html, label, cls=''):
    return f'<div class="phone-wrap"><div class="phone {cls}" role="img" aria-label="{label}"><div class="screen">{screen_html}</div></div></div>'

def status_bar(color='#E6EDF3'):
    return (f'<div class="sb" style="color:{color}"><span class="num">9:41</span><i>'
            f'<svg width="15" viewBox="0 0 15 11"><path d="M1 10h2V7H1zM5 10h2V5H5zM9 10h2V3H9zM13 10h2V1h-2z" fill="{color}" transform="translate(-1 0)"/></svg>'
            f'<svg width="14" viewBox="0 0 14 11"><path d="M7 10.5 0.5 3.6a9.4 9.4 0 0 1 13 0Z" fill="{color}"/></svg>'
            f'<svg width="22" viewBox="0 0 22 11"><rect x=".5" y=".5" width="18" height="10" rx="2.5" fill="none" stroke="{color}" opacity=".6"/><rect x="2" y="2" width="12" height="7" rx="1.4" fill="{color}"/><rect x="19.6" y="3.5" width="1.6" height="4" rx=".8" fill="{color}" opacity=".6"/></svg>'
            f'</i></div>')

NAV_ICONS = {
    'home': '<path d="M4 10.5 12 4l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5.5h-5V20H5a1 1 0 0 1-1-1z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>',
    'bm': '<path d="M6.5 4h11v16l-5.5-4-5.5 4z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>',
    'stats': '<path d="M5 20V12M12 20V5M19 20v-9" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
}
def app_home(logo_svg, logo_size=46, title_size=24):
    notes = [('Kotlin', '2026.10.06', '코루틴 취소와 예외'), ('Android', '2026.10.02', 'Room 마이그레이션 정리'), ('Compose', '2026.09.27', '리컴포지션을 줄이는 세 가지 습관')]
    cards = ''.join(f'<div class="card"><div class="meta"><span class="chip">{c}</span><span class="num">{d}</span></div><div class="t">{t}</div></div>' for c, d, t in notes)
    nav = ''.join(f'<span class="{"on" if k == "home" else ""}" style="color:{"#6EA8FE" if k == "home" else "#9AA7B4"}"><svg viewBox="0 0 24 24">{v}</svg></span>' for k, v in NAV_ICONS.items())
    burger = '<svg viewBox="0 0 24 24"><path d="M4 7h16M4 12h16M4 17h16" stroke="#C8D2DC" stroke-width="1.7" stroke-linecap="round"/></svg>'
    return (f'<div class="app">{status_bar()}<div class="bar">{burger}</div>'
            f'<div class="hero"><div class="logo" style="width:{logo_size}px;height:{logo_size}px;border-radius:{logo_size * .29:.1f}px">{logo_svg}</div>'
            f'<h4 style="font-size:{title_size}px">공부 기록</h4><p>읽고 싶은 노트를 고르세요. 왼쪽 위 ☰ 를 누르면 전체 목록이 열립니다.</p></div>'
            f'<div class="ov">최근 노트</div>{cards}<div class="fnav">{nav}</div></div>')

def hero_block(logo_svg, logo_size=54, title_size=28, radius=None):
    r = logo_size * .29 if radius is None else radius
    return (f'<div class="app" style="height:auto;padding:28px 8px 30px;border-radius:6px"><div class="hero">'
            f'<div class="logo" style="width:{logo_size}px;height:{logo_size}px;border-radius:{r:.1f}px">{logo_svg}</div>'
            f'<h4 style="font-size:{title_size}px">공부 기록</h4><p>왼쪽 목록에서 읽고 싶은 노트를 고르세요.</p></div></div>')

def launcher(our_icon, wallpaper, fg='#fff', shape='circle', dock_bg='rgba(255,255,255,.12)', themed=None):
    order = [0, 1, 2, 3, 4, 'me', 5, 6, 7, 8, 9, 10]
    cells = []
    for o in order:
        if o == 'me':
            cells.append(f'<div class="app-i me">{our_icon}<span>MyNote</span></div>')
        else:
            svg, name = fake_themed(o, shape, **themed) if themed else fake_icon(o, shape)
            cells.append(f'<div class="app-i">{svg}<span>{name}</span></div>')
    dock = ''.join(f'<div class="app-i">{(fake_themed(i, shape, **themed) if themed else fake_icon(i, shape))[0]}<span></span></div>' for i in (9, 3, 2, 11))
    return (f'<div class="home" style="background:{wallpaper};color:{fg}">{status_bar(fg)}'
            f'<div class="clock"><b class="num">9:41</b><span>10월 7일 수요일</span></div>'
            f'<div class="grid">{"".join(cells)}</div><div class="dock" style="background:{dock_bg}">{dock}</div></div>')

def notification(small_icon_svg, accent, dark=True):
    bg = '#1E232B' if dark else '#F1F3F6'; fg = '#E6EDF3' if dark else '#1A1D22'; track = '#3A414C' if dark else '#D5DAE1'
    return (f'<div class="notif" style="background:{bg};color:{fg}"><div class="row">{small_icon_svg}'
            f'<span style="opacity:.75">MyNote · 지금</span></div><div class="ttl">퀴즈 모델 받기</div>'
            f'<div class="body num">42% 받았습니다 · 1.1 / 2.6GB</div><div class="bar2" style="background:{track}"><i style="background:{accent}"></i></div></div>')

def small_icon(d, color, size=18):
    """알림 작은 아이콘 — 흰 실루엣을 색으로."""
    inner = {'a': lambda: a_mark('mono', ink=color),
             'b': lambda: b_mark('mono', c=color),
             'c': lambda: c_mark(red=color)}[d]()
    return svgw(inner, '0 0 100 100', size)
