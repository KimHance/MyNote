# MyNote 브랜드 아이덴티티 — 세 방향

`index.html` 이 브랜드 보드 한 장이다(브라우저로 바로 연다 · 글꼴은 Google Fonts · 외부 스크립트 없음).
방향마다 아이디어 → 마크 → 워드마크 → 락업 → 색·글꼴 → 앱 아이콘 → 적용 순서이고, 맨 끝에 비교와 추천이 있다.
노트 제목 · 날짜 · 통계 숫자 · 회독 횟수는 모두 가짜다.

고른 것은 A(켜 둔 불)다(`docs/spec/brand.md` 결정 1). 앱에 넣는 최종 그림과 시안은 `app/`(정본 `docs/design/brand.md`)이고, 이 보드는 세 방향의 원본으로 남는다.

## 세 방향

| | 이름 | 한 문장 | 마크 | 색 | 글꼴(제목 · 본문) |
|---|---|---|---|---|---|
| A | 켜 둔 불 | 언제 돌아와도, 내 노트 위에는 불이 켜져 있다. | m 을 아치 창 두 개로 — 오른쪽 창에만 불 | 밤 `#0A0F1C` · 창틀 = 앱 accent `#6EA8FE` · 주광 `#F4F8FF→#B9D5FF` | Gothic A1 · Jost / Gothic A1 |
| B | 형광펜 | 두 번 지나간 자리는 진해진다. | 끌 펜촉(폭 27 · 26°)으로 쓴 M — 겹친 자리만 진하다 | 형광 `#EEF23A` · 먹 `#141519` · 종이 `#F4F5EF` | Black Han Sans · Archivo Italic / Noto Sans KR |
| C | 회독 | 다시 읽을수록 내 것이 된다. | 回(회독의 회)를 한 획으로 새긴 백문 인장 | 인주 `#D6402A` · 먹 `#17130F` · 주묵 `#8E2A1E` · 한지 `#EEE7DA` | Hahmlet / Hahmlet |

- **A** 는 앱 토큰을 거의 그대로 잇는다(창틀이 곧 accent). 가장 조용하고, 작은 크기에서 "글자 m" 으로 읽힐 위험이 있다.
- **B** 는 홈 화면에서 가장 먼저 보이고 형광 띠가 배너·스티커로 넓게 늘어난다. 앱에 형광펜 기능이 없어 읽기 전용 앱이 "칠하기" 를 약속하는 셈이 되고, 노랑이 앱 UI 와 따로 논다.
- **C** 는 제품의 두 사실(노트는 사용자 것 → 도장, 다시 읽게 한다 → 回)을 마크 하나가 말한다. 인주는 앱 `wrong #FF7A85` 와 같은 붉은 계열이라 **로고에만** 쓰고 UI 상태색으로 쓰지 않는다는 규칙이 붙는다.

## 추천 — C · 회독

마크 하나가 "누구의 노트인가" 와 "앱이 무엇을 하게 하는가" 를 함께 말하는 것은 C 뿐이다. 붉은 네모 하나라 48dp 홈 화면에서 가장 또렷하고,
오픈소스 · 템플릿 저장소로 넓어질 때 "자기 저장소에 자기 도장" 이라는 그림이 자연스럽다. 앱과의 이음새를 가장 적게 바꾸고 싶다면 A 가 차선이다.

## 파일

```
index.html                         브랜드 보드
assets/a-lamp/ · b-highlighter/ · c-hoedok/
  logomark.svg  wordmark.svg  lockup-horizontal.svg  lockup-stacked.svg
  icon-background.svg  icon-foreground.svg  icon-monochrome.svg   ← 108 × 108, 전경은 지름 66 안
  notification.svg                 알림 작은 아이콘(흰 실루엣, 24 × 24)
  c-hoedok/lockup-seal-vertical.svg  세로쓰기 낙관(C 만)
renders/                           playwright 로 찍은 PNG — board-desktop-1440 · board-mobile-390 · 방향별 장 · compare · assets-contact-sheet
src/                               보드와 에셋을 만드는 스크립트(아래)
```

워드마크 · 락업 · 아이콘 SVG 는 글자까지 윤곽(path)이라 글꼴 없이 열린다. 워드마크 · 락업 파일에는 브랜드 바탕 사각이 들어 있다.

## 앱에 넣은 자리 — A

고른 A 는 `app/svg/` 의 최종 그림으로 앱에 들어갔다(정본 `docs/design/brand.md`). 이 보드의 `assets/a-lamp/` 는 원본으로 남는다.

- **런처 아이콘** — `:app` `drawable/ic_launcher_background.xml` · `ic_launcher_foreground.xml` · `ic_launcher_monochrome.xml`(`app/svg/launcher-*.svg` 를 옮김). `mipmap-anydpi/ic_launcher*.xml` 의 `<monochrome>` 은 전용 그림이다.
- **알림 작은 아이콘** — 바꾸지 않았다(시스템 아이콘 그대로 · 기능 문서 결정 9).
- **홈 히어로** — `:core:design-system` `MyNoteHero` 가 마크 · 워드마크 ImageVector(`MyNoteLogo.kt` · `app/svg/hero-*.svg` 를 옮김)와 연결 저장소 줄(mono · 가운데 말줄임)을 세로로 쌓는다. 높이 Compact 에서는 마크가 없다.
- **스플래시** — 테마 `windowSplashScreenBackground` = `brand_night` · `windowSplashScreenAnimatedIcon` = `splash_icon`(불 켜짐 AVD — 앞 166 꺼진 채 + 450 = 616) · 내려감은 `:app` `SplashExit.kt` 가 그린다.

## 다시 만들기

```bash
cd labs/brand-design/src && python3 build.py ..        # index.html · assets/ 를 다시 쓴다(표준 라이브러리만)
```

글자 윤곽은 `glyphs.json` 에 들어 있다. 다른 글자가 필요하면 `glyphs.py`(fontTools 필요)로 다시 뽑는다 — 글꼴 TTF 경로는 파일 안에 있다.
렌더 PNG 는 이 폴더에 스크립트가 없다(playwright 로 찍었다).

## 하지 않은 것

- 보드를 만들 때는 앱 코드 · `DESIGN.md` · 리소스를 건드리지 않았다. 고른 A 는 위 자리로 들어갔고 브랜드 색 · 로고 규칙은 DESIGN.md 2 · 7 에 옮겨졌다.
- 이 보드의 홈 화면 장면은 HTML 로 그린 것이다 — 앱에 넣은 뒤의 확인은 디자인 문서 `## 확인`.
- 사용자 인터뷰 없이 브리프만으로 만들었다.
