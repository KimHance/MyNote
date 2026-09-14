package com.hancekim.mynote.feature.notes.screen.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
internal fun QuizPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    leadingSparkle: Boolean = false,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(15.dp)
    val indication =
        remember(colors, shape) {
            PressIndication(shape, colors.accent, colors.accentPressed, scale = 0.98f)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingSparkle) {
            Icon(
                imageVector = MyNoteIcons.Sparkle,
                contentDescription = null,
                tint = colors.bg,
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            text = text,
            style = quizTypography().button,
            color = colors.bg,
            modifier = Modifier.padding(start = if (leadingSparkle) 8.dp else 0.dp),
        )
    }
}

@Composable
internal fun QuizOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(15.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.surface) }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .border(1.dp, colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = quizTypography().button.copy(fontWeight = FontWeight.W500),
            color = colors.text,
        )
    }
}

@Composable
internal fun QuizQuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.navItemPressed) }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(46.dp)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = quizTypography().body.copy(fontSize = 14.5f.sp),
            color = colors.text3,
        )
    }
}

@Composable
internal fun QuizCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(size / 2)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.navItemPressed) }
    Box(
        modifier =
            modifier
                .size(size)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MyNoteIcons.Close,
            contentDescription = "퀴즈 닫기",
            tint = colors.text2,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun QuizButtonsPreview() {
    MyNoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuizPrimaryButton(text = "다시 만들기", onClick = {})
            QuizPrimaryButton(text = "만들기", onClick = {}, height = 44.dp, leadingSparkle = true)
            QuizOutlineButton(text = "최대 3문제로 줄여서 다시", onClick = {})
            QuizQuietButton(text = "노트로 돌아가기", onClick = {})
            QuizCloseButton(onClick = {})
        }
    }
}
