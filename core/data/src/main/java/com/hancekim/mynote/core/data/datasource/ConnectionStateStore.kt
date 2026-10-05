package com.hancekim.mynote.core.data.datasource

data class AttemptRecord(
    val sourceId: String,
    val targetConfigJson: String,
    val tokenBefore: String?,
)

interface ConnectionStateStore {
    suspend fun usedSourceIds(): Set<String>

    suspend fun addUsedSourceId(id: String)

    suspend fun attempt(): AttemptRecord?

    suspend fun saveAttempt(record: AttemptRecord)

    suspend fun clearAttempt()

    suspend fun disconnectMarker(): String?

    suspend fun markDisconnect(sourceId: String)

    suspend fun clearDisconnectMarker()

    suspend fun clearDisconnectAndSeedBlock()

    suspend fun isSeedBlocked(): Boolean

    suspend fun clearSeedBlock()

    suspend fun bookmarkSweep(): Set<String>

    suspend fun addBookmarkSweep(ids: Collection<String>)

    suspend fun clearBookmarkSweep()
}

fun interface ConnectionIdGenerator {
    fun next(): String
}
