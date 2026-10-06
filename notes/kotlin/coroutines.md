---
title: 코루틴 디스패처 메모
date: 2026.05.30
---
## 디스패처 선택

- `Dispatchers.Main` 은 UI
- `Dispatchers.IO` 는 디스크/네트워크
- `Dispatchers.Default` 는 CPU 연산

> 취소는 협력적이다 — suspend 지점을 둬야 한다.

```
flow { emit(load()) }.flowOn(Dispatchers.IO)
```
