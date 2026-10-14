package com.hancekim.mynote.core.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication =
        remember(colors, shape) { PressIndication(shape, colors.accent, colors.accentPressed, scale = 0.98f) }
    Box(
        modifier =
            modifier
                .height(BUTTON_HEIGHT)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .background(colors.accent)
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = indication,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ButtonIcon(leadingIcon, colors.bg)
            ButtonLabel(text = text, color = colors.bg, weight = FontWeight.W700)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.border) }
    Box(
        modifier =
            modifier
                .height(BUTTON_HEIGHT)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .background(colors.surface2)
                .border(1.dp, colors.border, shape)
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = indication,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ButtonIcon(leadingIcon, colors.text2)
            ButtonLabel(text = text, color = colors.text, weight = FontWeight.W500)
            ButtonIcon(trailingIcon, colors.text2)
        }
    }
}

@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication =
        remember(colors, shape) { PressIndication(shape, pressColor = colors.wrong.copy(alpha = 0.10f)) }
    Box(
        modifier =
            modifier
                .height(BUTTON_HEIGHT)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = indication,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonLabel(text = text, color = colors.wrong, weight = FontWeight.W500)
    }
}

@Composable
internal fun SolidDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication =
        remember(
            colors,
            shape,
        ) { PressIndication(shape, colors.wrong, colors.wrong.copy(alpha = 0.82f), scale = 0.98f) }
    Box(
        modifier =
            modifier
                .height(BUTTON_HEIGHT)
                .clip(shape)
                .background(colors.wrong)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonLabel(text = text, color = colors.bg, weight = FontWeight.W700)
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.border) }
    Box(
        modifier =
            modifier
                .height(36.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = indication,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 12.dp)
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (trailingIcon == null) {
            ButtonLabel(text = text, color = colors.text2, weight = FontWeight.W500)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ButtonLabel(text = text, color = colors.text2, weight = FontWeight.W500)
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** 버튼 라벨 곁의 15dp 아이콘 — 장식이라 낭독하지 않는다(라벨이 이름). 없으면 자리도 없다. */
@Composable
private fun ButtonIcon(
    icon: ImageVector?,
    tint: Color,
) {
    if (icon == null) return
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(15.dp),
    )
}

@Composable
internal fun ButtonLabel(
    text: String,
    color: Color,
    weight: FontWeight,
) {
    Text(
        text = text,
        style = MyNoteTheme.textStyles.button.copy(fontWeight = weight),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

private val BUTTON_HEIGHT: Dp = 40.dp

internal const val DISABLED_ALPHA = 0.4f
