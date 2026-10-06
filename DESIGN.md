# DESIGN.md — MyNote 디자인 시스템

MyNote 디자인 시스템의 **단일 기준 문서**다. 사람과 AI 도구(Claude Code, Claude Design 같은 디자인 에이전트)가
이 문서 하나만 읽고 이 시스템 안에서 새 화면을 만들 수 있게 쓴다.

- 값은 **코드에 있는 것만** 적는다. 코드가 정본이고, 이 문서는 코드를 읽기 쉽게 옮긴 것이다.
  둘이 다르면 코드가 맞다. 이 문서를 고친다.
- 디자인 의도(원칙·사용 규칙·Do/Don't·보이스·접근성)는 디자이너가 쓰고, 토큰·컴포넌트·창 크기·모션의
  사실은 개발자가 코드에서 옮긴다.
- 디자인과 코드가 어긋나는 곳은 고치지 않고 **15절 표에 제안으로만** 남긴다.

---

## 0. 이 문서를 읽는 법

### 0.1 경로 약칭

| 약칭 | 실제 경로 |
|---|---|
| `foundation/` | `core/design-foundation/src/main/java/com/hancekim/mynote/core/designfoundation/` |
| `ds/` | `core/design-system/src/main/java/com/hancekim/mynote/core/designsystem/` |
| `nav/` | `core/navigation/src/main/java/com/hancekim/mynote/core/navigation/` |
| `feat/` | `feature/notes/src/main/java/com/hancekim/mynote/feature/notes/` |

`파일:줄` 표기는 이 문서를 쓴 시점(2026-10-02) 워킹트리 기준이다. 줄 번호는 흘러가지만 이름은 잘 안 바뀐다.
**이름으로 찾는다.**

### 0.2 모듈 두 개가 시스템을 나눠 갖는다

| 모듈 | 가진 것 | 성질 |
|---|---|---|
| `:core:design-foundation` | 토큰 값 — 색·타이포·반경·간격·창 크기(`AdaptiveTokens`) | `@Composable` 0개. 값과 순수 함수뿐. `Color(0x…)` 리터럴이 허용되는 **유일한 자리** |
| `:core:design-system` | `MyNoteTheme`(토큰 배선) · 컴포넌트 · Modifier · 모션 · 아이콘 · 본문 블록 렌더러 · 차트 | 순수 UI. 데이터·도메인·nav3·MVI 를 모른다 |

화살표는 `design-system → design-foundation` 한 방향이다(`api` 의존). feature 는 design-system 만 의존하고
토큰 타입은 그 `api` 로 받는다. 모듈별 세부 규약은 `core/design-foundation/AGENTS.md`,
`core/design-system/AGENTS.md` 에 있다.

### 0.3 표기

- **[통계(진행 중)]** — 통계 2·3단계 작업이 같은 워킹트리에서 진행 중이라 바뀔 수 있는 항목이다(`ds/chart/`, `feat/screen/stats/`).
- 색 값은 코드 리터럴 `0xAARRGGBB` 그대로 적고, 옆에 `#RRGGBB · 불투명도` 를 함께 적는다.

### 0.4 근거 표기

해석이 들어간 문장 끝에 근거를 괄호로 단다. 약어는 Claude Design 프로젝트 `190a5d4b-…` 의 핸드오프다.

| 약어 | 출처 |
|---|---|
| SN | `design_handoff_study_notes/README.md` — 1차 앱(홈·리더·패널) |
| NAV | `design_handoff_nav3_responsive/README.md` — 창 크기 양축·플로팅 내비·씬 전환 |
| QZ | `design_handoff_quiz/README.md` — 퀴즈 |
| ST | `design_handoff_settings/README.md` — 설정·모델 내려받기 |
| BM1 · BM2 · BM3 | `design_handoff_bookmark/` · `_v2/` · `_v3/README.md` — 북마크 v1 · 제안 · 확정 |
| STA | `design_handoff_stats/README.md` — 통계 |
| DS | `Design System.dc.html` — 토큰·프리미티브 스펙시트 |
| TK | `tokens/README.md` — DTCG 토큰 사본 |
| PRD | 레포 `docs/prd/`(8.2 = `backlog.md` 불일치 표). 로컬 전용 |

핸드오프의 숫자를 인용할 때는 근거를 붙인다. 코드 값과 다르면 15절에 있다.

**값은 코드가 원본이다.** 핸드오프·토큰 JSON·Figma 는 사본이다(TK "코드가 원본, 피그마는 사본"). 의도는 핸드오프에서 온다.

---

## 1. 원칙

MyNote 는 GitHub 의 노트(마크다운 · 옛 JSON)를 받아 **읽기만 하는** 학습 노트 앱이다. 사용자는 앱에서 아무것도 쓰지 않는다(SN Overview).
화면이 할 일은 하나 — 노트를 블로그 글처럼 편하게 읽게 하는 것이다. 아래 아홉 가지는 거기서 나온다.

1. **본문이 주인공이고 크롬은 비켜선다.** 내비는 아래로 스크롤하면 숨고(NAV §4-5), 스크롤 끝은 바탕색으로 페이드되고(SN Interactions),
   본문은 읽기 좋은 최대 폭에서 멈춘다(SN Responsive). 새 요소를 넣을 때 먼저 "읽는 동안 이게 보여야 하나" 를 묻는다.
2. **다크 하나, 토큰은 최소.** 라이트 테마가 없다(DS 머리말 "다크 테마 단일, 토큰은 최소"). 색이 모자라 보이면 새 색보다 기존 역할(2.3)을 다시 본다.
3. **강조는 조용하게, 한 가지 색으로.** 강조색은 `accent` 하나다. 상태 *표기*는 회색으로 한다 — 북마크 리본이 파랗지 않은 이유다
   (BM2 1c "회색 리본이라 칩과 다투지 않고", 1a 단점 "accent 남용 … 홈 전체가 파랗게 보입니다").
4. **유리는 떠 있는 크롬에만.** 내비·레일·맨 위로·상태 알약·다이얼로그. 글자를 읽어야 하는 면(본문·카드·선택지·스낵바)은 불투명이다
   (QZ §3.2 "글자 위에 유리를 깔면 읽히지 않는다").
5. **폴더블이 기본이다.** 너비 5단계 × 높이 3단계를 함께 보고(NAV §1), 열 수를 고정하지 않으며(NAV §2),
   한 단/두 단은 클래스 이름이 아니라 **실제로 남는 콘텐츠 폭**으로 정한다(BM3 §5 "너비 클래스가 아니라 실제 남는 폭으로 정합니다").
6. **상태는 정직하게.** 기다림은 남은 양으로 보여 주고(QZ §3.3 "남은 양이 보이면 기다릴 수 있다"), 결과는 상한만 약속하고
   (QZ §3.2 "'3~5문제' 라고 약속하지 않는다"), 실패는 사람 말로 원인과 다음 수를 준다(QZ §3.4).
7. **입구는 숨기지 않는다.** 지금 안 되는 기능도 자리를 지우지 않고 이유와 길을 보인다(QZ §1 "기능 존재를 숨기지 않음",
   ST §10.6, STA §1 "처음부터 다 보입니다"). 단, **만들어진 결과물**이 없으면 자리째 비운다 — 요약이 없으면 요약 카드를 그리지 않는다(STA §3.4).
   기준: 사용자가 *할 수 있는 것*의 입구는 남기고, *아직 없는 것*의 빈 상자는 두지 않는다.
8. **가볍게 들어오고 가볍게 나간다.** 되돌릴 수 있는 동작은 확인을 묻지 않고 바로 하고, 스낵바로 되돌리게 한다
   (BM1 §1.2, QZ §6 "다이얼로그로 붙잡는 것보다 스낵바 복구가 싸다"). 확인 다이얼로그는 되돌릴 수 없는 것(모델 삭제, 학습 기록 지우기)과
   **비용이 큰 시작**(2.6GB 내려받기를 노트 끝 CTA 에서 시작할 때 — ST §10.3)에만 쓴다. 비용을 이미 화면에 적어 둔 자리(설정의 모델 카드)는 확인을 건너뛴다.
9. **문은 하나.** 한 화면에 주 동작·나가는 길이 둘이면 하나를 지운다(QZ §3.6 "문을 두 개 만들지 않는다").

---

## 2. 색

### 2.1 팔레트

**다크 단일 팔레트 하나뿐이다.** 라이트 테마는 없다. 정의는 `foundation/token/MyNoteColors.kt` 의
`data class MyNoteColors`(30개 필드) + `val StudyNotesDarkColors`. 접근은 `MyNoteTheme.colors.<이름>`.

라이트 테마가 없다는 전제 덕에 알파가 섞인 값(`glass`·`scrim`·`navItem*`)을 그대로 쓴다.
대안 팔레트를 추가하면 그 전제가 깨진다.

"쓰는 자리" 는 2026-10-02 코드에서 그 토큰을 실제로 읽는 곳이다.

#### 바탕·면·선

| 이름 | 코드 값 | RGB · 불투명도 | 쓰는 자리 |
|---|---|---|---|
| `bg` | `0xFF0E1217` | `#0E1217` · 100% | 화면 배경(`Surface(color = colors.bg)`), 유리 폴백의 합성 바탕, 페이드 끝색, 표 가로 스크롤 페이드 |
| `surface` | `0xFF161C24` | `#161C24` · 100% | 카드 면(`NoteCard`, 북마크 큰 카드), 고정 패널, 눌림 면 |
| `surface2` | `0xFF1B232D` | `#1B232D` · 100% | 한 단 더 올라온 면 — 스낵바, 검색칸, 코드 블록·인라인 코드 바탕, 표 머리 행 바탕, 모델 카드 진행 막대 트랙(카드 면은 `SettingsCard` 의 `surface`) |
| `border` | `0xFF283039` | `#283039` · 100% | 카드 테두리 1dp, 구분선, 차트 트랙, 표 바깥 테두리·칸 선 |

#### 강조

| 이름 | 코드 값 | RGB · 불투명도 | 쓰는 자리 |
|---|---|---|---|
| `accent` | `0xFF6EA8FE` | `#6EA8FE` · 100% | 가장 많이 쓰는 색(65회). 칩 글자, 선택 탭 아이콘·라벨, 커서, 인용 막대, 인라인 코드 글자(취소선 줄 포함), 스낵바 액션, 주 버튼 면, 차트 1순위 |
| `accentPressed` | `0xFF5B98F0` | `#5B98F0` · 100% | 주 버튼 눌림색(`PressIndication(pressColor = …)`) |
| `accentSoft` | `0x246EA8FE` | `#6EA8FE` · 14% | 칩 바탕, 활성 `NoteRow` 바탕 |
| `accentSoftBorder` | `0x426EA8FE` | `#6EA8FE` · 26% | 칩 테두리(`CategoryChip` 만) |

#### 상태

| 이름 | 코드 값 | RGB · 불투명도 | 쓰는 자리 |
|---|---|---|---|
| `correct` | `0xFF57D9A3` | `#57D9A3` · 100% | 퀴즈 정답, 모델 준비됨 상태 점·알약 |
| `wrong` | `0xFFFF7A85` | `#FF7A85` · 100% | 퀴즈 오답, 실패 상태 |
| `warn` | `0xFFFFB457` | `#FFB457` · 100% | 주의 tone — 모델 알약·카드, 목록 패널 배지 |
| `warnText` | `0xFFD9C4A4` | `#D9C4A4` · 100% | 주의 안내 글자(모델 카드, 퀴즈 풀이) |

`correct`/`wrong` 은 **색만으로 말하지 않는다**는 전제로 만들어졌다(아이콘·글자와 함께 쓴다).
`warn`/`warnText` 는 실패가 아니라 "요청보다 적게 만들어짐" 같은 안내라서 빨강이 아니다.

#### 글자

| 이름 | 코드 값 | RGB · 불투명도 | 쓰는 자리 |
|---|---|---|---|
| `text` | `0xFFE6EDF3` | `#E6EDF3` · 100% | 제목·강조 글자, 본문의 굵은 구간, 표 머리 칸 |
| `bodyText` | `0xFFC8D2DC` | `#C8D2DC` · 100% | 본문 단락·목록 항목·표 칸(`NoteContent`) |
| `text2` | `0xFF9AA7B4` | `#9AA7B4` · 100% | 보조 글자, 비선택 내비 아이콘, 인용, 목록 기호·체크 상자, 북마크 리본 |
| `text3` | `0xFF687585` | `#687585` · 100% | 3순위 — 날짜·캡션·개수·placeholder·섹션 라벨·북마크 글리프 |

#### 유리·오버레이

| 이름 | 코드 값 | RGB · 불투명도 | 쓰는 자리 |
|---|---|---|---|
| `scrim` | `0xA806090C` | `#06090C` · 66% | 퀴즈 오버레이 뒤(`QuizOverlay` 만). Compact 의 목록 드로어 뒤는 이 토큰이 아니라 Material3 기본(검정 32%)이다 — `ModalNavigationDrawer` 에 `scrimColor` 를 주지 않았다 |
| `glass` | `0xA8181F28` | `#181F28` · 66% | 유리 면 기본색 — 내비 바/레일, 상태 알약 |
| `glassPanel` | `0xA81A2230` | `#1A2230` · 66% | 다이얼로그 유리(`QuizSetupDialog`, `ModelDialogs`) — 블러 세기가 달라 따로 있다 |
| `glassOpaque` | `0xEB181F28` | `#181F28` · 92% | `GlassSurface` 에 `backdrop` 이 없을 때의 불투명 폴백 |
| `glassBorder` | `0x17FFFFFF` | `#FFFFFF` · 9% | 폴백 유리의 1dp 테두리 |

#### 내비 선택 알약 (`FloatingNav` 의 `selectionPill` 전용)

| 이름 | 코드 값 | RGB · 불투명도 | 그리는 것 |
|---|---|---|---|
| `navItemSelectedTop` | `0x2B6EA8FE` | `#6EA8FE` · 17% | 알약 세로 그라디언트 위 |
| `navItemSelectedBottom` | `0x0D6EA8FE` | `#6EA8FE` · 5% | 알약 세로 그라디언트 아래 |
| `navItemRim` | `0x2EFFFFFF` | `#FFFFFF` · 18% | 위 가장자리 테두리광(높이 = 반경) |
| `navItemSpecular` | `0x59FFFFFF` | `#FFFFFF` · 35% | 좌상단 스페큘러 점 — "구체로 읽히게" |
| `navItemCaustic` | `0x4DB8D4FF` | `#B8D4FF` · 30% | 아래 가장자리 코스틱 |
| `navItemPressed` | `0x0FFFFFFF` | `#FFFFFF` · 6% | 눌림 덮개. 내비 밖에서도 유리 위 눌림색으로 쓴다(`ScrollToTopButton`, 퀴즈 버튼) |

선택 알약의 다섯 층은 모두 선택 진행도 `t`(0→1)를 알파에 곱해 그린다(`ds/component/FloatingNav.kt` `selectionPill`).

#### 차트 [통계(진행 중)]

| 이름 | 코드 값 | RGB · 불투명도 | 쓰는 자리 |
|---|---|---|---|
| `chartAccent2` | `0xFF4F7FC4` | `#4F7FC4` · 100% | 차트 2순위 계열(도넛·스택 막대·시간 막대) |
| `chartAccent3` | `0xFF3A5A88` | `#3A5A88` · 100% | 차트 3순위 계열 |
| `chartFuture` | `0xFF3A4450` | `#3A4450` · 100% | 아직 오지 않은 칸(`ChartBarKind.Pending`) |

차트의 계열 순서는 `accent` → `chartAccent2` → `chartAccent3` → `text2` → `text3` 다(`ds/chart/CategoryDonut.kt` 프리뷰).

### 2.2 토큰 밖에서 만드는 색

토큰을 그대로 쓰지 않고 **알파만 바꿔** 쓰는 곳이 있다(`colors.X.copy(alpha = …)`). 2026-10-02 기준 26곳
(숫자 리터럴 18곳 + 이름 붙은 상수 8곳, 예: `SELECTED_FILL`·`HINT_FILL_ALPHA`), 원본·알파 조합으로 19가지다.

| 원본 | 쓰는 알파 |
|---|---|
| `accent` | 0.05 · 0.14 · 0.16 · 0.26 · 0.32 · 0.34 · 0.5 |
| `correct` | 0.10 · 0.16 |
| `wrong` | 0.10 · 0.12 |
| `warn` | 0.08 · 0.09 · 0.20 · 0.22 |
| `surface` | 0.55 |
| `surface2` | 0.6 |
| `text3` | 0.35 |
| `bg` | 0 (페이드 끝) |

비활성 상태는 색 토큰이 아니라 컴포넌트 전체 알파 0.4 로 표현한다(`DISABLED_ALPHA`, 설정 화면의 저장 버튼).

이외에 `Color.White`/`Color.Black` 을 직접 쓰는 곳은 그림자 색(`GlassShadow` 의 검정 45%, 씬 전환 그림자), `StatusRing` 트랙(흰색 12%),
아이콘 벡터의 기본 채움(흰색 — `tint` 로 덮어 쓴다)이다.

### 2.3 색 — 역할과 사용 규칙

#### 토큰별 역할

2.1 표의 "쓰는 자리" 가 *지금 어디에 쓰였나* 라면, 이 표는 *어디에 써야 하고 어디에 쓰면 안 되나* 다.

| 토큰 | 쓰는 곳 | 쓰지 않는 곳 |
|---|---|---|
| `bg` | 앱 바닥 · 퀴즈 전체화면 바닥 · **`accent`·`correct`·`wrong` 면 위의 글자**(주 버튼 라벨, 채점 배지 숫자) | 카드·패널 면 |
| `surface` | 카드 · 고정 패널 · 드로어 · 설정 카드 · 차트 카드 · 선택지 기본 · 북마크 큰 카드 · `bg` 위 눌림 | 카드 *안의* 요소(→ `surface2`) |
| `surface2` | 카드 안의 2층 — 코드 블록 · 인라인 코드 · 검색칸 · 아이콘 상자 · 번호 배지 · 보조 버튼 · `surface` 위 눌림 · 스낵바 | **선택 표시**(선택은 `accentSoft`) · 카드 면 자체(눌림과 헷갈린다 — BM2 1b 단점) |
| `border` | 1dp 테두리·구분선 · 0 값 막대 · 진행 바 미도달 칸 · `surface2` 위 눌림 | 글자 색 · 누를 수 있다는 것을 **테두리만으로** 알리기(대비 1.3:1, 10.2) |
| `accent` | ① **지금·여기** — 활성 노트 행, 내비 선택, 현재 칸 ② **진행** — 링·막대·게이지·커서 ③ **주 동작** — 주 버튼 면, 스낵바 액션, AI 샤인, 스파클 ④ **본문 표지** — 카테고리 칩 글자, 인라인 코드 글자, 인용 막대, 로고 ⑤ 차트 첫 계열 | **북마크 표기**(리본·글리프 — 활성 행 위에서도 `text3`, BM1 §4.2) · 경고와 오류 · 넓은 면 채움(주 버튼 제외) · 장식용 강조선(BM2 1a) · 본문 단락 글자 |
| `accentPressed` | 주 버튼 눌림 | 그 밖 전부 |
| `accentSoft` · `accentSoftBorder` | 선택 면·선택 테두리 — 칩, 활성 행, 선택된 설정 줄 | 버튼 면 |
| `correct` · `wrong` | 채점 · 성공/실패 상태 점·링 · `wrong` 은 파괴적 버튼 글자 | **혼자서 뜻을 말하기** — 반드시 ✓/✕ 나 "정답/오답" 글자와 함께(QZ §6) · 장식 |
| `warn` · `warnText` | "실패는 아니지만 알아 둘 것" — 일부만 만들어짐, Wi-Fi 기다림, 재시도, 토큰·모델 없음 배지(QZ §2, ST §10.2) | 오류(→ `wrong`). warn 은 빨강의 약한 버전이 아니다 |
| `text` | 제목 · 강조 · 보조 버튼 라벨 · 굵은 본문 구간 | 긴 본문 단락(→ `bodyText`. 오래 읽으면 눈이 부시다) |
| `bodyText` | 노트 본문 단락 · 발췌 · 상단 바 아이콘 · 설정 줄 라벨 | 제목 |
| `text2` | 보조 문장 · 해설 · 비선택 내비 아이콘 · 북마크 리본·해제 아이콘 · **뜻을 지는 가장 낮은 글자** | 메타(→ `text3`) |
| `text3` | 메타(날짜·개수·시각) · 캡션 · placeholder · 비활성 · 북마크 글리프 · 섹션 라벨 | **읽어야 하는 문장**(surface 위 3.65:1 — 10.2) |
| `scrim` | 모달 뒤 — 퀴즈 설정·다이얼로그, (제안) 드로어 | — (드로어는 지금 M3 기본 — 15절 #9, 모델 다이얼로그는 창 기본 dim — 15절 #22) |
| `glass` 묶음 | 떠 있는 크롬 — 내비·레일·맨 위로·알약(`glass`), 다이얼로그(`glassPanel`), `backdrop` 이 없을 때의 폴백(`glassOpaque`) | 본문·카드·선택지·스낵바 |
| `navItem*` | 플로팅 내비 선택 알약 **전용**(`navItemPressed` 만 유리 위 눌림색으로 밖에서도 쓴다) | 다른 선택 표시(→ `accentSoft`) |
| `chart*` | 차트 **전용** | 글자, 차트 밖 UI |

#### `accent` 를 쓰기 전에 묻는 것

`accent` 는 "누르면 무언가 일어난다" 또는 "지금 여기다" 라고 말한다. 그 뜻이 아니면 쓰지 않는다.
한 화면에 accent 덩어리가 많아지면 어느 것도 강조가 아니다 — 새 accent 를 넣을 때 화면의 다른 accent 하나를 빼도 되는지 본다.

- **컨트롤의 켜짐은 accent, 목록 안의 표시는 회색.** 리더 상단 바의 북마크 토글이 켜지면 accent 로 채운다 — 방금 누른 컨트롤의 상태라서다(BM1 §4.1).
  같은 북마크가 카드·행에 *표기*될 때는 회색이다(BM1 §4.2, BM3 §2.1). 새 상태 표기에도 이 구분을 쓴다.
- 카테고리 칩은 accent 글자지만 **누를 수 없는 라벨**이다. 그래서 칩 모양을 버튼에 빌려 쓰지 않는다(8.8).

#### 알파 파생 — 같은 뜻이면 같은 알파

토큰에 없는 반투명 면은 기준 색에 알파를 곱해 만든다(2.2). 지금 흩어진 값(2.2 표)을 뜻별로 묶으면 다음과 같다. **새 자리는 이 표에서 고른다.**

| 뜻 | 알파 | 지금 쓰인 곳 |
|---|---|---|
| 상태 면(정답·오답·주의 배경) | 8–10% | 정답·오답 선택지 `correct`/`wrong` 0.10, 경고 배지 `warn` 0.09, 알림 거부 안내 `warn` 0.08 |
| 상태 테두리 | 20–22% | 일부만 성공 띠 `warn` 0.22, 알림 거부 안내 `warn` 0.20 |
| 선택 면 | 14% (`accentSoft`) | 칩, 활성 행, 세그먼트 선택 — 16% 로 쓴 곳도 있다(15절 #3 덧붙임) |
| 선택 테두리 | 26% (`accentSoftBorder`) | 칩 — 32% 로 쓴 곳도 있다(같은 곳) |
| 아이콘 상자·상태 아이콘 원 | 12–16% | 실패 원 `wrong` 0.12, 완료 배지 `correct` 0.16 |
| 입력 포커스 테두리 | 50% | 토큰 입력칸 `accent` 0.5 |

#### 눌림 — 면을 밝힌다

눌림은 리플이 아니라 **바탕보다 밝은 면을 까는 것**이다(DS §08 "리플 쓰지 않는다"). 무엇 위에 있느냐로 눌림색이 정해진다. 지금 쓰는 눌림색:

| 놓인 바탕 | 눌림색 | 지금 쓰는 곳 |
|---|---|---|
| `bg` | `surface` | 퀴즈 테두리 버튼("그만두기" — QZ §3.3) |
| `bg`(상단 바) | `surface2` | 상단 바 북마크 토글(BM1 §4.1) |
| `surface` | `surface2` | 목록 패널 설정 줄(ST §2.3) |
| `surface2` 면 · 카드 위 글자 버튼 | `border` | 보조 버튼, 설정의 조용한 버튼, 북마크 해제 버튼(BM1 §3) |
| 유리·투명 | `navItemPressed`(흰 6%) | 맨 위로, 퀴즈 닫기·조용한 버튼(QZ §2 "white 6%") |
| `accent` 면 | `accentPressed` + 0.98 축소 | 주 버튼 |
| 파괴적 글자 버튼 | `wrong` 10% | 지우기·삭제 |

새 자리는 같은 바탕의 줄을 따른다.

검정 덮개로 어둡게 하는 눌림은 쓰지 않는다 — 다크 바탕에서 어두워지면 눌림이 아니라 꺼짐으로 읽힌다(디자이너 판단, 15절 #10).

#### 상태 색 지도

진행 상태를 점·링·막대로 보일 때는 아래 대응을 쓴다(ST §10.2 표에서 일반화).

| 상태 | 색 | 예 |
|---|---|---|
| 끝났고 쓸 수 있음 | `correct` | 사용 가능, 연결됨, 정답 |
| 진행 중 | `accent` | 받는 중, 준비 중, 확인 중, 분석 중 |
| 기다림·재시도(스스로 풀릴 수 있음 — 시점은 약속하지 않음) | `warn` | Wi-Fi 기다리는 중, 받기 끊김 |
| 멈춤·없음(사용자가 움직여야 함) | `text3` | 멈춤, 없음 |
| 실패 | `wrong` | 받지 못함 |

#### 차트 색

- 카테고리 색은 **순위가 아니라 카테고리에 붙는다**(STA §2.1). 기간을 바꿔도 같은 카테고리는 같은 색이어야 한다.
- 이번 칸 `accent`, 지난 칸 `chartAccent2`, 아직 안 온 칸 `chartFuture` 점선(STA §4.1) — "지금" 이 가장 밝다.
- 범례에는 이름과 값을 **항상** 붙인다. 색만으로 계열을 구분하지 않는다(STA §2.1).
- `chartAccent3` 은 surface 위 2.45:1 로 그래픽 기준 3:1 에 못 미친다(10.2, 15절 #16). 범례 글자가 뜻을 지므로 지금은 허용하지만, 막대 하나가 범례 없이 보이는 자리에는 쓰지 않는다.

---

## 3. 타이포그래피

### 3.1 스타일 목록

정의는 `foundation/token/Typography.kt` 의 `data class MyNoteTextStyles`(19개) + `fun myNoteTextStyles(bodySizeSp, heightClass)`.
접근은 `MyNoteTheme.textStyles.<이름>`. 글꼴은 전부 시스템 기본(`FontFamily.Default`)이고 `mono*` 만 `FontFamily.Monospace` 다.
별도 글꼴 파일은 없다.

| 이름 | 크기(sp) | 굵기 | 줄 높이 | 자간 | 쓰는 자리 |
|---|---|---|---|---|---|
| `display` | 52 · 짧은 창 40 | 700 | 크기와 같음 | −0.03em | 퀴즈 결과 점수 |
| `noteTitle` | 28 | 700 | ×1.25 | −0.015em | 리더 노트 제목·홈 히어로. 크기는 3.3 참고 |
| `title` | 24 | 700 | — | −0.01em | 퀴즈 결과 제목, 북마크·통계 화면 제목, 큰 카드 제목 |
| `headline` | 23 | 700 | ×1.45 | −0.01em | 퀴즈 문항 |
| `sectionTitle` | 21 | 700 | — | −0.01em | 상단 바 제목, 퀴즈 실패 제목, 모델 다이얼로그 제목 |
| `screenTitle` | 19 · 짧은 창 17 | 700 | — | −0.01em | 퀴즈 화면 제목, 퀴즈 설정 다이얼로그 제목 |
| `cardTitle` | 16 | 500 | 22sp | — | `NoteCard` 제목, 모델 카드, 퀴즈 CTA, 설정 행 |
| `overline` | 12 | 600 | — | +0.08em | 섹션 라벨(`SectionLabel`) |
| `treeLabel` | 14 | 500 | — | — | 패널 트리 행(`NoteRow`·`CategoryRow`), 번호 행 |
| `caption` | 13 | 400 | — | — | 가장 많이 쓴다(50회). 칩·날짜·차트 라벨·알약 라벨 |
| `mdHeading2` | 본문 + 5 | 700 | — | −0.01em | 본문 블록 제목 level ≤ 2 |
| `mdHeading3` | 본문 + 1 | 600 | — | — | 본문 블록 제목 level ≥ 3 |
| `body` | 16 · 크게 18 | 400 | ×1.78 | — | 본문 단락·목록, 검색칸. 표 칸은 줄 높이만 ×1.5 로 덮어 쓴다(머리 칸 600) |
| `bodyCompact` | 15 | 400 | ×1.5 | — | 퀴즈 풀이 화면 |
| `bodySmall` | 14 | 400 | ×1.7 | — | 다이얼로그 설명, 통계 |
| `button` | 15.5 | 700 | — | — | 버튼 라벨 |
| `mono` | 13 | 400 | ×1.65 | — | 코드 블록, 카테고리 개수 |
| `monoLabel` | 12 | 600 | — | +0.12em | 퀴즈 문항 번호 라벨 |
| `monoSmall` | 12.5 | 600 | — | — | 숫자·시간(알약 trailing, 번호 행, 북마크 시간) |

"×1.25" 는 크기에 곱한 sp 다(예: `noteTitle` 28 × 1.25 = 35sp).

### 3.2 스타일을 바꾸는 입력 두 개

나머지 스타일은 고정값이다. 두 입력이 바꾸는 대상은 겹치지 않는다.

| 입력 | 값 | 바뀌는 스타일 |
|---|---|---|
| 사용자 글자 크기 `MyNoteFontScale` | `Normal`(16sp) / `Large`(18sp). 설정 화면 "본문 글자 크게" 스위치 | `body`, `mdHeading2`(+5), `mdHeading3`(+1) |
| 높이 클래스 `MyNoteHeightClass` | `Compact`(높이 < 480dp)일 때만 | `display` 52→40sp, `screenTitle` 19→17sp |

새 스타일을 넣을 때 "짧은 창에서 줄어야 한다" 면 `short` 분기를, "본문과 함께 커져야 한다" 면 `bodySizeSp` 기반을 쓴다.
섞으면 둘 다 아닌 값이 된다.

### 3.3 히어로·노트 제목 크기는 타이포가 아니라 창 크기 토큰이다

`heroTitleSize`/`noteTitleSize` 는 `AdaptiveTokens` 의 `TextUnit` 이다(5.2). 화면은
`textStyles.noteTitle.copy(fontSize = adaptive.noteTitleSize)` 처럼 합쳐 쓴다.
**이름 붙은 스타일은 타이포에, 폼팩터에 따라 연속으로 변하는 크기는 창 크기 토큰에** 둔다는 분담이다.

### 3.4 스타일 밖에서 정하는 글자 크기

스타일을 받아 `copy(fontSize = …)` 로 덮어 쓰는 곳이 54곳, `fontSize = N.sp` 를 직접 쓰는 곳이 약 55곳 있다
(가장 많은 값 12sp 21회, 15sp 7회, 13sp 6회). 예: `FloatingNav` 선택 라벨은 스타일 없이 12sp/600/−0.01em,
`NumberedRow` 제목은 `treeLabel.copy(15sp, 줄 21sp, 500)`. 15절 참고.

### 3.5 언제 어느 스타일을 쓰나

#### 두 위계

| 위계 | 스타일(큰 것 → 작은 것) | 쓰는 화면 |
|---|---|---|
| **읽기** | `noteTitle`(+창 크기 토큰 크기) → `mdHeading2` → `mdHeading3` → `body`(표 칸 포함) → `mono`(코드) | 노트 리더 본문 |
| **UI** | `display` → `title` → `headline` → `sectionTitle` → `screenTitle` → `cardTitle` → `treeLabel` → `caption` → `overline` · `monoLabel` · `monoSmall` | 그 밖 전부 |

스타일은 **크기가 아니라 역할로 고른다.** 3.1 표의 "쓰는 자리" 와 같은 역할이면 같은 스타일이다 — 화면 큰 제목은 `title`, 상단 바 제목은 `sectionTitle`, 다이얼로그 제목은 `screenTitle`(지금 둘로 갈려 있다 — 15절 #25), 카드·행 제목은 `cardTitle`/`treeLabel`, 메타는 `caption`, 묶음 이름은 `overline`.
비슷한 크기가 필요하다고 다른 역할의 스타일을 빌리지 않는다. 빌리면 그 스타일을 고칠 때 엉뚱한 화면이 같이 바뀐다.

#### 규칙

- **"본문 글자 크게" 는 읽기 위계만 키운다**(`body`·`mdHeading2`·`mdHeading3` — 3.2). UI 글자는 그대로다. 크롬이 본문과 함께 커지면 읽을 자리가 줄어든다.
  그래서 새 스타일이 "본문의 일부" 인지 "UI" 인지 먼저 정한다. 본문 안에 들어가는 것(인용·목록·코드)만 읽기 위계다.
- **본문 안 치수는 em 으로 잰다.** 1em = `body.fontSize` 를 dp 로 한 번 바꾼 값이고, 표 열 폭(목표 16em · 최소 6em) · 체크 상자(칸 1.625em · 아이콘 1.25em)는 거기에 곱한다. `(n × fontSize)` 를 바꾸면 비선형 글자 배율(API 34+)에서 값이 달라진다. 목록 기호 칸은 상수가 아니라 기호 글자를 잰다. 선 · 여백 · 페이드는 dp 다.
- **짧은 창(높이 Compact)은 큰 숫자·제목만 줄인다**(`display`·`screenTitle` — 3.2). 본문은 줄이지 않는다 — 짧은 창일수록 한 줄에 담기는 글이 소중하다.
- **숫자·시각·코드·카운터는 모노**다. 자릿수가 고정돼 정렬이 맞고, "읽는 문장이 아니라 값" 이라는 신호가 된다(BM3 §3.3 시각, §3.4 번호, STA §4.1 값 라벨).
- **섹션 라벨은 작고 조용하다**(`overline` + `text3`). 제목처럼 키우지 않는다 — 묶음의 이름이지 내용이 아니다.
- **굵기는 세 단**만 보이게 한다: 400 본문 · 500 라벨과 카드 제목 · 700 제목과 주 버튼(600 은 라벨·모노 강조). 한 화면에 네 단이 보이면 하나를 줄인다.
- 줄 높이는 "얼마나 오래 읽나" 로 고른다: 긴 읽기 `body`(×1.78) · 해설·설명 `bodySmall`(×1.7) · 선택지 `bodyCompact`(×1.5) · 제목은 1.25–1.45. 한 줄짜리 UI 글자에 긴 읽기 줄 높이를 주면 버튼이 부푼다.

#### 크기 하한

- **문장은 14sp 이상**(QZ §2 "최소 본문 14sp"). 12–13sp(`caption`·`overline`·`mono*`)는 메타·라벨·캡션에만 쓴다.
- 10–11sp 는 차트 값 라벨·범례 표시·배지처럼 **옆의 더 큰 글자가 뜻을 지는 자리**에만(STA §3.5, §4.1).

#### 덮어쓰기

- 화면 코드에서 `copy(fontSize = …)` 로 크기를 바꾸지 않는다. 컴포넌트 안에서 한 번 정하고 상수로 이름을 붙인다.
- 같은 `copy` 가 세 곳이면 스타일로 올린다(13.0). 지금 `12sp` 계열 라벨이 그 상태다(15절 #4).
- 한 화면 전용 스케일을 그 화면에 두지 않는다(13.2).

---

## 4. 간격·반경·표면

### 4.1 반경

`foundation/token/MyNoteRadius.kt` 의 `object MyNoteRadius`. 접근은 `MyNoteRadius.<이름>`(테마를 거치지 않는 상수).

| 이름 | 값 | 쓰는 자리 |
|---|---|---|
| `card` | 14dp | `NoteCard` |
| `list` | 10dp | 검색칸, 코드 블록 |
| `chip` | 8dp | `CategoryChip`, `NoteRow` |
| `inlineCode` | 5dp | **쓰는 곳 없음** — 인라인 코드는 `SpanStyle(background = …)` 라 반경을 줄 수 없다 |

**토큰 밖 반경이 더 많다.** 컴포넌트·화면이 `private val …RADIUS` 나 `RoundedCornerShape(N.dp)` 로 직접 정한다.

| 값 | 어디 |
|---|---|
| 30dp | 씬 전환 중 앞 화면 모서리(`SceneMotion.FrontCornerRadius`) |
| 26dp | 다이얼로그(`QuizSetupDialog`, `ModelDialogs`) |
| 24dp | 플로팅 내비 바 |
| 23dp | 내비 바 항목 알약, 맨 위로 버튼(46dp 원) |
| 22dp | 플로팅 내비 레일 |
| 20dp | 스낵바 |
| 20dp(높이/2) | 상태 알약(40dp) |
| 18dp | 큰 카드 — 퀴즈 CTA, 북마크 큰 카드, 통계 카드. 퀴즈 풀이 상단 바 |
| 15dp | 레일 항목, 퀴즈 주·보조 버튼(높이 50dp), 북마크 빈 상태 타일 |
| 14dp | 통계 헤더 띠, 퀴즈 생성 슬롯 행 |
| 13dp | 퀴즈 설정의 세그먼트(물방울 유리) |
| 12dp | **가장 흔한 리터럴** — 설정 행·입력칸, 목록 패널 행, 북마크 해제 버튼, 상단 바 토글, 퀴즈 칩·버튼 |
| 10dp · 9dp · 8dp · 7dp | 작은 칩·배지·세그먼트 항목 |
| 4dp · 3dp · 2dp | 막대 끝(차트·진행 막대) |

### 4.2 간격

간격 단계 토큰은 없다(쓰는 곳이 0이던 `Spacing` 은 2026-10-07 정리 때 지웠다 — 15절 #1). 실제 여백은 dp 리터럴과 창 크기 토큰(`page`·`cardGap`·`padY`)으로 정한다.
레이아웃 리터럴(padding·spacedBy·Spacer·height·width)에서 많이 나오는 값은 16 > 10 > 8 > 12 > 6 > 20 > 14 > 2 > 4dp 순이다
(grep 근사치).

자주 반복되는 컴포넌트 안쪽 여백(코드 사실):

| 자리 | 값 |
|---|---|
| `NoteCard` 안쪽 | 가로 16 · 세로 15, 칩↔날짜 10, 메타 줄↔제목 10, 메타 줄 끝 24(리본 자리) |
| `NoteRow` | 시작 29(트리 들여쓰기) · 끝 12 · 세로 8 |
| `CategoryRow` | 가로 8 · 세로 10, 캐럿↔이름 8 |
| `CategoryChip` | 가로 8 · 세로 3 |
| `SearchField` | 높이 40, 가로 12 |
| `NumberedRow` | 세로 12(짧은 창 6) · 칸 사이 12 · 최소 높이 44 · 번호 최소 폭 24 |
| `SectionLabel` | 라벨↔개수↔힌트 8 |
| 본문 블록 사이 | 12, 목록 항목 사이 6, 코드 블록 안쪽 가로 14 · 세로 12 |
| 스낵바 바깥 | 가로 16 · 세로 8 |

### 4.3 표면 단계

코드가 쓰는 면은 아래 단계로 쌓인다. 그림자 대신 **면 색과 1dp 테두리**로 층을 나눈다(카드에 그림자가 없다).

| 단계 | 색 | 예 |
|---|---|---|
| 바탕 | `bg` | 화면 |
| 면 | `surface` + 1dp `border` | 카드, 고정 패널, 드로어 |
| 올라온 면 | `surface2` | 카드 안의 입력·코드·배지, 스낵바 |
| 떠 있는 유리 | `glass`/`glassPanel` + 블러·굴절·그림자 | 플로팅 내비, 맨 위로, 상태 알약, 다이얼로그 |
| 덮개 | `scrim` | 퀴즈 오버레이 뒤 |

### 4.4 유리(리퀴드 글래스)

유리는 `ds/component/GlassSurface.kt` 의 `GlassSurface` **하나로만** 만든다. `drawBackdrop` 을 직접 부르는 곳은 이 파일뿐이다
(라이브러리 `com.kyant.backdrop`). 효과 체인은 `blur → lens(굴절) → highlight → shadow`.

| 상수 | 값 | 뜻 |
|---|---|---|
| `GLASS_BLUR_RADIUS` | 8dp | 기본 블러. 핸드오프의 20dp 로는 굴절이 안 보여 낮췄다 |
| `LENS_AMOUNT_RATIO` | 2f | 기본 굴절 = `radius × 2` |
| `DROPLET_REFRACTION_RATIO` | 3.4f | "물방울" 용 강한 굴절 |
| `GlassShadow` | 반경 20dp · y 14dp · 검정 45% | 기본 그림자 |

쓰는 곳마다 블러가 다르다.

| 자리 | 반경 | 블러 | 굴절 | 색 |
|---|---|---|---|---|
| 플로팅 내비 바 | 24dp | 8dp(기본) | ×2 | `glass` |
| 플로팅 내비 레일 | 22dp | 8dp | ×2 | `glass` |
| 맨 위로 버튼 | 23dp | 8dp | ×2 | `glass` |
| 상태 알약 | 20dp | 20dp | ×2 | `glass` |
| 퀴즈 설정 다이얼로그 | 26dp | 20dp | ×2 | `glassPanel` |
| 모델 다이얼로그 | 26dp | 8dp | ×2 | `glassPanel` |
| 퀴즈 설정 세그먼트(물방울, 높이 46dp) | 13dp | 4dp | ×3.4 | `glass`, 선택 시 `accentSoft` 를 `glass` 위에 합성 + 1dp `accent` 테두리. 그림자 없음 |

퀴즈 풀이 상단 바는 `GlassSurface` 를 쓰지 않고 `glassOpaque` 면 + 1dp `glassBorder` 를 직접 그린다(`feat/quiz/screen/QuizSolveScreen.kt` `SolveTopBar`).

- **블러와 굴절은 맞바꾼다.** 블러가 굴절이 밀어낼 디테일을 먼저 지운다. 읽을 게 많은 면(내비)은 블러로 가독성을,
  작은 물방울은 블러를 죽이고 굴절을 산다.
- `refractionAmount = radius × refractionRatio` 라서 **반경을 바꾸면 굴절 세기도 같이 변한다.**
- `backdrop = null` 이면 효과 없이 `glassOpaque` 면 + `glassBorder` 1dp 로 떨어진다. 프리뷰가 이 경로다.
- 단계적 저하(굴절 API 33+ / 블러 31+ / 그 아래 불투명)는 라이브러리가 가른다. 호출부에서 SDK 로 분기하지 않는다.
- 유리가 비추는 대상은 `NotesShell` 의 `layerBackdrop` Box 하나이고 **그 안에는 `NavDisplay` 만** 둔다.
  오버레이가 들어가면 유리가 자기를 샘플링한다.

### 4.5 그림자

| 자리 | 값 |
|---|---|
| 유리 기본(`GlassShadow`) | 반경 20dp · 아래로 14dp · 검정 45% |
| 씬 전환 중 앞 화면(`SceneMotion.FrontShadow`) | elevation 26dp × 진행도(전환 중에만) |

카드·행·칩에는 그림자가 없다.

### 4.6 간격·반경·표면·유리 사용 규칙

#### 간격

- **화면과 화면 사이의 리듬은 창 크기 토큰**이 정한다 — 좌우 여백 `page`, 카드 사이 `cardGap`, 세로 리듬 `padY`. 화면이 이 숫자를 새로 정하지 않는다.
- **한 부품의 생김새는 부품 상수**다. 카드 안쪽 16/15, 행 들여쓰기 29 처럼 단계 밖이어도 된다(4.2). 대신 그 컴포넌트 파일 한 곳에 둔다.
- 묶음 사이는 "같은 묶음이면 가깝게, 다른 묶음이면 멀게" 하나로 판단한다. 라벨↔카드보다 섹션↔섹션이 멀어야 한다(ST §4.1: 라벨↔카드 10, 섹션 사이 28).
- 간격 단계표는 없다(15절 #1). 단계표가 필요해지면 실제로 반복되는 값(16·10·8·12·6·20·14)으로 새로 만들고 화면이 그것을 쓰게 한다.

#### 반경 — 안에 든 것은 작게, 떠 있는 것은 크게

4.1 의 값을 크기순으로 놓으면 뜻이 보인다. 새 반경은 이 위계에서 고른다.

| 반경 | 무엇 | 뜻 |
|---|---|---|
| 7–10 | 배지 · 칩 · 트리 행 · 검색칸 · 코드 블록 | 한 줄짜리 라벨, 목록 안 요소 |
| 12 | 버튼 · 아이콘 상자 · 입력칸 · 설정 줄 · 해제 버튼 | 누르는 것 |
| 14 | 카드 · 선택지 | 내용을 담는 면 |
| 15–18 | 퀴즈 큰 버튼 · CTA · 북마크 큰 카드 · 통계 카드 | 한 화면의 주인공 면 |
| 20–24 | 스낵바 · 상태 알약 · 내비 레일 · 바 | 떠 있는 크롬 |
| 26 | 다이얼로그 | 가장 높은 층 |
| 30 | 씬 전환 중 앞 화면 | 움직이는 동안만 |

- **안쪽 반경 = 바깥 반경 − 테두리 두께.** 겹친 모서리가 평행해야 테두리 굵기가 고르게 보인다(QZ §3.1 CTA 카드 바깥 18 · 안쪽 16.5).
- 알약 모양(높이의 절반)은 떠 있는 것에만 쓴다 — 상태 알약, 내비 항목, 맨 위로.
- 12·18·26 이 여러 화면에 반복된다. 토큰 후보다(15절 #2).

#### 표면

4.3 의 단계를 쌓는 규칙:

- **그림자는 유리 층에만.** 카드는 그림자 없이 1dp 테두리와 면 밝기 차이로 층을 말한다(디자이너 판단: 다크 바탕에서 카드 그림자는 경계를 번지게 한다).
- **`surface2` 는 "안에 든 것" 이다** — 카드 안, 또는 테두리를 두른 작은 단독 상자(빈 상태 아이콘 상자 — BM1 §4.3). 바닥 위에 넓은 `surface2` 면을 깔지 않는다.
  예외는 스낵바 하나다 — 떠 있지만 글자를 읽어야 해서 유리 대신 불투명 `surface2` 를 쓴다(원칙 4).
- 한 화면에서 같은 층을 세 겹 이상 쌓지 않는다. 깊이가 더 필요해 보이면 묶음을 나누는 게 맞다.

#### 유리

- 유리는 "아래에 본문이 계속 있다" 를 보여 주려고 쓴다. 떠 있는 크롬이 본문을 가리지 않는다는 신호다(NAV §4-3).
- **읽을 게 많은 면은 블러, 작은 물방울은 굴절**이다(4.4). 새 유리 자리는 둘 중 어느 쪽인지 먼저 정하고 기존 자리 값을 따른다.
- **같은 층은 같은 유리로 보여야 한다.** 다이얼로그 두 개가 다른 블러를 쓰면 같은 층이 다르게 보인다(15절 #12).
- 유리 위 글자는 `text`·`text2` 까지다(유리 위 `text2` 7.1:1, `text3` 3.7:1 — 10.2).
- 효과가 빠진 모습도 디자인이다 — `backdrop` 이 없을 때의 `glassOpaque` 폴백, API 31 미만에서 라이브러리가 블러·굴절을 빼는 저하(4.4). 그 모습에서 글자가 안 읽히면 그 자리에 유리를 쓰면 안 된다.

---

## 5. 창 크기·레이아웃

### 5.1 창 크기 클래스 — 너비 5 × 높이 3

정의는 `foundation/window/AdaptiveTokens.kt`. 경계값은 Material3 창 크기 클래스와 같다.

| 너비 클래스 `MyNoteWindowClass` | 범위(dp) |
|---|---|
| `Compact` | < 600 |
| `Medium` | 600 – 839 |
| `Expanded` | 840 – 1199 |
| `Large` | 1200 – 1599 |
| `ExtraLarge` | ≥ 1600 |

| 높이 클래스 `MyNoteHeightClass` | 범위(dp) |
|---|---|
| `Compact` | < 480 |
| `Medium` | 480 – 899 |
| `Expanded` | ≥ 900 |

**판정은 한 곳에서 한 번 한다.** `feat/NotesRoute.kt` 의 `NotesScreen` 이 `LocalWindowInfo.current.containerDpSize` 로
`fromWidth`/`fromHeight` 를 부르고 `MyNoteTheme(windowClass, heightClass, fontScale)` 에 넘긴다. 그 아래는 전부
`MyNoteTheme.adaptive` 를 읽는다. 화면에서 `BoxWithConstraints` 로 다시 재지 않는다(그건 창이 아니라 부모 제약이다).

### 5.2 `AdaptiveTokens` 값

접근은 `MyNoteTheme.adaptive.<이름>`. 너비 토큰은 가로 구조를, 높이 토큰은 세로 리듬을 정한다.

**너비 토큰**

| 클래스 | `page` | `cardGap` | `paneWidth` | `readingMaxWidth` | `cardMinWidth` | `useModalDrawer` |
|---|---|---|---|---|---|---|
| Compact | 16 | 10 | 300 | 없음 | 240 | 예 |
| Medium | 24 | 12 | 268 | 없음 | 200 | 아니오 |
| Expanded | 32 | 14 | 300 | 720 | 200 | 아니오 |
| Large | 40 | 16 | 320 | 760 | 230 | 아니오 |
| ExtraLarge | 56 | 18 | 340 | 820 | 240 | 아니오 |

- `page` 화면 좌우 여백 · `cardGap` 카드 사이 · `paneWidth` 목록 패널 폭(Compact 는 드로어 폭) ·
  `readingMaxWidth` 본문 최대 폭 · `cardMinWidth` 홈 그리드 카드 최소 폭(`GridCells.Adaptive(minSize = …)`, 고정 열 수 금지) ·
  `useModalDrawer` 목록을 모달 드로어로 띄울지.

**높이 토큰**

| 클래스 | `headerHeight` | `padY` | `fadeHeight` | `heroLogoSize` | `heroTitleSize` |
|---|---|---|---|---|---|
| Compact | 44 | 10 | 16 | 없음 | 20sp |
| Medium | 56 | 20 | 24 | 46 | 24sp |
| Expanded | 60 | 28 | 30 | 54 | 28sp |

**`null`(표의 "없음")은 "없음" 이라는 뜻이다.** `readingMaxWidth == null` 은 본문 폭 제한 없음,
`heroLogoSize == null` 은 히어로 로고를 **그리지 않음**. `0.dp` 로 바꾸면 "0 크기로 그리기" 가 되어 의미가 사라진다.

**파생값** (두 축의 함수라 계산 프로퍼티로 여기 있다)

| 이름 | 식 |
|---|---|
| `noteTitleSize` | `heroTitleSize + 4sp` |
| `useFloatingRail` | 너비 Expanded 이상 **또는** 높이 Compact |
| `railRightMargin` | `max(14dp, page − 12dp)` |
| `barBottomMargin` | `padY + 6dp` |
| `panelHeaderPadTop` | `max(10dp, padY − 4dp)` |

### 5.3 레일이냐 바냐

`useFloatingRail` 이 참이면 오른쪽 플로팅 레일, 거짓이면 하단 플로팅 바다. 짧은 창(가로로 눕힌 폰)은 너비와 무관하게 레일이다 —
낮은 창에서 하단 바는 본문을 먹는다. 이 술어를 **세 곳이 같이 읽는다**: 하단 여백 예약, 맨 위로 버튼 위치, 레일/바 중 무엇을 그릴지.
그래서 화면이 아니라 토큰에 있다.

| 컴포넌트 | 크기 |
|---|---|
| 바 `FloatingNavDefaults.BarHeight` | 60dp |
| 레일 `FloatingNavDefaults.RailWidth` | 62dp(항목 46 + 안쪽 8 × 2) |
| 맨 위로 `ScrollToTopDefaults.Size` | 46dp |

### 5.4 여백 예약 — `feat/shell/NotesLayout.kt`

플로팅 내비가 본문을 가리지 않게 셸이 여백을 계산해 화면에 넘긴다. `Scaffold(contentWindowInsets = WindowInsets(0))` 라서
아무도 대신 밀어 주지 않는다.

| 상수 | 값 |
|---|---|
| `FLOATING_BAR_RESERVE` | 92dp — 바 형태일 때 하단에 더하는 여백 |
| `FLOATING_RAIL_GAP` | 12dp — 레일과 본문 사이 |
| `TWO_PANE_MIN_CONTENT` | 640dp — 2단 판정 최소 콘텐츠 폭 |
| `MODEL_PILL_RESERVE_DP` | 54dp — 상단 모델 알약이 떠 있는 동안 위 여백(300ms 로 변한다, `feat/shell/ModelPill.kt`) |

| 값 | 식 |
|---|---|
| `contentBottomPadding` | 레일: 하단 인셋 · 바: `92dp + 하단 인셋` |
| `contentEndPadding`(레일 예약) | 레일: `max(0, railRightMargin + 끝 인셋 + 62dp + 12dp − page)` · 바: 0 |
| `scrollToTopBottom` | 레일: `padY + 하단 인셋` · 바: `barBottomMargin + 하단 인셋 + (60 − 46)/2` (바와 세로 중앙 정렬) |
| `headerOffset` | 드로어(Compact): 0 · 그 외: `headerHeight + 상태 바` |

### 5.5 2단 판정 — 640dp

북마크·통계 화면의 2단 여부는 화면이 재지 않고 셸이 계산한다(`contentTwoPane`).

```
창 폭 − (펴진 고정 패널 ? paneWidth : 0) − page − 레일 예약 − page ≥ 640dp
```

패널을 접으면 2단으로 바뀔 수 있는 이유가 이것이다. 예: 폭 1280dp(Large) 창에 패널을 펴면
`1280 − 320 − 40 − 62 − 40 = 818 ≥ 640` 이라 2단이다.
가로 폰에서의 축소(큰 카드 줄이기, 가로 막대·낮은 차트)는 단 수가 아니라 `heightClass == Compact` 로 갈린다.

### 5.6 씬 구조

| 너비 | 구조 |
|---|---|
| Compact(`useModalDrawer`) | 한 칸 씬. 홈·리더가 별개 씬으로 쌓인다. 목록은 `ModalNavigationDrawer` |
| Medium 이상 | `ListDetailScene`(`nav/scene/ListDetailScene.kt`) — 왼쪽 고정 패널(`paneWidth`, 접을 수 있다) + 오른쪽 상세 한 칸. 헤더도 상세 칸 위, 씬 안에 있다 |

### 5.7 레이아웃 원칙

- **너비는 가로 구조, 높이는 세로 리듬과 내비 모양**을 정한다(NAV §1). 새 판정을 만들 때도 이 분담을 지킨다 — "몇 단인가" 는 폭만, "줄이나" 는 높이만 본다(STA §8 "단 수는 폭만, 축소는 높이만 봅니다").
- **두 단은 실제 콘텐츠 폭으로 정한다**(5.5). 펼친 폴드는 Expanded 지만 패널을 펴면 콘텐츠가 좁아 한 단이다. 클래스 이름으로 두 단을 정하면 큰 카드가 너무 좁아진다(BM3 §5).
- **줄 길이에 상한이 있다.** 본문은 `readingMaxWidth`, 퀴즈 풀이·설정은 가운데 열(560dp `CONTENT_MAX_WIDTH`), 퀴즈 설정 다이얼로그는 더 좁게(420dp `DIALOG_MAX_WIDTH`)(QZ §6 "선택지가 화면 폭만큼 늘어나면 눈이 가로로 너무 멀리 이동한다").
  넓은 창에서는 내용을 늘리지 말고 여백을 늘린다(SN 높이 Expanded "여백만 확대, 본문 폭은 그대로").
- **레일은 목록 패널의 반대편, 오른쪽 위**다(NAV §4-1 "노트 패널이 좌측에 있으므로"). 레일이 뜨면 콘텐츠는 레일 왼쪽에서 끝난다 — 누를 것이 레일 아래 깔리면 안 된다(5.4 레일 예약).
- **가로 폰(높이 Compact)은 핵심 블록이 한 화면에 들어오게 줄인다.** 스크롤해야 다 보이는 큰 카드는 큰 카드가 아니다(BM3 §5 "큰 카드가 한 화면 안에 다 들어와야 합니다").
- **열 수를 고정하지 않는다.** 카드 최소 폭으로 채운다(NAV §2). 폴더블을 접고 펼 때 열이 튀지 않게 하려는 것이다.
- **넓은 창에서 퀴즈처럼 리더 위에 얹히는 상태는 상세 칸만 덮는다.** 목록 패널은 남아 계속 눌린다 — 목적지가 아니라 리더 위 상태이기 때문이다(QZ §6).
- 화면은 창을 다시 재지 않는다(5.1). 디자인 쪽 이유: 화면마다 재면 같은 창에서 화면끼리 다른 답을 내고, 폴드를 펼 때 화면마다 다른 순간에 바뀐다.

---

## 6. 모션

### 6.1 이징

| 이름 | 곡선 | 정의 | 쓰는 자리 |
|---|---|---|---|
| `MyNoteEasing` | `cubic-bezier(0.2, 0.8, 0.2, 1)` | `ds/motion/Motion.kt` | 앱 공용. `riseIn`, `flip`, 차트, 북마크 행·큰 카드, 모델 알약 여백 |
| `SceneMotion.Easing` | `cubic-bezier(0.2, 0, 0, 1)` | `nav/motion/SceneMotion.kt`(internal) | 씬 전환 전용 |
| `FastOutSlowInEasing` | Compose 기본 | — | 플로팅 내비 숨김, 맨 위로 버튼 |

`:core:navigation` 은 design-system 을 의존하지 않아서 씬 이징을 따로 갖는다.

### 6.2 씬 전환 — `nav/motion/SceneMotion.kt`

| 상수 | 값 |
|---|---|
| `DURATION_MILLIS` | 340 |
| `SLIDE_FRACTION` | 1.05(화면 폭 대비) |
| `FRONT_SCALE` | 0.90 |
| `BACK_SCALE` / `BACK_ALPHA` | 0.93 / 0.5 |
| `FrontCornerRadius` / `FrontShadow` | 30dp / 26dp |
| `BODY_FADE_OUT_MILLIS` / `BODY_FADE_IN_MILLIS` | 160 / 200 |
| `BodyFadeOffset` | 10dp |

- **push** — 새 화면이 오른쪽에서 폭 × 1.05 만큼 밀려 들어오며 0.90 → 1 로 커진다. 뒤 화면은 0.93 으로 줄고 알파 0.5 로 흐려진다.
- **pop** — 반대. 예측 뒤로가기도 같은 스펙이다.
- **앞 화면 모서리** — 전환 중에만 모서리 30dp·그림자 26dp 가 진행도에 비례해 붙는다(`nav/scene/SceneEdgeTransform.kt` `sceneEdge`).
- **넓은 창의 상세 칸 교체** — 나가는 칸 160ms 페이드 + 10dp 이동, 들어오는 칸은 160ms 기다렸다가 200ms 페이드 + 10dp 올라옴.
  패널 펴기/접기는 가로 확장·축소 + 페이드.
- **연타 차단** — `Navigator` 는 `max(340, 160 + 200) = 360ms` 안에 들어온 다음 이동을 버린다(`NavResult.Throttled`).
- 공유 요소 — 홈 카드 → 리더의 칩·날짜·제목(`chip-/date-/title-{noteId}`)이 `navSharedElement`/`navSharedBounds` 로 이어진다.

### 6.3 컴포넌트 모션 값

| 자리 | 값 | 정의 |
|---|---|---|
| 눌림(`PressIndication`) | 누름 90ms tween, 뗌 spring(`StiffnessMediumLow`) | `PRESS_IN_MILLIS` |
| 내비 선택 알약·아이콘 | spring(`DampingRatioMediumBouncy`, `StiffnessLow`), 선택 아이콘 ×1.1 | `SELECTED_ICON_SCALE` |
| 내비 선택 라벨 | 나타남 150ms(60ms 지연) · 사라짐 90ms + 가로 확장/축소 | `LABEL_FADE_*` |
| 내비 숨김 | 이동 340ms · 알파 200ms, `FastOutSlowInEasing`. 바는 높이 × 1.6 아래로, 레일은 폭 + 오른쪽 여백만큼 밖으로 | `HIDE_*_MILLIS` |
| 내비 숨김 판정 | 6dp 미만 이동 무시, 상단 48dp 안에서는 안 숨음 | `FloatingNavScrollState` |
| 맨 위로 버튼 | 나타남 220ms · 사라짐 160ms, 0.7 ↔ 1 스케일 + 페이드 | `APPEAR_/DISAPPEAR_MILLIS` |
| `NoteCard` 북마크 리본 | 160ms 페이드 | `RIBBON_FADE_MILLIS` |
| `riseIn` | 300ms, 12dp 아래에서 올라오며 페이드, `MyNoteEasing`, 지연 가능 | `DEFAULT_RISE_MILLIS` |
| `flip` | 78° X축 회전, 0.52 → 1, 들어옴 460ms(회전 tween + 크기 spring 감쇠 0.68) · 나감 260ms, 카메라 거리 16 | `DEFAULT_FLIP_*` |
| `aiShine` | 테두리 빛 한 바퀴 3200ms 선형 반복, 선 1.5dp | `PERIOD_MILLIS` |
| `StatusRing`(진행률 모름) | 1100ms 한 바퀴, 호 90° | `SPIN_MILLIS` |
| 카테고리 캐럿 | 0° ↔ 90°(기본 spring) | `CategoryRow` |
| 퀴즈 결과 점수 게이지 | 채움 900ms · 튀어나옴 420ms(0.86 → 1), 둘 다 `MyNoteEasing` | `feat/quiz/screen/QuizResultScreen.kt` |
| 퀴즈 다이얼로그 본문 | 들어옴 190ms · 나감 150ms | `feat/quiz/QuizOverlay.kt` `BODY_MILLIS`/`BODY_EXIT_MILLIS` |
| 차트 등장 [통계(진행 중)] | 총 540ms = 칸 지연 최대 120(칸당 20) + 막대 300 + 라벨 120. 칸마다 `MyNoteEasing` | `ds/chart/ChartMotion.kt` |
| 모델 알약 여백 | 0 ↔ 54dp, 300ms, `MyNoteEasing` | `feat/shell/NotesShell.kt` |
| 목록 패널 페이드 | 위 0 ↔ 16dp · 아래 0 ↔ 52dp(`animateDpAsState`) | `fadingEdges` 사용처 |

### 6.4 애니메이션 값을 읽는 단계

애니메이션 값이 컴포지션에서 읽히면 매 프레임 리컴포지션이 돈다. 이 시스템은 세 방식으로 draw/layout 단계에 가둔다.

1. **값을 `() -> Float` 로 넘긴다.** `FlipState.rotationX`/`scale`, `selectionPill(selection: () -> Float, pressed: () -> Boolean)`,
   차트의 `progress: () -> Float` 가 람다다. 받는 쪽이 `graphicsLayer { }`/`drawBehind { }` **안에서** 부른다.
2. **`Modifier.Node` 안에 `Animatable` 을 둔다.** `PressIndication`·`riseIn`·`aiShine` 은 진행도가 컴포지션에 올라오지 않는다.
   쉬지 않고 도는 `aiShine` 은 자기 레이어(`graphicsLayer {}`)에 그려 다시 기록하는 범위가 카드에 머문다 — 레이어가 없으면 감싼 스크롤 내용 전체를 프레임마다 다시 기록한다.
3. **modifier 인자로 애니메이션 값을 넘기지 않는다.** `Modifier.alpha(v)`/`rotate(v)`/`offset(x)` 대신
   `graphicsLayer { alpha = v }`(예: `CategoryRow` 캐럿의 `rotationZ`).

크기에 따라 다시 만들 것(`Path`·`Brush`·`Outline`)은 `size` 를 키로 캐시한다. 매 프레임 새로 만들지 않는다.

### 6.5 감속 모션

`rememberReduceMotion()`(`ds/motion/Motion.kt`)은 시스템 "애니메이션 길이 배율"(`ANIMATOR_DURATION_SCALE`)이 0이면 참이다.
`LocalInspectionMode` 를 OR 로 물고 있어 **프리뷰에서는 항상 참**이다. 판정만 주고 처리는 쓰는 쪽이 한다.

코드가 쓰는 처리 방식:

| 방식 | 예 |
|---|---|
| tween 대신 `snap()` | 북마크 행 등장·퇴장·자리 이동, 큰 카드 크로스페이드·크기 변화 |
| 등장 모디파이어를 아예 안 건다 | 설정 화면 토큰 입력칸의 `riseIn` |
| 무한 반복을 멈춘다 | 퀴즈 오브(`pingPong(still, …)`), 퀴즈 생성·풀이·결과 화면 |
| 효과를 끈다 | 퀴즈 CTA 의 `aiShine`(`cta.shine && !rememberReduceMotion()`) |
| 처음부터 끝 값 | 차트 `rememberChartProgress` 가 1에서 시작 |

**감속 모션을 반영하지 않는 곳**(2026-10-02): 씬 전환(`:core:navigation` 은 이 함수를 못 본다), 플로팅 내비 숨김·선택,
맨 위로 버튼, `PressIndication`, `NoteCard` 리본 페이드, `CategoryRow` 캐럿, `StatusRing` 무한 회전(상태 알약·목록 패널 설정 줄),
퀴즈 화면의 `riseIn`(풀이·결과·실패 화면 15곳), 퀴즈 설정 다이얼로그의 `flip`(`QuizOverlay`).
`riseIn`·`flip`·`aiShine` 은 스스로 판정하지 않으므로 부르는 쪽이 판정해야 하는데, 지금 그렇게 하는 곳은
설정 화면의 `riseIn` 과 퀴즈 CTA 의 `aiShine` 둘뿐이다. 15절 참고.

### 6.6 모션 원칙

#### 언제 움직이나

움직임은 **설명**할 때만 쓴다. 넷 중 하나가 아니면 움직이지 않는다.

1. **상태가 바뀌었다** — 채점 색, 토글 색, 진행 바 칸 색(QZ §4, BM1 §5).
2. **같은 물체가 이어진다** — 카드의 칩·날짜·제목이 본문 헤더로 이어지고(NAV §6), 북마크 큰 카드를 해제하면 카드는 제자리에 있고 내용만 바뀐다(BM3 §4 "카드 자체는 움직이지 않음").
3. **기다리는 중이다** — 무한 루프는 기다림과 AI 에만 쓴다(QZ §3.3 "진행이 실제로 멈춰도 살아 있어 보이는 게 목적이다").
4. **순서대로 나타난다** — 처음 그려지는 블록이 아래에서 살짝 올라오며 계단으로 들어온다(`riseIn`, DS §08).

움직이지 않는 것:
- 사용자가 보지 않는 동안 바뀐 것. 리더에서 북마크를 켜고 돌아온 홈 카드의 리본은 이미 있는 채로 그린다(BM3 §2.1).
- 표시 순서 번호(BM3 §4 "번호 바뀜 — 애니메이션 없음").
- 글자 크기 설정 — 테마 교체로 바로 바뀐다(ST §4.2).
- 스크롤로 다시 보이는 항목. `riseIn` 은 처음 한 번이다.

#### 곡선의 성격

6.1 의 곡선과 코드 상수를 성격으로 묶으면 다음과 같다. 새 모션은 성격을 먼저 고르고 곡선을 따른다.

| 성격 | 곡선 | 쓰는 곳 |
|---|---|---|
| **도착**(빠르게 출발해 부드럽게 멈춤) | `MyNoteEasing` | 등장 · 펼침 · 막대·게이지 · 재배치 · 뒤집기 회전 |
| **장면**(무게 있는 이동) | (0.2, 0, 0, 1) — `SceneMotion.Easing`, 같은 값의 `NotesTopBar` 튀기 곡선 | 화면 push/pop · 리더 토글 튀기 |
| **표준** | `FastOutSlowInEasing` | 내비 숨김/복귀 · 맨 위로 · 리더 토글 색 |
| **떠남**(가속) | (0.4, 0, 1, 1) — 북마크 화면 private | 사라지는 항목 |
| **시간 그 자체** | 선형 | 무한 회전 · shimmer · 자동 진행 링 · 차트 전체 진행도 |
| **탄성** | spring | 뒤집기 크기 · 눌림 복귀 · 내비 선택 |

- **들어오는 것은 감속, 나가는 것은 가속.** 사라지는 항목에 도착 곡선을 쓰면 끝에서 머뭇거려 "안 지워졌나" 로 읽힌다.
- 핸드오프 몇 곳이 장면 곡선 (0.2, 0, 0, 1) 을 `MyNoteEasing` 이라 불렀다. 앱 공용 곡선은 도착 곡선 하나다(15절 #8).
- 공유 요소(카드 → 리더 헤더)는 지금 Compose 기본 spring 이다. 디자인 값은 장면 성격·씬과 같은 340ms 다(15절 #23).

#### 길이

6.2·6.3 의 값은 다섯 대역에 모인다. 새 모션은 이 대역 안에서 고른다.

| 대역 | 쓰는 곳 |
|---|---|
| 90–160ms | 눌림 · 라벨 페이드 · 색 바뀜 · 리본 — 손에 붙는 피드백 |
| 180–260ms | 작은 등장·퇴장 · 맨 위로 · 다이얼로그 닫힘 |
| 300–340ms | `riseIn` · 레이아웃 이동 · 장면 전환 · 내비 숨김 |
| 420–460ms | 점수 게이지 등장 · 뒤집기 등장 — "한 번 보여 줄 것" |
| 900ms 이상 | 점수 게이지 채움(900, QZ §4) · 무한 루프 한 바퀴(링 1100 · 샤인 3200) |

900ms 이상인 일회성 모션은 결과 화면의 게이지 하나다. 사용자가 기다리는 동안이 아니라 **결과를 보는 동안**이라 길어도 된다 — 다른 자리에서 이 길이를 빌리지 않는다.

- **나가는 것은 들어오는 것보다 짧다** — 뒤집기 460/260, 퀴즈 다이얼로그 본문 190/150, 내비 라벨 150/90, 맨 위로 220/160(6.3).
- 장면 전환 중 들어온 이동은 버려진다(6.2 연타 차단). 장면 전환보다 긴 모션을 화면 이동에 얹지 않는다.

#### 감속 모션

켜져 있으면 **이동·크기·무한 루프를 끄고 페이드만 남긴다**(BM1 §5, BM3 §4, ST §7, STA §7).

- 막대·게이지·도넛은 최종 값으로 바로 그린다.
- 무한 링·샤인·구슬·shimmer 는 멈춘 그림으로 그린다(QZ §4 "샤인 → `border` 단색, 구슬 → 정적, shimmer → 단색"). 진행 중임은 링을 반투명으로 두어 말한다(STA §7 "진행 중엔 alpha 0.5").
- 시간에 맞춰 화면이 바뀌는 것(정답 자동 진행)은 TalkBack 이 켜져 있으면 끈다(QZ §6 "읽는 중에 화면이 바뀌면 안 된다").
- **감속 모션 분기는 새 모션을 만들 때 같이 만든다.** 나중에 붙이면 빠진다 — 지금 빠진 자리가 6.5 끝 목록이다(15절 #5).

#### 무한 루프의 조건

무한 애니메이션은 배터리를 먹는다(QZ §4). 셋을 다 만족할 때만 돌게 한다 — 화면에 보인다 · 앱이 앞에 있다(RESUMED) · 감속 모션이 꺼져 있다.

---

## 7. 아이콘

`ds/component/MyNoteIcons.kt` 의 `object MyNoteIcons` — 손으로 그린 `ImageVector` 15개. 접근은 `MyNoteIcons.<이름>`.
Material 아이콘 세트를 쓰지 않는다.

| 이름 | 형태 | 쓰는 자리 |
|---|---|---|
| `Home` | 선 | 내비 홈 |
| `Search` | 선 | `FloatingNav` 프리뷰에서만. 실제 화면에서 쓰는 곳 없음 |
| `Bookmark` | 선 | 내비 북마크, 상단 바 북마크 토글(꺼짐), 북마크 빈 화면 |
| `BookmarkFilled` | 채움 | `BookmarkGlyph`, 북마크 행, 상단 바 북마크 토글(켜짐) |
| `Stats` | 선 | 내비 통계 |
| `Settings` | 선 | 목록 패널 맨 아래 설정 줄 |
| `Key` | 선 | 설정 화면 GitHub 토큰 |
| `ArrowUp` | 선 | 맨 위로 |
| `Sparkle` | 채움(별 두 개, 작은 별 55%) | 퀴즈·AI — CTA, 오브, 설정 다이얼로그, 모델 카드, 실패 화면 |
| `Check` | 선 | 완료 — 상태 알약·모델 알약, 퀴즈 정답·결과 |
| `CheckBox` | 선(상자 3–21 · 반경 4) | 목록 체크 항목 끔 |
| `CheckBoxChecked` | 채움(상자 바깥선까지 · 체크는 `PathFillType.EvenOdd` 구멍 — 바탕이 보인다) | 목록 체크 항목 켬 |
| `Close` | 선 | 퀴즈 닫기·오답 |
| `ChevronRight` | 선 | 퀴즈 CTA |
| `Warn` | 선 | 주의 — 상태 알약·모델 알약, 퀴즈 풀이 |

**그리는 규격**

| 항목 | 값 |
|---|---|
| 뷰포트 | 24 × 24 |
| 기본 크기 | 21dp(`Sparkle` 만 24dp) |
| 선 | 1.7, 둥근 끝(`StrokeCap.Round`), 둥근 이음(`StrokeJoin.Round`) |
| 채움 | 흰색. 쓰는 쪽이 `tint` 로 색을 입힌다 |

자주 쓰는 표시 크기: 내비 아이콘 21dp, 맨 위로 21dp, 상태 알약 14dp(링 18 − 4), 북마크 글리프 12dp, 체크 상자 1.25em(본문 16sp 에서 20dp — 본문 글자와 같이 커진다).

### 7.1 아이콘 사용 규칙

- **한 벌만 쓴다.** `MyNoteIcons` 의 규격(7절 표)으로 그린 선 아이콘이다(NAV §4-4, QZ §8). 다른 세트에서 가져와 섞지 않는다 — 선 굵기가 다르면 바로 티가 난다. 없는 아이콘은 같은 규격으로 그려 더한다.
- **채움은 켜짐**이다. 북마크는 꺼짐 = 선, 켜짐 = 채움(BM1 §4.1). 채운 아이콘을 장식으로 쓰지 않는다. 예외는 스파클 하나(원래 채운 모양)다.
- **스파클은 AI 전용 표지**다(퀴즈 CTA·설정 다이얼로그·생성 중·모델 카드·실패 — QZ §3.1, ST §10.2). AI 가 아닌 기능에 쓰지 않는다. 스파클이 보이면 "기기 안 모델이 하는 일" 이라는 약속이다.
- **크기는 자리로 정한다**: 내비·맨 위로 21dp, 상단 바 토글 22dp(BM1 §3), 설정 줄 20dp(ST §2.3), 해제 버튼 18–20dp(BM3 §1.3), 표기 글리프 12dp. 같은 자리에 다른 크기를 쓰지 않는다.
- **색은 tint 로만 입힌다.** 기본 `text2`, 켜짐·선택 `accent`, 상단 바 `bodyText`, 상태 아이콘은 상태 색(2.3 상태 색 지도).
- **장식 아이콘은 설명을 비운다.** 정보가 있는 아이콘(북마크 표기)은 아이콘이 아니라 부모 노드의 상태 설명으로 읽힌다(BM3 §2.3). 아이콘만 있는 버튼은 동작 이름을 설명으로 단다("북마크 해제", "맨 위로").
- 로고는 이미지가 아니라 `accent` 사각 + 글자 "N" 이다(SN Assets).

---

## 8. 컴포넌트

전부 `:core:design-system` 에 있다. 화면 전용 위젯은 feature 에 두고, 두 화면 이상이 쓰면 여기로 올린다.
**공통 규약:** 첫 필수 인자 뒤 `modifier: Modifier = Modifier`, 컬렉션은 `ImmutableList`, 콜백은 순수 람다(인텐트를 모른다),
UI 를 그리는 파일은 같은 파일에 `@Preview` 동반.

### 8.1 표면·내비

| 이름 | 용도 | 주요 파라미터 | 상태 | 파일 |
|---|---|---|---|---|
| `GlassSurface` | 리퀴드 글래스 면. 유리가 필요한 모든 자리 | `radius`, `backdrop: Backdrop?`, `blurRadius`(8dp), `refractionRatio`(2f), `tint`, `shadow`(`GlassShadow`), `content` | `backdrop` 있음 = 블러·굴절 / 없음 = 불투명 폴백 | `ds/component/GlassSurface.kt` |
| `FloatingNavBar` | 하단 플로팅 탭 바(반경 24dp, 안쪽 7dp, 항목 높이 46dp) | `items: ImmutableList<FloatingNavItem>`, `selectedId`, `onSelect`, `hidden`, `backdrop` | 선택(알약 + 라벨 펼침) · 비선택(아이콘만, `text2`) · 눌림 · 숨김 | `ds/component/FloatingNav.kt` |
| `FloatingNavRail` | 오른쪽 플로팅 레일(반경 22dp, 항목 46dp 정사각·반경 15dp) | 위와 같음 + `rightMargin` | 선택(알약, 라벨 없음) · 눌림 · 숨김 | `ds/component/FloatingNav.kt` |
| `FloatingNavItem` | 내비 항목 데이터 | `id`, `label`, `icon` | — | `ds/component/FloatingNav.kt` |
| `FloatingNavScrollState` | 스크롤로 내비 숨김을 판정하는 상태 홀더(`rememberFloatingNavScrollState()`) | `nestedScrollConnection`, `reset()` | `hidden` | `ds/component/FloatingNavScrollState.kt` |
| `ScrollToTopButton` | 맨 위로 46dp 유리 버튼 | `visible`, `onClick`, `backdrop` | 보임/숨김, 눌림(`navItemPressed`) | `ds/component/ScrollToTopButton.kt` |
| `FloatingStatusPill` | 40dp 유리 상태 알약(모델 내려받기 등) | `label`, `tone: Color`, `trailing`, `progress: Float?`, `icon`, `backdrop`, `onClick` | 진행률 있음(호) · 모름(회전) · 아이콘 · 누를 수 있음/없음 | `ds/component/FloatingStatusPill.kt` |
| `StatusRing` | 18dp 진행 링(선 2.4dp, 트랙 흰색 12%) | `progress: Float?`, `tone`, `diameter` | 결정 · 미결정(회전) | `ds/component/FloatingStatusPill.kt` |
| `MyNoteSnackbar` / `MyNoteSnackbarHost` | 테마 색 스낵바(`surface2` 면, 반경 20dp). M3 기본 스낵바 대신 | `hostState` / `data` | 액션 있음/없음 | `ds/component/MyNoteSnackbar.kt` |

### 8.2 목록·카드

| 이름 | 용도 | 주요 파라미터 | 상태 | 파일 |
|---|---|---|---|---|
| `NoteCard` | 홈 그리드 노트 카드(`surface`, 테두리 1dp, 반경 14dp) | `category`, `date`, `title`, `onClick`, `chipModifier`/`dateModifier`/`titleModifier`(공유 요소 슬롯), `bookmarked`, `trailing` | 기본 · 북마크(오른쪽 위 리본, 노드 `stateDescription = ds_bookmarked`) | `ds/component/NoteCard.kt` |
| `NoteRow` | 목록 패널 트리의 노트 행 | `title`, `active`, `onClick`, `bookmarked` | 활성(`accentSoft` 바탕, `accent` 500) · 비활성(`text2` 400) × 북마크 글리프 유무 | `ds/component/NoteRow.kt` |
| `CategoryRow` | 목록 패널 트리의 카테고리 행 | `name`, `count`, `expanded`, `onClick` | 펼침(캐럿 90°) · 접힘 | `ds/component/CategoryRow.kt` |
| `CategoryChip` | 카테고리 칩(`caption`, `accent` 글자, `accentSoft` 바탕) | `text` | 한 줄, 넘치면 말줄임 | `ds/component/CategoryChip.kt` |
| `NumberedRow` | `01` 부터 번호 붙은 행 + 아래 구분선(북마크·통계 목록) | `number`, `title`, `subtitle`, `contentDescription`, `onClick`, `compactHeight`, `trailing` | 기본 · 짧은 창(세로 여백 12 → 6) | `ds/component/NumberedRow.kt` |
| `SectionLabel` | 섹션 머리 라벨(`overline`, `text3`) + 개수 + 힌트. 접근성 heading | `text`, `count`, `hint` | 개수·힌트 유무 | `ds/component/SectionLabel.kt` |
| `BookmarkRibbon` | 카드 모서리 북마크 리본 12 × 17dp(`text2`, 홈 4.5/17) | — | — | `ds/component/BookmarkRibbon.kt` |
| `BookmarkGlyph` | 12dp 북마크 글리프(`text3`, 설명 `ds_bookmarked`). `NoteRow` 전용 | `size` | — | `ds/component/BookmarkGlyph.kt` |

### 8.3 입력·인터랙션

| 이름 | 용도 | 주요 파라미터 | 상태 | 파일 |
|---|---|---|---|---|
| `SearchField` | 40dp 검색칸(`surface2`, 반경 10dp, `body`, `accent` 커서). 검색 키를 누르면 포커스 해제 | `query`, `onQueryChange`, `placeholder: String?`(기본 `ds_search_hint`) | 빈 칸(placeholder `text3`) · 입력 | `ds/component/SearchField.kt` |
| `PressIndication` | 눌림 표현 `IndicationNodeFactory` — 면·눌림색·스케일 | `shape`, `color`, `pressColor`, `scale` | 평상 · 눌림 | `ds/component/PressIndication.kt` |
| `throttledProcess` | 연타 방지 콜백(기본 500ms, `:core:common` `Throttler`) | `minIntervalMillis`, `action` | — | `ds/component/ThrottledProcess.kt` |

**버튼 컴포넌트는 design-system 에 없다.** 화면마다 따로 있다(전부 `PressIndication` 을 쓴다).

| 묶음 | 함수 | 파일 |
|---|---|---|
| 설정 | `PrimaryButton` · `SecondaryButton` · `DestructiveButton` · `QuietButton` | `feat/screen/SettingsScreen.kt` |
| 퀴즈 | `QuizPrimaryButton` · `QuizOutlineButton`(높이 50dp, 반경 15dp) · `QuizQuietButton`(반경 12dp) · `QuizCloseButton`(40dp 원) | `feat/quiz/component/QuizParts.kt` |
| 퀴즈 설정 다이얼로그 | `DialogPrimaryButton` · `DialogQuietButton` | `feat/quiz/component/QuizSetupDialog.kt` |

**`indication` 을 주지 않은 `clickable` 은 Compose 기본 눌림 표현이 그려진다.** `MaterialTheme` 도 `LocalIndication` 제공도 없어서
foundation 기본값 `DefaultDebugIndication`(눌림 검정 30% 사각 덮개, 호버·포커스 검정 10%)이 뜬다. `NoteCard`·`NoteRow`·`CategoryRow`·
`NumberedRow`·`FloatingStatusPill` 이 지금 이 경로다. `clip` 뒤에 `clickable` 을 걸면 모양대로 잘리고, 아니면 사각이다.
디자인한 눌림을 원하면 `PressIndication` 을 넘긴다.

`PressIndication` 호출 규약: `color` 로 평상시 면을 그리면 `interactionSource = remember { MutableInteractionSource() }` 를 **명시**한다.
`null` 이면 Compose 가 첫 터치까지 노드 생성을 미뤄 **배경이 안 보이고 누를 때만 나타난다.** 면을 `Modifier.background` 로 따로 깔면
라벨만 줄어든 것처럼 보이므로 면은 `color`/`pressColor` 로 넘긴다.

### 8.4 Modifier·모션

| 이름 | 용도 | 주요 파라미터 | 파일 |
|---|---|---|---|
| `Modifier.riseIn` | 붙는 순간 아래에서 올라오며 나타남 | `delayMillis`, `durationMillis`(300), `distance`(12dp) | `ds/component/RiseIn.kt` |
| `Modifier.aiShine` | AI 기능 테두리를 도는 빛 | `color`, `baseColor`, `radius`, `width`(1.5dp), `running` | `ds/component/AiShine.kt` |
| `rememberFlipState` + `Modifier.flip` | `AnimatedVisibility` 안에서 X축으로 뒤집히며 등장·퇴장 | `degrees`(78), `startScale`(0.52), `transformOrigin`, `cameraDistance`(16), `inMillis`(460), `outMillis`(260), `bounce`(0.68) | `ds/component/Flip.kt` |
| `Modifier.fadingEdges` | 스크롤 영역 위·아래 끝을 투명하게 | `top`, `bottom` | `ds/component/FadingEdges.kt` |
| `Modifier.horizontalScrollFade` | 가로 스크롤에서 가려진 쪽 끝을 바탕색으로 덮는다(처음 = 끝 쪽 · 중간 = 양쪽 · 끝 = 시작 쪽). 스크롤 값은 그리기 단계에서만 읽는다 | `state`, `width`, `color` | `ds/component/FadingEdges.kt` |

### 8.5 아이콘

`MyNoteIcons` — 7절.

### 8.6 본문 블록 — `ds/block/`

서버드리븐 노트 본문을 그린다. UI 모델은 닫힌 sealed 집합 `NoteContentBlock`(`NoteContentBlock.kt`), 렌더러는 `NoteContent(blocks)`(`NoteContent.kt`).

| 블록 | 그리는 법 |
|---|---|
| `Heading(level, spans)` | level ≤ 2 → `mdHeading2`, 그 외 `mdHeading3`. `text`. 낭독: `heading()` |
| `Paragraph(spans)` | `body`, `bodyText` |
| `Code(code)` | `mono`, `text`, `surface2` 바탕, 반경 10dp, 안쪽 14 × 12, 줄바꿈 없이 가로 스크롤 |
| `Quote(spans)` | `body` 기울임, `text2`, 왼쪽 3dp `accent` 막대, 시작 여백 12. 안의 기울임 구간은 세워 구별하고, 코드 구간도 서 있다 |
| `BulletList(items)` / `OrderedList(items, start)` | 항목 사이 6dp, 기호 `text2`, 내용 `bodyText`. 기호 칸 = 그 목록 기호를 잰 최댓값(글머리 `•  ` · 번호 `n. ` · 체크가 있으면 상자 칸 1.625em) — 한 자리 번호 · 1단 글머리는 글자 폭 그대로다. 번호는 `start` 부터 칸 끝 맞춤, 글머리는 깊이마다 `•` `◦` `▪`. 체크 항목은 `CheckBox`/`CheckBoxChecked` 1.25em `text2`, 첫 줄 가운데, 번호 목록이면 번호 칸 뒤. 기호는 내용의 첫 기준선에 맞춘다. 항목의 하위 블록은 항목 글 시작선에서 시작하고 글 ↔ 하위 · 하위끼리 6dp. 들여쓰기는 3단에서 목록만 멈춘다 — 4단부터 목록은 3단 기호 x, 그 항목의 다른 하위 블록은 자기 글 시작선. 첫 문단 없는 항목은 첫 하위 블록이 기호 옆에서 시작한다. 낭독: 체크 항목 글에 상태 설명 `ds_block_checked` · `ds_block_unchecked`(글이 없으면 상자가 읽는다), 역할 · 클릭 없음. 파일 `NoteList.kt` |
| `Table(header, rows, align)` | 칸 격자 — 바깥 1dp `border` 반경 10, 칸 사이 1dp `border`, 머리 행 `surface2`. 칸 `body` 줄 높이 ×1.5 `bodyText`, 머리 600 `text`, 안쪽 12 × 8, 열 정렬(없음 = 시작). 같은 행 칸은 첫 줄 기준선을 맞춘다 — 가장 낮은 기준선에 맞춰 내려 놓는다(한글 대체 글꼴 · 라틴 · 코드의 줄 지표가 달라서). 칸 안 코드는 앞뒤 여백까지 한 낱말(줄이 안 바뀌는 공백). 열 폭: 목표 T = 가장 긴 줄(≤ 16em) · 최소 M = min(T, max(가장 긴 낱말, 6em)) → ΣT 가 들어가면 T · ΣM 이 들어가면 M + 남는 폭을 (T − M) 비율로(표 = 본문 폭) · 아니면 M 으로 가로 스크롤. 표 폭은 내용 폭이고 시작 쪽에 붙는다. 가려진 쪽 24dp `bg` 페이드(`horizontalScrollFade`), 스크롤바 없음. 낭독: `collectionInfo`(행 · 열) · 칸 `collectionItemInfo` · 머리 칸 `heading()`. 파일 `NoteTable.kt`, 열 폭 순수 함수 `TableColumnWidths.kt` |
| `Divider` | 1dp `border`, 위아래 8dp |
| `Spacer` | 8dp |
| `Unsupported(kind, salvaged, raw)` | 앱이 그리지 못한 블록(모르는 종류 `UnknownType` · 깨진 블록 `BrokenBlock` · 노트 전체 `BrokenNote` · 한도 넘음 `LimitExceeded(maxDepth, maxKilobytes, maxTableCells)`). 면 없는 1dp 점선 `border`(대시 4 · 간격 3) 상자, 반경 10, 안쪽 시작 14 · 끝 14 · 위 12 · 아래 4(버튼 없으면 12). 안내 `bodySmall` `text2`(노트 전체 · 한도 넘음이면 제목 `cardTitle` `text` + 본문 — 한도 넘음 문구 `ds_unsupported_limit_*` 의 세 숫자(겹침 단수 · 파일 KB · 표 칸 수)는 화면 모델 인자라 화면 쪽에 다시 적지 않는다) → 8 → 건진 글자 `body` `text2` → 4 → 조용한 버튼 `원문 보기`/`원문 숨기기`(44dp, `button`/500 `text2`, `ChevronRight` 90°/−90°). 펼치면 `surface2` 패널(반경 8, 12 × 10) `mono` `text2`, 줄바꿈 + 감긴 줄은 (앞 공백 + 2)칸 들여쓰기, 앞 40줄 → `N줄 더 보기` → 최대 200줄 · 10,000자, 길게 눌러 복사. `accent`·`warn`·`wrong` 없음. 파일 `UnsupportedBlock.kt`, 원문 자르기 `RawSlice.kt` |

블록 사이는 12dp. 구간 스타일 `NoteSpanStyle` 은 `Normal` · `Bold`(700, `text`) · `Code`(고정폭, `surface2` 바탕, `accent` 글자, 앞뒤 공백 한 칸). 여기에 `italic`(`FontStyle.Italic` — 인용 안에서는 세움) · `strike`(`LineThrough`, 줄 색 = 그 구간 글자 색)가 겹친다. 코드에는 취소선만 붙고, 인용 안에서도 기울지 않는다(`FontStyle.Normal`). 취소선 구간이 있는 글은 낭독에서 `ds_block_strike_start` … `ds_block_strike_end` 로 감싼다. 파일 `NoteSpans.kt`. 목록 하위 자리의 `Unsupported` 는 원문 보기 없이 안내만 보인다(펼침 상태는 맨 위 블록 자리에만 있다).

블록 종류를 늘리려면 `core/format/AGENTS.md` '새 블록 · 필드를 더할 때' 를 따른다 — 마크다운 변환 · format 캐시 DTO · domain · feature 매퍼 · 이 sealed 집합 · `NoteContent` 의 `when` 이 **한 세트로** 움직인다. JSON 형식 1 은 늘리지 않는다.
앱이 모르는 블록은 동기화를 멈추지 않고 그 자리에 `Unsupported` 상자로 보인다(루트 `AGENTS.md` "콘텐츠가 앱보다 먼저 나가면", 테스트 `core/format` `MyNoteJsonFormatTest`).

### 8.7 차트 — `ds/chart/` [통계(진행 중)]

Canvas 로 그리는 통계 차트 5종. 전부 `progress: () -> Float`(`rememberChartProgress(key)` 가 만든다)와 `contentDescription` 을 받는다.

| 이름 | 용도 |
|---|---|
| `TimeBars` | 시간 축 막대(`ChartBar(label, value, kind)`, `maxBarHeight`) |
| `CategoryDonut` | 카테고리 도넛 + 가운데 합계(`ChartSlice(value, color)`, `total`, `unit`) |
| `CategoryStackBar` | 카테고리 가로 누적 막대 |
| `HBarRow` | 제목·부제·값이 붙은 가로 막대 행(`fraction`, `highlighted`) |
| `DivergingBars` | 위·아래로 갈라지는 막대(`DivergingBar(label, up, down, kind)`) |

`ChartBarKind` 는 `Past` · `Current` · `Pending`. 등장 지연·곡선은 `ChartMotion` 한 곳에 있다.

### 8.8 컴포넌트를 고르는 법

#### 노트를 보여 주는 네 가지

| 컴포넌트 | 쓸 때 | 쓰지 말 때 |
|---|---|---|
| `NoteCard` | 홈 그리드의 노트 하나. 칩·날짜·제목이 리더 헤더로 이어진다 | 북마크 화면(모양이 달라 공유 요소를 안 쓴다 — BM3 §3.7) · 목록 패널 |
| 북마크 큰 카드(`feat/screen/bookmarks/`) | 한 화면의 "가장 최근 하나" 를 크게 | 여러 개를 나란히 — 큰 카드는 화면당 하나 |
| `NumberedRow` | **순서에 뜻이 있는** 목록(그전에 모은 북마크, 읽다 멈춘 노트). 번호는 표시 순서이지 키가 아니다(BM3 §3.4) | 순서가 무의미한 목록 — 번호가 거짓 위계를 만든다 |
| `NoteRow` · `CategoryRow` | 목록 패널의 카테고리/노트 트리 | 패널 밖 |

#### 북마크 표기는 자리마다 하나

| 자리 | 표기 | 이유 |
|---|---|---|
| 홈 카드 | `BookmarkRibbon` | 카드 윤곽이 바뀌어 훑을 때 걸리고, 색 없이 모양으로 읽힌다(BM2 1c) |
| 목록 패널 행 | `BookmarkGlyph` | 행이 낮아 리본 자리가 없다(BM3 §1) |
| 북마크 화면 | 표기 없음, 채운 해제 버튼만 | 화면 전체가 북마크라 표기가 중복이다(BM3 §1) |
| 리더 상단 바 | 토글(꺼짐 선 `bodyText`, 켜짐 채움 `accent`) | 스크롤해도 사라지지 않는 유일한 크롬 — "이 대상에 대한 동작" 자리(BM1 §1.1) |

#### 라벨과 입력

- **`CategoryChip`** — 노트의 분류 이름 하나. **누를 수 없는 라벨**이다. 필터·선택 칩으로 쓰지 않는다 — 선택 칩은 면·테두리·글자가 같이 바뀌는 별도 모양이다(STA §7 섹션 칩). 길면 칩이 먼저 말줄임되고 날짜는 줄지 않는다(BM3 §2.2).
- **`SectionLabel`** — 묶음의 이름 + 개수/힌트. 빈 묶음이면 라벨도 그리지 않는다(BM1 §4.3 "섹션 헤더와 개수는 빈 상태에서 그리지 않는다").
- **`SearchField`** — 목록 패널 필터 하나. 검색 중에는 카테고리를 모두 펼치고 빈 카테고리를 숨긴다(SN Interactions).
- **경고 배지**(`feat/screen/ListPanelScreen.kt` `WarnBadge`) — "사용자가 손봐야 할 것" 이 있는 줄에만(토큰 없음·모델 없음). 정보성 배지로 쓰지 않는다.

#### 떠 있는 크롬

- **플로팅 내비(바/레일)** — 최상위 목적지(홈·북마크·통계) 이동만. **동작을 넣지 않는다**(BM1 §1.1 "내비는 화면 간 이동용이다. 대상 동작을 섞으면 선택 상태 규칙이 흐려진다"). 설정·퀴즈 구간에서는 숨긴다(ST §3, QZ §6).
- **`ScrollToTopButton`** — 긴 스크롤 화면에서만.
- **`FloatingStatusPill`** — 앱 전역의 **오래 걸리는 백그라운드 작업** 하나(모델 내려받기). 한 번 알리고 끝나는 결과는 스낵바다. 알약이 보이는 동안은 콘텐츠 위쪽을 비워 가리지 않는다(ST §10.4, 5.4).
- **`MyNoteSnackbar`** — 결과 확인과 되돌리기. **Toast 는 쓰지 않는다**(QZ §6, ST §4.4). 되돌리기는 마지막 하나만 된다(BM3 §3.6).

#### 버튼

버튼은 아직 design-system 에 없다(8.3). 세 벌이 같은 규칙을 따르도록 아래를 기준으로 한다(DS §08 "설정 버튼").

| 종류 | 쓸 때 | 규칙 |
|---|---|---|
| 주(Primary) | 화면·카드·다이얼로그의 **하나뿐인** 다음 걸음 | `accent` 면 + `bg` 글자. 한 묶음에 둘 두지 않는다 |
| 보조(Secondary) | 주 동작 옆의 대안(바꾸기, 홈에서 노트 고르기) | `surface2` 면 + 1dp `border` |
| 파괴적(Destructive) | 지우기·삭제 | 면 없이 `wrong` 글자. **확인 다이얼로그 안의 최종 삭제만** `wrong` 면(ST §10.3) |
| 조용한(Quiet) | 취소·보기/숨기기·나중에 | 글자만 `text2` |

- 크기는 두 벌이다: **40dp · 반경 12**(설정·카드·다이얼로그 안) / **50dp · 반경 15**(퀴즈 전체화면 하단 — QZ §3.4–3.6). 전체화면 하단 버튼만 큰 쪽을 쓴다.
- 라벨은 **한 줄**이다(ST §4.3). 두 줄이 되면 문구를 줄인다.
- 다이얼로그 버튼은 오른쪽 정렬, 전체폭이 아니다(QZ §3.2 "그건 시트 문법이다").
- 비활성은 alpha 0.4 이고 **이유가 화면 어딘가에 보여야** 한다(빈 입력이면 저장이 비활성 — ST §4.3).

#### 다이얼로그 · 시트 · 화면 안 펼침

- **다이얼로그** — 되돌릴 수 없는 확인(모델 삭제, 기록 지우기), 비용이 큰 시작의 확인(모델 받기), 시작 전에 고를 것이 하나 있는 동작(퀴즈 문항 수). 제목은 질문, 본문은 결과·비용, 버튼은 동사(9.4).
- **바텀시트는 쓰지 않는다.** 시트는 목록에서 고르는 그릇이다(QZ §3.2 "바텀시트가 아니다").
- **화면 안 펼침** — 카드 안에서 끝나는 편집(토큰 입력)은 다이얼로그를 띄우지 않고 그 카드를 펼친다(ST §4.3 "다이얼로그 쓰지 않음").

#### 모션 프리미티브

| 프리미티브 | 쓸 때 | 쓰지 말 때 |
|---|---|---|
| `riseIn` | 화면이 처음 그려질 때 블록이 순서대로 들어오기 | 스크롤로 다시 보이는 항목 · 상태가 바뀔 때마다 |
| `flip` | 다이얼로그 등장(퀴즈 설정 — DS §08) | 화면 전환 |
| `aiShine` | **AI 동작의 입구 하나**(퀴즈 CTA, 모델이 준비됐을 때만 — ST §10.6) | 한 화면에 둘 이상 · AI 가 아닌 것 · 모델이 없을 때 |
| `fadingEdges` | 스크롤 영역의 위·아래 끝 | 스크롤하지 않는 영역 |
| `PressIndication` | 누르는 모든 것의 눌림(2.3 눌림 표) | 자기 눌림을 그리는 내비 |

#### 차트

- 차트는 숫자를 대신하지 않고 **숫자를 보조**한다. 값 라벨·범례 값·낭독 문장이 늘 같이 간다(STA §9).
- 0 은 "없음" 과 다르게 그린다 — 0 은 기준선 막대, 아직 안 온 칸은 점선(STA §4.1).
- 같은 데이터면 다시 자라지 않는다. 기간·섹션을 바꿨을 때만 다시 그린다(STA §7).

---

## 9. 문구

### 9.1 리소스 규칙

- 화면 문구는 전부 `strings.xml`. 코드에 한글 리터럴을 두지 않는다. 예외는 프리뷰 더미와 개발자용 `error()`·예외 메시지뿐이다.
- 모듈별 접두사가 Gradle `resourcePrefix` 로 **강제**된다 — design-system `ds_`, feature:notes `notes_`. 라이브러리 리소스는
  앱에 합쳐질 때 이름이 겹치면 조용히 덮이기 때문이다.
- design-system `strings.xml` 에는 여러 화면이 공유하는 액션 문구와 이 모듈 컴포넌트가 쓰는 문구만 둔다.
  feature 는 `import com.hancekim.mynote.core.designsystem.R as DsR` 로 참조한다.
- 파라미터 기본값에 문자열 리터럴을 두지 않는다. `placeholder: String? = null` + `stringResource` 폴백으로 받는다.
- 다국어는 하지 않는다. 리소스로 모으는 이유는 번역이 아니라 한 곳에 모으는 것이다.

### 9.2 공용 문구 `ds_*`

`core/design-system/src/main/res/values/strings.xml`:

| 이름 | 문구 |
|---|---|
| `ds_action_confirm` | 확인 |
| `ds_action_cancel` | 취소 |
| `ds_action_save` | 저장 |
| `ds_action_close` | 닫기 |
| `ds_action_retry` | 다시 시도 |
| `ds_action_undo` | 되돌리기 |
| `ds_action_back` | 뒤로 |
| `ds_scroll_to_top` | 맨 위로 |
| `ds_search_hint` | 노트 검색 |
| `ds_bookmarked` | 북마크됨 |
| `ds_unsupported_unknown_text` | 이 앱 버전이 모르는 형식이라 글자만 표시했습니다. |
| `ds_unsupported_unknown_empty` | 이 앱 버전이 모르는 형식이라 표시하지 못했습니다. |
| `ds_unsupported_broken_text` | 형식이 맞지 않아 글자만 표시했습니다. |
| `ds_unsupported_broken_empty` | 형식이 맞지 않아 표시하지 못했습니다. |
| `ds_unsupported_update_hint` | 앱을 업데이트하면 보일 수 있습니다. |
| `ds_unsupported_show_raw` | 원문 보기 |
| `ds_unsupported_hide_raw` | 원문 숨기기 |
| `ds_unsupported_more_lines` | %1$d줄 더 보기 |
| `ds_unsupported_truncated` | 앞 %1$d줄만 표시했습니다. |
| `ds_unsupported_note_title` | 이 노트를 읽지 못했습니다 |
| `ds_unsupported_note_body` | 노트 파일의 형식을 알아볼 수 없습니다. 파일이 바뀌거나 앱을 업데이트하면 다시 읽습니다. |
| `ds_unsupported_limit_title` | 이 노트는 읽기 한도를 넘었습니다 |
| `ds_unsupported_limit_body` | 목록이나 인용이 %1$d단보다 깊게 겹쳤거나, 파일이 %2$dKB를 넘었거나, 표 칸이 합쳐서 %3$,d개를 넘었거나, 읽은 결과가 너무 커서 읽지 않았습니다. 파일을 나누거나 겹침과 표를 줄이면 다시 읽습니다. |

### 9.3 말투

말투는 **합니다체** 하나다 — 서술 `~합니다`/`~습니다`, 권유·지시 `~하세요`, 확인 질문 `~할까요?`.

### 9.4 보이스와 쓰는 법

#### 핸드오프 문구를 옮길 때

초기 핸드오프(SN·QZ)는 해요체·반말로 쓰였고 코드가 전부 합니다체로 바꿨다(PRD `backlog.md` 8.2 #14). 핸드오프 문구는 말투를 바꿔 옮긴다.

| 핸드오프 원문 | 앱 문구 |
|---|---|
| 읽고 싶은 노트를 골라보세요. 왼쪽 위 ☰ 를 누르면 전체 목록이 열려요. | 읽고 싶은 노트를 고르세요. 왼쪽 위 ☰ 를 누르면 전체 목록이 열립니다. |
| 검색 결과가 없어요 | 검색 결과가 없습니다 |
| 3문제만 만들어졌어 | 3문제만 만들었습니다 |
| 처음 한 번은 10초쯤 걸려 | 처음 한 번은 10초쯤 걸립니다 |
| 다 맞았어 | 다 맞혔습니다 |
| 한 번 더 읽어보자 | 한 번 더 읽어 보세요 |
| 퀴즈를 그만뒀어 | 퀴즈를 그만뒀습니다 |

#### 문장 모양

| 자리 | 모양 | 예 |
|---|---|---|
| 상태 라벨 | 명사구 또는 `~하는 중`, 마침표 없음 | 받는 중 · 파일 확인 중 · 연결됨 · 멈춤 · 사용 가능 |
| 스낵바(결과) | 과거형 `~했습니다`, 마침표 없음 | 북마크에 추가했습니다 · 토큰을 지웠습니다 |
| 안내 문장 | 완결된 문장 + 마침표 | 이 기기에만 저장됩니다. |
| 버튼 | 짧은 동사형 | 받기 · 이어 받기 · 다시 시도 · 만들기 · 되돌리기 |
| 확인 다이얼로그 | 질문 제목 + 결과 본문 + 동사 버튼 | 퀴즈 모델을 삭제할까요? / 저장 공간 2.6GB가 비워집니다. … / 취소 · 삭제 |
| 빈 상태 | 무엇이 없나 + 어떻게 채우나 + 버튼 하나 | 북마크한 노트가 없습니다 / 노트를 읽다가 위쪽 바의 북마크 아이콘을 누르면 여기에 모입니다. / 홈에서 노트 고르기 |
| 실패 | 무엇이 안 됐나 + 왜(사람 말) + 다음 수 | 모델을 불러오지 못했습니다. / 모델 파일을 다시 내려받아야 할 수 있습니다. |

#### 원칙

- **사용자 탓을 하지 않는다.** 형식이 깨진 건 사용자 잘못이 아니니 원인을 사람 말로 한 줄(QZ §3.4).
- **상한만 약속한다.** "최대 4문제", "노트가 짧으면 이보다 적게 나올 수 있습니다"(QZ §3.2).
- **막다른 길 대신 통하는 두 번째 수를 준다.** "최대 3문제로 줄여서 다시"(QZ §3.4).
- **크기·조건을 미리 말한다.** "2.6GB · Wi-Fi에서 받습니다"(ST §10.2) — 미리 말해 두면 확인 다이얼로그를 줄일 수 있다(ST §10.3 "설정 카드의 받기는 확인을 건너뛴다").
- **낭독 문구는 보이는 문구와 따로 쓴다.** 행 낭독은 "{번호}번째, {제목}, {카테고리}, {시각}" 처럼 쉼표로 이은 사실이다(BM3 §3.4). 보이는 문구를 그대로 읽히면 기호·줄바꿈까지 읽힌다.
- 숫자와 단위는 붙여 쓴다: `2.6GB` · `42%` · `3문제` · `7개`. 시각은 24시간제(BM3 §3.5).

#### 쓰지 않는 표현 (디자이너 제안 — 지금 문구에 어긋난 곳은 없다)

- 해요체·반말(`~해요`, `~했어`), 느낌표, 이모지.
- 내부 용어: JSON · 파싱 · WorkManager · 해시. 대신 "형식에 맞지 않게 답해서", "파일 확인 중".
- 막연한 오류 문장: "오류가 발생했습니다", "알 수 없는 오류". 무엇이 안 됐는지를 말한다. (배지 "모델 오류" 처럼 상태의 *이름*으로 쓰는 것은 괜찮다 — 자세한 문장은 그 배지가 여는 설정 화면이 말한다.)
- 범위 약속: "3~5문제", "곧 끝납니다"(QZ §3.2).
- "클릭" — 터치 기기다. "누르다" 를 쓴다.
- 고유명사 밖의 영어 섞기. GitHub · Wi-Fi · Gemma 는 고유명사라 그대로 쓴다.

---

## 10. 접근성

### 10.1 코드가 지키는 것

- 클릭 가능한 `Box`/`Text` 는 `role` 을 준다(`Role.Button`·`Role.Tab`·`Role.RadioButton`). 내비 항목은 `selectable(role = Role.Tab)`.
- 장식 아이콘은 `contentDescription = null`. 내비 바는 선택된 항목의 라벨이 보이므로 그 아이콘의 설명을 뺀다(비선택 항목·레일은 라벨을 설명으로 단다).
- 북마크 리본은 설명이 없고, 카드 노드의 `stateDescription` 이 `ds_bookmarked` 다. 글리프는 자체 설명 `ds_bookmarked` 를 단다.
- `SectionLabel` 은 `heading()`, `NumberedRow` 는 `mergeDescendants` + 문장 하나로 된 `contentDescription`.
- 리더의 노트 제목과 본문 제목 블록은 `heading()` — TalkBack 제목 단위 이동이 절 제목에 선다(표 머리 칸도 `heading()`).
- 차트는 전부 `contentDescription` 을 필수로 받는다.
- `correct`/`wrong` 은 아이콘·글자와 함께 쓴다(2.1).
- TalkBack(터치 탐색)이 켜지면 퀴즈 자동 진행을 끄고 버튼을 띄운다(`feat/quiz/QuizAutoAdvancePolicy.kt`).
- 감속 모션 — 6.5.
- 글자 크기 — 사용자 토글 `MyNoteFontScale`(16/18sp, 3.2). 시스템 글꼴 배율은 `sp` 로 따로 적용된다.

### 10.2 접근성 원칙

#### 대비

2.1 의 토큰 값으로 계산한 WCAG 2.x 대비(작은 글자 기준 4.5:1, 그래픽·큰 글자 3:1).

| 글자·그래픽 | `bg` 위 | `surface` 위 | `surface2` 위 | 판단 |
|---|---|---|---|---|
| `text` | 15.9 | 14.5 | 13.4 | 통과 |
| `bodyText` | 12.3 | 11.2 | 10.4 | 통과 |
| `text2` | 7.7 | 7.0 | 6.5 | 통과 — 뜻을 지는 글자는 여기까지 |
| `text3` | 4.0 | **3.65** | **3.38** | 작은 글자 4.5 미달 → 없어도 되는 정보만 |
| `accent` | 7.8 | 7.1 | 6.6 | 통과 |
| `correct` · `wrong` · `warn` · `warnText` | 7.5–11.1 | 6.8–10.1 | 6.3–9.4 | 통과 |
| 주 버튼 글자(`bg` on `accent`) | 7.8 | | | 통과(눌림 `accentPressed` 6.4) |
| `chartAccent2` | 4.6 | 4.2 | 3.9 | 그래픽 통과 |
| `chartAccent3` | 2.7 | **2.45** | 2.3 | 그래픽 3:1 **미달**(15절 #16) |
| `border` | 1.4 | 1.3 | 1.2 | 장식. 경계를 테두리 하나로 알리지 않는다 |

유리 위(`glass` 를 `bg` 에 합성): `text2` 7.1, `text3` 3.7. 선택 면 위(`accentSoft` on `surface`): `accent` 5.5.

- **뜻을 지는 글자는 `text2` 이상.** `text3` 에 놓인 정보가 사라졌을 때 곤란하다면 그 글자는 `text3` 이 아니다. 지금 이 규칙에 걸리는 자리가 있다(15절 #17).

#### 터치

- **누르는 것은 44dp 이상**(QZ §2, ST §7). 보이는 크기가 작아도 터치 영역은 44 다 — 북마크 해제 버튼은 작은 아이콘에 44dp 영역이고, 카드 높이가 변하지 않게 음수 여백으로 넣는다(BM1 §4.3, BM3 §3.3).
- 자주 누르는 줄은 더 크게 — 퀴즈 선택지 56dp(QZ §2).
- 보이는 높이가 44 보다 낮은 버튼(40dp 버튼·상태 알약·조용한 버튼)은 터치 영역만 넓힌다(ST §7 "버튼 40dp 는 `minimumInteractiveComponentSize` 로 48dp 확보"). 15절 #20. 목록 행은 반대로 행 높이 자체를 44 이상으로 둔다 — 행 전체가 누르는 면이다.

#### 색만으로 말하지 않는다

- 정답·오답 = 색 + ✓/✕ + "정답/오답" 글자(QZ §6).
- 선택 = 색 + 굵기(STA §9 "선택 표시는 색 + 굵기 둘 다").
- 차트 계열 = 색 + 범례 이름 + 값(STA §2.1).
- 상태 점 옆에는 늘 상태 라벨이 있다(ST §10.2).

#### 낭독

- 카드·행은 **한 덩어리**로 읽히고 상태는 덧붙인다 — "TCP 3-way handshake, 네트워크, 9월 28일, 북마크됨"(BM3 §2.3).
- 장식(리본·구분선·장식 아이콘)은 읽지 않는다. **같은 뜻을 두 번 읽히지 않는다**(범례 행은 차트 설명과 겹쳐 읽지 않는다 — STA §9).
- 차트·진행 표현은 **문장 하나**로 설명한다 — 값이 없는 칸까지 "아직 없습니다" 로(STA §9).
- 역할을 준다: 버튼 · 탭(내비·세그먼트·섹션 칩) · 라디오(선택지) · 스위치(스위치 행).
- 스낵바는 정중하게(Polite) 읽힌다. 가린 토큰은 점을 읽지 않고 "끝자리 a3F9" 로(ST §7).

#### 시간과 움직임

- 감속 모션은 6.6.
- 시간이 지나면 저절로 넘어가는 것은 TalkBack 이 켜져 있으면 끄고 버튼을 띄운다(QZ §6, 10.1).
- 되돌리기 창은 스낵바가 떠 있는 동안이다 — 읽고 누를 시간을 준다.

#### 글자 크기

- 앱의 "본문 글자 크게" 는 본문만 키운다(3.2). 시스템 글꼴 배율은 모든 글자에 걸린다(10.1).
- 시스템 배율이 커져도 **버튼 라벨은 한 줄**이어야 한다(8.8). 넘치면 문구를 줄이지 크기를 고정하지 않는다.

---

## 11. Do / Don't

| | Do | Don't |
|---|---|---|
| 1 | 강조는 `accent` 하나로, 한 화면에 몇 군데만 | 북마크·상태 *표기*에 accent — 표기는 회색 |
| 2 | 뜻을 지는 글자는 `text2` 이상 | `text3` 으로 안내 문장 쓰기 |
| 3 | 떠 있는 크롬만 유리로 | 카드·본문·선택지·스낵바에 유리 |
| 4 | 카드는 1dp 테두리와 면 밝기로 층을 말한다 | 카드에 그림자 · 바닥 위에 넓은 `surface2` |
| 5 | 눌림은 한 층 올리기(2.3 눌림 표) | 리플 · 검정 덮개 |
| 6 | 되돌릴 수 있는 동작은 바로 하고 스낵바 되돌리기 | 되돌릴 수 있는 일에 확인 다이얼로그 · Toast |
| 7 | 주 버튼은 묶음에 하나, 다이얼로그 버튼은 오른쪽 정렬 | 주 버튼 둘 · 다이얼로그에 전체폭 버튼 · 바텀시트 |
| 8 | 들어올 땐 감속, 나갈 땐 가속·더 짧게 | 사라지는 항목에 도착 곡선 · 기다림 밖의 무한 루프 |
| 9 | 새 모션에 감속 모션 분기를 같이 만든다 | 감속 모션을 "나중에" |
| 10 | 두 단은 실제 콘텐츠 폭으로, 열 수는 카드 최소 폭으로 | 창 크기 클래스 이름으로 두 단 판정 · 고정 열 수 |
| 11 | 정답·오답·선택은 색 + 모양·글자·굵기 | 색 하나로 상태 구분 |
| 12 | 합니다체, 상한만 약속, 실패는 원인 + 다음 수 | 해요체·반말 · "3~5문제" · "오류가 발생했습니다" |
| 13 | 안 되는 기능의 입구는 남기고 이유를 보인다 | 모델이 없다고 CTA 를 지우기 |

---

## 12. 새 화면 체크리스트

### 12.1 코드 체크리스트

- [ ] 화면은 `feature/notes` 의 기존 구조 안에 끼운다(새 모듈이 아니다). 목적지 키·씬은 `:core:navigation`.
- [ ] 화면 루트는 `Surface(color = colors.bg)`. 프로덕션의 `MyNoteTheme { }` 는 `NotesRoute.kt` 한 곳뿐이므로 화면은 테마를 감싸지 않는다.
- [ ] 색은 `MyNoteTheme.colors`, 글자는 `MyNoteTheme.textStyles`, 창 크기는 `MyNoteTheme.adaptive`. `Color(0x…)` 0건, `MaterialTheme` 0건.
- [ ] Material3 컴포넌트를 쓰면 색을 **명시**한다. `colorScheme` 이 제공되지 않아 생략하면 머티리얼 기본색이 뜬다.
- [ ] 화면에서 창 크기를 다시 재지 않는다(`BoxWithConstraints` 금지). 2단 여부·여백은 셸(`NotesLayout`)이 넘긴다.
- [ ] 하단·오른쪽 여백은 `contentBottomPadding`/`contentEndPadding` 을 받는다 — 플로팅 내비에 가리면 안 된다.
- [ ] 유리는 `GlassSurface` 로만. 오버레이는 `layerBackdrop` Box 밖.
- [ ] 애니메이션 값은 `graphicsLayer { }`/draw 람다 안에서 읽는다. 무한 애니메이션은 `rememberReduceMotion()` 을 실제로 반영한다.
- [ ] 문구는 `notes_*`(공용이면 `ds_*`), 합니다체.
- [ ] `@Preview` 가 `MyNoteTheme(windowClass = …, heightClass = …)` 로 감싸고 폼팩터를 넘긴다. 여러 상태는 `PreviewParameterProvider`.
- [ ] 컬렉션 파라미터는 `ImmutableList`.

### 12.2 디자인 체크리스트

12.1 이 코드 규약이라면 이건 화면을 그리기 전·후에 디자인 쪽에서 보는 목록이다.

**구조**
- [ ] 이 화면은 최상위 목적지(내비 칸) · push 되는 화면 · 리더 위 상태(퀴즈처럼) 중 무엇인가? 그에 따라 내비를 보일지 정했나?
- [ ] 주 동작은 하나인가? 나가는 길은 하나인가?
- [ ] 상태를 다 그렸나 — 빈 상태 · 기다림 · 실패 · 하나뿐 · 많음 · 감속 모션.
- [ ] 되돌릴 수 있는 동작에 스낵바 되돌리기, 되돌릴 수 없거나 비용이 큰 동작에만 확인 다이얼로그인가?

**폼팩터** — 넷을 다 본다
- [ ] 폰 세로 · 펼친 폴드(패널 펼침/접힘) · 가로 폰(높이 Compact) · 넓은 창.
- [ ] 두 단이 필요하면 콘텐츠 폭 640 기준인가? 레일이 뜨면 누를 것이 레일 아래 깔리지 않나?
- [ ] 줄 길이에 상한이 있나?
- [ ] 가로 폰에서 핵심 블록이 한 화면에 들어오나?

**색·글자·면**
- [ ] accent 를 쓴 자리가 "지금·진행·주 동작·본문 표지" 중 하나인가?
- [ ] 반투명 면은 2.3 알파 표에서, 눌림색은 2.3 눌림 표에서 골랐나?
- [ ] 스타일은 역할로 골랐나? 문장 14sp 이상, 뜻을 지는 글자 `text2` 이상인가?
- [ ] 반경이 4.6 위계에 맞나(떠 있을수록 크게, 안에 들수록 작게)?

**모션**
- [ ] 움직임마다 6.6 "언제 움직이나" 넷 중 무엇인지 말할 수 있나?
- [ ] 곡선 성격·길이 대역이 6.6 안인가? 나가는 쪽이 더 짧은가?

**접근성**
- [ ] 누르는 것은 모두 44dp 이상인가?
- [ ] 카드·행이 한 덩어리로, 상태를 덧붙여 읽히나? 장식은 안 읽히나?
- [ ] 색만으로 말하는 자리가 없나? 차트·진행 표현에 낭독 문장이 있나?

**문구**
- [ ] 9.4 문장 모양대로인가? 버튼 라벨이 한 줄인가(글자 크게 + 시스템 배율에서도)?

---

## 13. 새 토큰·컴포넌트 추가

### 13.0 들일지 판단하는 기준

13.1–13.4 가 *어떻게* 넣나라면, 이건 *넣을지* 를 정하는 기준이다.

#### 새 색

1. 기존 역할(2.3)로 안 되는지 먼저 본다. "조금 다른 파랑" 은 대개 `accent` 의 알파 파생이다.
2. 알파 파생은 토큰이 아니다(TK "`.copy(alpha = …)` 로 즉석에서 만드는 값은 토큰이 아니라서 넣지 않았다"). 단 **같은 뜻·같은 알파가 세 곳 이상**이면 이름을 붙여 토큰으로 올린다.
3. 한 영역 전용이면 접두로 묶는다(`chart*`, `navItem*`). 다른 영역에서 빌려 쓰지 않는다.
4. 들이기 전에 `bg`·`surface`·`surface2` 위 대비를 계산해 10.2 표에 넣는다. 글자면 4.5:1, 그래픽이면 3:1.

#### 새 반경 · 간격 · 크기

- **두 컴포넌트 이상이 같은 값을 같은 뜻으로** 쓰면 토큰, 한 컴포넌트의 생김새면 그 파일의 상수다. 2단 기준 640dp 는 셸 한 곳만 써서 토큰이 아니다(PRD `backlog.md` 8.3).
- 창 크기에 따라 변해야 하면 창 크기 토큰이다. 너비가 정하나(가로 구조), 높이가 정하나(세로 리듬)를 먼저 정한다.

#### 새 타이포

- 같은 역할의 스타일이 있으면 그걸 쓴다(3.5). 컴포넌트 안 `copy` 한 번은 괜찮고, 같은 `copy` 가 세 곳이면 스타일로 올린다.
- 본문 배율을 따라야 하나, 짧은 창에서 줄어야 하나를 정한다(3.2). 둘을 섞지 않는다.

#### 새 컴포넌트 — design-system 에 둘 조건 셋

1. 두 화면 이상이 쓴다(또는 곧 쓴다). `NumberedRow`·`SectionLabel` 은 통계가 두 번째 사용처가 되면서 올라왔다(PRD `backlog.md` 8.2 #57).
2. 도메인을 모른다 — 노트·퀴즈 타입이 아니라 글자·숫자·콜백만 받는다.
3. 이 문서의 토큰만으로 그려진다.

하나라도 아니면 feature 의 화면 전용 위젯이다. 지금 feature 에 세 벌 있는 버튼은 셋을 다 만족한다(15절 #14).

#### 새 모션

- 6.6 "언제 움직이나" 넷 중 하나로 설명되지 않으면 만들지 않는다.
- 곡선은 6.6 성격 표에서, 길이는 대역 표에서 고른다. 새 곡선이 필요하면 이름을 붙여 한 곳(`ds/motion/`)에 둔다.
- 감속 모션 분기를 같이 만든다.

#### 디자인 쪽 사본

토큰을 바꾸면 Claude Design `tokens/`(DTCG JSON)와 Figma 변수를 다시 만든다. 코드가 원본이다(TK "코드가 원본, 피그마는 사본").

### 13.1 어느 모듈에 넣나

| 추가할 것 | 자리 | 고치는 곳 |
|---|---|---|
| 색 | `:core:design-foundation` | `MyNoteColors` 에 필드 + `StudyNotesDarkColors` 에 값. 테마는 홀더를 통째로 들고 있어 자동으로 따라온다 |
| 텍스트 스타일 | `:core:design-foundation` | `MyNoteTextStyles` 필드 + `myNoteTextStyles()`. 짧은 창 분기(`short`)인지 본문 배율(`bodySizeSp`)인지 먼저 정한다 |
| 창 크기에 따라 변하는 값 | `:core:design-foundation` | `AdaptiveTokens` — "가로 구조" 면 `WidthTokens`, "세로 리듬" 이면 `HeightTokens`. 두 축의 함수면 계산 프로퍼티 |
| 반경 | `:core:design-foundation` | `MyNoteRadius`(`object` + `val`) |
| 새 **종류**의 홀더(CompositionLocal) | `:core:design-system` | `theme/MyNoteTheme.kt` 에 Local 하나 더 + `remember(키)` |
| 모션 상수·이징 | `:core:design-system` | `motion/`. `theme/` 에 넣지 않는다 |
| 재사용 위젯·Modifier | `:core:design-system` | `component/` |
| 차트 | `:core:design-system` | `chart/`. 지연·곡선은 `ChartMotion` |
| 본문 블록 | 여러 모듈 한 세트 | 8.6 마지막 문단 |
| 화면 전용 위젯 | `:feature:notes` | `quiz/component/` 처럼 화면 옆 |

### 13.2 토큰 규칙

- `Color(0x…)` 리터럴은 `MyNoteColors.kt` 에만. 예외는 `@Preview(backgroundColor = …)` 뿐인데, 애노테이션 인자는 상수만 받아 토큰을 못 쓴다.
  `0xFF0E1217` = `bg`, `0xFF161C24` = `surface` 를 **손으로 베낀 값**이라 팔레트를 바꾸면 같이 고친다.
- 토큰 홀더는 멤버가 진짜 불변이라 `@Immutable` 이다. `MyNoteRadius` 는 `object` + `val` 이라 본래 stable 이라 붙이지 않는다.
- foundation 에는 `@Composable`·`Context`·리소스를 들이지 않는다. 들이면 JUnit4 만으로 도는 `AdaptiveTokensTest` 가 계측 테스트로 밀린다.
- **안 쓰는 토큰은 남기지 않는다.** 팔레트가 유일한 출처라서 안 쓰는 이름도 "있는 값" 처럼 읽힌다.
- 한 화면 전용 타이포 스케일을 그 화면에 두지 않는다(퀴즈가 자기 스케일 11종을 들고 있다가 foundation 으로 합쳐졌다).
- `AdaptiveTokens` 값을 고치면 `AdaptiveTokensTest` 가 먼저 빨개진다. 의도한 변경이면 테스트를 같이 고친다.
- 버전·좌표는 `gradle/libs.versions.toml` 에만. 모듈은 `hancenote.*` 컨벤션 플러그인만 적용한다(design 두 모듈은 `hancenote.android.library.compose`).

### 13.3 컴포넌트 규칙

- 시그니처: 필수 인자 → `modifier: Modifier = Modifier` → 선택 인자 → 마지막 `content` 슬롯. 컬렉션은 `ImmutableList`(`List<T>` 금지).
- 데이터·도메인·nav3·MVI 를 모른다. 콜백은 `onClick`/`onSelect` 같은 순수 람다. 공유 요소는 `Modifier` 슬롯으로만 받는다(`NoteCard(chipModifier = …)`).
- 애니메이션: 6.4 의 세 방식 중 하나. 감속 모션 판정은 노드 안이나 컴포넌트 안에서 한다(6.5 의 호출부 누락 참고). 상태가 람다면 `() -> Float` 로 노출한다.
- `content()` 는 정확히 한 번 부르고 분기는 `Modifier` 쪽에서 한다(ktlint `content-slot-reused`).
- `@Immutable` 클래스에 `equals`/`hashCode` 를 손으로 구현했다면(예: `PressIndication`) 파라미터를 추가할 때 둘 다 고친다.
- 같은 파일에 `@Preview` 동반, `MyNoteTheme { }` 로 감싸고 다크 배경 지정. 여러 상태는 `private class …PreviewParameterProvider`.
- 클릭 가능하면 `role`, 장식 아이콘은 `contentDescription = null`. 문구는 `ds_*` 리소스.
- 판정 로직은 Compose 를 모르는 순수 상태 홀더 + `remember…` 어댑터로 나눈다 — `FloatingNavScrollState` 가 그래서 단위 테스트가 된다.

### 13.4 테마 사용 예

```kotlin
@Composable
fun ExampleCard(
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(MyNoteRadius.card)
    Column(
        modifier =
            modifier
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .clickable(onClick = onClick, role = Role.Button)
                .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Text(caption, style = MyNoteTheme.textStyles.caption, color = colors.text3)
        Text(title, style = MyNoteTheme.textStyles.cardTitle, color = colors.text)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun ExampleCardPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Compact, heightClass = MyNoteHeightClass.Medium) {
        ExampleCard(title = "Compose 상태 관리 정리", caption = "2026.06.12", onClick = {})
    }
}
```

창 크기 토큰 읽기:

```kotlin
val adaptive = MyNoteTheme.adaptive
val maxWidth = adaptive.readingMaxWidth            // null 이면 제한 없음
Modifier.padding(horizontal = adaptive.page)
if (adaptive.useFloatingRail) { /* 레일 형태 */ }
```

`MyNoteTheme { }` 를 인자 없이 부르면 **Compact × Medium × Normal** 이다. 폼팩터를 보려면 프리뷰에서 명시한다.
`MyNoteTheme` 밖에서 `MyNoteTheme.colors` 등을 읽으면 `error("… MyNoteTheme 으로 감싸세요.")` 로 죽는다.

---

## 14. 출처

| 무엇 | 어디 | 비고 |
|---|---|---|
| 정본 | 코드 — `:core:design-foundation`, `:core:design-system` | 이 문서와 다르면 코드가 맞다 |
| Claude Design 프로젝트 | `190a5d4b-9ad2-4d81-ad76-7c2fb0fb4802`(MyNote) | 일반 프로젝트(디자인 시스템 타입 아님). 토큰 규약은 그 안의 `tokens/README.md` — 컬렉션 color·dimension·window·height·typography·reading·motion |
| 핸드오프(Claude Design 안) | `design_handoff_study_notes/` · `design_handoff_nav3_responsive/` · `design_handoff_quiz/` · `design_handoff_settings/` · `design_handoff_bookmark/`(`_v2/`·`_v3/`) · `design_handoff_stats/` 의 `README.md`, `Design System.dc.html`, `tokens/` | 약어는 0.4. 사본이고 코드가 원본이다 |
| 핸드오프(레포 로컬 사본) | `docs/design_handoff_settings/`(`README.md` · `Note App.dc.html` · `screenshots/`) | 설정 화면. 기준 커밋 `dcbb3e7`. **`/docs/` 는 gitignore 라 로컬에만 있다** |
| PRD | `docs/prd/` | 로컬 전용(위와 같음) |
| Figma 파일 | fileKey `zttNYcZEWK6amgyYq98y4A` | 코드 기준 커밋 `dcbb3e7` 에서 만들었다. 2026-10-01 시점 변수 219개 / 7컬렉션, 텍스트 스타일 24, 이펙트 스타일 7, 컴포넌트는 아이콘까지(원자 컴포넌트부터 미완) |

---

## 15. 디자인 ↔ 코드 차이

디자인 시스템 수준(토큰·컴포넌트·곡선·말투)만 적는다. 화면 동작의 차이는 PRD `backlog.md` 8.2 #1–#65 에 있어 여기 다시 적지 않는다.
**코드를 고치지 않고 제안만 적는다.** 코드 사실은 파일:줄로 확인했고, "출처" 는 차이를 처음 짚은 쪽이다.

| # | 항목 | 코드(사실) | 디자인/기대 | 제안 | 출처 |
|---|---|---|---|---|---|
| 1 | 간격 토큰 | **해결(2026-10-07)** — 쓰는 곳이 0이던 `Spacing`(4·8·12·16·24·32)을 지웠다("안 쓰는 토큰은 남기지 않는다"). 실제 여백은 dp 리터럴(16·10·8·12·6·20·14 순 — 4.2)과 창 크기 토큰 | 레이아웃 간격은 한 단계표에서 고른다(SN Spacing, 4.6) | 단계표가 필요해지면 실제 반복 값으로 새로 만든다 | 개발자 |
| 2 | 반경 토큰 | `MyNoteRadius` 는 14·10·8·5. `inlineCode`(5) 사용 0회 — 인라인 코드는 `SpanStyle` 배경이라 반경이 없다. 토큰 밖 리터럴 30·26·24·23·22·20·18·15·14·13·12·10·9·8·7·4·3·2dp(4.1) | 반경 위계(4.6). 12·18·26 이 여러 핸드오프에 반복(TK `dimension` 은 dialog 26·cta 18·button 15 를 "퀴즈 화면 상수" 로 둠) | `control 12` · `feature 18` · `dialog 26` 을 토큰 후보로. `inlineCode` 는 지우거나 반경을 그릴 방법을 정한다 | 둘 다 |
| 3 | 알파 파생색 · 선택 면 | `copy(alpha = …)` 26곳·19가지(2.2). 선택 면 14%(`accentSoft`·`CHIP_TINT`·`FOCUS_TINT`)와 16%(`SELECTED_FILL`·퀴즈 설정 아이콘 상자), 선택 테두리 26%(`accentSoftBorder`·`FOCUS_BORDER`)와 32%(`SELECTED_STROKE`)로 갈렸다 | 같은 뜻이면 같은 알파(2.3 알파 표). 핸드오프도 14%(QZ §3.2)와 16%(NAV §4-4, STA §2)로 갈려 있어 의도된 차이로 보기 어렵다 | 선택 면은 `accentSoft`, 선택 테두리는 `accentSoftBorder` 하나로 모은다. 같은 뜻·같은 알파가 세 곳 이상이면 이름 붙은 토큰으로(13.0) | 둘 다 |
| 4 | 타이포 덮어쓰기 | `copy(fontSize = …)` 54곳, `fontSize = N.sp` 직접 약 55곳(12sp 21회). 내비 라벨은 스타일 없이 12sp/600(3.4) | 스타일은 역할로 고르고 화면에서 `copy` 로 크기를 바꾸지 않는다(3.5) | 12sp 라벨 계열과 15sp 행 제목을 스타일로 올릴지 검토 | 개발자 |
| 5 | 감속 모션 | 반영 안 함: `StatusRing` 무한 회전(`FloatingStatusPill.kt:111`), 퀴즈 풀이·결과·실패 `riseIn` 15곳, `QuizOverlay.kt:111` 의 `flip`, 맨 위로, 내비 숨김·선택, 씬 전환, `PressIndication`, 리본, 캐럿(6.5) | 이동·크기·무한 루프를 끄고 페이드만 남긴다(6.6). DS §08 "모든 무한 모션은 감속 모션이면 멈춘다" — `StatusRing` 은 시트와도 어긋난다 | 무한 회전부터 반영. `riseIn`/`flip` 은 노드 안에서 판정하면 호출부 누락이 사라진다. 씬 전환은 navigation 이 판정을 받을 길이 필요하다 | 둘 다 |
| 6 | Figma · 토큰 사본 시차 | 기준 커밋 `dcbb3e7` 이후 코드에 `chartAccent2`·`chartAccent3`·`chartFuture`(27 → 30색), Expanded `cardMinWidth` 220 → 200, 아이콘 `BookmarkFilled`·`Settings`·`Key`, 컴포넌트 `FloatingStatusPill`·`BookmarkRibbon`·`BookmarkGlyph`·`NumberedRow`·`SectionLabel`·`chart/` 가 생겼다 | Figma 와 Claude Design `tokens/`(DTCG·plugin JSON)가 코드와 같다. 지금 `tokens/` 도 색 27개이고 `dimension` 에 퀴즈 화면 상수를 둔다 | Figma 와 `tokens/` 를 함께 다시 만든다(13.0 디자인 쪽 사본) | 둘 다 |
| 7 | 안 쓰는 아이콘 | `MyNoteIcons.Search` 는 `FloatingNav` 프리뷰에서만 쓰인다(실제 내비는 홈·북마크·통계 3칸) | — | 검색 진입이 생기기 전까지 남길지 정한다 | 개발자 |
| 8 | `MyNoteEasing` 이름 | 공개 곡선은 `MyNoteEasing`(0.2, 0.8, 0.2, 1, `ds/motion/Motion.kt:11`) 하나. (0.2, 0, 0, 1) 은 `SceneMotion.Easing`(internal)·`NotesTopBar` 의 private 곡선. 커밋 `51a3d90` 이 `QuizEasing`·`RiseEasing` 을 `MyNoteEasing` 으로 합쳤다. 북마크·통계는 `MyNoteEasing` 으로 구현됐다 | BM1 §4.1·§5, BM3 §4, STA 머리말이 "`MyNoteEasing` = cubic-bezier(.2, 0, 0, 1)" 로 적었다. QZ §4·DS §08 은 (.2, .8, .2, 1) | **코드 유지.** 디자이너 판단: 그 자리들(재배치·막대 등장·요약 카드 크기)은 6.6 의 "도착" 성격이라 이름(`MyNoteEasing`)이 의도이고 괄호 값이 오기다. 핸드오프 템플릿의 정의를 바로잡고, 장면 곡선은 문서에서 `SceneMotion.Easing` 으로 따로 부른다 | 디자이너 |
| 9 | 드로어 스크림 | `ModalNavigationDrawer` 에 `scrimColor` 없음(`feat/shell/NotesShell.kt:194`) → M3 기본 `colorScheme.scrim` × 0.32. `MaterialTheme` 이 없어 검정 32%(라이브러리 기본값이라 레포에 숫자가 없다) | SN: rgba(0,0,0,.55). 토큰 `scrim`(#06090C 66%)은 "모달 뒤" 의 색 | 드로어에도 `colors.scrim` 을 준다 — 모달 뒤는 한 색이어야 하고, `MaterialTheme` 이 없어 색을 생략하면 머티리얼 기본이 뜬다는 규칙(12.1)의 사례다 | 디자이너 |
| 10 | 카드·행 눌림 | `NoteCard`(`NoteCard.kt:67`)·`NoteRow`(`NoteRow.kt:40`)·`CategoryRow`(`CategoryRow.kt:35`)·`NumberedRow`·`FloatingStatusPill` 은 indication 미지정 → foundation 기본 `DefaultDebugIndication`(눌림 검정 30% 덮개). `CategoryRow` 는 `clip` 이 없어 사각 | 리플·덮개 없음, 눌림 = 바탕보다 밝은 면(2.3 눌림 표). SN §1 카드 active `surface2`, BM2 1b "눌림 상태(surface-2)", ST §2.3 "리플 없음", DS §08 "리플 쓰지 않는다" | 다섯 곳에 2.3 눌림 표의 바탕별 눌림색으로 `PressIndication` 을 준다 — `NoteCard` → `surface2` · 패널 행(`NoteRow`·`CategoryRow`, `surface` 위) → `surface2` · `NumberedRow`(`bg` 위) → `surface` · `FloatingStatusPill`(유리) → `navItemPressed`. 검정 덮개는 다크 바탕에서 "꺼짐" 으로 읽힌다. 기기로 확인 못 함(adb 금지) | 디자이너 |
| 11 | 플로팅 내비 항목 | 커밋 `e5c80fa`(2026-09-12) '리퀴드 글래스 플로팅 내비' 부터: 항목 높이 46 · 알약 반경 23 · 아이콘만, 선택 시 알약이 늘어나며 라벨(12sp/600 `accent`)을 연다 · `navItem*` 선택 알약 · spring 선택 · 드롭 섀도 없음(커밋 메시지에 이유) | NAV §4-2·4-4, DS §07: 62 × 50, 아이콘 위·라벨 아래(11/500, 늘 보임), 선택 `accent` 16%·반경 18. BM1 §6(2026-10-01)도 이 값을 "기존 값" 으로 다시 적었고 "선택 전환 애니메이션 없음" | **코드가 최신 결정이다** — 커밋에 근거가 있고, BM1 은 커밋 뒤에 쓰였는데도 옛 값을 베꼈다. DS 시트 §07·핸드오프 템플릿을 코드 값으로 고친다. 디자이너 판단: 비선택 항목이 아이콘만이라 칸이 셋인 지금은 괜찮지만, 칸이 늘면 라벨 방식을 다시 본다 | 디자이너 |
| 12 | 유리 그림자 · 다이얼로그 유리 | `GlassShadow` 반경 20 · y 14 · 45%(`GlassSurface.kt:76`, `e5c80fa`, CSS 환산 근거 없음). `glassPanel` 66%(`51a3d90` 이후 그대로). 퀴즈 설정 다이얼로그 블러 20(`QuizSetupDialog.kt:283`), 모델 다이얼로그 블러 8(기본) | NAV §4-3 `0 14 40 rgba(0,0,0,.45)`. QZ §3.2 다이얼로그 rgba(27,35,45,.84) + 블러 28. ST §10.3 `glassPanel` 92% | ① **두 다이얼로그의 블러를 하나로** — 같은 `glassPanel` 층이 화면마다 다르게 보인다(4.6 "같은 층은 같은 유리"). ② 핸드오프 사본은 66% 로 고친다. ③ 그림자 환산은 기기 캡처로 확인한 뒤 정한다 | 디자이너 |
| 13 | 카테고리 칩 크기 | `CategoryChip` 하나 — `caption` 13/400 · 가로 8 세로 3 · 반경 8. 카드와 리더 헤더(`NoteReaderScreen.kt:69`)가 같은 컴포저블이라 공유 요소 양 끝 크기가 같다. 초기 커밋 뒤 스타일 변경 없음 | NAV §6-1: 카드 칩 11/500 · 3/9 · 반경 7, 헤더 칩 12/500 · 4/11 · 반경 8, 카드 날짜 12 | **코드 유지.** 13sp 가 메타 크기(3.5) 안이고, 양 끝이 같아 전환이 단순하다. 핸드오프 사본을 코드 값으로 | 디자이너 |
| 14 | 버튼 컴포넌트 | design-system 에 없음. feature 에 세 벌 10개(설정 4종 · 퀴즈 4종 · 퀴즈 설정 다이얼로그 2종, 8.3) | DS §08 "설정 버튼" 을 공용 프리미티브(Primary · Secondary · Destructive · Quiet, 40dp · 반경 12 · 한 줄 · 비활성 α .4)로 그림 | `ds/component/` 로 올린다 — 13.0 조건 셋을 다 만족한다. 크기 두 벌(40/12, 50/15)을 한 컴포넌트의 크기 인자로 | 디자이너 |
| 15 | 타이포 · 숨김 임계 시트 값 | `mdHeading3` 본문 + 1 / 600(`foundation/token/Typography.kt:125`), `mono` 13(`:158`), 내비 숨김 임계 6dp(`FloatingNavScrollState.kt:16`) | DS §02 H3 17/500 · Mono 14, DS §07 임계 8dp(NAV README 는 6) | 코드 유지, DS 시트를 고친다 | 디자이너 |
| 16 | `chartAccent3` 대비 [통계(진행 중)] | `chartAccent3` #3A5A88 — `surface` 위 2.45:1(토큰 값으로 계산) | STA §2.1 "모든 색이 `surface` 대비 3 : 1 이상입니다" | 3:1 이 되게 밝히거나, "범례 이름·값이 늘 붙는다" 를 이 색을 쓰는 조건으로 못 박는다(2.3 차트 색) | 디자이너 |
| 17 | `text3` 위 문장 | `text3` — `bg` 4.0 · `surface` 3.65 · `surface2` 3.38:1. 설정 화면 도움말·안내 문장이 `caption`(13sp) + `text3`(`SettingsScreen.kt:217`, `:376`) | 문장 14sp 이상(QZ §2), 작은 글자 4.5:1, "뜻을 지는 글자는 `text2` 이상"(10.2) | 도움말·안내 **문장**은 `text2` 로 올린다. 날짜·개수·placeholder 같은 메타는 `text3` 그대로. 섹션 라벨(`text3`)은 조용한 라벨이라는 의도(3.5)와 대비 사이의 절충이라 그대로 두고 기록만 한다 | 디자이너 |
| 18 | 스낵바 재질 | `MyNoteSnackbar` = 불투명 `surface2` | QZ §3.2 "유리는 다이얼로그 · 풀이 상단 바 · 스낵바 세 군데" · DS §08 은 "surface-2 불투명" | 코드 · DS 가 맞다(원칙 4 — 글자를 읽는 면). QZ 문장만 낡았다 | 디자이너 |
| 19 | 경고 배지 테두리 | 테두리 없음, 면 `warn` 9%(`ListPanelScreen.kt:285`) | ST §2.3 "테두리 `warn` 22%" · DS §08 "테두리 없음" | 코드 · DS 가 맞다. ST 문장만 낡았다 | 디자이너 |
| 20 | 터치 최소 크기 | `minimumInteractiveComponentSize` 는 `BookmarksScreen.kt:390` 한 곳뿐. 44 미만: 설정 버튼 40(`SettingsScreen.kt:693`) · 조용한 버튼 36(`:658`) · 상태 알약 40(`FloatingStatusPill.kt:151`) · 퀴즈 닫기 40(`QuizParts.kt:137`) · `NoteRow` ≈ 36 · `CategoryRow` ≈ 40(패딩 + 줄 높이 근사, 기기 측정 안 함). 44 이상: 상단 바 토글 44 · 내비 항목 46 · 맨 위로 46 · `NumberedRow` 44 · 설정 줄 48 | 44dp(QZ §2, ST §7), 40dp 버튼은 터치 영역 48dp | 40 · 36dp 버튼과 알약은 보이는 크기를 두고 `minimumInteractiveComponentSize` 로 터치 영역만 넓힌다. 트리 행(`NoteRow`·`CategoryRow`)은 `heightIn(min = 44.dp)` 로 행 높이 자체를 44 로 — 행 전체가 누르는 면이고 활성 배경이 행을 채우므로 보이는 면과 터치 영역이 같아야 한다(디자이너 판단) | 디자이너 |
| 21 | 핸드오프 말투 | 앱 문구 전부 합니다체(9.3) | SN · QZ 핸드오프 원문이 해요체 · 반말(PRD 8.2 #14) | 코드가 맞다. 앞으로 핸드오프 문구는 합니다체로 쓰게 핸드오프 템플릿에 적는다 | 디자이너 |
| 22 | 모달 뒤 덮개 | `scrim` 을 쓰는 곳은 `QuizOverlay` 하나. 모델 다이얼로그는 Compose `Dialog` 의 기본 창 dim(`feat/screen/ModelDialogs.kt:87`) | `scrim` = 모달 뒤의 색(2.3) | 모델 다이얼로그 뒤도 `scrim` 으로 — #9 와 같은 이유 | 개발자 |
| 23 | 공유 요소 곡선 | `navSharedElement`/`navSharedBounds` 가 `boundsTransform` 을 주지 않아 Compose 기본 spring(`nav/shared/SharedElements.kt`). 길이가 정해지지 않는다 | NAV §6-1 · SN "공유 요소 전환": **360ms, cubic-bezier(.2, 0, 0, 1)**, 위치·크기·글자 크기 보간, 씬 슬라이드와 **동시에** 재생(6.6 "같은 물체가 이어진다") | 디자이너 결정: `boundsTransform` 을 장면 곡선·장면 길이(`SceneMotion` 340ms) 그대로 준다. 핸드오프 원값 360ms 와의 20ms 차이는 보이지 않고, 같은 상수를 쓰면 씬 슬라이드와 같이 출발해 같이 도착해 한 동작으로 읽힌다. 맞추면 6.6 곡선 표의 장면 행으로 돌아간다 | 개발자 |
| 24 | 아이콘 선 굵기 | 선을 긋는 아이콘 13개 전부 1.7(15개 중 — `Sparkle` · `CheckBoxChecked` 는 면만, `MyNoteIcons.kt` `strokeIcon`/`filledIcon`) | BM1 §3 "윤곽 stroke 1.8, StrokeJoin.Round", §4.1 표 "꺼짐 \| 윤곽, stroke bodyText 1.8" | 1.7 한 벌이 7.1 "한 벌만 쓴다" 와 맞다. 사본을 고친다 | 개발자 |
| 25 | 다이얼로그 제목 스타일 | 퀴즈 설정 다이얼로그 `screenTitle`(19 · 짧은 창 17, `QuizSetupDialog.kt:91`), 모델 다이얼로그 `sectionTitle`(21, `ModelDialogs.kt:100`) | QZ §2 "다이얼로그 제목 19sp / 700". ST §10.3 모델 확인 다이얼로그는 "설정 다이얼로그와 같은 유리" — 같은 계열이다. 3.5 "같은 역할이면 같은 스타일" | 다이얼로그 제목은 `screenTitle` 로 모은다. 짧은 창에서 17 로 줄어 가로 폰의 다이얼로그에 맞다 | 디자이너 |
| 26 | 연결 화면 두 단 판정 | 첫 실행 루트의 좌우 두 단은 **콘텐츠 폭 ≥ 640 과 높이 Compact 를 같이** 본다(`ConnectScreen`, 두 단 폭 상한 800) | 몇 단인가는 폭만 본다(5.7) | 예외로 둔다 — 한 단 합이 가로 폰 가용 높이(≈ 312–336dp)를 넘어 폭만 보면 `연결` · 입구가 스크롤 아래로 간다(D19). 5.7 에 예외 한 줄 | 디자이너 |
