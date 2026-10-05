package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import javax.inject.Inject

class RecoverConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke() = repository.recover()
}
