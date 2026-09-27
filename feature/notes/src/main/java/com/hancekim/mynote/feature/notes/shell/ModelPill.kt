package com.hancekim.mynote.feature.notes.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.FloatingStatusPill
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.fractionOrNull
import com.hancekim.mynote.feature.notes.element.isBusy
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal const val MODEL_PILL_RESERVE_DP = 54

@Composable
internal fun rememberModelPillVisible(
    status: ModelStatus,
    settingsOpen: Boolean,
): Boolean {
    var readyRecently by remember { mutableStateOf(false) }
    var failureSeen by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == ModelStatus.Ready) {
            readyRecently = true
            delay(READY_VISIBLE_MILLIS.milliseconds)
            readyRecently = false
        }
    }
    LaunchedEffect(status, settingsOpen) {
        if (status is ModelStatus.Failed && settingsOpen) failureSeen = true
        if (status !is ModelStatus.Failed) failureSeen = false
    }

    return !settingsOpen &&
        (status.isBusy || readyRecently || (status is ModelStatus.Failed && !failureSeen))
}

@Composable
internal fun BoxScope.ModelPill(
    status: ModelStatus,
    visible: Boolean,
    hidden: Boolean,
    topOffset: androidx.compose.ui.unit.Dp,
    backdrop: Backdrop,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    AnimatedVisibility(
        visible = visible && !hidden,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = topOffset + 10.dp),
    ) {
        FloatingStatusPill(
            label = stringResource(status.pillLabel()),
            tone = status.pillTone(colors.accent, colors.warn, colors.correct, colors.wrong),
            trailing = status.pillTrailing(),
            progress = status.fractionOrNull(),
            icon = status.pillIcon(),
            backdrop = backdrop,
            onClick = onClick,
        )
    }
}

private fun ModelStatus.pillLabel(): Int =
    when (this) {
        ModelStatus.WaitingForWifi -> R.string.notes_pill_waiting
        is ModelStatus.Retrying -> R.string.notes_pill_retrying
        ModelStatus.Ready -> R.string.notes_pill_ready
        is ModelStatus.Failed -> R.string.notes_pill_failed
        else -> R.string.notes_pill_downloading
    }

private fun ModelStatus.pillTone(
    accent: Color,
    warn: Color,
    correct: Color,
    wrong: Color,
): Color =
    when (this) {
        ModelStatus.WaitingForWifi, is ModelStatus.Retrying -> warn
        ModelStatus.Ready -> correct
        is ModelStatus.Failed -> wrong
        else -> accent
    }

@Composable
private fun ModelStatus.pillTrailing(): String? =
    when (this) {
        is ModelStatus.Downloading -> {
            stringResource(R.string.notes_model_percent, ((fractionOrNull() ?: 0f) * 100).toInt())
        }

        is ModelStatus.Retrying -> {
            inSeconds?.let { stringResource(R.string.notes_pill_seconds, it) }
        }

        else -> {
            null
        }
    }

private fun ModelStatus.pillIcon(): ImageVector? =
    when (this) {
        ModelStatus.Ready -> MyNoteIcons.Check
        is ModelStatus.Failed -> MyNoteIcons.Warn
        else -> null
    }

private const val READY_VISIBLE_MILLIS = 3200L
