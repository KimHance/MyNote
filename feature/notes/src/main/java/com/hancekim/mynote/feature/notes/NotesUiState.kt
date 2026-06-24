package com.hancekim.mynote.feature.notes

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.designsystem.theme.MyNoteFontScale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/** 홈 "최근 노트" 카드 한 장에 필요한 정보(노트 + 소속 카테고리명). */
@Immutable
data class RecentNote(
    val note: Note,
    val categoryName: String,
)

/**
 * 노트 화면 전체가 공유하는 단일 상태.
 *
 * `@Stable`(not `@Immutable`): 컬렉션은 ImmutableList/Map 이지만, 품고 있는 [Category] 가
 * `notes: List<Note>`(불변 보장이 아닌 읽기 전용 인터페이스)를 가져 deeply immutable 이 아니다.
 * 따라서 "절대 안 바뀜"을 못 박지 않고, 우리가 통제해서 안 바꾸는 stable 로 둔다.
 *
 * @param visibleCategories 검색 필터가 적용된 카테고리 목록(빈 카테고리는 제외)
 * @param expanded 카테고리별 펼침 상태(검색 중에는 화면에서 전체 펼침으로 간주)
 */
@Stable
data class NotesUiState(
    val categories: ImmutableList<Category> = persistentListOf(),
    val visibleCategories: ImmutableList<Category> = persistentListOf(),
    val recentNotes: ImmutableList<RecentNote> = persistentListOf(),
    val activeNoteId: String? = null,
    val query: String = "",
    val expanded: ImmutableMap<String, Boolean> = persistentMapOf(),
    val fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val totalCount: Int get() = categories.sumOf { it.notes.size }

    /** 검색 중이면 모든 카테고리를 펼친 것으로 본다. */
    fun isExpanded(categoryName: String): Boolean =
        query.isNotBlank() || expanded[categoryName] == true

    fun noteById(id: String?): Note? {
        if (id == null) return null
        return categories.firstNotNullOfOrNull { c -> c.notes.firstOrNull { it.id == id } }
    }

    fun categoryNameOf(id: String?): String? {
        if (id == null) return null
        return categories.firstOrNull { c -> c.notes.any { it.id == id } }?.name
    }

    val activeNote: Note? get() = noteById(activeNoteId)
}
