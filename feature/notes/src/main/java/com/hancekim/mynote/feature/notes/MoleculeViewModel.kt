package com.hancekim.mynote.feature.notes

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Molecule 기반 MVI ViewModel 베이스(cashapp/molecule 샘플 패턴).
 *
 * 상태를 `@Composable` [present] 프레젠터로 **선언적으로** 만든다 — `combine`/`stateIn` 없이.
 * 이벤트는 [take] 로 받아 단방향으로 [present] 의 events Flow 에 전달한다(MVI).
 *
 * `models` 는 `by lazy` — 첫 collect(UI) 시점에 생성되어 서브클래스 생성자 주입이 끝난 뒤
 * 프레젠터가 실행되도록 한다(초기화 순서 안전).
 */
abstract class MoleculeViewModel<Event, Model> : ViewModel() {

    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 64)

    val models: StateFlow<Model> by lazy(LazyThreadSafetyMode.NONE) {
        viewModelScope.launchMolecule(RecompositionMode.Immediate) { present(events) }
    }

    /** UI → ViewModel 단방향 이벤트 전달. */
    protected fun take(event: Event) {
        events.tryEmit(event)
    }

    /** 이벤트를 받아 모델(UiState)을 선언적으로 만드는 프레젠터. */
    @Composable
    protected abstract fun present(events: Flow<Event>): Model
}
