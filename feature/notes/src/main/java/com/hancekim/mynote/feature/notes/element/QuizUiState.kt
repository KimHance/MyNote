package com.hancekim.mynote.feature.notes.element

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.feature.notes.QuizQuestionUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 리더 위에 얹히는 퀴즈의 상태.
 *
 * **내비게이션 목적지가 아니다.** `Navigator` 의 백스택을 건드리지 않는다 — 퀴즈는 노트를
 * 읽던 자리 위에 잠깐 덮이는 상태이고, 나가면 읽던 자리가 그대로 있어야 한다.
 *
 * 고른 답([Solving.picked])과 채점 누적([Solving.marks])이 화면이 아니라 여기 있는 이유:
 * `rememberSaveable` 에 두면 회전·프로세스 사망·백프레스 복구에서 진행이 어긋난다.
 * 실제로 그렇게 만든 첫 구현에서 고르지도 않은 문제에 "오답" 이 떴다.
 */
@Immutable
sealed interface QuizUiState {
    data object Idle : QuizUiState

    /** 설정 다이얼로그. 노트 제목은 화면이 [NotesUiState.noteById] 로 찾는다 — 두 벌로 들지 않는다. */
    data class Setup(
        val noteId: String,
        val maxCount: Int = DEFAULT_COUNT,
    ) : QuizUiState

    /** 생성 중. 순환 문구·슬롯 채움 같은 **연출 진행도는 여기 없다** — 화면이 자기 타이머로 돈다. */
    data class Loading(
        val noteId: String,
        val maxCount: Int,
    ) : QuizUiState

    data class Solving(
        val questions: ImmutableList<QuizQuestionUi>,
        val index: Int = 0,
        /** null 이면 아직 안 골랐다. 한 문항당 한 번만 고를 수 있다. */
        val picked: Int? = null,
        val marks: ImmutableList<Boolean> = persistentListOf(),
        /** 요청한 수보다 적게 만들어졌다. 실패가 아니라 안내다. */
        val partial: Boolean = false,
        /**
         * 이 문항이 **스스로** 다음으로 넘어가는가.
         *
         * 맞힌 문제에서 버튼을 누르게 하는 건 헛수고라 자동으로 넘긴다. 다만 TalkBack 이
         * 켜져 있으면 읽는 중에 화면이 바뀌면 안 되므로 자동 진행을 끄는데, 그때는
         * **버튼이 대신 떠야 막다른 길이 안 된다.** 화면이 그걸 알아야 해서 상태에 둔다.
         */
        val autoAdvancing: Boolean = false,
    ) : QuizUiState {
        val current: QuizQuestionUi get() = questions[index]

        val isLast: Boolean get() = index == questions.lastIndex

        val revealed: Boolean get() = picked != null

        val isCorrect: Boolean get() = picked == current.answerIndex

        /**
         * 채점. 이미 고른 문항은 그대로 둔다 — 다시 눌러 답을 바꾸는 건 퀴즈가 아니다.
         *
         * 순수 함수라 화면·코루틴 없이 검증할 수 있다.
         */
        fun pick(choice: Int): Solving =
            if (revealed || choice !in current.choices.indices) {
                this
            } else {
                copy(picked = choice, marks = (marks + (choice == current.answerIndex)).toImmutableList())
            }

        /** 다음 문항으로. 마지막이었으면 [Result] 를 낸다. */
        fun advance(): QuizUiState =
            when {
                !revealed -> this
                isLast -> Result(marks)
                else -> copy(index = index + 1, picked = null, autoAdvancing = false)
            }
    }

    data class Result(
        val marks: ImmutableList<Boolean>,
    ) : QuizUiState {
        val total: Int get() = marks.size

        val score: Int get() = marks.count { it }

        /** 0 문제는 결과 화면에 오지 않는다(그건 [Failed.Kind.Empty]). 그래도 나눗셈은 지킨다. */
        val rate: Float get() = if (total == 0) 0f else score.toFloat() / total
    }

    /**
     * 실패. [kind] 가 안내 문장과 재시도 수단을 가른다 —
     * "3문제로 줄여서 다시" 는 출력이 짧아져 실제로 성공률이 오를 때만 보여 준다.
     */
    data class Failed(
        val kind: Kind,
        val maxCount: Int,
        val noteId: String,
    ) : QuizUiState {
        enum class Kind { Parse, Empty, Engine, Timeout }

        /** 노트가 짧아 낼 게 없었던 거라면 문항 수를 줄여도 달라지지 않는다. */
        val canRetryWithFewer: Boolean
            get() = kind != Kind.Empty && maxCount > MIN_COUNT
    }

    /**
     * 리더를 통째로 덮는 상태인가.
     *
     * 설정 다이얼로그는 **덮지 않는다** — 모달이라 뒤의 노트가 스크림 너머로 그대로 있어야 한다.
     * 이걸 구분하지 않고 퀴즈 전체를 기준으로 리더의 헤더를 감추면, 다이얼로그가 열리는 순간
     * 뒤의 본문이 헤더 높이만큼 위로 튀어 스크림 너머로 그게 보인다.
     */
    val coversReader: Boolean
        get() = this is Loading || this is Solving || this is Result || this is Failed

    companion object {
        const val MIN_COUNT = 3
        const val DEFAULT_COUNT = 4
        const val MAX_COUNT = 5

        /** 다이얼로그 세그먼트에 그대로 쓰인다. */
        val COUNT_CHOICES: ImmutableList<Int> = persistentListOf(MIN_COUNT, DEFAULT_COUNT, MAX_COUNT)
    }
}
