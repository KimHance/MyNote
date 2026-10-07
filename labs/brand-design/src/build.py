"""MyNote 브랜드 보드와 에셋을 만든다.  python3 build.py <출력 폴더>"""
import os, sys
from banners import *

OUT = sys.argv[1] if len(sys.argv) > 1 else 'out'
SLUG = {'a': 'a-lamp', 'b': 'b-highlighter', 'c': 'c-hoedok'}

# ═════════ 에셋 파일 ═════════
def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w').write(text)

def icon_file(inner):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">{inner}</svg>\n'

def write_assets():
    for d in 'abc':
        base = os.path.join(OUT, 'assets', SLUG[d])
        ground = {'a': A['night'], 'b': B['ink'], 'c': C['ink']}[d]
        sym = {'a': a_symbol, 'b': b_symbol, 'c': c_symbol}[d]()
        write(f'{base}/logomark.svg', sym.svg(pad=sym.h * .18, width=round(sym.w * 3)) + '\n')
        wm = {'a': lambda: a_wordmark(), 'b': lambda: b_wordmark(), 'c': lambda: c_wordmark()}[d]()
        write(f'{base}/wordmark.svg', wm.svg(pad=24, bg=ground, width=round(wm.w * 2)) + '\n')
        lh = {'a': a_lockup_h, 'b': b_lockup_h, 'c': c_lockup_h}[d]()
        write(f'{base}/lockup-horizontal.svg', lh.svg(pad=40, bg=ground, width=round(lh.w * 2)) + '\n')
        lv = {'a': a_lockup_v, 'b': b_lockup_v, 'c': c_lockup_v}[d]()
        write(f'{base}/lockup-stacked.svg', lv.svg(pad=48, bg=ground, width=round(lv.w * 1.6)) + '\n')
        write(f'{base}/icon-background.svg', icon_file(icon_bg(d)))
        write(f'{base}/icon-foreground.svg', icon_file(icon_fg(d)))
        write(f'{base}/icon-monochrome.svg', icon_file(icon_fg(d, mono=True, color='#000000')))
        write(f'{base}/notification.svg', f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="24" height="24">'
              + {'a': lambda: a_mark('mono', ink='#FFFFFF'), 'b': lambda: b_mark('mono', c='#FFFFFF'), 'c': lambda: c_mark(red='#FFFFFF')}[d]() + '</svg>\n')
        if d == 'c':
            sv = c_lockup_seal_vertical()
            write(f'{base}/lockup-seal-vertical.svg', sv.svg(pad=40, bg=ground, width=round(sv.w * 2.4)) + '\n')

