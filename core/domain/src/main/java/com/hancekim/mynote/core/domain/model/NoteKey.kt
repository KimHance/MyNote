package com.hancekim.mynote.core.domain.model

@JvmInline
value class SourceId(
    val value: String,
) {
    companion object {
        val Legacy: SourceId = SourceId("s0")
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
