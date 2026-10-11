package com.hancekim.mynote.feature.quiz.component

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
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.ui.element.CtaAction
import com.hancekim.mynote.core.ui.element.QuizCtaUi
import com.hancekim.mynote.core.ui.element.fractionOrNull
import com.hancekim.mynote.core.ui.element.quizCtaUi
import com.hancekim.mynote.feature.quiz.QuizUiState
import com.hancekim.mynote.feature.quiz.R
import com.hancekim.mynote.core.ui.R as UiR

@Suppress("ktlint:compose:modifier-clickable-order", "ktlint:compose:lambda-param-event-trailing")
@Composable
fun QuizCtaCard(
    status: ModelStatus,
    modifier: Modifier = Modifier,
    onAction: (CtaAction) -> Unit,
) {
    val colors = MyNoteTheme.colors
    val cta = quizCtaUi(status)
    val shine = cta.shine && !rememberReduceMotion()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HorizontalDivider(color = colors.border)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = null,
                        indication = remember { PressIndication(scale = 0.985f) },
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.quiz_setup_title),
                        onClick = throttledProcess(action = { onAction(cta.action) }),
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
                    text = stringResource(R.string.quiz_cta_title),
                    style = MyNoteTheme.textStyles.cardTitle.copy(fontWeight = FontWeight.W700),
                    color = colors.text,
                )
                Text(
                    text = ctaSubtitle(cta, status),
                    style = MyNoteTheme.textStyles.caption,
                    color = if (cta.warn) colors.wrong else colors.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val chip = cta.chip
            if (chip != null) {
                Text(
                    text = stringResource(chip),
                    style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W700),
                    color = colors.bg,
                    modifier =
                        Modifier
                            .background(colors.accent, RoundedCornerShape(CHIP_RADIUS))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            } else {
                Icon(
                    imageVector = MyNoteIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ctaSubtitle(
    cta: QuizCtaUi,
    status: ModelStatus,
): String {
    // 다른 모듈의 공개 속성이라 스마트 캐스트가 안 된다 — 한 번 읽어 둔다
    val arg = cta.subtitleArg
    return when {
        cta.subtitle == UiR.string.ui_quiz_cta_subtitle -> {
            stringResource(cta.subtitle, QuizUiState.MAX_COUNT)
        }

        cta.subtitle == UiR.string.ui_quiz_cta_downloading -> {
            stringResource(cta.subtitle, ((status.fractionOrNull() ?: 0f) * 100).toInt())
        }

        arg != null -> {
            stringResource(cta.subtitle, arg)
        }

        else -> {
            stringResource(cta.subtitle)
        }
    }
}

private val CARD_RADIUS = 18.dp
private val RING_WIDTH = 1.5.dp
private val CHIP_RADIUS = 10.dp

private class QuizCtaStatusProvider : PreviewParameterProvider<ModelStatus> {
    override val values =
        sequenceOf(
            ModelStatus.Ready,
            ModelStatus.Missing,
            ModelStatus.Downloading(1_090_000_000L, 2_588_147_712L),
            ModelStatus.Paused(1_090_000_000L, 2_588_147_712L),
            ModelStatus.Failed("no-space"),
        )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun QuizCtaCardPreview(
    @PreviewParameter(QuizCtaStatusProvider::class) status: ModelStatus,
) {
    MyNoteTheme {
        Box(Modifier.padding(16.dp)) {
            QuizCtaCard(status = status, onAction = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, fontScale = 1.3f)
@Composable
private fun QuizCtaCardLargeFontPreview() {
    MyNoteTheme {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            Box(Modifier.padding(16.dp)) {
                QuizCtaCard(status = ModelStatus.Ready, onAction = {})
            }
        }
    }
}
