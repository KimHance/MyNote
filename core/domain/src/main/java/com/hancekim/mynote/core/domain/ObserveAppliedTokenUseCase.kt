package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAppliedTokenUseCase @Inject constructor(
    private val source: AppliedTokenSource,
) {
    operator fun invoke(): Flow<String?> = source.applied()
}