# ═════════ 방향 설정 ═════════
DIRS = {
 'a': dict(
    name='켜 둔 불', key='A', anchor='lamp',
    idea='언제 돌아와도, 내 노트 위에는 불이 켜져 있다.',
    words=['고요한', '곁에 있는', '정확한', '밤에 강한'],
    style=("--ground:#0A0F1C;--fg:#E6EDF3;--muted:#8D9BB0;--plate:#0E1526;--line2:#1C2740;"
           "--display:'Gothic A1',sans-serif;--display-weight:200;--idea-weight:300;--h3-weight:300;--body:'Gothic A1','Noto Sans KR',sans-serif;"
           "--drench-bg:#6EA8FE;--drench-fg:#0A0F1C;--drench-muted:#1E3A66;--drench-line:rgba(10,15,28,.18);--pbg:#0E1526"),
    why_h='불은 내 방 안에서 켜진다',
    why=('MyNote 는 노트를 내 저장소에서 받아 기기에 두고 읽습니다. 퀴즈도 서버가 아니라 기기 안에서 만듭니다. 이 앱의 빛은 남의 서버가 아니라 내 방 안에 있습니다.',
         '앱이 다크 하나뿐인 것도 같은 장면에서 나왔습니다 — 밤에, 불 하나 켜 두고 읽는 사람. 마크는 그 장면을 그리지 않고 창 하나만 남깁니다.'),
    mark_h='창 둘, 불 하나',
    mark=('MyNote 의 m 을 아치 창 두 개로 지었습니다. 왼쪽 창은 꺼져 있고 <b>오른쪽 창에만 불이 켜져 있습니다</b> — 많은 노트 가운데 지금 읽는 하나.',
          '창틀은 앱 강조색 #6EA8FE 를 그대로 쓰고, 빛은 번지지 않고 창틀 안쪽 3.2 단위 앞에서 정확히 멈춥니다. 단색으로 채우면 아치 둘과 기둥 셋만 남아 엄지손톱 크기에서도 같은 모양입니다.'),
    wm_h='모든 글자를 같은 아치로',
    wm=('워드마크는 글꼴 없이 그렸습니다. m·n·o·e·y 가 모두 <b>같은 아치 반지름 R 과 획 T</b> 로 지어져 단어 전체가 하나의 회랑처럼 읽힙니다. 첫 글자 m 의 오른쪽 창에만 빛이 들어갑니다.',
        '소문자로 쓰는 것은 아치가 소문자에만 있어서입니다. 문장 안에서는 그대로 MyNote 라고 씁니다.'),
    wm_notes=['열주 — m·n·o·e·y 의 곡선은 전부 반지름 16.9', 't 는 아치가 없는 유일한 글자, 기둥 하나와 가로대', 'e 의 끝은 28° 에서 직각으로 자른다', '빛은 m 에만 — 단어에 불은 하나'],
    ko_h='공부 기록 은 Gothic A1 Light 로, 워드마크의 가는 획과 같은 무게에서 맞춥니다.',
    lock_rule='불은 하나만 켭니다. 심볼과 함께 쓰면 워드마크의 창은 끕니다.',
    clear='창 하나의 폭(2R + T)', minimum=[('심볼', '16px · 6mm'), ('워드마크', '폭 64px · 18mm'), ('가로 락업', '폭 120px · 32mm')],
    colors=[('밤', '#0A0F1C', '바탕. 앱 bg #0E1217 보다 한 단 푸르게', '#E6EDF3'),
            ('창틀', '#6EA8FE', '= 앱 accent. 마크의 구조', '#0A0F1C'),
            ('주광', '#F4F8FF → #B9D5FF', '빛. 브랜드 순간에만', '#0A0F1C', 'linear-gradient(#F4F8FF,#B9D5FF)'),
            ('꺼진 창', '#141D30', '보조 면 · 창 격자', '#E6EDF3'),
            ('글자', '#E6EDF3', '= 앱 text', '#0A0F1C')],
    app_rel=('<b>앱 토큰을 거의 그대로 이어받습니다.</b> accent 가 곧 창틀이라 앱 안에서 바꿀 것은 로고 하나뿐입니다. 주광은 아이콘·스플래시·히어로 로고에만 나오고 UI 상태색으로는 쓰지 않습니다.'),
    type_display=("'Gothic A1'", 200, '공부 기록'), type_latin=("'Jost'", 400, 'mynote 0123456789'), type_body=("'Gothic A1'", 400),
    type_note='제목 Gothic A1 200·300 · 숫자와 라틴 보조 Jost · 본문 Gothic A1 400. 앱 UI 는 지금처럼 시스템 글꼴로 둡니다.',
    icon_note='기둥은 일부러 마스크 아래로 흘려 보냅니다. 아이콘 안에서는 글자 m 이 아니라 건물의 창으로 읽히게 하려는 것입니다. 꼭대기 아치는 지름 66 안전 원 안에 있습니다.',
    themed=dict(bg='#D8E2FF', fg='#16305E'),
    splash_note='앱이 켜지는 순간 = 불을 켜는 순간. 창에 빛이 한 번 깜빡이고 켜집니다(0.9초, 한 번). 스플래시 API 의 애니메이션 아이콘(AVD)으로 옮길 수 있는 길이입니다.',
    free_h='불 켜진 날 — 학습 통계 공유 카드', free_note='한 달을 창 35개로. 공부한 날만 불이 켜집니다. 통계 화면의 "공유" 자리에 쓰는 제안이고 숫자는 가짜입니다.',
 ),
 'b': dict(
    name='형광펜', key='B', anchor='highlighter',
    idea='두 번 지나간 자리는 진해진다.',
    words=['선명한', '경쾌한', '손맛 있는', '거침없는'],
    style=("--ground:#EEF23A;--fg:#141519;--muted:#4A4D2A;--plate:#141519;--line2:rgba(20,21,25,.16);"
           "--display:'Black Han Sans',sans-serif;--display-weight:400;--idea-weight:400;--h3-weight:400;--body:'Noto Sans KR',sans-serif;"
           "--drench-bg:#141519;--drench-fg:#F4F5EF;--drench-muted:#A5A89F;--drench-line:rgba(244,245,239,.14);--pbg:#F4F5EF"),
    why_h='다시 읽는 사람의 손버릇',
    why=('공부한 사람이면 누구나 같은 몸짓을 압니다 — 다시 읽을 줄에 형광펜을 긋는 것. MyNote 에는 쓰기가 없지만, 읽기는 고르는 일입니다. 북마크로 다시 찾고 퀴즈로 다시 지나갑니다.',
         '형광펜은 같은 자리를 두 번 지나가면 진해집니다. 복습이 기억에 하는 일이 꼭 그렇습니다.'),
    mark_h='형광펜으로 쓴 M',
    mark=('끌 모양 펜촉(폭 27, 각도 26°)이 지나간 자리로만 M 을 지었습니다. 펜촉이 같은 각도를 지키니 획마다 굵기가 저절로 달라지고, <b>획이 겹친 두 자리만 잉크가 두 번 올라가 진해집니다.</b>',
          '단색으로 채우면 굵고 가는 획의 리듬이 남는 굵은 M 입니다. 겹침은 색이 있을 때만 보이는 덤입니다.'),
    wm_h='Note 에만 긋는다',
    wm=('Archivo Italic(폭 112, 굵기 800)을 바탕으로 자간을 −0.012em 좁히고 My 와 No 를 붙였습니다. <b>Note 위에만 띠를 긋고 글자를 먹으로 뒤집었습니다</b> — 내(My) 노트 위에 그은 한 줄.',
        '띠의 양 끝은 펜촉처럼 비스듬히 자르고, My 와 띠 사이에는 y 꼬리가 닿지 않을 만큼만 띄웁니다.'),
    wm_notes=['띠 — 끌 끝 평행사변형, Note 만 덮는다', 'Note 는 먹, My 는 바탕 반대색', '자간 −0.012em · My · No 붙임', '띠와 y 꼬리 사이 0.26em'],
    ko_h='공부 기록 은 Black Han Sans 로, 띠를 그을 때는 기록 까지 한 번에 긋습니다.',
    lock_rule='칠은 한 번만. 마크와 함께 쓰면 워드마크의 띠를 뺍니다.',
    clear='펜촉 폭(27 단위)', minimum=[('심볼', '16px · 6mm'), ('워드마크', '폭 72px · 20mm'), ('가로 락업', '폭 128px · 34mm')],
    colors=[('형광', '#EEF23A', '마크 · 띠 · 브랜드 바탕', '#141519'),
            ('먹', '#141519', '글자 · 아이콘 바탕', '#F4F5EF'),
            ('한 겹 · 두 겹', '#C2C633 · #E5E939', '먹 위 형광 80% 한 번, 두 번. 따로 만든 색이 아니다', '#141519', 'linear-gradient(90deg,#C2C633 0 50%,#E5E939 50%)'),
            ('종이', '#F4F5EF', '밝은 바탕 · 스티커', '#141519'),
            ('연필', '#8A8F98', '보조 글자', '#141519')],
    app_rel=('<b>노랑은 앱 UI 에 들이지 않습니다.</b> 아이콘·스플래시·히어로 로고에서만 나오고, 앱 accent 파랑은 UI 에 그대로 둡니다. warn #FFB457 · correct #57D9A3 와 색상이 떨어져 있어 상태색과 헷갈리지 않습니다.'),
    type_display=("'Black Han Sans'", 400, '공부 기록'), type_latin=("'Archivo'", 800, 'MyNote 0123456789'), type_body=("'Noto Sans KR'", 400),
    type_note='제목 Black Han Sans · 라틴 Archivo Italic 800 (폭 112) · 본문 Noto Sans KR — 안드로이드 시스템 한글 글꼴과 같은 집안이라 앱과 이어집니다.',
    icon_note='먹 바탕에 형광 M. 겹친 두 자리가 밝게 떠서 48dp 에서도 "칠한 것" 으로 읽힙니다. 펜촉 끝은 모두 지름 66 안전 원 안에 있습니다.',
    themed=dict(bg='#E3E9B8', fg='#2C3300'),
    splash_note='마크가 곧 획 순서라 스플래시에서는 네 획이 그어진 순서대로 칠해집니다(획당 0.17초). 펜이 지나간 순서를 보여 주는 것 말고 다른 움직임은 넣지 않습니다.',
    free_h='스티커 시트', free_note='노트 템플릿 저장소를 공개할 때 같이 나눠 줄 수 있는 스티커. 띠 모양 그대로 칼선을 냅니다.',
 ),
 'c': dict(
    name='회독', key='C', anchor='hoedok',
    idea='다시 읽을수록 내 것이 된다.',
    words=['단단한', '오래가는', '조용히 자부하는', '정직한'],
    style=("--ground:#17130F;--fg:#EEE7DA;--muted:#A89F93;--plate:#1F1A15;--line2:rgba(238,231,218,.12);"
           "--display:'Hahmlet',serif;--display-weight:600;--idea-weight:500;--h3-weight:600;--body:'Hahmlet',serif;"
           "--drench-bg:#D6402A;--drench-fg:#17130F;--drench-muted:#17130F;--drench-line:rgba(23,19,15,.22);--pbg:#EEE7DA"),
    open_style="background:#D6402A;--fg:#17130F;--muted:#17130F;--line2:rgba(23,19,15,.28);color:#17130F",
    why_h='回 — 돌아올 회, 회독의 회',
    why=('한국에서 공부한 사람은 "3회독" 이라는 말을 압니다. 같은 책을 세 번 읽었다는 뜻이고, 그 회(回)는 돌아온다는 뜻입니다. MyNote 가 하는 일이 그것입니다 — 내 저장소의 노트로 몇 번이고 돌아와 읽게 하는 것.',
         '도장은 "이것은 내 것" 이라는 표시입니다. 노트는 사용자의 저장소에 있고 앱은 빌려 읽을 뿐이니, 이 브랜드는 앱의 간판이 아니라 노트 주인의 도장이 됩니다.'),
    mark_h='한 획으로 새긴 回',
    mark=('回 를 한 획으로 새긴 백문(白文) 인장입니다 — 붉은 면에서 선을 파냈습니다. 획은 아래 가장자리를 뚫고 들어와 한 바퀴 돌고, 가운데에는 닫힌 작은 사각이 남습니다. <b>들어와서, 돌고, 남는다.</b>',
          '가장자리는 돌에 새긴 것처럼 변마다 아주 조금씩 휘었습니다. 엄지손톱 크기에서도 "붉은 네모 안의 回" 로 남습니다.'),
    wm_h='서명과 낙관',
    wm=('Hahmlet(한글·라틴을 함께 설계한 세리프) SemiBold 를 바탕으로 <b>M 과 y 의 세리프를 맞닿게 붙이고</b>(−0.034em), o 의 속공간 한가운데에 回 의 안 사각을 인주색으로 찍었습니다.',
        '가로 락업은 서화의 낙관처럼 서명(워드마크) 뒤, 기준선보다 조금 낮게 도장을 찍습니다.'),
    wm_notes=['M·y 세리프 맞닿음 (−0.034em)', 'o 속공간의 인주 사각 = 回 의 안 사각', 'N·o 붙임 (−0.012em), 나머지 +0.006em', '단색에서는 사각도 글자색'],
    ko_h='공부 기록 은 같은 Hahmlet 으로. 세로로 쓰고 아래에 도장을 찍으면 낙관이 됩니다.',
    lock_rule='도장은 서명 오른쪽, 기준선보다 낮게. 세로쓰기에서는 글 아래 가운데.',
    clear='도장 한 변의 1/4', minimum=[('도장', '16px · 5mm'), ('워드마크', '폭 72px · 20mm'), ('가로 락업', '폭 120px · 32mm')],
    colors=[('인주', '#D6402A', '도장 · 브랜드 바탕', '#17130F'),
            ('먹', '#17130F', '글자 · 아이콘 바탕', '#EEE7DA'),
            ('주묵', '#8E2A1E', '도장 그림자 · 겹친 인주', '#EEE7DA'),
            ('한지', '#EEE7DA', '도장이 찍히는 면에만', '#17130F'),
            ('재', '#8C857B', '보조 글자', '#17130F')],
    app_rel=('<b>인주는 로고에만 씁니다.</b> 앱의 wrong #FF7A85 와 같은 붉은 계열이라 UI 상태색으로 쓰면 "틀렸다" 로 읽힙니다. 앱 accent 파랑은 그대로 두고, 파랑 UI 위에 붉은 도장 하나가 서명처럼 놓입니다.'),
    type_display=("'Hahmlet'", 600, '공부 기록'), type_latin=("'Hahmlet'", 500, 'MyNote 0123456789'), type_body=("'Hahmlet'", 400),
    type_note='제목 Hahmlet 600 · 본문 Hahmlet 400 — 한글과 라틴이 한 벌이라 서명과 본문이 같은 손에서 나옵니다. 앱 UI 는 지금처럼 시스템 글꼴로 둡니다.',
    icon_note='먹 바탕에 도장. 도장 네 모서리가 모두 지름 66 안전 원 안에 들도록 한 변 45.8 로 맞췄습니다. 어느 마스크에서도 도장 가장자리가 잘리지 않습니다.',
    themed=dict(bg='#F2DED8', fg='#5C1A0E'),
    splash_note='앱을 여는 일 = 내 노트에 도장을 찍는 일. 도장이 한 번 눌렸다 떨어집니다(0.42초, 한 번). 그 밖의 움직임은 없습니다.',
    free_h='회독 도장 판', free_note='같은 노트를 다시 읽을 때마다 도장이 하나씩 찍히는 카드. 지금 앱에 없는 기능이라 제안으로만 둡니다 — 날짜와 횟수는 가짜입니다.',
 ),
}

