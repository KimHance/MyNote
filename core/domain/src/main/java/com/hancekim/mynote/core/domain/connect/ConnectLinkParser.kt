package com.hancekim.mynote.core.domain.connect

private const val INDEX_FILE = "index.json"
private const val DEFAULT_INDEX_PATH = "notes/$INDEX_FILE"
private const val MAX_CANDIDATES = 3

private val sshPattern = Regex("""^git@([^:/\s]+):(.+)$""")
private val schemePattern = Regex("""^([A-Za-z][A-Za-z0-9+.-]*):""")
internal val ownerPattern = Regex("""[A-Za-z0-9-]+""")
internal val repoPattern = Regex("""[A-Za-z0-9._-]+""")

private enum class HostKind { Github, Raw, Gist, Pages, Other }

fun parseConnectLink(input: String): ConnectLinkResult {
    val text =
        input
            .trim()
            .substringBefore('#')
            .substringBefore('?')
            .trim()
    if (text.isEmpty()) return reject(ConnectLinkRejection.Empty)

    sshPattern.matchEntire(text)?.let { ssh ->
        val kind = hostKind(ssh.groupValues[1])
        if (kind != HostKind.Github) return reject(rejectionOf(kind))
        return parseGithubPath(splitPath(ssh.groupValues[2]))
    }

    val scheme = schemePattern.find(text)
    if (scheme != null) {
        val rest = text.removePrefix(scheme.value)
        if (!scheme.groupValues[1].equals("https", ignoreCase = true) || !rest.startsWith("//")) {
            return reject(ConnectLinkRejection.NotHttps)
        }
        val segments = splitPath(rest)
        if (segments.isEmpty()) return reject(ConnectLinkRejection.NotGithub)
        return parseHosted(segments.first(), segments.drop(1))
    }

    val segments = splitPath(text)
    if (segments.isEmpty()) return reject(ConnectLinkRejection.Empty)
    return if ('.' in segments.first()) {
        parseHosted(segments.first(), segments.drop(1))
    } else {
        parseGithubPath(segments)
    }
}

private fun parseHosted(
    host: String,
    path: List<String>,
): ConnectLinkResult =
    when (val kind = hostKind(host)) {
        HostKind.Github -> parseGithubPath(path)
        HostKind.Raw -> parseRawPath(path)
        else -> reject(rejectionOf(kind))
    }

private fun hostKind(host: String): HostKind {
    val lower = host.lowercase()
    return when {
        lower == "github.com" || lower == "www.github.com" -> HostKind.Github
        lower == "raw.githubusercontent.com" -> HostKind.Raw
        lower == "gist.github.com" -> HostKind.Gist
        lower.endsWith(".github.io") -> HostKind.Pages
        else -> HostKind.Other
    }
}

private fun rejectionOf(kind: HostKind): ConnectLinkRejection =
    when (kind) {
        HostKind.Gist -> ConnectLinkRejection.Gist
        HostKind.Pages -> ConnectLinkRejection.GithubPages
        else -> ConnectLinkRejection.NotGithub
    }

private fun parseGithubPath(path: List<String>): ConnectLinkResult {
    val repo =
        when (val read = readRepo(path)) {
            is RepoRead.Found -> read.repo
            is RepoRead.Refused -> return reject(read.reason)
        }
    val rest = path.drop(2)
    val candidates =
        when (rest.firstOrNull()) {
            "tree" -> treeCandidates(rest.drop(1))
            "blob" -> fileCandidates(rest.drop(1))
            else -> listOf(defaultCandidate())
        } ?: return reject(ConnectLinkRejection.SingleFile)
    return ConnectLinkResult.Accepted(repo, candidates)
}

private fun parseRawPath(path: List<String>): ConnectLinkResult {
    val repo =
        when (val read = readRepo(path)) {
            is RepoRead.Found -> read.repo
            is RepoRead.Refused -> return reject(read.reason)
        }
    val candidates = fileCandidates(path.drop(2)) ?: return reject(ConnectLinkRejection.SingleFile)
    return ConnectLinkResult.Accepted(repo, candidates)
}

private sealed interface RepoRead {
    data class Found(
        val repo: ConnectLinkRepo,
    ) : RepoRead

    data class Refused(
        val reason: ConnectLinkRejection,
    ) : RepoRead
}

private fun readRepo(path: List<String>): RepoRead {
    val owner = path.getOrNull(0)
    val repo = path.getOrNull(1)?.removeSuffix(".git")
    return when {
        owner == null || repo.isNullOrEmpty() -> RepoRead.Refused(ConnectLinkRejection.MissingRepo)
        !ownerPattern.matches(owner) || !repoPattern.matches(repo) -> RepoRead.Refused(ConnectLinkRejection.NotGithub)
        else -> RepoRead.Found(ConnectLinkRepo(owner, repo))
    }
}

private fun treeCandidates(parts: List<String>): List<ConnectCandidate> {
    if (parts.isEmpty()) return listOf(defaultCandidate())
    val split =
        splitPoints(parts.size - 1).map { k ->
            ConnectCandidate(
                ConnectBranch.Named(parts.take(k).joinToString("/")),
                parts.drop(k).joinToString("/") + "/$INDEX_FILE",
            )
        }
    val whole = ConnectCandidate(ConnectBranch.Named(parts.joinToString("/")), DEFAULT_INDEX_PATH)
    return (split + whole).distinct().take(MAX_CANDIDATES)
}

private fun fileCandidates(parts: List<String>): List<ConnectCandidate>? {
    if (parts.size < 2 || parts.last() != INDEX_FILE) return null
    return splitPoints(parts.size - 1).map { k ->
        ConnectCandidate(
            ConnectBranch.Named(parts.take(k).joinToString("/")),
            parts.drop(k).joinToString("/"),
        )
    }
}

private fun splitPoints(max: Int): List<Int> = (max downTo 1).take(MAX_CANDIDATES)

private fun defaultCandidate() = ConnectCandidate(ConnectBranch.Default, DEFAULT_INDEX_PATH)

private fun splitPath(path: String): List<String> = path.split('/').filter { it.isNotEmpty() }

private fun reject(reason: ConnectLinkRejection) = ConnectLinkResult.Rejected(reason)
