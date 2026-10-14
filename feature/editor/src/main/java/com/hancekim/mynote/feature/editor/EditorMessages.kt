package com.hancekim.mynote.feature.editor

import android.app.Application
import com.hancekim.mynote.core.domain.model.ImportRejection
import javax.inject.Inject

/** 편집기 ViewModel 이 쓰는 문구 — ViewModel 은 문구를 직접 읽지 않는다(FM-8). `:app` 의 `NotesModule` 이 묶는다. */
interface EditorMessages {
    /** 그룹 칸을 비웠을 때 저장하는 그룹 이름(LN-4 · 정하지 않은 것 20 — 문구 글자 그대로 저장). */
    fun defaultGroup(): String

    fun rejected(reason: ImportRejection): String

    fun deleted(): String

    fun saveFailed(): String

    fun deleteFailed(): String
}

class AndroidEditorMessages
    @Inject
    constructor(
        private val context: Application,
    ) : EditorMessages {
        override fun defaultGroup(): String = context.getString(R.string.editor_group_default)

        override fun rejected(reason: ImportRejection): String =
            context.getString(
                when (reason) {
                    ImportRejection.Empty -> R.string.editor_import_rejected_empty
                    ImportRejection.NotUtf8 -> R.string.editor_import_rejected_not_utf8
                    ImportRejection.TooLarge -> R.string.editor_import_rejected_too_large
                    ImportRejection.Unreadable -> R.string.editor_import_rejected_unreadable
                    ImportRejection.UnsupportedType -> R.string.editor_import_rejected_type
                },
            )

        override fun deleted(): String = context.getString(R.string.editor_deleted)

        override fun saveFailed(): String = context.getString(R.string.editor_save_failed)

        override fun deleteFailed(): String = context.getString(R.string.editor_delete_failed)
    }
