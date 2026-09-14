package com.hancekim.mynote.core.domain.model

/**
 * 노트에서 뽑아낸 4지선다 문제 하나.
 *
 * [answerIndex] 는 [choices] 의 인덱스다. 범위를 벗어난 값은 생성 단계에서 걸러진다 —
 * 모델이 1-based 로 세거나 없는 번호를 쓰는 일이 흔하다.
 */
data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    val answerIndex: Int,
    val explanation: String,
)
