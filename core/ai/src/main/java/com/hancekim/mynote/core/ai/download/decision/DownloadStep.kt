package com.hancekim.mynote.core.ai.download.decision

internal sealed interface DownloadStep {
    data object VerifyInstalled : DownloadStep

    data object VerifyPart : DownloadStep

    data class Fetch(val from: Long) : DownloadStep
}

internal data class LocalState(
    val installedBytes: Long? = null,
    val partBytes: Long = 0L,
)

internal fun nextStep(local: LocalState, totalBytes: Long): DownloadStep =
    when {
        local.installedBytes == totalBytes -> DownloadStep.VerifyInstalled
        local.partBytes == totalBytes -> DownloadStep.VerifyPart
        local.partBytes in 1L until totalBytes -> DownloadStep.Fetch(local.partBytes)
        else -> DownloadStep.Fetch(0L)
    }
