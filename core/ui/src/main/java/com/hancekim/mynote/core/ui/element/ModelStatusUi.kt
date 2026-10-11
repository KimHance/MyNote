package com.hancekim.mynote.core.ui.element

import androidx.annotation.StringRes
import com.hancekim.mynote.core.domain.model.ModelFailure
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.ui.R

enum class ModelTone { Neutral, Accent, Warn, Wrong, Correct }

enum class ModelAction { Download, Resume, Retry, Cancel, DiscardPartial, Delete }

sealed interface ModelProgressUi {
    data object None : ModelProgressUi

    data class Indeterminate(val tone: ModelTone) : ModelProgressUi

    data class Determinate(
        val received: Long,
        val total: Long,
        val tone: ModelTone,
    ) : ModelProgressUi {
        val fraction: Float get() = if (total > 0L) (received.toFloat() / total).coerceIn(0f, 1f) else 0f

        val percent: Int get() = (fraction * 100).toInt()
    }
}

data class ModelCardUi(
    val tone: ModelTone,
    @param:StringRes val label: Int,
    @param:StringRes val detail: Int?,
    val detailArg: String?,
    val progress: ModelProgressUi,
    val actions: List<ModelAction>,
    @param:StringRes val help: Int,
    val notificationHint: Boolean,
)

fun modelCardUi(status: ModelStatus): ModelCardUi =
    when (status) {
        ModelStatus.Missing -> {
            ModelCardUi(
                tone = ModelTone.Neutral,
                label = R.string.ui_model_missing,
                detail = R.string.ui_model_missing_desc,
                detailArg = null,
                progress = ModelProgressUi.None,
                actions = listOf(ModelAction.Download),
                help = R.string.ui_model_help,
                notificationHint = false,
            )
        }

        ModelStatus.WaitingForWifi -> {
            ModelCardUi(
                tone = ModelTone.Warn,
                label = R.string.ui_model_waiting,
                detail = R.string.ui_model_waiting_desc,
                detailArg = null,
                progress = ModelProgressUi.Determinate(0L, 1L, ModelTone.Warn),
                actions = listOf(ModelAction.Cancel),
                help = R.string.ui_model_help,
                notificationHint = true,
            )
        }

        ModelStatus.Preparing -> {
            ModelCardUi(
                tone = ModelTone.Accent,
                label = R.string.ui_model_preparing,
                detail = null,
                detailArg = null,
                progress = ModelProgressUi.Indeterminate(ModelTone.Accent),
                actions = listOf(ModelAction.Cancel),
                help = R.string.ui_model_help,
                notificationHint = true,
            )
        }

        is ModelStatus.Downloading -> {
            ModelCardUi(
                tone = ModelTone.Accent,
                label = R.string.ui_model_downloading,
                detail = null,
                detailArg = null,
                progress = ModelProgressUi.Determinate(status.received, status.total, ModelTone.Accent),
                actions = listOf(ModelAction.Cancel),
                help = R.string.ui_model_help,
                notificationHint = true,
            )
        }

        is ModelStatus.Retrying -> {
            ModelCardUi(
                tone = ModelTone.Warn,
                label = R.string.ui_model_retrying,
                detail = R.string.ui_model_retrying_desc,
                detailArg = null,
                progress = ModelProgressUi.Determinate(status.received, status.total, ModelTone.Warn),
                actions = listOf(ModelAction.Cancel),
                help = R.string.ui_model_help,
                notificationHint = true,
            )
        }

        ModelStatus.Verifying -> {
            ModelCardUi(
                tone = ModelTone.Accent,
                label = R.string.ui_model_verifying,
                detail = null,
                detailArg = null,
                progress = ModelProgressUi.Indeterminate(ModelTone.Accent),
                actions = listOf(ModelAction.Cancel),
                help = R.string.ui_model_help,
                notificationHint = true,
            )
        }

        is ModelStatus.Paused -> {
            ModelCardUi(
                tone = ModelTone.Neutral,
                label = R.string.ui_model_paused,
                detail = R.string.ui_model_paused_desc,
                detailArg = gigabytes(status.received),
                progress = ModelProgressUi.Determinate(status.received, status.total, ModelTone.Neutral),
                actions = listOf(ModelAction.Resume, ModelAction.DiscardPartial),
                help = R.string.ui_model_help,
                notificationHint = false,
            )
        }

        ModelStatus.Ready -> {
            ModelCardUi(
                tone = ModelTone.Correct,
                label = R.string.ui_model_ready,
                detail = R.string.ui_model_ready_desc,
                detailArg = null,
                progress = ModelProgressUi.None,
                actions = listOf(ModelAction.Delete),
                help = R.string.ui_model_help_ready,
                notificationHint = false,
            )
        }

        is ModelStatus.Failed -> {
            ModelCardUi(
                tone = ModelTone.Wrong,
                label = R.string.ui_model_failed,
                detail =
                    if (status.isNoSpace) {
                        R.string.ui_model_failed_space
                    } else {
                        R.string.ui_model_failed_generic
                    },
                detailArg = null,
                progress = ModelProgressUi.None,
                actions = listOf(ModelAction.Retry),
                help = if (status.isNoSpace) R.string.ui_model_help_space else R.string.ui_model_help,
                notificationHint = false,
            )
        }
    }

