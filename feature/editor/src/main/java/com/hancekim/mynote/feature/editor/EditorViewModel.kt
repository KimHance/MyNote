package com.hancekim.mynote.feature.editor

import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.domain.CheckDeviceNoteDraftUseCase
import com.hancekim.mynote.core.domain.DeleteDeviceNoteUseCase
import com.hancekim.mynote.core.domain.ImportNoteUseCase
import com.hancekim.mynote.core.domain.LoadDeviceNoteUseCase
import com.hancekim.mynote.core.domain.PreviewDeviceNoteUseCase
import com.hancekim.mynote.core.domain.SaveDeviceNoteUseCase
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DeviceNoteFields
import com.hancekim.mynote.core.domain.model.ImportOutcome
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.editorKey
import com.hancekim.mynote.core.navigation.isEditorTop
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.openEditorCommand
import com.hancekim.mynote.core.navigation.savedNoteCommand
import com.hancekim.mynote.core.navigation.withoutNoteCommand
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.element.NoticeUi
import com.hancekim.mynote.core.ui.element.unseen
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import com.hancekim.mynote.core.ui.signal.SharedTextInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 기기 노트 편집기 · 가져오기(local-notes LN-3 · LN-6 ~ LN-11). 편집 세션 · 가져오기 그릇 · 붙여넣기 칸 · 공유 받기 · 지우기를
 * 한 ViewModel 이 든다 — 공유 위 공유(LN-8 ③)가 지금 편집 세션의 바뀜을 알아야 해서다.
 *
 * 세션은 스택에 있는 편집기 키를 따른다. 키가 새로 나타나면 세션을 열고, 빠지면 버린다(저장 안 한 글은 남지 않는다 — 결정 6).
 * 미리 채운 초안은 [prepared] 에 두고 키에는 초안 id 만 싣는다.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val navigator: Navigator,
    private val signals: FeatureSignals,
    private val inbox: SharedTextInbox,
    private val checkDraft: CheckDeviceNoteDraftUseCase,
    private val saveNote: SaveDeviceNoteUseCase,
    private val deleteNote: DeleteDeviceNoteUseCase,
    private val loadNote: LoadDeviceNoteUseCase,
    private val previewNote: PreviewDeviceNoteUseCase,
    private val importNote: ImportNoteUseCase,
    private val messages: EditorMessages,
    private val failureLog: FailureLog,
) : MviViewModel<EditorUiState, EditorIntent>() {
    private val session = trigger<EditorSession?>(null)
    private val exitConfirm = trigger(false)
    private val deleteConfirm = trigger<String?>(null)
    private val importDialog = trigger(ImportDialogUi.Closed)
    private val filePicks = trigger(0)
    private val pasteText = trigger("")
    private val pasteTooLarge = trigger(false)
    private val notice = trigger<NoticeUi<EditorIntent>?>(null)
    private val shownSignals = trigger(persistentMapOf<NotesSignal, Int>())

    private val prepared = mutableMapOf<Int, DeviceNoteDraft>()
    private var draftSeq = 0
    private var noticeSeq = 0

    /** 고른 파일을 읽는 일 — 읽는 중에 `취소` 하면 결과를 버린다(편집기도 이유도 없다). */
    private var fileRead: Job? = null

    /** 나가기 확인 뒤에 열 편집기 — 바뀐 편집기 위로 공유가 들어왔을 때(LN-8 ③). null 이면 확인 뒤 닫는다. */
    private var pendingOpen: NotesNavKey.Editor? = null

    init {
        navigator.stack
            .map { it.editorKey() }
            .distinctUntilChanged()
            .onEach { onEditorKey(it) }
            .launchIn(viewModelScope)
        navigator.stack
            .map { NotesNavKey.Paste in it }
            .distinctUntilChanged()
            .onEach { open ->
                if (!open) {
                    pasteText.value = ""
                    pasteTooLarge.value = false
                }
            }.launchIn(viewModelScope)
        // 일회성 신호 — 만들어질 때의 값은 이미 본 것이다(싱글턴이라 옛 요청이 남아 있다)
        signals.importRequests
            .drop(1)
            .onEach { importDialog.value = ImportDialogUi.Choosing }
            .launchIn(viewModelScope)
        inbox.pending
            .filterNotNull()
            .onEach { shared ->
                inbox.take(shared.id)?.let { receive(importNote.text(it.text, it.subject), immediate = true) }
            }.launchIn(viewModelScope)
    }

    override fun build() =
        EditorUiState(
            session = session.value,
            exitConfirm = exitConfirm.value,
            deleteConfirmNoteId = deleteConfirm.value,
            importDialog = importDialog.value,
            filePickRequestId = filePicks.value,
            pasteText = pasteText.value,
            pasteTooLarge = pasteTooLarge.value,
            notice = notice.value?.takeIf { shownSignals.value.unseen(NotesSignal.Notice, it.id) != 0 },
        )

    override fun onFailure(cause: Throwable) {
        failureLog.failed(TAG, "편집기 인텐트 처리 실패", cause)
    }

    override fun handleIntent(event: EditorIntent) {
        when (event) {
            EditorIntent.NewNote -> {
                openEditor(NotesNavKey.Editor(), immediate = false)
            }

            is EditorIntent.Edit -> {
                openEditor(NotesNavKey.Editor(noteId = event.noteId), immediate = false)
            }

            is EditorIntent.TitleChanged -> {
                edit { it.copy(title = DeviceNoteFields.singleLine(event.text)) }
            }

            is EditorIntent.GroupChanged -> {
                edit { it.copy(group = DeviceNoteFields.singleLine(event.text)) }
            }

            is EditorIntent.BodyChanged -> {
                edit { it.copy(body = event.text) }
            }

            is EditorIntent.SetPreview -> {
                setPreview(event.on)
            }

            EditorIntent.Save -> {
                save()
            }

            EditorIntent.Close -> {
                close()
            }

            is EditorIntent.ExitAnswered -> {
                answerExit(event.discard)
            }

            is EditorIntent.RequestDelete -> {
                deleteConfirm.value = event.noteId
            }

            is EditorIntent.DeleteAnswered -> {
                answerDelete(event.confirm)
            }

            EditorIntent.ChooseFile -> {
                filePicks.update { it + 1 }
            }

            is EditorIntent.FileChosen -> {
                readFile(event.uri)
            }

            EditorIntent.ChoosePaste -> {
                importDialog.value = ImportDialogUi.Closed
                navigator.dispatch(NavCommand.Push(NotesNavKey.Paste))
            }

            EditorIntent.DismissImport -> {
                fileRead?.cancel()
                fileRead = null
                importDialog.value = ImportDialogUi.Closed
            }

            is EditorIntent.PasteChanged -> {
                pasteText.value = event.text
                pasteTooLarge.value = Utf8Size.bytes(event.text, stopAfter = RAW_LIMIT) > RAW_LIMIT
            }

            EditorIntent.PasteNext -> {
                // `다음` 이 꺼진 동안(빈 칸 · 512KB 넘음)은 이유 줄이 이미 보인다 — 스낵바를 겹쳐 띄우지 않는다
                if (!pasteTooLarge.value && pasteText.value.isNotBlank()) {
                    receive(importNote.text(pasteText.value), immediate = false)
                }
            }

            is EditorIntent.SignalShown -> {
                shownSignals.update { it.put(event.signal, event.id) }
            }
        }
    }

    private fun onEditorKey(key: NotesNavKey.Editor?) {
        if (key == null) {
            session.value = null
            exitConfirm.value = false
            return
        }
        if (session.value?.key == key) return
        val noteId = key.noteId
        val start = key.draftId?.let { prepared.remove(it) } ?: DeviceNoteDraft()
        session.value = sessionOf(key, start, loading = noteId != null)
        if (noteId != null) {
            launchSafely {
                val loaded = loadNote(noteId)
                if (session.value?.key != key) return@launchSafely
                if (loaded == null) {
                    // 기기 노트가 아니거나 이미 지워졌다 — 열 것이 없으니 편집기를 닫는다
                    session.value = null
                    navigator.dispatchImmediately(NavCommand.ReplaceAll(navigator.stack.value.filterNot { it == key }))
                } else {
                    session.value = sessionOf(key, loaded, loading = false)
                }
            }
        }
    }

    private fun sessionOf(
        key: NotesNavKey.Editor,
        draft: DeviceNoteDraft,
        loading: Boolean,
    ) = checked(EditorSession(key = key, draft = draft, original = draft, check = checkDraft(draft), loading = loading))

    /** 크기 넘음이면 이유 줄의 KB 를 센다 — 넘지 않으면 세지 않는다(글자마다 512KB 를 다 세지 않게). */
    private fun checked(session: EditorSession): EditorSession =
        session.copy(
            bodyKilobytes =
                if (session.check.bodyTooLarge) Utf8Size.kilobytes(Utf8Size.bytes(session.draft.body)) else 0,
        )

    private fun edit(change: (DeviceNoteDraft) -> DeviceNoteDraft) {
        val current = session.value ?: return
        if (current.loading) return
        val draft = change(current.draft)
        // 미리보기는 본문의 해석이다 — 미리보기 중에 제목 · 그룹을 고쳐도 블록은 그대로 둔다(버리면 다시 해석할 일이 없어 진행 막대가 남는다)
        val blocks = if (draft.body == current.draft.body) current.previewBlocks else null
        session.value = checked(current.copy(draft = draft, check = checkDraft(draft), previewBlocks = blocks))
    }

    private fun setPreview(on: Boolean) {
        val current = session.value ?: return
        session.value = current.copy(previewing = on, previewBlocks = if (on) current.previewBlocks else null)
        if (!on || current.previewBlocks != null) return
        val body = current.draft.body
        launchSafely {
            val blocks = previewNote(body)
            session.update { now ->
                if (now?.key == current.key && now.previewing &&
                    now.draft.body == body
                ) {
                    now.copy(previewBlocks = blocks)
                } else {
                    now
                }
            }
        }
    }

    private fun save() {
        val current = session.value ?: return
        if (!current.canSave) return
        session.value = current.copy(saving = true)
        launchSafely {
            saveNote(current.key.noteId, current.draft, messages.defaultGroup())
                .onSuccess { noteId ->
                    // 저장 결과는 버려지면 안 된다(두 번 저장) — 사용자 탭이 아니라 정정처럼 바로 보낸다
                    session.update { it?.copy(original = it.draft, saving = false) }
                    navigator.dispatchImmediately(navigator.stack.value.savedNoteCommand(noteId))
                }.onFailure { cause ->
                    session.update { it?.copy(saving = false) }
                    failureLog.failed(TAG, "기기 노트 저장 실패", cause)
                    announce(messages.saveFailed())
                }
        }
    }

    private fun close() {
        if (session.value?.dirty == true) {
            pendingOpen = null
            exitConfirm.value = true
        } else {
            leave()
        }
    }

    private fun leave() {
        val stack = navigator.stack.value
        if (stack.isEditorTop()) {
            navigator.dispatch(NavCommand.Pop)
        } else {
            stack.editorKey()?.let { key -> navigator.dispatch(NavCommand.ReplaceAll(stack.filterNot { it == key })) }
        }
    }

    private fun answerExit(discard: Boolean) {
        exitConfirm.value = false
        val next = pendingOpen
        pendingOpen = null
        when {
            !discard -> next?.draftId?.let { prepared.remove(it) }
            next != null -> navigator.dispatchImmediately(navigator.stack.value.openEditorCommand(next))
            else -> leave()
        }
    }

    /** 편집기를 연다. 스택에 바뀐 편집기가 있으면 먼저 묻는다(LN-8 ③ · LN-10). */
    private fun openEditor(
        key: NotesNavKey.Editor,
        immediate: Boolean,
    ) {
        val stack = navigator.stack.value
        if (stack.editorKey() != null && session.value?.dirty == true) {
            pendingOpen?.draftId?.let { prepared.remove(it) }
            pendingOpen = key
            exitConfirm.value = true
            return
        }
        val command = stack.openEditorCommand(key)
        if (immediate) navigator.dispatchImmediately(command) else navigator.dispatch(command)
    }

    private fun readFile(uri: String?) {
        if (uri == null) return
        importDialog.value = ImportDialogUi.Reading
        fileRead?.cancel()
        fileRead =
            launchSafely {
                val outcome = importNote.file(uri)
                importDialog.value =
                    if (outcome == ImportOutcome.Dismissed) ImportDialogUi.Choosing else ImportDialogUi.Closed
                receive(outcome, immediate = false)
            }
    }

    /** 가져오기 결과 — 미리 채운 편집기를 열거나 이유를 알린다(LN-6 · LN-7). 노트는 만들지 않는다. */
    private fun receive(
        outcome: ImportOutcome,
        immediate: Boolean,
    ) {
        when (outcome) {
            is ImportOutcome.Ready -> {
                val id = ++draftSeq
                prepared[id] = outcome.draft
                openEditor(NotesNavKey.Editor(draftId = id), immediate)
            }

            is ImportOutcome.Rejected -> {
                announce(messages.rejected(outcome.reason))
            }

            ImportOutcome.Dismissed -> {
                Unit
            }
        }
    }

    private fun answerDelete(confirm: Boolean) {
        val noteId = deleteConfirm.value ?: return
        deleteConfirm.value = null
        if (!confirm) return
        launchSafely {
            deleteNote(noteId)
                .onSuccess {
                    // 화면 정리(LN-11) — 그 노트의 리더 · 편집기를 스택에서 빼고 퀴즈를 닫는다. 연결 정리(ScreenCleanup)와 따로다
                    navigator.dispatchImmediately(navigator.stack.value.withoutNoteCommand(noteId))
                    signals.closeQuiz()
                    announce(messages.deleted())
                }.onFailure { cause ->
                    failureLog.failed(TAG, "기기 노트 지우기 실패", cause)
                    announce(messages.deleteFailed())
                }
        }
    }

    private fun announce(message: String) {
        notice.value = NoticeUi(message, undo = null, id = ++noticeSeq)
    }
}

private const val TAG = "MyNoteEditor"

/** 가져올 원문 한도 — LN-7 의 512KB(524,288 바이트). 붙여넣기 이유 줄이 같은 경계를 쓴다. */
private val RAW_LIMIT = NoteLimits.Default.maxBytes
