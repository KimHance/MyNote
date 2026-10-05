package com.hancekim.mynote.insight

import com.hancekim.mynote.core.domain.model.WrongAnswer
import com.hancekim.mynote.core.domain.stats.DigestCategory
import com.hancekim.mynote.core.domain.stats.DigestTotals
import com.hancekim.mynote.core.domain.stats.WeeklyDigest
import java.time.LocalDate

internal data class DigestCase(
    val id: String,
    val digest: WeeklyDigest,
)

internal data class WrongCase(
    val id: String,
    val wrong: List<WrongAnswer>,
)

private val week = LocalDate.of(2026, 9, 28)
private val categoryIds = listOf("android", "compose", "cs", "datastructure", "kotlin")

private fun categories(vararg minutes: Pair<String, Triple<Int, Int, Int>>): List<DigestCategory> {
    val given = minutes.toMap()
    return categoryIds.map { id ->
        val (min, correct, asked) = given[id] ?: Triple(0, 0, 0)
        DigestCategory(id, min, correct, asked)
    }
}

private fun digest(
    days: Int,
    daysLeft: Int,
    current: DigestTotals,
    previous: DigestTotals?,
    categories: List<DigestCategory>,
    stalled: List<String> = emptyList(),
    reread: String? = null,
    weekStart: LocalDate = week,
) = WeeklyDigest(
    weekStart = weekStart,
    days = days,
    daysLeft = daysLeft,
    current = current,
    previous = previous,
    change =
        previous?.let {
            DigestTotals(
                notes = current.notes - it.notes,
                minutes = current.minutes - it.minutes,
                quizzes = current.quizzes - it.quizzes,
                accuracy = current.accuracy?.let { now -> it.accuracy?.let { before -> now - before } },
            )
        },
    categories = categories,
    stalled = stalled,
    reread = reread,
)

