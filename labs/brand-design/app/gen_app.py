"""브랜드 A 를 앱에 넣을 최종 그림(svg/)과 시안(scenes/ · shots/ · sheet.html)을 만든다.

  python3 gen_app.py           svg/ · scenes/ · sheet.html 을 다시 쓴다(표준 라이브러리만)
  python3 gen_app.py --shots   위 + scenes/*.html 을 headless Chrome 으로 찍어 shots/ 에 쓴다
                               Chrome 은 환경 변수 CHROME, 없으면 PATH 의 google-chrome · chromium

좌표는 변환 없이 굳힌다 — path 는 M · H · V · A · Z, 선은 stroke, 칠은 단색 또는 2정지점 선형 그라디언트(userSpaceOnUse).
그대로 VectorDrawable · AnimatedVectorDrawable · ImageVector 로 옮길 수 있다.
"""
import math, os, re, shutil, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))

# ───────────── 값 — 디자인 문서 docs/design/brand.md 와 같다 ─────────────
BRAND = dict(night='#0A0F1C', icon_top='#13203A', icon_bot='#070B15', lamp_top='#F4F8FF', lamp_bot='#B9D5FF')
TOK = dict(bg='#0E1217', surface='#161C24', surface2='#1B232D', border='#283039', accent='#6EA8FE',
           text='#E6EDF3', body='#C8D2DC', text2='#9AA7B4', text3='#687585')

STROKE = 8.5                 # 마크 · 워드마크 선 두께(원본 좌표)
ARCH_R, LAMP_R = 16.9, 9.45  # 아치 반지름 · 빛 반지름
MARK_W, MARK_H = 76.1, 92.0  # 맨 마크 상자(선 바깥까지)

LAUNCHER = dict(scale=0.73587, tx=26.0, ty=112.0, pillar=-90.28)   # 108 캔버스 · 기둥은 일부러 마스크 밖
SPLASH = dict(canvas=288, circle=192, scale=1.3)                     # 바탕 없음 · 원 안에 마크 전체
MOTION = dict(hold=166, dur=450, alpha_from=0.0, scale_from=0.8, exit=180)   # 앞 166 꺼진 값 keyframe · 뒤 450 MyNoteEasing · 합 616 = 테마 길이
EASE_ARRIVE = (0.2, 0.8, 0.2, 1.0)   # MyNoteEasing
EASE_LEAVE = (0.4, 0.0, 1.0, 1.0)    # 떠남

HERO_LOGO = dict(C=None, M=46, E=54)      # heroLogoSize = 마크 높이
HERO_TITLE = dict(C=20, M=24, E=28)       # heroTitleSize 값(dp) = 워드마크 x높이
HEADER = dict(C=44, M=56, E=60)
PADY = dict(C=10, M=20, E=28)
PAGE = dict(compact=16, medium=24, expanded=32)
GAP = dict(compact=10, medium=12, expanded=14)
PANE = dict(compact=300, medium=268, expanded=300)
CARD_MIN = dict(compact=240, medium=200, expanded=200)

TITLE_STYLES = [  # 제목 글꼴 스타일 — 이름 · 크기 sp · 줄 높이 배수 · 자간 em
    ('noteTitle', 28, 1.25, -0.015), ('title', 24, 1.3, -0.01), ('sectionTitle', 21, 1.3, -0.01), ('screenTitle', 19, 1.3, -0.01)]

WORDMARK = ('M4.25 0 V-50.85 A16.9 16.9 0 0 1 38.05 -50.85 V0 M38.05 -50.85 A16.9 16.9 0 0 1 71.85 -50.85 V0 '
            'M90.85 -72 V-21.15 A16.9 16.9 0 0 0 124.65 -21.15 M124.65 -72 V24 '
            'M143.65 0 V-50.85 A16.9 16.9 0 0 1 177.45 -50.85 V0 '
            'M195.45 -50.85 A16.9 16.9 0 0 1 229.25 -50.85 V-20.15 A16.9 16.9 0 0 1 195.45 -20.15 Z '
            'M252 -90 V0 M240.5 -67.75 H267.5 '
            'M273.01 -38.5 H311.05 V-50.85 A16.9 16.9 0 0 0 277.25 -50.85 V-20.15 A16.9 16.9 0 0 0 309.07 -12.22')
WM_W, WM_TOP, WM_BOT, WM_XH = 315.3, -90.0, 24.0, 72.0   # 워드마크 상자 · x높이(아치 꼭대기 선 바깥)


# ───────────── 기하 ─────────────
def f(v, digits=3):
    s = f'{v:.{digits}f}'.rstrip('0').rstrip('.')
    return '0' if s in ('', '-0') else s


SEG = re.compile(r'([MLHVAZ])([^MLHVAZ]*)')
NUM = re.compile(r'-?\d*\.?\d+')


def xf(d, s=1.0, tx=0.0, ty=0.0):
    """절대 좌표 path 를 고르게 키우고 옮긴다(원호 반지름도 같이).

    반올림한 끝점 사이 거리가 반올림한 지름보다 길면 렌더러가 경고하고 반지름을 키운다 —
    반지름은 소수 넷째 자리까지 쓰고, 그래도 짧은 원호는 끝점 거리의 절반으로 올림한다.
    """
    out = []
    cur = start = (0.0, 0.0)
    r = lambda v: float(f(v))
    for c, args in SEG.findall(d):
        n = [float(x) for x in NUM.findall(args)]
        if c in 'ML':
            pts = [(r(n[i] * s + tx), r(n[i + 1] * s + ty)) for i in range(0, len(n), 2)]
            out.append(c + ' '.join(f'{f(x)} {f(y)}' for x, y in pts))
            cur = pts[-1]
            if c == 'M':
                start = pts[0]
        elif c == 'H':
            xs = [r(x * s + tx) for x in n]
            out.append('H' + ' '.join(f(x) for x in xs))
            cur = (xs[-1], cur[1])
        elif c == 'V':
            ys = [r(y * s + ty) for y in n]
            out.append('V' + ' '.join(f(y) for y in ys))
            cur = (cur[0], ys[-1])
        elif c == 'A':
            parts = []
            for i in range(0, len(n), 7):
                rx, ry = round(n[i] * s, 4), round(n[i + 1] * s, 4)
                end = (r(n[i + 5] * s + tx), r(n[i + 6] * s + ty))
                half = math.hypot(end[0] - cur[0], end[1] - cur[1]) / 2
                if rx == ry and rx < half - 1e-9:
                    rx = ry = math.ceil(half * 1e4) / 1e4
                parts.append(f'{f(rx, 4)} {f(ry, 4)} {f(n[i + 2])} {int(n[i + 3])} {int(n[i + 4])} {f(end[0])} {f(end[1])}')
                cur = end
            out.append('A' + ' '.join(parts))
        else:
            out.append('Z')
            cur = start
    return ' '.join(out)


