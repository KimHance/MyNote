package com.hancekim.mynote.feature.notes.screen.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.aiShine
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.designsystem.theme.rememberReduceMotion
import com.hancekim.mynote.feature.notes.R

@Suppress("ktlint:compose:modifier-clickable-order", "ktlint:compose:lambda-param-event-trailing")
@Composable
internal fun QuizCtaCard(
    available: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shine = available && !rememberReduceMotion()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HorizontalDivider(color = colors.border)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (available) 1f else UNAVAILABLE_ALPHA }
                    .clickable(
                        enabled = available,
                        interactionSource = null,
                        indication = remember { PressIndication(scale = 0.985f) },
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.notes_quiz_setup_title),
                        onClick = throttledProcess(action = onClick),
                    ).aiShine(
                        color = colors.accent,
                        baseColor = colors.border,
                        radius = CARD_RADIUS,
                        width = RING_WIDTH,
                        running = shine,
                    ).padding(RING_WIDTH)
                    .background(colors.surface, RoundedCornerShape(CARD_RADIUS - RING_WIDTH))
                    .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MyNoteIcons.Sparkle,
                contentDescription = null,
                tint = colors.accent,
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(colors.accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .wrapContentSize()
                        .size(22.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.notes_quiz_cta_title),
                    style = MyNoteTheme.textStyles.cardTitle.copy(fontWeight = FontWeight.W700),
                    color = colors.text,
                )
                Text(
                    text =
                        stringResource(
                            if (available) R.string.notes_quiz_cta_subtitle else R.string.notes_quiz_cta_unavailable,
                        ),
                    style = MyNoteTheme.textStyles.caption,
                    color = colors.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = MyNoteIcons.ChevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val CARD_RADIUS = 18.dp
private val RING_WIDTH = 1.5.dp
private const val UNAVAILABLE_ALPHA = 0.55f

private class QuizCtaAvailabilityProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun QuizCtaCardPreview(
    @PreviewParameter(QuizCtaAvailabilityProvider::class) available: Boolean,
) {
    MyNoteTheme {
        Box(Modifier.padding(16.dp)) {
            QuizCtaCard(available = available, onClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, fontScale = 1.3f)
@Composable
private fun QuizCtaCardLargeFontPreview() {
    MyNoteTheme {
        CompositionLocalProvider(
            LocalInspectionMode provides true,
        ) {
            Box(Modifier.padding(16.dp)) {
                QuizCtaCard(available = true, onClick = {})
            }
        }
    }
}
