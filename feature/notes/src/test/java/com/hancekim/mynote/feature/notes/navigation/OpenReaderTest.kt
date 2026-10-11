package com.hancekim.mynote.feature.notes.navigation

import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.NavResult
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val WINDOW = 400L

private val home = NotesNavKey.Home
private val bookmarks = NotesNavKey.Bookmarks
private val settings = NotesNavKey.Settings
private val readerA = NotesNavKey.Reader("a")
private val readerB = NotesNavKey.Reader("b")

class OpenReaderTest {
    private fun TestScope.navigatorAt(vararg stack: MyNoteNavKey): Navigator {
        val navigator = Navigator(this, home, WINDOW) { currentTime }
        navigator.dispatchImmediately(NavCommand.ReplaceAll(stack.toList()))
        advanceUntilIdle()
        return navigator
    }

    private fun Navigator.openReader(noteId: String): NavResult = dispatch(stack.value.openReaderCommand(noteId))

    @Test
    fun `설정 위에서 노트를 고르면 설정 자리에 리더가 열린다`() =
        runTest {
            val navigator = navigatorAt(home, settings)

            assertEquals(NavResult.Applied, navigator.openReader("a"))
            assertEquals(listOf(home, readerA), navigator.stack.value)
        }

    @Test
    fun `북마크 위 설정에서 고르면 북마크 위에 리더가 열린다`() =
        runTest {
            val navigator = navigatorAt(home, bookmarks, settings)

            assertEquals(NavResult.Applied, navigator.openReader("a"))
            assertEquals(listOf(home, bookmarks, readerA), navigator.stack.value)
        }

    @Test
    fun `설정 아래가 같은 노트면 설정만 걷는다`() =
        runTest {
            val navigator = navigatorAt(home, readerA, settings)

            assertEquals(NavResult.Applied, navigator.openReader("a"))
            assertEquals(listOf(home, readerA), navigator.stack.value)
        }

    @Test
    fun `설정 아래가 다른 노트면 그 위에 리더가 쌓인다`() =
        runTest {
            val navigator = navigatorAt(home, readerA, settings)

            assertEquals(NavResult.Applied, navigator.openReader("b"))
            assertEquals(listOf(home, readerA, readerB), navigator.stack.value)
        }

    @Test
    fun `설정이 맨 위가 아니면 리더를 쌓는다`() =
        runTest {
            val navigator = navigatorAt(home)

            assertEquals(NavResult.Applied, navigator.openReader("a"))
            assertEquals(listOf(home, readerA), navigator.stack.value)
        }
}
