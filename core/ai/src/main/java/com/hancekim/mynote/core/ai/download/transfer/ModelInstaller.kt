package com.hancekim.mynote.core.ai.download.transfer

import com.hancekim.mynote.core.ai.download.decision.DownloadStep
import com.hancekim.mynote.core.ai.download.decision.ExpectedModel
import com.hancekim.mynote.core.ai.download.decision.LocalState
import com.hancekim.mynote.core.ai.download.decision.Verdict
import com.hancekim.mynote.core.ai.download.decision.enoughSpace
import com.hancekim.mynote.core.ai.download.decision.nextStep
import com.hancekim.mynote.core.domain.model.ModelFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

internal sealed interface InstallResult {
    data object Installed : InstallResult

    data object Retry : InstallResult

    data class Failed(val reason: String) : InstallResult
}

internal class ModelInstaller(
    private val downloader: ModelDownloader,
    private val files: ModelFiles,
    private val expected: ExpectedModel,
    private val allocatableBytes: () -> Long = { Long.MAX_VALUE },
) {
    suspend fun install(
        url: String,
        onVerifying: () -> Unit = {},
        onProgress: (Long, Long) -> Unit,
    ): InstallResult {
        if (!enoughSpace(allocatableBytes(), expected.totalBytes, files.partBytes())) {
            return InstallResult.Failed(REASON_NO_SPACE)
        }

        repeat(MAX_ROUNDS) {
            when (val step = nextStep(localState(), expected.totalBytes)) {
                DownloadStep.VerifyInstalled -> {
                    onVerifying()
                    if (verified(files.target)) return InstallResult.Installed
                    files.target.delete()
                }

                DownloadStep.VerifyPart -> {
                    onVerifying()
                    if (verified(files.part)) {
                        currentCoroutineContext().ensureActive()
                        files.promote()
                        return InstallResult.Installed
                    }
                    files.discardPartial()
                }

                is DownloadStep.Fetch -> {
                    if (step.from == 0L) files.discardPartial()
                    val before = files.partBytes()
                    val verdict =
                        downloader.fetchInto(url, files.part, step.from) {
                            onProgress(it, expected.totalBytes)
                        }
                    when (verdict) {
                        is Verdict.Append -> {
                            if (files.partBytes() <= before) return InstallResult.Failed(REASON_NO_PROGRESS)
                        }

                        Verdict.StartOver -> {
                            files.discardPartial()
                        }

                        Verdict.Complete -> {
                            Unit
                        }

                        Verdict.RetryLater -> {
                            return InstallResult.Retry
                        }

                        is Verdict.GiveUp -> {
                            return InstallResult.Failed(verdict.reason)
                        }
                    }
                }
            }
        }
        return InstallResult.Failed(REASON_TOO_MANY_ROUNDS)
    }

    private suspend fun verified(file: File): Boolean =
        withContext(Dispatchers.IO) {
            files.matches(file, expected) { ensureActive() }
        }

    private fun localState(): LocalState =
        LocalState(
            installedBytes = files.target.takeIf { it.isFile }?.length(),
            partBytes = files.partBytes(),
        )

    internal companion object {
        const val MAX_ROUNDS = 8
        const val REASON_NO_SPACE = ModelFailure.NO_SPACE
        const val REASON_NO_PROGRESS = ModelFailure.NO_PROGRESS
        const val REASON_TOO_MANY_ROUNDS = ModelFailure.TOO_MANY_ROUNDS
    }
}
