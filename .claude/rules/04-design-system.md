# 디자인 시스템

## 토큰
- `:core:design-foundation` 에 `MyNoteColors`(다크 단일 팔레트), `MyNoteRadius`/`Spacing`, 타이포(`MyNoteTextStyles`), `AdaptiveTokens`(반응형 5단계), 마크다운 파서.
- `:core:design-system` 의 `MyNoteTheme` 이 CompositionLocal 로 노출. 진입점은 `MyNoteTheme.colors/textStyles/adaptive`.
- **`Color(0xFFXXXXXX)` 리터럴은 feature/design-system 에서 금지** — 토큰은 `:core:design-foundation` 의 팔레트에만 정의(인라인 hex 는 팔레트 정의부에서만).

## 프리뷰 (필수)
- design-system 의 **모든 Composable 은 같은 파일에 `@Preview` 동반**(다크 배경 `bg`). 예외 없음.
- 한 컴포넌트가 여러 상태(active/inactive, 펼침/접힘 등)를 가지면 **해당 파일에 `private class XxxPreviewParameterProvider : PreviewParameterProvider<…>`** 를 만들어 `@PreviewParameter` 로 주입.
- 프리뷰는 `MyNoteTheme { … }` 으로 감싼다.

## 레이어 경계 (필수)
- **design-system 은 데이터 레이어(`:core:data`)를 의존하지 않는다.** 순수 UI 컴포넌트 + **UI 모델**만 둔다.
- 서버드리븐 콘텐츠는 design-system 의 UI 모델(`NoteContentBlock`)로 렌더한다. 데이터 DTO(`:core:data` 의 `NoteBlock`) → UI 모델 매핑은 **feature(presentation)** 에서 단방향으로 한다(`NoteBlockMapper`). DTO 를 UI 까지 끌고 오지 않는다.

## 컴포넌트 배치
| 레이어 | 예시 |
|---|---|
| `:core:design-foundation` | 토큰, `AdaptiveTokens`, `MarkdownParser`/`MdBlock` |
| `:core:design-system` | `MyNoteTheme`, 공용 컴포넌트(`NoteCard`·`CategoryChip`·`SearchField`·`CategoryRow`·`NoteRow`), `MarkdownContent` 렌더 |
| `:feature:notes` | 화면 전용 Composable (재사용 위젯 금지) |

## 접근성
- 클릭 가능한 `Box`/비-`Button` 위젯은 `Modifier.semantics { role = Role.Button; … }`.
- 의미 없는 장식 아이콘은 `contentDescription = null`, 의미 있는 것은 라벨 부여. 탭 타깃 ≥ 36.dp.
