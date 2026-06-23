package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 노트 데이터 소스 추상화(읽기 전용).
 *
 * PoC 는 로컬 assets 더미를 사용한다. 이후 단계에서 GitHub raw JSON 을 가져오는
 * 구현으로 교체해도 소비자(ViewModel)는 바뀌지 않는다.
 */
interface NotesRepository {
    fun getCategories(): Flow<List<Category>>
}
