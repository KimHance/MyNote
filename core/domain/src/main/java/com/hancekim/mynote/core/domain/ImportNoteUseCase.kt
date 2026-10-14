package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.FileText
import com.hancekim.mynote.core.domain.model.ImportFileTypes
import com.hancekim.mynote.core.domain.model.ImportOutcome
import com.hancekim.mynote.core.domain.model.ImportRejection
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import com.hancekim.mynote.core.domain.repository.NoteFileReader
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 가져오기 세 길(LN-6) — 글 공유 · 붙여넣기는 [text], 파일 열기는 [file]. 셋 다 같은 규칙으로 미리 채운 편집기 값을 내거나
 * 거절 이유(LN-7)를 낸다. 노트는 만들지 않는다 — 저장은 편집기가 한다.
 */
class ImportNoteUseCase(
    private val rules: DeviceNoteText,
    private val files: NoteFileReader,
    private val today: () -> LocalDate,
) {
    @Inject
    constructor(rules: DeviceNoteText, files: NoteFileReader) : this(rules, files, { LocalDate.now() })

    /**
     * 글 공유 · 붙여넣기.
     *
     * @param subject 공유 제목(이름 후보). 붙여넣기는 null
     */
    fun text(
        text: String,
        subject: String? = null,
    ): ImportOutcome = ready(text, subject)

    /**
     * 파일 열기. 표시 이름 확장자 → 읽기(512KB + 1 바이트에서 멈춤 · 엄격한 UTF-8) → 빈 글 순으로 거른다.
     *
     * @param uri 시스템 파일 고르기가 준 주소. 고르지 않고 돌아왔으면 null
     */
    suspend fun file(uri: String?): ImportOutcome {
        if (uri == null) return ImportOutcome.Dismissed
        val read =
            try {
                val name = files.displayName(uri)
                if (!ImportFileTypes.accepts(name)) return ImportOutcome.Rejected(ImportRejection.UnsupportedType)
                checkNotNull(name) to files.read(uri, NoteLimits.Default.maxBytes)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                return ImportOutcome.Rejected(ImportRejection.Unreadable)
            }
        val (name, content) = read
        return when (content) {
            FileText.TooLarge -> ImportOutcome.Rejected(ImportRejection.TooLarge)
            FileText.NotUtf8 -> ImportOutcome.Rejected(ImportRejection.NotUtf8)
            is FileText.Read -> ready(content.text, ImportFileTypes.stem(name))
        }
    }

    private fun ready(
        text: String,
        nameHint: String?,
    ): ImportOutcome =
        when {
            !rules.rawFits(text) -> ImportOutcome.Rejected(ImportRejection.TooLarge)
            text.isBlank() -> ImportOutcome.Rejected(ImportRejection.Empty)
            else -> ImportOutcome.Ready(rules.imported(text, nameHint?.takeIf { it.isNotBlank() }, today()))
        }
}
