package com.hancekim.mynote.feature.editor

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DraftCheck
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.element.NoticeUi

/**
 * 편집 세션 하나 — 스택에 있는 편집기 키 하나와 짝이다(키 = 세션). 본문은 상태 복원에 넣지 않고 Activity owner 인
 * ViewModel 이 든다(local-notes 결정 6 · LN-10).
 *
 * @property original 연 때의 값 — 나가기 확인의 기준이다. 가져오기로 미리 채운 편집기는 미리 채운 값이 기준이다(LN-10)
 * @property loading 고칠 노트의 지금 값을 읽는 중(LN-9)
 * @property previewBlocks 미리보기 블록. 미리보기 중인데 null 이면 해석 중이다(LN-3 ②)
 */
@Immutable
data class EditorSession(
    val key: NotesNavKey.Editor,
    val draft: DeviceNoteDraft,
    val original: DeviceNoteDraft,
    val check: DraftCheck,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val previewing: Boolean = false,
    val previewBlocks: List<NoteBlock>? = null,
) {
    /** 고치기 편집기인가(제목 `노트 고치기`) — 아니면 새 노트 · 가져온 글이다. */
    val editing: Boolean get() = key.noteId != null

    /** 처음 연 때와 달라졌나 — 나가기 전에 묻는다(LN-10). 미리보기 전환은 바뀜이 아니다. */
    val dirty: Boolean get() = draft != original

    val canSave: Boolean get() = check.canSave && !loading && !saving
}

/** 가져오기 그릇(대화상자)의 상태(LN-6 · 디자인 정하지 않은 것 23). */
enum class ImportDialogUi {
    Closed,
    Choosing,

    /** 고른 파일을 읽는 중 — 같은 상자에 진행 막대(디자인 C18). */
    Reading,
}

/**
 * 편집기 기능이 그리는 값 — 편집 세션 · 나가기 · 지우기 확인 · 가져오기 그릇 · 붙여넣기 칸 · 스낵바 알림.
 *
 * @property filePickRequestId 시스템 파일 고르기를 띄워 달라는 일회성 신호. 셸이 늘 컴포즈하는 [ImportHost] 가 받는다
 * @property deleteConfirmNoteId 지우기 확인을 묻는 노트(LN-11). 리더 위에 뜬다
 */
@Stable
data class EditorUiState(
    val session: EditorSession? = null,
    val exitConfirm: Boolean = false,
    val deleteConfirmNoteId: String? = null,
    val importDialog: ImportDialogUi = ImportDialogUi.Closed,
    val filePickRequestId: Int = 0,
    val pasteText: String = "",
    val notice: NoticeUi<EditorIntent>? = null,
) : MviState {
    /** 셸의 뒤로가기 ④ 지킴 — 바뀐 편집기면 pop 대신 [EditorIntent.Close] 로 묻는다(LN-10). */
    val guardsExit: Boolean get() = session?.dirty == true
}

sealed interface EditorIntent : MviEvent {
    /** 빈 편집기를 연다(LN-3). */
    data object NewNote : EditorIntent

    /** 기기 노트를 고친다(LN-9). */
    data class Edit(
        val noteId: String,
    ) : EditorIntent

    data class TitleChanged(
        val text: String,
    ) : EditorIntent

    data class GroupChanged(
        val text: String,
    ) : EditorIntent

    data class BodyChanged(
        val text: String,
    ) : EditorIntent

    data class SetPreview(
        val on: Boolean,
    ) : EditorIntent

    data object Save : EditorIntent

    /** 닫기 · 지킴에 걸린 뒤로가기. 바뀐 것이 있으면 묻고, 없으면 닫는다(LN-10). */
    data object Close : EditorIntent

    /** 나가기 확인의 답 — [discard] 면 버리고 닫거나(또는 기다리던 새 공유를 연다), 아니면 계속 쓴다. */
    data class ExitAnswered(
        val discard: Boolean,
    ) : EditorIntent

    data class RequestDelete(
        val noteId: String,
    ) : EditorIntent

    data class DeleteAnswered(
        val confirm: Boolean,
    ) : EditorIntent

    data object ChooseFile : EditorIntent

    /** 파일 고르기 결과. 고르지 않고 돌아왔으면 null(LN-7). */
    data class FileChosen(
        val uri: String?,
    ) : EditorIntent

    data object ChoosePaste : EditorIntent

    data object DismissImport : EditorIntent

    data class PasteChanged(
        val text: String,
    ) : EditorIntent

    data object PasteNext : EditorIntent

    data class SignalShown(
        val signal: NotesSignal,
        val id: Int,
    ) : EditorIntent
}
