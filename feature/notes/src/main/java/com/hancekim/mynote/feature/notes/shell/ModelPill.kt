package com.hancekim.mynote.feature.notes.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.FloatingStatusPill
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.domain.model.ModelFailure
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.ModelPillUi
import com.hancekim.mynote.feature.notes.element.PillIcon
import com.hancekim.mynote.feature.notes.element.isBusy
import com.hancekim.mynote.feature.notes.element.modelPillUi
import com.hancekim.mynote.feature.notes.screen.ModelPreviewFrame
import com.hancekim.mynote.feature.notes.screen.color
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
    AnimatedVisibility(
        visible = visible && !hidden,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = topOffset + 10.dp),
    ) {
        ModelStatusPill(pill = modelPillUi(status), backdrop = backdrop, onClick = onClick)
    }
}

@Composable
private fun ModelStatusPill(
    pill: ModelPillUi,
    backdrop: Backdrop?,
    onClick: () -> Unit,
) {
    FloatingStatusPill(
        label = stringResource(pill.label),
        tone = pill.tone.color(),
        trailing = pill.percent?.let { stringResource(R.string.notes_model_percent, it) },
        progress = pill.progress,
        icon = pill.icon?.vector(),
        backdrop = backdrop,
        onClick = onClick,
    )
}

private fun PillIcon.vector(): ImageVector =
    when (this) {
        PillIcon.Check -> MyNoteIcons.Check
        PillIcon.Warn -> MyNoteIcons.Warn
    }

private const val READY_VISIBLE_MILLIS = 3200L

private class ModelPillStatusProvider : PreviewParameterProvider<ModelStatus> {
    override val values =
        sequenceOf(
            ModelStatus.Retrying(1_090_000_000L, 2_588_147_712L),
            ModelStatus.Downloading(1_090_000_000L, 2_588_147_712L),
            ModelStatus.WaitingForWifi,
            ModelStatus.Preparing,
            ModelStatus.Verifying,
            ModelStatus.Ready,
            ModelStatus.Failed(ModelFailure.NO_SPACE),
        )
}

@Preview(name = "알약", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun ModelPillPreview(
    @PreviewParameter(ModelPillStatusProvider::class) status: ModelStatus,
) {
    ModelPreviewFrame(largeFont = false) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            ModelStatusPill(pill = modelPillUi(status), backdrop = null, onClick = {})
        }
    }
}

@Preview(name = "알약 · 큰 글자", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun ModelPillLargeFontPreview(
    @PreviewParameter(ModelPillStatusProvider::class) status: ModelStatus,
) {
    ModelPreviewFrame(largeFont = true) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            ModelStatusPill(pill = modelPillUi(status), backdrop = null, onClick = {})
        }
    }
}
