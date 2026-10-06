---
title: 스택 (Stack)
date: 2026.06.29
---
마지막에 넣은 게 먼저 나오는 **LIFO** (Last In First Out) 구조. 한쪽 끝(top)에서만 넣고 뺀다. **되돌리기·중첩 구조 처리·DFS** 에 쓴다.

## 동작 — push / pop / peek

```
push(c)            pop() → c
  │                  │
  ▼                  ▼
┌───┐  ┌───┐  ┌───┐  ┌───┐
│ c │  │   │  │ b │  │ b │   top 에서만 출입
│ b │  │ b │  │ a │  │ a │
│ a │  │ a │  └───┘  └───┘
└───┘  └───┘

  push: top 에 추가  ·  pop: top 제거+반환  ·  peek: top 확인만
```

### 복잡도

```
연산        평균/최악
──────────────────
push       O(1)*    *동적배열 리사이즈 시 분할상환
pop        O(1)
peek       O(1)
탐색        O(n)     (스택답지 않은 사용)
```

## Kotlin 사용

예전 `java.util.Stack` 은 `Vector` 기반이라 동기화 오버헤드가 있다. **ArrayDeque 를 스택으로 쓰는 게 권장**.

```
// 권장: ArrayDeque
val stack = ArrayDeque<Int>()
stack.addLast(1)          // push
stack.addLast(2)
val top = stack.removeLast()  // pop → 2
val peek = stack.lastOrNull() // peek

// 활용: 괄호 매칭
fun balanced(s: String): Boolean {
    val st = ArrayDeque<Char>()
    val pair = mapOf(')' to '(', ']' to '[', '}' to '{')
    for (c in s) when (c) {
        '(', '[', '{' -> st.addLast(c)
        ')', ']', '}' -> if (st.removeLastOrNull() != pair[c]) return false
    }
    return st.isEmpty()
}
```

- **괄호 매칭** · 수식 파싱 · 실행취소(undo) · 그래프 `DFS` (재귀 대신 명시적 스택).
- 재귀 호출 자체가 콜 스택을 쓰는 것 — 깊은 재귀를 스택으로 풀면 `StackOverflow` 회피.

> **요약: LIFO, 한쪽 끝에서만 출입해 push/pop 모두 O(1). Kotlin 에선 java.util.Stack 말고 ArrayDeque. DFS·괄호매칭·되돌리기의 기본 도구.**