GROUND = {'a': A['night'], 'b': B['ink'], 'c': C['ink']}

def mark_main(d):
    return mark_svg(d, label=f'{DIRS[d]["name"]} 마크')

def hero_logo(d, size=46):
    if d == 'c':
        return svgw(c_mark(), '5 5 90 90')
    return masked_icon(d, 'square', size).replace(f'width="{size}" height="{size}"', 'width="100%" height="100%"')

def splash_screen(d):
    ground = GROUND[d] if d != 'a' else A['night']
    if d == 'a':
        ic = masked_icon('a', 'circle', 160)
        brand = a_wordmark(lit=False, frame='#E6EDF3')
    elif d == 'b':
        ic = masked_icon('b', 'circle', 160).replace('fill-opacity="0.8"/>', 'fill-opacity="0.8" class="nib"/>')
        brand = b_wordmark(band=False)
    else:
        ic = svgw(f'<g class="stamp">{c_mark()}</g>', '-12 -12 124 124', 160)
        brand = c_wordmark()
    if d == 'a':
        i = ic.rfind('<path d="'); ic = ic[:i] + '<path class="lit-pane" d="' + ic[i + len('<path d="'):]
    bimg = svgw(brand.body, f'{f2(brand.x)} {f2(brand.y)} {f2(brand.w)} {f2(brand.h)}', 128, style='') if not brand.defs else svgw(f'<defs>{brand.defs}</defs>' + brand.body, f'{f2(brand.x)} {f2(brand.y)} {f2(brand.w)} {f2(brand.h)}', 128)
    return f'<div class="splash" style="background:{ground}">{status_bar()}<div class="ic">{ic}</div><div class="brandimg">{bimg}</div></div>'

