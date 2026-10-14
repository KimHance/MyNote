package com.hancekim.mynote.feature.editor

import com.hancekim.mynote.core.domain.CheckDeviceNoteDraftUseCase
import com.hancekim.mynote.core.domain.DeleteDeviceNoteUseCase
import com.hancekim.mynote.core.domain.ImportNoteUseCase
import com.hancekim.mynote.core.domain.LoadDeviceNoteUseCase
import com.hancekim.mynote.core.domain.PreviewDeviceNoteUseCase
import com.hancekim.mynote.core.domain.SaveDeviceNoteUseCase
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.FileText
import com.hancekim.mynote.core.domain.model.ImportRejection
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import com.hancekim.mynote.core.domain.repository.DeviceNotesRepository
import com.hancekim.mynote.core.domain.repository.NoteFileReader
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.NavResult
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import com.hancekim.mynote.core.ui.signal.SharedTextInbox
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

private const val WINDOW = 400L
private const val DEFAULT_GROUP = "그룹 없음"
private val home = NotesNavKey.Home
private val readerX = NotesNavKey.Reader("local/x.md")

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `LN-8 공유 진입 스택은 꺼져 있으면 홈과 편집기다`() =
        runTest {
            val f = Fixture(this)
            f.inbox.put("# 공유 제목\n본문", subject = null)

            val vm = f.vm()

            val editor = f.stack().last() as NotesNavKey.Editor
            assertEquals(listOf(home, editor), f.stack())
            assertEquals(
                "본문",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )
            assertNull("꺼낸 글은 받은함에 남지 않는다", f.inbox.pending.value)
        }

    @Test
    fun `LN-8 떠 있으면 지금 스택 위에 편집기다`() =
        runTest {
            val f = Fixture(this)
            f.at(home, readerX)
            f.vm()

            f.inbox.put("글", subject = "제목")

            assertEquals(listOf(home, readerX), f.stack().dropLast(1))
            assertTrue(f.stack().last() is NotesNavKey.Editor)
        }

    @Test
    fun `LN-8 공유 위 공유는 편집기를 갈아 끼운다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            f.inbox.put("첫 글", subject = "첫")
            val first = f.stack().last()

            f.inbox.put("둘째 글", subject = "둘")

            assertEquals(2, f.stack().size)
            assertTrue(f.stack().last() != first)
            assertEquals(
                "둘",
                vm.state.value.session
                    ?.draft
                    ?.title,
            )
        }

    @Test
    fun `LN-8 저장 안 한 편집기가 있으면 먼저 묻는다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            f.inbox.put("첫 글", subject = "첫")
            vm.onIntent(EditorIntent.BodyChanged("고친 글"))
            val before = f.stack()

            f.inbox.put("둘째 글", subject = "둘")

            assertTrue(vm.state.value.exitConfirm)
            assertEquals(before, f.stack())
            // 계속 쓰기 — 새 공유는 버려지고 쓰던 글이 그대로다
            vm.onIntent(EditorIntent.ExitAnswered(discard = false))
            assertFalse(vm.state.value.exitConfirm)
            assertEquals(before, f.stack())
            assertEquals(
                "고친 글",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )

            // 버리기 — 새 공유가 편집기를 갈아 끼운다
            f.inbox.put("셋째 글", subject = "셋")
            vm.onIntent(EditorIntent.ExitAnswered(discard = true))
            assertEquals(2, f.stack().size)
            assertEquals(
                "셋",
                vm.state.value.session
                    ?.draft
                    ?.title,
            )
        }

    @Test
    fun `LN-8 공유 글이 비어 있으면 편집기가 열리지 않고 이유가 보인다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()

            f.inbox.put("   ", subject = null)

            assertEquals(listOf(home), f.stack())
            assertEquals(
                "rejected:Empty",
                vm.state.value.notice
                    ?.message,
            )
        }

    @Test
    fun `LN-10 바뀐 것이 있으면 나가기 전에 묻는다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.TitleChanged("가"))
            assertTrue(vm.state.value.guardsExit)

            vm.onIntent(EditorIntent.Close)
            assertTrue(vm.state.value.exitConfirm)
            vm.onIntent(EditorIntent.ExitAnswered(discard = false))
            assertEquals(
                "가",
                vm.state.value.session
                    ?.draft
                    ?.title,
            )

            f.settle()
            vm.onIntent(EditorIntent.Close)
            vm.onIntent(EditorIntent.ExitAnswered(discard = true))
            assertEquals(listOf(home), f.stack())
            assertNull(vm.state.value.session)
            assertEquals(0, f.repository.created.size)
        }

    @Test
    fun `LN-10 바뀐 것이 없으면 바로 닫힌다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            f.settle()

            vm.onIntent(EditorIntent.Close)

            assertFalse(vm.state.value.exitConfirm)
            assertEquals(listOf(home), f.stack())
        }

    @Test
    fun `LN-10 미리 채운 값이 기준이다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            f.inbox.put("미리 채운 글", subject = "미리")

            assertFalse(vm.state.value.guardsExit)
            vm.onIntent(EditorIntent.SetPreview(on = true))
            assertFalse("미리보기 전환은 바뀜이 아니다", vm.state.value.guardsExit)
            vm.onIntent(EditorIntent.BodyChanged("미리 채운 글!"))
            assertTrue(vm.state.value.guardsExit)
            vm.onIntent(EditorIntent.BodyChanged("미리 채운 글"))
            assertFalse(vm.state.value.guardsExit)
        }

    @Test
    fun `LN-10 화면을 다시 만들어도 편집기 입력이 남는다`() =
        runTest {
            // 입력은 Activity owner ViewModel 이 든다 — 편집기 키가 스택에 있는 동안 위에 무엇이 쌓였다 빠져도 세션은 그대로다
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.BodyChanged("쓰던 글"))
            vm.onIntent(EditorIntent.SetPreview(on = true))

            f.navigator.dispatchImmediately(NavCommand.Push(NotesNavKey.Settings))
            f.navigator.dispatchImmediately(NavCommand.Pop)

            assertEquals(
                "쓰던 글",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )
            assertTrue(
                vm.state.value.session
                    ?.previewing == true,
            )
        }

    @Test
    fun `LN-10 버리기는 같은 키의 편집기도 새로 연다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            // 새 노트 위에 새 노트 — 키가 같아 스택 명령이 무시되어도 쓰던 글이 남지 않는다
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.BodyChanged("쓰던 글"))
            f.settle()
            vm.onIntent(EditorIntent.NewNote)
            assertTrue(vm.state.value.exitConfirm)
            vm.onIntent(EditorIntent.ExitAnswered(discard = true))

            assertEquals(listOf(home, NotesNavKey.Editor()), f.stack())
            assertEquals(
                "",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )
            assertFalse(vm.state.value.guardsExit)
        }

    @Test
    fun `LN-10 버리기는 같은 노트를 다시 고칠 때 지금 값을 다시 읽는다`() =
        runTest {
            val f = Fixture(this)
            f.repository.stored[readerX.noteId] = DeviceNoteDraft("제목", "", "저장된 본문")
            f.at(home, readerX)
            val vm = f.vm()
            vm.onIntent(EditorIntent.Edit(readerX.noteId))
            vm.onIntent(EditorIntent.BodyChanged("고치던 본문"))
            f.settle()

            vm.onIntent(EditorIntent.Edit(readerX.noteId))
            vm.onIntent(EditorIntent.ExitAnswered(discard = true))

            assertEquals(listOf(home, readerX, NotesNavKey.Editor(noteId = readerX.noteId)), f.stack())
            assertEquals(
                "저장된 본문",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )
            assertFalse(vm.state.value.guardsExit)
        }

    @Test
    fun `LN-10 넓은 창에서 같은 키를 버리고 다시 열면 새 편집기가 리더 위로 온다`() =
        runTest {
            // plan D39 — 목록 패널에서 연 리더가 편집기 위에 쌓인 채 새 노트를 누른 경우. 키가 같아도 편집기가 리더 밑에 깔리지 않는다
            val f = Fixture(this)
            val vm = f.vm()
            val readerA = NotesNavKey.Reader("local/a.md")
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.BodyChanged("쓰던 글"))
            f.settle()
            f.navigator.dispatch(NavCommand.Push(readerA))
            f.settle()
            assertEquals(listOf(home, NotesNavKey.Editor(), readerA), f.stack())

            vm.onIntent(EditorIntent.NewNote)
            assertTrue(vm.state.value.exitConfirm)
            vm.onIntent(EditorIntent.ExitAnswered(discard = true))

            assertEquals(listOf(home, readerA, NotesNavKey.Editor()), f.stack())
            assertEquals(
                "",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )
            assertFalse(vm.state.value.guardsExit)
        }

    @Test
    fun `LN-10 바뀐 편집기를 떼는 탭 전환은 먼저 묻는다`() =
        runTest {
            // 넓은 창 — 편집기가 상세 칸에 있어도 플로팅 내비 · 목록 패널이 보인다(plan D32)
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.BodyChanged("쓰던 글"))
            f.settle()
            val bookmarksTab = listOf<MyNoteNavKey>(home, NotesNavKey.Bookmarks)

            assertEquals(NavResult.Guarded, f.navigator.dispatch(NavCommand.ReplaceAll(bookmarksTab)))
            assertTrue(vm.state.value.exitConfirm)
            assertEquals(listOf(home, NotesNavKey.Editor()), f.stack())

            // 계속 쓰기 — 그대로
            vm.onIntent(EditorIntent.ExitAnswered(discard = false))
            assertEquals(listOf(home, NotesNavKey.Editor()), f.stack())
            assertEquals(
                "쓰던 글",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )

            // 버리기 — 막혔던 이동을 마저 한다
            f.navigator.dispatch(NavCommand.ReplaceAll(bookmarksTab))
            vm.onIntent(EditorIntent.ExitAnswered(discard = true))
            assertEquals(bookmarksTab, f.stack())
            assertNull(vm.state.value.session)
        }

    @Test
    fun `LN-10 편집기를 남기는 이동과 바뀌지 않은 편집기는 막지 않는다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.BodyChanged("쓰던 글"))
            f.settle()

            // 목록 패널에서 다른 노트를 열면 리더가 편집기 위에 쌓인다 — 편집기는 남으니 묻지 않는다
            assertEquals(NavResult.Applied, f.navigator.dispatch(NavCommand.Push(NotesNavKey.Reader("local/a.md"))))
            assertFalse(vm.state.value.exitConfirm)
            f.settle()
            f.navigator.dispatch(NavCommand.Pop)
            f.settle()

            vm.onIntent(EditorIntent.BodyChanged(""))
            assertEquals(NavResult.Applied, f.navigator.dispatch(NavCommand.ReplaceAll(listOf(home))))
            assertFalse(vm.state.value.exitConfirm)
        }

    @Test
    fun `LN-8 파일 공유는 파일 열기 규칙으로 미리 채운 편집기를 연다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()

            f.inbox.putFile("content://files/flow.md", subject = null)

            assertEquals(2, f.stack().size)
            assertTrue(f.stack().last() is NotesNavKey.Editor)
            assertEquals(
                "파일 제목",
                vm.state.value.session
                    ?.draft
                    ?.title,
            )
            assertEquals(0, f.repository.created.size)
        }

    @Test
    fun `LN-8 받지 않는 파일 공유는 LN-7 이유로 알린다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()

            f.inbox.putFile("content://files/scan.pdf", subject = "스캔")

            assertEquals(listOf(home), f.stack())
            assertEquals(
                "rejected:${ImportRejection.UnsupportedType}",
                vm.state.value.notice
                    ?.message,
            )
        }

    @Test
    fun `LN-8 트램펄린이 거른 파일 공유는 읽지 않고 LN-7 받지 않는 종류로 알린다`() =
        runTest {
            // plan D39 — content 주소가 아니거나 이 앱 자신의 주소. 파일 리더에 닿지 않는다
            val f = Fixture(this)
            val vm = f.vm(files = UnreachableFiles)

            f.inbox.putRefused(subject = "flow")

            assertEquals(listOf(home), f.stack())
            assertNull(vm.state.value.session)
            assertEquals(
                "rejected:${ImportRejection.UnsupportedType}",
                vm.state.value.notice
                    ?.message,
            )
        }

    @Test
    fun `LN-3 저장 뒤 스택은 편집기 자리에 리더다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.TitleChanged("제목"))
            vm.onIntent(EditorIntent.BodyChanged("본문"))

            vm.onIntent(EditorIntent.Save)

            assertEquals(listOf(DeviceNoteDraft("제목", "", "본문") to DEFAULT_GROUP), f.repository.created)
            assertEquals(listOf(home, NotesNavKey.Reader("local/new-1.md")), f.stack())
            assertNull(vm.state.value.session)
        }

    @Test
    fun `LN-3 미리보기는 저장할 본문을 해석한다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            vm.onIntent(EditorIntent.BodyChanged("본문"))

            vm.onIntent(EditorIntent.SetPreview(on = true))

            assertEquals(listOf("본문"), f.repository.previewed)
            assertEquals(
                1,
                vm.state.value.session
                    ?.previewBlocks
                    ?.size,
            )
        }

    @Test
    fun `LN-9 고쳐 저장하면 원래 리더로 돌아간다`() =
        runTest {
            val f = Fixture(this)
            f.repository.stored[readerX.noteId] = DeviceNoteDraft("옛 제목", "kotlin", "옛 본문", LocalDate.of(2026, 10, 14))
            f.at(home, readerX)
            val vm = f.vm()

            vm.onIntent(EditorIntent.Edit(readerX.noteId))
            assertEquals(listOf(home, readerX, NotesNavKey.Editor(noteId = readerX.noteId)), f.stack())
            assertEquals(
                "옛 본문",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )
            assertTrue(
                vm.state.value.session
                    ?.editing == true,
            )
            vm.onIntent(EditorIntent.BodyChanged("새 본문"))
            vm.onIntent(EditorIntent.Save)

            assertEquals(
                readerX.noteId,
                f.repository.updated
                    .single()
                    .first,
            )
            assertEquals(
                "새 본문",
                f.repository.updated
                    .single()
                    .second.body,
            )
            assertEquals(listOf(home, readerX), f.stack())
        }

    @Test
    fun `LN-11 지우면 리더와 퀴즈가 닫힌다`() =
        runTest {
            val f = Fixture(this)
            f.at(home, readerX)
            val vm = f.vm()

            vm.onIntent(EditorIntent.RequestDelete(readerX.noteId))
            assertEquals(readerX.noteId, vm.state.value.deleteConfirmNoteId)
            vm.onIntent(EditorIntent.DeleteAnswered(confirm = true))

            assertEquals(listOf(readerX.noteId), f.repository.deleted)
            assertEquals(listOf(home), f.stack())
            assertEquals(1, f.signals.quizCloses.value)
            assertEquals(
                "deleted",
                vm.state.value.notice
                    ?.message,
            )
            assertNull(vm.state.value.deleteConfirmNoteId)
        }

    @Test
    fun `LN-11 취소하면 변화가 없다`() =
        runTest {
            val f = Fixture(this)
            f.at(home, readerX)
            val vm = f.vm()

            vm.onIntent(EditorIntent.RequestDelete(readerX.noteId))
            vm.onIntent(EditorIntent.DeleteAnswered(confirm = false))

            assertEquals(emptyList<String>(), f.repository.deleted)
            assertEquals(listOf(home, readerX), f.stack())
            assertEquals(0, f.signals.quizCloses.value)
            assertNull(vm.state.value.notice)
        }

    @Test
    fun `LN-6 가져오기 그릇은 홈의 요청으로 열리고 붙여넣기는 미리 채운 편집기로 이어진다`() =
        runTest {
            val f = Fixture(this)
            f.signals.requestImport()
            val vm = f.vm()
            assertEquals("만들어지기 전의 요청은 이미 본 것이다", ImportDialogUi.Closed, vm.state.value.importDialog)

            f.signals.requestImport()
            assertEquals(ImportDialogUi.Choosing, vm.state.value.importDialog)
            vm.onIntent(EditorIntent.ChoosePaste)
            assertEquals(ImportDialogUi.Closed, vm.state.value.importDialog)
            assertEquals(listOf(home, NotesNavKey.Paste), f.stack())
            f.settle()

            vm.onIntent(EditorIntent.PasteChanged("붙인 글"))
            vm.onIntent(EditorIntent.PasteNext)

            assertEquals(2, f.stack().size)
            assertTrue(f.stack().last() is NotesNavKey.Editor)
            assertEquals(
                "붙인 글",
                vm.state.value.session
                    ?.draft
                    ?.body,
            )
            assertEquals("", vm.state.value.pasteText)
        }

    @Test
    fun `LN-6 저장하기 전에는 노트가 생기지 않는다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            f.inbox.put("공유 글", subject = "공유")
            vm.onIntent(EditorIntent.ChooseFile)
            vm.onIntent(EditorIntent.FileChosen("content://notes/flow.md"))

            assertEquals(0, f.repository.created.size)
            assertEquals(1, vm.state.value.filePickRequestId)
        }

    @Test
    fun `LN-7 가져오기 거절은 편집기를 열지 않고 이유를 알린다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            f.signals.requestImport()

            vm.onIntent(EditorIntent.FileChosen("content://notes/flow.txt"))

            assertEquals(listOf(home), f.stack())
            assertEquals(
                "rejected:${ImportRejection.UnsupportedType}",
                vm.state.value.notice
                    ?.message,
            )
            assertEquals(ImportDialogUi.Closed, vm.state.value.importDialog)
        }

    @Test
    fun `LN-7 고르지 않고 돌아오면 아무 일도 없다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            f.signals.requestImport()

            vm.onIntent(EditorIntent.FileChosen(null))

            assertEquals(listOf(home), f.stack())
            assertNull(vm.state.value.notice)
            assertEquals(ImportDialogUi.Choosing, vm.state.value.importDialog)
        }

    @Test
    fun `LN-3 제목이나 본문이 비면 저장이 꺼지고 빈 칸 이유가 보인다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            f.settle()

            assertEquals(
                SaveBlock.Empty,
                vm.state.value.session
                    ?.saveBlock,
            )
            vm.onIntent(EditorIntent.TitleChanged("제목"))
            assertEquals(
                "본문이 비면 그대로 빈 칸 이유다",
                SaveBlock.Empty,
                vm.state.value.session
                    ?.saveBlock,
            )
            vm.onIntent(EditorIntent.BodyChanged("  \n "))
            assertEquals(
                SaveBlock.Empty,
                vm.state.value.session
                    ?.saveBlock,
            )

            vm.onIntent(EditorIntent.BodyChanged("본문"))

            assertNull(
                vm.state.value.session
                    ?.saveBlock,
            )
            assertTrue(
                vm.state.value.session!!
                    .canSave,
            )
        }

    @Test
    fun `LN-3 크기를 넘으면 저장이 꺼지고 이유 줄에 본문 KB 가 보인다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            f.settle()
            vm.onIntent(EditorIntent.TitleChanged("제목"))

            // 대역 경계는 1,000 글자 — 2,049 바이트 본문은 넘고 이유 줄은 올림한 3KB 다
            vm.onIntent(EditorIntent.BodyChanged("a".repeat(2_049)))

            val over = vm.state.value.session!!
            assertEquals(SaveBlock.TooLarge, over.saveBlock)
            assertEquals(3, over.bodyKilobytes)
            assertFalse(over.canSave)

            vm.onIntent(EditorIntent.BodyChanged("a".repeat(1_000)))

            val back = vm.state.value.session!!
            assertNull(back.saveBlock)
            assertEquals("넘지 않으면 세지 않는다", 0, back.bodyKilobytes)
            assertTrue(back.canSave)
        }

    @Test
    fun `LN-3 미리보기 중에 제목을 고쳐도 미리보기 블록이 남고 본문을 고치면 다시 해석한다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            f.settle()
            vm.onIntent(EditorIntent.BodyChanged("본문"))
            vm.onIntent(EditorIntent.SetPreview(true))
            assertEquals(listOf("본문"), f.repository.previewed)

            vm.onIntent(EditorIntent.TitleChanged("새 제목"))
            vm.onIntent(EditorIntent.GroupChanged("kotlin"))

            assertTrue(
                vm.state.value.session!!
                    .previewBlocks != null,
            )
            assertEquals("제목 · 그룹은 다시 해석하지 않는다", 1, f.repository.previewed.size)

            vm.onIntent(EditorIntent.SetPreview(false))
            vm.onIntent(EditorIntent.BodyChanged("고친 본문"))
            vm.onIntent(EditorIntent.SetPreview(true))

            assertEquals(listOf("본문", "고친 본문"), f.repository.previewed)
        }

    @Test
    fun `LN-3 제목과 그룹 칸은 한 줄 200자다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.NewNote)
            f.settle()

            vm.onIntent(EditorIntent.TitleChanged("첫 줄\r\n둘째 줄" + "가".repeat(300)))
            vm.onIntent(EditorIntent.GroupChanged("kotlin\n" + "b".repeat(300)))

            val draft =
                vm.state.value.session!!
                    .draft
            assertEquals(200, draft.title.codePointCount(0, draft.title.length))
            assertTrue(draft.title.startsWith("첫 줄 둘째 줄"))
            assertEquals(200, draft.group.length)
            assertTrue(draft.group.startsWith("kotlin b"))
        }

    @Test
    fun `LN-7 붙여넣기 글이 512KB 를 넘으면 다음이 꺼지고 이유 줄이 보인다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.ChoosePaste)
            f.settle()
            assertEquals(PasteBlock.Empty, vm.state.value.pasteBlock)

            vm.onIntent(EditorIntent.PasteChanged("a".repeat(524_288)))
            assertNull("524,288 바이트는 받는다", vm.state.value.pasteBlock)

            vm.onIntent(EditorIntent.PasteChanged("a".repeat(524_287) + "가"))
            assertEquals(PasteBlock.TooLarge, vm.state.value.pasteBlock)
            vm.onIntent(EditorIntent.PasteNext)

            assertEquals("꺼진 다음은 편집기를 열지 않는다", listOf(home, NotesNavKey.Paste), f.stack())
            assertNull("이유는 이유 줄이 이미 보인다 — 스낵바를 겹치지 않는다", vm.state.value.notice)
        }

    @Test
    fun `LN-7 빈 붙여넣기는 다음이 꺼진다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            vm.onIntent(EditorIntent.ChoosePaste)
            f.settle()

            vm.onIntent(EditorIntent.PasteChanged(" \n\t"))
            vm.onIntent(EditorIntent.PasteNext)

            assertEquals(PasteBlock.Empty, vm.state.value.pasteBlock)
            assertEquals(listOf(home, NotesNavKey.Paste), f.stack())
            assertNull(vm.state.value.notice)
        }

    @Test
    fun `LN-6 파일을 읽는 중에 취소하면 가져온 글을 버린다`() =
        runTest {
            val f = Fixture(this)
            val gate = CompletableDeferred<Unit>()
            val vm = f.vm(files = GatedFiles(gate))
            f.signals.requestImport()

            vm.onIntent(EditorIntent.FileChosen("content://notes/flow.md"))
            assertEquals(ImportDialogUi.Reading, vm.state.value.importDialog)

            vm.onIntent(EditorIntent.DismissImport)
            gate.complete(Unit)

            assertEquals(ImportDialogUi.Closed, vm.state.value.importDialog)
            assertEquals(listOf(home), f.stack())
            assertNull(vm.state.value.notice)
        }

    @Test
    fun `LN-6 다 읽으면 그릇이 닫히고 미리 채운 편집기가 열린다`() =
        runTest {
            val f = Fixture(this)
            val gate = CompletableDeferred<Unit>()
            val vm = f.vm(files = GatedFiles(gate))
            f.signals.requestImport()
            vm.onIntent(EditorIntent.FileChosen("content://notes/flow.md"))

            gate.complete(Unit)

            assertEquals(ImportDialogUi.Closed, vm.state.value.importDialog)
            assertTrue(f.stack().last() is NotesNavKey.Editor)
            assertEquals(
                "파일 제목",
                vm.state.value.session
                    ?.draft
                    ?.title,
            )
        }

    @Test
    fun `FM-16 본 알림은 다시 나오지 않는다`() =
        runTest {
            val f = Fixture(this)
            val vm = f.vm()
            f.inbox.put("", subject = null)
            val id =
                vm.state.value.notice!!
                    .id

            vm.onIntent(EditorIntent.SignalShown(NotesSignal.Notice, id))

            assertNull(vm.state.value.notice)
        }
}

private class Fixture(
    private val scope: TestScope,
) {
    val navigator = Navigator(scope.backgroundScope, NotesNavKey.Home, WINDOW) { scope.currentTime }
    val signals = FeatureSignals()
    val inbox = SharedTextInbox()
    val repository = FakeDeviceNotes()
    private val text = FakeText()

    fun at(vararg stack: MyNoteNavKey) {
        navigator.dispatchImmediately(NavCommand.ReplaceAll(stack.toList()))
        settle()
    }

    fun settle() = scope.advanceTimeBy(WINDOW + 1)

    fun stack(): List<MyNoteNavKey> = navigator.stack.value

    fun vm(files: NoteFileReader = FakeFiles()): EditorViewModel {
        val vm =
            EditorViewModel(
                navigator = navigator,
                signals = signals,
                inbox = inbox,
                checkDraft = CheckDeviceNoteDraftUseCase(text),
                saveNote = SaveDeviceNoteUseCase(repository),
                deleteNote = DeleteDeviceNoteUseCase(repository),
                loadNote = LoadDeviceNoteUseCase(repository),
                previewNote = PreviewDeviceNoteUseCase(repository),
                importNote = ImportNoteUseCase(text, files, { LocalDate.of(2026, 10, 14) }),
                messages = FakeMessages,
                failureLog = { _, _, cause -> throw AssertionError(cause) },
            )
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { vm.state.collect {} }
        return vm
    }
}

private class FakeDeviceNotes : DeviceNotesRepository {
    val created = mutableListOf<Pair<DeviceNoteDraft, String>>()
    val updated = mutableListOf<Pair<String, DeviceNoteDraft>>()
    val deleted = mutableListOf<String>()
    val previewed = mutableListOf<String>()
    val stored = mutableMapOf<String, DeviceNoteDraft>()

    override suspend fun create(
        draft: DeviceNoteDraft,
        defaultGroup: String,
    ): Result<String> {
        created += draft to defaultGroup
        return Result.success("local/new-${created.size}.md")
    }

    override suspend fun update(
        noteId: String,
        draft: DeviceNoteDraft,
        defaultGroup: String,
    ): Result<Unit> {
        updated += noteId to draft
        return Result.success(Unit)
    }

    override suspend fun delete(noteId: String): Result<Unit> {
        deleted += noteId
        return Result.success(Unit)
    }

    override suspend fun load(noteId: String): DeviceNoteDraft? = stored[noteId]

    override suspend fun preview(body: String): List<NoteBlock> {
        previewed += body
        return listOf(NoteBlock.Paragraph(emptyList()))
    }
}

/** 가져오기 규칙 대역 — 첫 줄이 `# ` 면 제목으로 떼고, 아니면 이름 후보가 제목이다. 규칙 자체는 `:core:format` 시험이 본다. */
private class FakeText : DeviceNoteText {
    override fun fits(body: String): Boolean = body.length <= LIMIT

