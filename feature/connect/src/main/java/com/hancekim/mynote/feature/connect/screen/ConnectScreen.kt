package com.hancekim.mynote.feature.connect.screen

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
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
import com.hancekim.mynote.core.domain.connect.TemplateRepo
import com.hancekim.mynote.core.ui.element.TokenDraft
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import com.hancekim.mynote.core.ui.element.TokenReason
import com.hancekim.mynote.core.ui.element.tokenFailureRes
import com.hancekim.mynote.core.ui.screen.CONTENT_MAX_WIDTH
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.core.ui.screen.QuietButton
import com.hancekim.mynote.core.ui.screen.connect.ConnectIcons
import com.hancekim.mynote.core.ui.screen.connect.KeepWordsText
import com.hancekim.mynote.core.ui.screen.connect.NoticeBox
import com.hancekim.mynote.core.ui.screen.connect.ProgressBar
import com.hancekim.mynote.core.ui.screen.connect.TOKEN_PAGE_URL
import com.hancekim.mynote.core.ui.screen.connect.TokenCreateButton
import com.hancekim.mynote.core.ui.screen.connect.TokenField
import com.hancekim.mynote.core.ui.screen.connect.TokenHowTo
import com.hancekim.mynote.core.ui.screen.connect.VerifyingLine
import com.hancekim.mynote.core.ui.screen.connect.monoAnnotated
import com.hancekim.mynote.core.ui.screen.connect.openExternal
import com.hancekim.mynote.core.ui.shell.NotesTopBar
import com.hancekim.mynote.feature.connect.R
import com.hancekim.mynote.feature.connect.element.ConfirmKind
import com.hancekim.mynote.feature.connect.element.ConfirmUi
import com.hancekim.mynote.feature.connect.element.ConnectBackAction
import com.hancekim.mynote.feature.connect.element.ConnectNotice
import com.hancekim.mynote.feature.connect.element.ConnectSection
import com.hancekim.mynote.feature.connect.element.ConnectStep
import com.hancekim.mynote.feature.connect.element.LinkCheck
import com.hancekim.mynote.feature.connect.element.NoticeWays
import com.hancekim.mynote.feature.connect.element.canConnect
import com.hancekim.mynote.feature.connect.element.connectBackAction
import com.hancekim.mynote.feature.connect.element.connectNoticeRes
import com.hancekim.mynote.feature.connect.element.connectSectionsOf
import com.hancekim.mynote.feature.connect.element.linkReasonRes
import com.hancekim.mynote.feature.connect.element.noticeWays
import kotlinx.coroutines.launch
import com.hancekim.mynote.core.designsystem.R as DsR
import com.hancekim.mynote.core.ui.R as UiR

@Composable
fun ConnectRoute(
    hasConnection: Boolean,
    drafts: com.hancekim.mynote.core.ui.element.NotesDrafts,
    snackbarHostState: SnackbarHostState,
    onShellBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = drafts.connectToken
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browserMissing = stringResource(UiR.string.ui_token_browser_missing)
    val openOrNotify: (String) -> Unit = { url ->
        if (!openExternal(context, url)) scope.launch { snackbarHostState.showSnackbar(browserMissing) }
    }
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
    LaunchedEffect(state.resetCount) { drafts.consumeReset(state.resetCount) }

    val backAction = connectBackAction(state.step)
    BackHandler(enabled = backAction != ConnectBackAction.PopScreen) {
        when (backAction) {
            ConnectBackAction.CancelProbe -> viewModel.onIntent(ConnectIntent.CancelProbe)
            ConnectBackAction.Ignore, ConnectBackAction.PopScreen -> Unit
        }
    }

    val title =
        stringResource(
            if (hasConnection) R.string.connect_change_title else R.string.connect_attach_title,
        )
    ConnectScreen(
        state = state,
        title = title,
        draft = draft,
        onIntent = viewModel::onIntent,
        onBack = {
            when (backAction) {
                ConnectBackAction.CancelProbe -> viewModel.onIntent(ConnectIntent.CancelProbe)
                ConnectBackAction.Ignore -> Unit
                ConnectBackAction.PopScreen -> onShellBack()
            }
        },
        onCreateToken = { openOrNotify(TOKEN_PAGE_URL) },
        modifier = modifier,
        onOpenCreate = { state.template?.let { openOrNotify(it.generateUrl) } },
        onOpenGuide = { state.template?.let { openOrNotify(it.introUrl) } },
    )

    state.confirm?.let { confirm ->
        ConnectConfirmDialog(
            confirm = confirm,
            onDismiss = { viewModel.onIntent(ConnectIntent.DismissConfirm) },
            onConfirm = { viewModel.onIntent(ConnectIntent.AcceptConfirm) },
        )
    }
}

