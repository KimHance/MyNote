package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 네트워크에 적용된 토큰을 관찰(없으면 null). ViewModel 의 sync 트리거 소스. */
class ObserveAppliedTokenUseCase
    @Inject
    constructor(
        private val source: AppliedTokenSource,
    ) {
        operator fun invoke(): Flow<String?> = source.applied()
    }
