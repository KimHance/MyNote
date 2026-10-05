package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import javax.inject.Inject

class BeginConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(
        connection: RepoConnection,
        token: String? = null,
    ): PendingConnection = repository.begin(connection, token?.ifBlank { null })
}
