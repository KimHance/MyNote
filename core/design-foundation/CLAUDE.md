# :core:design-foundation — Claude Code Instructions

디자인 토큰의 단일 출처 + 반응형 토큰. 의존 방향: 가장 아래(아무도 의존 안 하는 leaf). **`:core:design-system` 을 역으로 의존하지 않는다.**

## 하드 룰
- **색 hex 리터럴(`Color(0xFFXXXXXX)`)은 오직 이 모듈의 팔레트 정의부에서만.** feature/design-system 에서
  인라인 hex 금지 — 전부 여기 정의된 토큰을 경유한다.
- 다크 단일 팔레트(`MyNoteColors`), `MyNoteRadius`/`Spacing`(`object` — `@Immutable`/`@Stable` 안 붙임,
  싱글톤 + `val` 상수라 본래 stable), 타이포(`MyNoteTextStyles`), `AdaptiveTokens`(5단계).
- 토큰 데이터 홀더(예: 색 묶음, `AdaptiveTokens`)는 모든 멤버가 진짜 불변(원시/`Color`/`Dp`/`TextStyle`/
  `ImmutableList`)이므로 **`@Immutable`**.
- 반응형 단계는 `AdaptiveTokens`(5단계)로만. 간격/폭/열수를 하드코딩하지 않는다.
- 순수 UI 토대만 둔다 — 데이터/도메인 의존 금지. (마크다운 파서 등 콘텐츠 무관 유틸은 허용)

## 구성
- `token/` — `MyNoteColors`, `Dimens`(Radius/Spacing), `Typography`
- `window/AdaptiveTokens.kt` — `MyNoteWindowClass`(5단계) + 단계별 토큰
