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

## 구성
- `theme/` — `MyNoteTheme`(토큰 CompositionLocal), `Typography`
- 공용 컴포넌트 — `NoteCard`·`CategoryChip`·`SearchField`·`CategoryRow`·`NoteRow`
- `block/` — `NoteContentBlock`(UI 모델) + `NoteContent` 렌더러
