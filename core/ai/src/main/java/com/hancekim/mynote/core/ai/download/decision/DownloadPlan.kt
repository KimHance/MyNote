package com.hancekim.mynote.core.ai.download.decision

import kotlinx.serialization.Serializable

@Serializable
internal data class DownloadPlan(
    val commit: String,
    val sha256: String,
    val totalBytes: Long,
)
