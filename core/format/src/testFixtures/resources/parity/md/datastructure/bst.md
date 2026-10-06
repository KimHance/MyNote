---
title: 이진탐색트리 (BST)
date: 2026.06.29
---
각 노드에서 **왼쪽 \< 자신 \< 오른쪽** 규칙을 지키는 이진트리. 정렬을 유지하면서 탐색·삽입·삭제를 평균 `O(log n)` 에 한다.

## 구조 & 탐색

```
        8
      /   \
     3     10
    / \      \
   1   6      14

  탐색(6): 8보다 작다 →왼쪽 3, 3보다 크다 →오른쪽 6. 찾음.
  매 단계 후보가 절반씩 → 균형 잡혀 있으면 O(log n).
  중위순회(in-order)하면 정렬된 순서: 1 3 6 8 10 14
```

삽입은 탐색과 같은 경로로 내려가 빈 자리에 매단다. 삭제는 세 경우: 리프(그냥 제거), 자식 1개(자식을 끌어올림), **자식 2개(중위 후속자로 대체)**.

### 복잡도 — 그리고 편향 문제

```
연산     평균       최악(편향)
────────────────────────────
탐색     O(log n)   O(n)
삽입     O(log n)   O(n)
삭제     O(log n)   O(n)

  정렬된 데이터를 그냥 넣으면 한쪽으로 쏠려 연결 리스트가 된다:
   1
    \
     2
      \
       3   ← 높이 n → 모든 연산 O(n)
```

이 **편향(skew)** 이 BST 의 약점이다. 해결책이 **자가 균형 트리** (Red-Black, AVL) — 형제 노트 `균형 이진트리`.

## Kotlin — 핵심 구현

```
class TreeNode(val v: Int, var left: TreeNode? = null, var right: TreeNode? = null)

fun insert(root: TreeNode?, v: Int): TreeNode {
    if (root == null) return TreeNode(v)
    if (v < root.v) root.left = insert(root.left, v)
    else if (v > root.v) root.right = insert(root.right, v)
    return root                      // 중복은 무시
}

fun contains(root: TreeNode?, v: Int): Boolean = when {
    root == null -> false
    v == root.v  -> true
    v < root.v   -> contains(root.left, v)
    else         -> contains(root.right, v)
}
```

- 실무에선 직접 BST 를 쓰기보다 균형이 보장된 `TreeMap` / `TreeSet` 을 쓴다(형제 노트 `집합`).
- **정렬·범위질의·후속자 탐색** 가 필요할 때가 트리의 자리. 단순 키-값은 해시맵이 더 빠르다.

> **요약: 왼\<자신\<오른 규칙으로 정렬을 유지해 평균 O(log n). 단 편향되면 O(n) 으로 무너진다 — 그래서 실전은 자가 균형 트리(RB/AVL)를 쓴다.**
