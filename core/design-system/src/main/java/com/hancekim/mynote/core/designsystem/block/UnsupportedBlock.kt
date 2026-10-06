package com.hancekim.mynote.core.designsystem.block

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList

internal object UnsupportedBlockDefaults {
    val CornerRadius = 10.dp
    val DashOn = 4.dp
    val DashOff = 3.dp
    val BorderWidth = 1.dp
    val PaddingStart = 14.dp
    val PaddingEnd = 14.dp
    val PaddingTop = 12.dp
    val PaddingBottom = 4.dp
    val PaddingBottomNoButton = 12.dp
    val NoticeToText = 8.dp
    val TitleToBody = 6.dp
    val TextToButton = 4.dp
    val ButtonHeight = 44.dp
    val ButtonRadius = 12.dp
    val ButtonHorizontalPadding = 10.dp
    val ButtonStartOffset = (-10).dp
    val ChevronSize = 16.dp
    val ChevronGap = 4.dp
    val ButtonToPanel = 4.dp
    val PanelToNext = 8.dp
    val RawPanelRadius = 8.dp
    val RawPanelPaddingH = 12.dp
    val RawPanelPaddingV = 10.dp
    const val FIRST_LINES = 40
    const val MAX_LINES = 200
    const val MAX_CHARS = 10_000
    const val EXPAND_MILLIS = 240
    const val COLLAPSE_MILLIS = 180
    const val FADE_MILLIS = 160
    const val CHEVRON_MILLIS = 160
    const val CHEVRON_COLLAPSED = 90f
    const val CHEVRON_EXPANDED = -90f
    const val INDENT_EXTRA_CELLS = 2
}

@Composable
internal fun UnsupportedBlock(
    block: NoteContentBlock.Unsupported,
    expanded: Boolean,
    showAll: Boolean,
    onToggle: () -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val reduceMotion = rememberReduceMotion()
    val raw = block.raw
    val sizeMotion =
        if (reduceMotion) {
            Modifier
        } else {
            Modifier.animateContentSize(
                tween(
                    durationMillis =
                        if (expanded) {
                            UnsupportedBlockDefaults.EXPAND_MILLIS
                        } else {
                            UnsupportedBlockDefaults.COLLAPSE_MILLIS
                        },
                    easing = MyNoteEasing,
                ),
            )
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .dashedOutline(colors.border)
                .then(sizeMotion)
                .padding(
                    start = UnsupportedBlockDefaults.PaddingStart,
                    end = UnsupportedBlockDefaults.PaddingEnd,
                    top = UnsupportedBlockDefaults.PaddingTop,
                    bottom =
                        if (raw != null) {
                            UnsupportedBlockDefaults.PaddingBottom
                        } else {
                            UnsupportedBlockDefaults.PaddingBottomNoButton
                        },
                ),
    ) {
        UnsupportedNotice(block)
        if (raw != null) {
            Spacer(Modifier.height(UnsupportedBlockDefaults.TextToButton))
            QuietButton(
                text =
                    stringResource(
                        if (expanded) R.string.ds_unsupported_hide_raw else R.string.ds_unsupported_show_raw,
                    ),
                onClick = onToggle,
                chevronRotation = rememberChevronRotation(expanded, reduceMotion),
            )
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(UnsupportedBlockDefaults.FADE_MILLIS)),
                exit = ExitTransition.None,
            ) {
                RawPanel(raw = raw, showAll = showAll, onShowAll = onShowAll)
            }
        }
    }
}

@Composable
private fun UnsupportedNotice(block: NoteContentBlock.Unsupported) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        when (val kind = block.kind) {
            UnsupportedKind.BrokenNote -> {
                WholeNoteNotice(
                    title = stringResource(R.string.ds_unsupported_note_title),
                    body = stringResource(R.string.ds_unsupported_note_body),
                )
            }

            is UnsupportedKind.LimitExceeded -> {
                WholeNoteNotice(
                    title = stringResource(R.string.ds_unsupported_limit_title),
                    body =
                        stringResource(
                            R.string.ds_unsupported_limit_body,
                            kind.maxDepth,
                            kind.maxKilobytes,
                            kind.maxTableCells,
                        ),
                )
            }

            UnsupportedKind.UnknownType, UnsupportedKind.BrokenBlock -> {
                Text(
                    text = noticeText(kind == UnsupportedKind.UnknownType, hasSalvaged = block.salvaged != null),
                    style = styles.bodySmall,
                    color = colors.text2,
                )
                if (block.salvaged != null) {
                    Spacer(Modifier.height(UnsupportedBlockDefaults.NoticeToText))
                    Text(
                        text = block.salvaged,
                        style = styles.body,
                        color = colors.text2,
                    )
                }
            }
        }
    }
}