def opener(d, cfg):
    words = ''.join(f'<span>{w}</span>' for w in cfg['words'])
    tag = {"a": "밤 · 창 · 주광", "b": "형광 · 먹 · 펜촉", "c": "인주 · 먹 · 回"}[d]
    art = mark_svg(d, label=f'{cfg["name"]} 마크', mode='ink' if d in 'bc' else 'color')
    return (f'<div class="open-band" style="{cfg.get("open_style", "")}"><div class="wrap opener"><div>'
            f'<h2>{cfg["name"]}</h2><p class="idea">{cfg["idea"]}</p><div class="words">{words}</div>'
            f'<div class="who"><b>방향 {cfg["key"]}</b><span>{tag}</span></div></div>'
            f'<div class="art">{art}</div></div></div>')

def head(h, *ps):
    return f'<div class="head"><h3>{h}</h3><div style="display:grid;gap:.9em">' + ''.join(f'<p>{p}</p>' for p in ps) + '</div></div>'

def sheet(inner, cls=''):
    return f'<section class="sheet {cls}"><div class="wrap">{inner}</div></section>'

def ladder(d, bg, label_color):
    sizes = [128, 64, 32, 24, 16]
    light = bg.upper() in ('#F2F3F0',)
    cells = ''.join(f'<div>{mark_svg(d, width=s, mode="light" if light else "color")}<span>{s}</span></div>' for s in sizes)
    return f'<div class="plate pad" style="--pbg:{bg};--plabel:{label_color}"><div class="ladder">{cells}</div></div>'

def mono_pair(d):
    if d == 'a':
        k = svgw(a_mark('mono', ink='#0A0F1C'), '0 0 100 100'); w = svgw(a_mark('mono', ink='#F4F8FF'), '0 0 100 100')
    elif d == 'b':
        k = svgw(b_mark('mono', c='#141519'), '0 0 100 100'); w = svgw(b_mark('mono', c='#F4F5EF'), '0 0 100 100')
    else:
        k = svgw(c_mark(red='#17130F'), '0 0 100 100'); w = svgw(c_mark(red='#EEE7DA'), '0 0 100 100')
    return (f'<div class="cols c2"><div class="plate center pad" style="--pbg:#F2F3F0;aspect-ratio:1"><div style="width:58%">{k}</div><span class="lab" style="color:#5b616b">단색 · 밝은 바탕</span></div>'
            f'<div class="plate center pad" style="--pbg:#0B0C0E;aspect-ratio:1"><div style="width:58%">{w}</div><span class="lab" style="color:#8a909a">단색 · 어두운 바탕</span></div></div>')

def wordmark_plate(d):
    if d == 'a':
        wm = a_wordmark(); bg = A['night']
    elif d == 'b':
        wm = b_wordmark(); bg = B['ink']
    else:
        wm = c_wordmark(); bg = C['ink']
    return f'<div class="plate pad" style="--pbg:{bg};padding:clamp(28px,7vw,110px) clamp(22px,6vw,96px)">{wm.svg(pad=6)}</div>'

def wordmark_light(d):
    if d == 'a':
        wm = a_wordmark(color=False, frame='#0A0F1C', pane_fill='#6EA8FE'); bg = '#EEF2F8'
    elif d == 'b':
        wm = b_wordmark(on_dark=False); bg = B['paper']
    else:
        wm = c_wordmark(fill=C['ink']); bg = C['paper']
    return f'<div class="plate pad center" style="--pbg:{bg};min-height:100%">{wm.svg(pad=4)}</div>'

def korean_plate(d):
    if d == 'a':
        lk = a_lockup_h(); ko = a_korean(40)
        bg = A['night']
    elif d == 'b':
        lk = b_wordmark(); ko = b_korean(40, band=True, fill=B['ink'])
        bg = B['ink']
    else:
        lk = c_wordmark(); ko = c_korean(40)
        bg = C['ink']
    s = 0.62
    W = max(lk.w * s, ko.w) ; gap = 36
    body = lk.at(0, 0, s) + ko.at(0, lk.h * s + gap)
    a = Art(body, 0, 0, W, lk.h * s + gap + ko.h, lk.defs)
    return f'<div class="plate pad center" style="--pbg:{bg};min-height:100%">{a.svg(pad=4)}</div>'

