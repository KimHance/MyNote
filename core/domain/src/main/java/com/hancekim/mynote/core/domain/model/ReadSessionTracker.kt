package com.hancekim.mynote.core.domain.model

data class Moment(
    val wallMillis: Long,
    val elapsedMillis: Long,
)

class ReadSessionTracker(
    private val minActiveMillis: Long = MIN_ACTIVE_MILLIS,
    private val maxActiveMillis: Long = MAX_ACTIVE_MILLIS,
) {
    private var topNoteId: String? = null
    private var topCategory: String = ""
    private var lastPercent: Int? = null
    private var visible = false
    private var covered = false
    private var open: OpenSession? = null

    fun onTop(
        noteId: String?,
        category: String?,
        now: Moment,
    ): ReadSession? {
        if (noteId == topNoteId) return null
        val closed = close(now)
        topNoteId = noteId
        topCategory = category.orEmpty()
        lastPercent = null
        if (noteId != null && visible) start(now)
        return closed
    }

    fun onVisible(
        visible: Boolean,
        now: Moment,
    ): ReadSession? {
        if (visible == this.visible) return null
        this.visible = visible
        if (!visible) return close(now)
        if (topNoteId != null) start(now)
        return null
    }

    fun onCovered(
        covered: Boolean,
        now: Moment,
    ) {
        if (covered == this.covered) return
        this.covered = covered
        val session = open ?: return
        if (covered) session.pause(now) else session.resume(now)
    }

    fun onProgress(
        noteId: String,
        percent: Int,
    ) {
        if (noteId != topNoteId) return
        val seen = percent.coerceIn(0, MAX_PERCENT)
        lastPercent = seen
        val session = open ?: return
        session.maxPercent = maxOf(session.maxPercent, seen)
    }

    private fun start(now: Moment) {
        val noteId = topNoteId ?: return
        open =
            OpenSession(
                noteId = noteId,
                category = topCategory,
                startedAt = now.wallMillis,
                runningSince = if (covered) null else now.elapsedMillis,
            ).also { it.maxPercent = lastPercent ?: 0 }
    }

    private fun close(now: Moment): ReadSession? {
        val session = open ?: return null
        open = null
        session.pause(now)
        if (session.activeMillis < minActiveMillis) return null
        return ReadSession(
            noteId = session.noteId,
            category = session.category,
            startedAt = session.startedAt,
            endedAt = now.wallMillis,
            activeMillis = session.activeMillis.coerceAtMost(maxActiveMillis),
            maxPercent = session.maxPercent,
        )
    }

    private class OpenSession(
        val noteId: String,
        val category: String,
        val startedAt: Long,
        var runningSince: Long?,
    ) {
        var activeMillis = 0L
        var maxPercent = 0

        fun pause(now: Moment) {
            val since = runningSince ?: return
            activeMillis += (now.elapsedMillis - since).coerceAtLeast(0L)
            runningSince = null
        }

        fun resume(now: Moment) {
            if (runningSince == null) runningSince = now.elapsedMillis
        }
    }

    companion object {
        const val MIN_ACTIVE_MILLIS = 30_000L
        const val MAX_ACTIVE_MILLIS = 3_600_000L
        private const val MAX_PERCENT = 100
    }
}
