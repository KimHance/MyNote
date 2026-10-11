package com.hancekim.mynote.feature.notes.quiz.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.riseIn
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.quiz.QuizIntent
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import com.hancekim.mynote.feature.notes.quiz.component.QuizCloseButton
import com.hancekim.mynote.feature.notes.quiz.component.QuizOutlineButton
import com.hancekim.mynote.feature.notes.quiz.component.QuizPrimaryButton
import com.hancekim.mynote.feature.notes.quiz.component.QuizQuietButton

@Composable
internal fun QuizFailedScreen(
    state: QuizUiState.Failed,
    onIntent: (QuizIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val type = MyNoteTheme.textStyles

    Surface(modifier = modifier.fillMaxSize(), color = colors.bg) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                QuizCloseButton(
                    onClick = { onIntent(QuizIntent.Dismiss) },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FailIcon(modifier = Modifier.riseIn())
                Text(
                    modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS).padding(top = 26.dp, bottom = 14.dp),
                    text = stringResource(R.string.notes_quiz_failed_title),
                    style = type.sectionTitle,
                    color = colors.text,
                )
                Text(
                    modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS * 2),
                    text = stringResource(state.kind.reasonRes()),
                    style = type.bodySmall,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                )
                Box(Modifier.size(26.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().riseIn(delayMillis = RISE_STEP_MILLIS * 3),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuizPrimaryButton(
                        text = stringResource(R.string.notes_quiz_retry),
                        onClick = throttledProcess { onIntent(QuizIntent.Retry) },
                    )
                    if (state.canRetryWithFewer) {
                        QuizOutlineButton(
                            text = stringResource(R.string.notes_quiz_retry_fewer, QuizUiState.MIN_COUNT),
                            onClick = { onIntent(QuizIntent.RetryWithFewer(QuizUiState.MIN_COUNT)) },
                        )
                    }
                }
            }

            QuizQuietButton(
                text = stringResource(R.string.notes_quiz_back_to_note),
                onClick = { onIntent(QuizIntent.Dismiss) },
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun FailIcon(modifier: Modifier = Modifier) {
    val colors = MyNoteTheme.colors
    Box(
        modifier =
            modifier
                .size(76.dp)
                .background(colors.wrong.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MyNoteIcons.Sparkle,
            contentDescription = null,
            tint = colors.wrong,
            modifier =
                Modifier
                    .size(32.dp)
                    .graphicsLayer { alpha = 0.5f },
        )
    }
}

private const val RISE_STEP_MILLIS = 70

@StringRes
private fun QuizUiState.Failed.Kind.reasonRes(): Int =
    when (this) {
        QuizUiState.Failed.Kind.Parse -> R.string.notes_quiz_failed_parse
        QuizUiState.Failed.Kind.Empty -> R.string.notes_quiz_failed_empty
        QuizUiState.Failed.Kind.Engine -> R.string.notes_quiz_failed_engine
        QuizUiState.Failed.Kind.Timeout -> R.string.notes_quiz_failed_timeout
    }

private class FailKindProvider : PreviewParameterProvider<QuizUiState.Failed.Kind> {
    override val values =
        QuizUiState.Failed.Kind.entries
            .asSequence()
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390, heightDp = 844)
@Composable
private fun QuizFailedScreenPreview(
    @PreviewParameter(FailKindProvider::class) kind: QuizUiState.Failed.Kind,
) {
    MyNoteTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(MyNoteTheme.colors.bg),
        ) {
            QuizFailedScreen(
                state = QuizUiState.Failed(kind = kind, maxCount = 4, noteId = "n"),
                onIntent = {},
            )
        }
    }
}
