package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.block.NoteColumnAlign
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentCell
import com.hancekim.mynote.core.designsystem.block.NoteContentListItem
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import com.hancekim.mynote.core.designsystem.block.UnsupportedKind
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.feature.notes.NoteUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private fun plain(text: String) = NoteContentSpan(text, NoteSpanStyle.Normal)

private fun italic(text: String) = NoteContentSpan(text, NoteSpanStyle.Normal, italic = true)

private fun struck(text: String) = NoteContentSpan(text, NoteSpanStyle.Normal, strike = true)

private fun bold(
    text: String,
    italic: Boolean = false,
    strike: Boolean = false,
) = NoteContentSpan(text, NoteSpanStyle.Bold, italic, strike)

private fun code(
    text: String,
    strike: Boolean = false,
) = NoteContentSpan(text, NoteSpanStyle.Code, strike = strike)

private fun spans(vararg spans: NoteContentSpan): ImmutableList<NoteContentSpan> = spans.toList().toImmutableList()

private fun paragraph(vararg spans: NoteContentSpan) = NoteContentBlock.Paragraph(spans(*spans))

private fun item(
    vararg spans: NoteContentSpan,
    checked: Boolean? = null,
    children: List<NoteContentBlock> = emptyList(),
) = NoteContentListItem(spans(*spans), checked, children.toImmutableList())

private fun bullets(vararg items: NoteContentListItem) = NoteContentBlock.BulletList(items.toList().toImmutableList())

private fun numbers(
    start: Int,
    vararg items: NoteContentListItem,
) = NoteContentBlock.OrderedList(items.toList().toImmutableList(), start)

private fun cells(vararg cells: List<NoteContentSpan>): ImmutableList<NoteContentCell> =
    cells.map { NoteContentCell(it.toImmutableList()) }.toImmutableList()

private fun table(
    header: ImmutableList<NoteContentCell>,
    align: List<NoteColumnAlign>,
    vararg rows: ImmutableList<NoteContentCell>,
) = NoteContentBlock.Table(header, rows.toList().toImmutableList(), align.toImmutableList())

private fun note(
    title: String,
    vararg blocks: NoteContentBlock,
    readable: Boolean = true,
) = NoteUi(
    id = title,
    title = title,
    date = "2026.10.07",
    blocks = blocks.toList().toImmutableList(),
    readable = readable,
)

private val textShapes =
    note(
        "Kotlin 스타일 메모",
        NoteContentBlock.Heading(2, spans(italic("선택"), plain(" 사항과 기본값"))),
        paragraph(
            plain("코루틴은 "),
            italic("가벼운 스레드"),
            plain("처럼 보이지만 "),
            struck("스레드 하나를 통째로 차지한다"),
            plain(" 실제로는 스레드를 빌려 쓴다."),
        ),
        paragraph(
            bold("굵게와 기울임", italic = true),
            plain(" · "),
            bold("굵게 안의 취소선", strike = true),
            plain(" · "),
            NoteContentSpan("기울임 안의 취소선", NoteSpanStyle.Normal, italic = true, strike = true),
            plain(" · "),
            bold("셋 다", italic = true, strike = true),
        ),
        paragraph(
            struck("UI 스레드에서 "),
            code("runBlocking", strike = true),
            struck(" 으로 기다린다"),
            plain(" 대신 "),
            code("withContext(Dispatchers.IO)"),
            plain(" 를 쓴다."),
        ),
        NoteContentBlock.Quote(
            spans(
                plain("상태는 "),
                italic("단방향"),
                plain("으로 흐른다. "),
                bold("이벤트는 위로"),
                plain(", "),
                struck("상태는 아래로"),
                plain(" 값은 아래로."),
            ),
        ),
        bullets(
            item(italic("선택"), plain(" 인자는 뒤에 둔다")),
            item(code("!!", strike = true), struck(" 로 널을 무시한다")),
        ),
    )

