package com.hancekim.mynote.core.ai.insight

internal object InsightWork {
    const val NAME = "study-insights"
    const val KEY_PHASE = "phase"
    const val PHASE_GENERATING = "generating"
    const val SCHEMA_VERSION = 2
    const val SPEC_TAG = "study-insights-spec-1"
    const val TIMEOUT_MILLIS = 180_000L
    const val BACKOFF_MINUTES = 30L
    const val PERIOD_DAYS = 1L
}
