"""큰 적용물 — README 배너(1280×640) · Play 그래픽(1024×500) · 자유 적용 하나씩."""
from parts import *

FONT_A = "'Gothic A1', sans-serif"
FONT_B = "'Noto Sans KR', sans-serif"
FONT_BH = "'Black Han Sans', sans-serif"
FONT_C = "'Hahmlet', serif"

def facade(x0, y0, cols, rows, ww, wh, gx, gy, lit=(), stroke=4.2, dim='#1C2945', lit_frame=None):
    """꺼진 창들의 격자. lit = {(행, 열)} 만 불이 켜진다."""
    out = []; r = ww / 2
    gid = uid('fl')
    out.append(f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="{A["light_bot"]}"/></linearGradient></defs>')
    for i in range(rows):
        for j in range(cols):
            x = x0 + j * (ww + gx); y = y0 + i * (wh + gy)
            d = f'M{f2(x)} {f2(y + wh)} V{f2(y + r)} A{f2(r)} {f2(r)} 0 0 1 {f2(x + ww)} {f2(y + r)} V{f2(y + wh)}'
            if (i, j) in lit:
                g = stroke * .4 + 2; pr = r - stroke / 2 - g
                pd = f'M{f2(x + stroke / 2 + g)} {f2(y + wh)} V{f2(y + r)} A{f2(pr)} {f2(pr)} 0 0 1 {f2(x + ww - stroke / 2 - g)} {f2(y + r)} V{f2(y + wh)} Z'
                out.append(f'<path d="{d}" fill="none" stroke="{lit_frame or A["frame"]}" stroke-width="{stroke}"/><path class="lit-pane" d="{pd}" fill="url(#{gid})"/>')
            else:
                out.append(f'<path d="{d}" fill="none" stroke="{dim}" stroke-width="{stroke}"/>')
    return ''.join(out)

# ───────── A ─────────
def a_readme():
    gid = uid('bg')
    lk = a_lockup_h()
    body = (f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0D1528"/><stop offset="1" stop-color="#070A12"/></linearGradient></defs>'
            f'<rect width="1280" height="640" fill="url(#{gid})"/>'
            + facade(706, 92, 7, 4, 50, 92, 28, 26, lit={(1, 4)}) +
            f'<defs>{lk.defs}</defs>' + lk.at(96, 214, 0.9) +
            f'<text x="98" y="378" font-family="{FONT_A}" font-size="27" fill="#9AA7B4">GitHub 저장소의 마크다운 노트를</text>'
            f'<text x="98" y="418" font-family="{FONT_A}" font-size="27" fill="#9AA7B4">읽는 안드로이드 앱. 퀴즈는 기기 안에서.</text>')
    return svgw(body, '0 0 1280 640', label='켜 둔 불 README 배너')

def a_play():
    gid = uid('bg')
    wm = a_wordmark(lit=False, frame='#E6EDF3')
    body = (f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0D1528"/><stop offset="1" stop-color="#070A12"/></linearGradient></defs>'
            f'<rect width="1024" height="500" fill="url(#{gid})"/>'
            + facade(-30, 46, 14, 2, 46, 84, 30, 30, lit={(0, 9)}, dim='#18233B') +
            f'<rect y="290" width="1024" height="210" fill="#070A12" opacity=".0"/>'
            + wm.at(512 - wm.w * .62, 318, .62) +
            f'<text x="512" y="452" text-anchor="middle" font-family="{FONT_A}" font-size="24" fill="#9AA7B4">언제 돌아와도 불이 켜져 있습니다</text>')
    return svgw(body, '0 0 1024 500', label='켜 둔 불 Play 그래픽')

def a_free():
    """불 켜진 날 — 한 달 학습 기록 공유 카드(가짜 데이터)."""
    W, H = 540, 720
    lit_days = {1, 2, 4, 5, 6, 8, 9, 11, 12, 13, 15, 16, 19, 20, 22, 23, 26, 27, 29}
    gid = uid('bg')
    out = [f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0D1528"/><stop offset="1" stop-color="#070A12"/></linearGradient></defs>',
           f'<rect width="{W}" height="{H}" fill="url(#{gid})"/>']
    out.append(f'<text x="44" y="78" font-family="{FONT_A}" font-size="19" fill="#9AA7B4">2026년 10월</text>')
    out.append(f'<text x="44" y="138" font-family="{FONT_A}" font-weight="300" font-size="54" letter-spacing="-1.5" fill="#E6EDF3">불 켜진 날 <tspan font-weight="600" fill="#F4F8FF">19</tspan>일</text>')
    lit = set()
    day = 1
    for i in range(5):
        for j in range(7):
            if day in lit_days: lit.add((i, j))
            day += 1
    out.append(facade(44, 196, 7, 5, 46, 66, 18.7, 22, lit=lit, stroke=3.6, dim='#1F2C48'))
    # 날짜 숫자는 생략 — 창이 곧 날
    out.append(f'<text x="44" y="{H - 44}" font-family="{FONT_A}" font-size="15" fill="#687585">mynote · 노트 31개 · 퀴즈 12회</text>')
    sym = a_symbol()
    out.append(f'<defs>{sym.defs}</defs>' + sym.at(W - 44 - 76.1 * .3, H - 44 - 92 * .3 - 4, .3))
    return svgw(''.join(out), f'0 0 {W} {H}', label='불 켜진 날 공유 카드')

# ───────── B ─────────
NOTE_LINES = [
    ('코루틴은 중단 지점에서만 취소를 ', '확인한다'),
    ('긴 계산 루프 안에서는 ensureActive() 나', None),
    ('yield() 로 직접 확인해야 한다.', None),
    ('CancellationException 은 실패가 아니라', None),
    ('', '정상적인 종료로 취급되어'),
    ('부모에게 전파되지 않는다.', None),
]
def b_page(x, y, size=31, lh=54, width=None):
    """가짜 노트 본문 위에 형광펜 — 줄을 넘긴 칠."""
    out = []
    for i, (a, hi) in enumerate(NOTE_LINES):
        yy = y + i * lh
        if a:
            out.append(f'<text x="{x}" y="{yy}" font-family="{FONT_B}" font-size="{size}" fill="#9B9E95">{a}</text>')
        if hi:
            # 글자 폭 근사(한글 1em · 공백 .3em)
            ax = x + sum(size * (0.3 if ch == ' ' else (0.56 if ch.isascii() else 0.98)) for ch in a)
            hw = sum(size * (0.3 if ch == ' ' else 0.98) for ch in hi)
            out.append(f'<path d="{b_band(ax - 10, ax + hw + 12, yy - size * .86, yy + size * .24, k=12)}" fill="{B["hi"]}"/>')
            out.append(f'<text x="{f2(ax)}" y="{yy}" font-family="{FONT_B}" font-weight="700" font-size="{size}" fill="{B["ink"]}">{hi}</text>')
    return ''.join(out)

def b_page_html(x, y, w, h, size=30):
    mk = f'background:linear-gradient(104deg,transparent 7px,{B["hi"]} 7px,{B["hi"]} calc(100% - 7px),transparent calc(100% - 7px));color:{B["ink"]};font-weight:700;padding:0 10px;-webkit-box-decoration-break:clone;box-decoration-break:clone'
    return (f'<foreignObject x="{x}" y="{y}" width="{w}" height="{h}"><div xmlns="http://www.w3.org/1999/xhtml" style="font-family:{FONT_B};font-size:{size}px;line-height:1.78;color:#8A8D84;word-break:keep-all">'
            f'<div style="font-size:22px;font-weight:700;color:#55585F;margin-bottom:14px">코루틴 취소와 예외</div>'
            f'코루틴은 <span style="{mk}">중단 지점에서만 취소를 확인한다. 긴 계산 루프 안에서는</span> ensureActive() 나 yield() 로 직접 확인해야 한다. CancellationException 은 '
            f'<span style="{mk}">실패가 아니라 정상적인 종료</span>로 취급되어 부모에게 전파되지 않는다.</div></foreignObject>')

def b_readme():
    wm = b_wordmark(on_dark=False)
    body = (f'<rect width="1280" height="640" fill="{B["paper"]}"/>'
            + b_page_html(676, 92, 520, 480)
            + wm.at(92, 220, 1.05) +
            f'<text x="98" y="396" font-family="{FONT_B}" font-size="27" fill="#3A3D44">GitHub 저장소의 마크다운 노트를</text>'
            f'<text x="98" y="436" font-family="{FONT_B}" font-size="27" fill="#3A3D44">읽는 안드로이드 앱. 퀴즈는 기기 안에서.</text>')
    return svgw(body, '0 0 1280 640', label='형광펜 README 배너')

def b_play():
    sym = b_symbol('mono', c=B['ink'])
    wm = b_wordmark(on_dark=False, band=False)
    body = (f'<rect width="1024" height="500" fill="{B["hi"]}"/>'
            + sym.at(560, -60, 6.6) +
            wm.at(72, 168, .92) +
            f'<text x="76" y="340" font-family="{FONT_BH}" font-size="40" fill="{B["ink"]}">두 번 지나간 자리는</text>'
            f'<text x="76" y="392" font-family="{FONT_BH}" font-size="40" fill="{B["ink"]}">진해진다.</text>')
    return svgw(body, '0 0 1024 500', label='형광펜 Play 그래픽')

def b_sticker(cx, cy, label, rot=0, size=34):
    w = sum(size * (0.3 if ch == ' ' else 0.98) for ch in label) + 40
    h = size + 30
    band = b_band(-w / 2, w / 2, -h / 2, h / 2, k=16)
    return (f'<g transform="translate({cx} {cy}) rotate({rot})">'
            f'<path d="{band}" fill="#fff" stroke="#fff" stroke-width="14" stroke-linejoin="round" filter="url(#stk)"/>'
            f'<path d="{band}" fill="{B["hi"]}"/>'
            f'<text x="0" y="{size * .36}" text-anchor="middle" font-family="{FONT_BH}" font-size="{size}" fill="{B["ink"]}">{label}</text></g>')

def b_free():
    W, H = 640, 520
    sym = b_symbol()
    out = [f'<defs><filter id="stk" x="-10%" y="-20%" width="120%" height="150%"><feDropShadow dx="0" dy="3" stdDeviation="4" flood-color="#000" flood-opacity=".18"/></filter></defs>',
           f'<rect width="{W}" height="{H}" fill="#E4E6DE"/>',
           b_sticker(186, 108, '다시 읽기', -4), b_sticker(470, 92, '외울 것', 3),
           b_sticker(170, 410, '이해함', 2), b_sticker(452, 430, '시험에 나옴', -3, 30),
           f'<g filter="url(#stk)"><rect x="{W/2 - 92}" y="{H/2 - 92}" width="184" height="184" rx="34" fill="#fff"/></g>',
           f'<rect x="{W/2 - 82}" y="{H/2 - 82}" width="164" height="164" rx="28" fill="{B["ink"]}"/>',
           sym.at(W / 2 - 92 * .62, H / 2 - 84 * .62, 1.24)]
    wm = b_wordmark(on_dark=False)
    out.append(f'<g transform="translate({W - 66} {H/2}) rotate(90)">' + wm.at(-wm.w * .26, -28, .52) + '</g>')
    return svgw(''.join(out), f'0 0 {W} {H}', label='형광펜 스티커 시트')

# ───────── C ─────────
def c_readme():
    sym = c_symbol(); wm = c_wordmark()
    body = (f'<rect width="1280" height="640" fill="{C["ink"]}"/>'
            + sym.at(744, 52, 5.9) +
            wm.at(92, 214, 1.0) +
            f'<text x="98" y="384" font-family="{FONT_C}" font-size="27" fill="#B8AFA3">GitHub 저장소의 마크다운 노트를</text>'
            f'<text x="98" y="424" font-family="{FONT_C}" font-size="27" fill="#B8AFA3">읽는 안드로이드 앱. 퀴즈는 기기 안에서.</text>')
    return svgw(body, '0 0 1280 640', label='회독 README 배너')

def c_play():
    edge, outer, ring = c_seal_path()
    wm = c_wordmark(fill=C['ink'], dot=C['paper'])
    body = (f'<rect width="1024" height="500" fill="{C["red"]}"/>'
            f'<g transform="translate(560 -30) scale(5.4)"><path fill-rule="evenodd" fill="#B93320" d="{outer} {ring}"/></g>'
            + wm.at(72, 176, .98) +
            f'<text x="78" y="350" font-family="{FONT_C}" font-weight="600" font-size="36" fill="{C["ink"]}">다시 읽을수록</text>'
            f'<text x="78" y="398" font-family="{FONT_C}" font-weight="600" font-size="36" fill="{C["ink"]}">내 것이 된다.</text>')
    return svgw(body, '0 0 1024 500', label='회독 Play 그래픽')

def c_free():
    """회독 도장 판 — 노트 하나를 다시 읽을 때마다 찍히는 도장(제안 · 가짜 데이터)."""
    W, H = 640, 400
    out = [f'<rect width="{W}" height="{H}" fill="{C["ink"]}"/>',
           f'<rect x="28" y="28" width="{W - 56}" height="{H - 56}" rx="4" fill="{C["paper"]}"/>',
           f'<text x="64" y="92" font-family="{FONT_C}" font-size="15" fill="#7A7065">Kotlin · 노트</text>',
           f'<text x="64" y="132" font-family="{FONT_C}" font-weight="600" font-size="30" fill="{C["ink"]}">코루틴 취소와 예외</text>']
    slots = [(1, '10.02', -5), (2, '10.04', 3), (3, '10.07', -2), (4, None, 0), (5, None, 0)]
    for i, (n, date, rot) in enumerate(slots):
        x = 64 + i * 104; y = 188
        out.append(f'<text x="{x + 40}" y="{y + 132}" text-anchor="middle" font-family="{FONT_C}" font-size="14" fill="#7A7065">{n}회독</text>')
        if date:
            out.append(f'<g transform="translate({x + 40} {y + 40}) rotate({rot}) translate(-40 -40)" opacity="{.93 - i * .04}">' + c_symbol().at(0, 0, 80 / 90) + '</g>')
            out.append(f'<text x="{x + 40}" y="{y + 154}" text-anchor="middle" font-family="{FONT_C}" font-size="12.5" fill="#9A8F82" class="num">{date}</text>')
        else:
            out.append(f'<rect x="{x + 4}" y="{y + 4}" width="72" height="72" fill="none" stroke="#C9BFAF" stroke-width="1.4" stroke-dasharray="4 4"/>')
    return svgw(''.join(out), f'0 0 {W} {H}', label='회독 도장 판')