private val nested =
    note(
        "코루틴 빌더 정리",
        bullets(
            item(
                plain("코루틴 빌더"),
                children =
                    listOf(
                        bullets(
                            item(
                                code("launch"),
                                plain(" — 결과 없이 일을 시작한다"),
                                children =
                                    listOf(
                                        bullets(
                                            item(plain("돌려주는 값: "), code("Job")),
                                            item(
                                                plain("취소: "),
                                                code("job.cancel()"),
                                                children =
                                                    listOf(
                                                        bullets(
                                                            item(
                                                                plain("4단 — 3단 기호 자리에 선다"),
                                                                children =
                                                                    listOf(
                                                                        paragraph(plain("멈춘 단의 문단은 그 항목 글 시작선에 선다.")),
                                                                        bullets(item(plain("5단도 같은 자리"))),
                                                                    ),
                                                            ),
                                                        ),
                                                    ),
                                            ),
                                        ),
                                    ),
                            ),
                            item(
                                code("async"),
                                plain(" — 결과를 "),
                                code("Deferred"),
                                plain(" 로 돌려준다"),
                                children =
                                    listOf(
                                        numbers(
                                            1,
                                            item(code("await()"), plain(" 로 기다린다")),
                                            item(plain("예외는 "), code("await()"), plain(" 에서 던져진다")),
                                        ),
                                    ),
                            ),
                        ),
                        paragraph(plain("구조화된 동시성 덕에 부모가 취소되면 자식도 취소된다.")),
                        NoteContentBlock.Code("val user = async { api.user() }\nprintln(user.await())"),
                        NoteContentBlock.Quote(spans(plain("빌더는 CoroutineScope 의 확장 함수다."))),
                    ),
            ),
            item(
                plain("디스패처"),
                children =
                    listOf(
                        table(
                            cells(listOf(plain("디스패처")), listOf(plain("스레드"))),
                            listOf(NoteColumnAlign.None, NoteColumnAlign.None),
                            cells(listOf(code("Main")), listOf(plain("UI 하나"))),
                            cells(listOf(code("IO")), listOf(plain("필요한 만큼"))),
                        ),
                    ),
            ),
            item(children = listOf(bullets(item(plain("첫 문단 없는 항목은 하위 목록이 기호 옆에서 시작한다"))))),
            item(),
        ),
    )

private val checks =
    note(
        "Room 마이그레이션 체크리스트",
        bullets(
            item(plain("스키마 내보내기 켜기"), checked = true),
            item(code("MigrationTestHelper"), plain(" 로 시험 쓰기"), checked = true),
            item(plain("자동 마이그레이션으로 옮기기 — 이름이 바뀐 열은 "), code("@RenameColumn"), plain(" 으로 따로 적는다"), checked = false),
            item(plain("스키마 JSON 은 커밋한다")),
        ),
        paragraph(plain("버전을 올릴 때는 아래 순서를 지킨다.")),
        numbers(8, item(plain("인덱스 추가")), item(plain("외래 키 확인")), item(plain("오래된 열 지우기")), item(plain("버전 올리기"))),
        paragraph(plain("배포 전에 한 번 더 본다.")),
        numbers(1, item(plain("빌드 통과"), checked = true), item(plain("기기에서 열기"), checked = false)),
    )

private val alerts =
    note(
        "코루틴 빌더 정리",
        NoteContentBlock.Quote(spans(plain("[!NOTE]\n구조화된 동시성은 부모 범위가 자식이 끝날 때까지 기다리게 한다."))),
        NoteContentBlock.Quote(spans(plain("[!WARNING]\n"), code("GlobalScope"), plain(" 는 생명주기를 따르지 않는다."))),
    )

private val limitExceeded =
    note(
        "깊게 겹친 목록",
        NoteContentBlock.Unsupported(
            kind =
                UnsupportedKind.LimitExceeded(
                    NoteLimits.Default.maxDepth,
                    NoteLimits.Default.maxKilobytes,
                    NoteLimits.Default.maxTableCells,
                ),
            salvaged = null,
            raw = (0 until 40).joinToString("\n") { "  ".repeat(it) + "- 단 ${it + 1}" },
        ),
        readable = false,
    )

private val collections =
    table(
        cells(listOf(plain("컬렉션")), listOf(plain("순서")), listOf(plain("평균 조회"))),
        listOf(NoteColumnAlign.Left, NoteColumnAlign.Center, NoteColumnAlign.Right),
        cells(listOf(code("ArrayList")), listOf(plain("있음")), listOf(plain("O(1)"))),
        cells(listOf(code("HashSet")), listOf(plain("없음")), listOf(plain("O(1)"))),
        cells(listOf(code("TreeMap")), listOf(plain("키 순서")), listOf(plain("O(log n)"))),
    )

