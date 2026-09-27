package com.hancekim.mynote.core.ai.download

internal sealed interface InstallResult {
    data object Installed : InstallResult

    data class Retry(val afterSeconds: Long?) : InstallResult

    data class Failed(val reason: String) : InstallResult
}

internal class ModelInstaller(
    private val downloader: ModelDownloader,
    private val files: ModelFiles,
    private val allocatableBytes: () -> Long = { Long.MAX_VALUE },
) {
    suspend fun install(url: String, onProgress: (Long, Long) -> Unit): InstallResult {
        val remote = downloader.plan(url) ?: return InstallResult.Failed(REASON_NO_PLAN)
        if (!enoughSpace(allocatableBytes(), remote.totalBytes, files.partBytes())) {
            return InstallResult.Failed(REASON_NO_SPACE)
        }

        repeat(MAX_ROUNDS) {
            when (val step = nextStep(localState(), remote)) {
                DownloadStep.VerifyInstalled -> {
                    if (files.matches(files.target, remote)) return InstallResult.Installed
                    files.target.delete()
                }

                DownloadStep.VerifyPart -> {
                    if (files.matches(files.part, remote)) {
                        files.promote()
                        return InstallResult.Installed
                    }
                    files.discardPart()
                }

                is DownloadStep.Fetch -> {
                    if (step.from == 0L) files.discardPart()
                    files.savePlan(remote)
                    val before = files.partBytes()
                    val verdict =
                        downloader.fetchInto(url, files.part, step.from) {
                            onProgress(it, remote.totalBytes)
                        }
                    when (verdict) {
                        is Verdict.Append -> {
                            if (files.partBytes() <= before) return InstallResult.Failed(REASON_NO_PROGRESS)
                        }

                        Verdict.StartOver -> {
                            files.discardPart()
                        }

                        Verdict.Complete -> {
                            Unit
                        }

                        is Verdict.RetryLater -> {
                            return InstallResult.Retry(verdict.afterSeconds)
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

    private fun localState(): LocalState =
        LocalState(
            installedBytes = files.target.takeIf { it.isFile }?.length(),
            partBytes = files.partBytes(),
            savedPlan = files.savedPlan(),
        )

    internal companion object {
        const val MAX_ROUNDS = 8
        const val REASON_NO_PLAN = "no-plan"
        const val REASON_NO_SPACE = "no-space"
        const val REASON_NO_PROGRESS = "no-progress"
        const val REASON_TOO_MANY_ROUNDS = "too-many-rounds"
    }
}