@Composable
private fun WholeNoteNotice(
    title: String,
    body: String,
) {
    Column {
        Text(
            text = title,
            style = MyNoteTheme.textStyles.cardTitle,
            color = MyNoteTheme.colors.text,
        )
        Spacer(Modifier.height(UnsupportedBlockDefaults.TitleToBody))
        Text(
            text = body,
            style = MyNoteTheme.textStyles.bodySmall,
            color = MyNoteTheme.colors.text2,
        )
    }
}

@Composable
private fun noticeText(
    unknownType: Boolean,
    hasSalvaged: Boolean,
): String =
    if (unknownType) {
        val lead =
            stringResource(
                if (hasSalvaged) R.string.ds_unsupported_unknown_text else R.string.ds_unsupported_unknown_empty,
            )
        "$lead ${stringResource(R.string.ds_unsupported_update_hint)}"
    } else {
        stringResource(if (hasSalvaged) R.string.ds_unsupported_broken_text else R.string.ds_unsupported_broken_empty)
    }

@Composable
private fun rememberChevronRotation(
    expanded: Boolean,
    reduceMotion: Boolean,
): () -> Float {
    val rotation by animateFloatAsState(
        targetValue =
            if (expanded) {
                UnsupportedBlockDefaults.CHEVRON_EXPANDED
            } else {
                UnsupportedBlockDefaults.CHEVRON_COLLAPSED
            },
        animationSpec =
            if (reduceMotion) {
                snap()
            } else {
                tween(UnsupportedBlockDefaults.CHEVRON_MILLIS, easing = MyNoteEasing)
            },
        label = "unsupportedChevron",
    )
    return { rotation }
}

@Composable
private fun RawPanel(
    raw: String,
    showAll: Boolean,
    onShowAll: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val first = remember(raw) { sliceRaw(raw, UnsupportedBlockDefaults.FIRST_LINES) }
    val second = remember(raw) { sliceRaw(raw, UnsupportedBlockDefaults.MAX_LINES) }
    val shown = if (showAll) second else first
    val moreLines = second.lines.size - first.lines.size
    val canShowMore = !showAll && first.hasMore && moreLines > 0
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(UnsupportedBlockDefaults.ButtonToPanel))
        RawLines(lines = shown.lines)
        when {
            canShowMore -> {
                Spacer(Modifier.height(UnsupportedBlockDefaults.TextToButton))
                QuietButton(
                    text = stringResource(R.string.ds_unsupported_more_lines, moreLines),
                    onClick = onShowAll,
                    chevronRotation = null,
                )
            }

            shown.hasMore -> {
                Spacer(Modifier.height(UnsupportedBlockDefaults.PanelToNext))
                Text(
                    text = stringResource(R.string.ds_unsupported_truncated, shown.lines.size),
                    style = styles.bodySmall,
                    color = colors.text2,
                )
                Spacer(Modifier.height(UnsupportedBlockDefaults.PanelToNext))
            }

            else -> {
                Spacer(Modifier.height(UnsupportedBlockDefaults.PanelToNext))
            }
        }
    }
}

@Composable
private fun RawLines(lines: ImmutableList<String>) {
    val colors = MyNoteTheme.colors
    val mono = MyNoteTheme.textStyles.mono
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cell: TextUnit =
        remember(mono, density) {
            with(density) {
                measurer
                    .measure("0", mono)
                    .size.width
                    .toSp()
            }
        }
    SelectionContainer {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .clip(RoundedCornerShape(UnsupportedBlockDefaults.RawPanelRadius))
                    .background(colors.surface2)
                    .padding(
                        horizontal = UnsupportedBlockDefaults.RawPanelPaddingH,
                        vertical = UnsupportedBlockDefaults.RawPanelPaddingV,
                    ),
        ) {
            lines.forEach { line ->
                val hang = cell * (leadingCells(line) + UnsupportedBlockDefaults.INDENT_EXTRA_CELLS)
                Text(
                    text = line,
                    style = mono.copy(textIndent = TextIndent(firstLine = 0.sp, restLine = hang)),
                    color = colors.text2,
                )
            }
        }
    }
}

