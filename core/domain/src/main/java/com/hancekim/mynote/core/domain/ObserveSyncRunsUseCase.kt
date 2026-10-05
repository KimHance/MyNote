package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.SyncRun
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

class ObserveSyncRunsUseCase @Inject constructor(
    private val syncNotes: SyncNotesUseCase,
    private val observeAppliedToken: ObserveAppliedTokenUseCase,
    private val connections: ConnectionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(refreshes: Flow<Int>): Flow<SyncRun> =
        flow {
            var handled: Pair<String?, Int>? = null
            emitAll(
                combine(observeAppliedToken(), refreshes, connections.observeAttempting()) { token, tick, attempting ->
                    Triple(token, tick, attempting)
                }.transform { (token, tick, attempting) ->
                    val key = token to tick
                    when {
                        attempting -> {
                            handled = key
                            emit(null)
                        }

                        key != handled -> {
                            handled = key
                            emit(tick)
                        }
                    }
                },
            )
        }.flatMapLatest { tick ->
            if (tick == null) {
                flowOf<SyncRun>(SyncRun.Idle)
            } else {
                val announce = tick > 0
                flow<SyncRun> {
                    emit(SyncRun.Running(announce))
                    emit(SyncRun.Finished(announce, syncNotes()))
                }
            }
        }
}
