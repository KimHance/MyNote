# AGENTS.md — :core:ui

이 파일은 이 모듈에서 작업하는 에이전트를 위한 안내다.

---

## 이 모듈이 있는 이유

**두 기능 이상이 쓰는 화면 조각 · 화면 모델 · 기능 사이 신호 계약의 자리**다(기능 문서 `feature-modules` 결정 3).
기능 모듈은 서로를 의존하지 않으므로(FM-2) 둘 이상이 쓰는 것은 여기를 거친다. 디자인 시스템은 기능 뜻(모델 상태 ·
토큰 입력)을 모르는 자리로 두고, 그 뜻을 아는 공용 조각은 이 모듈이 든다.

지금은 빈 뼈대다. 공용 조각은 아직 `:feature:notes` 에 있고 결정 17 의 순서로 옮겨 온다(기능 문서 공용 조각 표).

## 경계

| 지킬 것 | 지키는 것 |
|---|---|
| 의존은 `:core:{common, design-system, domain, navigation}` 뿐 — 기능 모듈 · 데이터 층 · 모델 구현을 모른다 | `notes-tests/test_feature_modules.py` FM-2 · FM-3 |
| 문구 접두사는 `ui_`(`build.gradle.kts` 의 `resourcePrefix`) — 참조하는 모듈이 둘 이상인 문구만 둔다(결정 15) | `lintDebug` · FM-11 파이썬 시험 |
| 기능 컨벤션(`hancenote.android.feature`)을 쓰지 않는다 — 그 컨벤션이 이 모듈을 넣으므로 쓰면 자기를 의존한다. Hilt 도 없다 | FM-1 파이썬 시험 |
