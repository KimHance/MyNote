package com.hancekim.mynote.core.ai.download

import kotlinx.serialization.Serializable

@Serializable
internal data class DownloadPlan(
    val commit: String,
    val sha256: String,
    val totalBytes: Long,
)
