package com.hancekim.mynote.feature.notes.element

import androidx.annotation.StringRes
import com.hancekim.mynote.core.domain.model.ModelFailure
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.feature.notes.R

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
)

fun modelCardUi(status: ModelStatus): ModelCardUi =
    when (status) {
        ModelStatus.Missing -> {
            ModelCardUi(
                tone = ModelTone.Neutral,
                label = R.string.notes_model_missing,
                detail = R.string.notes_model_missing_desc,
                detailArg = null,
                progress = ModelProgressUi.None,
                actions = listOf(ModelAction.Download),
                help = R.string.notes_model_help,
            )
        }

        ModelStatus.WaitingForWifi -> {
            ModelCardUi(
                tone = ModelTone.Warn,
                label = R.string.notes_model_waiting,
                detail = R.string.notes_model_waiting_desc,
                detailArg = null,
                progress = ModelProgressUi.Determinate(0L, 1L, ModelTone.Warn),
                actions = listOf(ModelAction.Cancel),
                help = R.string.notes_model_help,
            )
        }

        ModelStatus.Preparing -> {
            ModelCardUi(
                tone = ModelTone.Accent,
                label = R.string.notes_model_preparing,
                detail = null,
                detailArg = null,
                progress = ModelProgressUi.Indeterminate(ModelTone.Accent),
                actions = listOf(ModelAction.Cancel),
                help = R.string.notes_model_help,
            )
        }

        is ModelStatus.Downloading -> {
            ModelCardUi(
                tone = ModelTone.Accent,
                label = R.string.notes_model_downloading,
                detail = null,
                detailArg = null,
                progress = ModelProgressUi.Determinate(status.received, status.total, ModelTone.Accent),
                actions = listOf(ModelAction.Cancel),
                help = R.string.notes_model_help,
            )
        }

        is ModelStatus.Retrying -> {
            ModelCardUi(
                tone = ModelTone.Warn,
                label = R.string.notes_model_retrying,
                detail = status.inSeconds?.let { R.string.notes_model_retrying_desc },
                detailArg = status.inSeconds?.toString(),
                progress = ModelProgressUi.Determinate(status.received, status.total, ModelTone.Warn),
                actions = listOf(ModelAction.Cancel),
                help = R.string.notes_model_help,
            )
        }

        ModelStatus.Verifying -> {
            ModelCardUi(
                tone = ModelTone.Accent,
                label = R.string.notes_model_verifying,
                detail = null,
                detailArg = null,
                progress = ModelProgressUi.Indeterminate(ModelTone.Accent),
                actions = listOf(ModelAction.Cancel),
                help = R.string.notes_model_help,
            )
        }

        is ModelStatus.Paused -> {
            ModelCardUi(
                tone = ModelTone.Neutral,
                label = R.string.notes_model_paused,
                detail = R.string.notes_model_paused_desc,
                detailArg = gigabytes(status.received),
                progress = ModelProgressUi.Determinate(status.received, status.total, ModelTone.Neutral),
                actions = listOf(ModelAction.Resume, ModelAction.DiscardPartial),
                help = R.string.notes_model_help,
            )
        }

        ModelStatus.Ready -> {
            ModelCardUi(
                tone = ModelTone.Correct,
                label = R.string.notes_model_ready,
                detail = R.string.notes_model_ready_desc,
                detailArg = null,
                progress = ModelProgressUi.None,
                actions = listOf(ModelAction.Delete),
                help = R.string.notes_model_help_ready,
            )
        }

        is ModelStatus.Failed -> {
            ModelCardUi(
                tone = ModelTone.Wrong,
                label = R.string.notes_model_failed,
                detail =
                    if (status.isNoSpace) {
                        R.string.notes_model_failed_space
                    } else {
                        R.string.notes_model_failed_generic
                    },
                detailArg = null,
                progress = ModelProgressUi.None,
                actions = listOf(ModelAction.Retry),
                help = if (status.isNoSpace) R.string.notes_model_help_space else R.string.notes_model_help,
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
                subtitle = R.string.notes_quiz_cta_subtitle,
                subtitleArg = null,
                chip = null,
                action = CtaAction.OpenSetup,
                shine = true,
                warn = false,
            )
        }

        ModelStatus.Missing -> {
            QuizCtaUi(
                subtitle = R.string.notes_quiz_cta_need_model,
                subtitleArg = null,
                chip = R.string.notes_model_download,
                action = CtaAction.ConfirmDownload,
                shine = false,
                warn = false,
            )
        }

        is ModelStatus.Paused -> {
            QuizCtaUi(
                subtitle = R.string.notes_quiz_cta_paused,
                subtitleArg = gigabytes(status.received),
                chip = R.string.notes_model_resume,
                action = CtaAction.StartDownload,
                shine = false,
                warn = false,
            )
        }

        is ModelStatus.Failed -> {
            QuizCtaUi(
                subtitle = R.string.notes_pill_failed,
                subtitleArg = null,
                chip = R.string.notes_model_retry,
                action = CtaAction.StartDownload,
                shine = false,
                warn = true,
            )
        }

        else -> {
            QuizCtaUi(
                subtitle = R.string.notes_quiz_cta_downloading,
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
