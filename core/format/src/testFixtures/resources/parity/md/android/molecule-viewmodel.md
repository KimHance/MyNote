---
title: Molecule 로 ViewModel 상태 만들기
date: 2026.06.29
---
## 핵심 아이디어

ViewModel 상태를 **@Composable 프레젠터** 로 선언적으로 만든다. `combine` / `stateIn` 대신 remember·collectAsState·LaunchedEffect 로 조립.

```
val models = viewModelScope.launchMolecule(RecompositionMode.Immediate) { present() }
```

---

### 왜 회전에 안전한가

- 컴포지션이 viewModelScope 에 산다 → VM 보존 = remember 유지
- 프로세스 death 는 못 견딤 → SavedStateHandle 필요

> UI 컴포지션(Activity)과 프레젠터 컴포지션(viewModelScope)은 별개다.
