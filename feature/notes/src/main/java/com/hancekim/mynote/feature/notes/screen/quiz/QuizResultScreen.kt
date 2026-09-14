package com.hancekim.mynote.feature.notes.screen.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.riseIn
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.QuizUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun QuizResultScreen(
    state: QuizUiState.Result,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val type = quizTypography()
    val short = MyNoteTheme.adaptive.heightClass == MyNoteHeightClass.Compact
    val verdict = verdictFor(state.rate)

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 30.dp)) {
        if (short) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScoreGauge(
                    score = state.score,
                    total = state.total,
                    rate = state.rate,
                    size = 132.dp,
                    scoreStyle = type.score,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS),
                        text = verdict.title,
                        style = type.resultTitle,
                        color = colors.text,
                    )
                    Text(
                        modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS * 2),
                        text = verdict.body,
                        style = type.body,
                        color = colors.text2,
                    )
                    MarkChips(
                        marks = state.marks,
                        modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS * 3),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ScoreGauge(
                    score = state.score,
                    total = state.total,
                    rate = state.rate,
                    size = 186.dp,
                    scoreStyle = type.score,
                )
                Box(Modifier.size(34.dp))
                Text(
                    modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS),
                    text = verdict.title,
                    style = type.resultTitle,
                    color = colors.text,
                )
                Box(Modifier.size(12.dp))
                Text(
                    text = verdict.body,
                    style = type.body.copy(fontSize = 14.5f.sp),
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS * 2).widthIn(max = 280.dp),
                )
                Box(Modifier.size(34.dp))
                MarkChips(
                    marks = state.marks,
                    modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS * 3),
                )
            }
        }

        QuizPrimaryButton(
            text = "노트로 돌아가기",
            onClick = { onIntent(NotesIntent.DismissQuiz) },
            height = 52.dp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun ScoreGauge(
    score: Int,
    total: Int,
    rate: Float,
    size: Dp,
    scoreStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val still = rememberReduceMotion()

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val sweep by animateFloatAsState(
        targetValue = if (started) rate else 0f,
        animationSpec = if (still) snap() else tween(900, easing = QuizEasing),
        label = "gaugeSweep",
    )
    val pop by animateFloatAsState(
        targetValue = if (started) 1f else 0.86f,
        animationSpec = if (still) snap() else tween(420, easing = QuizEasing),
        label = "gaugePop",
    )

    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                    alpha = pop
                }.drawWithCache {
                    val stroke = Stroke(width = GAUGE_STROKE.toPx(), cap = StrokeCap.Round)
                    val inset = GAUGE_STROKE.toPx() / 2f
                    val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                    onDrawBehind {
                        drawArc(
                            color = colors.surface2,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = stroke,
                        )
                        drawArc(
                            color = colors.accent,
                            startAngle = -90f,
                            sweepAngle = sweep * 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = stroke,
                        )
                    }
                }.clearAndSetSemantics { contentDescription = "${total}문제 중 ${score}문제 정답" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$score", style = scoreStyle, color = colors.text)
            Text(
                text = "/ $total",
                style = quizTypography().counter.copy(fontSize = 14.sp),
                color = colors.text3,
            )
        }
    }
}

@Composable
private fun MarkChips(
    marks: ImmutableList<Boolean>,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        marks.forEachIndexed { index, correct ->
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .background(
                            (if (correct) colors.correct else colors.wrong).copy(alpha = 0.14f),
                            RoundedCornerShape(11.dp),
                        ).clearAndSetSemantics {
                            contentDescription = "${index + 1}번 " + if (correct) "정답" else "오답"
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (correct) MyNoteIcons.Check else MyNoteIcons.Close,
                    contentDescription = null,
                    tint = if (correct) colors.correct else colors.wrong,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

private data class Verdict(
    val title: String,
    val body: String,
)

private fun verdictFor(rate: Float): Verdict =
    when {
        rate >= 1f -> Verdict("다 맞았어", "이 노트는 충분히 소화한 것 같아.")
        rate >= 0.6f -> Verdict("거의 잡았어", REVIEW_BODY)
        else -> Verdict("한 번 더 읽어보자", REVIEW_BODY)
    }

private const val REVIEW_BODY = "틀린 문제의 해설은 방금 본 그 자리에 있었어. 노트를 다시 한 번 훑어봐."

private const val RISE_STEP_MILLIS = 90

private val GAUGE_STROKE = 10.dp

private class ResultMarksProvider : PreviewParameterProvider<ImmutableList<Boolean>> {
    override val values =
        sequenceOf(
            persistentListOf(true, false, true, true),
            persistentListOf(true, true, true, true),
            persistentListOf(false, false, true),
        )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390, heightDp = 844)
@Composable
private fun QuizResultScreenPreview(
    @PreviewParameter(ResultMarksProvider::class) marks: ImmutableList<Boolean>,
) {
    MyNoteTheme {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizResultScreen(state = QuizUiState.Result(marks), onIntent = {})
        }
    }
}

@Preview(name = "점수 · 짧은 창", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 890, heightDp = 420)
@Composable
private fun QuizResultScreenShortPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Compact) {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizResultScreen(
                state = QuizUiState.Result(persistentListOf(true, false, true, true)),
                onIntent = {},
            )
        }
    }
}
