package com.hancekim.mynote.core.domain.model

sealed interface ConnectionState {
    data object None : ConnectionState

    data class Connected(
        val sourceId: SourceId,
        val connection: RepoConnection,
    ) : ConnectionState
}

data class PendingConnection(
    val sourceId: SourceId,
    val connection: RepoConnection,
    val kind: Kind,
) {
    enum class Kind { New, Adopt, SameRepository }
}

data class ErasureCount(
    val notes: Int,
    val bookmarks: Int,
)

data class ConnectionSeed(
    val owner: String,
    val repo: String,
    val branch: String,
    val token: String?,
)

sealed interface SeedOutcome {
    data object NotApplicable : SeedOutcome

    data class Connected(
        val outcome: SyncOutcome,
    ) : SeedOutcome

    data class Failed(
        val cause: Throwable,
    ) : SeedOutcome
}

sealed interface SyncRun {
    data object Idle : SyncRun

    data class Running(
        val announce: Boolean,
    ) : SyncRun

    data class Finished(
        val announce: Boolean,
        val result: Result<SyncOutcome>,
    ) : SyncRun
}
