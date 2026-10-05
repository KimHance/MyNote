package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    operator fun invoke(): Flow<ConnectionState> = repository.observeConnection()
}
