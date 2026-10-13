package com.hancekim.mynote.core.domain.model

data class Category(
    val name: String,
    val notes: List<Note>,
)

data class Note(
    val id: String,
    val title: String,
    val date: String,
    val blocks: List<NoteBlock>,
) {
    /** 노트가 어디서 왔나. 키의 소스 부분에서 파생한다(LN-5). */
    val origin: NoteOrigin get() = NoteOrigin.of(id)
}

/** 노트의 출처 — 연결한 저장소에서 불러온 노트 · 앱 DB 가 원본인 기기 노트. */
enum class NoteOrigin {
    Repository,
    Device,
    ;

    /** 앱에서 고치고 지울 수 있는가. 저장소 노트는 읽기 전용이다(local-notes 결정 2). */
    val editable: Boolean get() = this == Device

    companion object {
        fun of(noteId: String): NoteOrigin =
            if (NoteKey.parse(noteId)?.source == SourceId.Device) Device else Repository
    }
}

/**
 * 연결 안내 띠가 세는 노트 수 — 연결이 없을 때 보이는 저장소 노트(연결 정보 없는 옛 캐시)뿐이고 기기 노트는 세지 않는다(RC-1).
 * 연결이 있거나 아직 모르면 0 이다.
 */
fun List<Category>.connectBandNoteCount(connection: ConnectionState?): Int =
    if (connection != ConnectionState.None) {
        0
    } else {
        sumOf { category -> category.notes.count { it.origin == NoteOrigin.Repository } }
    }
