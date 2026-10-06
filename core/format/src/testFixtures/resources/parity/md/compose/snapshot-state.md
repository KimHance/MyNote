---
title: Compose 스냅샷 상태 시스템 (Snapshot State / MVCC)
date: 2026.06.29
---
여기서 "스냅샷"은 **Compose 의 상태 격리 시스템** 을 말한다. 메모리 힙 스냅샷(프로파일러)이나 VCS 스냅샷이 **아니다**. 헷갈리지 말 것 — 자세한 구분은 마지막 절.

## 1. 무엇인가 / 왜

`mutableStateOf` 로 만든 상태는 평범한 변수가 아니다. 값을 한 칸에 덮어쓰는 게 아니라, **여러 버전을 동시에 보유하는 MVCC**(다중 버전 동시성 제어)로 관리된다. 직관적으로는 **상태계의 git** 이다 — 시점마다 커밋(버전)이 있고, 작업은 격리된 브랜치에서 하다가 합친다.

스냅샷(snapshot) = **특정 시점 전체 상태의 격리된 뷰**. 한 스냅샷 안에서 상태를 읽으면 그 스냅샷이 보는 버전이 일관되게 나온다 — 다른 스레드가 중간에 값을 바꿔도 내 뷰는 흔들리지 않는다.

```
// 평범한 변수처럼 보이지만, 읽기/쓰기가 스냅샷 시스템을 거친다
var count by mutableStateOf(0)

// 이 읽기는 "현재 스냅샷에서 보이는 count 의 버전"을 반환한다
println(count)   // 0
count = 1        // 새 버전(record)을 현재 스냅샷에 기록
```

---

## 2. 내부 구조 — StateObject + StateRecord

각 상태는 `StateObject` 이고, 연결 리스트로 매달린 `StateRecord` (버전)들을 가진다. 각 record 는 **자신이 쓰여진 스냅샷 id 로 태그** 된다. 그래서 한 상태가 동시에 여러 값을 들고 있고, 스냅샷마다 보이는 record 가 달라진다.

```
StateObject  (예: count = mutableStateOf(0))
   │
   └ firstStateRecord ─► [rec @snap1 : 0] ─► [rec @snap4 : 1] ─► [rec @snap7 : 2]
                              ▲                  ▲                  ▲
         각 record = (snapshotId 태그, 값)        스냅샷마다 "보이는" record 가 다르다

  읽기 규칙: 내 snapshotId 이하(<=) 중
            가장 최신이면서 invalid 집합에 없는 record 를 고른다.
```

- 버전마다 객체를 통째로 복제하지 않는다 — 변경된 `StateRecord` 만 새로 단다.
- 더 이상 어떤 스냅샷도 못 보는 오래된 record 는 재사용/회수된다(GC 압박 완화).

---

## 3. 스냅샷 종류

- `global snapshot` — 기본 스냅샷. 아무 스냅샷도 안 잡았을 때 읽기/쓰기가 여기로 간다.
- `MutableSnapshot` — 읽기 + 쓰기 가능한 격리 스냅샷. `apply()` 로 전역 반영.
- `read-only snapshot` — 읽기 전용. 그 시점 상태를 고정해 일관된 뷰로 관찰.
- `nested snapshot` — 스냅샷 안에서 또 잡는 자식 스냅샷. 자식 `apply` 는 부모로만 합쳐진다.

```
// 읽기 전용: 그 순간의 일관된 뷰를 잡아 관찰
val snap = Snapshot.takeSnapshot()
try {
    snap.enter { /* 여기서의 읽기는 잡은 시점 값으로 고정 */ }
} finally {
    snap.dispose()   // 스냅샷은 반드시 dispose
}

// 읽기+쓰기: 격리된 작업공간
val mut = Snapshot.takeMutableSnapshot()
mut.enter { count = 42 }   // 바깥엔 아직 안 보임
mut.apply()                // 전역 반영(충돌 검사)
mut.dispose()
```

---

## 4. 격리와 apply — git branch 비유

