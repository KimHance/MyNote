---
title: 트라이 (Trie, 접두사 트리)
date: 2026.06.29
---
문자열을 **문자 단위로 쪼개 경로로 저장** 하는 트리. 공통 접두사(prefix)를 공유해서, **접두사 검색·자동완성** 을 문자열 길이에 비례해 처리한다.

## 구조 — 간선이 문자, 경로가 단어

```
단어: "cat", "car", "card", "dog"

        (root)
        /    \
       c      d
       |      |
       a      o
      / \     |
     t*  r    g*
         |
         d*        * = 단어 끝(isEnd) 표시

  "car" 와 "card" 는 c-a-r 경로를 공유 → 접두사 공유가 핵심.
```

각 노드는 `자식 맵(문자→노드)` 과 `단어 끝 표시(isEnd)` 를 가진다. "car" 로 시작하는 모든 단어"는 c-a-r 노드 아래 서브트리 전체다.

### 복잡도 — n 무관, 길이 L 에 비례

```
연산              복잡도   (L = 문자열 길이)
────────────────────────────────────────
삽입              O(L)
탐색(정확)        O(L)
접두사 검색       O(L + 결과수)

  단어 개수 n 과 무관! 해시맵은 키 전체를 해시하지만
  트라이는 prefix 질의를 자연스럽게 지원한다.
```

대가는 **공간** — 노드마다 자식 맵을 들어 메모리를 많이 쓴다. 시간(빠른 prefix)과 공간의 트레이드오프.

## Kotlin — 핵심 구현

```
class TrieNode {
    val children = HashMap<Char, TrieNode>()
    var isEnd = false
}

class Trie {
    private val root = TrieNode()

    fun insert(word: String) {
        var node = root
        for (c in word) node = node.children.getOrPut(c) { TrieNode() }
        node.isEnd = true
    }

    fun startsWith(prefix: String): Boolean {
        var node = root
        for (c in prefix) node = node.children[c] ?: return false
        return true                  // 접두사 경로가 존재
    }
}
```

- **자동완성** · 사전/맞춤법 · IP 라우팅 · 검색어 추천에 쓴다.
- 메모리가 부담이면 자식 맵을 배열 대신 해시맵으로(형제 노트 `해시맵 / 해시테이블`), 또는 압축 트라이(Radix)로 경로를 묶는다.

> **요약: 문자를 간선으로, 단어를 경로로 저장해 접두사를 공유. 삽입·탐색·접두사 검색이 단어 수 n 과 무관하게 길이 L 에 비례. 빠른 prefix 질의를 공간으로 산다 — 자동완성의 정석.**
