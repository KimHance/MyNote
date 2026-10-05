package com.hancekim.mynote.core.data.format

import java.time.DateTimeException
import java.time.LocalDate

private val DATE_SHAPE = Regex("""(\d{4})[.\-](\d{2})[.\-](\d{2})""")

fun epochDayOf(text: String?): Long? {
    val match = text?.trim()?.let { DATE_SHAPE.matchAt(it, 0) } ?: return null
    val (year, month, day) = match.destructured
    return try {
        LocalDate.of(year.toInt(), month.toInt(), day.toInt()).toEpochDay()
    } catch (_: DateTimeException) {
        null
    }
}
