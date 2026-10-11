# AGENTS.md — :feature:connect

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

첫 실행 연결 화면 · 쌓은 연결 화면 · 바꾸기 · 받아들이기 대화상자 · 연결 판정의 자리다(기능 문서 `feature-modules` 모듈 지도).
지금은 빈 뼈대다. 화면 파일은 아직 `:feature:notes` 에 있고 결정 17 의 순서로 옮겨 온다.

## 경계

| 지킬 것 | 지키는 것 |
|---|---|
| `build.gradle.kts` 는 `hancenote.android.feature` 하나 — `:core:{common, design-system, domain, navigation, ui}` · 공통 의존 · 문구 접두사 `connect_` 를 컨벤션이 넣는다 | `build-logic/AGENTS.md` |
| 다른 기능 모듈을 의존하지 않는다. 둘 이상이 쓰는 것은 `:core:ui`, 다른 기능 화면으로는 목적지 키로 간다 | `notes-tests/test_feature_modules.py` FM-2 |
| 데이터 층(`:core:{data, network, database, format}`) · 모델 구현(`:core:ai`)을 모른다 — `:core:domain` 의 UseCase 만 | FM-3 |
| 문구는 이 모듈만 참조하는 것만 둔다(결정 15) | `lintDebug` · FM-11 |
