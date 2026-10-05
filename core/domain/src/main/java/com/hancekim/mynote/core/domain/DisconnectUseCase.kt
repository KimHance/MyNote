package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import javax.inject.Inject

class DisconnectUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.disconnect()
}
