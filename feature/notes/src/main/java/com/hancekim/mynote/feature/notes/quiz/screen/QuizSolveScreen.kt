package com.hancekim.mynote.feature.notes.screen.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.riseIn
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteEasing
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.designsystem.theme.rememberReduceMotion
import com.hancekim.mynote.feature.notes.QuizQuestionUi
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.QuizUiState
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun QuizSolveScreen(
    state: QuizUiState.Solving,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val type = MyNoteTheme.textStyles
    val short = MyNoteTheme.adaptive.heightClass == MyNoteHeightClass.Compact

    val enter = remember { Animatable(0f) }
    val scroll = rememberScrollState()
    LaunchedEffect(state.index) {
        scroll.scrollTo(0)
        enter.snapTo(0f)
        enter.animateTo(1f, tween(320, easing = MyNoteEasing))
    }
    val reach = scroll.maxValue
    LaunchedEffect(state.index, state.revealed, reach) {
        if (state.revealed && scroll.value < reach) scroll.animateScrollTo(reach)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = CONTENT_MAX_WIDTH)) {
            SolveTopBar(state = state, onIntent = onIntent, compact = short)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = enter.value
                            translationX = (1f - enter.value) * ENTER_SLIDE.toPx()
                        }.verticalScroll(scroll)
                        .padding(
                            start = 22.dp,
                            end = 22.dp,
                            top = if (short) 14.dp else 26.dp,
                            bottom = 20.dp,
                        ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                if (state.partial) {
                    PartialBand(count = state.questions.size)
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.notes_quiz_question_index, state.index + 1),
                        style = type.monoLabel,
                        color = colors.accent,
                    )
                    Text(text = state.current.question, style = type.headline, color = colors.text)
                }

                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.current.choices.forEachIndexed { index, choice ->
                        key(state.index, index) {
                            ChoiceRow(
                                index = index,
                                text = choice,
                                state = state,
                                onPick = { onIntent(NotesIntent.PickChoice(index)) },
                            )
                        }
                    }
                }

                if (state.revealed) {
                    Explanation(state = state)
                }
            }

            if (state.revealed && !state.autoAdvancing) {
                QuizPrimaryButton(
                    text =
                        stringResource(
                            if (state.isLast) R.string.notes_quiz_see_result else R.string.notes_quiz_next,
                        ),
                    onClick = throttledProcess { onIntent(NotesIntent.NextQuestion()) },
                    height = 52.dp,
                    modifier =
                        Modifier
                            .padding(horizontal = 22.dp)
                            .padding(bottom = if (short) 10.dp else 16.dp)
                            .riseIn(durationMillis = 260),
                )
            }
        }
    }
}

@Composable
private fun SolveTopBar(
    state: QuizUiState.Solving,
    onIntent: (NotesIntent) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = 4.dp)
                .clip(shape)
                .background(colors.glassOpaque)
                .border(1.dp, colors.glassBorder, shape)
                .padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val progressDescription =
            stringResource(R.string.notes_quiz_progress, state.questions.size, state.index + 1)
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = progressDescription
                    },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.questions.indices.forEach { index ->
                val target =
                    when {
                        index < state.marks.size -> if (state.marks[index]) colors.correct else colors.wrong
                        index == state.index -> colors.accent
                        else -> colors.border
                    }
                val cell by animateColorAsState(target, tween(300), label = "progressCell")
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(cell),
                )
            }
        }
        Text(
            text = "${state.index + 1} / ${state.questions.size}",
            style = MyNoteTheme.textStyles.monoSmall,
            color = colors.text3,
            maxLines = 1,
            softWrap = false,
        )
        QuizCloseButton(
            onClick = { onIntent(NotesIntent.DismissQuiz) },
            size = 34.dp,
            iconSize = 17.dp,
        )
    }
}

@Composable
private fun PartialBand(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.warn.copy(alpha = 0.09f))
                .border(1.dp, colors.warn.copy(alpha = 0.22f), shape)
                .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MyNoteIcons.Warn,
            contentDescription = null,
            tint = colors.warn,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.notes_quiz_partial, count),
            style = MyNoteTheme.textStyles.caption,
            color = colors.warnText,
        )
    }
}

