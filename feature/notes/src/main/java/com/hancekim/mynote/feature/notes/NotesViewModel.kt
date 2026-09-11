package com.hancekim.mynote.feature.notes

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.designsystem.theme.MyNoteFontScale
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import com.hancekim.mynote.core.domain.ObserveAppliedTokenUseCase
import com.hancekim.mynote.core.domain.SaveTokenUseCase
import com.hancekim.mynote.core.domain.SyncNotesUseCase
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class SyncState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorId: Int = 0,
    val infoMessage: String? = null,
    val infoId: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
    private val syncNotes: SyncNotesUseCase,
    private val saveToken: SaveTokenUseCase,
    observeAppliedToken: ObserveAppliedTokenUseCase,
) : MviViewModel<NotesUiState, NotesIntent>() {
    private val activeNoteId = trigger<String?>(null)
    private val query = trigger("")
    private val expanded = trigger(persistentMapOf<String, Boolean>())
    private val fontScale = trigger(MyNoteFontScale.Normal)
    private val refreshTicks = MutableStateFlow(0)

    private val appliedToken =
        observeAppliedToken()
            .triggerIn(null, started = SharingStarted.Lazily)

    private val categories: Flow<ImmutableList<CategoryUi>> =
        getCategories()
            .catch { emit(emptyList()) }
            .map { it.toUi() }

    private val categoryList =
        categories
            .triggerIn(persistentListOf(), started = SharingStarted.Lazily)

    private val recentNotes =
        categoryList
            .map { list ->
                list
                    .flatMap { c -> c.notes.map { RecentNoteUi(it, c.name) } }
                    .sortedByDescending { it.note.date }
                    .toImmutableList()
            }.triggerIn(persistentListOf())

    private val visibleCategories =
        combine(categoryList, query) { list, keyword ->
            if (keyword.isBlank()) {
                list
            } else {
                list
                    .mapNotNull { c ->
                        val matched =
                            c.notes.filter { note ->
                                note.title.contains(keyword, ignoreCase = true) ||
                                    c.name.contains(keyword, ignoreCase = true)
                            }
                        if (matched.isEmpty()) null else c.copy(notes = matched.toImmutableList())
                    }.toImmutableList()
            }
        }.triggerIn(persistentListOf())

    private var errorSeq = 0
    private var infoSeq = 0

    private val sync =
        combine(appliedToken, refreshTicks) { _, tick -> tick }
            .flatMapLatest { tick -> syncFlow(announce = tick > 0) }
            .triggerIn(SyncState())

    private val snackbarHostState = SnackbarHostState()

    override fun build() =
        NotesUiState(
            categories = categoryList.value,
            visibleCategories = visibleCategories.value,
            recentNotes = recentNotes.value,
            activeNoteId = activeNoteId.value,
            query = query.value,
            expanded = expanded.value,
            fontScale = fontScale.value,
            isLoading = sync.value.isLoading,
            errorMessage = sync.value.errorMessage,
            errorId = sync.value.errorId,
            hasToken = !appliedToken.value.isNullOrBlank(),
            infoMessage = sync.value.infoMessage,
            infoId = sync.value.infoId,
            snackbarHostState = snackbarHostState,
        )

    override fun onIntent(event: NotesIntent) {
        when (event) {
            is NotesIntent.SelectNote -> {
                activeNoteId.value = event.id
            }

            is NotesIntent.QueryChanged -> {
                query.value = event.query
            }

            is NotesIntent.ToggleCategory -> {
                expanded.update { it.putting(event.name, !(it[event.name] ?: false)) }
            }

            NotesIntent.ToggleFontScale -> {
                fontScale.update {
                    if (it == MyNoteFontScale.Normal) MyNoteFontScale.Large else MyNoteFontScale.Normal
                }
            }

            NotesIntent.CloseNote -> {
                activeNoteId.value = null
            }

            is NotesIntent.SaveToken -> {
                viewModelScope.launch { saveToken(event.token) }
            }

            NotesIntent.Refresh -> {
                refreshTicks.update { it + 1 }
            }
        }
    }

    private fun syncFlow(announce: Boolean): Flow<SyncState> =
        flow {
            emit(SyncState(isLoading = true))
            emit(
                syncNotes().fold(
                    onSuccess = { outcome ->
                        activeNoteId.update { id -> if (id != null && id in outcome.deleted) null else id }
                        if (announce && outcome.hasChanges) {
                            SyncState(
                                isLoading = false,
                                infoMessage = buildChangeMessage(outcome),
                                infoId = ++infoSeq,
                            )
                        } else {
                            SyncState(isLoading = false)
                        }
                    },
                    onFailure = {
                        SyncState(
                            isLoading = false,
                            errorMessage = "노트를 불러오지 못했어요. 연결을 확인해 주세요.",
                            errorId = ++errorSeq,
                        )
                    },
                ),
            )
        }

    private fun buildChangeMessage(outcome: SyncOutcome): String {
        val parts =
            buildList {
                if (outcome.changed > 0) add("갱신 ${outcome.changed}개")
                if (outcome.deleted.isNotEmpty()) add("삭제 ${outcome.deleted.size}개")
            }
        return "노트 변경 반영 — " + parts.joinToString(" · ")
    }
}
