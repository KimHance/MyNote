package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import javax.inject.Inject

class ConfirmConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(pending: PendingConnection) = repository.confirm(pending)
}