internal val digestCases =
    listOf(
        DigestCase(
            "busy-week",
            digest(
                days = 6,
                daysLeft = 1,
                current = DigestTotals(11, 214, 6, 78),
                previous = DigestTotals(7, 140, 4, 70),
                categories =
                    categories(
                        "android" to Triple(64, 9, 12),
                        "compose" to Triple(41, 4, 6),
                        "cs" to Triple(18, 0, 0),
                        "datastructure" to Triple(55, 5, 6),
                        "kotlin" to Triple(36, 3, 4),
                    ),
                stalled = listOf("Compose 스냅샷 상태 시스템 (Snapshot State / MVCC)"),
                reread = "해시맵 / 해시테이블 (HashMap)",
            ),
        ),
        DigestCase(
            "one-category",
            digest(
                days = 4,
                daysLeft = 3,
                current = DigestTotals(5, 97, 2, 67),
                previous = DigestTotals(4, 60, 1, 50),
                categories = categories("datastructure" to Triple(97, 4, 6)),
                stalled = listOf("균형 이진트리 (Red-Black / AVL)", "트라이 (Trie, 접두사 트리)"),
                reread = "균형 이진트리 (Red-Black / AVL)",
            ),
        ),
        DigestCase(
            "no-quiz",
            digest(
                days = 3,
                daysLeft = 2,
                current = DigestTotals(4, 58, 0, null),
                previous = DigestTotals(3, 41, 2, 75),
                categories = categories("android" to Triple(33, 0, 0), "kotlin" to Triple(25, 0, 0)),
                stalled = listOf("Kotlin 제네릭 (기초~심화)"),
            ),
        ),
        DigestCase(
            "no-last-week",
            digest(
                days = 2,
                daysLeft = 4,
                current = DigestTotals(3, 34, 1, 100),
                previous = null,
                categories = categories("compose" to Triple(22, 3, 3), "android" to Triple(12, 0, 0)),
            ),
        ),
        DigestCase(
            "cs-zero",
            digest(
                days = 5,
                daysLeft = 2,
                current = DigestTotals(8, 131, 3, 64),
                previous = DigestTotals(6, 120, 3, 64),
                categories =
                    categories(
                        "android" to Triple(48, 3, 4),
                        "compose" to Triple(30, 2, 4),
                        "datastructure" to Triple(29, 2, 3),
                        "kotlin" to Triple(24, 0, 0),
                    ),
                reread = "Navigation 3 첫인상",
            ),
        ),
        DigestCase(
            "light-week",
            digest(
                days = 1,
                daysLeft = 5,
                current = DigestTotals(1, 12, 0, null),
                previous = DigestTotals(5, 88, 2, 80),
                categories = categories("cs" to Triple(12, 0, 0)),
                stalled = listOf("HTTP 캐싱 기초"),
            ),
        ),
        DigestCase(
            "quiz-heavy",
            digest(
                days = 4,
                daysLeft = 3,
                current = DigestTotals(3, 26, 7, 41),
                previous = DigestTotals(4, 52, 2, 63),
                categories =
                    categories(
                        "datastructure" to Triple(14, 6, 15),
                        "kotlin" to Triple(12, 5, 12),
                    ),
                stalled = listOf("스택 (Stack)", "큐 & 덱 (Queue & Deque)"),
            ),
        ),
        DigestCase(
            "declining",
            digest(
                days = 2,
                daysLeft = 0,
                weekStart = week.minusWeeks(1),
                current = DigestTotals(2, 31, 1, 33),
                previous = DigestTotals(6, 104, 3, 82),
                categories = categories("android" to Triple(20, 1, 3), "compose" to Triple(11, 0, 0)),
                stalled = listOf("Molecule 로 ViewModel 상태 만들기"),
            ),
        ),
        DigestCase(
            "finished-with-reread",
            digest(
                days = 5,
                daysLeft = 0,
                weekStart = week.minusWeeks(1),
                current = DigestTotals(6, 142, 4, 88),
                previous = DigestTotals(6, 139, 4, 81),
                categories =
                    categories(
                        "kotlin" to Triple(71, 8, 9),
                        "android" to Triple(45, 6, 7),
                        "cs" to Triple(26, 1, 1),
                    ),
                stalled = listOf("sealed interface 활용"),
                reread = "코루틴 디스패처 메모",
            ),
        ),
        DigestCase(
            "six-categories-long-titles",
            WeeklyDigest(
                weekStart = week,
                days = 3,
                daysLeft = 3,
                current = DigestTotals(6, 77, 2, 56),
                previous = DigestTotals(2, 19, 0, null),
                change = DigestTotals(4, 58, 2, null),
                categories =
                    listOf(
                        DigestCategory("algorithm", 9, 0, 0),
                        DigestCategory("android", 21, 3, 4),
                        DigestCategory("compose", 18, 2, 5),
                        DigestCategory("cs", 0, 0, 0),
                        DigestCategory("datastructure", 17, 0, 0),
                        DigestCategory("kotlin", 12, 0, 0),
                    ),
                stalled =
                    listOf(
                        "Compose SlotTable & 갭버퍼 (Composition 저장 구조)",
                        "유니온 파인드 / 분리집합 (Union-Find)",
                        "LRU 캐시 (Least Recently Used)",
                    ),
                reread = "Compose 상태 관리 정리",
            ),
        ),
    )

private fun wrong(
    noteId: String,
    category: String,
    question: String,
    picked: String,
    answer: String,
) = WrongAnswer(noteId, category, question, picked, answer, 0L)

