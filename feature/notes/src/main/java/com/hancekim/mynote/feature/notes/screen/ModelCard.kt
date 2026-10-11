package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ModelFailure
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.ui.element.ModelAction
import com.hancekim.mynote.core.ui.element.ModelCardUi
import com.hancekim.mynote.core.ui.element.ModelProgressUi
import com.hancekim.mynote.core.ui.element.gigabytes
import com.hancekim.mynote.core.ui.element.modelCardUi
import com.hancekim.mynote.core.ui.screen.DestructiveButton
import com.hancekim.mynote.core.ui.screen.ModelPreviewFrame
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.core.ui.screen.color
import com.hancekim.mynote.core.ui.screen.connect.KeepWordsText
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.settings.SettingsIntent
import com.hancekim.mynote.core.ui.R as UiR

@Composable
internal fun ModelSection(
    card: ModelCardUi,
    notificationsEnabled: Boolean,
    onIntent: (SettingsIntent) -> Unit,
    onEnableNotifications: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    SettingsSection(label = stringResource(R.string.notes_settings_section_model)) {
        SettingsCard {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBox(icon = MyNoteIcons.Sparkle, tint = colors.accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.notes_model_name),
                        style = MyNoteTheme.textStyles.cardTitle.copy(fontWeight = FontWeight.W500),
                        color = colors.text,
                    )
                    ModelStatusLine(card = card)
                }
            }

            if (card.progress != ModelProgressUi.None) {
                ModelProgress(progress = card.progress)
            }
            if (card.notificationHint && !notificationsEnabled) {
                NotificationHint(onEnable = onEnableNotifications)
            }

            ModelActions(actions = card.actions, onIntent = onIntent)
        }
        Text(
            text = stringResource(card.help),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ModelStatusLine(card: ModelCardUi) {
    val colors = MyNoteTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(card.tone.color()),
        )
        Text(
            text = stringResource(card.label),
            style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W500),
            color = colors.bodyText,
            maxLines = 1,
        )
        val detail = card.detail
        if (detail != null) {
            Text(
                text = card.detailArg?.let { stringResource(detail, it) } ?: stringResource(detail),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ModelProgress(progress: ModelProgressUi) {
    val colors = MyNoteTheme.colors
    Column(
        modifier = Modifier.padding(start = MODEL_INDENT, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val shape = RoundedCornerShape(BAR_RADIUS)
        Box(
            Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(shape)
                .background(colors.surface2),
        ) {
            when (progress) {
                is ModelProgressUi.Indeterminate -> {
                    LinearProgressIndicator(
                        color = progress.tone.color(),
                        trackColor = Color.Transparent,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        gapSize = 0.dp,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    )
                }

                is ModelProgressUi.Determinate -> {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.fraction)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(progress.tone.color()),
                    )
                }

                ModelProgressUi.None -> {
                    Unit
                }
            }
        }
        if (progress is ModelProgressUi.Determinate) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text =
                        stringResource(
                            R.string.notes_model_bytes,
                            gigabytes(progress.received),
                            gigabytes(progress.total),
                        ),
                    style = MyNoteTheme.textStyles.monoSmall,
                    color = colors.text3,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(UiR.string.ui_model_percent, progress.percent),
                    style = MyNoteTheme.textStyles.monoSmall,
                    color = colors.text3,
                )
            }
        }
    }
}

@Composable
private fun NotificationHint(onEnable: () -> Unit) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            Modifier
                .padding(start = MODEL_INDENT, end = 16.dp, top = 10.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(colors.warn.copy(alpha = HINT_FILL_ALPHA))
                .border(1.dp, colors.warn.copy(alpha = HINT_BORDER_ALPHA), shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KeepWordsText(
            text = stringResource(R.string.notes_model_notif_off),
            style = MyNoteTheme.textStyles.caption,
            color = colors.warnText,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.notes_model_notif_enable),
            style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W700),
            color = colors.warn,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onEnable)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ModelActions(
    actions: List<ModelAction>,
    onIntent: (SettingsIntent) -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = MODEL_INDENT, end = 16.dp, top = 14.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        actions.forEach { action ->
            when (action) {
                ModelAction.Download -> {
                    PrimaryButton(
                        text = stringResource(UiR.string.ui_model_download),
                        onClick = { onIntent(SettingsIntent.StartModelDownload) },
                    )
                }

                ModelAction.Resume -> {
                    PrimaryButton(
                        text = stringResource(UiR.string.ui_model_resume),
                        onClick = { onIntent(SettingsIntent.StartModelDownload) },
                    )
                }

                ModelAction.Retry -> {
                    PrimaryButton(
                        text = stringResource(UiR.string.ui_model_retry),
                        onClick = { onIntent(SettingsIntent.StartModelDownload) },
                    )
                }

                ModelAction.Cancel -> {
                    SecondaryButton(
                        text = stringResource(R.string.notes_model_cancel),
                        onClick = { onIntent(SettingsIntent.CancelModelDownload) },
                    )
                }

                ModelAction.DiscardPartial -> {
                    DestructiveButton(
                        text = stringResource(R.string.notes_model_discard_partial),
                        onClick = { onIntent(SettingsIntent.DiscardPartialModel) },
                    )
                }

                ModelAction.Delete -> {
                    DestructiveButton(
                        text = stringResource(R.string.notes_model_delete),
                        onClick = { onIntent(SettingsIntent.DeleteModel) },
                    )
                }
            }
        }
    }
}

private val MODEL_INDENT = 70.dp
private val BAR_HEIGHT = 6.dp
private val BAR_RADIUS = 3.dp
private const val HINT_FILL_ALPHA = 0.08f
private const val HINT_BORDER_ALPHA = 0.20f

private class ModelCardStatusProvider : PreviewParameterProvider<ModelStatus> {
    override val values =
        sequenceOf(
            ModelStatus.Retrying(1_090_000_000L, 2_588_147_712L),
            ModelStatus.Paused(1_090_000_000L, 2_588_147_712L),
            ModelStatus.WaitingForWifi,
            ModelStatus.Preparing,
            ModelStatus.Downloading(1_090_000_000L, 2_588_147_712L),
            ModelStatus.Verifying,
            ModelStatus.Missing,
            ModelStatus.Ready,
            ModelStatus.Failed(ModelFailure.NO_SPACE),
            ModelStatus.Failed("no-plan"),
        )
}

@Preview(name = "모델 카드 · 알림 꺼짐", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun ModelSectionPreview(
    @PreviewParameter(ModelCardStatusProvider::class) status: ModelStatus,
) {
    ModelPreviewFrame(largeFont = false) {
        ModelSection(
            card = modelCardUi(status),
            notificationsEnabled = false,
            onIntent = {},
            onEnableNotifications = {},
        )
    }
}

@Preview(name = "모델 카드 · 알림 꺼짐 · 큰 글자", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun ModelSectionLargeFontPreview(
    @PreviewParameter(ModelCardStatusProvider::class) status: ModelStatus,
) {
    ModelPreviewFrame(largeFont = true) {
        ModelSection(
            card = modelCardUi(status),
            notificationsEnabled = false,
            onIntent = {},
            onEnableNotifications = {},
        )
    }
}
