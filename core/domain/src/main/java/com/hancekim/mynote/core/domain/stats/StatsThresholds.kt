package com.hancekim.mynote.core.domain.stats

object StatsThresholds {
    const val SHORT_RANGE_DAYS = 14
    const val MAX_MONTH_BUCKETS = 12
    const val STALLED_BELOW_PERCENT = 90
    const val STALE_BOOKMARK_DAYS = 14
    const val LIST_LIMIT = 5
    const val MIN_CATEGORIES_FOR_LOWEST = 2

    const val INSIGHT_MIN_SESSIONS = 3
    const val INSIGHT_MIN_QUIZZES = 2
    const val INSIGHT_REFRESH_EVENTS = 20
    const val INSIGHT_REFRESH_HOURS = 24L
    const val PATTERN_WINDOW_DAYS = 28L
    const val PATTERN_MIN_WRONG = 3
    const val PATTERN_REFRESH_WRONG = 3
    const val PATTERN_INPUT_LIMIT = 8
    const val INSIGHT_KEEP_WEEKS = 26L
    const val HISTORY_WEEKS = 52L
    const val STALE_INSIGHT_DAYS = 10
    const val DIGEST_STALLED_LIMIT = 3
}
