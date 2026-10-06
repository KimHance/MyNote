---
title: LRU 캐시 (Least Recently Used)
date: 2026.06.29
---
용량이 차면 **가장 오래 안 쓴 항목을 버리는** 캐시. **해시맵 + 이중 연결 리스트** 를 엮어 `get·put 을 모두 O(1)` 로 만든다.

## 왜 두 자료구조를 합치나

필요한 두 연산을 각각 O(1) 로 얻으려는 조합이다: **키로 즉시 찾기** 는 해시맵(형제 노트 `해시맵 / 해시테이블`), **사용 순서 갱신·맨 끝 제거** 는 이중 연결 리스트(형제 노트 `연결 리스트`)가 담당한다.

```
HashMap: key ──► 노드 참조 (O(1) 조회)

이중 연결 리스트 (사용 순서):
  [MRU] head ◄─►(A)◄─►(C)◄─►(B)◄─► tail [LRU]
          최근 사용                 오래됨 → 꽉 차면 여기서 evict

  get(C): 맵으로 C 노드 즉시 찾고 → 리스트에서 떼어 head 로 이동 (O(1))
  put(new): head 에 추가, 용량 초과면 tail 직전 노드 제거 + 맵에서 삭제
```

### 복잡도

```
연산   복잡도
──────────────
get    O(1)   맵 조회 + 리스트 이동(참조라 시프트 없음)
put    O(1)   맵 삽입 + head 추가 (+초과 시 tail 제거)

  배열/단일연결로는 "순서 이동"이 O(n) — 이중 연결이라 O(1).
```

## Kotlin — LinkedHashMap accessOrder

직접 엮지 않아도 `LinkedHashMap` 이 내부적으로 해시맵+이중연결을 이미 갖췄다. `accessOrder=true` 로 두고 `removeEldestEntry` 를 오버라이드하면 LRU 가 된다.

```
fun <K, V> lruCache(capacity: Int): LinkedHashMap<K, V> =
    object : LinkedHashMap<K, V>(capacity, 0.75f, /* accessOrder = */ true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean =
            size > capacity            // 용량 초과 시 가장 오래된 항목 자동 제거
    }

val cache = lruCache<Int, String>(2)
cache[1] = "a"; cache[2] = "b"
cache[1]            // 1 사용 → 최신으로
cache[3] = "c"      // 용량 2 초과 → 가장 오래된 2 가 evict
// cache.keys == [1, 3]

// 안드로이드: androidx.collection.LruCache 가 표준 (메모리 캐시·이미지)
```

- **안드로이드** 는 `androidx.collection.LruCache` 를 제공 — 비트맵/메모리 캐시에 바로 쓴다(`sizeOf` 로 바이트 기준 용량도 가능).
- **동시성** 이 필요하면 외부 동기화 또는 전용 캐시 라이브러리. 순수 LinkedHashMap 은 스레드 안전하지 않다.

> **요약: 해시맵(O(1) 조회) + 이중 연결 리스트(O(1) 순서 이동/끝 제거)의 합작으로 get·put 모두 O(1). Kotlin 은 LinkedHashMap(accessOrder=true)+removeEldestEntry, 안드로이드는 androidx LruCache.**
