package com.hancekim.mynote.core.data.connection

import com.hancekim.mynote.core.domain.model.RepoConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val CONNECTION = RepoConnection("Foo", "Bar", "feature/x", "docs/index.json", isPublic = false)

class ConnectionConfigCodecTest {
    @Test
    fun `RC-12 연결은 저장했다가 그대로 읽힌다`() {
        val decoded = ConnectionConfigCodec.decode(ConnectionConfigCodec.encode(CONNECTION))

        assertEquals(CONNECTION, decoded?.connection)
        assertFalse(decoded?.pending ?: true)
    }

    @Test
    fun `RC-15 미확정 표식 상수는 인코딩 결과와 일치한다`() {
        assertTrue(
            ConnectionConfigCodec.encode(CONNECTION, pending = true).contains(ConnectionConfigCodec.PENDING_MARKER),
        )
        assertFalse(
            ConnectionConfigCodec.encode(CONNECTION, pending = false).contains(ConnectionConfigCodec.PENDING_MARKER),
        )
        assertEquals(
            true,
            ConnectionConfigCodec.decode(ConnectionConfigCodec.encode(CONNECTION, pending = true))?.pending,
        )
    }

    @Test
    fun `RC-12 읽을 수 없는 연결 정보는 연결 없음으로 본다`() {
        assertNull(ConnectionConfigCodec.decode(null))
        assertNull(ConnectionConfigCodec.decode(""))
        assertNull(ConnectionConfigCodec.decode("not json"))
        assertNull(
            ConnectionConfigCodec.decode("""{"owner":"","repo":"r","branch":"b","indexPath":"i","isPublic":true}"""),
        )
    }

    @Test
    fun `RC-14 같은 저장소는 대소문자 무시로 판정한다`() {
        assertTrue(CONNECTION.isSameRepositoryAs(CONNECTION.copy(owner = "foo", repo = "BAR", branch = "main")))
        assertFalse(CONNECTION.isSameRepositoryAs(CONNECTION.copy(repo = "Baz")))
        assertEquals("Foo/Bar", CONNECTION.fullName)
    }
}