enum class PillIcon { Check, Warn }

data class ModelPillUi(
    @param:StringRes val label: Int,
    val tone: ModelTone,
    val percent: Int?,
    val progress: Float?,
    val icon: PillIcon?,
)

fun modelPillUi(status: ModelStatus): ModelPillUi =
    when (status) {
        ModelStatus.WaitingForWifi -> {
            ModelPillUi(
                label = R.string.ui_pill_waiting,
                tone = ModelTone.Warn,
                percent = null,
                progress = null,
                icon = null,
            )
        }

        is ModelStatus.Retrying -> {
            ModelPillUi(
                label = R.string.ui_pill_retrying,
                tone = ModelTone.Warn,
                percent = null,
                progress = status.fractionOrNull(),
                icon = null,
            )
        }

        is ModelStatus.Downloading -> {
            val fraction = status.fractionOrNull() ?: 0f
            ModelPillUi(
                label = R.string.ui_pill_downloading,
                tone = ModelTone.Accent,
                percent = (fraction * 100).toInt(),
                progress = fraction,
                icon = null,
            )
        }

        ModelStatus.Ready -> {
            ModelPillUi(
                label = R.string.ui_pill_ready,
                tone = ModelTone.Correct,
                percent = null,
                progress = null,
                icon = PillIcon.Check,
            )
        }

        is ModelStatus.Failed -> {
            ModelPillUi(
                label = R.string.ui_pill_failed,
                tone = ModelTone.Wrong,
                percent = null,
                progress = null,
                icon = PillIcon.Warn,
            )
        }

        else -> {
            ModelPillUi(
                label = R.string.ui_pill_downloading,
                tone = ModelTone.Accent,
                percent = null,
                progress = status.fractionOrNull(),
                icon = null,
            )
        }
    }

enum class CtaAction { OpenSetup, ConfirmDownload, OpenSettings, StartDownload }

data class QuizCtaUi(
    @param:StringRes val subtitle: Int,
    val subtitleArg: String?,
    @param:StringRes val chip: Int?,
    val action: CtaAction,
    val shine: Boolean,
    val warn: Boolean,
)

fun quizCtaUi(status: ModelStatus): QuizCtaUi =
    when (status) {
        ModelStatus.Ready -> {
            QuizCtaUi(
                subtitle = R.string.ui_quiz_cta_subtitle,
                subtitleArg = null,
                chip = null,
                action = CtaAction.OpenSetup,
                shine = true,
                warn = false,
            )
        }

        ModelStatus.Missing -> {
            QuizCtaUi(
                subtitle = R.string.ui_quiz_cta_need_model,
                subtitleArg = null,
                chip = R.string.ui_model_download,
                action = CtaAction.ConfirmDownload,
                shine = false,
                warn = false,
            )
        }

        is ModelStatus.Paused -> {
            QuizCtaUi(
                subtitle = R.string.ui_quiz_cta_paused,
                subtitleArg = gigabytes(status.received),
                chip = R.string.ui_model_resume,
                action = CtaAction.StartDownload,
                shine = false,
                warn = false,
            )
        }

        is ModelStatus.Failed -> {
            QuizCtaUi(
                subtitle = R.string.ui_pill_failed,
                subtitleArg = null,
                chip = R.string.ui_model_retry,
                action = CtaAction.StartDownload,
                shine = false,
                warn = true,
            )
        }

        else -> {
            QuizCtaUi(
                subtitle = R.string.ui_quiz_cta_downloading,
                subtitleArg = null,
                chip = null,
                action = CtaAction.OpenSettings,
                shine = false,
                warn = false,
            )
        }
    }

fun ModelStatus.fractionOrNull(): Float? =
    when (this) {
        is ModelStatus.Downloading -> if (total > 0L) (received.toFloat() / total).coerceIn(0f, 1f) else 0f
        is ModelStatus.Retrying -> if (total > 0L) (received.toFloat() / total).coerceIn(0f, 1f) else 0f
        is ModelStatus.Paused -> if (total > 0L) (received.toFloat() / total).coerceIn(0f, 1f) else 0f
        else -> null
    }

val ModelStatus.isBusy: Boolean
    get() =
        this is ModelStatus.WaitingForWifi ||
            this is ModelStatus.Preparing ||
            this is ModelStatus.Downloading ||
            this is ModelStatus.Retrying ||
            this is ModelStatus.Verifying

val ModelStatus.Failed.isNoSpace: Boolean get() = reason == ModelFailure.NO_SPACE

fun gigabytes(bytes: Long): String = String.format(java.util.Locale.US, "%.2f", bytes / BYTES_PER_GB)

private const val BYTES_PER_GB = 1_000_000_000.0