@Composable
private fun QuietButton(
    text: String,
    onClick: () -> Unit,
    chevronRotation: (() -> Float)?,
) {
    val colors = MyNoteTheme.colors
    val indication = remember(colors.surface) { PressIndication(ButtonShape, pressColor = colors.surface) }
    Row(
        modifier =
            Modifier
                .offset(x = UnsupportedBlockDefaults.ButtonStartOffset)
                .heightIn(min = UnsupportedBlockDefaults.ButtonHeight)
                .clip(ButtonShape)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = UnsupportedBlockDefaults.ButtonHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(UnsupportedBlockDefaults.ChevronGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MyNoteTheme.textStyles.button.copy(fontWeight = FontWeight.W500),
            color = colors.text2,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        if (chevronRotation != null) {
            Icon(
                imageVector = MyNoteIcons.ChevronRight,
                contentDescription = null,
                tint = colors.text2,
                modifier =
                    Modifier
                        .size(UnsupportedBlockDefaults.ChevronSize)
                        .graphicsLayer { rotationZ = chevronRotation() },
            )
        }
    }
}

private val ButtonShape = RoundedCornerShape(UnsupportedBlockDefaults.ButtonRadius)

private fun Modifier.dashedOutline(color: Color): Modifier =
    drawWithCache {
        val stroke = UnsupportedBlockDefaults.BorderWidth.toPx()
        val radius = UnsupportedBlockDefaults.CornerRadius.toPx() - stroke / 2
        val effect =
            PathEffect.dashPathEffect(
                floatArrayOf(UnsupportedBlockDefaults.DashOn.toPx(), UnsupportedBlockDefaults.DashOff.toPx()),
            )
        val style = Stroke(width = stroke, pathEffect = effect)
        val topLeft = Offset(stroke / 2, stroke / 2)
        val outline = Size(size.width - stroke, size.height - stroke)
        onDrawBehind {
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = outline,
                cornerRadius = CornerRadius(radius, radius),
                style = style,
            )
        }
    }

private val previewTableRaw: String =
    buildString {
        appendLine("{")
        appendLine("  \"type\": \"table\",")
        appendLine("  \"fallback\": \"1. 클라이언트 → 서버: SYN\\n2. 서버 → 클라이언트: SYN+ACK\\n3. 클라이언트 → 서버: ACK\",")
        appendLine("  \"rows\": [")
        repeat(16) { index ->
            appendLine("    {")
            appendLine("      \"step\": ${index + 1},")
            val note = if (index == 4) "클라이언트는 서버의 응답을 기다리며 재전송 타이머를 돌린다. ".repeat(6).trim() else "응답을 기다린다"
            appendLine("      \"note\": \"$note\"")
            appendLine(if (index == 15) "    }" else "    },")
        }
        appendLine("  ]")
        append("}")
    }

private val previewLongRaw: String = (1..260).joinToString("\n") { "  \"line$it\": \"$it 번째 줄\"," }

private val previewSalvaged = "1. 클라이언트 → 서버: SYN\n2. 서버 → 클라이언트: SYN+ACK\n3. 클라이언트 → 서버: ACK"

private data class UnsupportedPreviewState(
    val block: NoteContentBlock.Unsupported,
    val expanded: Boolean = false,
    val showAll: Boolean = false,
)

private class UnsupportedBlockPreviewProvider : PreviewParameterProvider<UnsupportedPreviewState> {
    override val values =
        sequenceOf(
            UnsupportedPreviewState(
                NoteContentBlock.Unsupported(UnsupportedKind.UnknownType, previewSalvaged, previewTableRaw),
            ),
            UnsupportedPreviewState(
                NoteContentBlock.Unsupported(UnsupportedKind.BrokenBlock, null, "{\n  \"type\": \"heading\"\n}"),
            ),
            UnsupportedPreviewState(NoteContentBlock.Unsupported(UnsupportedKind.UnknownType, null, null)),
            UnsupportedPreviewState(
                NoteContentBlock.Unsupported(UnsupportedKind.UnknownType, previewSalvaged, previewTableRaw),
                expanded = true,
            ),
            UnsupportedPreviewState(
                NoteContentBlock.Unsupported(UnsupportedKind.BrokenBlock, null, previewLongRaw),
                expanded = true,
                showAll = true,
            ),
            UnsupportedPreviewState(NoteContentBlock.Unsupported(UnsupportedKind.BrokenNote, null, previewTableRaw)),
            UnsupportedPreviewState(
                NoteContentBlock.Unsupported(UnsupportedKind.BrokenNote, null, previewTableRaw),
                expanded = true,
            ),
            UnsupportedPreviewState(
                NoteContentBlock.Unsupported(
                    UnsupportedKind.LimitExceeded(maxDepth = 8, maxKilobytes = 64, maxTableCells = 128),
                    null,
                    previewLongRaw,
                ),
            ),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "모르는 종류 · 글자"
            1 -> "깨짐 · 글자 없음"
            2 -> "원문 없음"
            3 -> "펼침 1단"
            4 -> "펼침 2단 · 잘림"
            5 -> "노트 전체"
            6 -> "노트 전체 · 펼침"
            else -> "한도 넘음"
        }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun UnsupportedBlockPreview(
    @PreviewParameter(UnsupportedBlockPreviewProvider::class) state: UnsupportedPreviewState,
) {
    MyNoteTheme {
        UnsupportedBlock(
            block = state.block,
            expanded = state.expanded,
            showAll = state.showAll,
            onToggle = {},
            onShowAll = {},
            modifier =
                Modifier
                    .background(MyNoteTheme.colors.bg)
                    .padding(16.dp),
        )
    }
}
