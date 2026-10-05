package com.hancekim.mynote.feature.notes.screen.connect

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.MyNoteSnackbarHost
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.connect.ConnectLinkRejection
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.ConfirmKind
import com.hancekim.mynote.feature.notes.element.ConfirmUi
import com.hancekim.mynote.feature.notes.element.ConnectBackAction
import com.hancekim.mynote.feature.notes.element.ConnectNotice
import com.hancekim.mynote.feature.notes.element.ConnectPlace
import com.hancekim.mynote.feature.notes.element.ConnectStep
import com.hancekim.mynote.feature.notes.element.LinkCheck
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.TokenDraft
import com.hancekim.mynote.feature.notes.element.TokenFailureKind
import com.hancekim.mynote.feature.notes.element.TokenReason
import com.hancekim.mynote.feature.notes.element.canConnect
import com.hancekim.mynote.feature.notes.element.connectBackAction
import com.hancekim.mynote.feature.notes.element.connectNoticeRes
import com.hancekim.mynote.feature.notes.element.linkReasonRes
import com.hancekim.mynote.feature.notes.element.tokenFailureRes
import com.hancekim.mynote.feature.notes.screen.CONTENT_MAX_WIDTH
import com.hancekim.mynote.feature.notes.screen.PrimaryButton
import com.hancekim.mynote.feature.notes.screen.QuietButton
import com.hancekim.mynote.feature.notes.shell.NotesTopBar
import kotlinx.coroutines.launch
import com.hancekim.mynote.core.designsystem.R as DsR

