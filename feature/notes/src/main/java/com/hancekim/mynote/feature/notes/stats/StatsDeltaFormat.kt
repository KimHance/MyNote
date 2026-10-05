package com.hancekim.mynote.feature.notes.stats

import kotlin.math.abs

internal object StatsDeltaFormat {
    const val DASH = "—"
    private const val MINUS = "−"
    private const val PLUS = "+"
    private const val PERCENT_POINT = "%p"
    private const val PERCENT = "%"

    fun signed(
        diff: Int,
        percentPoint: Boolean,
    ): String {
        val body = abs(diff).toString() + if (percentPoint) PERCENT_POINT else ""
        return when {
            diff > 0 -> PLUS + body
            diff < 0 -> MINUS + body
            else -> body
        }
    }

    fun magnitude(
        diff: Int,
        unit: String,
    ): String = abs(diff).toString() + unit

    fun percent(value: Int): String = value.toString() + PERCENT
}
