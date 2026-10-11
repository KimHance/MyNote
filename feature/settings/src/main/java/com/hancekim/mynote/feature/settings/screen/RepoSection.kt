package com.hancekim.mynote.feature.settings.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.riseIn
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.ui.element.LastSync
import com.hancekim.mynote.core.ui.element.NotesDrafts
import com.hancekim.mynote.core.ui.element.RepoCardUi
import com.hancekim.mynote.core.ui.element.RepoDot
import com.hancekim.mynote.core.ui.element.SyncFailureKind
import com.hancekim.mynote.core.ui.element.TokenDraft
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import com.hancekim.mynote.core.ui.element.syncFailureKindRes
import com.hancekim.mynote.core.ui.element.tokenFailureRes
import com.hancekim.mynote.core.ui.screen.DestructiveButton
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.core.ui.screen.QuietButton
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.core.ui.screen.connect.ConnectIcons
import com.hancekim.mynote.core.ui.screen.connect.KeepWordsText
import com.hancekim.mynote.core.ui.screen.connect.NoticeBox
import com.hancekim.mynote.core.ui.screen.connect.TOKEN_PAGE_URL
import com.hancekim.mynote.core.ui.screen.connect.TokenCreateButton
import com.hancekim.mynote.core.ui.screen.connect.TokenField
import com.hancekim.mynote.core.ui.screen.connect.TokenHowTo
import com.hancekim.mynote.core.ui.screen.connect.VerifyingLine
import com.hancekim.mynote.core.ui.screen.connect.monoAnnotated
import com.hancekim.mynote.core.ui.screen.connect.openExternal
import com.hancekim.mynote.feature.settings.R
import com.hancekim.mynote.feature.settings.SettingsIntent
import com.hancekim.mynote.feature.settings.SettingsUiState
import com.hancekim.mynote.feature.settings.element.TokenEditState
import com.hancekim.mynote.feature.settings.element.maskedTokenTail
import kotlinx.coroutines.launch
import com.hancekim.mynote.core.designsystem.R as DsR
import com.hancekim.mynote.core.ui.R as UiR

