---
title: 연결 리스트 (LinkedList)
date: 2026.06.29
---
각 원소(노드)가 **다음 노드의 참조** 를 들고 사슬처럼 연결된 구조. 메모리 불연속. **노드 참조만 있으면 삽입/삭제가 O(1)** 인 대신, 인덱스 접근이 느리다.

## 노드 구조 — 단일 vs 이중

```
단일 연결 (singly): next 만
[a|•]─►[b|•]─►[c|•]─► null

이중 연결 (doubly): prev + next (양방향 이동·뒤에서 삭제 쉬움)
null ◄─[•|a|•]◄─►[•|b|•]◄─►[•|c|•]─► null

  노드 = (값, 이웃 노드 참조). 배열과 달리 메모리상 흩어져 있다.
```

삽입/삭제는 **앞뒤 노드의 참조만 다시 연결** 하면 끝 — 시프트가 없다. 단, **그 위치를 찾는 데 O(n)** 이 든다(인덱스로 바로 못 감).

```
b 와 c 사이에 x 삽입: 참조 2개만 갱신 → O(1)
[a]─►[b]─►[x]─►[c]
         └──►──┘  (b.next = x, x.next = c)
```

### 복잡도

```
연산                   평균/최악
────────────────────────────────
인덱스 접근 get(i)      O(n)    앞에서부터 따라가야
탐색 (값으로)          O(n)
앞에 삽입/삭제          O(1)
노드 참조 있을 때 삭제   O(1)    (이중연결이면 특히)
끝에 삽입               O(1)    꼬리 포인터 있으면
```

## Kotlin 사용

```
// 표준 LinkedList (이중 연결 + Deque 구현)
val ll = java.util.LinkedList<Int>()
ll.addFirst(1); ll.addLast(2)
ll.removeFirst()                 // O(1)
val x = ll[0]                    // O(n) — 인덱스 접근은 피해라

// 직접 구현 핵심 (단일 연결)
class Node<T>(val value: T, var next: Node<T>? = null)

fun <T> addFront(head: Node<T>?, v: T): Node<T> =
    Node(v, next = head)         // 새 노드를 머리에 — O(1)
```

- 실무에서 순수 LinkedList 는 드물다 — 랜덤 접근이 없어 `ArrayList` 가 대개 더 빠르고 캐시 친화적(형제 노트 `배열 & ArrayList`).
- 단, 양끝 삽입/삭제가 핵심이면 `ArrayDeque` 가 더 실용적(형제 노트 `Stack` · `Queue & Deque`).
- **LRU 캐시** 는 이중 연결 리스트를 해시맵과 엮는다(형제 노트 `LRU 캐시`).

> **요약: 참조 사슬이라 삽입/삭제는 O(1)이지만 위치 탐색·인덱스 접근은 O(n). 캐시 지역성이 나빠 실무 기본값은 ArrayList, 연결 리스트는 LRU 같은 특정 패턴에서 빛난다.**
