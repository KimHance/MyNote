package com.hancekim.mynote.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.data.model.Category
import com.hancekim.mynote.core.designsystem.theme.MyNoteFontScale
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
) : ViewModel() {

    private val activeNoteId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val expanded = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val fontScale = MutableStateFlow(MyNoteFontScale.Normal)

    private val categoriesFlow = getCategories().catch { emit(emptyList()) }

    val uiState: StateFlow<NotesUiState> = combine(
        categoriesFlow,
        activeNoteId,
        query,
        expanded,
        fontScale,
    ) { categories, active, q, exp, scale ->
        buildState(categories, active, q, exp, scale)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(),
    )

    /** 단일 인텐트 진입점(MVI). */
    fun onIntent(intent: NotesIntent) {
        when (intent) {
            is NotesIntent.SelectNote -> activeNoteId.value = intent.id
            is NotesIntent.QueryChanged -> query.value = intent.query
            is NotesIntent.ToggleCategory -> expanded.update { current ->
                current + (intent.name to !(current[intent.name] ?: false))
            }

            NotesIntent.ToggleFontScale -> fontScale.update {
                if (it == MyNoteFontScale.Normal) MyNoteFontScale.Large else MyNoteFontScale.Normal
            }

            NotesIntent.CloseNote -> activeNoteId.value = null
        }
    }

    private fun buildState(
        categories: List<Category>,
        active: String?,
        q: String,
        exp: Map<String, Boolean>,
        scale: MyNoteFontScale,
    ): NotesUiState {
        val recent = categories
            .flatMap { c -> c.notes.map { RecentNote(it, c.name) } }
            .sortedByDescending { it.note.date }

        val visible = if (q.isBlank()) {
            categories
        } else {
            categories.mapNotNull { c ->
                val matched = c.notes.filter { note ->
                    note.title.contains(q, ignoreCase = true) ||
                        c.name.contains(q, ignoreCase = true)
                }
                if (matched.isEmpty()) null else c.copy(notes = matched)
            }
        }

        return NotesUiState(
            categories = categories.toImmutableList(),
            visibleCategories = visible.toImmutableList(),
            recentNotes = recent.toImmutableList(),
            activeNoteId = active,
            query = q,
            expanded = exp.toImmutableMap(),
            fontScale = scale,
            isLoading = false,
        )
    }
}
