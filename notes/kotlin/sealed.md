---
title: sealed interface 활용
date: 2026.05.14
---
## 봉인 계층

sealed interface 로 상태/이벤트를 표현하면 **when 이 모든 분기를 강제** 한다.

1. Loading
2. Success(data)
3. Error(message)
