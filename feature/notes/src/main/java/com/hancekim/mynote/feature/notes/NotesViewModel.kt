package com.hancekim.mynote.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.designsystem.theme.MyNoteFontScale
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import com.hancekim.mynote.core.domain.ObserveTokenUseCase
import com.hancekim.mynote.core.domain.SaveTokenUseCase
import com.hancekim.mynote.core.domain.SyncNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import javax.inject.Inject

/** ViewModel 내부의 동기화 진행/실패 상태. */
private data class SyncStatus(val loading: Boolean = true, val error: String? = null)

@HiltViewModel
class NotesViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
    private val syncNotes: SyncNotesUseCase,
    private val saveToken: SaveTokenUseCase,
    observeToken: ObserveTokenUseCase,
) : ViewModel() {

    private val activeNoteId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val expanded = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val fontScale = MutableStateFlow(MyNoteFontScale.Normal)
    private val syncStatus = MutableStateFlow(SyncStatus())
    private val hasToken = MutableStateFlow(false)

    private val categoriesFlow = getCategories().catch { emit(emptyList()) }

    private val contentState = combine(
        categoriesFlow,
        activeNoteId,
        query,
        expanded,
        fontScale,
    ) { categories, active, q, exp, scale ->
        buildState(categories, active, q, exp, scale)
    }

    val uiState: StateFlow<NotesUiState> = combine(contentState, syncStatus, hasToken) { state, sync, token ->
        state.copy(isLoading = sync.loading, errorMessage = sync.error, hasToken = token)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(),
    )

    init {
        // 토큰이 로드/변경될 때마다 재동기화(앱 시작, 런타임 입력 포함)
        viewModelScope.launch {
            observeToken().distinctUntilChanged().collect { token ->
                hasToken.value = !token.isNullOrBlank()
                sync()
            }
        }
    }

    /** 동기화 진입점. 토큰 관찰 흐름에서 자동 호출됨. */
    private fun sync() {
        viewModelScope.launch {
            syncStatus.update { it.copy(loading = true, error = null) }
            val result = syncNotes()
            syncStatus.update {
                it.copy(
                    loading = false,
                    error = if (result.isFailure) "노트를 불러오지 못했어요. 연결을 확인해 주세요." else null,
                )
            }
        }
    }

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

            is NotesIntent.SaveToken -> viewModelScope.launch { saveToken(intent.token) }
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
