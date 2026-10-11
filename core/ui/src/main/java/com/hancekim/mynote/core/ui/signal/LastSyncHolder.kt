package com.hancekim.mynote.core.ui.signal

import com.hancekim.mynote.core.ui.element.LastSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 마지막 동기화 결과(결정 8). 동기화를 구독하는 노트 ViewModel 이 쓰고 설정 · 목록 패널은 읽기만 한다 —
 * 동기화 흐름은 구독마다 다시 돌아서(SY-1) 읽는 쪽이 따로 구독하면 안 된다.
 */
@Singleton
class LastSyncHolder @Inject constructor() {
    private val last = MutableStateFlow<LastSync>(LastSync.None)
    val value: StateFlow<LastSync> = last.asStateFlow()

    fun set(next: LastSync) {
        last.value = next
    }
}
