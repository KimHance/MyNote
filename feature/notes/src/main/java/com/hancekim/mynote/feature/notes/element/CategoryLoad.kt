package com.hancekim.mynote.feature.notes.element

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.feature.notes.CategoryUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

@Immutable
data class CategoryLoad(
    val loaded: Boolean,
    val list: ImmutableList<CategoryUi>,
    val settling: Boolean = false,
) {
    companion object {
        val Initial = CategoryLoad(loaded = false, list = persistentListOf())
    }
}

private sealed interface CategoryEvent {
    data class Settling(
        val waiting: Boolean,
    ) : CategoryEvent

    data class Emitted(
        val list: ImmutableList<CategoryUi>,
    ) : CategoryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
fun settledCategories(
    connections: Flow<ConnectionState>,
    categories: () -> Flow<ImmutableList<CategoryUi>>,
): Flow<CategoryLoad> =
    connections
        .distinctUntilChanged()
        .flatMapLatest { state ->
            flow {
                emit(CategoryEvent.Settling(waiting = state is ConnectionState.Connected))
                emitAll(categories().map { CategoryEvent.Emitted(it) })
            }
        }.scan(CategoryLoad.Initial) { load, event ->
            when (event) {
                is CategoryEvent.Settling -> load.copy(settling = event.waiting)
                is CategoryEvent.Emitted -> CategoryLoad(loaded = true, list = event.list, settling = false)
            }
        }.distinctUntilChanged()
