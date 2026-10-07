# 글꼴에서 글자 윤곽을 뽑아 glyphs.json 으로 둔다. build.py 는 이 JSON 만 읽는다(fontTools 불필요).
# 필요: pip install fonttools · ../fonts/ 에 Google Fonts 정적 TTF —
#   Archivo Italic(wdth 112, wght 800 · 900) · Hahmlet 500 · 600 · Black Han Sans · Gothic A1 300 · 500
#   (css2 API 를 User-Agent 없이 부르면 .ttf 주소를 준다)
import json, sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
F = '../fonts/'
jobs = {
  'archivo': (F + 'archivo-it-112-800.ttf', 'MyNote'),
  'archivo900': (F + 'archivo-it-112-900.ttf', 'MyNote'),
  'hahmlet': (F + 'hahmlet-600.ttf', 'MyNote공부기록'),
  'hahmlet500': (F + 'hahmlet-500.ttf', 'MyNote공부기록'),
  'bhs': (F + 'blackhansans.ttf', '공부기록'),
  'gothic300': (F + 'gothica1-300.ttf', '공부기록'),
  'gothic500': (F + 'gothica1-500.ttf', '공부기록'),
}
out = {}
for key, (path, text) in jobs.items():
    f = TTFont(path)
    gs = f.getGlyphSet(); cmap = f.getBestCmap()
    os2 = f['OS/2']; head = f['head']; hhea = f['hhea']
    glyphs = {}
    for ch in dict.fromkeys(text):
        name = cmap[ord(ch)]
        pen = SVGPathPen(gs)
        gs[name].draw(TransformPen(pen, (1, 0, 0, -1, 0, 0)))  # y 뒤집기: 기준선 0, 위가 음수
        glyphs[ch] = {'d': pen.getCommands(), 'adv': gs[name].width}
    out[key] = {
        'upm': head.unitsPerEm, 'asc': hhea.ascent, 'desc': hhea.descent,
        'cap': getattr(os2, 'sCapHeight', 0), 'xh': getattr(os2, 'sxHeight', 0), 'glyphs': glyphs,
    }
    print(key, out[key]['upm'], out[key]['cap'], out[key]['xh'], {c: g['adv'] for c, g in glyphs.items()})
json.dump(out, open('glyphs.json', 'w'))