@Composable
internal fun ConnectRoute(
    place: ConnectPlace,
    hasConnection: Boolean,
    drafts: com.hancekim.mynote.feature.notes.element.NotesDrafts,
    snackbarHostState: SnackbarHostState,
    onNotesIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = drafts.connectToken
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browserMissing = keepWordsResource(R.string.notes_token_browser_missing)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.confirm != null) {
        if (state.confirm != null) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    LaunchedEffect(state.step) {
        if (state.step == ConnectStep.Done) draft.clear()
        if (state.step == ConnectStep.Probing) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    LaunchedEffect(state.resetCount) {
        if (state.resetCount > 0) draft.clear()
    }

    val backAction = connectBackAction(place, state.step)
    BackHandler(enabled = place == ConnectPlace.Root || backAction != ConnectBackAction.PopScreen) {
        when (backAction) {
            ConnectBackAction.CancelProbe -> viewModel.onIntent(ConnectIntent.CancelProbe)
            ConnectBackAction.ShowExitHint -> onNotesIntent(NotesIntent.Back)
            ConnectBackAction.Ignore, ConnectBackAction.PopScreen -> Unit
        }
    }

    val title =
        keepWordsResource(
            if (hasConnection) R.string.notes_connect_change_title else R.string.notes_connect_attach_title,
        )
    val content =
        @Composable {
            ConnectScreen(
                state = state,
                place = place,
                title = title,
                draft = draft,
                onIntent = viewModel::onIntent,
                onBack = {
                    when (backAction) {
                        ConnectBackAction.CancelProbe -> viewModel.onIntent(ConnectIntent.CancelProbe)
                        ConnectBackAction.Ignore -> Unit
                        else -> onNotesIntent(NotesIntent.Back)
                    }
                },
                onCreateToken = {
                    if (!openTokenPage(context)) scope.launch { snackbarHostState.showSnackbar(browserMissing) }
                },
                modifier = modifier,
            )
        }
    if (place == ConnectPlace.Root) {
        Box(Modifier.fillMaxSize()) {
            content()
            MyNoteSnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    } else {
        content()
    }

    state.confirm?.let { confirm ->
        ConnectConfirmDialog(
            confirm = confirm,
            onDismiss = { viewModel.onIntent(ConnectIntent.DismissConfirm) },
            onConfirm = { viewModel.onIntent(ConnectIntent.AcceptConfirm) },
        )
    }
}

@Composable
internal fun ConnectScreen(
    state: ConnectUiState,
    place: ConnectPlace,
    title: String,
    draft: TokenDraft,
    onIntent: (ConnectIntent) -> Unit,
    onBack: () -> Unit,
    onCreateToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val locked = state.step != ConnectStep.Editing
    val panel = state.tokenPanel
    val canSubmit = !locked && state.linkCheck.canConnect && (panel == null || !draft.isBlank)
    val submit = {
        if (canSubmit) onIntent(ConnectIntent.Submit(if (panel != null) draft.secret() else null))
    }
    val short = adaptive.heightClass == MyNoteHeightClass.Compact
    val gap = if (short) SHORT_GAP else GAP
    val topPad =
        when {
            place == ConnectPlace.Stacked -> STACKED_TOP
            short -> SHORT_ROOT_TOP
            adaptive.useModalDrawer -> COMPACT_ROOT_TOP
            else -> WIDE_ROOT_TOP
        }

    val buttonInView = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom, panel != null) { if (imeBottom > 0) buttonInView.bringIntoView() }

    Surface(modifier = modifier.fillMaxSize(), color = colors.bg) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    if (place == ConnectPlace.Root) {
                        WindowInsets.safeDrawing
                    } else {
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    },
                ),
        ) {
            if (place == ConnectPlace.Stacked) {
                NotesTopBar(
                    title = title,
                    showBack = true,
                    bookmarked = null,
                    onMenu = {},
                    onBack = onBack,
                    onRefresh = {},
                    onToggleBookmark = {},
                    showRefresh = false,
                    backEnabled = state.step == ConnectStep.Editing || state.step == ConnectStep.Probing,
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = CONTENT_MAX_WIDTH)
                            .fillMaxWidth()
                            .padding(start = SIDE, end = SIDE, top = topPad, bottom = BOTTOM_PAD),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    ConnectIntro(place = place, title = title)
                    LinkBlock(
                        state = state,
                        locked = locked,
                        onLinkChange = { onIntent(ConnectIntent.LinkChanged(it)) },
                        onGo = submit,
                        onCancel = { onIntent(ConnectIntent.CancelProbe) },
                        showProbing = panel == null,
                    )
                    state.notice?.let { NoticeBox(text = keepWordsResource(connectNoticeRes(it))) }
                    if (panel != null) {
                        TokenCard(
                            panel = panel,
                            draft = draft,
                            probing = state.step == ConnectStep.Probing,
                            locked = locked,
                            onCreateToken = onCreateToken,
                            onDone = submit,
                            onCancel = { onIntent(ConnectIntent.CancelProbe) },
                        )
                    }
                    if (state.step == ConnectStep.Syncing || state.step == ConnectStep.Done) {
                        SyncProgressCard()
                    } else {
                        PrimaryButton(
                            text = keepWordsResource(R.string.notes_connect_action),
                            enabled = canSubmit,
                            onClick = submit,
                            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(buttonInView),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectIntro(
    place: ConnectPlace,
    title: String,
) {
    val colors = MyNoteTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (place == ConnectPlace.Root) {
            Text(
                text = keepWordsResource(R.string.notes_connect_title),
                style = MyNoteTheme.textStyles.title,
                color = colors.text,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            text = keepWordsResource(R.string.notes_connect_intro),
            style = MyNoteTheme.textStyles.body,
            color = colors.bodyText,
        )
    }
}

@Composable
private fun LinkBlock(
    state: ConnectUiState,
    locked: Boolean,
    onLinkChange: (String) -> Unit,
    onGo: () -> Unit,
    onCancel: () -> Unit,
    showProbing: Boolean,
) {
    val colors = MyNoteTheme.colors
    val check = state.linkCheck
    val rejected = check as? LinkCheck.Rejected
    val warn = rejected != null && rejected.reason != ConnectLinkRejection.Empty
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(text = keepWordsResource(R.string.notes_connect_link_label))
        LinkField(
            value = state.link,
            onValueChange = onLinkChange,
            locked = locked,
            warn = warn,
            onGo = onGo,
        )
        when {
            state.step == ConnectStep.Probing && showProbing -> {
                VerifyingLine(
                    text = keepWordsResource(R.string.notes_connect_probing),
                    trailing = {
                        QuietButton(
                            text = keepWordsResource(DsR.string.ds_action_cancel),
                            onClick = onCancel,
                        )
                    },
                )
            }

            rejected != null -> {
                Text(
                    text = keepWordsResource(linkReasonRes(rejected.reason)),
                    style = MyNoteTheme.textStyles.bodySmall,
                    color = if (warn) colors.warnText else colors.text3,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            check is LinkCheck.Found -> {
                Text(
                    text =
                        if (check.branch == null) {
                            monoAnnotated(R.string.notes_connect_found_default, check.repoName, check.folder)
                        } else {
                            monoAnnotated(R.string.notes_connect_found, check.repoName, check.branch, check.folder)
                        },
                    style = MyNoteTheme.textStyles.bodySmall,
                    color = colors.text2,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TokenCard(
    panel: TokenPanelUi,
    draft: TokenDraft,
    probing: Boolean,
    locked: Boolean,
    onCreateToken: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(CARD_RADIUS)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ConnectIcons.Lock,
                    contentDescription = null,
                    tint = colors.text2,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text =
                        keepWordsResource(
                            if (panel.reason == TokenReason.Limit) {
                                R.string.notes_token_err_limit_title
                            } else {
                                R.string.notes_connect_token_title
                            },
                        ),
                    style = MyNoteTheme.textStyles.cardTitle,
                    color = colors.text,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text =
                        keepWordsResource(
                            if (panel.reason == TokenReason.Limit) {
                                R.string.notes_token_err_limit_desc
                            } else {
                                R.string.notes_connect_token_desc
                            },
                        ),
                    style = MyNoteTheme.textStyles.bodySmall,
                    color = colors.text2,
                )
            }
        }
        TokenHowTo()
        Row { TokenCreateButton(onClick = onCreateToken) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel(text = keepWordsResource(R.string.notes_connect_token_label))
            TokenField(
                draft = draft,
                locked = locked,
                warn = panel.failure != null,
                onDone = onDone,
            )
            panel.failure?.let { NoticeBox(text = keepWordsResource(tokenFailureRes(it))) }
            if (draft.isClassic) NoticeBox(text = keepWordsResource(R.string.notes_token_classic_warn))
            if (probing) {
                VerifyingLine(
                    text = keepWordsResource(R.string.notes_token_verifying),
                    trailing = {
                        QuietButton(
                            text = keepWordsResource(DsR.string.ds_action_cancel),
                            onClick = onCancel,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SyncProgressCard() {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(CARD_RADIUS)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .semantics(mergeDescendants = true) {},
    ) {
        ProgressBar(Modifier.fillMaxWidth())
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = keepWordsResource(R.string.notes_connect_syncing_title),
                style = MyNoteTheme.textStyles.cardTitle,
                color = colors.text,
            )
            Text(
                text = keepWordsResource(R.string.notes_connect_syncing_desc),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
            )
        }
    }
}

private val SIDE: Dp = 16.dp
private val BOTTOM_PAD: Dp = 24.dp
private val STACKED_TOP: Dp = 12.dp
private val SHORT_ROOT_TOP: Dp = 20.dp
private val COMPACT_ROOT_TOP: Dp = 28.dp
private val WIDE_ROOT_TOP: Dp = 72.dp
private val GAP: Dp = 24.dp
private val SHORT_GAP: Dp = 16.dp
private val CARD_RADIUS: Dp = 14.dp

private val previewFound = LinkCheck.Found("octo-learner/android-notes", null, "notes")
private const val PREVIEW_LINK = "https://github.com/octo-learner/android-notes"

@Composable
private fun PreviewHost(
    windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
    heightClass: MyNoteHeightClass = MyNoteHeightClass.Medium,
    fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    content: @Composable () -> Unit,
) {
    MyNoteTheme(windowClass = windowClass, heightClass = heightClass, fontScale = fontScale, content = content)
}

@Preview(name = "연결 · 빈 · 루트", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectEmptyPreview() {
    PreviewHost {
        ConnectScreen(ConnectUiState(), ConnectPlace.Root, "", TokenDraft(), {}, {}, {})
    }
}

@Preview(name = "연결 · 알아본 결과", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectFoundPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "연결 · 확인 중", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectProbingPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, step = ConnectStep.Probing),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "연결 · 토큰 필요", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun ConnectTokenNeededPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                tokenPanel = TokenPanelUi(TokenReason.Needed),
            ),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "토큰 · classic 경고", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1100)
@Composable
private fun ConnectTokenClassicPreview() {
    val draft = remember { TokenDraft().also { it.text = "ghp_example" } }
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                tokenPanel = TokenPanelUi(TokenReason.Needed),
            ),
            ConnectPlace.Root,
            "",
            draft,
            {},
            {},
            {},
        )
    }
}

@Preview(name = "토큰 · 확인 중", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun ConnectTokenVerifyingPreview() {
    val draft = remember { TokenDraft().also { it.text = "github_pat_example" } }
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                step = ConnectStep.Probing,
                tokenPanel = TokenPanelUi(TokenReason.Needed),
            ),
            ConnectPlace.Root,
            "",
            draft,
            {},
            {},
            {},
        )
    }
}

@Preview(name = "토큰 · 거부", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun ConnectTokenRejectedPreview() {
    val draft = remember { TokenDraft().also { it.text = "github_pat_example" } }
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                tokenPanel = TokenPanelUi(TokenReason.Needed, TokenFailureKind.Rejected),
            ),
            ConnectPlace.Root,
            "",
            draft,
            {},
            {},
            {},
        )
    }
}

@Preview(name = "토큰 · 저장소 안 보임", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun ConnectTokenNotVisiblePreview() {
    val draft = remember { TokenDraft().also { it.text = "github_pat_example" } }
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                tokenPanel = TokenPanelUi(TokenReason.Needed, TokenFailureKind.NotVisible),
            ),
            ConnectPlace.Root,
            "",
            draft,
            {},
            {},
            {},
        )
    }
}

