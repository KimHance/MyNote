---
title: 그래프 (Graph)
date: 2026.06.29
---
정점(vertex)과 그것을 잇는 간선(edge)의 집합. **관계·연결망** (지도·소셜·의존성)을 표현한다. 방향 유무·가중치 유무로 갈린다.

## 표현 — 인접행렬 vs 인접리스트

```
그래프:  0─1   0─2   1─2   2─3

인접 행렬 (V×V):            인접 리스트 (정점별 이웃):
    0 1 2 3                 0 → [1, 2]
  0 0 1 1 0                 1 → [0, 2]
  1 1 0 1 0                 2 → [0, 1, 3]
  2 1 1 0 1                 3 → [2]
  3 0 0 1 0

  행렬: 간선조회 O(1), 공간 O(V^2) — 조밀(dense)에 유리
  리스트: 공간 O(V+E), 이웃순회 효율 — 희소(sparse)에 유리(보통 기본)
```

### 종류 & 복잡도

- **방향(directed)** / 무방향(undirected) — 간선에 방향이 있나.
- **가중치(weighted)** — 간선에 비용(거리·시간). 최단경로는 다익스트라(힙 사용, 형제 노트 `힙 & 우선순위 큐`).

```
             인접행렬     인접리스트
──────────────────────────────────
공간          O(V^2)      O(V + E)
간선 (u,v)?   O(1)        O(deg u)
이웃 순회      O(V)        O(deg u)
BFS/DFS 전체  O(V^2)      O(V + E)
```

## Kotlin — 순회 (BFS / DFS)

```
val adj = mapOf(0 to listOf(1,2), 1 to listOf(0,2), 2 to listOf(0,1,3), 3 to listOf(2))

// BFS — 큐 (형제 노트: 큐 & 덱)
fun bfs(start: Int): List<Int> {
    val seen = mutableSetOf(start); val order = mutableListOf<Int>()
    val q = ArrayDeque<Int>().apply { add(start) }
    while (q.isNotEmpty()) {
        val u = q.removeFirst(); order += u
        for (v in adj[u].orEmpty()) if (seen.add(v)) q.addLast(v)
    }
    return order
}

// DFS — 스택/재귀 (형제 노트: 스택)
fun dfs(u: Int, seen: MutableSet<Int> = mutableSetOf(), out: MutableList<Int> = mutableListOf()): List<Int> {
    if (!seen.add(u)) return out
    out += u
    for (v in adj[u].orEmpty()) dfs(v, seen, out)
    return out
}
```

- `BFS` = 최단(간선 수) 경로·레벨 탐색,  `DFS` = 경로·사이클·위상정렬·연결요소.
- **방문 집합** 으로 재방문/무한루프를 막는다(형제 노트 `집합`). 연결요소·사이클은 `유니온 파인드` 도(형제 노트).

> **요약: 정점+간선으로 관계를 표현. 희소면 인접리스트 O(V+E), 조밀이면 인접행렬 O(V^2). BFS(큐)=최단·레벨, DFS(스택/재귀)=경로·사이클. 방문 집합은 필수.**