`takeMutableSnapshot` 은 git 브랜치를 따는 것과 같다. 그 안에서 상태를 써도 **바깥(전역)에는 보이지 않는다**. `apply()` 를 호출해야 전역에 머지되며, 이때 **충돌 검사** 가 일어난다.

```
global  ──●─────────────────────────●──────►  snapshotId 자동 증가
           \                      apply() ↑  (충돌 검사 후 머지)
  takeMutableSnapshot ●──[ count = 5 ]──────┘
         (격리 작업공간: enter{} 안의 write 는 바깥에서 안 보임)

  충돌: 내가 스냅샷을 잡은 뒤, 같은 StateObject 를
        다른 스냅샷이 먼저 apply 했다면 → 충돌.
```

충돌이 나면 결과는 `SnapshotApplyResult.Failure` 이고, 상태 객체가 병합을 지원하면(`StateObject.mergeRecords`) record 를 병합해 해소한다. 예를 들어 `SnapshotStateList` 같은 컬렉션은 서로 다른 변경을 병합할 수 있다.

```
when (val result = mut.apply()) {
    is SnapshotApplyResult.Success -> { /* 전역 반영 완료 */ }
    is SnapshotApplyResult.Failure -> {
        // 충돌: 머지 불가한 변경 → 재시도하거나 버린다
    }
}
```

- 스레드 격리 덕분에 **찢어진 중간 상태** 가 UI 에 절대 안 보인다 — apply 는 원자적.
- 여러 상태를 한 `apply` 로 묶으면 "전부 보이거나 전부 안 보이거나" 가 보장된다.

---

## 5. 관찰자 — recomposition 의 원리

"State 를 읽기만 했는데 알아서 recompose" 되는 마법의 정체가 바로 **스냅샷 관찰자(observer)** 다. 스냅샷은 읽기/쓰기에 콜백을 붙일 수 있다.

- **read observer** — 컴포저블이 `어떤 state 를 읽었는지` 기록한다. Compose 는 이를 현재 재구성 스코프(`RecomposeScope`)에 매핑한다.
- **write/apply observer** — state 가 바뀌면 그 state 를 읽던 스코프를 `invalid` 로 표시하고 recomposition 을 예약한다.

```
// 개념적으로 Compose 의 Recomposer 가 하는 일
Snapshot.takeMutableSnapshot(
    readObserver  = { state -> currentScope.recordRead(state) },
    writeObserver = { state -> scopesReading(state).forEach { it.invalidate() } }
)
// → 읽힌 state 와 스코프가 연결되고,
//    그 state 가 쓰이면 정확히 그 스코프만 다시 그린다.
```

그래서 recomposition 이 **실제로 읽은 state 단위로 정밀** 하게 일어난다 — 화면 전체가 아니라 그 값을 읽은 스코프만.

---

## 6. 연관 API

- `derivedStateOf { }` — 스냅샷 읽기 위에서 파생값을 캐싱. 의존하는 state 가 바뀔 때만 재계산 → 잦은 입력에서 recomposition 절감.
- `snapshotFlow { }` — 블록 안의 스냅샷 읽기를 `Flow` 로 변환. 읽은 값이 바뀔 때만 emit.
- `Snapshot.withMutableSnapshot { }` — takeMutableSnapshot → 블록 실행 → apply 를 한 번에 묶는 편의 함수.

```
// 파생값: firstVisibleItemIndex 가 0 인지의 결과만 추적
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}

// 스냅샷 read 를 Flow 로: query 가 바뀔 때만 흘러나온다
snapshotFlow { query }
    .debounce(300)
    .collect { search(it) }
```

---

## 7. 헷갈림 방지

이 "스냅샷"은 **Compose 의 상태 격리/버전 관리 시스템** 이다. Android Studio 프로파일러의 `메모리 힙 스냅샷` 이나 git 등 `VCS 스냅샷` 과는 전혀 다른 개념 — 이름만 같다.

> **요약: mutableStateOf 는 MVCC 로 버전을 들고(StateRecord), 스냅샷이 격리된 뷰를 주며(git branch), apply 가 충돌 검사 후 머지하고, read/write observer 가 "읽은 만큼만" recompose 를 일으킨다.**
