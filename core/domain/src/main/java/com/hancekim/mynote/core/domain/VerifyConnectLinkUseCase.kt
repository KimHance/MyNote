package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.connect.ConnectLinkResult
import com.hancekim.mynote.core.domain.connect.ConnectProbe
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.connect.ConnectProbeResult
import com.hancekim.mynote.core.domain.connect.ConnectReplacement
import com.hancekim.mynote.core.domain.connect.ConnectVerification
import com.hancekim.mynote.core.domain.connect.parseConnectLink
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.NetworkStatus
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import com.hancekim.mynote.core.domain.repository.TokenRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class VerifyConnectLinkUseCase @Inject constructor(
    private val probe: ConnectProbe,
    private val network: NetworkStatusSource,
    private val connections: ConnectionRepository,
    private val tokens: TokenRepository,
) {
    suspend operator fun invoke(
        link: String,
        candidateToken: String?,
    ): ConnectVerification {
        val parsed =
            when (val result = parseConnectLink(link)) {
                is ConnectLinkResult.Rejected -> return ConnectVerification.Rejected(result.reason)
                is ConnectLinkResult.Accepted -> result
            }
        if (network.current() == NetworkStatus.OFFLINE) {
            return ConnectVerification.Failed(ConnectProbeFailure.Offline)
        }
        val token = candidateToken?.trim()?.ifBlank { null }
        return when (val verified = probe.verify(parsed.repo, parsed.candidates, token)) {
            is ConnectProbeResult.Failed -> {
                ConnectVerification.Failed(verified.failure)
            }

            is ConnectProbeResult.Verified -> {
                ConnectVerification.Ready(
                    connection = verified.connection,
                    candidateToken = token,
                    replacement = replacementOf(verified.connection, token),
                )
            }
        }
    }

    private suspend fun replacementOf(
        connection: RepoConnection,
        candidateToken: String?,
    ): ConnectReplacement =
        when (val current = connections.observeConnection().first()) {
            is ConnectionState.Connected -> {
                if (current.connection.isSameRepositoryAs(connection)) {
                    ConnectReplacement.None
                } else {
                    ConnectReplacement.Erases(
                        count = connections.countErasable(),
                        dropsToken =
                            connection.isPublic && candidateToken == null && tokens.currentToken() != null,
                        adopts = false,
                    )
                }
            }

            ConnectionState.None -> {
                val count = connections.countErasable()
                if (count.notes > 0) {
                    ConnectReplacement.Erases(count = count, dropsToken = false, adopts = true)
                } else {
                    ConnectReplacement.None
                }
            }
        }
}
