---
title: 균형 이진트리 (Red-Black / AVL)
date: 2026.06.29
---
삽입/삭제마다 스스로 높이를 **O(log n) 으로 유지** 하는 이진탐색트리. BST 의 편향 문제(형제 노트 `이진탐색트리`)를 회전으로 막는다.

## 핵심 아이디어 — 회전(rotation)

삽입으로 한쪽이 깊어지면 **부분 트리를 회전해 높이를 재분배** 한다. 회전은 BST 순서를 보존하면서 모양만 바꾸는 국소 연산(정확한 케이스 분기는 구현마다 다르므로 개념으로 본다).

```
좌회전(왼쪽으로): 오른쪽 자식을 끌어올린다
     x                 y
      \               / \
       y    ──►      x   z
        \
         z

  중위순회 순서(x < y < z)는 그대로 유지된다 → 탐색 규칙 안전,
  높이만 줄어든다.  (우회전은 대칭)
```

### AVL vs Red-Black — 개념 차이

- **AVL** — 모든 노드에서 좌우 높이차 ≤ 1 로 **엄격히** 균형. 더 납작해 **조회가 빠름**, 대신 삽입/삭제 시 회전이 잦다. 읽기 위주에 적합.
- **Red-Black** — 색(빨강/검정) 규칙으로 **느슨하게** 균형(최장경로 ≤ 2×최단경로). 회전이 적어 **삽입/삭제가 효율적**. 범용 기본값.

### 복잡도

```
연산     보장(평균=최악)
──────────────────────
탐색     O(log n)
삽입     O(log n)   (+ 상수 번 회전/재색칠)
삭제     O(log n)

  BST 와 달리 최악도 O(log n) 으로 "보장"되는 게 핵심 가치.
```

## Kotlin — TreeMap 이 곧 Red-Black 트리

```
// java.util.TreeMap / TreeSet 의 내부가 Red-Black 트리
val map = java.util.TreeMap<Int, String>()
map[5] = "e"; map[1] = "a"; map[3] = "c"

map.firstKey()          // 1   (최솟값 O(log n))
map.ceilingKey(2)       // 3   (2 이상 최소 키 — 범위질의)
map.headMap(4)          // {1,3} 미만 부분
for ((k, v) in map) { } // 항상 키 정렬 순서로 순회
```

- **정렬 유지 + 범위/이웃 질의** (`ceiling/floor/headMap/tailMap`)가 필요하면 TreeMap/TreeSet. 단순 키-값이면 해시맵이 더 빠르다(형제 노트 `해시맵 / 해시테이블`).

> **요약: 회전으로 높이를 O(log n) 에 묶어 BST 편향을 막는다. AVL=엄격(읽기 빠름), RB=느슨(쓰기 효율, 범용). Kotlin/Java 의 TreeMap·TreeSet 이 곧 Red-Black 트리.**
