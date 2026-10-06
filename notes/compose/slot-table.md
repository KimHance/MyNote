---
title: Compose SlotTable & 갭버퍼 (Composition 저장 구조)
date: 2026.06.29
---
형제 노트 **Compose 스냅샷 상태 시스템** 이 "무엇을 다시 그릴지"를 추적하는 **상태 추적 레이어** 라면, 이 노트의 `SlotTable` 은 그 결과인 UI 트리를 담는 **트리 저장 레이어** 다. 둘은 맞물려 동작한다(마지막 절).

## 1. 무엇 / 왜

Composition 의 결과(UI 트리)는 `SlotTable` 에 저장된다. 트리를 노드 객체 그래프로 들지 않고 **평평한 배열에 직렬화** 한다 — 깊이 우선 순서로 펼쳐 담는다.

문제는 재구성(recomposition) 때 트리 **중간에 삽입/삭제** 가 잦다는 것이다. 평범한 배열은 중간 수정이 `O(n)` (뒤를 전부 시프트). 이걸 **갭버퍼(gap buffer)** 로 해결한다.

- 객체 트리 대신 배열 → 캐시 지역성↑, 할당↓.
- 편집이 몰리는 지점에 `gap` 을 두어 삽입/삭제를 분할상환 싸게.

---

## 2. SlotTable 내부 구조 — 두 배열

`SlotTable` 은 크게 두 배열로 이뤄진다. (정확한 필드 레이아웃은 버전마다 다르므로 **개념** 으로 본다.)

- `groups: IntArray` — 그룹 **메타데이터**. 그룹당 고정폭 레코드로, key·그룹/노드 개수·부모 anchor·data(slot) 시작 위치 등을 정수로.
- `slots: Array<Any?>` — 실제 **값**. remember 된 값, 컴포저블 파라미터, `LayoutNode` 등 객체 참조.

`group` = **하나의 컴포저블 호출 단위(스코프)**. 그룹이 자식 그룹을 포함해 트리 구조를 이룬다.

```
groups: IntArray  (그룹당 고정폭 레코드 — 정수 메타데이터)
┌──────────┬──────────┬──────────┬──────────┐
│ G0 root  │ G1 Column│ G2 Text  │ G3 Button│ ...
│ key,size │ key,size │ key,size │ key,size │
│ slotStart│ slotStart│ slotStart│ slotStart│
└────┬─────┴────┬─────┴────┬─────┴────┬─────┘
     │          │          │          │   각 group 의 slotStart 가
     ▼          ▼          ▼          ▼   slots 의 한 구간을 가리킨다
slots: Array<Any?> (실제 값/객체)
[ .. ][ remember값 ][ "Hello" ][ onClick 람다 ][ LayoutNode ][ .. ]

  groups = 트리의 뼈대(메타)   ·   slots = 거기 매달린 실제 데이터
```

---

## 3. 갭버퍼란

갭버퍼는 텍스트 에디터(Emacs)에서 온 자료구조다. 배열 안에 **이동 가능한 빈 구간(gap)** 을 둔다. 편집 지점에 gap 을 맞춰두면 삽입/삭제가 `O(1)` 이고, 편집 위치가 바뀔 때 **gap 을 옮기는 비용만** 원소 시프트로 발생한다.

```
초기:   [a][b][c][_][_][_][d][e]      gap = 인덱스 3..5 (빈 구간)
                  └──gap──┘

c 뒤에 x 삽입:  gap 시작에 써넣고 gap 을 한 칸 줄인다 → O(1)
        [a][b][c][x][_][_][d][e]
                     └─gap─┘

편집 위치를 a 뒤로 이동: gap 을 왼쪽으로 옮긴다(그만큼만 시프트)
        [a][_][_][b][c][x][d][e]
            └gap┘

  핵심: 편집이 한 곳에 몰리면 gap 이동이 드물어 분할상환 저렴.
```

---

## 4. Composer 의 reader / writer

Composer 는 SlotTable 을 두 가지 커서로 다룬다. 한 시점에 **reader 또는 writer 중 하나만** 연다.

