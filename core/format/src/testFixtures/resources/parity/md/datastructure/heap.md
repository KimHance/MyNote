---
title: 힙 & 우선순위 큐 (Heap & PriorityQueue)
date: 2026.06.29
---
부모가 자식보다 항상 작은(혹은 큰) **완전 이진트리**. 최솟값/최댓값을 **O(1) 에 보고 O(log n) 에 꺼낸다**. 우선순위 큐의 토대.

## 배열로 표현하는 트리

완전 이진트리라 **포인터 없이 배열에 그대로** 담는다. 인덱스 산수로 부모/자식을 오간다.

```
min-heap (부모 ≤ 자식)
        1            배열: [1, 3, 2, 7, 4, 5]
      /   \           idx:   0  1  2  3  4  5
     3     2
    / \   /          i 의 부모   = (i-1)/2
   7  4  5           i 의 왼자식 = 2i+1,  오른자식 = 2i+2

  루트(idx 0)가 항상 최솟값.
```

### sift-up / sift-down

- **삽입** — 배열 끝에 넣고 부모와 비교하며 위로 올린다(`sift-up`). O(log n).
- **최솟값 제거** — 루트를 빼고 마지막 원소를 루트에 올린 뒤 자식과 비교하며 내린다(`sift-down`). O(log n).

```
삽입 6:  끝에 추가 후 부모와 비교하며 sift-up
[1,3,2,7,4,5,6] → 6의 부모 idx2=2, 2<6 이라 멈춤 (위치 OK)

extractMin: 루트1 제거 → 마지막을 루트로 → sift-down
[5,3,2,7,4] → 5 vs min(3,2)=2 교환 → ...
```

### 복잡도

```
연산              복잡도
──────────────────────────
peek (min/max)    O(1)
삽입 (offer)       O(log n)
최솟값 제거 (poll)  O(log n)
임의 원소 탐색      O(n)
배열로 힙 만들기     O(n)   (heapify, 바텀업)
```

## Kotlin 사용

```
// PriorityQueue 는 기본 min-heap
val pq = java.util.PriorityQueue<Int>()
pq.offer(5); pq.offer(1); pq.offer(3)
pq.poll()                      // 1 (최솟값)

// max-heap 은 역순 비교자
val maxPq = java.util.PriorityQueue<Int>(compareByDescending { it })

// 객체 우선순위: comparator 로
val tasks = java.util.PriorityQueue<Task>(compareBy { it.priority })

// 활용: 다익스트라, top-K, 작업 스케줄링
```

- **힙은 부분 정렬일 뿐** — 전체가 정렬돼 있지 않다. 순회 순서는 정렬 순서가 아니다.
- `top-K` 문제는 크기 K 힙으로 O(n log K). 정렬·범위가 필요하면 트리(형제 노트 `이진탐색트리` · `균형 이진트리`).
- 우선순위 없는 단순 대기열은 큐로(형제 노트 `큐 & 덱`).

> **요약: 완전 이진트리를 배열에 담아 부모/자식을 인덱스 산수로 오간다. peek O(1), 삽입·삭제 O(log n). PriorityQueue=min-heap, 비교자로 max·객체 우선순위. 다익스트라·top-K의 핵심.**
