package com.hancekim.mynote.feature.notes.screen.quiz

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.QuizUiState

@Composable
internal fun QuizSetupDialog(
    state: QuizUiState.Setup,
    noteTitle: String,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    bodyModifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val type = quizTypography()

    Column(
        modifier =
            modifier
                .widthIn(max = DIALOG_MAX_WIDTH)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.glassOpaque)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(26.dp))
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
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
                Text(text = "퀴즈 만들기", style = type.screenTitle, color = colors.text)
                Text(
                    text = "$noteTitle · 기기 안에서 생성",
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
                        text = "최대 문항 수",
                        style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W500),
                        color = colors.text2,
                    )
                    Text(
                        text = "최대 ${state.maxCount}문제",
                        style = type.counter.copy(fontSize = 12.sp),
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
                            onSelect = { onIntent(NotesIntent.SetQuizCount(count)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    text = "노트가 짧으면 이보다 적게 나올 수 있어. 물어볼 게 없는 내용으로는 문제를 만들지 않아.",
                    style = type.caption,
                    color = colors.text3,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogQuietButton(text = "취소", onClick = { onIntent(NotesIntent.DismissQuiz) })
                DialogPrimaryButton(
                    text = "만들기",
                    onClick = throttledProcess { onIntent(NotesIntent.GenerateQuiz(state.noteId, state.maxCount)) },
                )
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
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(13.dp)
    val fill =
        if (selected) colors.accent.copy(alpha = 0.14f) else colors.navItemPressed.copy(alpha = 0.03f)
    val indication =
        remember(colors, shape, fill) { PressIndication(shape, fill, colors.navItemPressed) }
    Box(
        modifier =
            modifier
                .height(46.dp)
                .clickable(
                    interactionSource = null,
                    indication = indication,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).border(1.dp, if (selected) colors.accent else colors.glassBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${count}문제",
            style =
                MyNoteTheme.textStyles.treeLabel.copy(
                    fontSize = 14.5f.sp,
                    fontWeight = if (selected) FontWeight.W700 else FontWeight.W500,
                ),
            color = if (selected) colors.text else colors.text2,
        )
    }
}

@Composable
private fun DialogPrimaryButton(
    text: String,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication =
        remember(colors, shape) {
            PressIndication(shape, colors.accent, colors.accentPressed, scale = 0.98f)
        }
    Row(
        modifier =
            Modifier
                .height(44.dp)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp),
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
            style = quizTypography().button.copy(fontSize = 15.sp),
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