def clear_space(art, unit, bg, line):
    """락업 둘레에 여백 상자."""
    x, y, w, h = art.x - unit, art.y - unit, art.w + 2 * unit, art.h + 2 * unit
    g = (f'<rect x="{f2(x)}" y="{f2(y)}" width="{f2(w)}" height="{f2(h)}" fill="none" stroke="{line}" stroke-width="1.2" stroke-dasharray="5 5" vector-effect="non-scaling-stroke"/>'
         f'<rect x="{f2(art.x)}" y="{f2(art.y)}" width="{f2(art.w)}" height="{f2(art.h)}" fill="none" stroke="{line}" stroke-width=".8" opacity=".5" vector-effect="non-scaling-stroke"/>')
    marks = ''
    for (mx, my) in [(x, y), (x + w - unit, y), (x, y + h - unit), (x + w - unit, y + h - unit)]:
        marks += f'<rect x="{f2(mx)}" y="{f2(my)}" width="{f2(unit)}" height="{f2(unit)}" fill="{line}" opacity=".22"/>'
    vb = f'{f2(x - 6)} {f2(y - 6)} {f2(w + 12)} {f2(h + 12)}'
    return svgw((f'<defs>{art.defs}</defs>' if art.defs else '') + marks + art.body + g, vb)

def lockups(d, cfg):
    if d == 'a':
        lh, lv, unit, bg, line = a_lockup_h(), a_lockup_v(), 2 * R + T, A['night'], '#6EA8FE'
    elif d == 'b':
        lh, lv, unit, bg, line = b_lockup_h(), b_lockup_v(), 27, B['ink'], B['hi']
    else:
        lh, lv, unit, bg, line = c_lockup_h(), c_lockup_v(), 90 * .86 / 4, C['ink'], C['red']
    extra = ''
    if d == 'c':
        sv = c_lockup_seal_vertical()
        extra = (f'<figure style="margin-top:clamp(14px,2vw,24px)"><div class="plate pad" style="--pbg:{C["ink"]};display:grid;grid-template-columns:minmax(0,1fr) minmax(0,3fr);gap:clamp(20px,4vw,56px);align-items:center">'
                 f'<div style="max-width:120px;justify-self:center">{sv.svg(pad=4)}</div>'
                 f'<p class="note" style="color:#A89F93">세로쓰기 낙관 — 노트 템플릿 표지나 인쇄물처럼 세로로 긴 자리에서. 글은 위에서 아래로, 도장은 글 아래 가운데에 글 폭의 0.6 배로 찍습니다.</p></div></figure>')
    mins = ''.join(f'<li><span>{a}</span><span class="num">{b}</span></li>' for a, b in cfg['minimum'])
    return (head('락업', cfg['lock_rule']) +
            f'<div class="cols c7-5"><figure><div class="plate pad" style="--pbg:{bg}">{clear_space(lh, unit, bg, line)}</div><figcaption><b>가로형</b> — 점선 상자가 최소 여백, 모서리 칸이 여백 단위 하나({cfg["clear"]}).</figcaption></figure>'
            f'<figure><div class="plate pad" style="--pbg:{bg}"><div style="max-width:300px;margin:0 auto">{clear_space(lv, unit, bg, line)}</div></div><figcaption><b>세로형</b> — 앱 정보 화면 · 스플래시 아래 · 정사각 자리.</figcaption></figure></div>'
            + extra +
            f'<div class="cols c2" style="margin-top:clamp(28px,4vw,48px)"><div><p class="note" style="margin-bottom:12px"><b>최소 크기</b> — 화면 · 인쇄</p><ul class="rules">{mins}</ul></div>'
            f'<div><p class="note" style="margin-bottom:12px"><b>여백</b></p><ul class="rules"><li><span>단위</span><span>{cfg["clear"]}</span></li><li><span>규칙</span><span>네 변 모두 단위 하나 이상. 사진 위에서는 단위 둘.</span></li></ul></div></div>')

def palette(cfg):
    sw = ''
    for c in cfg['colors']:
        name, hexv, role, fg = c[:4]; bg = c[4] if len(c) > 4 else hexv
        sw += f'<div class="sw"><div class="chip" style="background:{bg}"></div><b>{name}</b><span class="num">{hexv}</span><span>{role}</span></div>'
    return f'<div class="swatches">{sw}</div><p class="note" style="margin-top:20px;max-width:46em">{cfg["app_rel"]}</p>'

def typespec(cfg):
    fd, wd, td = cfg['type_display']; fl, wl, tl = cfg['type_latin']; fb, wb = cfg['type_body']
    ital = 'font-style:italic;font-stretch:112%;' if 'Archivo' in fl else ''
    return (f'<div class="cols c7-5"><div class="spec-type"><p class="big" style="font-family:{fd};font-weight:{wd}">{td}</p>'
            f'<p class="mid" style="font-family:{fl};font-weight:{wl};{ital}">{tl}</p></div>'
            f'<div class="spec-type"><p class="body" style="font-family:{fb};font-weight:{wb};color:inherit;opacity:.86">취소는 협력적입니다. 코루틴은 중단 지점에서만 취소를 확인하므로, 긴 계산 루프 안에서는 직접 확인해야 합니다. Room 마이그레이션은 스키마 버전을 하나씩 올립니다.</p>'
            f'<p class="note" style="margin-top:16px">{cfg["type_note"]}</p></div></div>')

