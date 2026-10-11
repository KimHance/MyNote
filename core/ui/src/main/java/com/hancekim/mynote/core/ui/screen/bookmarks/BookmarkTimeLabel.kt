package com.hancekim.mynote.core.ui.screen.bookmarks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.hancekim.mynote.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class BookmarkTimeKind {
    Today,
    Yesterday,
    ThisYear,
    OtherYear,
    ;

    companion object {
        fun of(
            addedAt: Long,
            today: LocalDate,
            zone: ZoneId,
        ): BookmarkTimeKind {
            val date = Instant.ofEpochMilli(addedAt).atZone(zone).toLocalDate()
            return when {
                date == today -> Today
                date == today.minusDays(1) -> Yesterday
                date.year == today.year -> ThisYear
                else -> OtherYear
            }
        }
    }
}

@Immutable
private class BookmarkTimeFormats(
    val time: DateTimeFormatter,
    val thisYear: DateTimeFormatter,
    val otherYear: DateTimeFormatter,
    val otherYearShort: DateTimeFormatter,
)

@Composable
private fun rememberBookmarkTimeFormats(): BookmarkTimeFormats {
    val time = stringResource(R.string.ui_bookmark_time)
    val thisYear = stringResource(R.string.ui_bookmark_date_this_year)
    val otherYear = stringResource(R.string.ui_bookmark_date_other_year)
    val otherYearShort = stringResource(R.string.ui_bookmark_date_other_year_short)
    return remember(time, thisYear, otherYear, otherYearShort) {
        BookmarkTimeFormats(
            time = DateTimeFormatter.ofPattern(time, Locale.KOREA),
            thisYear = DateTimeFormatter.ofPattern(thisYear, Locale.KOREA),
            otherYear = DateTimeFormatter.ofPattern(otherYear, Locale.KOREA),
            otherYearShort = DateTimeFormatter.ofPattern(otherYearShort, Locale.KOREA),
        )
    }
}

@Composable
fun rememberBookmarkTimeLabel(
    addedAt: Long,
    long: Boolean,
): String {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(zone) { LocalDate.now(zone) }
    val formats = rememberBookmarkTimeFormats()
    val moment: ZonedDateTime = remember(addedAt, zone) { Instant.ofEpochMilli(addedAt).atZone(zone) }
    val kind = remember(addedAt, today, zone) { BookmarkTimeKind.of(addedAt, today, zone) }
    return when (kind) {
        BookmarkTimeKind.Today -> {
            val time = formats.time.format(moment)
            if (long) stringResource(R.string.ui_bookmark_added_today, time) else time
        }

        BookmarkTimeKind.Yesterday -> {
            if (long) {
                stringResource(R.string.ui_bookmark_added_yesterday, formats.time.format(moment))
            } else {
                stringResource(R.string.ui_bookmark_yesterday)
            }
        }

        BookmarkTimeKind.ThisYear -> {
            val date = formats.thisYear.format(moment)
            if (long) stringResource(R.string.ui_bookmark_added_date, date) else date
        }

        BookmarkTimeKind.OtherYear -> {
            if (long) {
                stringResource(R.string.ui_bookmark_added_date, formats.otherYear.format(moment))
            } else {
                formats.otherYearShort.format(moment)
            }
        }
    }
}
