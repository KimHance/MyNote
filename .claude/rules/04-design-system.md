# 디자인 시스템

## 토큰
- `:core:design-foundation` 에 `MyNoteColors`(다크 단일 팔레트), `MyNoteRadius`/`Spacing`, 타이포(`MyNoteTextStyles`), `AdaptiveTokens`(반응형 5단계), 마크다운 파서.
- `:core:design-system` 의 `MyNoteTheme` 이 CompositionLocal 로 노출. 진입점은 `MyNoteTheme.colors/textStyles/adaptive`.
- **`Color(0xFFXXXXXX)` 리터럴은 feature/design-system 에서 금지** — 토큰은 `:core:design-foundation` 의 팔레트에만 정의(인라인 hex 는 팔레트 정의부에서만).

## 프리뷰 (필수)
- design-system 의 **모든 Composable 은 같은 파일에 `@Preview` 동반**(다크 배경 `bg`). 예외 없음.
- 한 컴포넌트가 여러 상태(active/inactive, 펼침/접힘 등)를 가지면 **해당 파일에 `private class XxxPreviewParameterProvider : PreviewParameterProvider<…>`** 를 만들어 `@PreviewParameter` 로 주입.
- 프리뷰는 `MyNoteTheme { … }` 으로 감싼다.

## 유리 표면 (liquid glass)
- **backdrop**(`com.kyant.backdrop`) 를 쓴다. 짝이 맞아야 한다 —
  콘텐츠에 **`Modifier.layerBackdrop(backdrop)`**, 유리 표면에 **`Modifier.drawBackdrop(backdrop, …)`**,
  둘이 **같은 `Backdrop` 인스턴스**를 봐야 한다. 하나라도 빠지면 조용히 아무것도 안 그려진다.
- **`layerBackdrop` 은 "굴절 대상"만 감싸는 전용 `Box` 에 건다.** `SharedTransitionLayout` 에
  직접 걸면 레이어가 비어 효과가 통째로 사라진다(실측). 내비 자신은 그 Box 밖에 둬야
  자기 자신을 샘플링하지 않는다.
- 효과 단계: `blur` → `lens`(굴절) → `highlight`(스펙큘러) → `shadow`. 테두리 선 대신 `highlight` 를 쓴다.
- **블러를 세게 걸수록 굴절이 안 보인다** — 블러가 굴절이 밀어낼 디테일을 먼저 지운다. 둘은 맞바꾸는 값이다.
- 단계적 저하는 라이브러리가 알아서 한다: **굴절은 API 33+**(AGSL `RuntimeShader`), 블러는 31+,
  그 아래는 불투명. 호출부에서 분기하지 않는다.
- `Backdrop` 은 화면(feature)이 소유하고 컴포넌트는 파라미터로 받는다. **기본값 `null` = 효과 없음**
  이라 프리뷰가 안 깨진다.

## 공유 요소 (shared element) 경계
- **design-system 컴포넌트는 nav3 를 모른다.** 공유 전환이 필요한 자리는 `Modifier` 슬롯으로 받는다
  (`NoteCard(chipModifier=…, dateModifier=…, titleModifier=…)`). 기본값 `Modifier` 라 프리뷰가 안 깨진다.
- 실제 `sharedElement`/`sharedBounds` 조립은 feature 의 `rememberNoteSharedModifiers(noteId)` 가 한다.

## 레이어 경계 (필수)
- **design-system 은 데이터 레이어(`:core:data`)를 의존하지 않는다.** 순수 UI 컴포넌트 + **UI 모델**만 둔다.
- 서버드리븐 콘텐츠는 design-system 의 UI 모델(`NoteContentBlock`)로 렌더한다. 데이터 DTO(`:core:data` 의 `NoteBlock`) → UI 모델 매핑은 **feature(presentation)** 에서 단방향으로 한다(`NoteBlockMapper`). DTO 를 UI 까지 끌고 오지 않는다.

## 컴포넌트 배치
| 레이어 | 예시 |
|---|---|
| `:core:design-foundation` | 토큰, `AdaptiveTokens`, `MarkdownParser`/`MdBlock` |
| `:core:design-system` | `MyNoteTheme`, 공용 컴포넌트(`NoteCard`·`CategoryChip`·`SearchField`·`CategoryRow`·`NoteRow`·`FloatingNavBar`/`FloatingNavRail`·`ScrollToTopButton`), `Modifier.fadingEdges`, `MyNoteIcons`, `NoteContent` 렌더 |
| `:feature:notes` | 화면 전용 Composable (재사용 위젯 금지) |

## 접근성
- 클릭 가능한 `Box`/비-`Button` 위젯은 `Modifier.semantics { role = Role.Button; … }`.
- 의미 없는 장식 아이콘은 `contentDescription = null`, 의미 있는 것은 라벨 부여. 탭 타깃 ≥ 36.dp.
