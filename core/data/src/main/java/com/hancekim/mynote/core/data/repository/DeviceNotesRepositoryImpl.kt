package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.common.result.runCatchingCancellable
import com.hancekim.mynote.core.data.datasource.BookmarkStore
import com.hancekim.mynote.core.data.datasource.DeviceNoteEdit
import com.hancekim.mynote.core.data.datasource.DeviceNoteInsert
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DeviceNoteFields
import com.hancekim.mynote.core.domain.model.InvalidDraftException
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteDates
import com.hancekim.mynote.core.domain.model.NoteKey
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.check
import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import com.hancekim.mynote.core.domain.repository.DeviceNotesRepository
import com.hancekim.mynote.core.format.NoteFormat
import com.hancekim.mynote.core.format.PARSER_VERSION
import com.hancekim.mynote.core.format.deviceNoteBody
import com.hancekim.mynote.core.format.deviceNotePath
import com.hancekim.mynote.core.format.deviceNoteRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * 기기 노트의 쓰기 길 — 저장 · 고침 · 지움이 `source_notes` 의 기기 소스 행만 건드린다. 동기화 · 연결 흐름과 섞이지 않는다(LN-12).
 * 원문은 빈 앞머리 + 본문, 경로는 `<docId>.md` 라 리더 · 재변환이 저장소 노트와 같은 해석을 탄다(LN-14).
 */
class DeviceNotesRepositoryImpl internal constructor(
    private val local: LocalNotesDataSource,
    private val bookmarks: BookmarkStore,
    private val format: NoteFormat,
    private val text: DeviceNoteText,
    private val newDocId: () -> String,
    private val now: () -> Long,
    private val today: () -> LocalDate,
) : DeviceNotesRepository {
    @Inject
    constructor(
        local: LocalNotesDataSource,
        bookmarks: BookmarkStore,
        format: NoteFormat,
        text: DeviceNoteText,
    ) : this(local, bookmarks, format, text, {
        UUID.randomUUID().toString()
    }, System::currentTimeMillis, { LocalDate.now() })

    override suspend fun create(
        draft: DeviceNoteDraft,
        defaultGroup: String,
    ): Result<String> =
        withContext(Dispatchers.Default) {
            runCatchingCancellable {
                requireSavable(draft)
                val docId = newDocId()
                require(docId.isNotBlank() && '/' !in docId) { "bad document id: $docId" }
                val path = deviceNotePath(docId)
                val raw = deviceNoteRaw(draft.body)
                val derived = format.derive(path, raw)
                val date = draft.date ?: today()
                local.insertDeviceNote(
                    DeviceNoteInsert(
                        docId = docId,
                        path = path,
                        group = DeviceNoteFields.group(draft.group, defaultGroup),
                        title = DeviceNoteFields.title(draft.title),
                        date = NoteDates.format(date),
                        epochDay = date.toEpochDay(),
                        firstSeenAt = now(),
                        parserVersion = PARSER_VERSION,
                        raw = raw,
                        blocksJson = derived.blocksJson,
                        excerpt = derived.excerpt,
                    ),
                )
                NoteKey(SourceId.Device, docId).value
            }
        }

    override suspend fun update(
        noteId: String,
        draft: DeviceNoteDraft,
        defaultGroup: String,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatchingCancellable {
                val key = deviceKey(noteId)
                requireSavable(draft)
                val raw = deviceNoteRaw(draft.body)
                val derived = format.derive(deviceNotePath(key.docId), raw)
                val edit =
                    DeviceNoteEdit(
                        key = key.value,
                        group = DeviceNoteFields.group(draft.group, defaultGroup),
                        title = DeviceNoteFields.title(draft.title),
                        raw = raw,
                        blocksJson = derived.blocksJson,
                        excerpt = derived.excerpt,
                        parserVersion = PARSER_VERSION,
                    )
                if (!local.updateDeviceNote(edit)) throw NoSuchElementException("device note not found: $noteId")
            }
        }

    override suspend fun delete(noteId: String): Result<Unit> =
        runCatchingCancellable {
            val key = deviceKey(noteId).value
            val deleted = local.deleteDeviceNote(key)
            bookmarks.remove(listOf(key))
            if (!deleted) throw NoSuchElementException("device note not found: $noteId")
        }

    override suspend fun load(noteId: String): DeviceNoteDraft? {
        val key = NoteKey.parse(noteId)?.takeIf { it.source == SourceId.Device } ?: return null
        val row = local.loadDeviceNote(key.value, NoteLimits.Default.maxCacheBytes) ?: return null
        return withContext(Dispatchers.Default) {
            DeviceNoteDraft(
                title = row.title,
                group = row.group,
                body = deviceNoteBody(row.raw),
                date = row.date?.let(NoteDates::parse),
            )
        }
    }

    override suspend fun preview(body: String): List<NoteBlock> =
        withContext(Dispatchers.Default) { format.parse(PREVIEW_PATH, deviceNoteRaw(body)) }

    private fun requireSavable(draft: DeviceNoteDraft) {
        val check = draft.check(text)
        if (!check.canSave) throw InvalidDraftException(check)
    }

    private fun deviceKey(noteId: String): NoteKey =
        requireNotNull(NoteKey.parse(noteId)?.takeIf { it.source == SourceId.Device }) { "not a device note: $noteId" }

    private companion object {
        val PREVIEW_PATH = deviceNotePath("preview")
    }
}
