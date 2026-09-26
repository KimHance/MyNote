package com.hancekim.mynote.core.domain.model

data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    val answerIndex: Int,
    val explanation: String,
)
