package com.hancekim.mynote.core.ai.download.work

import androidx.work.WorkInfo
import com.hancekim.mynote.core.ai.R
import com.hancekim.mynote.core.ai.download.ModelSource
import com.hancekim.mynote.core.ai.download.decision.DownloadFacts
import com.hancekim.mynote.core.ai.download.decision.WorkFacts
import com.hancekim.mynote.core.ai.download.decision.statusOf
import com.hancekim.mynote.core.ai.download.transfer.ModelInstaller
import com.hancekim.mynote.core.domain.model.ModelStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadNoticesTest {
    @Test
    fun `MD-9 상태마다 알림이 하나로 정해진다`() {
        val table =
            listOf(
                ModelStatus.Missing to null,
                ModelStatus.Paused(RECEIVED, TOTAL) to null,
                ModelStatus.WaitingForWifi to WAITING,
                ModelStatus.Preparing to PREPARING,
                ModelStatus.Downloading(RECEIVED, TOTAL) to DOWNLOADING,
                ModelStatus.Retrying(RECEIVED, TOTAL) to RETRYING,
                ModelStatus.Verifying to VERIFYING,
                ModelStatus.Ready to DONE,
                ModelStatus.Failed("HTTP 404") to FAILED,
                ModelStatus.Failed(ModelInstaller.REASON_NO_SPACE) to
                    FAILED.copy(body = R.string.ai_model_download_no_space),
            )

        table.forEach { (status, row) -> assertEquals("$status", row, noticeOf(status)) }
    }

    @Test
    fun `MD-9 파일 확인 알림에만 취소가 없다`() {
        val withoutCancel =
            STATUSES.filter {
                noticeOf(it)?.ongoing == true &&
                    noticeOf(it)?.action != NoticeAction.CANCEL
            }

        assertEquals(listOf(ModelStatus.Verifying), withoutCancel)
    }

    @Test
    fun `MD-9 하나를 올리면 다른 자리는 비운다`() {
        val board = FakeBoard()

        STATUSES.mapNotNull(::noticeOf).forEach {
            board.post(it)
            assertEquals(listOf(it), board.shown)
        }
    }

    @Test
    fun `MD-9 재시도 막대는 워커와 앱이 같은 비율에서 멈춘다`() {
        val byApp =
            statusOf(
                DownloadFacts(
                    work = WorkFacts(WorkInfo.State.ENQUEUED, runAttemptCount = 1),
                    partBytes = RECEIVED,
                ),
            )
        val byWorker = ModelStatus.Retrying(RECEIVED, ModelSource.TOTAL_BYTES)

        assertEquals(noticeOf(byWorker), noticeOf(byApp))
        assertEquals(NoticeBar.Value(42), noticeOf(byApp)?.bar)
        assertEquals(noticeOf(byWorker), noticeAfterStop(WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY, RECEIVED, TOTAL))
    }

    @Test
    fun `MD-9 앱이 앞으로 와도 대기 · 재시도 알림은 남는다`() {
        val waiting = FakeBoard().feed(NoticeEvent.Started, NoticeEvent.Backgrounded(ModelStatus.WaitingForWifi))
        waiting.feed(NoticeEvent.Observed(ModelStatus.WaitingForWifi))

        val retrying =
            FakeBoard().feed(
                NoticeEvent.Reported(ModelStatus.Retrying(RECEIVED, TOTAL)),
                NoticeEvent.Backgrounded(ModelStatus.Retrying(RECEIVED, TOTAL)),
            )
        retrying.feed(NoticeEvent.Observed(ModelStatus.Retrying(RECEIVED, TOTAL)))

        assertEquals(listOf(WAITING), waiting.shown)
        assertEquals(listOf(RETRYING), retrying.shown)
    }

    @Test
    fun `MD-10 다시 시도하면 실패 알림이 사라진다`() {
        val retried =
            FakeBoard().feed(
                NoticeEvent.Reported(ModelStatus.Downloading(RECEIVED, TOTAL)),
                NoticeEvent.Reported(ModelStatus.Failed("HTTP 404")),
            )
        assertEquals(listOf(FAILED), retried.shown)

        retried.feed(NoticeEvent.Started, NoticeEvent.Observed(ModelStatus.WaitingForWifi))
        val workerStarted =
            FakeBoard().feed(
                NoticeEvent.Reported(ModelStatus.Failed("HTTP 404")),
                NoticeEvent.Reported(ModelStatus.Preparing),
            )

        assertEquals(emptyList<Notice>(), retried.shown)
        assertEquals(listOf(PREPARING), workerStarted.shown)
    }

    @Test
    fun `MD-10 쉬는 중 취소해도 알림이 남지 않는다`() {
        val waiting =
            FakeBoard().feed(
                NoticeEvent.Started,
                NoticeEvent.Observed(ModelStatus.WaitingForWifi),
                NoticeEvent.Backgrounded(ModelStatus.WaitingForWifi),
            )
        val retrying =
            FakeBoard().feed(
                NoticeEvent.Reported(ModelStatus.Downloading(RECEIVED, TOTAL)),
                NoticeEvent.Reported(ModelStatus.Retrying(RECEIVED, TOTAL)),
                NoticeEvent.Observed(ModelStatus.Retrying(RECEIVED, TOTAL)),
            )
        assertEquals(listOf(WAITING), waiting.shown)
        assertEquals(listOf(RETRYING), retrying.shown)

        val byWaitingAction = waiting.copy().feed(NoticeEvent.Observed(ModelStatus.Missing))
        val byRetryingAction = retrying.copy().feed(NoticeEvent.Observed(ModelStatus.Paused(RECEIVED, TOTAL)))
        val byAppOnWaiting = waiting.copy().feed(NoticeEvent.Cancelled)
        val byAppOnRetrying = retrying.copy().feed(NoticeEvent.Cancelled)

        listOf(byWaitingAction, byRetryingAction, byAppOnWaiting, byAppOnRetrying).forEach {
            assertEquals(emptyList<Notice>(), it.shown)
        }
    }

    @Test
    fun `MD-10 받는 중 취소해도 알림이 남지 않는다`() {
        val board =
            FakeBoard().feed(
                NoticeEvent.Reported(ModelStatus.Downloading(RECEIVED, TOTAL)),
                NoticeEvent.Stopped(WorkInfo.STOP_REASON_CANCELLED_BY_APP, RECEIVED, TOTAL),
                NoticeEvent.Observed(ModelStatus.Paused(RECEIVED, TOTAL)),
            )

        assertEquals(emptyList<Notice>(), board.shown)
    }

    @Test
    fun `MD-10 재시도 뒤 성공하면 받았습니다 하나만 남는다`() {
        val board =
            FakeBoard().feed(
                NoticeEvent.Started,
                NoticeEvent.Reported(ModelStatus.Preparing),
                NoticeEvent.Reported(ModelStatus.Downloading(RECEIVED, TOTAL)),
                NoticeEvent.Reported(ModelStatus.Retrying(RECEIVED, TOTAL)),
                NoticeEvent.Observed(ModelStatus.Retrying(RECEIVED, TOTAL)),
                NoticeEvent.Backgrounded(ModelStatus.Retrying(RECEIVED, TOTAL)),
                NoticeEvent.Reported(ModelStatus.Preparing),
                NoticeEvent.Reported(ModelStatus.Downloading(TOTAL, TOTAL)),
                NoticeEvent.Reported(ModelStatus.Verifying),
                NoticeEvent.Reported(ModelStatus.Ready),
                NoticeEvent.Observed(ModelStatus.Ready),
            )

        assertEquals(listOf(DONE), board.shown)
    }

    @Test
    fun `MD-10 모델을 지우면 결과 알림도 사라진다`() {
        val done = FakeBoard().feed(NoticeEvent.Reported(ModelStatus.Ready), NoticeEvent.Observed(ModelStatus.Ready))
        assertEquals(listOf(DONE), done.shown)

        val deleted = done.copy().feed(NoticeEvent.Deleted)
        val observedMissing = done.copy().feed(NoticeEvent.Observed(ModelStatus.Missing))

        assertEquals(emptyList<Notice>(), deleted.shown)
        assertEquals(emptyList<Notice>(), observedMissing.shown)
    }

    @Test
    fun `MD-11 사용자가 멈춘 게 아니면 재시도 알림을 남긴다`() {
        val notByUser =
            listOf(
                WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY,
                WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW,
                WorkInfo.STOP_REASON_PREEMPT,
                WorkInfo.STOP_REASON_TIMEOUT,
                WorkInfo.STOP_REASON_DEVICE_STATE,
                WorkInfo.STOP_REASON_QUOTA,
                WorkInfo.STOP_REASON_SYSTEM_PROCESSING,
                WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT,
                WorkInfo.STOP_REASON_UNKNOWN,
            )

        notByUser.forEach { reason ->
            val board =
                FakeBoard().feed(
                    NoticeEvent.Reported(ModelStatus.Downloading(RECEIVED, TOTAL)),
                    NoticeEvent.Stopped(reason, RECEIVED, TOTAL),
                )
            assertEquals("$reason", listOf(RETRYING), board.shown)
        }
        assertEquals(null, noticeAfterStop(WorkInfo.STOP_REASON_CANCELLED_BY_APP, RECEIVED, TOTAL))
    }

    @Test
    fun `MD-11 앱이 뒤로 가면 쉬는 상태의 알림을 올린다`() {
        assertEquals(listOf(WAITING), FakeBoard().feed(NoticeEvent.Backgrounded(ModelStatus.WaitingForWifi)).shown)
        assertEquals(
            listOf(RETRYING),
            FakeBoard().feed(NoticeEvent.Backgrounded(ModelStatus.Retrying(RECEIVED, TOTAL))).shown,
        )

        STATUSES
            .filterNot { it == ModelStatus.WaitingForWifi || it is ModelStatus.Retrying }
            .forEach { status ->
                val before = FakeBoard().feed(NoticeEvent.Reported(status))
                val after = before.copy().feed(NoticeEvent.Backgrounded(status))
                assertEquals("$status", before.shown, after.shown)
            }
    }

    private class FakeBoard : NoticeBoard {
        private val slots = linkedMapOf<NoticeSlot, Notice>()

        val shown: List<Notice> get() = slots.values.toList()

        override fun show(notice: Notice) {
            slots[notice.slot] = notice
        }

        override fun hide(slot: NoticeSlot) {
            slots.remove(slot)
        }

        fun feed(vararg events: NoticeEvent): FakeBoard = apply { events.forEach { on(it) } }

        fun copy(): FakeBoard = FakeBoard().also { it.slots.putAll(slots) }
    }

    private companion object {
        const val RECEIVED = 1_090_000_000L
        const val TOTAL = 2_588_147_712L

        val STATUSES =
            listOf(
                ModelStatus.Missing,
                ModelStatus.Paused(RECEIVED, TOTAL),
                ModelStatus.WaitingForWifi,
                ModelStatus.Preparing,
                ModelStatus.Downloading(RECEIVED, TOTAL),
                ModelStatus.Retrying(RECEIVED, TOTAL),
                ModelStatus.Verifying,
                ModelStatus.Ready,
                ModelStatus.Failed("HTTP 404"),
            )

        val WAITING =
            Notice(
                slot = NoticeSlot.PROGRESS,
                title = R.string.ai_model_download_title,
                body = R.string.ai_model_download_waiting_wifi,
                bar = NoticeBar.Moving,
                action = NoticeAction.CANCEL,
                icon = android.R.drawable.stat_sys_download,
                ongoing = true,
            )
        val PREPARING = WAITING.copy(body = R.string.ai_model_download_preparing)
        val DOWNLOADING =
            WAITING.copy(
                body = R.string.ai_model_download_progress_bytes,
                bodyArgs = listOf(42, "1.09", "2.59"),
                bar = NoticeBar.Value(42),
            )
        val RETRYING =
            Notice(
                slot = NoticeSlot.RETRY,
                title = R.string.ai_model_download_title,
                body = R.string.ai_model_download_retrying,
                bar = NoticeBar.Value(42),
                action = NoticeAction.CANCEL,
                icon = R.drawable.ai_stat_download_retrying,
                ongoing = true,
            )
        val VERIFYING = WAITING.copy(body = R.string.ai_model_download_verifying, action = null)
        val DONE =
            Notice(
                slot = NoticeSlot.RESULT,
                title = R.string.ai_model_download_done,
                body = R.string.ai_model_download_done_body,
                bar = NoticeBar.None,
                action = null,
                icon = android.R.drawable.stat_sys_download_done,
                ongoing = false,
            )
        val FAILED =
            Notice(
                slot = NoticeSlot.RESULT,
                title = R.string.ai_model_download_failed,
                body = R.string.ai_model_download_failed_body,
                bar = NoticeBar.None,
                action = NoticeAction.RETRY,
                icon = android.R.drawable.stat_notify_error,
                ongoing = false,
                expanded = true,
            )
    }
}
