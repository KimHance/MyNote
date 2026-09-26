package com.hancekim.mynote.core.network.status

import com.hancekim.mynote.core.domain.model.NetworkStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkStatusResolverTest {
    @Test
    fun `와이파이로 붙어 있으면 WIFI`() {
        val status =
            resolveNetworkStatus(
                hasInternet = true,
                isValidated = true,
                hasWifi = true,
                hasCellular = false,
            )

        assertEquals(NetworkStatus.WIFI, status)
    }

    @Test
    fun `이동통신으로 붙어 있으면 CELLULAR`() {
        val status =
            resolveNetworkStatus(
                hasInternet = true,
                isValidated = true,
                hasWifi = false,
                hasCellular = true,
            )

        assertEquals(NetworkStatus.CELLULAR, status)
    }

    @Test
    fun `이더넷처럼 둘 다 아니면 OTHER`() {
        val status =
            resolveNetworkStatus(
                hasInternet = true,
                isValidated = true,
                hasWifi = false,
                hasCellular = false,
            )

        assertEquals(NetworkStatus.OTHER, status)
    }

    @Test
    fun `인터넷을 못 내보내는 망이면 OFFLINE`() {
        val status =
            resolveNetworkStatus(
                hasInternet = false,
                isValidated = true,
                hasWifi = true,
                hasCellular = false,
            )

        assertEquals(NetworkStatus.OFFLINE, status)
    }

    @Test
    fun `검증되지 않은 망이면 와이파이라도 OFFLINE`() {
        val status =
            resolveNetworkStatus(
                hasInternet = true,
                isValidated = false,
                hasWifi = true,
                hasCellular = false,
            )

        assertEquals(NetworkStatus.OFFLINE, status)
    }

    @Test
    fun `핸드오버로 둘 다 올라와 있으면 WIFI 를 택한다`() {
        val status =
            resolveNetworkStatus(
                hasInternet = true,
                isValidated = true,
                hasWifi = true,
                hasCellular = true,
            )

        assertEquals(NetworkStatus.WIFI, status)
    }
}
