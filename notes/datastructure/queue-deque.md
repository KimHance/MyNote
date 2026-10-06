---
title: 큐 & 덱 (Queue & Deque)
date: 2026.06.29
---
**큐(Queue)** 는 먼저 넣은 게 먼저 나오는 `FIFO` (First In First Out). **덱(Deque)** 은 **양끝 모두 출입** 가능한 일반화. BFS·작업 대기열의 기본.

## 동작 — 양끝, 그리고 원형 버퍼

```
Queue (FIFO):  enqueue 는 뒤, dequeue 는 앞
   front ►[a][b][c]◄ rear
   dequeue→a        enqueue(d)→ 뒤에 추가

Deque: 양끝 모두 add/remove
   ◄ addFirst        addLast ►
   ◄ removeFirst     removeLast ►
```

배열로 큐를 만들면 앞에서 빼면서 매번 시프트하면 O(n). 그래서 **원형 버퍼(circular buffer)** 로 front/rear 인덱스만 굴려 `O(1)` 을 만든다.

```
원형 버퍼: 인덱스가 끝에 닿으면 0 으로 되돌아온다 (% capacity)
  [ _ ][ b ][ c ][ d ][ _ ]
         ▲front          ▲rear
  dequeue → front++,  enqueue → rear++  (둘 다 modulo 로 순환)
```

### 복잡도

```
연산                평균/최악
──────────────────────────
enqueue / addLast   O(1)*
dequeue / removeFirst O(1)
front / peek        O(1)
탐색                O(n)
               *동적 리사이즈 시 분할상환
```

## Kotlin 사용

```
// ArrayDeque 가 큐로도 덱으로도 표준
val q = ArrayDeque<Int>()
q.addLast(1); q.addLast(2)    // enqueue
val first = q.removeFirst()   // dequeue → 1 (FIFO)

// 활용: 그래프 BFS
fun bfs(start: Int, adj: Map<Int, List<Int>>): List<Int> {
    val visited = mutableSetOf(start)
    val order = mutableListOf<Int>()
    val q = ArrayDeque<Int>()
    q.addLast(start)
    while (q.isNotEmpty()) {
        val cur = q.removeFirst()
        order += cur
        for (next in adj[cur].orEmpty()) if (visited.add(next)) q.addLast(next)
    }
    return order
}
```

- **BFS** · 작업/이벤트 대기열 · 슬라이딩 윈도우 최댓값(덱). 그래프 노트는 형제 `그래프`.
- 양끝만 쓰면 덱이 LinkedList 보다 캐시 친화적이고 빠르다(형제 노트 `Stack` 도 ArrayDeque 기반).
- 우선순위가 있는 대기열은 큐가 아니라 `PriorityQueue` (형제 노트 `힙 & PriorityQueue`).

> **요약: 큐=FIFO, 덱=양끝 출입. 원형 버퍼로 양끝 연산을 O(1) 로 유지. Kotlin 의 ArrayDeque 하나로 스택·큐·덱을 다 덮고, BFS 의 기본 자료구조다.**
