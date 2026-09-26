package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.NetworkStatus

interface NetworkStatusSource {
    fun current(): NetworkStatus
}
