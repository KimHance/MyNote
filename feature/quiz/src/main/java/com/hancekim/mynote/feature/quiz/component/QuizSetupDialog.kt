package com.hancekim.mynote.feature.quiz.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.component.DROPLET_REFRACTION_RATIO
import com.hancekim.mynote.core.designsystem.component.GlassSurface
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.quiz.QuizIntent
import com.hancekim.mynote.feature.quiz.QuizUiState
import com.hancekim.mynote.feature.quiz.R
import com.kyant.backdrop.Backdrop
import com.hancekim.mynote.core.designsystem.R as DsR

@Composable
internal fun QuizSetupDialog(
    state: QuizUiState.Setup,
    noteTitle: String,
    onIntent: (QuizIntent) -> Unit,
    modifier: Modifier = Modifier,
    bodyModifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    val colors = MyNoteTheme.colors
    val type = MyNoteTheme.textStyles

    GlassSurface(
        radius = DIALOG_RADIUS,
        backdrop = backdrop,
        modifier = modifier.widthIn(max = DIALOG_MAX_WIDTH),
        blurRadius = DIALOG_BLUR_RADIUS,
        tint = colors.glassPanel,
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .background(colors.accent.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MyNoteIcons.Sparkle,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.quiz_setup_title),
                        style = type.screenTitle,
                        color = colors.text,
                    )
                    Text(
                        text = stringResource(R.string.quiz_setup_subtitle, noteTitle),
                        style = MyNoteTheme.textStyles.caption,
                        color = colors.text3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = bodyModifier,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.quiz_setup_count_label),
                            style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W500),
                            color = colors.text2,
                        )
                        Text(
                            text = stringResource(R.string.quiz_setup_count_max, state.maxCount),
                            style = type.monoSmall.copy(fontSize = 12.sp),
                            color = colors.text3,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        QuizUiState.COUNT_CHOICES.forEach { count ->
                            CountSegment(
                                count = count,
                                selected = count == state.maxCount,
                                onSelect = { onIntent(QuizIntent.SetCount(count)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.quiz_setup_hint),
                        style = type.caption,
                        color = colors.text3,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogQuietButton(
                        text = stringResource(DsR.string.ds_action_cancel),
                        onClick = { onIntent(QuizIntent.Dismiss) },
                    )
                    DialogPrimaryButton(
                        text = stringResource(R.string.quiz_create),
                        onClick = throttledProcess { onIntent(QuizIntent.Generate(state.noteId, state.maxCount)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CountSegment(
    count: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(SEGMENT_RADIUS)
    val indication = remember { PressIndication(scale = 0.94f) }
    val tint =
        if (selected) colors.accentSoft.compositeOver(colors.glass) else colors.glass

    GlassSurface(
        radius = SEGMENT_RADIUS,
        backdrop = backdrop,
        modifier =
            modifier
                .height(46.dp)
                .selectable(
                    selected = selected,
                    interactionSource = null,
                    indication = indication,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ),
        blurRadius = SEGMENT_BLUR_RADIUS,
        refractionRatio = DROPLET_REFRACTION_RATIO,
        tint = tint,
        shadow = null,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(1.dp, if (selected) colors.accent else Color.Transparent, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.quiz_count, count),
                style =
                    MyNoteTheme.textStyles.treeLabel.copy(
                        fontSize = 14.5f.sp,
                        fontWeight = if (selected) FontWeight.W700 else FontWeight.W500,
                    ),
                color = if (selected) colors.text else colors.text2,
            )
        }
    }
}

@Composable
private fun DialogPrimaryButton(
    text: String,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val indication =
        remember(colors, shape) {
            PressIndication(shape, colors.accent, colors.accentPressed, scale = 0.98f)
        }
    Row(
        modifier =
            Modifier
                .height(44.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = indication,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MyNoteIcons.Sparkle,
            contentDescription = null,
            tint = colors.bg,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = text,
            style = MyNoteTheme.textStyles.button.copy(fontSize = 15.sp),
            color = colors.bg,
            maxLines = 1,
        )
    }
}

@Composable
private fun DialogQuietButton(
    text: String,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.navItemPressed) }
    Box(
        modifier =
            Modifier
                .height(44.dp)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MyNoteTheme.textStyles.treeLabel.copy(fontSize = 15.sp),
            color = colors.text2,
            maxLines = 1,
        )
    }
}

private val DIALOG_MAX_WIDTH = 420.dp
private val DIALOG_RADIUS = 26.dp
private val DIALOG_BLUR_RADIUS = 20.dp
private val SEGMENT_RADIUS = 13.dp
private val SEGMENT_BLUR_RADIUS = 4.dp

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390, heightDp = 420)
@Composable
private fun QuizSetupDialogPreview() {
    MyNoteTheme {
        Box(Modifier.padding(22.dp), contentAlignment = Alignment.Center) {
            QuizSetupDialog(
                state = QuizUiState.Setup(noteId = "coroutine-dispatcher", maxCount = 4),
                noteTitle = "코루틴 디스패처",
                onIntent = {},
            )
        }
    }
}
