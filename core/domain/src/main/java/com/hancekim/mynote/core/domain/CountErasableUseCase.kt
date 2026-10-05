package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import javax.inject.Inject

class CountErasableUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(): ErasureCount = repository.countErasable()
}