@Composable
fun ConnectScreen(
    state: ConnectUiState,
    title: String,
    draft: TokenDraft,
    onIntent: (ConnectIntent) -> Unit,
    onBack: () -> Unit,
    onCreateToken: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCreate: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val locked = state.step != ConnectStep.Editing
    val panel = state.tokenPanel
    val canSubmit = !locked && state.linkCheck.canConnect && (panel == null || !draft.isBlank)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val submit = {
        if (canSubmit) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            onIntent(ConnectIntent.Submit(if (panel != null) draft.secret() else null))
        }
    }
    val short = adaptive.heightClass == MyNoteHeightClass.Compact
    val gap = if (short) SHORT_GAP else GAP

    val buttonInView = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom, panel != null) { if (imeBottom > 0) buttonInView.bringIntoView() }

    val linkSectionContent =
        @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                ConnectIntroText()
                LinkBlock(
                    state = state,
                    locked = locked,
                    onLinkChange = { onIntent(ConnectIntent.LinkChanged(it)) },
                    onGo = submit,
                    onCancel = { onIntent(ConnectIntent.CancelProbe) },
                    showProbing = panel == null,
                )
                state.notice?.let { notice ->
                    val withWays = noticeWays(notice, state.template != null) == NoticeWays.CreateAndGuide
                    NoticeBox(
                        text = stringResource(connectNoticeRes(notice)),
                        secondLine = if (withWays) stringResource(R.string.connect_err_no_list_fresh) else null,
                        ways =
                            if (withWays) {
                                (
                                    {
                                        NoticeWayButtons(
                                            onCreate = onOpenCreate,
                                            onGuide = onOpenGuide,
                                        )
                                    }
                                )
                            } else {
                                null
                            },
                    )
                }
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
                        text = stringResource(R.string.connect_action),
                        enabled = canSubmit,
                        onClick = submit,
                        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(buttonInView),
                    )
                }
            }
        }

    Surface(modifier = modifier.fillMaxSize(), color = colors.bg) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                ),
        ) {
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
            var guideOpen by rememberSaveable { mutableStateOf(false) }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = CONTENT_MAX_WIDTH)
                            .fillMaxWidth()
                            .padding(start = SIDE, end = SIDE, top = TOP_PAD, bottom = BOTTOM_PAD),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    connectSectionsOf(state.entryShown).forEach { section ->
                        when (section) {
                            ConnectSection.Link -> {
                                linkSectionContent()
                            }

                            ConnectSection.Entry -> {
                                EntryDivider()
                                EntrySection(
                                    onCreate = onOpenCreate,
                                    expanded = guideOpen,
                                    onToggle = { guideOpen = !guideOpen },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectIntroText() {
    KeepWordsText(
        text = stringResource(R.string.connect_intro),
        style = MyNoteTheme.textStyles.body,
        color = MyNoteTheme.colors.bodyText,
    )
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
        FieldLabel(text = stringResource(R.string.connect_link_label))
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
                    text = stringResource(R.string.connect_probing),
                    trailing = {
                        QuietButton(
                            text = stringResource(DsR.string.ds_action_cancel),
                            onClick = onCancel,
                        )
                    },
                )
            }

            rejected != null -> {
                KeepWordsText(
                    text = stringResource(linkReasonRes(rejected.reason)),
                    style = MyNoteTheme.textStyles.bodySmall,
                    color = if (warn) colors.warnText else colors.text3,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            check is LinkCheck.Found -> {
                KeepWordsText(
                    text =
                        if (check.branch == null) {
                            monoAnnotated(R.string.connect_found_default, check.repoName, check.folder)
                        } else {
                            monoAnnotated(R.string.connect_found, check.repoName, check.branch, check.folder)
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
                KeepWordsText(
                    text =
                        stringResource(
                            if (panel.reason == TokenReason.Limit) {
                                UiR.string.ui_token_err_limit_title
                            } else {
                                R.string.connect_token_title
                            },
                        ),
                    style = MyNoteTheme.textStyles.cardTitle,
                    color = colors.text,
                    modifier = Modifier.semantics { heading() },
                )
                KeepWordsText(
                    text =
                        stringResource(
                            if (panel.reason == TokenReason.Limit) {
                                R.string.connect_token_err_limit_desc
                            } else {
                                R.string.connect_token_desc
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
            FieldLabel(text = stringResource(R.string.connect_token_label))
            TokenField(
                draft = draft,
                locked = locked,
                warn = panel.failure != null,
                onDone = onDone,
            )
            panel.failure?.let { NoticeBox(text = stringResource(tokenFailureRes(it))) }
            if (draft.isClassic) NoticeBox(text = stringResource(UiR.string.ui_token_classic_warn))
            if (probing) {
                VerifyingLine(
                    text = stringResource(UiR.string.ui_token_verifying),
                    trailing = {
                        QuietButton(
                            text = stringResource(DsR.string.ds_action_cancel),
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
            KeepWordsText(
                text = stringResource(R.string.connect_syncing_title),
                style = MyNoteTheme.textStyles.cardTitle,
                color = colors.text,
            )
            KeepWordsText(
                text = stringResource(R.string.connect_syncing_desc),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
            )
        }
    }
}

private val SIDE: Dp = 16.dp
private val BOTTOM_PAD: Dp = 24.dp
private val TOP_PAD: Dp = 12.dp
private val GAP: Dp = 24.dp
private val SHORT_GAP: Dp = 16.dp
private val CARD_RADIUS: Dp = 14.dp

private val previewFound = LinkCheck.Found("octo-learner/android-notes", null, "notes")
private const val PREVIEW_LINK = "https://github.com/octo-learner/android-notes"
private val previewTemplate = TemplateRepo("octo-learner", "notes-template")

@Composable
private fun PreviewHost(
    windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
    heightClass: MyNoteHeightClass = MyNoteHeightClass.Medium,
    fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    content: @Composable () -> Unit,
) {
    MyNoteTheme(windowClass = windowClass, heightClass = heightClass, fontScale = fontScale, content = content)
}

@Preview(name = "연결 · 빈", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectEmptyPreview() {
    PreviewHost {
        ConnectScreen(ConnectUiState(), "저장소 연결", TokenDraft(), {}, {}, {})
    }
}

@Preview(name = "연결 · 알아본 결과", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectFoundPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound),
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
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
            "저장소 연결",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "첫 동기화 중", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectSyncingPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, step = ConnectStep.Syncing),
            "저장소 연결",
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
        ConnectScreen(ConnectUiState(), "저장소 연결", TokenDraft(), {}, {}, {})
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
            "저장소 연결",
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

@Preview(name = "입구 · 빈", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectEntryEmptyPreview() {
    PreviewHost {
        ConnectScreen(ConnectUiState(template = previewTemplate), "저장소 연결", TokenDraft(), {}, {}, {})
    }
}

@Preview(name = "입구 · 알아본 결과", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectEntryFoundPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, template = previewTemplate),
            "저장소 연결",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "입구 · 빈 · 852", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 852, heightDp = 800)
@Composable
private fun ConnectEntryWidePreview() {
    PreviewHost(windowClass = MyNoteWindowClass.Medium) {
        ConnectScreen(ConnectUiState(template = previewTemplate), "저장소 연결", TokenDraft(), {}, {}, {})
    }
}

@Preview(name = "입구 · 빈 · 가로", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 800, heightDp = 360)
@Composable
private fun ConnectEntryLandscapePreview() {
    PreviewHost(windowClass = MyNoteWindowClass.Medium, heightClass = MyNoteHeightClass.Compact) {
        ConnectScreen(ConnectUiState(template = previewTemplate), "저장소 연결", TokenDraft(), {}, {}, {})
    }
}

@Preview(name = "입구 · 빈 · 글자 ×1.3", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectEntryLargeFontPreview() {
    PreviewHost(fontScale = MyNoteFontScale.Large) {
        ConnectScreen(ConnectUiState(template = previewTemplate), "저장소 연결", TokenDraft(), {}, {}, {})
    }
}

@Preview(name = "입구 · 쌓은 · 연결", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectEntryStackedPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, template = previewTemplate),
            "저장소 바꾸기",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "입구 · 쌓은 · 852", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 852, heightDp = 800)
@Composable
private fun ConnectEntryStackedWidePreview() {
    PreviewHost(windowClass = MyNoteWindowClass.Medium) {
        ConnectScreen(
            ConnectUiState(link = PREVIEW_LINK, linkCheck = previewFound, template = previewTemplate),
            "저장소 바꾸기",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(
    name = "RC-9 · 목록 없음 · 두 길",
    showBackground = true,
    backgroundColor = 0xFF0E1217,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun ConnectNoListWaysPreview() {
    PreviewHost {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                notice = ConnectNotice.NoList,
                template = previewTemplate,
            ),
            "저장소 연결",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "RC-9 · 목록 없음 · 가로", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 800, heightDp = 360)
@Composable
private fun ConnectNoListWaysLandscapePreview() {
    PreviewHost(windowClass = MyNoteWindowClass.Medium, heightClass = MyNoteHeightClass.Compact) {
        ConnectScreen(
            ConnectUiState(
                link = PREVIEW_LINK,
                linkCheck = previewFound,
                notice = ConnectNotice.NoList,
                template = previewTemplate,
            ),
            "저장소 연결",
            TokenDraft(),
            {},
            {},
            {},
        )
    }
}
