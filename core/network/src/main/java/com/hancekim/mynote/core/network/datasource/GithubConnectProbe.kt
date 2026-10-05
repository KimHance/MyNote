package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.domain.connect.ConnectBranch
import com.hancekim.mynote.core.domain.connect.ConnectCandidate
import com.hancekim.mynote.core.domain.connect.ConnectLinkRepo
import com.hancekim.mynote.core.domain.connect.ConnectProbe
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.connect.ConnectProbeResult
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.network.api.ConnectProbeApi
import com.hancekim.mynote.core.network.di.ProbeClient
import com.hancekim.mynote.core.network.failure.HttpFailureKind
import com.hancekim.mynote.core.network.failure.classifyHttp
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject

internal class GithubConnectProbe @Inject constructor(
    @ProbeClient private val api: ConnectProbeApi,
) : ConnectProbe {
    override suspend fun verify(
        repo: ConnectLinkRepo,
        candidates: List<ConnectCandidate>,
        token: String?,
    ): ConnectProbeResult =
        try {
            run(repo, candidates, token)
        } catch (failure: SerializationException) {
            ConnectProbeResult.Failed(ConnectProbeFailure.MalformedManifest)
        } catch (failure: InterruptedIOException) {
            ConnectProbeResult.Failed(ConnectProbeFailure.Transient)
        } catch (failure: IOException) {
            ConnectProbeResult.Failed(ConnectProbeFailure.Offline)
        }

    private suspend fun run(
        repo: ConnectLinkRepo,
        candidates: List<ConnectCandidate>,
        token: String?,
    ): ConnectProbeResult {
        val authorization = token?.let { "Bearer $it" }
        val repoResponse =
            try {
                api.getRepo(authorization, repo.owner, repo.repo)
            } catch (failure: SerializationException) {
                return ConnectProbeResult.Failed(ConnectProbeFailure.Transient)
            }
        if (!repoResponse.isSuccessful) {
            return ConnectProbeResult.Failed(
                repoFailure(classifyHttp(repoResponse.code(), repoResponse.headers()), token != null),
            )
        }
        val body = repoResponse.body() ?: return ConnectProbeResult.Failed(ConnectProbeFailure.Transient)
        val (owner, name) = body.fullName.split('/', limit = 2).takeIf { it.size == 2 } ?: listOf(repo.owner, repo.repo)

        for (candidate in candidates) {
            val branch =
                when (val named = candidate.branch) {
                    ConnectBranch.Default -> body.defaultBranch
                    is ConnectBranch.Named -> named.name
                }
            val response = api.getIndex(authorization, owner, name, candidate.indexPath, branch.ifBlank { null })
            if (response.isSuccessful) {
                return ConnectProbeResult.Verified(
                    RepoConnection(owner, name, branch, candidate.indexPath, isPublic = !body.isPrivate),
                    noteIds =
                        response
                            .body()
                            .orEmpty()
                            .values
                            .flatMap { notes -> notes.map { it.id } }
                            .toSet(),
                )
            }
            val kind = classifyHttp(response.code(), response.headers())
            if (kind != HttpFailureKind.NotFound) {
                return ConnectProbeResult.Failed(manifestFailure(kind, token != null))
            }
        }
        return ConnectProbeResult.Failed(
            if (token == null) ConnectProbeFailure.NoManifest else ConnectProbeFailure.ManifestInaccessible,
        )
    }

    private fun repoFailure(
        kind: HttpFailureKind,
        withToken: Boolean,
    ): ConnectProbeFailure =
        when (kind) {
            HttpFailureKind.RateLimited -> {
                ConnectProbeFailure.RateLimited
            }

            HttpFailureKind.Server -> {
                ConnectProbeFailure.Transient
            }

            HttpFailureKind.TokenRejected -> {
                if (withToken) ConnectProbeFailure.TokenRejected else ConnectProbeFailure.NeedsToken
            }

            HttpFailureKind.NotFound, HttpFailureKind.AccessDenied -> {
                if (withToken) ConnectProbeFailure.RepoNotVisible else ConnectProbeFailure.NeedsToken
            }
        }

    private fun manifestFailure(
        kind: HttpFailureKind,
        withToken: Boolean,
    ): ConnectProbeFailure =
        when (kind) {
            HttpFailureKind.RateLimited -> {
                ConnectProbeFailure.RateLimited
            }

            HttpFailureKind.TokenRejected -> {
                if (withToken) ConnectProbeFailure.TokenRejected else ConnectProbeFailure.NeedsToken
            }

            HttpFailureKind.NotFound, HttpFailureKind.AccessDenied -> {
                ConnectProbeFailure.ManifestInaccessible
            }

            HttpFailureKind.Server -> {
                ConnectProbeFailure.Transient
            }
        }
}
