---
title: 해시맵 / 해시테이블 (HashMap)
date: 2026.06.29
---
키를 해시함수로 배열 인덱스(버킷)에 매핑해 **평균 O(1) 조회·삽입·삭제** 를 내는 구조. "키로 값을 즉시 찾는다"가 필요하면 1순위.

## 구조 — 해시함수 + 버킷 배열

키의 `hashCode()` 를 버킷 개수로 접어(보통 `hash & (n-1)`) 인덱스를 얻는다. 서로 다른 키가 같은 버킷에 떨어지는 게 **충돌(collision)**.

```
key ──hash()──► 정수 ──& (n-1)──► 버킷 인덱스

buckets (n=4)
[0] ─► ("apple", 3)
[1] ─► null
[2] ─► ("kiwi", 9) ─► ("pear", 5)   ◄ 충돌 → 체이닝(연결)
[3] ─► ("fig", 1)
```

### 충돌 해결 — 체이닝 vs 개방주소법

- **체이닝(separate chaining)** — 버킷마다 연결 리스트(또는 많아지면 트리). Java/Kotlin `HashMap` 방식. 한 버킷이 너무 길어지면 `트리화(treeify)` 해 그 버킷을 O(log n) 으로.
- **개방주소법(open addressing)** — 충돌 시 다음 빈 칸을 탐사(선형/이차/이중해시). 별도 노드 없이 배열 한 장. 캐시 친화적이나 삭제가 까다롭다.

### 리사이즈 & load factor

`load factor` = 원소수 / 버킷수. 이 값이 임계(기본 `0.75`)를 넘으면 버킷을 2배로 늘리고 **전체를 재배치(rehash)** 한다 — 그 순간만 O(n), 평균내면 분할상환.

### 복잡도

```
연산      평균    최악
──────────────────────
get       O(1)    O(n)   최악=한 버킷에 다 몰림(나쁜 해시)
put       O(1)*   O(n)   *리사이즈 분할상환
remove    O(1)    O(n)
  체이닝 트리화 시 최악 O(log n) 으로 완화
```

## Kotlin 사용

```
val map = HashMap<String, Int>()   // 순서 보장 없음
map["a"] = 1
val v = map["a"]                   // 평균 O(1)
map.getOrPut("b") { 0 }            // 없으면 기본값 넣고 반환

val ordered = LinkedHashMap<String, Int>()  // 삽입 순서 유지
// accessOrder=true 면 접근 순서 → LRU 캐시의 토대 (형제 노트)
```

- **equals/hashCode 계약** 필수 — 키로 쓰는 객체는 둘을 함께 올바르게 구현(`data class` 가 자동 생성). 가변 필드를 해시에 쓰면 키가 미아가 된다.
- 순서가 필요하면 `LinkedHashMap`, 정렬이 필요하면 `TreeMap` (형제 노트 `균형 이진트리`).
- 안드로이드 소수 정수키는 `SparseArray` / `ArrayMap` 가 박싱·메모리 면에서 유리.

> **요약: 해시함수로 버킷에 흩뿌려 평균 O(1). 충돌은 체이닝(자바식)·개방주소법으로 풀고, load factor 0.75 초과 시 2배 리사이즈+rehash. 키 객체는 equals/hashCode 를 반드시 같이 구현.**
