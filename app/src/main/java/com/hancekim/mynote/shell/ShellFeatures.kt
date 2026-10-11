package com.hancekim.mynote.shell

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.ui.element.CtaAction
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.ListPanelChrome
import com.hancekim.mynote.feature.quiz.QuizIntent
import com.hancekim.mynote.feature.quiz.QuizOverlayState
import com.hancekim.mynote.feature.settings.SettingsIntent
import com.hancekim.mynote.feature.settings.SettingsUiState

/** 셸이 그리는 데 쓰는 기능 상태 넷. 화면만 쓰는 값은 여기서 묶어 넘긴다(결정 13). */
@Stable
internal data class FeatureStates(
    val shell: ShellUiState = ShellUiState(),
    val notes: NotesUiState = NotesUiState(),
    val settings: SettingsUiState = SettingsUiState(),
    val quiz: QuizOverlayState = QuizOverlayState(),
) {
    val activeNote: NoteUi? get() = notes.noteById(shell.activeNoteId)

    val listPanel: ListPanelChrome
        get() =
            ListPanelChrome(
                activeNoteId = shell.activeNoteId,
                settingsOpen = shell.isSettingsOpen,
                tokenCheck = settings.repoCard.showBadge,
                modelStatus = shell.modelStatus,
            )
}

/** 기능 ViewModel 넷의 입구. 화면 이벤트는 그 기능의 ViewModel 로 간다. */
@Immutable
internal class FeatureIntents(
    val shell: (ShellIntent) -> Unit,
    val notes: (NotesIntent) -> Unit,
    val settings: (SettingsIntent) -> Unit,
    val quiz: (QuizIntent) -> Unit,
)

/** 리더 끝 CTA 를 누르면 어디로 가나(FM-6 ⑤). */
internal sealed interface CtaTarget {
    data class Quiz(
        val intent: QuizIntent,
    ) : CtaTarget

    data object ConfirmDownload : CtaTarget

    data class Shell(
        val intent: ShellIntent,
    ) : CtaTarget

    data class Settings(
        val intent: SettingsIntent,
    ) : CtaTarget
}

internal fun ctaTarget(
    noteId: String,
    category: String,
    action: CtaAction,
): CtaTarget =
    when (action) {
        CtaAction.OpenSetup -> CtaTarget.Quiz(QuizIntent.OpenSetup(noteId, category))
        CtaAction.ConfirmDownload -> CtaTarget.ConfirmDownload
        CtaAction.OpenSettings -> CtaTarget.Shell(ShellIntent.OpenSettings)
        CtaAction.StartDownload -> CtaTarget.Settings(SettingsIntent.StartModelDownload)
    }

internal fun FeatureIntents.send(
    target: CtaTarget,
    onConfirmDownload: () -> Unit,
) {
    when (target) {
        is CtaTarget.Quiz -> quiz(target.intent)
        CtaTarget.ConfirmDownload -> onConfirmDownload()
        is CtaTarget.Shell -> shell(target.intent)
        is CtaTarget.Settings -> settings(target.intent)
    }
}

/** 맨 위로 버튼 — 퀴즈 · 연결 화면 · 설정의 토큰 편집 중에는 숨긴다(FM-6 ⑩). */
internal fun scrollToTopVisible(
    scrolled: Boolean,
    quizOpen: Boolean,
    connectOpen: Boolean,
    tokenEditing: Boolean,
): Boolean = scrolled && !quizOpen && !connectOpen && !tokenEditing
