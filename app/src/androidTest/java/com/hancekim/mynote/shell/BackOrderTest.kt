package com.hancekim.mynote.shell

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import com.hancekim.mynote.core.designsystem.block.UnsupportedKind
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DraftCheck
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.key.activeNoteId
import com.hancekim.mynote.core.ui.element.NotesDrafts
import com.hancekim.mynote.feature.editor.EditorIntent
import com.hancekim.mynote.feature.editor.EditorSession
import com.hancekim.mynote.feature.editor.EditorUiState
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.quiz.QuizIntent
import com.hancekim.mynote.feature.quiz.QuizOverlayState
import com.hancekim.mynote.feature.quiz.QuizQuestionUi
import com.hancekim.mynote.feature.quiz.QuizUiState
import com.hancekim.mynote.feature.settings.SettingsIntent
import com.hancekim.mynote.feature.settings.SettingsUiState
import com.hancekim.mynote.feature.settings.element.TokenEditState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import com.hancekim.mynote.core.designsystem.R as DsR
import com.hancekim.mynote.core.ui.R as UiR

/**
 * 뒤로가기 7단(FM-10)과 창 등급 전환(FM-4)을 상태 없는 셸로 지킨다.
 *
 * 몸통은 [ShellHarness] 의 말(장면 · 드로어 · 뒤로 · 기록된 콜백)만 쓴다. ViewModel 나누기 · 등록 틀 · 셸 이동은
 * 하네스만 바꾸고 몸통은 같은 이름으로 통과해야 한다.
 */
class BackOrderTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var shell: ShellHarness

    @Before
    fun setUp() {
        shell = ShellHarness(rule)
    }

    @Test
    fun `FM-10 열린 드로어가 먼저 닫힌다`() {
        shell.show(ShellScene())
        shell.openDrawer()
        assertTrue("드로어가 열려 있어야 한다", shell.listPanelShown())

        shell.back()

        assertFalse("드로어가 닫혀야 한다", shell.listPanelShown())
        assertEquals(emptyList<Recorded>(), shell.recorded())
    }

    @Test
    fun `FM-10 예측 뒤로가기로 열린 드로어가 닫힌다`() {
        shell.show(ShellScene(stack = READER_STACK))
        shell.openDrawer()

        shell.predictiveBack()

        assertFalse("드로어가 닫혀야 한다", shell.listPanelShown())
        assertEquals(emptyList<Recorded>(), shell.recorded())
    }

    @Test
    fun `FM-10 예측 뒤로가기 진행 중에는 열린 드로어가 M3 연출로 움직인다`() {
        shell.show(ShellScene(stack = READER_STACK))
        shell.openDrawer()
        val resting = shell.listPanelTop()

        shell.startPredictiveBack()

        val moving = shell.listPanelTop()
        assertTrue("진행 중 드로어 내용이 움직여야 한다(쉴 때 $resting · 진행 중 $moving)", moving > resting + MOVED_PX)
        assertTrue("확정 전에는 드로어가 열려 있어야 한다", shell.listPanelShown())

        shell.finishPredictiveBack()

        assertFalse("드로어가 닫혀야 한다", shell.listPanelShown())
        assertEquals(emptyList<Recorded>(), shell.recorded())
    }

    @Test
    fun `FM-10 퀴즈 오버레이가 닫힌다`() {
        for (quiz in listOf(ShellQuiz.Setup, ShellQuiz.Solving)) {
            shell.show(ShellScene(stack = READER_STACK, quiz = quiz))

            shell.back()

            assertEquals("$quiz", listOf(Recorded.DismissQuiz), shell.recorded())
            shell.clearRecorded()
        }
    }

    @Test
    fun `FM-10 전환 중 뒤로가기는 삼킨다`() {
        shell.show(ShellScene(stack = READER_STACK, settled = false))

        shell.back()

        assertEquals(emptyList<Recorded>(), shell.recorded())
    }

    @Test
    fun `FM-10 예측 뒤로가기 도중 전환 중이면 삼킨다`() {
        shell.show(ShellScene(stack = READER_STACK, settled = false))

        shell.predictiveBack()

        assertEquals(emptyList<Recorded>(), shell.recorded())
        assertTrue("리더가 그대로 보여야 한다", shell.readerShown())
    }

    @Test
    fun `FM-10 쌓인 화면이 pop 된다`() {
        shell.show(ShellScene(stack = READER_STACK))

        shell.back()

        assertEquals(listOf(Recorded.Back), shell.recorded())
    }

    @Test
    fun `FM-10 여는 중인 드로어가 닫힌다`() {
        shell.show(ShellScene())
        shell.startOpeningDrawer()

        shell.back()

        assertFalse("드로어가 닫혀야 한다", shell.listPanelShown())
        assertEquals(emptyList<Recorded>(), shell.recorded())
    }

    @Test
    fun `FM-10 펼친 고정 패널이 접힌다`() {
        shell.show(ShellScene(window = ShellWindow.Wide))
        assertTrue("고정 패널이 펼쳐져 있어야 한다", shell.listPanelShown())

        shell.back()

        assertFalse("고정 패널이 접혀야 한다", shell.listPanelShown())
        assertEquals(emptyList<Recorded>(), shell.recorded())
    }

    @Test
    fun `FM-10 루트에서 뒤로가기는 인텐트를 내고 종료 요청에 종료 콜백이 불린다`() {
        shell.show(ShellScene())

        shell.back()
        assertEquals(listOf(Recorded.Back), shell.recorded())

        shell.raiseExitRequest()
        assertEquals(listOf(Recorded.Back, Recorded.Exit), shell.recorded())
    }

    @Test
    fun `FM-10 겹치면 순위가 높은 쪽만 일어난다`() {
        // ① + ④
        shell.show(ShellScene(stack = READER_STACK))
        shell.openDrawer()
        shell.back()
        assertFalse("① + ④ 드로어가 닫혀야 한다", shell.listPanelShown())
        assertEquals("① + ④", emptyList<Recorded>(), shell.recorded())

        // ② + ④
        shell.show(ShellScene(stack = READER_STACK, quiz = ShellQuiz.Solving))
        shell.back()
        assertEquals("② + ④", listOf(Recorded.DismissQuiz), shell.recorded())
        shell.clearRecorded()

        // ⑥ + ⑦ — 패널을 접은 뒤에야 종료 쪽으로 간다
        shell.show(ShellScene(window = ShellWindow.Wide))
        shell.back()
        assertFalse("⑥ + ⑦ 고정 패널이 접혀야 한다", shell.listPanelShown())
        assertEquals("⑥ + ⑦", emptyList<Recorded>(), shell.recorded())
        shell.back()
        assertEquals("⑥ 다음 ⑦", listOf(Recorded.Back), shell.recorded())
    }

    @Test
    fun `FM-10 토큰 편집 중 뒤로가기는 편집을 취소한다`() {
        shell.show(ShellScene(stack = SETTINGS_STACK, tokenEditing = true))

        shell.back()

        assertEquals(listOf(Recorded.CancelTokenEdit), shell.recorded())
    }

    @Test
    fun `FM-10 LN-10 바뀐 편집기의 뒤로가기는 pop 대신 나가기 확인을 묻는다`() {
        shell.show(ShellScene(stack = EDITOR_STACK, editor = ShellEditor.Dirty))

        shell.back()

        assertEquals(listOf(Recorded.CloseEditor), shell.recorded())
    }

    @Test
    fun `FM-10 LN-10 바뀐 편집기는 예측 뒤로가기도 미리보기 없이 묻는다`() {
        shell.show(ShellScene(stack = EDITOR_STACK, editor = ShellEditor.Dirty))

        shell.predictiveBack()

        assertEquals(listOf(Recorded.CloseEditor), shell.recorded())
    }

    @Test
    fun `FM-10 LN-10 바뀌지 않은 편집기는 pop 된다`() {
        shell.show(ShellScene(stack = EDITOR_STACK, editor = ShellEditor.Clean))

        shell.back()

        assertEquals(listOf(Recorded.Back), shell.recorded())
    }

    @Test
    fun `FM-10 LN-10 전환 중이면 바뀐 편집기도 삼킨다`() {
        shell.show(ShellScene(stack = EDITOR_STACK, editor = ShellEditor.Dirty, settled = false))

        shell.back()

        assertEquals(emptyList<Recorded>(), shell.recorded())
    }

    @Test
    fun `FM-4 창 등급이 바뀌어도 리더 스크롤과 원문 펼침과 토큰 입력이 남는다`() {
        shell.show(ShellScene(stack = READER_STACK))
        shell.expandRaw()
        shell.scrollReader()
        val scrolled = shell.readerScroll()
        assertTrue("리더가 스크롤돼 있어야 한다($scrolled)", scrolled > 0f)

        for (window in listOf(ShellWindow.Wide, ShellWindow.Compact)) {
            shell.setWindow(window)
            assertEquals("$window 리더 스크롤", scrolled, shell.readerScroll(), 1f)
            assertTrue("$window 원문 펼침", shell.rawExpanded())
        }

        shell.show(ShellScene(stack = SETTINGS_STACK, tokenEditing = true))
        shell.typeToken(TOKEN_INPUT)
        for (window in listOf(ShellWindow.Wide, ShellWindow.Compact)) {
            shell.setWindow(window)
            assertTrue("$window 토큰 입력", shell.tokenShown(TOKEN_INPUT))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 하네스 — 셸을 만지는 곳은 여기 하나다. 셸의 상태 · 인텐트 타입이 바뀌면 이 아래만 고친다.
// ---------------------------------------------------------------------------------------------

/** 몸통이 단언하는 콜백. 셸의 인텐트 타입과 떨어져 있어 ViewModel 이 나뉘어도 그대로다. */
internal enum class Recorded { Back, DismissQuiz, CancelTokenEdit, CloseEditor, Exit }

/** 편집기 세션 — 없음 · 연 때 그대로 · 바뀜(LN-10). */
internal enum class ShellEditor { None, Clean, Dirty }

internal enum class ShellWindow { Compact, Wide }

internal enum class ShellQuiz { None, Setup, Solving }

/**
 * 셸에 넣을 가짜 장면.
 *
 * @property stack 백스택. 통계 · 지난 요약 · 연결은 넣지 않는다(엔트리가 Hilt ViewModel 을 쓴다)
 * @property settled 전환이 끝났는가
 * @property tokenEditing 설정의 토큰 편집 중인가
 * @property editor 편집기 세션 — 스택 맨 위가 [EDITOR_KEY] 일 때 쓴다(LN-10)
 */
internal data class ShellScene(
    val stack: List<NotesNavKey> = listOf(NotesNavKey.Home),
    val settled: Boolean = true,
    val quiz: ShellQuiz = ShellQuiz.None,
    val window: ShellWindow = ShellWindow.Compact,
    val tokenEditing: Boolean = false,
    val editor: ShellEditor = ShellEditor.None,
)

internal val EDITOR_KEY = NotesNavKey.Editor()
internal val EDITOR_STACK: List<NotesNavKey> = listOf(NotesNavKey.Home, EDITOR_KEY)

internal val READER_STACK: List<NotesNavKey> = listOf(NotesNavKey.Home, NotesNavKey.Reader(LONG_NOTE_ID))
internal val SETTINGS_STACK: List<NotesNavKey> = listOf(NotesNavKey.Home, NotesNavKey.Settings)
internal const val TOKEN_INPUT = "ghp_backOrderTest"

internal class ShellHarness(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>,
) {
    private var scene by mutableStateOf(ShellScene())
    private var exitRequestId by mutableIntStateOf(0)
    private var shown = false
    private val calls = CopyOnWriteArrayList<Recorded>()
    private val activity get() = rule.activity

    fun show(next: ShellScene) {
        scene = next
        calls.clear()
        if (!shown) {
            shown = true
            rule.setContent { Shell() }
        }
        rule.waitForIdle()
    }

    fun setWindow(window: ShellWindow) {
        scene = scene.copy(window = window)
        rule.waitForIdle()
    }

    fun recorded(): List<Recorded> = calls.toList()

    fun clearRecorded() = calls.clear()

    fun openDrawer() {
        rule.onNodeWithContentDescription(activity.getString(UiR.string.ui_action_note_list)).performClick()
        rule.waitForIdle()
    }

    /** 드로어 여는 애니메이션의 첫 프레임에서 멈춘다 — 목표는 열림, 현재는 아직 닫힘. */
    fun startOpeningDrawer() {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription(activity.getString(UiR.string.ui_action_note_list)).performClick()
        repeat(OPENING_FRAMES) { rule.mainClock.advanceTimeByFrame() }
    }

    fun back() {
        rule.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    /** 예측 뒤로가기 제스처를 끝까지 — 시작 · 진행 · 확정. */
    fun predictiveBack() {
        startPredictiveBack()
        finishPredictiveBack()
    }

    /** 예측 뒤로가기 제스처를 반쯤 끌어 둔다 — 시작 · 진행. */
    fun startPredictiveBack() {
        val dispatcher = activity.onBackPressedDispatcher
        rule.runOnUiThread { dispatcher.dispatchOnBackStarted(backEvent(0f)) }
        rule.waitForIdle()
        rule.runOnUiThread { dispatcher.dispatchOnBackProgressed(backEvent(HALF)) }
        rule.waitForIdle()
    }

    /** 끌어 둔 예측 뒤로가기를 확정한다. */
    fun finishPredictiveBack() {
        rule.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    /** ViewModel 이 두 번째 뒤로가기를 종료로 판정했을 때처럼 종료 요청 id 를 올린다. */
    fun raiseExitRequest() {
        exitRequestId += 1
        rule.waitForIdle()
    }

    /** 목록 패널(드로어 · 고정 패널)이 화면에 보이는가. */
    fun listPanelShown(): Boolean {
        val count = activity.getString(R.string.notes_panel_count, 1)
        val nodes = rule.onAllNodesWithText(count)
        return (0 until nodes.fetchSemanticsNodes().size).any { nodes[it].isDisplayed() }
    }

    /**
     * 보이는 목록 패널 머리(노트 수)의 위쪽 끝(px). M3 드로어는 예측 뒤로가기 진행 중 시트를 세로 가운데 기준으로 줄여
     * 위쪽 내용이 내려온다 — 평범한 BackHandler 가 받으면 끝까지 그대로다.
     */
    fun listPanelTop(): Float {
        val count = activity.getString(R.string.notes_panel_count, 1)
        val nodes = rule.onAllNodesWithText(count)
        val shown = (0 until nodes.fetchSemanticsNodes().size).first { nodes[it].isDisplayed() }
        return nodes[shown].fetchSemanticsNode().boundsInRoot.top
    }

    fun readerShown(): Boolean = rule.onNodeWithText(PARAGRAPH_FIRST).isDisplayed()

    fun expandRaw() {
        rule.onNodeWithText(activity.getString(DsR.string.ds_unsupported_show_raw)).performClick()
        rule.waitForIdle()
    }

    fun rawExpanded(): Boolean =
        rule
            .onAllNodesWithText(activity.getString(DsR.string.ds_unsupported_hide_raw))
            .fetchSemanticsNodes()
            .isNotEmpty()

    fun scrollReader() {
        reader().performScrollToNode(hasText(PARAGRAPH_SCROLL_TO))
        rule.waitForIdle()
    }

    fun readerScroll(): Float =
        reader().fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    fun typeToken(text: String) {
        rule
            .onNode(hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            .performTextInput(text)
        rule.onNodeWithText(activity.getString(UiR.string.ui_settings_token_show)).performClick()
        rule.waitForIdle()
    }

    fun tokenShown(text: String): Boolean =
        rule.onAllNodes(hasSetTextAction() and hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private fun reader() = rule.onNode(hasScrollAction() and hasAnyDescendant(hasText(PARAGRAPH_FIRST)))

    @Composable
    private fun Shell() {
        val drafts = remember { NotesDrafts() }
        val host = remember { SnackbarHostState() }
        val current = scene
        val states = remember(current, exitRequestId) { current.toStates(exitRequestId) }
        val intents =
            remember {
                FeatureIntents(
                    shell = { if (it == ShellIntent.Back) calls += Recorded.Back },
                    notes = {},
                    settings = { if (it == SettingsIntent.CancelTokenEdit) calls += Recorded.CancelTokenEdit },
                    quiz = { if (it == QuizIntent.Dismiss) calls += Recorded.DismissQuiz },
                    editor = { if (it == EditorIntent.Close) calls += Recorded.CloseEditor },
                )
            }
        MyNoteTheme(windowClass = current.window.windowClass, heightClass = MyNoteHeightClass.Medium) {
            NotesExitEffects(states.shell, host, intents.shell, onExit = { calls += Recorded.Exit })
            NotesShell(
                states = states,
                intents = intents,
                snackbarHostState = host,
                drafts = drafts,
                onConfirmDownload = {},
                onEnableNotifications = {},
            )
        }
    }
}

private val ShellWindow.windowClass: MyNoteWindowClass
    get() =
        when (this) {
            ShellWindow.Compact -> MyNoteWindowClass.Compact
            ShellWindow.Wide -> MyNoteWindowClass.Medium
        }

private fun ShellScene.toStates(exitRequestId: Int): FeatureStates {
    val connection = ConnectionState.Connected(SourceId("back-order"), REPO)
    return FeatureStates(
        shell =
            ShellUiState(
                backStack = stack.toPersistentList(),
                activeNoteId = stack.activeNoteId(),
                isNavSettled = settled,
                exitRequestId = exitRequestId,
                connection = connection,
                seedPending = false,
            ),
        notes =
            NotesUiState(
                categories = CATEGORIES,
                visibleCategories = CATEGORIES,
                isLoading = false,
                connection = connection,
                notesLoaded = true,
            ),
        settings =
            SettingsUiState(
                hasToken = true,
                maskedToken = MASKED_TOKEN,
                tokenEdit = if (tokenEditing) TokenEditState(editing = true) else TokenEditState.Viewing,
                connection = connection,
            ),
        quiz =
            QuizOverlayState(
                quiz =
                    when (quiz) {
                        ShellQuiz.None -> QuizUiState.Idle
                        ShellQuiz.Setup -> QuizUiState.Setup(LONG_NOTE_ID)
                        ShellQuiz.Solving -> QuizUiState.Solving(questions = QUESTIONS)
                    },
            ),
        editor = EditorUiState(session = editorSession(editor)),
    )
}

private fun editorSession(editor: ShellEditor): EditorSession? {
    if (editor == ShellEditor.None) return null
    val original = DeviceNoteDraft()
    val draft = if (editor == ShellEditor.Dirty) original.copy(body = "가") else original
    return EditorSession(
        EDITOR_KEY,
        draft,
        original,
        DraftCheck(titleBlank = true, bodyBlank = false, bodyTooLarge = false),
    )
}

private fun backEvent(progress: Float) = BackEventCompat(0f, 0f, progress, BackEventCompat.EDGE_LEFT)

private fun paragraph(text: String) =
    NoteContentBlock.Paragraph(persistentListOf(NoteContentSpan(text, NoteSpanStyle.Normal)))

private const val LONG_NOTE_ID = "back-order-long"
private const val PARAGRAPH_COUNT = 60
private const val PARAGRAPH_FIRST = "paragraph 0"
private const val PARAGRAPH_SCROLL_TO = "paragraph 20"
private const val MASKED_TOKEN = "ghp_••••••••test"
private const val OPENING_FRAMES = 2
private const val HALF = 0.5f
private const val MOVED_PX = 1f

private val REPO = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)

private val LONG_NOTE =
    NoteUi(
        id = LONG_NOTE_ID,
        title = "back order",
        date = "2026.10.11",
        blocks =
            (
                listOf(
                    NoteContentBlock.Unsupported(
                        UnsupportedKind.UnknownType,
                        salvaged = null,
                        raw = "{\"type\":\"x\"}",
                    ),
                ) +
                    (0 until PARAGRAPH_COUNT).map { paragraph("paragraph $it") }
            ).toPersistentList(),
    )

private val CATEGORIES = persistentListOf(CategoryUi("Android", persistentListOf(LONG_NOTE)))

private val QUESTIONS =
    persistentListOf(
        QuizQuestionUi(
            question = "q",
            choices = persistentListOf("a", "b", "c", "d"),
            answerIndex = 0,
            explanation = "e",
        ),
    )
