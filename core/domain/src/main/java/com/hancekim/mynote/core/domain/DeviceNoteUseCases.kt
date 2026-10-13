package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DraftCheck
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.check
import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import com.hancekim.mynote.core.domain.repository.DeviceNotesRepository
import javax.inject.Inject

/** 편집기의 `저장` 이 켜지는지(LN-3 ④). 글자를 칠 때마다 불러도 된다 — 본문 바이트를 경계까지만 센다. */
class CheckDeviceNoteDraftUseCase @Inject constructor(
    private val text: DeviceNoteText,
) {
    operator fun invoke(draft: DeviceNoteDraft): DraftCheck = draft.check(text)
}

/** 편집기 `저장` — [noteId] 가 없으면 새 노트(LN-3), 있으면 그 기기 노트를 고친다(LN-9). 저장한 노트 id 를 돌려준다. */
class SaveDeviceNoteUseCase @Inject constructor(
    private val repository: DeviceNotesRepository,
) {
    suspend operator fun invoke(
        noteId: String?,
        draft: DeviceNoteDraft,
        defaultGroup: String,
    ): Result<String> =
        if (noteId == null) {
            repository.create(draft, defaultGroup)
        } else {
            repository.update(noteId, draft, defaultGroup).map { noteId }
        }
}

/** 기기 노트 지우기(LN-11) — 노트와 그 북마크를 데이터 층에서 지운다. 리더 · 퀴즈 정리는 화면 쪽 몫이다. */
class DeleteDeviceNoteUseCase @Inject constructor(
    private val repository: DeviceNotesRepository,
) {
    suspend operator fun invoke(noteId: String): Result<Unit> = repository.delete(noteId)
}

/** 고치기 편집기를 채울 지금 값(LN-9). 기기 노트가 아니면 null. */
class LoadDeviceNoteUseCase @Inject constructor(
    private val repository: DeviceNotesRepository,
) {
    suspend operator fun invoke(noteId: String): DeviceNoteDraft? = repository.load(noteId)
}

/** 미리보기 블록(LN-3 ②) — 저장 뒤 리더와 같다. 메인 스레드 밖에서 해석한다. */
class PreviewDeviceNoteUseCase @Inject constructor(
    private val repository: DeviceNotesRepository,
) {
    suspend operator fun invoke(body: String): List<NoteBlock> = repository.preview(body)
}
