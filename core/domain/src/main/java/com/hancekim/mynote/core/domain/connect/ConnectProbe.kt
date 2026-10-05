package com.hancekim.mynote.core.domain.connect

import com.hancekim.mynote.core.domain.model.RepoConnection

interface ConnectProbe {
    suspend fun verify(
        repo: ConnectLinkRepo,
        candidates: List<ConnectCandidate>,
        token: String?,
    ): ConnectProbeResult
}

sealed interface ConnectProbeResult {
    data class Verified(
        val connection: RepoConnection,
        val noteIds: Set<String>,
    ) : ConnectProbeResult

    data class Failed(
        val failure: ConnectProbeFailure,
    ) : ConnectProbeResult
}

enum class ConnectProbeFailure {
    NeedsToken,
    TokenRejected,
    RepoNotVisible,
    ManifestInaccessible,
    RateLimited,
    NoManifest,
    MalformedManifest,
    Offline,
    Transient,
}
