package com.hancekim.mynote.core.domain.model

@JvmInline
value class SourceId(
    val value: String,
) {
    companion object {
        /** 기기 노트(앱 DB 가 원본)의 소스 id. 저장 값이다 — 북마크 · 학습 기록 키의 접두가 된다. */
        const val DEVICE_ID: String = "local"

        val Legacy: SourceId = SourceId("s0")

        /** 기기 노트의 소스. `/` 가 없고 [Legacy] 가 아니며 연결 id 생성기가 피한다(LN-5). */
        val Device: SourceId = SourceId(DEVICE_ID)
    }
}

data class NoteKey(
    val source: SourceId,
    val docId: String,
) {
    val value: String get() = "${source.value}/$docId"

    override fun toString(): String = value

    companion object {
        fun parse(value: String): NoteKey? {
            val slash = value.indexOf('/')
            if (slash <= 0 || slash == value.lastIndex) return null
            return NoteKey(SourceId(value.substring(0, slash)), value.substring(slash + 1))
        }
    }
}

enum class SourceKind { GitHub, Link, Local }