    override fun rawFits(text: String): Boolean = text.length <= LIMIT

    override fun imported(
        text: String,
        nameHint: String?,
        today: LocalDate,
    ): DeviceNoteDraft {
        val lines = text.lines()
        val heading = lines.first().takeIf { it.startsWith("# ") }
        return if (heading != null) {
            DeviceNoteDraft(title = heading.removePrefix("# "), body = lines.drop(1).joinToString("\n"), date = today)
        } else {
            DeviceNoteDraft(title = nameHint.orEmpty(), body = text, date = today)
        }
    }

    private companion object {
        const val LIMIT = 1_000
    }
}

private class FakeFiles : NoteFileReader {
    override suspend fun displayName(uri: String): String? = uri.substringAfterLast('/')

    override suspend fun read(
        uri: String,
        maxBytes: Int,
    ): FileText = FileText.Read("# 파일 제목\n파일 본문")
}

/** 닿으면 실패하는 대역 — 거른 공유가 파일을 읽지 않는지 본다. */
private object UnreachableFiles : NoteFileReader {
    override suspend fun displayName(uri: String): String? = throw AssertionError("읽으면 안 된다: $uri")

    override suspend fun read(
        uri: String,
        maxBytes: Int,
    ): FileText = throw AssertionError("읽으면 안 된다: $uri")
}

/** 읽기를 [gate] 가 열릴 때까지 붙잡는 대역 — 클라우드 파일처럼 느린 읽기(디자인 C18). */
private class GatedFiles(
    private val gate: CompletableDeferred<Unit>,
) : NoteFileReader {
    override suspend fun displayName(uri: String): String? = uri.substringAfterLast('/')

    override suspend fun read(
        uri: String,
        maxBytes: Int,
    ): FileText {
        gate.await()
        return FileText.Read("# 파일 제목\n파일 본문")
    }
}

private object FakeMessages : EditorMessages {
    override fun defaultGroup() = DEFAULT_GROUP

    override fun rejected(reason: ImportRejection) = "rejected:$reason"

    override fun deleted() = "deleted"

    override fun saveFailed() = "saveFailed"

    override fun deleteFailed() = "deleteFailed"
}
