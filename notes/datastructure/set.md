---
title: 집합 (Set)
date: 2026.06.29
---
중복 없는 원소들의 모임. **"있나/없나"와 중복 제거** 가 핵심. 내부는 대개 맵의 키만 쓰는 형태(값 없음)다.

## 세 가지 구현 — 무엇이 다른가

```
HashSet        해시맵의 키 → 순서 없음,    포함검사 평균 O(1)
LinkedHashSet  해시 + 연결 → 삽입 순서 유지, 평균 O(1)
TreeSet        균형 이진트리 → 정렬 유지,   연산 O(log n)

  내부적으로 HashSet 은 HashMap, TreeSet 은 TreeMap 을 감싼 것.
```

그래서 선택 기준은 단순하다: **순서가 필요 없으면 HashSet**, 넣은 순서를 보존하려면 `LinkedHashSet`, 항상 정렬·범위질의가 필요하면 `TreeSet`.

### 복잡도

```
             add/remove/contains
────────────────────────────────
HashSet       O(1) 평균 / O(n) 최악
LinkedHashSet O(1) 평균 (+순서)
TreeSet       O(log n)  (+정렬·범위)
```

## Kotlin 사용 — 집합 연산

```
val a = setOf(1, 2, 3)
val b = setOf(2, 3, 4)

a union b          // 합집합 {1,2,3,4}
a intersect b      // 교집합 {2,3}
a subtract b       // 차집합 {1}

// 중복 제거: 리스트 → 집합 → 리스트
val unique = listOf(1, 1, 2, 3, 3).toSet().toList()  // [1, 2, 3]

// 가변 집합
val seen = HashSet<String>()
if (seen.add("x")) { /* 처음 본 값일 때만 true */ }
```

- **add 의 반환값** (이미 있으면 false)으로 "처음 보는가"를 O(1) 에 판정 — 방문 체크의 정석(BFS/DFS).
- 원소 객체도 맵 키처럼 `equals/hashCode` 가 정확해야 한다(형제 노트 `해시맵 / 해시테이블`).
- `TreeSet` 은 정렬·`ceiling/floor/headSet` 범위질의가 필요할 때(형제 노트 `균형 이진트리`).

> **요약: 중복 없는 모임. HashSet=무순서 O(1), LinkedHashSet=삽입순서, TreeSet=정렬 O(log n). add 반환값으로 방문 체크, union/intersect/subtract 로 집합 연산.**