@Preview(name = "토큰 · 목록 접근 불가", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun ConnectTokenNoListPreview() {
    val draft = remember { TokenDraft().also { it.text = "github_pat_example" } }
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                tokenPanel = TokenPanelUi(TokenReason.Needed, TokenFailureKind.NoList),
            ),
            ConnectPlace.Root,
            "",
            draft,
            {},
            {},
            {},
        )
    }
}

@Preview(name = "토큰 · 한도 권유", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 1000)
@Composable
private fun ConnectTokenLimitPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                tokenPanel = TokenPanelUi(TokenReason.Limit),
            ),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "토큰 · 한도 권유 · 852", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 852, heightDp = 800)
@Composable
private fun ConnectTokenLimitWidePreview() {
    PreviewHost(windowClass = MyNoteWindowClass.Medium) {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                tokenPanel = TokenPanelUi(TokenReason.Limit),
            ),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "실패 · 링크 이유", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectLinkReasonPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = "http://github.com/octo-learner/android-notes",
                linkCheck = LinkCheck.Rejected(ConnectLinkRejection.NotHttps),
            ),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "연결 · 실패 · 목록 없음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectNoListPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, notice = ConnectNotice.NoList),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "실패 · 오프라인", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectOfflinePreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, notice = ConnectNotice.Offline),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "첫 동기화 중 · 루트", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectSyncingPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, step = ConnectStep.Syncing),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "쌓은 · 연결", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectStackedPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound),
            ConnectPlace.Stacked,
            "저장소 바꾸기",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "쌓은 · 연결 · 852", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 852, heightDp = 800)
