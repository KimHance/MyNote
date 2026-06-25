package com.hancekim.mynote.feature.notes

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hancekim.mynote.core.designsystem.theme.MyNoteFontScale
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import com.hancekim.mynote.core.domain.ObserveAppliedTokenUseCase
import com.hancekim.mynote.core.domain.SaveTokenUseCase
import com.hancekim.mynote.core.domain.SyncNotesUseCase
import com.hancekim.mynote.core.domain.model.SyncOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

/**
 * 노트 화면 MVI ViewModel — 상태 생산을 **Molecule** 프레젠터([present])로 한다([MoleculeViewModel]).
 * MVI 모양(UiState + Intent + 단방향)은 그대로. 이벤트는 [onIntent] → [take] → events Flow.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val syncNotes: SyncNotesUseCase,
    private val saveToken: SaveTokenUseCase,
    private val observeAppliedToken: ObserveAppliedTokenUseCase,
) : MoleculeViewModel<NotesIntent, NotesUiState>() {

    /** 단일 인텐트 진입점(MVI). */
    fun onIntent(intent: NotesIntent) = take(intent)

    @Composable
    override fun present(events: Flow<NotesIntent>): NotesUiState {
        // 원격 SSOT(Room) 관찰 — 실패 시 빈 목록 폴백
        val categories by remember { getCategories().catch { emit(emptyList()) } }
            .collectAsState(initial = emptyList())
        // 네트워크 클라에 적용된 토큰 관찰(인터셉터가 읽는 값과 동일) — 레이스 없음
        val appliedToken by remember { observeAppliedToken() }.collectAsState(initial = null)

        var activeNoteId by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var fontScale by remember { mutableStateOf(MyNoteFontScale.Normal) }

        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var errorId by remember { mutableIntStateOf(0) }
        var infoMessage by remember { mutableStateOf<String?>(null) }
        var infoId by remember { mutableIntStateOf(0) }

        // 새로고침 트리거 + 안내 플래그(자동 sync 와 구분)
        var refreshTick by remember { mutableIntStateOf(0) }
        var announceNext by remember { mutableStateOf(false) }

        // 스낵바 호스트 상태도 프레젠터가 소유 — 에러/안내 발동까지 여기서(UI 는 SnackbarHost 만 렌더)
        val snackbarHostState = remember { SnackbarHostState() }

        // 토큰 변경(자동) 또는 새로고침 시 동기화. key 가 바뀌면 이전 실행을 취소(job-cancel 효과).
        LaunchedEffect(appliedToken, refreshTick) {
            val announce = announceNext
            announceNext = false
            loading = true
            error = null
            val result = syncNotes()
            result.onSuccess { outcome ->
                // 보던 노트가 삭제됐으면 홈으로
                if (activeNoteId != null && activeNoteId in outcome.deleted) activeNoteId = null
                // 수동 새로고침이고 변경이 있으면 안내(변경 없으면 무시)
                if (announce && outcome.hasChanges) {
                    infoMessage = buildChangeMessage(outcome)
                    infoId++
                }
            }
            if (result.isFailure) {
                error = "노트를 불러오지 못했어요. 연결을 확인해 주세요."
                errorId++
            }
            loading = false
        }

        // 이벤트 처리
        LaunchedEffect(Unit) {
            events.collect { intent ->
                when (intent) {
                    is NotesIntent.SelectNote -> activeNoteId = intent.id
                    is NotesIntent.QueryChanged -> query = intent.query
                    is NotesIntent.ToggleCategory ->
                        expanded = expanded + (intent.name to !(expanded[intent.name] ?: false))

                    NotesIntent.ToggleFontScale ->
                        fontScale = if (fontScale == MyNoteFontScale.Normal) MyNoteFontScale.Large else MyNoteFontScale.Normal

                    NotesIntent.CloseNote -> activeNoteId = null
                    is NotesIntent.SaveToken -> saveToken(intent.token)
                    NotesIntent.Refresh -> {
                        announceNext = true
                        refreshTick++
                    }
                }
            }
        }

        // 에러/안내 스낵바 발동 — id 가 바뀔 때마다(같은 메시지 재알림 포함). 메시지 없으면 무시.
        LaunchedEffect(errorId) { if (errorId > 0) error?.let { snackbarHostState.showSnackbar(it) } }
        LaunchedEffect(infoId) { if (infoId > 0) infoMessage?.let { snackbarHostState.showSnackbar(it) } }

        // 파생 상태 — combine 없이 평범한 계산(recomposition 이 갱신)
        val recentNotes = remember(categories) {
            categories
                .flatMap { c -> c.notes.map { RecentNote(it, c.name) } }
                .sortedByDescending { it.note.date }
                .toImmutableList()
        }
        val visibleCategories = remember(categories, query) {
            val list = if (query.isBlank()) {
                categories
            } else {
                categories.mapNotNull { c ->
                    val matched = c.notes.filter { note ->
                        note.title.contains(query, ignoreCase = true) ||
                            c.name.contains(query, ignoreCase = true)
                    }
                    if (matched.isEmpty()) null else c.copy(notes = matched)
                }
            }
            list.toImmutableList()
        }

        return NotesUiState(
            categories = remember(categories) { categories.toImmutableList() },
            visibleCategories = visibleCategories,
            recentNotes = recentNotes,
            activeNoteId = activeNoteId,
            query = query,
            expanded = remember(expanded) { expanded.toImmutableMap() },
            fontScale = fontScale,
            isLoading = loading,
            errorMessage = error,
            errorId = errorId,
            hasToken = !appliedToken.isNullOrBlank(),
            infoMessage = infoMessage,
            infoId = infoId,
            snackbarHostState = snackbarHostState,
        )
    }

    private fun buildChangeMessage(outcome: SyncOutcome): String {
        val parts = buildList {
            if (outcome.changed > 0) add("갱신 ${outcome.changed}개")
            if (outcome.deleted.isNotEmpty()) add("삭제 ${outcome.deleted.size}개")
        }
        return "노트 변경 반영 — " + parts.joinToString(" · ")
    }
}
