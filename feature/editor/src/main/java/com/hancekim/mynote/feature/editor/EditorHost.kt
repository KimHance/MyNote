package com.hancekim.mynote.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.QuietButton
import com.hancekim.mynote.core.ui.screen.SolidDestructiveButton
import com.hancekim.mynote.core.ui.screen.connect.GlassDialog
import com.hancekim.mynote.core.ui.screen.connect.ProgressBar
import com.hancekim.mynote.core.designsystem.R as DsR
import com.hancekim.mynote.core.ui.R as UiR

/**
 * 편집기 기능이 셸 위에 띄우는 것 — 시스템 파일 고르기 런처 · 가져오기 대화상자 · 나가기 확인 · 지우기 확인.
 *
 * 셸이 시작 분기 · 창 등급 분기 위에서 늘 컴포즈한다. 런처가 사라지는 자리(대화상자 안)에 있으면 파일 고르기 결과가 돌아올 곳이
 * 없다(brief 표면 제약). 대화상자는 각자 창이라 셸 뒤로가기 7단과 겹치지 않는다. 모양은 디자인 local-notes 해부(r1 README §3.2 ·
 * README-2 §3.1 · §3.3) — 유리 판 틀은 `:core:ui` 의 [GlassDialog] 다.
 */
@Composable
fun EditorHost(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
) {
    val currentIntent by rememberUpdatedState(onIntent)
    // 받는 파일은 표시 이름 확장자로 판정한다 — `.md` 를 octet-stream · text/plain 으로 주는 제공자가 많다(LN-6 (b))
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            currentIntent(EditorIntent.FileChosen(uri?.toString()))
        }
    val seenPick = remember { intArrayOf(state.filePickRequestId) }
    LaunchedEffect(state.filePickRequestId) {
        if (state.filePickRequestId != seenPick[0]) {
            seenPick[0] = state.filePickRequestId
            launcher.launch(arrayOf(ANY_FILE))
        }
    }

    if (state.importDialog != ImportDialogUi.Closed) {
        EditorDialog(onDismiss = { onIntent(EditorIntent.DismissImport) }) {
            ImportChooser(reading = state.importDialog == ImportDialogUi.Reading, onIntent = onIntent)
        }
    }
    if (state.exitConfirm) {
        val keep = { onIntent(EditorIntent.ExitAnswered(discard = false)) }
        EditorDialog(onDismiss = keep) {
            ConfirmContent(
                title = stringResource(R.string.editor_exit_title),
                body = stringResource(R.string.editor_exit_body),
                titleGap = EXIT_TITLE_GAP,
                buttonGap = EXIT_BUTTON_GAP,
                dismissLabel = stringResource(R.string.editor_exit_keep),
                confirmLabel = stringResource(R.string.editor_exit_discard),
                onDismiss = keep,
                onConfirm = { onIntent(EditorIntent.ExitAnswered(discard = true)) },
            )
        }
    }
    if (state.deleteConfirmNoteId != null) {
        val cancel = { onIntent(EditorIntent.DeleteAnswered(confirm = false)) }
        EditorDialog(onDismiss = cancel) {
            ConfirmContent(
                title = stringResource(R.string.editor_delete_title),
                body = stringResource(R.string.editor_delete_body),
                titleGap = DELETE_TITLE_GAP,
                buttonGap = DELETE_BUTTON_GAP,
                dismissLabel = stringResource(DsR.string.ds_action_cancel),
                confirmLabel = stringResource(UiR.string.ui_action_delete),
                onDismiss = cancel,
                onConfirm = { onIntent(EditorIntent.DeleteAnswered(confirm = true)) },
            )
        }
    }
}

/** 창 하나 + 유리 판. 안쪽 24(높이 Compact 의 위아래 20) · 넘치면 판 안에서 내용과 버튼 줄이 함께 스크롤한다. */
@Composable
private fun EditorDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val vertical = if (MyNoteTheme.adaptive.heightClass == MyNoteHeightClass.Compact) 20.dp else 24.dp
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassDialog(padding = PaddingValues(horizontal = 24.dp, vertical = vertical)) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