internal val noteTitles =
    mapOf(
        "stack" to "스택 (Stack)",
        "queue-deque" to "큐 & 덱 (Queue & Deque)",
        "set" to "집합 (Set)",
        "balanced-tree" to "균형 이진트리 (Red-Black / AVL)",
        "array-list" to "배열 & ArrayList",
        "hash-map" to "해시맵 / 해시테이블 (HashMap)",
        "heap" to "힙 & 우선순위 큐 (Heap & PriorityQueue)",
        "compose-state" to "Compose 상태 관리 정리",
        "generics" to "Kotlin 제네릭 (기초~심화)",
        "http-cache" to "HTTP 캐싱 기초",
        "coroutines" to "코루틴 디스패처 메모",
        "sealed" to "sealed interface 활용",
        "bst" to "이진탐색트리 (BST)",
        "trie" to "트라이 (Trie, 접두사 트리)",
        "union-find" to "유니온 파인드 / 분리집합 (Union-Find)",
        "nav3" to "Navigation 3 첫인상",
        "slot-table" to "Compose SlotTable & 갭버퍼 (Composition 저장 구조)",
    )

internal val wrongCases =
    listOf(
        WrongCase(
            "stack-queue",
            listOf(
                wrong("stack", "datastructure", "스택에서 마지막에 넣은 원소가 먼저 나오는 방식을 무엇이라 하나요?", "FIFO", "LIFO"),
                wrong("queue-deque", "datastructure", "큐에서 원소를 꺼낼 때 어떤 원소가 먼저 나오나요?", "가장 나중에 넣은 원소", "가장 먼저 넣은 원소"),
                wrong(
                    "queue-deque",
                    "datastructure",
                    "덱(Deque)을 스택처럼 쓸 때 push 와 pop 은 어느 쪽에서 일어나나요?",
                    "서로 반대쪽 끝",
                    "같은 쪽 끝",
                ),
            ),
        ),
        WrongCase(
            "complexity",
            listOf(
                wrong("set", "datastructure", "HashSet 의 contains 평균 시간 복잡도는?", "O(log n)", "O(1)"),
                wrong("balanced-tree", "datastructure", "TreeSet 에서 원소를 찾는 시간 복잡도는?", "O(1)", "O(log n)"),
                wrong("array-list", "datastructure", "ArrayList 맨 앞에 원소를 넣는 비용은?", "O(1)", "O(n)"),
                wrong("hash-map", "datastructure", "해시 충돌이 모두 한 버킷에 몰리면 조회 비용은?", "O(1)", "O(n)"),
            ),
        ),
        WrongCase(
            "compose-state",
            listOf(
                wrong("compose-state", "compose", "화면 회전 뒤에도 값을 유지하려면 무엇을 써야 하나요?", "remember", "rememberSaveable"),
                wrong("compose-state", "compose", "remember 로 만든 값은 언제 사라지나요?", "구성 변경 뒤에도 남는다", "컴포지션을 떠나면 사라진다"),
                wrong("compose-state", "compose", "State 값을 읽은 범위만 다시 그리는 단위는?", "Activity 전체", "읽은 컴포저블 범위"),
            ),
        ),
        WrongCase(
            "generics-variance",
            listOf(
                wrong("generics", "kotlin", "꺼내기만 하는 제네릭 타입 파라미터에 붙이는 변성 키워드는?", "in", "out"),
                wrong("generics", "kotlin", "Comparable<T> 처럼 소비만 하는 타입 파라미터에 붙이는 키워드는?", "out", "in"),
                wrong(
                    "generics",
                    "kotlin",
                    "List<out T> 가 공변이라는 뜻은?",
                    "하위 타입 리스트에 쓸 수 있다",
                    "하위 타입 리스트를 상위 타입으로 읽을 수 있다",
                ),
            ),
        ),
        WrongCase(
            "http-cache",
            listOf(
                wrong("http-cache", "cs", "Cache-Control: no-cache 의 뜻은?", "저장하지 않는다", "저장하되 쓰기 전에 재검증한다"),
                wrong("http-cache", "cs", "Cache-Control: no-store 의 뜻은?", "재검증 후 사용한다", "어디에도 저장하지 않는다"),
                wrong("http-cache", "cs", "ETag 를 보내 재검증할 때 쓰는 요청 헤더는?", "If-Modified-Since", "If-None-Match"),
                wrong("http-cache", "cs", "변경이 없을 때 서버가 돌려주는 상태 코드는?", "200", "304"),
            ),
        ),
        WrongCase(
            "mixed-eight",
            listOf(
                wrong("coroutines", "kotlin", "CPU 를 많이 쓰는 계산에 알맞은 디스패처는?", "Dispatchers.IO", "Dispatchers.Default"),
                wrong("coroutines", "kotlin", "파일·네트워크처럼 기다리는 작업에 알맞은 디스패처는?", "Dispatchers.Default", "Dispatchers.IO"),
                wrong(
                    "sealed",
                    "kotlin",
                    "sealed interface 의 when 이 else 없이 컴파일되는 이유는?",
                    "런타임에 검사해서",
                    "하위 타입을 컴파일러가 모두 알아서",
                ),
                wrong("bst", "datastructure", "BST 를 중위 순회하면 어떤 순서로 나오나요?", "넣은 순서", "오름차순"),
                wrong("trie", "datastructure", "트라이에서 접두사 검색 비용은 무엇에 비례하나요?", "저장된 단어 수", "접두사 길이"),
                wrong("union-find", "datastructure", "유니온 파인드의 경로 압축이 줄이는 것은?", "메모리", "find 의 트리 높이"),
                wrong("nav3", "android", "Navigation 3 에서 백스택의 주인은 누구인가요?", "NavController", "앱이 가진 리스트"),
                wrong("slot-table", "compose", "SlotTable 의 갭버퍼가 빠르게 하는 연산은?", "임의 위치 조회", "현재 위치 근처 삽입·삭제"),
            ),
        ),
    )

