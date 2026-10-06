---
title: 유니온 파인드 / 분리집합 (Union-Find)
date: 2026.06.29
---
원소들을 겹치지 않는 그룹으로 묶고, **"이 둘이 같은 그룹인가"와 "두 그룹 합치기"** 를 거의 `O(1)` 에 한다. 사이클 판별·최소 신장 트리(MST)의 핵심.

## 구조 — 부모 배열로 만든 숲

각 원소가 부모를 가리키고, 루트가 그 그룹의 대표다. `find` 는 루트까지 따라가고, `union` 은 한 루트를 다른 루트 밑에 붙인다.

```
parent: [0,0,1,3,3]   (i 의 부모 = parent[i], 루트는 자기자신)

   0        3        find(2): 2→1→0  루트 0
   |       / \       find(4): 4→3     루트 3
   1      ?   4      0 과 3 은 다른 그룹
   |
   2

union(2,4): find(2)=0, find(4)=3 → parent[0]=3 (한 그룹으로)
```

### 두 가지 최적화

- **경로 압축(path compression)** — `find` 중 거친 노드를 루트에 직접 매달아 다음 탐색을 평탄화.
- **랭크/크기 합치기(union by rank)** — 항상 작은 트리를 큰 트리 밑에 붙여 높이를 낮게 유지.

둘을 함께 쓰면 연산당 **거의 상수** — 정확히는 역아커만 함수 `O(α(n))` 으로, 실질적으로 O(1).

## Kotlin — 구현

```
class UnionFind(n: Int) {
    private val parent = IntArray(n) { it }
    private val rank = IntArray(n)

    fun find(x: Int): Int {
        if (parent[x] != x) parent[x] = find(parent[x])  // 경로 압축
        return parent[x]
    }

    fun union(a: Int, b: Int): Boolean {
        val ra = find(a); val rb = find(b)
        if (ra == rb) return false            // 이미 같은 그룹 → 사이클!
        if (rank[ra] < rank[rb]) parent[ra] = rb
        else if (rank[ra] > rank[rb]) parent[rb] = ra
        else { parent[rb] = ra; rank[ra]++ }
        return true
    }
}
```

- `union 이 false` = 두 원소가 이미 연결됨 → 간선을 더하면 **사이클**. 그래프 사이클 판별의 정석(형제 노트 `그래프`).
- **크루스칼 MST** — 간선을 가중치순 정렬 후 사이클을 안 만드는 것만 union 으로 채택.

> **요약: 부모 배열 숲으로 그룹을 관리. 경로 압축 + 랭크 합치기를 함께 쓰면 연산당 거의 O(1)(O(α(n))). 같은 그룹 union 시도 = 사이클 → 사이클 판별·크루스칼 MST 의 핵심.**