@Composable
private fun ChoiceRow(
    index: Int,
    text: String,
    state: QuizUiState.Solving,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(14.dp)

    val correctLabel = stringResource(R.string.notes_quiz_correct)
    val wrongLabel = stringResource(R.string.notes_quiz_wrong)
    val isAnswer = index == state.current.answerIndex
    val isMyWrong = state.revealed && index == state.picked && !isAnswer
    val revealedAnswer = state.revealed && isAnswer

    val borderColor by animateColorAsState(
        targetValue =
            when {
                revealedAnswer -> colors.correct
                isMyWrong -> colors.wrong
                else -> colors.border
            },
        animationSpec = tween(200),
        label = "choiceBorder",
    )
    val fill by animateColorAsState(
        targetValue =
            when {
                revealedAnswer -> colors.correct.copy(alpha = 0.10f)
                isMyWrong -> colors.wrong.copy(alpha = 0.10f)
                state.revealed -> colors.surface.copy(alpha = 0.55f)
                else -> colors.surface
            },
        animationSpec = tween(200),
        label = "choiceFill",
    )
    val dimmed = state.revealed && !revealedAnswer && !isMyWrong

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(fill)
                .border(if (revealedAnswer || isMyWrong) 1.5.dp else 1.dp, borderColor, shape)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    enabled = !state.revealed,
                    role = Role.RadioButton,
                    onClick = onPick,
                ).semantics {
                    if (revealedAnswer) stateDescription = correctLabel
                    if (isMyWrong) stateDescription = wrongLabel
                }.padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .background(
                        when {
                            revealedAnswer -> colors.correct
                            isMyWrong -> colors.wrong
                            else -> colors.surface2
                        },
                        RoundedCornerShape(9.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${index + 1}",
                style = MyNoteTheme.textStyles.monoSmall.copy(fontWeight = FontWeight.W700),
                color = if (revealedAnswer || isMyWrong) colors.bg else colors.text3,
            )
        }

        Text(
            text = text,
            style = MyNoteTheme.textStyles.bodyCompact,
            color = if (dimmed) colors.text3 else colors.text,
            modifier = Modifier.weight(1f),
        )

        if (revealedAnswer || isMyWrong) {
            Icon(
                imageVector = if (revealedAnswer) MyNoteIcons.Check else MyNoteIcons.Close,
                contentDescription = null,
                tint = if (revealedAnswer) colors.correct else colors.wrong,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun Explanation(
    state: QuizUiState.Solving,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val type = MyNoteTheme.textStyles
    val correct = state.isCorrect
    val accentColor = if (correct) colors.correct else colors.wrong

    Row(
        modifier = modifier.fillMaxWidth().riseIn().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(accentColor),
        )
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS),
                    text = stringResource(if (correct) R.string.notes_quiz_correct else R.string.notes_quiz_wrong),
                    style = type.bodySmall.copy(fontWeight = FontWeight.W700),
                    color = accentColor,
                )
                if (state.autoAdvancing) {
                    Spacer(Modifier.weight(1f))
                    AutoAdvanceRing(color = accentColor)
                    Text(
                        text = stringResource(R.string.notes_quiz_auto_next),
                        style = type.monoSmall.copy(fontSize = 12.sp),
                        color = colors.text3,
                    )
                }
            }
            Text(
                modifier = Modifier.riseIn(delayMillis = RISE_STEP_MILLIS * 2),
                text = state.current.explanation,
                style = type.bodySmall,
                color = colors.text2,
            )
        }
    }
}

@Composable
private fun AutoAdvanceRing(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val still = rememberReduceMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!still) progress.animateTo(1f, tween(QuizUiState.AUTO_ADVANCE_MILLIS, easing = LinearEasing))
    }
    Box(
        modifier
            .size(20.dp)
            .drawWithCache {
                val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                val inset = 1.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                onDrawBehind {
                    drawArc(
                        color = colors.glassBorder,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = progress.value * 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                }
            },
    )
}

private const val RISE_STEP_MILLIS = 70

private val CONTENT_MAX_WIDTH = 560.dp
private val ENTER_SLIDE = 26.dp

private val previewQuestions =
    persistentListOf(
        QuizQuestionUi(
            question = "Dispatchers.IO 가 맡기에 알맞은 작업은?",
            choices = persistentListOf("큰 배열 정렬", "네트워크·파일 입출력", "UI 상태 갱신", "화면 측정과 배치"),
            answerIndex = 1,
            explanation = "IO 는 블로킹 대기가 긴 작업용이라 스레드를 넉넉히 늘려 대기 시간을 감춘다.",
        ),
        QuizQuestionUi(
            question = "withContext 가 하는 일은?",
            choices = persistentListOf("새 코루틴을 띄우고 즉시 반환한다", "현재 코루틴을 취소한다", "코루틴을 유지한 채 컨텍스트만 바꾼다", "전용 스레드를 새로 만든다"),
            answerIndex = 2,
            explanation = "코루틴을 새로 만들지 않고 블록이 실행되는 컨텍스트만 갈아끼운 뒤 결과를 돌려준다.",
        ),
    )

@Preview(name = "풀이 · 미선택", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390, heightDp = 844)
@Composable
private fun QuizSolveScreenPreview() {
    MyNoteTheme {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizSolveScreen(state = QuizUiState.Solving(questions = previewQuestions), onIntent = {})
        }
    }
}

@Preview(name = "풀이 · 오답", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390, heightDp = 844)
@Composable
private fun QuizSolveScreenWrongPreview() {
    MyNoteTheme {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizSolveScreen(
                state =
                    QuizUiState.Solving(
                        questions = previewQuestions,
                        picked = 0,
                        marks = persistentListOf(false),
                    ),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "풀이 · 정답 자동진행", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390, heightDp = 844)
@Composable
private fun QuizSolveScreenCorrectPreview() {
    MyNoteTheme {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizSolveScreen(
                state =
                    QuizUiState.Solving(
                        questions = previewQuestions,
                        picked = 1,
                        marks = persistentListOf(true),
                        autoAdvancing = true,
                        partial = true,
                    ),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "풀이 · 넓은 창", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 720, heightDp = 760)
@Composable
private fun QuizSolveScreenWidePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Medium) {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizSolveScreen(state = QuizUiState.Solving(questions = previewQuestions), onIntent = {})
        }
    }
}