@Composable
private fun ConnectStackedWidePreview() {
    PreviewHost(windowClass = MyNoteWindowClass.Medium) {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound),
            ConnectPlace.Stacked,
            "저장소 바꾸기",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "쌓은 · 첫 동기화 중", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectStackedSyncingPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, step = ConnectStep.Syncing),
            ConnectPlace.Stacked,
            "저장소 바꾸기",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "연결 · 빈 · 가로", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 800, heightDp = 360)
@Composable
private fun ConnectLandscapePreview() {
    PreviewHost(windowClass = MyNoteWindowClass.Medium, heightClass = MyNoteHeightClass.Compact) {
        ConnectScreen(ConnectUiState(), ConnectPlace.Root, "", TokenDraft(), {}, {}, {})
    }
}

@Preview(
    name = "연결 · 알아본 결과 · 글자 ×1.3",
    showBackground = true,
    backgroundColor = 0xFF0E1217,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun ConnectLargeFontPreview() {
    PreviewHost(fontScale = MyNoteFontScale.Large) {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound),
            ConnectPlace.Root,
            "",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "대화상자 · 바꾸기 · 이어받기", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun ConnectConfirmAdoptPreview() {
    PreviewHost {
        ConnectConfirmContent(
            confirm = ConfirmUi(ConfirmKind.Adopt, notes = 5, bookmarks = 0, dropsToken = false),
            onDismiss = {},
            onConfirm = {},
        )
    }
}
