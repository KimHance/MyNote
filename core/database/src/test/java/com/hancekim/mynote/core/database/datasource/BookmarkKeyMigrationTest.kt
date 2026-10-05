package com.hancekim.mynote.core.database.datasource

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkKeyMigrationTest {
    @Test
    fun `슬래시 없는 북마크 키에 s0 접두를 단다`() =
        runTest {
            val before = preferencesOf(longPreferencesKey("graph") to 10L, longPreferencesKey("nav3") to 20L)

            val after = BookmarkKeyMigration.migrate(before)

            assertEquals(
                mapOf("s0/graph" to 10L, "s0/nav3" to 20L),
                after.asMap().mapKeys { it.key.name },
            )
        }

    @Test
    fun `이미 s0 키가 있으면 그 값을 남기고 옛 키를 지운다`() =
        runTest {
            val before = preferencesOf(longPreferencesKey("graph") to 10L, longPreferencesKey("s0/graph") to 99L)

            val after = BookmarkKeyMigration.migrate(before)

            assertEquals(mapOf("s0/graph" to 99L), after.asMap().mapKeys { it.key.name })
        }

    @Test
    fun `Long 이 아닌 값과 이미 접두가 있는 키는 그대로 둔다`() =
        runTest {
            val before =
                preferencesOf(
                    stringPreferencesKey("odd") to "x",
                    longPreferencesKey("s1/clip") to 5L,
                    longPreferencesKey("graph") to 10L,
                )

            val after = BookmarkKeyMigration.migrate(before)

            assertEquals(
                mapOf("odd" to "x", "s1/clip" to 5L, "s0/graph" to 10L),
                after.asMap().mapKeys { it.key.name },
            )
        }

    @Test
    fun `옮길 키가 없으면 shouldMigrate 가 false 다`() =
        runTest {
            assertFalse(BookmarkKeyMigration.shouldMigrate(preferencesOf()))
            assertFalse(BookmarkKeyMigration.shouldMigrate(preferencesOf(longPreferencesKey("s0/graph") to 1L)))
            assertFalse(BookmarkKeyMigration.shouldMigrate(preferencesOf(stringPreferencesKey("odd") to "x")))
            assertTrue(BookmarkKeyMigration.shouldMigrate(preferencesOf(longPreferencesKey("graph") to 1L)))
        }

    @Test
    fun `두 번 돌려도 결과가 같다`() =
        runTest {
            val once = BookmarkKeyMigration.migrate(preferencesOf(longPreferencesKey("graph") to 10L))

            assertFalse(BookmarkKeyMigration.shouldMigrate(once))
            assertEquals(once, BookmarkKeyMigration.migrate(once))
        }
}