def mark_paths(pillar=-70.85, bottom=0.0):
    """창틀(선) · 빛(칠) path. 원본 좌표 — 왼쪽 아래가 (0, 0), 위가 음수."""
    frame = (f'M4.25 {f(bottom)} V{f(pillar)} A{ARCH_R} {ARCH_R} 0 0 1 38.05 {f(pillar)} V{f(bottom)} '
             f'M38.05 {f(pillar)} A{ARCH_R} {ARCH_R} 0 0 1 71.85 {f(pillar)} V{f(bottom)}')
    lamp = f'M45.5 {f(bottom)} V{f(pillar)} A{LAMP_R} {LAMP_R} 0 0 1 64.4 {f(pillar)} V{f(bottom)} Z'
    return frame, lamp, pillar - ARCH_R - STROKE / 2


def bezier(p, x):
    """cubic-bezier(p) 의 x → y."""
    x1, y1, x2, y2 = p
    lo, hi = 0.0, 1.0
    for _ in range(60):
        t = (lo + hi) / 2
        bx = 3 * (1 - t) ** 2 * t * x1 + 3 * (1 - t) * t ** 2 * x2 + t ** 3
        lo, hi = (t, hi) if bx < x else (lo, t)
    t = (lo + hi) / 2
    return 3 * (1 - t) ** 2 * t * y1 + 3 * (1 - t) * t ** 2 * y2 + t ** 3


def grad(gid, y1, y2, c1=BRAND['lamp_top'], c2=BRAND['lamp_bot']):
    return (f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="0" y1="{f(y1)}" x2="0" y2="{f(y2)}">'
            f'<stop offset="0" stop-color="{c1}"/><stop offset="1" stop-color="{c2}"/></linearGradient>')


def svg(vb_w, vb_h, inner, w=None, h=None, extra=''):
    size = f' width="{f(w)}" height="{f(h)}"' if w else f' width="{f(vb_w)}" height="{f(vb_h)}"'
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {f(vb_w)} {f(vb_h)}"{size}{extra}>{inner}</svg>'


# ───────────── 최종 그림 ─────────────
def launcher_geom():
    L = LAUNCHER
    bottom = (108 - L['ty']) / L['scale']             # 기둥을 캔버스 아래 끝(108)에서 자른다
    fr, lp, top = mark_paths(L['pillar'], bottom)
    s, tx, ty = L['scale'], L['tx'], L['ty']
    return xf(fr, s, tx, ty), xf(lp, s, tx, ty), top * s + ty, STROKE * s


def launcher_background(gid='lbg'):
    return svg(108, 108, f'<defs>{grad(gid, 0, 108, BRAND["icon_top"], BRAND["icon_bot"])}</defs>'
                         f'<path id="background" d="M0 0 H108 V108 H0 Z" fill="url(#{gid})"/>', 108, 108)


def launcher_foreground(gid='lfg'):
    fr, lp, top, sw = launcher_geom()
    return svg(108, 108, f'<defs>{grad(gid, top, LAUNCHER["ty"])}</defs>'
                         f'<path id="frame" d="{fr}" fill="none" stroke="{TOK["accent"]}" stroke-width="{f(sw)}"/>'
                         f'<path id="lamp" d="{lp}" fill="url(#{gid})"/>', 108, 108)


def launcher_monochrome():
    fr, lp, _, sw = launcher_geom()
    return svg(108, 108, f'<path id="frame" d="{fr}" fill="none" stroke="#000000" stroke-width="{f(sw)}"/>'
                         f'<path id="lamp" d="{lp}" fill="#000000"/>', 108, 108)


def splash_geom():
    c, s = SPLASH['canvas'], SPLASH['scale']
    tx, ty = c / 2 - MARK_W * s / 2, c / 2 + MARK_H * s / 2
    fr, lp, top = mark_paths()
    pivot = ((45.5 + 64.4) / 2 * s + tx, ty)
    return xf(fr, s, tx, ty), xf(lp, s, tx, ty), top * s + ty, ty, STROKE * s, pivot


def splash_icon(alpha=1.0, scale_y=1.0, gid='spl', guide=False):
    """끝 그림이 기본 상태다 — 불 켜짐은 lamp 그룹의 scaleY 와 lamp path 의 fillAlpha 만 움직인다."""
    fr, lp, top, base, sw, (px, py) = splash_geom()
    c = SPLASH['canvas']
    g = (f'<circle cx="{c / 2}" cy="{c / 2}" r="{SPLASH["circle"] / 2}" fill="none" stroke="#FF5FD2" '
         f'stroke-dasharray="4 4" stroke-width="1.5"/>') if guide else ''
    t = '' if scale_y == 1 else f' transform="translate({f(px)} {f(py)}) scale(1 {f(scale_y)}) translate({f(-px)} {f(-py)})"'
    return svg(c, c, f'<defs>{grad(gid, top, base)}</defs>{g}'
                     f'<path id="frame" d="{fr}" fill="none" stroke="{TOK["accent"]}" stroke-width="{f(sw)}"/>'
                     f'<g id="lamp_group"{t}><path id="lamp" d="{lp}" fill="url(#{gid})" fill-opacity="{f(alpha)}"/></g>',
               c, c)


def hero_mark(h=None, gid='hm'):
    fr, lp, _ = mark_paths()
    return svg(MARK_W, MARK_H, f'<defs>{grad(gid, 0, MARK_H)}</defs>'
                               f'<path id="frame" d="{xf(fr, 1, 0, MARK_H)}" fill="none" stroke="{TOK["accent"]}" stroke-width="{STROKE}"/>'
                               f'<path id="lamp" d="{xf(lp, 1, 0, MARK_H)}" fill="url(#{gid})"/>',
               h and h * MARK_W / MARK_H, h)


def hero_wordmark(xh=None, color=TOK['text']):
    hh = WM_BOT - WM_TOP
    h = xh and xh * hh / WM_XH
    return svg(WM_W, hh, f'<path id="wordmark" d="{xf(WORDMARK, 1, 0, -WM_TOP)}" fill="none" stroke="{color}" stroke-width="{STROKE}"/>',
               h and h * WM_W / hh, h)


def safe_circle_check():
    """BR-1 ② — 아치 꼭대기 점과 아치 바깥 끝이 안전 원(중심 54 · 54, 반지름 33) 안인가."""
    s, tx, ty, p = LAUNCHER['scale'], LAUNCHER['tx'], LAUNCHER['ty'], LAUNCHER['pillar']
    out = []
    for cx in (21.15, 54.95):
        x, y = cx * s + tx, p * s + ty
        r = (ARCH_R + STROKE / 2) * s
        top = math.hypot(54 - x, 54 - (y - r))
        far = math.hypot(54 - x, 54 - y) + r
        out.append((top, far))
    return out


# ───────────── 장면 ─────────────
FONTS = ('<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>'
         '<link href="https://fonts.googleapis.com/css2?family=Gothic+A1:wght@700&family=Noto+Sans+KR:wght@400;500;600;700'
         '&family=Noto+Sans+Mono:wght@400;600&family=Roboto:wght@400;500;600;700&display=block" rel="stylesheet">')
DEVICE = "Roboto, 'Noto Sans KR', sans-serif"
MONO = "'Noto Sans Mono', monospace"
GOTHIC = "'Gothic A1', " + DEVICE

NL_FROM = [8, 10, 12, 14, 18, 20, 24, 30, 100]   # 안드로이드 14 비선형 글자 배율 2.0 근사(sp → dp)
NL_TO = [16, 20, 24, 26, 30, 34, 36, 38, 100]


