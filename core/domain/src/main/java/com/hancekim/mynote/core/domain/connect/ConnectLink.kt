package com.hancekim.mynote.core.domain.connect

data class ConnectLinkRepo(
    val owner: String,
    val repo: String,
)

sealed interface ConnectBranch {
    data object Default : ConnectBranch

    data class Named(
        val name: String,
    ) : ConnectBranch
}

data class ConnectCandidate(
    val branch: ConnectBranch,
    val indexPath: String,
)

enum class ConnectLinkRejection {
    Empty,
    NotHttps,
    NotGithub,
    Gist,
    GithubPages,
    SingleFile,
    MissingRepo,
}

sealed interface ConnectLinkResult {
    data class Accepted(
        val repo: ConnectLinkRepo,
        val candidates: List<ConnectCandidate>,
    ) : ConnectLinkResult

    data class Rejected(
        val reason: ConnectLinkRejection,
    ) : ConnectLinkResult
}
