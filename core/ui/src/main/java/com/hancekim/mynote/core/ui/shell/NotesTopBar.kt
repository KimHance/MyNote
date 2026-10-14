package com.hancekim.mynote.core.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.R
import com.hancekim.mynote.core.designsystem.R as DsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopBar(
    title: String,
    showBack: Boolean,
    bookmarked: Boolean?,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    showRefresh: Boolean = true,
    backEnabled: Boolean = true,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val colors = MyNoteTheme.colors
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MyNoteTheme.textStyles.sectionTitle,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        expandedHeight = MyNoteTheme.adaptive.headerHeight,
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack, enabled = backEnabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(DsR.string.ds_action_back),
                        tint = colors.text,
                        modifier = Modifier.graphicsLayer { alpha = if (backEnabled) 1f else DISABLED_BACK_ALPHA },
                    )
                }
            } else {
                val menuLabel = stringResource(R.string.ui_action_note_list)
                IconButton(onClick = onMenu, modifier = Modifier.semantics { contentDescription = menuLabel }) {
                    Text(
                        text = "☰",
                        style = MyNoteTheme.textStyles.sectionTitle,
                        color = colors.text,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        },
        actions = {
            if (bookmarked != null) {
                BookmarkToggle(bookmarked = bookmarked, onToggle = onToggleBookmark)
                Spacer(Modifier.width(ACTION_GAP))
            }
            // 기기 노트 리더(local-notes 22B) — 새로고침 자리에 고치기 · 지우기(손그림 22dp · bodyText). 보임은 셸이 정한다(null = 없음).
            // 지우기는 바로 지우지 않고 편집기 기능의 확인 대화상자를 연다(LN-11 · 결정 7)
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = MyNoteIcons.Edit,
                        contentDescription = stringResource(R.string.ui_action_edit),
                        tint = colors.bodyText,
                        modifier = Modifier.size(ACTION_ICON),
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = MyNoteIcons.Delete,
                        contentDescription = stringResource(R.string.ui_action_delete),
                        tint = colors.bodyText,
                        modifier = Modifier.size(ACTION_ICON),
                    )
                }
            }
            if (showRefresh) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.ui_action_refresh),
                        tint = colors.accent,
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.bg,
                titleContentColor = colors.text,
                navigationIconContentColor = colors.text,
                actionIconContentColor = colors.accent,
            ),
        modifier = modifier,
    )
}

@Composable
private fun BookmarkToggle(
    bookmarked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(TOGGLE_RADIUS)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.surface2) }
    val reduceMotion = rememberReduceMotion()
    var bounces by remember { mutableIntStateOf(0) }
    val toggle =
        throttledProcess {
            if (!bookmarked) bounces++
            onToggle()
        }
    val tint =
        animateColorAsState(
            targetValue = if (bookmarked) colors.accent else colors.bodyText,
            animationSpec = tween(TINT_MILLIS),
            label = "bookmarkTint",
        )
    val scale = remember { Animatable(1f) }
    LaunchedEffect(bounces) {
        if (bounces > 0 && !reduceMotion) {
            scale.snapTo(1f)
            scale.animateTo(BOUNCE_SCALE, tween(BOUNCE_MILLIS / 2, easing = BounceEasing))
            scale.animateTo(1f, tween(BOUNCE_MILLIS / 2, easing = BounceEasing))
        }
    }
    val actionLabel = stringResource(R.string.ui_bookmark_action)
    val stateLabel =
        stringResource(if (bookmarked) R.string.ui_bookmark_state_on else R.string.ui_bookmark_state_off)

    Box(
        modifier =
            Modifier
                .size(width = TOGGLE_SIZE, height = minOf(TOGGLE_SIZE, MyNoteTheme.adaptive.headerHeight))
                .clip(shape)
                .toggleable(
                    value = bookmarked,
                    interactionSource = null,
                    indication = indication,
                    role = Role.Checkbox,
                    onValueChange = { toggle() },
                ).semantics {
                    contentDescription = actionLabel
                    stateDescription = stateLabel
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (bookmarked) MyNoteIcons.BookmarkFilled else MyNoteIcons.Bookmark,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier =
                Modifier
                    .size(TOGGLE_ICON)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        colorFilter = ColorFilter.tint(tint.value)
                    },
        )
    }
}

private const val DISABLED_BACK_ALPHA = 0.4f
private val TOGGLE_SIZE = 44.dp
private val TOGGLE_ICON = 22.dp
private val ACTION_ICON = 22.dp
private val TOGGLE_RADIUS = 12.dp
private val ACTION_GAP = 2.dp
private const val TINT_MILLIS = 160
private const val BOUNCE_MILLIS = 240
private const val BOUNCE_SCALE = 1.2f
private val BounceEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Preview(name = "TopBar · 리더 · 북마크 꺼짐", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 400)
@Composable
private fun NotesTopBarReaderPreview() {
    MyNoteTheme {
        NotesTopBar(
            title = "TCP 3-way handshake",
            showBack = false,
            bookmarked = false,
            onMenu = {},
            onBack = {},
            onRefresh = {},
            onToggleBookmark = {},
        )
    }
}

@Preview(name = "TopBar · 리더 · 북마크 켜짐", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 400)
@Composable
private fun NotesTopBarBookmarkedPreview() {
    MyNoteTheme {
        NotesTopBar(
            title = "TCP 3-way handshake",
            showBack = false,
            bookmarked = true,
            onMenu = {},
            onBack = {},
            onRefresh = {},
            onToggleBookmark = {},
        )
    }
}

@Preview(name = "TopBar · 홈", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 400)
@Composable
private fun NotesTopBarHomePreview() {
    MyNoteTheme {
        NotesTopBar(
            title = stringResource(R.string.ui_app_title),
            showBack = false,
            bookmarked = null,
            onMenu = {},
            onBack = {},
            onRefresh = {},
            onToggleBookmark = {},
        )
    }
}

@Preview(name = "TopBar · 기기 노트 리더(22B)", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun NotesTopBarDeviceNotePreview() {
    MyNoteTheme {
        NotesTopBar(
            title = "코루틴 취소와 예외 처리 정리",
            showBack = true,
            bookmarked = false,
            onMenu = {},
            onBack = {},
            onRefresh = {},
            onToggleBookmark = {},
            showRefresh = false,
            onEdit = {},
            onDelete = {},
        )
    }
}
