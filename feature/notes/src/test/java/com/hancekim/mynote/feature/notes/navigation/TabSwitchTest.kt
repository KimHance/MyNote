package com.hancekim.mynote.feature.notes.navigation

import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.NavResult
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.element.NotesTab
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val WINDOW = 400L

private val home = NotesNavKey.Home
private val bookmarks = NotesNavKey.Bookmarks
private val readerA = NotesNavKey.Reader("a")
private val readerB = NotesNavKey.Reader("b")
private val stats = NotesNavKey.Stats

class TabSwitchTest {
    private fun TestScope.navigatorAt(vararg stack: MyNoteNavKey): Navigator {
        val navigator = Navigator(this, home, WINDOW) { currentTime }
        navigator.dispatchImmediately(NavCommand.ReplaceAll(stack.toList()))
        advanceUntilIdle()
        return navigator
    }

    private fun Navigator.switchTab(tab: NotesTab): NavResult = dispatch(stack.value.switchTabCommand(tab))

    @Test
    fun `홈 단독에서 북마크를 누르면 홈 위에 쌓인다`() =
        runTest {
            val navigator = navigatorAt(home)

            assertEquals(NavResult.Applied, navigator.switchTab(NotesTab.Bookmarks))
            assertEquals(listOf(home, bookmarks), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `홈에서 리더를 열어 둔 채 북마크를 누르면 리더를 버리고 북마크로 간다`() =
        runTest {
            val navigator = navigatorAt(home, readerA)

            assertEquals(NavResult.Applied, navigator.switchTab(NotesTab.Bookmarks))
            assertEquals(listOf(home, bookmarks), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `이미 북마크가 맨 위면 무시되고 쓰로틀 창도 열리지 않는다`() =
        runTest {
            val navigator = navigatorAt(home, bookmarks)

            assertEquals(NavResult.Ignored, navigator.switchTab(NotesTab.Bookmarks))
            assertEquals(listOf(home, bookmarks), navigator.stack.value)
            assertTrue(navigator.isSettled.value)
        }

    @Test
    fun `북마크 위에 리더가 있을 때 북마크를 누르면 리더만 닫힌다`() =
        runTest {
            val navigator = navigatorAt(home, bookmarks, readerA)

            assertEquals(NavResult.Applied, navigator.switchTab(NotesTab.Bookmarks))
            assertEquals(listOf(home, bookmarks), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `홈은 어디서 누르든 홈까지 한 번에 돌아간다`() =
        runTest {
            val fromBookmarks = navigatorAt(home, bookmarks, readerA)
            assertEquals(NavResult.Applied, fromBookmarks.switchTab(NotesTab.Home))
            assertEquals(listOf(home), fromBookmarks.stack.value)

            val fromStackedReaders = navigatorAt(home, readerA, readerB)
            assertEquals(NavResult.Applied, fromStackedReaders.switchTab(NotesTab.Home))
            assertEquals(listOf(home), fromStackedReaders.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `홈 단독에서 홈을 누르면 아무 일도 없다`() =
        runTest {
            val navigator = navigatorAt(home)

            assertEquals(NavResult.Ignored, navigator.switchTab(NotesTab.Home))
            assertEquals(listOf(home), navigator.stack.value)
            assertTrue(navigator.isSettled.value)
        }

    @Test
    fun `홈 단독에서 통계를 누르면 홈 위에 쌓인다`() =
        runTest {
            val navigator = navigatorAt(home)

            assertEquals(NavResult.Applied, navigator.switchTab(NotesTab.Stats))
            assertEquals(listOf(home, stats), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `리더나 북마크 위에서 통계를 누르면 홈 위의 통계 하나로 바뀐다`() =
        runTest {
            val fromReader = navigatorAt(home, readerA)
            assertEquals(NavResult.Applied, fromReader.switchTab(NotesTab.Stats))
            assertEquals(listOf(home, stats), fromReader.stack.value)

            val fromBookmarks = navigatorAt(home, bookmarks, readerA)
            assertEquals(NavResult.Applied, fromBookmarks.switchTab(NotesTab.Stats))
            assertEquals(listOf(home, stats), fromBookmarks.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `이미 통계가 맨 위면 무시되고 쓰로틀 창도 열리지 않는다`() =
        runTest {
            val navigator = navigatorAt(home, stats)

            assertEquals(NavResult.Ignored, navigator.switchTab(NotesTab.Stats))
            assertEquals(listOf(home, stats), navigator.stack.value)
            assertTrue(navigator.isSettled.value)
        }

    @Test
    fun `통계 위에 리더가 있을 때 통계를 누르면 리더만 닫힌다`() =
        runTest {
            val navigator = navigatorAt(home, stats, readerA, readerB)

            assertEquals(NavResult.Applied, navigator.switchTab(NotesTab.Stats))
            assertEquals(listOf(home, stats), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `지난 요약에서 통계를 누르면 통계로 돌아간다`() =
        runTest {
            val navigator = navigatorAt(home, stats, NotesNavKey.StatsHistory)

            assertEquals(NavResult.Applied, navigator.switchTab(NotesTab.Stats))
            assertEquals(listOf(home, stats), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `통계에서 홈과 북마크로 옮긴다`() =
        runTest {
            val toHome = navigatorAt(home, stats)
            assertEquals(NavResult.Applied, toHome.switchTab(NotesTab.Home))
            assertEquals(listOf(home), toHome.stack.value)

            val toBookmarks = navigatorAt(home, stats)
            assertEquals(NavResult.Applied, toBookmarks.switchTab(NotesTab.Bookmarks))
            assertEquals(listOf(home, bookmarks), toBookmarks.stack.value)
            advanceUntilIdle()
        }
}
