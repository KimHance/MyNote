package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightResult

internal fun InsightResult.toColumn(): String =
    when (this) {
        InsightResult.Success -> "success"
        InsightResult.Failure -> "failure"
        InsightResult.Skipped -> "skipped"
    }

internal fun String.toInsightResult(): InsightResult? =
    when (this) {
        "success" -> InsightResult.Success
        "failure" -> InsightResult.Failure
        "skipped" -> InsightResult.Skipped
        else -> null
    }

internal fun InsightFailureKind.toColumn(): String =
    when (this) {
        InsightFailureKind.Parse -> "parse"
        InsightFailureKind.Engine -> "engine"
        InsightFailureKind.Timeout -> "timeout"
    }

internal fun String.toFailureKind(): InsightFailureKind? =
    when (this) {
        "parse" -> InsightFailureKind.Parse
        "engine" -> InsightFailureKind.Engine
        "timeout" -> InsightFailureKind.Timeout
        else -> null
    }