def icon_section(d, cfg):
    bgL = layer_svg(icon_bg(d)); fgL = layer_svg(icon_fg(d), checker=True); full = layer_svg(icon_full(d))
    monoL = layer_svg(icon_fg(d, mono=True, color='#E6EDF3'), checker=True)
    masks = ''.join(f'<div style="display:grid;gap:8px;justify-items:center">{masked_icon(d, s, 96)}<span class="note">{n}</span></div>'
                    for s, n in [('circle', '원'), ('rounded', '둥근 사각'), ('squircle', '스쿼클'), ('teardrop', '물방울')])
    themed = themed_icon(d, 'circle', 96, **cfg['themed'])
    lab = 'style="color:#8a909a"'
    layers = (f'<div class="cols c4">'
              f'<figure><div class="plate" style="--pbg:#0B0C0E">{bgL}</div><figcaption><b>배경</b> 108 × 108</figcaption></figure>'
              f'<figure><div class="plate" style="--pbg:#0B0C0E">{fgL}</div><figcaption><b>전경</b> 안전 원 지름 66</figcaption></figure>'
              f'<figure><div class="plate" style="--pbg:#0B0C0E">{full}</div><figcaption><b>합친 것</b> 분홍 = 보이는 72 · 하늘 = 안전 66</figcaption></figure>'
              f'<figure><div class="plate" style="--pbg:#0B0C0E">{monoL}</div><figcaption><b>monochrome</b> 테마 아이콘용 한 색</figcaption></figure></div>')
    # 홈 화면
    ours_dark = masked_icon(d, 'circle', 48)
    ours_light = masked_icon(d, 'squircle', 48)
    ours_themed = themed_icon(d, 'circle', 48, **cfg['themed'])
    dark = launcher(ours_dark, 'radial-gradient(120% 80% at 20% 10%,#2A3A5C 0%,#141B2B 45%,#07090E 100%)', '#fff', 'circle', 'rgba(255,255,255,.10)')
    light = launcher(ours_light, 'radial-gradient(120% 90% at 80% 0%,#F6E7D4 0%,#DCE3EE 55%,#B9C6D8 100%)', '#16181D', 'squircle', 'rgba(255,255,255,.55)')
    themed_home = launcher(ours_themed, 'linear-gradient(160deg,#EEF0F7,#D9DEEC)', '#1B1F29', 'circle', 'rgba(255,255,255,.6)', themed=cfg['themed'])
    accent = {'a': '#6EA8FE', 'b': '#C9CC1F', 'c': '#D6402A'}[d]
    n_dark = notification(small_icon(d, accent, 16), '#6EA8FE')
    n_light = notification(small_icon(d, {'a': '#2F6BD8', 'b': '#6B6E00', 'c': '#C2391F'}[d], 16), '#2F6BD8', dark=False)
    sb_icons = ''.join(f'<span style="display:inline-flex">{small_icon(d, "#E6EDF3", 15)}</span>' if i == 1 else f'<span style="width:13px;height:13px;border-radius:3px;background:#E6EDF3;opacity:.55;display:inline-block"></span>' for i in range(3))
    return (head('앱 아이콘', cfg['icon_note']) + layers +
            f'<div class="plate pad" style="--pbg:#0B0C0E;margin-top:clamp(14px,2vw,24px);display:flex;flex-wrap:wrap;gap:clamp(18px,3vw,40px);align-items:flex-end;justify-content:space-between">{masks}'
            f'<div style="display:grid;gap:8px;justify-items:center">{themed}<span class="note">테마 아이콘</span></div></div>'
            f'<div class="phones" style="margin-top:clamp(36px,5vw,64px)">'
            f'<figure>{phone(dark, "어두운 배경화면 홈 화면")}<figcaption><b>어두운 배경화면 · 원 마스크</b> — 48dp, 실제 크기.</figcaption></figure>'
            f'<figure>{phone(light, "밝은 배경화면 홈 화면")}<figcaption><b>밝은 배경화면 · 스쿼클 마스크</b> — 48dp, 실제 크기.</figcaption></figure>'
            f'<figure>{phone(themed_home, "테마 아이콘 홈 화면")}<figcaption><b>테마 아이콘(Android 13+)</b> — monochrome 레이어를 시스템이 칠한 모습.</figcaption></figure></div>'
            f'<div class="cols c2" style="margin-top:clamp(28px,4vw,48px);align-items:start">'
            f'<figure><div class="plate pad" style="--pbg:#0B0C0E;display:grid;gap:12px">{n_dark}{n_light}</div><figcaption><b>알림 작은 아이콘</b> — 흰 실루엣 하나(24dp). 시스템이 색을 입힙니다. 퀴즈 모델 내려받기 알림의 실제 문구.</figcaption></figure>'
            f'<figure><div class="plate pad" style="--pbg:#0B0C0E"><div style="display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-radius:14px;background:#000;color:#E6EDF3;font-size:13px"><span class="num">9:41</span><span style="display:flex;gap:8px;align-items:center">{sb_icons}</span></div>'
            f'<div style="margin-top:22px;display:flex;gap:28px;align-items:flex-end">' + ''.join(f'<div style="display:grid;gap:6px;justify-items:center">{small_icon(d, "#E6EDF3", s)}<span class="note num">{s}</span></div>' for s in (48, 24, 16)) +
            f'</div></div><figcaption><b>상태 표시줄</b> — 16px 에서도 실루엣만으로 구별되는지 확인하는 자리.</figcaption></figure></div>')

def applications(d, cfg):
    readme = {'a': a_readme, 'b': b_readme, 'c': c_readme}[d]()
    play = {'a': a_play, 'b': b_play, 'c': c_play}[d]()
    free = {'a': a_free, 'b': b_free, 'c': c_free}[d]()
    logo46 = hero_logo(d, 46); logo54 = hero_logo(d, 54)
    r46 = None if d != 'c' else 0
    home = app_home(logo46, 46, 24)
    if d == 'c':
        home = home.replace('border-radius:13.3px', 'border-radius:0')
    hero54 = hero_block(logo54, 54, 28, radius=r46)
    return (head('적용', '같은 장면이 앱 밖으로 나갔을 때. 화면 문구는 앱의 실제 문구이고, 노트 제목·통계 숫자는 가짜입니다.') +
            f'<div class="phones">'
            f'<figure>{phone(splash_screen(d), "스플래시 화면", cls="splash-phone")}<figcaption><b>스플래시</b> — {cfg["splash_note"]}</figcaption><button class="replay" type="button" data-replay>다시 보기</button></figure>'
            f'<figure>{phone(home, "앱 홈 히어로")}<figcaption><b>앱 홈 히어로 · 46dp</b> — "N" 사각 자리를 그대로 바꿉니다. 제목 24sp.</figcaption></figure>'
            f'<figure style="flex:1 1 280px;max-width:420px"><div style="--s:1">{hero54}</div><figcaption><b>높이가 넉넉한 창 · 54dp</b> — 제목 28sp. 펼친 폴더블 · 태블릿.</figcaption></figure></div>'
            f'<figure style="margin-top:clamp(48px,6vw,88px)"><div class="art-frame">{readme}</div><div class="dims"><span>GitHub README 배너 · 소셜 미리보기</span><span class="num">1280 × 640</span></div></figure>'
            f'<div class="cols c7-5" style="margin-top:clamp(28px,4vw,48px);align-items:start">'
            f'<figure><div class="art-frame">{play}</div><div class="dims"><span>Play 스토어 그래픽 이미지</span><span class="num">1024 × 500</span></div></figure>'
            f'<figure><div class="art-frame">{free}</div><div class="dims"><span>{cfg["free_h"]}</span></div><figcaption>{cfg["free_note"]}</figcaption></figure></div>')