private val lifetimes =
    table(
        cells(listOf(plain("범위")), listOf(plain("값이 사라지는 때"))),
        listOf(NoteColumnAlign.None, NoteColumnAlign.None),
        cells(listOf(code("remember")), listOf(plain("리컴포지션에서 빠질 때 · 화면 회전"))),
        cells(listOf(code("rememberSaveable")), listOf(plain("화면을 닫을 때 — 회전 · 프로세스 종료 뒤에는 "), bold("복원"), plain("한다"))),
        cells(listOf(code("ViewModel")), listOf(plain("화면 기록에서 빠질 때"))),
    )

private val streams =
    table(
        cells(
            listOf(plain("기능")),
            listOf(plain("LiveData")),
            listOf(plain("StateFlow")),
            listOf(plain("SharedFlow")),
            listOf(plain("Channel")),
        ),
        emptyList(),
        cells(listOf(plain("초기값")), listOf(plain("없음")), listOf(plain("필수")), listOf(plain("없음")), listOf(plain("없음"))),
        cells(
            listOf(plain("같은 값")),
            listOf(plain("보냄")),
            listOf(plain("거름")),
            listOf(plain("보냄")),
            listOf(plain("보냄")),
        ),
        cells(
            listOf(plain("구독자 여럿")),
            listOf(plain("됨")),
            listOf(plain("됨")),
            listOf(plain("됨")),
            listOf(plain("하나만")),
        ),
        cells(
            listOf(plain("생명주기")),
            listOf(plain("자동")),
            listOf(plain("직접")),
            listOf(plain("직접")),
            listOf(plain("직접")),
        ),
    )

private val narrowTables =
    note(
        "컬렉션과 상태 비교",
        paragraph(plain("자주 쓰는 컬렉션을 순서와 조회 비용으로 나눠 본다.")),
        collections,
        paragraph(plain("Compose 에서 상태를 어디에 두느냐에 따라 값이 사라지는 때가 다르다.")),
        lifetimes,
    )

private val wideTable =
    note(
        "컬렉션과 상태 비교",
        paragraph(plain("관찰할 수 있는 값을 내보내는 타입을 기능별로 비교한다.")),
        streams,
        paragraph(plain("화면 상태는 보통 "), code("StateFlow"), plain(", 한 번만 처리할 이벤트는 "), code("Channel"), plain(" 로 보낸다.")),
    )

private val foldTables =
    note(
        "컬렉션과 상태 비교",
        lifetimes,
        streams,
        bullets(item(plain("목록 항목 아래 표"), children = listOf(collections))),
    )

internal data class NoteFormatSample(
    val name: String,
    val note: NoteUi,
    val windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
    val fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
)

private val samples =
    listOf(
        NoteFormatSample("글자 모양-compact", textShapes),
        NoteFormatSample("글자 모양-큰 글자", textShapes, fontScale = MyNoteFontScale.Large),
        NoteFormatSample("하위 블록-compact", nested),
        NoteFormatSample("하위 블록-852", nested, windowClass = MyNoteWindowClass.Expanded),
        NoteFormatSample("하위 블록-큰 글자", nested, fontScale = MyNoteFontScale.Large),
        NoteFormatSample("체크 시작 번호-compact", checks),
        NoteFormatSample("알림-compact", alerts),
        NoteFormatSample("한도 넘음-compact", limitExceeded),
        NoteFormatSample("표 좁은-compact", narrowTables),
        NoteFormatSample("표 좁은-큰 글자", narrowTables, fontScale = MyNoteFontScale.Large),
        NoteFormatSample("표 넓은-compact", wideTable),
        NoteFormatSample("표-852", foldTables, windowClass = MyNoteWindowClass.Expanded),
        NoteFormatSample("체크 시작 번호-852", checks, windowClass = MyNoteWindowClass.Expanded),
        NoteFormatSample("체크 시작 번호-큰 글자", checks, fontScale = MyNoteFontScale.Large),
    )

private class NoteFormatSampleProvider : PreviewParameterProvider<NoteFormatSample> {
    override val values = samples.asSequence()

    override fun getDisplayName(index: Int): String? = samples.getOrNull(index)?.name
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun NoteFormatSamplePreview(
    @PreviewParameter(NoteFormatSampleProvider::class) sample: NoteFormatSample,
) {
    MyNoteTheme(windowClass = sample.windowClass, fontScale = sample.fontScale) {
        NoteReaderScreen(
            note = sample.note,
            categoryName = "Kotlin",
            contentTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            onQuizAction = {},
        )
    }
}
