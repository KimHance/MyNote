package com.hancekim.mynote.core.network.status

import com.hancekim.mynote.core.domain.model.NetworkStatus

internal fun resolveNetworkStatus(
    hasInternet: Boolean,
    isValidated: Boolean,
    hasWifi: Boolean,
    hasCellular: Boolean,
): NetworkStatus =
    when {
        !hasInternet || !isValidated -> NetworkStatus.OFFLINE
        hasWifi -> NetworkStatus.WIFI
        hasCellular -> NetworkStatus.CELLULAR
        else -> NetworkStatus.OTHER
    }
