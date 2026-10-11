package com.hancekim.mynote.feature.quiz.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.quiz.QuizIntent
import com.hancekim.mynote.feature.quiz.QuizUiState
import com.hancekim.mynote.feature.quiz.R
import com.hancekim.mynote.feature.quiz.component.QuizCloseButton
import com.hancekim.mynote.feature.quiz.component.QuizOrb
import com.hancekim.mynote.feature.quiz.component.QuizOutlineButton
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun QuizGeneratingScreen(
    state: QuizUiState.Loading,
    onIntent: (QuizIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val short = MyNoteTheme.adaptive.heightClass == MyNoteHeightClass.Compact
    val steps = stringArrayResource(R.array.quiz_generating_steps)

    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(steps) {
        for (next in 1..steps.lastIndex) {
            delay(STEP_PERIOD_MILLIS.milliseconds)
            step = next
        }
    }

    var arrived by remember(state.maxCount) { mutableIntStateOf(0) }
    LaunchedEffect(state.maxCount) {
        for (next in 1 until state.maxCount) {
            delay(SLOT_PERIOD_MILLIS.milliseconds)
            arrived = next
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MyNoteTheme.colors.bg) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                QuizCloseButton(
                    onClick = { onIntent(QuizIntent.Dismiss) },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (short) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        QuizOrb(size = 64.dp)
                        GeneratingTitle(title = steps[step])
                        QuitButton(onIntent = onIntent, modifier = Modifier.widthIn(max = 200.dp))
                    }
                    SlotColumn(
                        maxCount = state.maxCount,
                        arrived = arrived,
                        compact = true,
                        modifier = Modifier.weight(1f).widthIn(max = 380.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    QuizOrb()
                    Spacer(Modifier.height(30.dp))
                    GeneratingTitle(title = steps[step])
                    Spacer(Modifier.height(30.dp))
                    SlotColumn(maxCount = state.maxCount, arrived = arrived, compact = false)
                }
                QuitButton(
                    onIntent = onIntent,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun GeneratingTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    val type = MyNoteTheme.textStyles
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = type.screenTitle, color = MyNoteTheme.colors.text)
        Text(
            text = stringResource(R.string.quiz_generating_hint),
            style = MyNoteTheme.textStyles.caption.copy(fontSize = 13.5f.sp),
            color = MyNoteTheme.colors.text3,
        )
    }
}

@Composable
private fun QuitButton(
    onIntent: (QuizIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    QuizOutlineButton(
        text = stringResource(R.string.quiz_generating_stop),
        onClick = { onIntent(QuizIntent.Dismiss) },
        modifier = modifier,
    )
}

@Composable
private fun SlotColumn(
    maxCount: Int,
    arrived: Int,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(maxCount) { index ->
            QuizSlotRow(
                number = index + 1,
                status =
                    when {
                        index < arrived -> SlotStatus.Done
                        index == arrived -> SlotStatus.Active
                        else -> SlotStatus.Waiting
                    },
                compact = compact,
            )
        }
    }
}

@Composable
private fun QuizSlotRow(
    number: Int,
    status: SlotStatus,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val fade by animateFloatAsState(
        targetValue = if (status == SlotStatus.Waiting) 0.45f else 1f,
        animationSpec = tween(300),
        label = "slotAlpha",
    )
    val active = status == SlotStatus.Active

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = fade }
                .clip(shape)
                .background(
                    if (active) colors.accent.copy(alpha = 0.05f) else colors.surface.copy(alpha = 0.55f),
                ).border(1.dp, if (active) colors.accent.copy(alpha = 0.34f) else colors.surface2, shape)
                .padding(horizontal = if (compact) 11.dp else 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val done = status == SlotStatus.Done
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .background(
                        if (done) colors.correct.copy(alpha = 0.16f) else colors.surface2,
                        RoundedCornerShape(8.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$number",
                style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 11.5f.sp, fontWeight = FontWeight.W700),
                color = if (done) colors.correct else colors.text3,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SlotBar(status = status, widthFraction = 1f)
            SlotBar(status = status, widthFraction = 0.62f)
        }

        SlotCheck(visible = done)
    }
}

@Composable
private fun SlotCheck(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(15.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.6f),
        ) {
            Icon(
                imageVector = MyNoteIcons.Check,
                contentDescription = null,
                tint = MyNoteTheme.colors.correct,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun SlotBar(
    status: SlotStatus,
    widthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val still = rememberReduceMotion()
    val shimmer =
        if (status == SlotStatus.Active && !still) {
            rememberInfiniteTransition(label = "shimmer").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(SHIMMER_PERIOD, easing = LinearEasing)),
                label = "shimmerOffset",
            )
        } else {
            null
        }
    val base = if (status == SlotStatus.Waiting) colors.surface2.copy(alpha = 0.6f) else colors.border

    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(9.dp)
            .clip(RoundedCornerShape(4.5.dp))
            .drawWithCache {
                val highlight =
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, colors.text3.copy(alpha = 0.35f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                    )
                onDrawBehind {
                    drawRect(base)
                    val progress = shimmer?.value ?: return@onDrawBehind
                    translate(left = (progress * 2f - 1f) * size.width) { drawRect(brush = highlight) }
                }
            },
    )
}

private enum class SlotStatus { Done, Active, Waiting }

private const val STEP_PERIOD_MILLIS = 900L
private const val SLOT_PERIOD_MILLIS = 2400L
private const val SHIMMER_PERIOD = 1500

@Preview(name = "생성 중 · 폰", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390, heightDp = 844)
@Composable
private fun QuizGeneratingScreenPreview() {
    MyNoteTheme {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizGeneratingScreen(state = QuizUiState.Loading(noteId = "n", maxCount = 4), onIntent = {})
        }
    }
}

@Preview(name = "생성 중 · 짧은 창", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 890, heightDp = 420)
@Composable
private fun QuizGeneratingScreenShortPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Compact) {
        Box(Modifier.fillMaxSize().background(MyNoteTheme.colors.bg)) {
            QuizGeneratingScreen(state = QuizUiState.Loading(noteId = "n", maxCount = 4), onIntent = {})
        }
    }
}
