package com.hancekim.mynote.core.network.status

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.hancekim.mynote.core.domain.model.NetworkStatus
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatusSourceImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NetworkStatusSource {
        private val connectivity: ConnectivityManager? =
            context.getSystemService(ConnectivityManager::class.java)

        override fun current(): NetworkStatus {
            val manager = connectivity ?: return NetworkStatus.OFFLINE
            val capabilities =
                manager.activeNetwork
                    ?.let { manager.getNetworkCapabilities(it) }
                    ?: return NetworkStatus.OFFLINE

            return resolveNetworkStatus(
                hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            )
        }
    }
