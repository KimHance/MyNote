package com.hancekim.mynote.core.ai.download

internal sealed interface DownloadStep {
    data object VerifyInstalled : DownloadStep

    data object VerifyPart : DownloadStep

    data class Fetch(val from: Long) : DownloadStep
}

internal data class LocalState(
    val installedBytes: Long? = null,
    val partBytes: Long = 0L,
    val savedPlan: DownloadPlan? = null,
)

internal fun nextStep(local: LocalState, remote: DownloadPlan): DownloadStep =
    when {
        local.installedBytes == remote.totalBytes -> DownloadStep.VerifyInstalled
        local.savedPlan != remote -> DownloadStep.Fetch(0L)
        local.partBytes == remote.totalBytes -> DownloadStep.VerifyPart
        local.partBytes in 1L until remote.totalBytes -> DownloadStep.Fetch(local.partBytes)
        else -> DownloadStep.Fetch(0L)
    }
