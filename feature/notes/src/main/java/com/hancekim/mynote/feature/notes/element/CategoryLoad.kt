package com.hancekim.mynote.feature.notes.element

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.connectBandNoteCount
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.mapper.toUi
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
    val connectBandCount: Int = 0,
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
        val connectBandCount: Int,
    ) : CategoryEvent
}

/**
 * 연결이 바뀔 때마다 노트 목록을 새로 받고, 받는 동안은 기다림(RC-21)을 낸다. 받은 목록마다 연결 안내 띠가 셀 노트 수
 * (연결 정보 없는 저장소 노트 — RC-1)를 같은 연결 기준으로 같이 싣는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun settledCategories(
    connections: Flow<ConnectionState>,
    categories: () -> Flow<List<Category>>,
): Flow<CategoryLoad> =
    connections
        .distinctUntilChanged()
        .flatMapLatest { state ->
            flow {
                emit(CategoryEvent.Settling(waiting = state is ConnectionState.Connected))
                emitAll(categories().map { CategoryEvent.Emitted(it.toUi(), it.connectBandNoteCount(state)) })
            }
        }.scan(CategoryLoad.Initial) { load, event ->
            when (event) {
                is CategoryEvent.Settling -> {
                    load.copy(settling = event.waiting)
                }

                is CategoryEvent.Emitted -> {
                    CategoryLoad(
                        loaded = true,
                        list = event.list,
                        settling = false,
                        connectBandCount = event.connectBandCount,
                    )
                }
            }
        }.distinctUntilChanged()