def direction(d):
    cfg = DIRS[d]
    why = ''.join(f'<p>{p}</p>' for p in cfg['why'])
    out = [f'<section class="dir" id="{cfg["anchor"]}" style="{cfg["style"]}" aria-label="방향 {cfg["key"]} {cfg["name"]}">']
    out.append(opener(d, cfg))
    # 왜 — 짙은 면
    out.append(sheet(f'<div class="head" style="margin:0"><h3>{cfg["why_h"]}</h3><div style="display:grid;gap:1em">{why}</div></div>', 'drench manifesto'))
    # 마크
    ground = GROUND[d] if d != 'b' else B['ink']
    cons = {'a': construct_a, 'b': construct_b, 'c': construct_c}[d]()
    cons_bg = {'a': '#0E1526', 'b': '#EEF23A', 'c': '#D6402A'}[d]
    big = f'<div class="plate center" style="--pbg:{ground};aspect-ratio:1"><div style="width:62%">{mark_svg(d)}</div></div>'
    out.append(sheet(head(cfg['mark_h'], *cfg['mark']) +
        f'<div class="cols c2">{big}<div class="plate center" style="--pbg:{cons_bg};aspect-ratio:1"><div style="width:80%">{cons}</div><span class="lab" style="color:{"#0A0F1C" if d != "a" else "#6F86AD"}">구성</span></div></div>'
        f'<div class="cols c2" style="margin-top:clamp(14px,2vw,24px)">{ladder(d, ground, "#8a909a")}{ladder(d, "#F2F3F0", "#5b616b")}</div>'
        f'<div style="margin-top:clamp(14px,2vw,24px)">{mono_pair(d)}</div>', 'tint' if d != 'b' else 'drench'))
    # 워드마크
    notes = ''.join(f'<li><span class="num">{i + 1}</span><span>{n}</span></li>' for i, n in enumerate(cfg['wm_notes']))
    out.append(sheet(head(cfg['wm_h'], *cfg['wm']) + wordmark_plate(d) +
        f'<div class="cols c3" style="margin-top:clamp(14px,2vw,24px);align-items:stretch">{wordmark_light(d)}{korean_plate(d)}'
        f'<div><ul class="rules tight">{notes}</ul><p class="note" style="margin-top:14px">{cfg["ko_h"]}</p></div></div>'))
    # 락업
    out.append(sheet(lockups(d, cfg), 'tint' if d != 'b' else 'drench'))
    # 색 · 글꼴
    out.append(sheet(head('색과 글꼴', '브랜드 팔레트와 앱 토큰의 관계, 제목 · 본문 글꼴 짝.') + palette(cfg) + f'<div style="margin-top:clamp(48px,6vw,88px)">{typespec(cfg)}</div>', 'drench' if d == 'b' else ''))
    # 아이콘
    out.append(sheet(icon_section(d, cfg), 'tint' if d != 'b' else 'drench'))
    # 적용
    out.append(sheet(applications(d, cfg)))
    out.append('</section>')
    return '\n'.join(out)

# ═════════ 표지 · 비교 ═════════
def cover():
    tiles = ''
    for d, bg in [('a', A['night']), ('b', B['hi']), ('c', C['red'])]:
        cfg = DIRS[d]
        m = mark_svg(d, mode='ink' if d in 'bc' else 'color')
        tiles += (f'<a href="#{cfg["anchor"]}"><div class="tile" style="background:{bg}">{m}</div>'
                  f'<div class="cap"><span><b>{cfg["key"]} · {cfg["name"]}</b></span><span>{cfg["idea"]}</span></div></a>')
    return (f'<header class="wrap cover"><h1>MyNote 브랜드 아이덴티티 <span>— 세 방향</span></h1>'
            f'<p class="lede">GitHub 저장소에 마크다운으로 쌓아 둔 학습 노트를 읽는 안드로이드 앱. 아이콘 하나가 아니라, 마크 · 워드마크 · 색 · 글꼴 · 적용까지 한 세계로 묶은 방향 셋입니다.</p>'
            f'<div class="trio">{tiles}</div></header>'
            f'<section class="wrap brief"><h2>지금 자리와 버린 것</h2><div>'
            f'<p>지금 런처 아이콘은 안드로이드 기본 로봇이고, 홈 히어로 로고는 파란 사각에 글자 "N" 을 얹은 자리표시입니다. 앞선 두 번의 시안은 아이콘 세트에서 꺼낸 기능 아이콘처럼 보였습니다.</p>'
            f'<p>그래서 세 방향 모두 같은 것을 하지 않습니다.</p>'
            f'<ul><li><b>아이콘 라이브러리 글리프</b> — 모든 마크를 손으로 그린 path 로</li><li><b>기성 글꼴 글자 하나를 사각에</b> — 글자를 쓰면 그 글자를 지은 규칙까지 브랜드가 된다</li>'
            f'<li><b>책 · 연필 · 문서 · 전구 · 졸업모자</b> — 학습 앱이면 누구나 쓰는 그림</li><li><b>스파클(✦)</b> — 앱 안에서 "기기 안 AI" 전용 표지라 브랜드가 가져가지 않는다</li></ul></div></section>')