def fs(sp, scale=1.0):
    if scale == 1.0:
        return sp
    for i in range(len(NL_FROM) - 1):
        a, b = NL_FROM[i], NL_FROM[i + 1]
        if a <= sp <= b:
            return NL_TO[i] + (NL_TO[i + 1] - NL_TO[i]) * (sp - a) / (b - a)
    return sp * scale


def page(title, w, h, body, extra_css=''):
    return f'''<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>{title}</title>{FONTS}<style>
*{{box-sizing:border-box;margin:0;padding:0}}
html,body{{width:{w}px;height:{h}px;overflow:hidden;background:{TOK["bg"]};color:{TOK["text"]};font-family:{DEVICE};-webkit-font-smoothing:antialiased}}
.abs{{position:absolute}} .row{{display:flex;align-items:center}} .col{{display:flex;flex-direction:column}}
.status{{height:24px;padding:0 16px;font:500 13px {DEVICE};color:{TOK["text"]};display:flex;justify-content:space-between;align-items:center}}
.ov{{font:600 12px {DEVICE};letter-spacing:.08em;color:{TOK["text3"]}}}
.card{{background:{TOK["surface"]};border:1px solid {TOK["border"]};border-radius:14px;padding:15px 16px}}
.chip{{display:inline-block;font:400 13px {DEVICE};color:{TOK["accent"]};background:rgba(110,168,254,.14);border:1px solid rgba(110,168,254,.26);border-radius:8px;padding:1px 8px}}
.cap{{font:400 13px {DEVICE};color:{TOK["text3"]}}}
.glass{{background:rgba(24,31,40,.86);border:1px solid rgba(255,255,255,.09)}}
.note{{position:absolute;font:500 11px {DEVICE};color:#FF5FD2;letter-spacing:.02em}}
{extra_css}
</style></head><body>{body}</body></html>'''


def status_bar(w, color=TOK['text']):
    return (f'<div class="status abs" style="top:0;left:0;width:{w}px;color:{color}"><span>9:41</span>'
            f'<span style="letter-spacing:3px">▴ ◖ ▮</span></div>')


def icon(kind, size=21, color=TOK['text2']):
    p = {'home': 'M4 11 L12 4 L20 11 V20 H14 V14 H10 V20 H4 Z',
         'bookmark': 'M7 4 H17 V20 L12 16 L7 20 Z',
         'stats': 'M5 20 V12 M12 20 V5 M19 20 V9',
         'menu': 'M4 7 H20 M4 12 H20 M4 17 H20',
         'refresh': 'M19 12 A7 7 0 1 1 16.5 6.6 M19 4 V8 H15',
         'back': 'M15 5 L8 12 L15 19',
         'search': 'M11 18 A7 7 0 1 1 11 4 A7 7 0 0 1 11 18 M16 16 L20 20'}[kind]
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24"><path d="{p}" fill="none" stroke="{color}" '
            f'stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>')


NOTES = [('Kotlin', '2026.10.06', '코루틴 취소와 예외'), ('Android', '2026.10.02', 'Room 마이그레이션 정리'),
         ('Compose', '2026.09.27', '리컴포지션을 줄이는 세 가지 습관'), ('CS', '2026.09.20', 'HTTP 캐싱 기초'),
         ('Kotlin', '2026.09.14', 'Flow 와 StateFlow 차이'), ('Android', '2026.09.03', 'WorkManager 재시도 정책')]
REPO = 'study-team/android-notes'
LONG_OWNER = 'study-group-of-the-android-reading-club'
LONG_REPO = 'android-study-notes-' * 4 + 'and-reading-log-2026'
assert len(LONG_OWNER) == 39 and len(LONG_REPO) == 100
LONG = f'{LONG_OWNER}/{LONG_REPO}'

HINT_COMPACT = '읽고 싶은 노트를 고르세요. 왼쪽 위 ☰ 를 누르면 전체 목록이 열립니다.'
HINT_WIDE = '왼쪽 목록에서 읽고 싶은 노트를 고르세요.'


