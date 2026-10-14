package com.hancekim.mynote.core.navigation.key

import kotlinx.serialization.Serializable

@Serializable
sealed interface NotesNavKey : MyNoteNavKey {
    @Serializable
    data object Home : NotesNavKey

    @Serializable
    data object Bookmarks : NotesNavKey

    @Serializable
    data object Stats : NotesNavKey

    @Serializable
    data object StatsHistory : NotesNavKey

    @Serializable
    data class Reader(
        val noteId: String,
    ) : NotesNavKey

    @Serializable
    data object Settings : NotesNavKey

    @Serializable
    data object Connect : NotesNavKey

    /**
     * 기기 노트 편집기(local-notes LN-3 · LN-9). 셋 다 같은 키 모양이다 — 새 노트는 둘 다 null,
     * 고치기는 [noteId], 가져오기로 미리 채운 편집기는 [draftId](본문은 키에 싣지 않는다 — 편집기 ViewModel 이 든다).
     * 스택에 편집기는 한 번에 하나다(LN-8 ③).
     */
    @Serializable
    data class Editor(
        val noteId: String? = null,
        val draftId: Int? = null,
    ) : NotesNavKey

    /** 가져오기의 붙여넣기 화면(LN-6 (c)). `다음` 을 누르면 미리 채운 편집기가 이 자리를 갈아 끼운다. */
    @Serializable
    data object Paste : NotesNavKey
}

fun List<MyNoteNavKey>.isSettingsTop(): Boolean = lastOrNull() is NotesNavKey.Settings

fun List<MyNoteNavKey>.activeNoteId(): String? = (lastOrNull() as? NotesNavKey.Reader)?.noteId
