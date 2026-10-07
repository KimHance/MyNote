# 브랜드 A — 앱에 넣는 최종 그림과 시안

방향 A(켜 둔 불)를 앱 자리마다 굳힌 것이다. 값과 규칙의 정본은 `docs/design/brand.md` 이고, 이 폴더는 그 그림 원본과 시안 사진이다.

```bash
python3 gen_app.py            # svg/ · scenes/ · sheet.html 을 다시 쓴다(표준 라이브러리만)
python3 gen_app.py --shots    # 위 + scenes/*.html 을 headless Chrome 으로 찍어 shots/ 에(환경 변수 CHROME 또는 PATH)
```

## 그림 원본 — `svg/`

좌표는 변환 없이 굳혔다(좌표 소수 셋째 · 원호 반지름 넷째 자리 — 반올림한 끝점 거리보다 지름이 짧은 원호가 없게 생성기가 맞춘다). path 는 `M H V A Z`, 선은 stroke(끝 butt · 이음 miter — VectorDrawable 기본과 같다), 칠은 단색 또는 2정지점 선형 그라디언트(`userSpaceOnUse`)뿐이라 VectorDrawable · AnimatedVectorDrawable · ImageVector 로 그대로 옮긴다. `id` 는 옮길 때의 이름이다.

| 파일 | 캔버스 | 들어갈 곳 | 메모 |
|---|---|---|---|
| `launcher-background.svg` | 108 | `:app` `ic_launcher_background` | 세로 그라디언트 아이콘 바탕 위 → 아래 |
| `launcher-foreground.svg` | 108 | `:app` `ic_launcher_foreground` | 창틀 선 6.255 · 빛 그라디언트 y 30 → 112. 기둥은 캔버스 아래 끝(108)에서 끝난다 |
| `launcher-monochrome.svg` | 108 | `:app` `ic_launcher_monochrome`(새) | 같은 자리 · 단색 칠. 시스템이 색을 입힌다 |
| `splash-icon.svg` | 288 | `:app` 스플래시 AVD 의 그림 | 끝 그림 = 기본 상태. 움직이는 것은 `lamp_group`(scaleY) · `lamp`(fillAlpha) 둘 — 앞 166 꺼진 값 keyframe · 뒤 450 MyNoteEasing · 합 616 |
| `hero-mark.svg` | 76.1 × 92 | `:core:design-system` 히어로 마크 ImageVector | 창틀 = accent 토큰 · 빛 = 로고 전용 색 |
| `hero-wordmark.svg` | 315.3 × 114 | `:core:design-system` 히어로 워드마크 ImageVector | 바탕 사각 없음 · 선 한 색(text 토큰으로 칠한다). x높이 72 = 위에서 18 ~ 90 |

`sheet.html` 은 위 그림 · 색 · 움직임 · 히어로 규칙을 한 장에 모은 것이다(인라인 SVG · 외부 파일 없음).

## 시안 사진 — `shots/`

`scenes/<이름>.html` 을 dp = CSS px 로 그려 2배로 찍었다. 앱 화면 모양은 근사이고(상단 바 · 패널 · 내비는 자리만), 보려는 것은 히어로 · 스플래시 · 제목 글꼴이다. 기기 글꼴은 Roboto + Noto Sans KR, 모노는 Noto Sans Mono 로 흉내 냈다. 200% 는 안드로이드 14 비선형 글자 배율 2.0 의 근사다. 노트 · 저장소 이름은 가짜다.

| 사진 | 무엇 |
|---|---|
| `launcher-sheet` | 세 층 · 보이는 72 · 안전 원 66 · 마스크 넷(원 · 둥근 사각 · 스쿼클 · 물방울) · 테마 아이콘 밝은 · 어두운 · 48 · 36dp |
| `splash-1-off` · `-2-mid` · `-3-on` | 불 켜짐 처음(빛 0 · scaleY 0.8 — 앞 166 동안 그대로) · 불 켜짐 450 의 25% 지점(진행 0.767) · 끝(= 감속 모션 첫 프레임) |
| `splash-4-exit-reading` · `-5-exit-home` | 내려감 90ms 지점(덮개 0.675) — 아래가 읽는 중 화면 · 홈 |
| `hero-{repo,none,long}-{landscape,portrait,tablet}` | 연결 있음 · 없음(연결 안내 띠) · 긴 이름(39 + 100자) × 높이 Compact(800×360) · Medium(360×800) · Expanded(800×1280) |
| `hero-repo-portrait-x2` · `hero-long-portrait-x2` | 글자 배율 200% — 마크 · 워드마크는 그대로, 저장소 줄 · 안내는 커진다 |
| `hero-a11y-portrait` | 접근성 — 마크 장식 · 워드마크 제목(MyNote) · 저장소 줄 전체 글자 |
| `title-topbar-compare` | 홈 상단 바 MyNote — Gothic A1 · 기기 글꼴로 떨어진 경우(높이 같음) |
| `title-reading-portrait` | 읽는 중 화면 MyNote(sectionTitle) |
| `font-specimen-x1` · `-x2` | 제목 글꼴 스타일 넷 × 두 글꼴 — 줄 상자 높이가 같다 · 목록 밖 스타일 |
| `font-reader-portrait` | 리더 — 상단 바와 머리의 같은 노트 제목이 한 글꼴 |
| `font-bookmark-portrait` · `font-stats-portrait` | `title` 을 쓰는 큰 카드 노트 제목 · 통계 요약 문장(19sp · 지난 것 17sp W600 → 700) |
| `font-copy-lineheight` | copy 로 줄 높이를 덮는 두 자리(통계 요약 · 연결 입구 제목) — 물려받은 LineHeightStyle 로 두 글꼴의 줄 상자가 같다 |
