package com.hancekim.mynote.core.ai.download.decision

import androidx.work.WorkInfo
import com.hancekim.mynote.core.ai.download.DownloadWork
import com.hancekim.mynote.core.ai.download.ModelSource
import com.hancekim.mynote.core.ai.download.transfer.ModelInstaller
import com.hancekim.mynote.core.ai.download.work.ModelDownloadWorker
import com.hancekim.mynote.core.domain.model.ModelStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadFactsTest {
    @Test
    fun `작업이 없고 자리 파일이 있으면 쓸 수 있다`() {
        val facts = DownloadFacts(installedBytes = TOTAL)

        assertEquals(ModelStatus.Ready, statusOf(facts))
    }

    @Test
    fun `작업이 없고 받던 조각만 있으면 멈춘 것이다`() {
        val facts = DownloadFacts(partBytes = HALF)

        assertEquals(ModelStatus.Paused(HALF, TOTAL), statusOf(facts))
    }

    @Test
    fun `작업도 파일도 없으면 없는 것이다`() {
        assertEquals(ModelStatus.Missing, statusOf(DownloadFacts()))
    }

    @Test
    fun `대기 중이고 아직 한 번도 안 돌았으면 Wi-Fi 를 기다린다`() {
        val facts = DownloadFacts(work = WorkFacts(WorkInfo.State.ENQUEUED))

        assertEquals(ModelStatus.WaitingForWifi, statusOf(facts))
    }

    @Test
    fun `막힌 작업도 Wi-Fi 대기로 본다`() {
        val facts = DownloadFacts(work = WorkFacts(WorkInfo.State.BLOCKED))

        assertEquals(ModelStatus.WaitingForWifi, statusOf(facts))
    }

    @Test
    fun `MD-8 재시도 상태는 남은 시간 없이 받은 만큼만 안다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.ENQUEUED, runAttemptCount = 1),
                partBytes = GIGABYTE,
            )

        assertEquals(ModelStatus.Retrying(GIGABYTE, TOTAL), statusOf(facts))
    }

    @Test
    fun `돌고 있는데 진행률이 아직 없으면 준비 중이다`() {
        val facts = DownloadFacts(work = WorkFacts(WorkInfo.State.RUNNING))

        assertEquals(ModelStatus.Preparing, statusOf(facts))
    }

    @Test
    fun `진행률이 올라오면 받는 중이다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.RUNNING, received = HALF, total = TOTAL),
            )

        assertEquals(ModelStatus.Downloading(HALF, TOTAL), statusOf(facts))
    }

    @Test
    fun `검증 단계 신호가 오면 파일을 확인하는 중이다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.RUNNING, phase = DownloadWork.PHASE_VERIFY),
            )

        assertEquals(ModelStatus.Verifying, statusOf(facts))
    }

    @Test
    fun `검증 단계가 남아 있는 진행률보다 우선이다`() {
        val facts =
            DownloadFacts(
                work =
                    WorkFacts(
                        state = WorkInfo.State.RUNNING,
                        received = TOTAL,
                        total = TOTAL,
                        phase = DownloadWork.PHASE_VERIFY,
                    ),
            )

        assertEquals(ModelStatus.Verifying, statusOf(facts))
    }

    @Test
    fun `성공한 작업이고 파일이 자리에 있으면 쓸 수 있다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.SUCCEEDED),
                installedBytes = TOTAL,
            )

        assertEquals(ModelStatus.Ready, statusOf(facts))
    }

    @Test
    fun `성공한 작업이어도 파일을 지웠으면 없는 것이다`() {
        val facts = DownloadFacts(work = WorkFacts(WorkInfo.State.SUCCEEDED))

        assertEquals(ModelStatus.Missing, statusOf(facts))
    }

    @Test
    fun `성공한 작업인데 조각만 남아 있으면 멈춤이다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.SUCCEEDED),
                partBytes = 1_000L,
            )

        assertEquals(ModelStatus.Paused(1_000L, TOTAL), statusOf(facts))
    }

    @Test
    fun `실패한 작업이어도 파일이 자리에 있으면 쓸 수 있다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.FAILED, reason = ModelInstaller.REASON_NO_SPACE),
                installedBytes = TOTAL,
            )

        assertEquals(ModelStatus.Ready, statusOf(facts))
    }

    @Test
    fun `실패한 작업은 이유를 안고 온다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.FAILED, reason = ModelInstaller.REASON_NO_SPACE),
            )

        assertEquals(ModelStatus.Failed(ModelInstaller.REASON_NO_SPACE), statusOf(facts))
    }

    @Test
    fun `실패에 이유가 없으면 모름으로 채운다`() {
        val facts = DownloadFacts(work = WorkFacts(WorkInfo.State.FAILED))

        assertEquals(ModelStatus.Failed(REASON_UNKNOWN), statusOf(facts))
    }

    @Test
    fun `취소했고 받던 조각이 남았으면 멈춘 것이다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.CANCELLED),
                partBytes = HALF,
            )

        assertEquals(ModelStatus.Paused(HALF, TOTAL), statusOf(facts))
    }

    @Test
    fun `취소했고 남은 조각도 없으면 없는 것이다`() {
        val facts = DownloadFacts(work = WorkFacts(WorkInfo.State.CANCELLED))

        assertEquals(ModelStatus.Missing, statusOf(facts))
    }

    @Test
    fun `취소했어도 자리 파일이 있으면 쓸 수 있다`() {
        val facts =
            DownloadFacts(
                work = WorkFacts(WorkInfo.State.CANCELLED),
                installedBytes = TOTAL,
                partBytes = HALF,
            )

        assertEquals(ModelStatus.Ready, statusOf(facts))
    }

    @Test
    fun `MD-4 전체 크기는 plan 을 보지 않는다`() {
        val running = DownloadFacts(work = WorkFacts(WorkInfo.State.RUNNING, received = HALF))
        val resting = DownloadFacts(partBytes = HALF)

        assertEquals(ModelStatus.Downloading(HALF, ModelSource.TOTAL_BYTES), statusOf(running))
        assertEquals(ModelStatus.Paused(HALF, ModelSource.TOTAL_BYTES), statusOf(resting))
    }

    private companion object {
        const val TOTAL = 2_588_147_712L
        const val HALF = 1_090_000_000L
        const val GIGABYTE = 1_000_000_000L
    }
}