/** 나가기 확인(LN-10) · 지우기 확인(LN-11) — 제목 · 본문 · 오른쪽 정렬 버튼 줄(조용한 버튼 · 파괴적 확인 = wrong 면). */
@Composable
private fun ConfirmContent(
    title: String,
    body: String,
    titleGap: Dp,
    buttonGap: Dp,
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Column(modifier = modifier) {
        DialogTitle(title)
        Spacer(Modifier.height(titleGap))
        Text(text = body, style = MyNoteTheme.textStyles.bodySmall, color = colors.text2)
        Spacer(Modifier.height(buttonGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietButton(text = dismissLabel, onClick = onDismiss)
            SolidDestructiveButton(text = confirmLabel, onClick = onConfirm)
        }
    }
}

/**
 * 가져오기 그릇(LN-6 · 디자인 정하지 않은 것 23 — 시작 전에 고를 것 하나라 대화상자). 길 두 줄(파일 열기 · 붙여넣기) + 공유 안내 +
 * `취소`. 고른 파일을 읽는 동안(디자인 C18)은 길 · 안내 자리에 `파일을 읽는 중입니다` + 진행 막대이고 버튼은 `취소` 만이다.
 */
@Composable
private fun ImportChooser(
    reading: Boolean,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val gap = if (MyNoteTheme.adaptive.heightClass == MyNoteHeightClass.Compact) 12.dp else 16.dp
    Column(modifier = modifier) {
        DialogTitle(stringResource(R.string.editor_import_title))
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.editor_import_body), style = styles.bodySmall, color = colors.text2)
        Spacer(Modifier.height(gap))
        if (reading) {
            Text(
                text = stringResource(R.string.editor_import_reading),
                style = styles.bodySmall,
                color = colors.bodyText,
            )
            Spacer(Modifier.height(8.dp))
            ProgressBar(modifier = Modifier.fillMaxWidth())
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportWay(
                    icon = MyNoteIcons.Import,
                    name = stringResource(R.string.editor_import_file),
                    hint = stringResource(R.string.editor_import_file_hint),
                    onClick = { onIntent(EditorIntent.ChooseFile) },
                )
                ImportWay(
                    icon = MyNoteIcons.Paste,
                    name = stringResource(R.string.editor_import_paste),
                    hint = stringResource(R.string.editor_import_paste_hint),
                    onClick = { onIntent(EditorIntent.ChoosePaste) },
                )
            }
            Spacer(Modifier.height(gap))
            Text(text = stringResource(R.string.editor_import_share), style = styles.caption, color = colors.text3)
        }
        Spacer(Modifier.height(if (gap == 16.dp) 20.dp else gap))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            QuietButton(
                text = stringResource(DsR.string.ds_action_cancel),
                onClick = { onIntent(EditorIntent.DismissImport) },
            )
        }
    }
}

/** 가져오기 길 한 줄 — 높이 64 · surface2 + 1dp border · 반경 12. 왼쪽 아이콘 21 · 이름 · 설명(각 한 줄 말줄임) · 오른쪽 셰브론. */
@Composable
private fun ImportWay(
    icon: ImageVector,
    name: String,
    hint: String,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val shape = RoundedCornerShape(WAY_RADIUS)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.border) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(WAY_HEIGHT)
                .clip(shape)
                .background(colors.surface2)
                .border(1.dp, colors.border, shape)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.text2, modifier = Modifier.size(WAY_ICON))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = name,
                style = styles.cardTitle,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hint,
                style = styles.caption,
                color = colors.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = MyNoteIcons.ChevronRight,
            contentDescription = null,
            tint = colors.text3,
            modifier = Modifier.size(CHEVRON),
        )
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        style = MyNoteTheme.textStyles.screenTitle,
        color = MyNoteTheme.colors.text,
        modifier = Modifier.semantics { heading() },
    )
}

private const val ANY_FILE = "*/*"
private val WAY_HEIGHT: Dp = 64.dp
private val WAY_RADIUS: Dp = 12.dp
private val WAY_ICON: Dp = 21.dp
private val CHEVRON: Dp = 16.dp
private val EXIT_TITLE_GAP: Dp = 10.dp
private val EXIT_BUTTON_GAP: Dp = 22.dp
private val DELETE_TITLE_GAP: Dp = 8.dp
private val DELETE_BUTTON_GAP: Dp = 20.dp

@Preview(name = "가져오기 · 길 고르기", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun ImportChooserPreview() {
    MyNoteTheme { GlassDialog(padding = PaddingValues(24.dp)) { ImportChooser(reading = false, onIntent = {}) } }
}

@Preview(name = "가져오기 · 읽는 중", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun ImportReadingPreview() {
    MyNoteTheme { GlassDialog(padding = PaddingValues(24.dp)) { ImportChooser(reading = true, onIntent = {}) } }
}

@Preview(name = "지우기 확인", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun DeleteConfirmPreview() {
    MyNoteTheme {
        GlassDialog(padding = PaddingValues(24.dp)) {
            ConfirmContent(
                title = stringResource(R.string.editor_delete_title),
                body = stringResource(R.string.editor_delete_body),
                titleGap = DELETE_TITLE_GAP,
                buttonGap = DELETE_BUTTON_GAP,
                dismissLabel = stringResource(DsR.string.ds_action_cancel),
                confirmLabel = stringResource(UiR.string.ui_action_delete),
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}