internal val extraDigestCases =
    listOf(
        DigestCase(
            "long-stalled-titles",
            digest(
                days = 3,
                daysLeft = 2,
                current = DigestTotals(3, 64, 1, 50),
                previous = DigestTotals(4, 70, 0, null),
                categories = categories("compose" to Triple(52, 2, 4), "android" to Triple(12, 0, 0)),
                stalled =
                    listOf(
                        "Compose 스냅샷 상태 시스템 (Snapshot State / MVCC)",
                        "Compose SlotTable & 갭버퍼 (Composition 저장 구조)",
                    ),
                reread = "Molecule 로 ViewModel 상태 만들기",
            ),
        ),
    )

internal val extraWrongCases =
    listOf(
        WrongCase(
            "korean-source-bilingual-title",
            listOf(
                wrong("heap", "datastructure", "최소 힙에서 루트에 있는 원소는?", "가장 큰 원소", "가장 작은 원소"),
                wrong("heap", "datastructure", "우선순위 큐에서 꺼내는 원소는?", "먼저 넣은 원소", "우선순위가 가장 높은 원소"),
                wrong("heap", "datastructure", "힙에 원소를 넣은 뒤 하는 작업은?", "전체 정렬", "위로 올리며 자리 맞추기"),
            ),
        ),
        WrongCase(
            "two-topics",
            listOf(
                wrong("stack", "datastructure", "스택에서 마지막에 넣은 원소가 먼저 나오는 방식을 무엇이라 하나요?", "FIFO", "LIFO"),
                wrong("stack", "datastructure", "스택이 비어 있을 때 pop 을 호출하면 어떻게 되나요?", "맨 아래 원소를 돌려준다", "예외가 나거나 실패를 알린다"),
                wrong("set", "datastructure", "집합(Set)의 핵심 정의는 무엇인가요?", "순서가 있는 원소들의 모임", "중복되지 않는 원소들의 모임"),
                wrong("set", "datastructure", "TreeSet을 선택해야 하는 주된 이유는 무엇인가요?", "가장 빠른 포함 검사", "항상 정렬 상태를 유지해야 할 때"),
            ),
        ),
    )
