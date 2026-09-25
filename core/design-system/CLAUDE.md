# :core:design-system — Claude Code Instructions

`MyNoteTheme` + 공용 컴포넌트 + 서버드리븐 콘텐츠 렌더. 의존: `api(:core:design-foundation)`.

## 하드 룰
- **데이터 레이어(`:core:data`/`:core:domain`)를 의존하지 않는다.** 순수 UI 컴포넌트 + **UI 모델**만 둔다.
  서버드리븐 콘텐츠는 이 모듈의 UI 모델(`NoteContentBlock`)로 렌더하고, 데이터 DTO/도메인 모델 → UI 모델
  매핑은 **feature(presentation)** 에서 단방향으로 한다. DTO 를 UI 까지 끌고 오지 않는다.
- **색/타이포/간격은 `MyNoteTheme.colors` / `.textStyles` / `.adaptive` 로만.** `MaterialTheme` 을 테마
  용도로 직접 쓰지 않는다(Material3 컴포넌트 사용 자체는 정상, 색은 토큰으로 지정).
- **`Color(0x...)` 리터럴 금지** — 토큰은 design-foundation 팔레트에만.
- **모든 Composable 은 같은 파일에 `@Preview` 동반**(다크 배경, `MyNoteTheme { }` 로 감쌈). 예외 없음.
  여러 상태를 가지면 `PreviewParameterProvider` 로 주입.
- 컴포저블 파라미터/State 의 컬렉션은 `ImmutableList`/`ImmutableMap` — `List<T>` 금지.
- 재사용 컴포넌트는 MVI 를 모른다 — 순수 콜백(`onClick` 등)만 받는다.
- 애니메이션 값은 deferred read: `Modifier.graphicsLayer { rotationZ = … }` (O) / `Modifier.rotate(value)` (X).
- 유리 표면은 **backdrop**(liquid glass) — `layerBackdrop`(뒤 콘텐츠, feature 가 건다) ↔
  `drawBackdrop`(표면, 여기가 건다)가 같은 `Backdrop` 을 봐야 한다. 파라미터로 받고
  **기본값 `null` = 효과 없음**(프리뷰용). 굴절(API 33+)·블러(31+)·불투명 폴백은 라이브러리가 가른다.
- `content` 슬롯은 분기마다 부르지 않는다 — Modifier 만 분기하고 호출은 한 번
  (`compose:content-slot-reused` 위반이자 슬롯 상태가 날아간다).

## 구성
- `theme/` — `MyNoteTheme`(토큰 CompositionLocal), `Typography`(`MyNoteTextStyles` — 앱 전체 타이포 스케일),
  `Motion`(`MyNoteEasing`·`rememberReduceMotion()`)
- 공용 컴포넌트 — `NoteCard`·`CategoryChip`·`SearchField`·`CategoryRow`·`NoteRow`·
  `FloatingNavBar`/`FloatingNavRail`·`ScrollToTopButton`(+`MyNoteIcons`), `Modifier.fadingEdges`
- 모션/인디케이션 — `PressIndication`(`IndicationNodeFactory`), `Modifier.riseIn`, `Modifier.aiShine`,
  `FlipState`(+`Modifier.flip`), `throttledProcess`. 값은 전부 `graphicsLayer {}` 안에서 읽는다.
- 유리 표면 조립(`drawBackdrop`·효과 체인)은 **`GlassSurface` 안에서만** 한다. 바깥은 이 컴포넌트를
  쓰기만 하고 `drawBackdrop` 을 직접 부르지 않는다.
- `block/` — `NoteContentBlock`(UI 모델) + `NoteContent` 렌더러
- `res/values/strings.xml` — **공통 문구**(확인·취소·저장·닫기·되돌리기)와 이 모듈 컴포넌트가 쓰는 문구.
  `resourcePrefix = "ds_"` 라 feature 리소스와 이름이 겹치지 않는다. feature 에서는
  `com.hancekim.mynote.core.designsystem.R as DsR` 로 참조한다.

## 문구 (다국어 없음)
- **화면에 보이는 문자열은 전부 `strings.xml`.** 코드에 한글 리터럴을 두지 않는다
  (프리뷰 더미 데이터와 개발자용 `error()` 메시지는 예외).
- 말투는 **합니다체** 하나로 통일한다 — 서술은 `~합니다`/`~습니다`, 권유·지시는 `~하세요`.
  반말(`~했어`)·해요체(`~돼요`)를 섞지 않는다.