def middle_ellipsis(text, width_dp, size_dp, adv=0.6):
    """모노 글자 폭(0.6em)으로 한 줄에 드는 만큼 — 가운데 말줄임."""
    n = int(width_dp // (size_dp * adv))
    if len(text) <= n:
        return text
    keep = n - 1
    head = (keep + 1) // 2
    return text[:head] + '…' + text[len(text) - (keep - head):]


def hero_html(h, width, state, scale=1.0, compact_width=True):
    logo, xh = HERO_LOGO[h], HERO_TITLE[h]
    parts = []
    if logo:
        parts.append(f'<div id="h-mark" style="height:{logo}px">{hero_mark(logo, gid="hm" + h)}</div><div style="height:16px"></div>')
    parts.append(f'<div id="h-word" style="height:{f(xh * 114 / 72)}px;line-height:0">{hero_wordmark(xh)}</div>')
    if state != 'none':
        size = fs(13, scale)
        text = middle_ellipsis(LONG if state == 'long' else REPO, width, size)
        parts.append(f'<div style="height:8px"></div><div id="h-repo" style="font:400 {f(size)}px/{f(size * 1.65)}px {MONO};'
                     f'color:{TOK["text2"]};white-space:nowrap;overflow:hidden">{text}</div>')
    if logo:
        size = fs(16, scale)
        parts.append(f'<div style="height:8px"></div><div style="font:400 {f(size)}px/{f(size * 1.78)}px {DEVICE};color:{TOK["text2"]}">'
                     f'{HINT_COMPACT if compact_width else HINT_WIDE}</div>')
    return f'<div id="hero" style="padding:8px 0;position:relative">{"".join(parts)}</div>'


def connect_band(scale=1.0):
    return (f'<div class="row" style="min-height:56px;background:rgba(110,168,254,.14);border:1px solid rgba(110,168,254,.26);'
            f'border-radius:12px;padding:12px 16px;gap:12px"><div class="col" style="flex:1;gap:2px">'
            f'<div style="font:700 {f(fs(14, scale))}px/1.5 {DEVICE}">저장소를 연결하세요</div>'
            f'<div style="font:400 {f(fs(13, scale))}px/1.45 {DEVICE};color:{TOK["text2"]}">지금 노트는 이 기기에만 있습니다. 연결하면 저장소의 노트를 받습니다.</div></div>'
            f'<div style="font:700 {f(fs(15.5, scale))}px {DEVICE};color:{TOK["accent"]};white-space:nowrap">연결하기 ›</div></div>')


def note_card(cat, date, title, scale=1.0):
    return (f'<div class="card"><div class="row" style="gap:10px"><span class="chip" style="font-size:{f(fs(13, scale))}px">{cat}</span>'
            f'<span class="cap" style="font-size:{f(fs(13, scale))}px">{date}</span></div>'
            f'<div style="margin-top:10px;font:500 {f(fs(16, scale))}px/{f(fs(16, scale) * 1.375)}px {DEVICE}">{title}</div></div>')


def top_bar(width, h, title, title_font=GOTHIC, menu=True, scale=1.0, back=False, actions=('refresh',)):
    lead = icon('back', 22, TOK['body']) if back else (icon('menu', 22, TOK['body']) if menu else '')
    acts = ''.join(f'<div style="width:48px;display:flex;justify-content:center">{icon(a, 22, TOK["body"])}</div>' for a in actions)
    size = fs(21, scale)
    return (f'<div class="row" style="height:{HEADER[h]}px;width:{width}px;padding:0 4px">'
            + (f'<div style="width:48px;display:flex;justify-content:center">{lead}</div>' if lead else '<div style="width:12px"></div>')
            + f'<div id="bar-title" style="flex:1;font:700 {f(size)}px/{f(size * 1.3)}px {title_font};letter-spacing:-0.01em;'
              f'white-space:nowrap;overflow:hidden;text-overflow:ellipsis;padding-left:4px">{title}</div>{acts}</div>')


def pane(width, height, top):
    rows = ''.join(f'<div class="row" style="justify-content:space-between;padding:10px 8px;font:500 14px {DEVICE};color:{TOK["body"]}">'
                   f'<span>▸ {c}</span><span style="font:400 13px {MONO};color:{TOK["text3"]}">{n}</span></div>'
                   for c, n in (('Android', 12), ('Kotlin', 9), ('Compose', 7), ('CS', 5), ('Git', 3)))
    return (f'<div class="abs col" style="left:0;top:{top}px;width:{width}px;height:{height - top}px;background:{TOK["surface"]};'
            f'border-right:1px solid {TOK["border"]};padding:12px 12px">'
            f'<div class="row" style="height:40px;border-radius:10px;background:{TOK["surface2"]};padding:0 12px;gap:8px;'
            f'color:{TOK["text3"]};font:400 15px {DEVICE}">{icon("search", 18, TOK["text3"])}노트 검색</div>'
            f'<div class="ov" style="margin:18px 8px 6px">목록</div>{rows}</div>')


def nav(width, height, rail):
    items = ''.join(f'<div style="width:46px;height:46px;border-radius:23px;display:flex;align-items:center;justify-content:center;'
                    f'{"background:rgba(110,168,254,.17)" if i == 0 else ""}">{icon(k, 21, TOK["accent"] if i == 0 else TOK["text2"])}</div>'
                    for i, k in enumerate(('home', 'bookmark', 'stats')))
    if rail:
        return (f'<div class="abs glass col" style="right:14px;top:{height / 2 - 85}px;width:62px;padding:8px;gap:8px;'
                f'border-radius:22px;align-items:center">{items}</div>')
    return (f'<div class="abs glass row" style="left:{width / 2 - 95}px;bottom:42px;width:190px;height:60px;border-radius:24px;'
            f'justify-content:space-around;padding:0 10px">{items}</div>')


DEVICES = {  # 이름 → (폭, 높이, 높이 클래스, 너비 클래스)
    'landscape': (800, 360, 'C', 'medium'),
    'portrait': (360, 800, 'M', 'compact'),
    'tablet': (800, 1280, 'E', 'medium'),
}


def home_scene(device, state, scale=1.0, title_font=GOTHIC, a11y=False, show_cards=True):
    w, h, hc, wc = DEVICES[device]
    compact = wc == 'compact'
    rail = hc == 'C' or wc in ('expanded',)
    left = 0 if compact else PANE[wc]
    right_res = 90 if rail else 0
    page_x = PAGE[wc]
    col_w = w - left - right_res - page_x * 2
    body = [status_bar(w)]
    body.append(f'<div class="abs" style="top:24px;left:{left}px">{top_bar(w - left, hc, "MyNote", title_font, menu=compact, scale=scale)}</div>')
    if not compact:
        body.append(pane(left, h, 24))
    inner = []
    if state == 'none':
        inner.append(connect_band(scale) + f'<div style="height:{GAP[wc]}px"></div>')
    inner.append(hero_html(hc, col_w, state, scale, compact))
    inner.append(f'<div class="ov" style="padding:{GAP[wc] + 8}px 0 {4 + GAP[wc]}px;font-size:{f(fs(12, scale))}px">최근 노트</div>')
    if show_cards:
        cols = max(1, int((col_w + GAP[wc]) // (CARD_MIN[wc] + GAP[wc])))
        cards = ''.join(note_card(*n, scale=scale) for n in NOTES)
        inner.append(f'<div style="display:grid;grid-template-columns:repeat({cols},1fr);gap:{GAP[wc]}px">{cards}</div>')
    top = 24 + HEADER[hc] + PADY[hc]
    body.append(f'<div class="abs" style="top:{top}px;left:{left + page_x}px;width:{col_w}px">{"".join(inner)}</div>')
    body.append(f'<div class="abs" style="left:0;right:0;bottom:0;height:90px;background:linear-gradient(transparent,{TOK["bg"]})"></div>')
    body.append(nav(w, h, rail))
    if a11y:
        body.append(a11y_overlay(left + page_x, top, hc, col_w, w))
    return w, h, ''.join(body)


def a11y_overlay(x, y, hc, col_w, w):
    logo, xh = HERO_LOGO[hc], HERO_TITLE[hc]
    wh = xh * 114 / 72
    y0 = y + 8
    boxes = []
    if logo:
        boxes.append((x - 3, y0 - 3, logo * MARK_W / MARK_H + 6, logo + 6, '마크 — 장식 · 읽지 않음(설명 없음)'))
        y0 += logo + 16
    boxes.append((x - 3, y0 - 3, xh * WM_W / WM_XH + 6, wh + 6, '워드마크 — 제목(heading) · 이름 “MyNote”'))
    y0 += wh + 8
    boxes.append((x - 3, y0 - 2, col_w + 6, 21.45 + 4, '저장소 줄 — 글자 · owner/repo 전체(잘려도 전체를 읽음)'))
    out, legend = [], []
    for i, (bx, by, bw, bh, label) in enumerate(boxes, 1):
        out.append(f'<div class="abs" style="left:{f(bx)}px;top:{f(by)}px;width:{f(bw)}px;height:{f(bh)}px;border:1.5px dashed #FF5FD2;border-radius:4px"></div>'
                   f'<div class="note" style="left:{f(bx + bw - 14)}px;top:{f(by - 8)}px;background:#FF5FD2;color:{TOK["bg"]};border-radius:8px;padding:0 5px">{i}</div>')
        legend.append(f'<div style="margin:3px 0"><b style="color:#FF5FD2">{i}</b>  {label}</div>')
    out.append(f'<div class="abs" style="left:16px;width:{w - 32}px;bottom:120px;padding:12px 14px;border-radius:12px;background:#1d1520;'
               f'border:1px solid #FF5FD2;font:500 12.5px/1.5 {DEVICE};color:{TOK["text"]}">{"".join(legend)}'
               f'<div style="margin-top:4px;color:{TOK["text2"]}">읽는 차례: 1 건너뜀 → 2 → 3 → 안내 문장</div></div>')
    return ''.join(out)


def reading_screen(w, h, title_font=GOTHIC):
    ring = (f'<svg width="28" height="28" viewBox="0 0 28 28"><circle cx="14" cy="14" r="11" fill="none" stroke="rgba(255,255,255,.12)" stroke-width="2.4"/>'
            f'<path d="M14 3 A11 11 0 0 1 25 14" fill="none" stroke="{TOK["accent"]}" stroke-width="2.4" stroke-linecap="round"/></svg>')
    return (f'{status_bar(w)}<div class="abs col" style="left:0;top:0;width:{w}px;height:{h}px;align-items:center;justify-content:center;gap:14px">'
            f'<div style="font:700 21px/27.3px {title_font};letter-spacing:-0.01em">MyNote</div>{ring}'
            f'<div class="cap">연결 정보 읽는 중</div></div>')


def splash_scene(alpha, sy, under=None, cover=1.0, w=360, h=800):
    c = SPLASH['canvas']
    layer = (f'<div class="abs" style="left:0;top:0;width:{w}px;height:{h}px;background:{BRAND["night"]};opacity:{f(cover)}">'
             f'<div class="abs" style="left:{(w - c) / 2}px;top:{(h - c) / 2}px;width:{c}px;height:{c}px">{splash_icon(alpha, sy, "s")}</div>'
             f'{status_bar(w)}<div class="abs" style="left:{w / 2 - 54}px;bottom:8px;width:108px;height:4px;border-radius:2px;background:rgba(255,255,255,.55)"></div></div>')
    return w, h, (under or '') + layer


def splash_frames():
    d = MOTION
    t_mid = 0.25
    p_mid = bezier(EASE_ARRIVE, t_mid)
    exit_mid = 1 - bezier(EASE_LEAVE, 0.5)
    rw, rh = 360, 800
    reading = reading_screen(rw, rh)
    _, _, home = home_scene('portrait', 'repo')
    return [
        ('splash-1-off', splash_scene(d['alpha_from'], d['scale_from'])),
        ('splash-2-mid', splash_scene(d['alpha_from'] + (1 - d['alpha_from']) * p_mid, d['scale_from'] + (1 - d['scale_from']) * p_mid)),
        ('splash-3-on', splash_scene(1.0, 1.0)),
        ('splash-4-exit-reading', splash_scene(1.0, 1.0, reading, exit_mid)),
        ('splash-5-exit-home', splash_scene(1.0, 1.0, home, exit_mid)),
    ], p_mid, exit_mid


def mask_shape(kind):
    if kind == 'circle':
        return '<circle cx="54" cy="54" r="36"/>'
    if kind == 'rounded':
        return '<rect x="18" y="18" width="72" height="72" rx="16"/>'
    if kind == 'squircle':
        pts = []
        for i in range(72):
            a = 2 * math.pi * i / 72
            ca, sa = math.cos(a), math.sin(a)
            pts.append((54 + 36 * math.copysign(abs(ca) ** 0.5, ca), 54 + 36 * math.copysign(abs(sa) ** 0.5, sa)))
        return '<path d="M' + ' L'.join(f'{f(x)} {f(y)}' for x, y in pts) + ' Z"/>'
    return '<path d="M54 18 H90 V72 A18 18 0 0 1 72 90 H54 A36 36 0 0 1 18 54 A36 36 0 0 1 54 18 Z"/>'


def masked_icon(kind, size, uid, mono=None):
    fg = launcher_foreground('g' + uid) if not mono else launcher_monochrome()
    fg_inner = re.sub(r'^<svg[^>]*>|</svg>$', '', fg)
    if mono:
        fg_inner = fg_inner.replace('#000000', mono[1])
        bg = f'<rect width="108" height="108" fill="{mono[0]}"/>'
    else:
        bg = re.sub(r'^<svg[^>]*>|</svg>$', '', launcher_background('b' + uid))
    return (f'<svg width="{size}" height="{size}" viewBox="18 18 72 72"><defs><clipPath id="m{uid}">{mask_shape(kind)}</clipPath></defs>'
            f'<g clip-path="url(#m{uid})">{bg}{fg_inner}</g></svg>')


def launcher_sheet():
    w, h = 1180, 640
    layer = lambda inner, label: (f'<div class="col" style="gap:8px"><div style="width:216px;height:216px;background:repeating-conic-gradient(#1a2029 0 25%,#141920 0 50%) 0 0/16px 16px;'
                                  f'position:relative">{inner}</div><div class="cap">{label}</div></div>')
    guide = ('<svg class="abs" style="left:0;top:0" width="216" height="216" viewBox="0 0 108 108">'
             '<rect x="18" y="18" width="72" height="72" fill="none" stroke="#FF5FD2" stroke-dasharray="2 2" stroke-width=".6"/>'
             '<circle cx="54" cy="54" r="33" fill="none" stroke="#5FE0FF" stroke-width=".6"/></svg>')
    big = lambda s: s.replace('width="108" height="108"', 'width="216" height="216"')
    (t1, r1), (t2, r2) = safe_circle_check()
    row1 = ''.join([layer(big(launcher_background('sb')) + guide, '배경 · 108 · 아이콘 바탕 그라디언트 #13203A → #070B15'),
                    layer(big(launcher_foreground('sf')) + guide, f'전경 · 아치 꼭대기 {f(min(t1, t2))} · 바깥 끝 {f(max(r1, r2))} &lt; 33'),
                    layer(f'<div class="abs" style="inset:0;background:#E6EDF3"></div>' + big(launcher_monochrome()).replace('<svg ', '<svg style="position:absolute;left:0;top:0" ') + guide,
                          'monochrome · 전용 그림(단색 칠 · 기둥 같은 자리)'),
                    layer(f'<div class="abs" style="inset:0;background:#0E1217"></div>' + big(launcher_background('sc')).replace('<svg ', '<svg style="position:absolute;left:0;top:0" ')
                          + big(launcher_foreground('sd')).replace('<svg ', '<svg style="position:absolute;left:0;top:0" ') + guide, '합친 것 · 보이는 72 · 안전 원 66')])
    masks = ''.join(f'<div class="col" style="align-items:center;gap:8px">{masked_icon(k, 96, "x" + k)}<div class="cap">{n}</div></div>'
                    for k, n in (('circle', '원'), ('rounded', '둥근 사각'), ('squircle', '스쿼클'), ('teardrop', '물방울')))
    themed = (f'<div class="col" style="align-items:center;gap:8px">{masked_icon("circle", 96, "tl", ("#D9E3FF", "#1B2F57"))}<div class="cap">테마 아이콘 · 밝은</div></div>'
              f'<div class="col" style="align-items:center;gap:8px">{masked_icon("circle", 96, "td", ("#24324C", "#C9DAFF"))}<div class="cap">테마 아이콘 · 어두운</div></div>')
    small = ''.join(f'<div class="col" style="align-items:center;gap:6px">{masked_icon("circle", s, "s" + str(s))}<div class="cap">{s}dp</div></div>' for s in (48, 36))
    body = (f'<div style="padding:28px 32px"><div style="font:700 22px {GOTHIC};margin-bottom:6px">런처 아이콘 — 세 층 · 마스크 · 테마 아이콘</div>'
            f'<div class="cap" style="margin-bottom:18px">분홍 점선 = 보이는 72 · 하늘 원 = 안전 원 66. 기둥은 일부러 마스크 밖으로 흐른다. 그림 원본 svg/launcher-*.svg</div>'
            f'<div class="row" style="gap:24px;align-items:flex-start">{row1}</div>'
            f'<div class="row" style="gap:40px;margin-top:30px;align-items:flex-end">{masks}<div style="width:24px"></div>{themed}<div style="width:24px"></div>{small}</div></div>')
    return w, h, body


def topbar_compare():
    w, h = 360, 330
    rows = []
    for label, font in (('Gothic A1 · 700', GOTHIC), ('기기 글꼴로 떨어짐', DEVICE)):
        rows.append(f'<div style="position:relative;height:150px;border-bottom:1px solid {TOK["border"]}">{status_bar(w)}'
                    f'<div class="abs" style="top:24px;left:0">{top_bar(w, "M", "MyNote", font)}</div>'
                    f'<div class="note" style="left:30px;top:108px">{label} · 줄 높이 27.3 같음</div>'
                    f'<div class="abs" style="left:16px;top:96px;width:328px;height:40px;border-radius:12px;background:{TOK["surface"]}"></div></div>')
    return w, h, ''.join(rows)


def specimen(scale):
    w = 980
    h = 640 if scale == 1.0 else 1120
    sample = 'MyNote 코루틴 취소와 예외 2026'
    rows = []
    for name, sp, lh, ls in TITLE_STYLES:
        size = fs(sp, scale)
        wrap = 'nowrap' if scale == 1.0 else 'normal'
        cells = ''.join(f'<div style="flex:1;min-width:0;position:relative"><div style="font:700 {f(size)}px/{f(size * lh)}px {fam};letter-spacing:{ls}em;'
                        f'outline:1px dashed rgba(255,95,210,.55);white-space:{wrap};overflow:hidden">{sample}</div>'
                        f'<div class="cap" style="margin-top:4px">{lab} · 줄 상자 {size * lh:.1f}</div></div>'
                        for fam, lab in ((GOTHIC, 'Gothic A1'), (DEVICE, '기기 글꼴')))
        rows.append(f'<div style="margin-bottom:22px"><div class="ov" style="margin-bottom:6px">{name} · {sp}sp · 줄 ×{lh}</div>'
                    f'<div class="row" style="gap:28px;align-items:flex-start">{cells}</div></div>')
    keep = ''.join(f'<div style="font:{wgt} {f(fs(sp, scale))}px/1.45 {DEVICE};margin-bottom:6px">{n} · 그대로 — 퀴즈 문항처럼 읽는 문장은 기기 글꼴</div>'
                   for n, sp, wgt in (('headline', 23, 700), ('cardTitle', 16, 500), ('body', 16, 400)))
    body = (f'<div style="padding:28px 32px"><div style="font:700 22px {GOTHIC};margin-bottom:4px">제목 글꼴 스타일 — 글자 배율 {f(scale * 100)}%</div>'
            f'<div class="cap" style="margin-bottom:20px">굵기 700 하나. 점선 = 줄 상자 — 줄 높이를 정해 두어 두 글꼴의 높이가 같다.</div>{"".join(rows)}'
            f'<div class="ov" style="margin:6px 0 8px">목록 밖</div>{keep}</div>')
    return w, h, body


def reader_scene():
    w, h = 360, 800
    title = 'Room 마이그레이션 정리'
    body = (f'{status_bar(w)}<div class="abs" style="top:24px">{top_bar(w, "M", title, GOTHIC, back=True, actions=("bookmark",))}</div>'
            f'<div class="abs" style="top:{24 + 56 + 20}px;left:16px;width:328px">'
            f'<div class="row" style="gap:10px"><span class="chip">Android</span><span class="cap">2026.10.02</span></div>'
            f'<div style="margin-top:12px;font:700 28px/35px {GOTHIC};letter-spacing:-0.015em">{title}</div>'
            f'<div style="margin-top:18px;font:400 16px/1.78 {DEVICE};color:{TOK["body"]}">스키마가 바뀌면 버전을 올리고 옮기는 방법을 적는다. 자동 마이그레이션이 못 하는 것은 열 이름 바꾸기와 나누기다.</div>'
            f'<div style="margin-top:16px;font:700 21px/1.4 {DEVICE};letter-spacing:-0.01em">자동 마이그레이션</div>'
            f'<div style="margin-top:8px;font:400 16px/1.78 {DEVICE};color:{TOK["body"]}">버전 사이에 어노테이션을 달면 Room 이 차이를 계산한다. 본문 제목 블록과 본문은 기기 글꼴 그대로다.</div></div>'
            f'<div class="note" style="left:16px;top:64px;top:{24 + 56 - 4}px">상단 바 · 머리 둘 다 제목 글꼴 — 같은 제목이 한 글꼴</div>')
    return w, h, body


def bookmark_scene():
    w, h = 360, 640
    body = (f'{status_bar(w)}<div class="abs" style="top:24px">{top_bar(w, "M", "북마크", GOTHIC, menu=True, actions=())}</div>'
            f'<div class="abs" style="top:110px;left:16px;width:328px;border-radius:18px;background:{TOK["surface"]};border:1px solid {TOK["border"]};padding:18px">'
            f'<div class="row" style="gap:10px"><span class="chip">Compose</span><span class="cap">2026.09.27</span></div>'
            f'<div style="margin-top:14px;font:700 22px/29px {GOTHIC};letter-spacing:-0.01em">리컴포지션을 줄이는 세 가지 습관</div>'
            f'<div style="margin-top:10px;font:400 14px/1.7 {DEVICE};color:{TOK["text2"]}">상태를 읽는 자리를 람다 안으로 미루면 바뀔 때 다시 그리는 범위가 줄어든다.</div></div>'
            f'<div class="abs" style="top:330px;left:16px;width:328px">'
            + ''.join(f'<div class="row" style="justify-content:space-between;padding:12px 4px;border-bottom:1px solid {TOK["border"]};font:500 15px {DEVICE}">'
                      f'<span>{t}</span><span style="font:600 12.5px {MONO};color:{TOK["text3"]}">{d}</span></div>' for _, d, t in NOTES[:4])
            + '</div><div class="note" style="left:16px;top:96px">큰 카드 제목 = title — 노트 제목이 제목 글꼴</div>')
    return w, h, body


def stats_scene():
    w, h = 360, 560
    card = lambda head, wgt, color, label, size: (
        f'<div style="border-radius:18px;background:{TOK["surface"]};border:1px solid {TOK["border"]};padding:18px;margin-bottom:14px">'
        f'<div class="ov" style="margin-bottom:8px">{label}</div>'
        f'<div style="font:{wgt} {size}px/1.4 {GOTHIC};color:{color};letter-spacing:-0.01em">{head}</div>'
        f'<div class="cap" style="margin-top:8px">노트 31개 · 퀴즈 12회 · 연속 5일</div></div>')
    body = (f'{status_bar(w)}<div class="abs" style="top:24px">{top_bar(w, "M", "통계", GOTHIC, menu=True, actions=())}</div>'
            f'<div class="abs" style="top:100px;left:16px;width:328px">'
            + card('이번 주는 Kotlin 을 가장 많이 읽었습니다', 700, TOK['text'], '요약 · 지금(19sp · 줄 1.4em)', 19)
            + card('지난주에는 Compose 를 가장 많이 읽었습니다', 600, TOK['text2'], '요약 · 지난 것(17sp · W600 요청 → 700 으로 그려짐)', 17)
            + f'</div><div class="note" style="left:16px;top:84px">요약 문장 = title. 지난 것은 굵기 대신 색(text2)으로만 갈린다</div>')
    return w, h, body


COPY_SITES = [  # copy 로 줄 높이를 덮는 자리 — 이름 · 글자 · 크기 sp · 줄 높이 배수 · 줄 수
    ('통계 요약 문장 · title.copy(lineHeight = 1.4.em) · SummaryCard', '이번 주는 Kotlin 을 가장 많이 읽었습니다', 19, 1.4, 2),
    ('연결 입구 제목 · screenTitle.copy(lineHeight = 19 × 1.35) · ConnectEntry', '저장소가 아직 없나요?', 19, 1.35, 1)]
DEVICE_HANGUL_EM = 1.448   # 기기 한글 글꼴 예(Noto Sans CJK KR) ascent + descent
GOTHIC_EM = 1.0            # Gothic A1 ascent + descent(817 + 207 / 1024)


def copy_lineheight_scene():
    w, h = 980, 560
    rows = []
    for label, text, sp, lh, lines in COPY_SITES:
        box = sp * lh * lines
        today = box - (lh - DEVICE_HANGUL_EM) * sp
        gothic_both = box - (lh - GOTHIC_EM) * sp
        width = 300 if lines == 2 else 400
        cells = ''.join(f'<div style="flex:1"><div style="width:{width}px;font:700 {sp}px/{f(sp * lh)}px {fam};letter-spacing:-0.01em;'
                        f'outline:1px dashed rgba(255,95,210,.6)">{text}</div><div class="cap" style="margin-top:6px">{lab}</div></div>'
                        for fam, lab in ((GOTHIC, f'Gothic A1 · 줄 상자 {box:.1f}'), (DEVICE, f'기기 글꼴 · 줄 상자 {box:.1f}')))
        rows.append(f'<div style="margin-bottom:34px"><div class="ov" style="margin-bottom:10px">{label}</div>'
                    f'<div class="row" style="gap:36px;align-items:flex-start">{cells}</div>'
                    f'<div style="margin-top:10px;font:400 13px/1.6 {DEVICE};color:{TOK["text2"]}">기본 스타일의 가운데 · 자르기 없음을 그대로 물려받으면 두 글꼴 모두 {box:.1f}. '
                    f'지금 판(기기 글꼴 · 위아래 자르기)은 {today:.1f} — {today - box:.1f} 짧아진다. '
                    f'자르기를 되살려 덮으면 Gothic A1 이 {gothic_both:.1f} 로 글꼴 따라 {today - gothic_both:.1f} 갈린다.</div></div>')
    body = (f'<div style="padding:28px 32px"><div style="font:700 22px {GOTHIC};margin-bottom:4px">copy 로 줄 높이를 덮는 두 자리</div>'
            f'<div class="cap" style="margin-bottom:22px">점선 = 줄 상자(줄 수 × 줄 높이). 기본 스타일의 LineHeightStyle(Center · Trim.None)이 copy 로 넘어간 모양이다.</div>{"".join(rows)}</div>')
    return w, h, body


def scenes():
    out = [('launcher-sheet', launcher_sheet())]
    frames, _, _ = splash_frames()
    out += frames
    for st in ('repo', 'none', 'long'):
        for dev in ('landscape', 'portrait', 'tablet'):
            out.append((f'hero-{st}-{dev}', home_scene(dev, st)))
    out.append(('hero-repo-portrait-x2', home_scene('portrait', 'repo', 2.0)))
    out.append(('hero-long-portrait-x2', home_scene('portrait', 'long', 2.0)))
    out.append(('hero-a11y-portrait', home_scene('portrait', 'repo', a11y=True)))
    out.append(('title-topbar-compare', topbar_compare()))
    out.append(('title-reading-portrait', (360, 800, reading_screen(360, 800))))
    out.append(('font-specimen-x1', specimen(1.0)))
    out.append(('font-specimen-x2', specimen(2.0)))
    out.append(('font-reader-portrait', reader_scene()))
    out.append(('font-bookmark-portrait', bookmark_scene()))
    out.append(('font-stats-portrait', stats_scene()))
    out.append(('font-copy-lineheight', copy_lineheight_scene()))
    return out


# ───────────── 시트(한 장) ─────────────
def sheet():
    (t1, r1), (t2, r2) = safe_circle_check()
    _, p_mid, exit_mid = splash_frames()
    d = MOTION
    total = d['hold'] + d['dur']
    k = 0.45   # 시간 줄 1ms = 0.45px

    def cell(inner, label, bg=TOK['bg']):
        return f'<figure><div class="tile" style="background:{bg}">{inner}</div><figcaption>{label}</figcaption></figure>'

    def bar(x0, x1, y, label, color):
        return (f'<rect x="{f(x0 * k)}" y="{y}" width="{f((x1 - x0) * k)}" height="16" rx="3" fill="{color}"/>'
                f'<text x="400" y="{y + 12}" font-size="11" fill="{TOK["text2"]}">{label}</text>')
    timeline = ('<svg viewBox="0 0 720 96" width="720" height="96" font-family="sans-serif">'
                + bar(0, d['hold'], 6, f'꺼진 채 붙잡음 {d["hold"]} · 같은 꺼진 값 keyframe 둘(비율 0 · {d["hold"] / total:.6f})', TOK['border'])
                + bar(d['hold'], total, 28, f'불 켜짐 {d["dur"]} · fillAlpha {f(d["alpha_from"])}→1 · scaleY {d["scale_from"]}→1 · MyNoteEasing', TOK['accent'])
                + bar(total, total + d['exit'], 50, f'내려감 {d["exit"]} · 알파 1→0 · 떠남 곡선', TOK['text3'])
                + ''.join(f'<line x1="{f(t * k)}" y1="0" x2="{f(t * k)}" y2="74" stroke="#FF5FD2" stroke-dasharray="2 3"/>'
                          f'<text x="{f(t * k + 3)}" y="90" font-size="10" fill="{TOK["text3"]}">{t}ms</text>' for t in (0, d['hold'], total, total + d['exit']))
                + '</svg>')
    colors = ''.join(f'<tr><td><span class="sw" style="background:{v}"></span></td><td>{n}</td><td><code>{v}</code></td><td>{u}</td></tr>'
                     for n, v, u in (('밤 night', BRAND['night'], '스플래시 바탕'), ('아이콘 바탕 위 iconTop', BRAND['icon_top'], '런처 배경 그라디언트 0'),
                                     ('아이콘 바탕 아래 iconBottom', BRAND['icon_bot'], '런처 배경 그라디언트 1'),
                                     ('주광 위 lampTop', BRAND['lamp_top'], '빛 그라디언트 0 — 마크 · 워드마크 하나로'),
                                     ('주광 아래 lampBottom', BRAND['lamp_bot'], '빛 그라디언트 1'),
                                     ('창틀 = accent', TOK['accent'], '토큰'), ('워드마크 = text', TOK['text'], '토큰')))
    sp = lambda a, s, gid: splash_icon(a, s, gid).replace(f'width="{SPLASH["canvas"]}" height="{SPLASH["canvas"]}"', 'width="150" height="150"')
    tiles = ''.join([
        cell(launcher_background('k1').replace('width="108" height="108"', 'width="150" height="150"'), 'launcher-background'),
        cell(launcher_foreground('k2').replace('width="108" height="108"', 'width="150" height="150"'), 'launcher-foreground', '#151a22'),
        cell(launcher_monochrome().replace('width="108" height="108"', 'width="150" height="150"'), 'launcher-monochrome', '#E6EDF3'),
        cell(sp(0, d['scale_from'], 'k3'), '스플래시 · 처음(빛 0)', BRAND['night']),
        cell(sp(1, 1, 'k4'), '스플래시 · 끝 = 기본 상태', BRAND['night']),
        cell(hero_mark(72, 'k5'), 'hero-mark'),
        cell(hero_wordmark(34), 'hero-wordmark'),
    ])
    return f'''<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>MyNote 브랜드 A</title><style>
:root{{--bg:{TOK["bg"]};--surface:{TOK["surface"]};--border:{TOK["border"]};--text:{TOK["text"]};--text2:{TOK["text2"]};--text3:{TOK["text3"]};--accent:{TOK["accent"]}}}
*{{box-sizing:border-box}} body{{margin:0;background:var(--bg);color:var(--text);font:15px/1.6 -apple-system,'Noto Sans KR',Roboto,sans-serif}}
main{{max-width:1080px;margin:0 auto;padding:40px 16px 64px}} h1{{font-size:28px;margin:0 0 4px}} h2{{font-size:18px;margin:40px 0 12px}}
p,li{{color:var(--text2)}} .grid{{display:flex;flex-wrap:wrap;gap:16px}} figure{{margin:0}}
.tile{{width:170px;height:170px;border:1px solid var(--border);border-radius:14px;display:flex;align-items:center;justify-content:center;overflow:hidden}}
figcaption{{font-size:12px;color:var(--text3);margin-top:6px}} table{{border-collapse:collapse;width:100%;font-size:14px}}
td,th{{border-bottom:1px solid var(--border);padding:8px 10px;text-align:left;vertical-align:top}} th{{color:var(--text3);font-weight:600}}
.sw{{display:inline-block;width:22px;height:22px;border-radius:6px;border:1px solid var(--border)}} code{{color:var(--accent)}}
.scroll{{overflow-x:auto}}
</style></head><body><main>
<h1>MyNote 브랜드 A — 켜 둔 불</h1>
<p>앱에 들어가는 최종 그림 · 값 한 장. 정본은 레포의 docs/design/brand.md 이고, 그림 원본은 labs/brand-design/app/svg/ 다.</p>
<h2>최종 그림</h2><div class="grid">{tiles}</div>
<p>런처 안전 원 66 검사 — 아치 꼭대기 점 {f(min(t1, t2))} · 아치 바깥 끝 {f(max(r1, r2))} (둘 다 33 안). 스플래시 아이콘은 288 캔버스 · 지름 192 원 안에 마크 전체(크기 ×{SPLASH["scale"]}).</p>
<h2>색 — 로고 전용</h2><div class="scroll"><table><tr><th></th><th>이름</th><th>값</th><th>쓰는 곳</th></tr>{colors}</table></div>
<p>로고 전용 색은 UI 면 · 글자에 쓰지 않는다. 앱 바탕은 토큰 bg {TOK["bg"]} 그대로다.</p>
<h2>스플래시 움직임</h2><div class="scroll">{timeline}</div>
<p>objectAnimator {total}ms 하나에 keyframe 셋 — 앞 {d["hold"]} 은 꺼진 값 그대로(startOffset 이 아니다 — 지연 동안은 켜진 기본 상태가 그려진다) · 뒤 {d["dur"]} 은 마지막 keyframe 의 보간기 MyNoteEasing · objectAnimator 보간기는 선형. 테마 windowSplashScreenAnimationDuration 도 {total}.</p>
<p>끝 그림이 그림의 기본 상태다 — 감속 모션 · 12L · 프리뷰에서는 처음부터 끝 그림. 중간 프레임(불 켜짐 25%) 진행도 {f(p_mid)} · 내려감 중간 덮개 {f(exit_mid)}.</p>
<h2>홈 히어로</h2><div class="scroll"><table><tr><th>높이</th><th>마크 높이</th><th>워드마크 x높이</th><th>그 밖</th></tr>
<tr><td>Compact</td><td>없음</td><td>20</td><td>저장소 줄만 · 안내 숨김</td></tr><tr><td>Medium</td><td>46</td><td>24</td><td>저장소 줄 · 안내</td></tr>
<tr><td>Expanded</td><td>54</td><td>28</td><td>저장소 줄 · 안내</td></tr></table></div>
<p>세로 쌓기 · 왼쪽 맞춤: 마크 → 16 → 워드마크 → 8 → 저장소 줄(mono 13 · text2 · 한 줄 · 가운데 말줄임) → 8 → 안내. 워드마크는 그림이라 글자 배율로 커지지 않는다. 워드마크가 제목(heading · 이름 MyNote), 마크는 장식.</p>
<h2>제목 글꼴 스타일</h2><p>Gothic A1 700 하나 — noteTitle(×1.25) · title · sectionTitle · screenTitle(×1.3). 목록 밖 스타일은 그대로.</p>
</main></body></html>'''


# ───────────── 쓰기 ─────────────
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as fh:
        fh.write(text)


def build():
    files = {
        'svg/launcher-background.svg': launcher_background(),
        'svg/launcher-foreground.svg': launcher_foreground(),
        'svg/launcher-monochrome.svg': launcher_monochrome(),
        'svg/splash-icon.svg': splash_icon(),
        'svg/hero-mark.svg': hero_mark(),
        'svg/hero-wordmark.svg': hero_wordmark(),
    }
    for p, t in files.items():
        write(os.path.join(HERE, p), t + '\n')
    sc = scenes()
    for name, (w, h, body) in sc:
        write(os.path.join(HERE, 'scenes', f'{name}.html'), page(name, w, h, body))
    write(os.path.join(HERE, 'sheet.html'), sheet())
    return sc


def chrome():
    exe = os.environ.get('CHROME') or shutil.which('google-chrome') or shutil.which('chromium')
    if not exe:
        sys.exit('Chrome 을 찾지 못했다 — CHROME 환경 변수에 실행 파일을 준다')
    return exe


def shots(sc):
    exe = chrome()
    os.makedirs(os.path.join(HERE, 'shots'), exist_ok=True)
    for name, (w, h, _) in sc:
        src = 'file://' + os.path.join(HERE, 'scenes', f'{name}.html')
        out = os.path.join(HERE, 'shots', f'{name}.png')
        subprocess.run([exe, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=2',
                        f'--window-size={w},{h}', '--virtual-time-budget=6000', f'--screenshot={out}', src],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print('shots/' + name + '.png')


if __name__ == '__main__':
    built = build()
    if '--shots' in sys.argv:
        shots(built)
