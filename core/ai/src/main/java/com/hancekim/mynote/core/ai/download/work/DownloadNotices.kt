package com.hancekim.mynote.core.ai.download.work

import androidx.work.WorkInfo
import com.hancekim.mynote.core.ai.R
import com.hancekim.mynote.core.ai.download.decision.gigabytesOf
import com.hancekim.mynote.core.ai.download.decision.percentOf
import com.hancekim.mynote.core.ai.download.transfer.ModelInstaller
import com.hancekim.mynote.core.domain.model.ModelStatus

internal enum class NoticeSlot(val id: Int) {
    PROGRESS(4201),
    RESULT(4202),
    RETRY(4203),
}

internal sealed interface NoticeBar {
    data object None : NoticeBar

    data object Moving : NoticeBar

    data class Value(val percent: Int) : NoticeBar
}

internal enum class NoticeAction {
    CANCEL,
    RETRY,
}

internal data class Notice(
    val slot: NoticeSlot,
    val title: Int,
    val body: Int,
    val bodyArgs: List<Any> = emptyList(),
    val bar: NoticeBar,
    val action: NoticeAction?,
    val icon: Int,
    val ongoing: Boolean,
    val expanded: Boolean = false,
)

internal sealed interface NoticeEvent {
    data object Started : NoticeEvent

    data object Launched : NoticeEvent

    data object Cancelled : NoticeEvent

    data object Deleted : NoticeEvent

    data class Reported(val status: ModelStatus) : NoticeEvent

    data class Stopped(val reason: Int, val received: Long, val total: Long) : NoticeEvent

    data class Observed(val status: ModelStatus) : NoticeEvent

    data class Backgrounded(val status: ModelStatus) : NoticeEvent
}

internal interface NoticeBoard {
    fun show(notice: Notice)

    fun hide(slot: NoticeSlot)
}

internal fun NoticeBoard.on(event: NoticeEvent) {
    when (event) {
        NoticeEvent.Started -> hide(NoticeSlot.RESULT)
        NoticeEvent.Launched -> clear(NoticeSlot.entries - NoticeSlot.PROGRESS)
        NoticeEvent.Cancelled, NoticeEvent.Deleted -> clear(NoticeSlot.entries)
        is NoticeEvent.Reported -> leave(noticeOf(event.status))
        is NoticeEvent.Stopped -> leave(noticeAfterStop(event.reason, event.received, event.total))
        is NoticeEvent.Observed -> clear(emptySlotsOf(event.status))
        is NoticeEvent.Backgrounded -> noticeOnBackground(event.status)?.let(::post)
    }
}

internal fun NoticeBoard.post(notice: Notice) {
    clear(NoticeSlot.entries - notice.slot)
    show(notice)
}

private fun NoticeBoard.leave(notice: Notice?) {
    if (notice == null) clear(NoticeSlot.entries) else post(notice)
}

private fun NoticeBoard.clear(slots: Collection<NoticeSlot>) = slots.forEach(::hide)

internal fun noticeOf(status: ModelStatus): Notice? =
    when (status) {
        ModelStatus.Missing, is ModelStatus.Paused -> {
            null
        }

        ModelStatus.WaitingForWifi -> {
            working(R.string.ai_model_download_waiting_wifi)
        }

        ModelStatus.Preparing -> {
            working(R.string.ai_model_download_preparing)
        }

        is ModelStatus.Downloading -> {
            val percent = percentOf(status.received, status.total)
            working(
                body = R.string.ai_model_download_progress_bytes,
                bodyArgs = listOf(percent, gigabytesOf(status.received), gigabytesOf(status.total)),
                bar = NoticeBar.Value(percent),
            )
        }

        is ModelStatus.Retrying -> {
            Notice(
                slot = NoticeSlot.RETRY,
                title = R.string.ai_model_download_title,
                body = R.string.ai_model_download_retrying,
                bar = NoticeBar.Value(percentOf(status.received, status.total)),
                action = NoticeAction.CANCEL,
                icon = R.drawable.ai_stat_download_retrying,
                ongoing = true,
            )
        }

        ModelStatus.Verifying -> {
            working(R.string.ai_model_download_verifying, action = null)
        }

        ModelStatus.Ready -> {
            Notice(
                slot = NoticeSlot.RESULT,
                title = R.string.ai_model_download_done,
                body = R.string.ai_model_download_done_body,
                bar = NoticeBar.None,
                action = null,
                icon = android.R.drawable.stat_sys_download_done,
                ongoing = false,
            )
        }

        is ModelStatus.Failed -> {
            Notice(
                slot = NoticeSlot.RESULT,
                title = R.string.ai_model_download_failed,
                body = failureBodyRes(status.reason),
                bar = NoticeBar.None,
                action = NoticeAction.RETRY,
                icon = android.R.drawable.stat_notify_error,
                ongoing = false,
                expanded = true,
            )
        }
    }

internal fun emptySlotsOf(status: ModelStatus): Set<NoticeSlot> =
    when (status) {
        ModelStatus.Missing, is ModelStatus.Paused -> {
            NoticeSlot.entries.toSet()
        }

        ModelStatus.WaitingForWifi,
        ModelStatus.Preparing,
        is ModelStatus.Downloading,
        is ModelStatus.Retrying,
        ModelStatus.Verifying,
        ModelStatus.Ready,
        is ModelStatus.Failed,
        -> {
            emptySet()
        }
    }

internal fun noticeAfterStop(reason: Int, received: Long, total: Long): Notice? =
    when (reason) {
        WorkInfo.STOP_REASON_CANCELLED_BY_APP -> null
        else -> noticeOf(ModelStatus.Retrying(received, total))
    }

internal fun noticeOnBackground(status: ModelStatus): Notice? =
    when (status) {
        ModelStatus.WaitingForWifi, is ModelStatus.Retrying -> {
            noticeOf(status)
        }

        ModelStatus.Missing,
        is ModelStatus.Paused,
        ModelStatus.Preparing,
        is ModelStatus.Downloading,
        ModelStatus.Verifying,
        ModelStatus.Ready,
        is ModelStatus.Failed,
        -> {
            null
        }
    }

internal fun failureBodyRes(reason: String): Int =
    when (reason) {
        ModelInstaller.REASON_NO_SPACE -> R.string.ai_model_download_no_space
        else -> R.string.ai_model_download_failed_body
    }

private fun working(
    body: Int,
    bodyArgs: List<Any> = emptyList(),
    bar: NoticeBar = NoticeBar.Moving,
    action: NoticeAction? = NoticeAction.CANCEL,
): Notice =
    Notice(
        slot = NoticeSlot.PROGRESS,
        title = R.string.ai_model_download_title,
        body = body,
        bodyArgs = bodyArgs,
        bar = bar,
        action = action,
        icon = android.R.drawable.stat_sys_download,
        ongoing = true,
    )
