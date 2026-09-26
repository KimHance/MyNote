package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.NetworkStatus
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import javax.inject.Inject

class GetNetworkStatusUseCase @Inject constructor(
    private val source: NetworkStatusSource,
) {
    operator fun invoke(): NetworkStatus = source.current()
}