def compare():
    cols = ''
    rows = {
      'a': ('앱과 가장 잘 이어집니다. accent 가 곧 창틀이라 앱 안에서 바꿀 것이 로고 하나뿐이고, 밤에 읽는 다크 앱의 장면과 정확히 맞습니다.',
            '셋 중 가장 조용합니다. m 이라는 글자에서 출발해 작은 크기에서는 "글자 m" 으로 읽힐 위험이 있고, 아치는 요즘 흔한 모양입니다.'),
      'b': ('홈 화면에서 가장 먼저 눈에 들어옵니다. 형광 띠는 배너 · 스티커 · 템플릿 어디에나 늘어나는 장치라 시스템으로 가장 넓게 쓰입니다.',
            '앱에는 형광펜 기능이 없습니다 — 읽기 전용 앱이 "칠하기" 를 약속하는 셈입니다. 노랑은 앱 UI 와 따로 놀아 브랜드와 앱이 두 얼굴이 됩니다.'),
      'c': ('제품의 두 사실 — 노트는 사용자 것, 앱은 다시 읽게 한다 — 을 마크 하나가 같이 말합니다. 붉은 네모 하나라 홈 화면에서 가장 또렷하고, 한국 학습자에게 回(회독)는 설명이 필요 없습니다.',
            '인주는 앱 wrong 과 같은 붉은 계열이라 UI 에 들이면 안 됩니다. 回 를 모르는 사람에게는 그냥 "붉은 도장의 미로" — 뜻은 잃어도 모양은 남습니다.'),
    }
    for d, bg in [('a', A['night']), ('b', B['ink']), ('c', C['ink'])]:
        cfg = DIRS[d]; good, bad = rows[d]
        strip = (f'<div style="display:flex;gap:16px;align-items:flex-end;padding:18px 16px;border-radius:6px;background:radial-gradient(120% 80% at 20% 10%,#2A3A5C 0%,#141B2B 45%,#07090E 100%)">'
                 + ''.join(f'<div style="display:grid;gap:5px;justify-items:center;font-size:11px;color:#fff">{fake_icon(i)[0]}<span>{fake_icon(i)[1]}</span></div>' for i in (0, 3))
                 + f'<div style="display:grid;gap:5px;justify-items:center;font-size:11px;color:#fff">{masked_icon(d, "circle", 48)}<span>MyNote</span></div>'
                 + ''.join(f'<div style="display:grid;gap:5px;justify-items:center;font-size:11px;color:#fff">{fake_icon(i)[0]}<span>{fake_icon(i)[1]}</span></div>' for i in (4,)) + '</div>')
        r = {'a': a_readme, 'b': b_readme, 'c': c_readme}[d]()
        cols += (f'<div><div class="plate center" style="--pbg:{bg};aspect-ratio:5/4"><div style="width:42%">{mark_svg(d)}</div></div>'
                 f'<h3 class="cmp-h">{cfg["key"]} · {cfg["name"]}</h3><p>{cfg["idea"]}</p>{strip}<div class="art-frame">{r}</div>'
                 f'<p class="k">강점</p><p>{good}</p><p class="k">약점</p><p>{bad}</p></div>')
    return (f'<section class="compare" id="compare"><div class="wrap"><h2>나란히 놓고 보면</h2>'
            f'<p class="lede">같은 크기, 같은 홈 화면, 같은 배너 자리에 셋을 놓았습니다.</p><div class="cmp">{cols}</div>'
            f'<div class="pick"><h3>추천 — C · 회독</h3><div>'
            f'<p><b>마크 하나가 제품의 사실 둘을 같이 말하는 것은 회독뿐입니다.</b> 노트는 사용자의 저장소에 있고 앱은 빌려 읽는다 — 그래서 도장. 앱이 하는 일은 같은 노트로 몇 번이고 돌아오게 하는 것 — 그래서 回. 켜 둔 불은 장면을, 형광펜은 몸짓을 말하지만, 둘 다 "이 노트가 누구 것인가" 는 말하지 않습니다.</p>'
            f'<p><b>오픈소스와 템플릿 저장소로 넓어질 때도 가장 잘 늘어납니다.</b> 남이 자기 노트 저장소를 만들면 그 저장소에 자기 도장을 찍는 그림이 자연스럽고, 회독 도장 판 · 세로쓰기 낙관처럼 앱 밖에서 쓸 자리가 많습니다.</p>'
            f'<p><b>고를 때 받아들일 것:</b> 인주는 로고에만 쓰고 UI 상태색으로는 쓰지 않는다는 규칙 하나. 앱의 파랑 UI 는 그대로 두고 붉은 도장 하나가 서명처럼 놓입니다. 앱과의 이음새를 가장 적게 바꾸고 싶다면 A · 켜 둔 불이 차선입니다.</p>'
            f'</div></div></div></section>')

FONTS = ("https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;600;700"
         "&family=Gothic+A1:wght@200;300;400;600&family=Jost:wght@400;500"
         "&family=Archivo:ital,wdth,wght@1,112,800&family=Black+Han+Sans&family=Hahmlet:wght@400;500;600&display=swap")

SCRIPT = """
// 화면에 들어온 스플래시만 한 번 움직인다. 기본 상태는 끝난 모습이다.
(function(){
  var phones = document.querySelectorAll('.splash-phone');
  function play(p){ p.classList.remove('play'); void p.offsetWidth; p.classList.add('play'); }
  if ('IntersectionObserver' in window) {
    var io = new IntersectionObserver(function(es){ es.forEach(function(e){ if (e.isIntersecting) { play(e.target); io.unobserve(e.target); } }); }, { threshold: .6 });
    phones.forEach(function(p){ io.observe(p); });
  }
  document.querySelectorAll('[data-replay]').forEach(function(b){
    b.addEventListener('click', function(){ var p = b.parentNode.querySelector('.splash-phone'); if (p) play(p); });
  });
})();
"""

def page():
    css = open(os.path.join(HERE, 'board.css')).read()
    nav = ('<nav class="nav" aria-label="방향"><div class="wrap"><span class="brand">MyNote 브랜드 보드</span>'
           + ''.join(f'<a href="#{DIRS[d]["anchor"]}">{DIRS[d]["key"]} {DIRS[d]["name"]}</a>' for d in 'abc')
           + '<a href="#compare">비교</a></div></nav>')
    body = cover() + ''.join(direction(d) for d in 'abc') + compare()
    foot = '<footer class="foot"><div class="wrap">MyNote 브랜드 아이덴티티 시안 · 2026-10-07 · 노트 제목 · 통계 숫자 · 회독 횟수는 가짜입니다.</div></footer>'
    body = body.replace('</b> — ', '.</b> ')
    return (f'<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
            f'<title>MyNote 브랜드 아이덴티티 — 세 방향</title>'
            f'<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>'
            f'<link href="{FONTS}" rel="stylesheet"><style>{css}</style></head><body>{nav}<main>{body}</main>{foot}<script>{SCRIPT}</script></body></html>')

if __name__ == '__main__':
    write_assets()
    write(os.path.join(OUT, 'index.html'), page())
    print('ok', OUT)