- `SlotReader` — 순차 **읽기**. 재구성 시 기존 트리를 훑어 위치/값을 따라간다(구조 변경 없음).
- `SlotWriter` — 구조 **변경**. gap 을 편집 위치로 옮긴 뒤 group/slot 을 삽입·삭제·이동한다.

```
// 개념적 흐름 (실제 API 형태와 다를 수 있음)
val reader = slotTable.openReader()
try {
    // 기존 group 들을 순차로 방문하며 변경 필요 지점 탐지
} finally { reader.close() }

val writer = slotTable.openWriter()
try {
    writer.moveTo(editIndex)   // gap 을 편집 지점으로
    writer.insertGroup(...)    // 거기서 삽입/삭제/이동
} finally { writer.close() }
// reader 와 writer 는 동시에 열 수 없다
```

---

## 5. 재구성 국소성 + Anchor

재구성은 보통 트리의 **작은 국소 영역** 만 바꾼다(스냅샷이 invalidate 한 스코프). gap 을 그 활성 편집 지점 근처에 유지하면 변경들이 서로 가까워 싸진다 — 갭버퍼가 빛나는 지점.

문제는 gap 이동·삽입으로 **슬롯의 절대 인덱스가 흔들린다** 는 것. 그래서 `Anchor` 를 쓴다 — 슬롯이 이동해도 같은 group 을 가리키는 **안정 참조** 다. RecomposeScope 등이 anchor 로 자기 위치를 기억한다.

```
// 인덱스는 흔들려도 anchor 는 같은 group 을 추적
val anchor: Anchor = slotTable.anchor(groupIndex)
// ...gap 이동/삽입으로 인덱스가 바뀐 뒤에도...
val current = slotTable.anchorIndex(anchor)  // 갱신된 실제 위치
```

- 인덱스 = 흔들리는 좌표,  `Anchor` = 안정된 신원(identity).

---

## 6. key / movableContent

그룹은 위치(호출 순서)로 식별되는 게 기본이지만, `key()` 로 명시적 식별자를 주면 순서가 바뀌어도 같은 group(=상태)을 매칭한다.

```
// key: 항목이 재정렬돼도 상태가 따라가도록 group 신원 고정
for (item in items) {
    key(item.id) { Row(item) }   // slot table 에서 item.id 로 group 매칭
}

// movableContent: group 서브트리를 통째로 "이동"(상태 보존 재배치)
val movable = remember { movableContentOf { Profile(user) } }
if (wide) Row { movable() } else Column { movable() }
// 위치가 바뀌어도 같은 slot 구간을 옮겨와 재사용 → 상태 유지
```

---

## 7. 스냅샷과의 연결

두 레이어가 맞물린다: **스냅샷 시스템** 이 어떤 `RecomposeScope` 가 invalidate 됐는지(=무엇을 다시 그릴지)를 정하면, `Composer` 가 SlotTable 의 **해당 group 만** reader/writer 로 갱신한다.

```
[상태 추적 레이어]  Snapshot: state 변경 → 그 state 를 읽은 RecomposeScope invalidate
        │
        ▼  "이 group 들을 다시 구성하라"
[트리 저장 레이어]  Composer → SlotTable: 해당 group 에 reader 로 진입,
                    필요하면 writer 로 gap 옮겨 group/slot 갱신
        │
        ▼
[적용]  Applier: 바뀐 노드를 LayoutNode 트리에 반영 → 측정/배치/그리기

  snapshot = 무엇을(what)   ·   slot table = 어디에 어떻게(where/how)
```

즉 스냅샷이 변경의 **범위** 를 좁혀주기 때문에 SlotTable 편집도 국소적이고, 그래서 gap 기반 갱신이 효율적으로 맞아떨어진다. (형제 노트: `Compose 스냅샷 상태 시스템`)

> **요약: SlotTable 은 UI 트리를 groups(메타)+slots(값) 두 배열에 평탄화하고, 갭버퍼로 중간 삽입/삭제를 분할상환 O(1) 로 만든다. Anchor 가 인덱스 흔들림을 흡수하고, 스냅샷이 정한 국소 범위만 reader/writer 로 갱신한다.**
