package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.BookmarkStore
import com.hancekim.mynote.core.data.datasource.ConnectionStateStore
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.TokenStore
import com.hancekim.mynote.core.domain.model.SourceKind
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRecovery @Inject constructor(
    private val local: LocalNotesDataSource,
    private val tokens: TokenStore,
    private val bookmarks: BookmarkStore,
    private val state: ConnectionStateStore,
) {
    private val mutex = Mutex()
    private var done = false

    suspend fun ensure() {
        if (done) return
        mutex.withLock {
            if (done) return
            run()
            done = true
        }
    }

    suspend fun run() {
        finishDisconnect()
        discardPending()
        restoreTokenOfUnconfirmedAttempt()
        sweepBookmarks()
    }

    private suspend fun finishDisconnect() {
        val id = state.disconnectMarker() ?: return
        if (local.sources().any { it.id.value == id }) {
            state.clearDisconnectAndSeedBlock()
            return
        }
        bookmarks.removeByPrefix("$id/")
        if (tokens.currentToken() != null) tokens.saveToken(null)
        state.clearDisconnectMarker()
    }

    private suspend fun discardPending() {
        local
            .sources()
            .filter { it.kind == SourceKind.GitHub && ConnectionConfigCodec.decode(it.configJson)?.pending == true }
            .forEach { local.deleteSource(it.id) }
    }

    private suspend fun restoreTokenOfUnconfirmedAttempt() {
        val attempt = state.attempt() ?: return
        val row = local.sources().firstOrNull { it.id.value == attempt.sourceId }
        val confirmed = ConnectionConfigCodec.decode(row?.configJson)?.attemptId == attempt.attemptId
        if (!confirmed && tokens.currentToken() != attempt.tokenBefore) {
            tokens.saveToken(attempt.tokenBefore)
        }
        state.clearAttempt()
    }

    private suspend fun sweepBookmarks() {
        val sweep = state.bookmarkSweep()
        if (sweep.isEmpty()) return
        val alive = local.sources().mapTo(HashSet()) { it.id.value }
        sweep.filterNot { it in alive }.forEach { bookmarks.removeByPrefix("$it/") }
        state.clearBookmarkSweep()
    }
}
