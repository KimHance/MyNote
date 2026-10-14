package com.hancekim.mynote.feature.editor.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.feature.editor.R

// 편집기 · 붙여넣기가 같이 쓰는 조각 — 디자인 문서 local-notes 편집기 · 붙여넣기 해부(r1 README §3.2 · README-2 §3.2)

/**
 * 편집기 · 붙여넣기의 자기 상단 바 — 왼쪽 닫기 · 제목 · 오른쪽 주 버튼 하나(셸 크롬은 셸이 숨긴다). 꺼진 주 버튼은 0.4 이고 이유는 이유 줄이 보인다.
 *
 * @param navigationIcon 닫기(편집기 X) · 뒤로(붙여넣기 화살표)
 * @param navigationLabel 아이콘의 낭독 이름
 */
@Composable
internal fun EditorTopBar(
    title: String,
    navigationIcon: ImageVector,
    navigationLabel: String,
    onNavigation: () -> Unit,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MyNoteTheme.adaptive.headerHeight)
                .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onNavigation) {
            Icon(
                imageVector = navigationIcon,
                contentDescription = navigationLabel,
                tint = colors.text,
                modifier = Modifier.size(NAV_ICON),
            )
        }
        Text(
            text = title,
            style = MyNoteTheme.textStyles.sectionTitle,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        PrimaryButton(text = actionLabel, onClick = onAction, enabled = actionEnabled)
    }
}

/**
 * 면 · 테두리 없는 입력 칸(디자인 C1 — 디자인 시스템에 여러 줄 칸이 없어 편집기가 조립한다). 빈 칸이면 [hint] 를 text3 로 보인다.
 * 칸 글은 ViewModel 이 든다(결정 6 — `rememberSaveable` 로 들지 않는다).
 */
@Composable
internal fun PlainField(
    value: String,
    hint: String,
    style: TextStyle,
    singleLine: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MyNoteTheme.colors.text,
    enabled: Boolean = true,
) {
    val colors = MyNoteTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = style.copy(color = color),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = if (singleLine) KeyboardOptions(imeAction = ImeAction.Next) else KeyboardOptions.Default,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(text = hint, style = style, color = colors.text3, maxLines = 1)
                inner()
            }
        },
    )
}

/** 칸 아래 1dp border 선 — 제목 칸 · 그룹 줄. */
internal fun Modifier.underline(color: Color): Modifier =
    drawBehind {
        val stroke = 1.dp.toPx()
        drawRect(color = color, topLeft = Offset(0f, size.height - stroke), size = Size(size.width, stroke))
    }

/**
 * 이유 줄 — 주 버튼이 꺼진 까닭(디자인 C11). [warn] 이면 warn 경고 아이콘 14 + warnText(사용자가 고칠 수 있는 안내 — 빨강 아님),
 * 아니면 caption text3 글만. 줄바꿈을 허용한다.
 */
@Composable
internal fun ReasonLine(
    text: String,
    warn: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (warn) {
            Icon(
                imageVector = MyNoteIcons.Warn,
                contentDescription = null,
                tint = colors.warn,
                modifier = Modifier.padding(top = 2.dp).size(REASON_ICON),
            )
        }
        Text(
            text = text,
            style = MyNoteTheme.textStyles.caption,
            color = if (warn) colors.warnText else colors.text3,
        )
    }
}

/**
 * `쓰기` ↔ `미리보기` 전환 — surface2 트랙(높이 36 · 반경 10 · 안쪽 3) 안에서 고른 칸만 surface + 1dp border.
 * 칸마다 터치 높이는 48 이다(트랙은 그 가운데에 그린다).
 */
@Composable
internal fun ModeToggle(
    previewing: Boolean,
    onSelect: (preview: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Row(
        modifier =
            modifier
                .height(TOUCH)
                .drawBehind {
                    val trackHeight = TRACK.toPx()
                    drawRoundRect(
                        color = colors.surface2,
                        topLeft = Offset(0f, (size.height - trackHeight) / 2f),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(TRACK_RADIUS.toPx()),
                    )
                }.padding(horizontal = TRACK_INSET)
                .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeSegment(stringResource(R.string.editor_mode_write), !previewing) {
            onSelect(false)
        }
        ModeSegment(stringResource(R.string.editor_mode_preview), previewing) {
            onSelect(true)
        }
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(SEGMENT_RADIUS)
    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .height(TRACK - TRACK_INSET * 2)
                    .then(
                        if (selected) {
                            Modifier.background(colors.surface, shape).border(1.dp, colors.border, shape)
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = SEGMENT_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style =
                    MyNoteTheme.textStyles.caption.copy(
                        fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
                    ),
                color = if (selected) colors.text else colors.text2,
                maxLines = 1,
            )
        }
    }
}

private val NAV_ICON: Dp = 21.dp
private val REASON_ICON: Dp = 14.dp
private val TOUCH: Dp = 48.dp
private val TRACK: Dp = 36.dp
private val TRACK_RADIUS: Dp = 10.dp
private val TRACK_INSET: Dp = 3.dp
private val SEGMENT_RADIUS: Dp = 8.dp
private val SEGMENT_PADDING: Dp = 14.dp