@Composable
internal fun RepoSection(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    drafts: NotesDrafts,
    snackbarHostState: SnackbarHostState,
) {
    val card = uiState.repoCard
    val edit = uiState.tokenEdit
    val connection = uiState.connectedRepo
    val colors = MyNoteTheme.colors

    BackHandler(enabled = edit.editing) { onIntent(SettingsIntent.CancelTokenEdit) }
    LaunchedEffect(edit.editing) { if (!edit.editing) drafts.settingsToken.clear() }

    SettingsCard(modifier = Modifier.animateContentSize()) {
        uiState.disconnectDialog?.let { dialog ->
            DisconnectDialog(
                notes = dialog.notes,
                bookmarks = dialog.bookmarks,
                onDismiss = { onIntent(SettingsIntent.DismissDisconnect) },
                onConfirm = { onIntent(SettingsIntent.ConfirmDisconnect) },
            )
        }
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBox(icon = ConnectIcons.Folder)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeepWordsText(
                    text = stringResource(R.string.settings_repo_card_title),
                    style =
                        MyNoteTheme.textStyles.cardTitle
                            .copy(fontWeight = FontWeight.W500),
                    color = colors.text,
                )
                RepoStatusLine(card = card, uiState = uiState)
            }
        }
        if (connection == null) {
            Box(Modifier.padding(start = ACTION_INDENT, end = 16.dp, bottom = 16.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.settings_repo_connect),
                    onClick = { onIntent(SettingsIntent.OpenConnect) },
                )
            }
        } else {
            RepoLines(connection = connection, last = uiState.lastSync)
            HorizontalDivider(color = colors.border)
            RepoActions(locked = edit.locksRepoActions, onIntent = onIntent)
            HorizontalDivider(color = colors.border)
            TokenBlock(
                uiState = uiState,
                card = card,
                edit = edit,
                drafts = drafts,
                onIntent = onIntent,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

@Composable
private fun RepoStatusLine(
    card: RepoCardUi,
    uiState: SettingsUiState,
) {
    val colors = MyNoteTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val tail = maskedTokenTail(uiState.maskedToken)
        val tailDescription = tail?.let { stringResource(R.string.settings_token_tail_a11y, it) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            StatusDot(card.dot)
            KeepWordsText(
                text = stringResource(card.label),
                style =
                    MyNoteTheme.textStyles.caption
                        .copy(fontWeight = FontWeight.W500),
                color = colors.bodyText,
                maxLines = 1,
            )
            if (uiState.hasToken && uiState.connectedRepo != null) {
                KeepWordsText(
                    text = uiState.maskedToken.orEmpty(),
                    style = MyNoteTheme.textStyles.monoSmall,
                    color = colors.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.semantics {
                            if (tailDescription != null) contentDescription = tailDescription
                        },
                )
            }
        }
        card.description?.let {
            KeepWordsText(
                text = stringResource(it),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
            )
        }
    }
}

@Composable
private fun StatusDot(dot: RepoDot) {
    val colors = MyNoteTheme.colors
    Box(
        Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(
                when (dot) {
                    RepoDot.Ok -> colors.correct
                    RepoDot.Warn -> colors.warn
                    RepoDot.Muted -> colors.text3
                },
            ),
    )
}

@Composable
private fun RepoLines(
    connection: RepoConnection,
    last: LastSync,
) {
    val colors = MyNoteTheme.colors
    Column(
        modifier = Modifier.padding(start = ACTION_INDENT, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KeepWordsText(
                text = stringResource(R.string.settings_repo_label),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text2,
            )
            KeepWordsText(
                text =
                    connection.indexPath.substringBeforeLast('/', "").ifEmpty { "/" }.let { folder ->
                        if (connection.branch.isBlank()) {
                            monoAnnotated(R.string.settings_repo_value_default, connection.fullName, folder)
                        } else {
                            monoAnnotated(R.string.settings_repo_value, connection.fullName, connection.branch, folder)
                        }
                    },
                style = MyNoteTheme.textStyles.monoSmall,
                color = colors.bodyText,
            )
        }
        when (last) {
            LastSync.None -> {
                Unit
            }

            LastSync.Succeeded -> {
                LastSyncLine(RepoDot.Ok, stringResource(R.string.settings_repo_last_ok))
            }

            is LastSync.Failed -> {
                LastSyncLine(
                    RepoDot.Warn,
                    stringResource(R.string.settings_repo_last_fail, stringResource(syncFailureKindRes(last.kind))),
                )
            }
        }
    }
}

@Composable
private fun LastSyncLine(
    dot: RepoDot,
    text: String,
) {
    val colors = MyNoteTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        StatusDot(dot)
        KeepWordsText(
            text = text,
            style =
                MyNoteTheme.textStyles.caption
                    .copy(fontWeight = FontWeight.W500),
            color = if (dot == RepoDot.Warn) colors.warnText else colors.bodyText,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepoActions(
    locked: Boolean,
    onIntent: (SettingsIntent) -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = ACTION_INDENT, end = 16.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SecondaryButton(
            text = stringResource(R.string.settings_repo_change),
            enabled = !locked,
            onClick = { onIntent(SettingsIntent.OpenConnect) },
        )
        DestructiveButton(
            text = stringResource(R.string.settings_repo_disconnect),
            enabled = !locked,
            onClick = { onIntent(SettingsIntent.RequestDisconnect) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenBlock(
    uiState: SettingsUiState,
    card: RepoCardUi,
    edit: TokenEditState,
    drafts: NotesDrafts,
    onIntent: (SettingsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val colors = MyNoteTheme.colors
    Column(
        modifier = Modifier.padding(start = ACTION_INDENT, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KeepWordsText(
                text = stringResource(R.string.settings_token_title),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text2,
            )
            if (uiState.hasToken) {
                KeepWordsText(
                    text = uiState.maskedToken.orEmpty(),
                    style = MyNoteTheme.textStyles.monoSmall,
                    color = colors.bodyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                card.tokenNone?.let {
                    KeepWordsText(
                        text = stringResource(it),
                        style = MyNoteTheme.textStyles.caption,
                        color = colors.text3,
                    )
                }
            }
        }
        if (edit.editing) {
            TokenEditor(
                uiState = uiState,
                edit = edit,
                draft = drafts.settingsToken,
                onIntent = onIntent,
                snackbar = snackbarHostState,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (uiState.hasToken) {
                    val change = stringResource(R.string.settings_token_change_a11y)
                    val changeModifier = Modifier.semantics { contentDescription = change }
                    if (card.showBadge) {
                        PrimaryButton(
                            text = stringResource(R.string.settings_token_change),
                            onClick = { onIntent(SettingsIntent.StartTokenEdit) },
                            modifier = changeModifier,
                        )
                    } else {
                        SecondaryButton(
                            text = stringResource(R.string.settings_token_change),
                            onClick = { onIntent(SettingsIntent.StartTokenEdit) },
                            modifier = changeModifier,
                        )
                    }
                    DestructiveButton(
                        text = stringResource(R.string.settings_token_clear),
                        onClick = { onIntent(SettingsIntent.ClearToken) },
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.settings_token_enter),
                        onClick = { onIntent(SettingsIntent.StartTokenEdit) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenEditor(
    uiState: SettingsUiState,
    edit: TokenEditState,
    draft: TokenDraft,
    onIntent: (SettingsIntent) -> Unit,
    snackbar: SnackbarHostState,
) {
    val colors = MyNoteTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browserMissing = stringResource(UiR.string.ui_token_browser_missing)
    val focusRequester = remember { FocusRequester() }
    val fieldInView = remember { BringIntoViewRequester() }
    val reduceMotion = rememberReduceMotion()
    val imeBottom =
        WindowInsets.ime
            .getBottom(LocalDensity.current)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(edit.verifying) {
        if (edit.verifying) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    LaunchedEffect(imeBottom) { if (imeBottom > 0) fieldInView.bringIntoView() }
    val save = {
        if (!draft.isBlank && !edit.verifying) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            onIntent(SettingsIntent.SaveToken(draft.secret()))
        }
    }

    Column(
        modifier =
            Modifier
                .then(if (reduceMotion) Modifier else Modifier.riseIn())
                .bringIntoViewRequester(fieldInView),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TokenHowTo()
        Row {
            TokenCreateButton(
                onClick = {
                    if (!openExternal(
                            context,
                            TOKEN_PAGE_URL,
                        )
                    ) {
                        scope.launch { snackbar.showSnackbar(browserMissing) }
                    }
                },
            )
        }
        TokenField(
            draft = draft,
            locked = edit.verifying,
            warn = edit.failure != null,
            onDone = save,
            focusRequester = focusRequester,
        )
        KeepWordsText(
            text = stringResource(R.string.settings_token_local_only),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
        )
        edit.failure?.let { NoticeBox(text = stringResource(tokenFailureRes(it))) }
        if (draft.isClassic) NoticeBox(text = stringResource(UiR.string.ui_token_classic_warn))
        if (edit.verifying) VerifyingLine(text = stringResource(UiR.string.ui_token_verifying))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.hasToken && !edit.verifying) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    DestructiveButton(
                        text = stringResource(R.string.settings_token_clear),
                        onClick = { onIntent(SettingsIntent.ClearToken) },
                    )
                }
            }
            QuietButton(
                text = stringResource(DsR.string.ds_action_cancel),
                onClick = { onIntent(SettingsIntent.CancelTokenEdit) },
            )
            PrimaryButton(
                text =
                    stringResource(
                        if (edit.verifying) R.string.settings_token_verifying_action else DsR.string.ds_action_save,
                    ),
                enabled = !draft.isBlank && !edit.verifying,
                onClick = save,
            )
        }
    }
}

private val ACTION_INDENT: Dp = 70.dp

private val previewRepo = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
private val previewConnected = ConnectionState.Connected(SourceId("s1"), previewRepo)

private fun previewState(
    connection: ConnectionState? = previewConnected,
    hasToken: Boolean = false,
    last: LastSync = LastSync.Succeeded,
    edit: TokenEditState = TokenEditState.Viewing,
) = SettingsUiState(
    connection = connection,
    hasToken = hasToken,
    maskedToken = if (hasToken) "github_pat_••••••••a1b2" else null,
    lastSync = last,
    tokenEdit = edit,
)

@Composable
private fun SectionPreview(
    state: SettingsUiState,
    drafts: NotesDrafts = remember { NotesDrafts() },
    windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
    fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
) {
    MyNoteTheme(windowClass = windowClass, fontScale = fontScale) {
        Column(Modifier.background(MyNoteTheme.colors.bg).padding(16.dp)) {
            RepoSection(
                uiState = state,
                onIntent = {},
                drafts = drafts,
                snackbarHostState = remember { SnackbarHostState() },
            )
        }
    }
}

@Preview(name = "카드 · 공개", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun CardPublicPreview() = SectionPreview(previewState())

@Preview(name = "카드 · 공개 · 852", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 560, heightDp = 640)
@Composable
private fun CardPublicWidePreview() = SectionPreview(previewState(), windowClass = MyNoteWindowClass.Medium)

@Preview(name = "카드 · 토큰 사용", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun CardTokenPreview() = SectionPreview(previewState(hasToken = true))

@Preview(name = "카드 · 토큰 확인 필요", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun CardCheckTokenPreview() =
    SectionPreview(
        previewState(
            connection = ConnectionState.Connected(SourceId("s1"), previewRepo.copy(isPublic = false)),
            hasToken = true,
            last = LastSync.Failed(SyncFailureKind.TokenRejected),
        ),
    )

@Preview(name = "카드 · 연결 없음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 400)
@Composable
private fun CardNonePreview() = SectionPreview(previewState(connection = ConnectionState.None))

@Preview(name = "카드 · 결과 없음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun CardNoResultPreview() = SectionPreview(previewState(last = LastSync.None))

@Preview(name = "카드 · 결과 인터넷", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun CardOfflinePreview() =
    SectionPreview(previewState(last = LastSync.Failed(SyncFailureKind.Offline)))

@Preview(name = "카드 · 결과 한도", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun CardLimitPreview() =
    SectionPreview(previewState(last = LastSync.Failed(SyncFailureKind.RateLimited)))

@Preview(name = "카드 · 토큰 편집 열림", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 900)
@Composable
private fun CardEditPreview() =
    SectionPreview(previewState(edit = TokenEditState(editing = true)))

@Preview(name = "편집기 · 확인 중", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun EditorVerifyingPreview() {
    val drafts = remember { NotesDrafts().also { it.settingsToken.text = "github_pat_example" } }
    SectionPreview(
        previewState(hasToken = true, edit = TokenEditState(editing = true, verifying = true)),
        drafts = drafts,
    )
}

@Preview(name = "편집기 · 실패", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun EditorFailedPreview() {
    val drafts = remember { NotesDrafts().also { it.settingsToken.text = "github_pat_example" } }
    SectionPreview(
        previewState(
            hasToken = true,
            edit = TokenEditState(editing = true, failure = TokenFailureKind.Rejected),
        ),
        drafts = drafts,
    )
}

@Preview(name = "편집기 · classic 경고", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun EditorClassicPreview() {
    val drafts = remember { NotesDrafts().also { it.settingsToken.text = "ghp_example" } }
    SectionPreview(previewState(hasToken = true, edit = TokenEditState(editing = true)), drafts = drafts)
}

@Preview(
    name = "편집기 · 큰 글자 ×1.3",
    showBackground = true,
    backgroundColor = 0xFF0E1217,
    widthDp = 360,
    heightDp = 1100,
    fontScale = 1.3f,
)
@Composable
private fun EditorLargePreview() {
    val drafts = remember { NotesDrafts().also { it.settingsToken.text = "github_pat_example" } }
    SectionPreview(previewState(hasToken = true, edit = TokenEditState(editing = true)), drafts = drafts)
}
