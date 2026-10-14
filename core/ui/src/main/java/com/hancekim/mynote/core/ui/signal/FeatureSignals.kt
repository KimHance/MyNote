package com.hancekim.mynote.core.ui.signal

import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** 다른 기능이 리더를 열었다 — 이어 읽을 위치 · 퀴즈 설정을 같이 바란다. */
data class ReaderOpened(
    val noteId: String,
    val resumeAt: Float?,
    val quiz: Boolean,
    val id: Int,
)

/** 노트가 퀴즈 설정을 띄워 달라고 한다. 읽을 수 있는지는 노트가 이미 봤다. */
data class QuizSetupRequest(
    val noteId: String,
    val category: String,
    val id: Int,
)

/**
 * 기능 사이에서 ViewModel 이 받는 신호(결정 13). 이동은 목적지 키로 하고 여기에는 이동이 아닌 것만 싣는다.
 *
 * 일회성 신호는 증가하는 id 로 나간다. 받는 쪽은 만들어질 때의 id 를 이미 본 것으로 잡는다 — 싱글턴이라
 * Activity 가 다시 열려 ViewModel 이 새로 생겨도 옛 요청이 남아 있다.
 */
@Singleton
class FeatureSignals @Inject constructor() {
    private val seq = AtomicInteger()

    private val opened = MutableStateFlow<ReaderOpened?>(null)
    val readerOpened: StateFlow<ReaderOpened?> = opened.asStateFlow()

    private val setups = MutableStateFlow<QuizSetupRequest?>(null)
    val quizSetup: StateFlow<QuizSetupRequest?> = setups.asStateFlow()

    private val covers = MutableStateFlow(false)
    val quizCoversReader: StateFlow<Boolean> = covers.asStateFlow()

    private val closes = MutableStateFlow(0)
    val quizCloses: StateFlow<Int> = closes.asStateFlow()

    private val locked = MutableStateFlow(false)
    val repoActionsLocked: StateFlow<Boolean> = locked.asStateFlow()

    private val entries = MutableStateFlow(0)
    val settingsEntries: StateFlow<Int> = entries.asStateFlow()

    private val clears = MutableStateFlow(0)
    val studyLogClears: StateFlow<Int> = clears.asStateFlow()

    private val imports = MutableStateFlow(0)
    val importRequests: StateFlow<Int> = imports.asStateFlow()

    fun reportReaderOpened(
        noteId: String,
        resumeAt: Float?,
        quiz: Boolean,
    ) {
        opened.value = ReaderOpened(noteId, resumeAt, quiz, seq.incrementAndGet())
    }

    fun requestQuizSetup(
        noteId: String,
        category: String,
    ) {
        setups.value = QuizSetupRequest(noteId, category, seq.incrementAndGet())
    }

    fun quizCovers(covered: Boolean) {
        covers.value = covered
    }

    /** 연결이 바뀌거나 그 노트를 지워 퀴즈를 닫는다(RC-23 · LN-11). 덮음도 같이 내려 읽기 세션이 같은 틱에 다시 돈다. */
    fun closeQuiz() {
        covers.value = false
        closes.update { it + 1 }
    }

    fun lockRepoActions(lock: Boolean) {
        locked.value = lock
    }

    fun enterSettings() {
        entries.update { it + 1 }
    }

    fun studyLogCleared() {
        clears.update { it + 1 }
    }

    /** 홈의 `가져오기` — 편집기 기능이 가져오기 그릇을 연다(local-notes LN-1 · LN-2 · LN-6). */
    fun requestImport() {
        imports.update { it + 1 }
    }
}

/** 설정을 연다 — 설정이 토큰 편집을 정리하게 알린 뒤 목적지 키로 간다(FM-6 ④). */
fun Navigator.openSettings(signals: FeatureSignals) {
    signals.enterSettings()
    push(NotesNavKey.Settings)
}
