package com.hancekim.mynote.feature.notes

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.designsystem.theme.MyNoteFontScale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * `@Stable`(not `@Immutable`): 나머지 필드는 전부 불변이지만 [snackbarHostState] 가 바뀔 수 있다.
 */
@Stable
data class NotesUiState(
    val categories: ImmutableList<CategoryUi> = persistentListOf(),
    val visibleCategories: ImmutableList<CategoryUi> = persistentListOf(),
    val recentNotes: ImmutableList<RecentNoteUi> = persistentListOf(),
    val activeNoteId: String? = null,
    val query: String = "",
    val expanded: ImmutableMap<String, Boolean> = persistentMapOf(),
    val fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorId: Int = 0,
    val hasToken: Boolean = false,
    // 새로고침 변경 안내(갱신/삭제). infoId 가 바뀔 때마다 스낵바 발동.
    val infoMessage: String? = null,
    val infoId: Int = 0,
    // 스낵바 호스트 인스턴스는 ViewModel 이 소유한다(재조립돼도 유지). 발동은 화면에서 id 를 보고 한다.
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : MviState {
    val totalCount: Int get() = categories.sumOf { it.notes.size }

    /** 검색 중이면 모든 카테고리를 펼친 것으로 본다. */
    fun isExpanded(categoryName: String): Boolean =
        query.isNotBlank() || expanded[categoryName] == true

    fun noteById(id: String?): NoteUi? {
        if (id == null) return null
        return categories.firstNotNullOfOrNull { c -> c.notes.firstOrNull { it.id == id } }
    }

    fun categoryNameOf(id: String?): String? {
        if (id == null) return null
        return categories.firstOrNull { c -> c.notes.any { it.id == id } }?.name
    }

    val activeNote: NoteUi? get() = noteById(activeNoteId)
}
