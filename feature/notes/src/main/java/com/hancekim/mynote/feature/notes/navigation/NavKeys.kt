package com.hancekim.mynote.feature.notes.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 목록(=상세 미선택 시 Home placeholder). Nav3 백스택의 루트 키. */
@Serializable
data object NotesListKey : NavKey

/** 노트 상세. */
@Serializable
data class NoteDetailKey(val id: String) : NavKey
