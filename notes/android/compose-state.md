---
title: Compose 상태 관리 정리
date: 2026.06.12
---
## 상태 호이스팅

Composable 은 가능한 **상태를 직접 들지 않고** 상위로 끌어올린다. 재사용성과 테스트 용이성이 올라간다.

### remember 와 rememberSaveable

- `remember` 는 컴포지션이 유지되는 동안만 값을 보존한다.
- `rememberSaveable` 은 구성 변경(회전 등)에도 값을 복원한다.

> 상태는 단방향으로 흐른다: 이벤트는 위로, 상태는 아래로.

```
var text by rememberSaveable { mutableStateOf("") }
TextField(value = text, onValueChange = { text = it })
```
