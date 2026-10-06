package com.hancekim.mynote.core.ai.download.decision

import androidx.work.WorkInfo
import com.hancekim.mynote.core.ai.download.DownloadWork
import com.hancekim.mynote.core.ai.download.ModelSource
import com.hancekim.mynote.core.ai.download.work.ModelDownloadWorker
import com.hancekim.mynote.core.domain.model.ModelStatus

internal const val REASON_UNKNOWN = "unknown"

internal data class WorkFacts(
    val state: WorkInfo.State,
    val runAttemptCount: Int = 0,
    val received: Long? = null,
    val total: Long? = null,
    val phase: String? = null,
    val reason: String? = null,
)

internal data class DownloadFacts(
    val work: WorkFacts? = null,
    val installedBytes: Long? = null,
    val partBytes: Long = 0L,
)

internal fun statusOf(facts: DownloadFacts): ModelStatus {
    val work = facts.work ?: return resting(facts)
    return when (work.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> queued(facts, work)
        WorkInfo.State.RUNNING -> running(facts, work)
        WorkInfo.State.SUCCEEDED -> resting(facts)
        WorkInfo.State.FAILED -> failed(facts, work)
        WorkInfo.State.CANCELLED -> resting(facts)
    }
}

private fun failed(facts: DownloadFacts, work: WorkFacts): ModelStatus =
    if (facts.installedBytes != null) {
        ModelStatus.Ready
    } else {
        ModelStatus.Failed(work.reason ?: REASON_UNKNOWN)
    }

private fun resting(facts: DownloadFacts): ModelStatus =
    when {
        facts.installedBytes != null -> ModelStatus.Ready
        facts.partBytes > 0L -> ModelStatus.Paused(facts.partBytes, totalOf(facts, null))
        else -> ModelStatus.Missing
    }

private fun queued(facts: DownloadFacts, work: WorkFacts): ModelStatus =
    when (work.runAttemptCount) {
        0 -> {
            ModelStatus.WaitingForWifi
        }

        else -> {
            ModelStatus.Retrying(
                received = facts.partBytes,
                total = totalOf(facts, work),
            )
        }
    }

private fun running(facts: DownloadFacts, work: WorkFacts): ModelStatus =
    when {
        work.phase == DownloadWork.PHASE_VERIFY -> ModelStatus.Verifying
        work.received == null -> ModelStatus.Preparing
        else -> ModelStatus.Downloading(work.received, totalOf(facts, work))
    }

private fun totalOf(facts: DownloadFacts, work: WorkFacts?): Long =
    work?.total?.takeIf { it > 0L }
        ?: ModelSource.TOTAL_BYTES
