package com.hancekim.mynote.feature.notes.screen.quiz

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.riseIn
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.QuizUiState

@Composable
internal fun QuizFailedScreen(
    state: QuizUiState.Failed,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val type = quizTypography()

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            QuizCloseButton(
                onClick = { onIntent(NotesIntent.DismissQuiz) },
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
                text = "문제를 만들지 못했습니다",
                style = type.failTitle,
                color = colors.text,
            )
            Text(
                modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS * 2),
                text = state.kind.reason(),
                style = type.body,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.size(26.dp))

            Column(
                modifier = Modifier.fillMaxWidth().riseIn(delayMillis = RISE_STEP_MILLIS * 3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuizPrimaryButton(
                    text = "다시 만들기",
                    onClick = throttledProcess { onIntent(NotesIntent.RetryQuiz) },
                )
                if (state.canRetryWithFewer) {
                    QuizOutlineButton(
                        text = "최대 ${QuizUiState.MIN_COUNT}문제로 줄여서 다시",
                        onClick = { onIntent(NotesIntent.RetryQuizWithFewer(QuizUiState.MIN_COUNT)) },
                    )
                }
            }
        }

        QuizQuietButton(
            text = "노트로 돌아가기",
            onClick = { onIntent(NotesIntent.DismissQuiz) },
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 12.dp),
        )
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

private fun QuizUiState.Failed.Kind.reason(): String =
    when (this) {
        QuizUiState.Failed.Kind.Parse -> {
            "모델이 형식을 어긋나게 답해서 문제를 읽어낼 수 없었습니다.\n한번 더 시도하면 성공할 수 있습니다."
        }

        QuizUiState.Failed.Kind.Empty -> {
            "노트에서 문제로 낼 만한 내용을 찾지 못했습니다.\n노트가 너무 짧을 수 있습니다."
        }

        QuizUiState.Failed.Kind.Engine -> {
            "모델을 불러오지 못했습니다.\n모델 파일을 다시 내려받아야 할 수 있습니다."
        }

        QuizUiState.Failed.Kind.Timeout -> {
            "시간이 너무 오래 걸려서 멈췄습니다.\n문항 수를 줄이면 빨라집니다."
        }
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
