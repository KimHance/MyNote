package com.hancekim.mynote.feature.stats

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.core.domain.stats.StatsThresholds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Immutable
sealed interface GeneratedAgo {
    data object Today : GeneratedAgo

    data object Yesterday : GeneratedAgo

    data class Recent(
        val date: LocalDate,
    ) : GeneratedAgo

    data class Stale(
        val days: Int,
    ) : GeneratedAgo

    companion object {
        fun of(
            generatedAt: Long,
            today: LocalDate,
            zone: ZoneId,
        ): GeneratedAgo {
            val date = Instant.ofEpochMilli(generatedAt).atZone(zone).toLocalDate()
            val days = ChronoUnit.DAYS.between(date, today).toInt()
            return when {
                days <= 0 -> Today
                days == 1 -> Yesterday
                days < StatsThresholds.STALE_INSIGHT_DAYS -> Recent(date)
                else -> Stale(days)
            }
        }
    }
}
