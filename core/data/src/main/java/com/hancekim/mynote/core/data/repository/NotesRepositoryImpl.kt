package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.NotesDataSource
import com.hancekim.mynote.core.data.dto.toDomain
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * [NotesDataSource] 에서 DTO 를 받아 도메인 모델로 매핑한다(DIP 구현체).
 * 바인딩되는 DataSource 만 바꾸면 로컬 ↔ 원격(GitHub raw) 이 교체된다.
 */
class NotesRepositoryImpl @Inject constructor(
    private val dataSource: NotesDataSource,
) : NotesRepository {

    override fun getCategories(): Flow<List<Category>> = flow {
        emit(dataSource.getCategories().map { it.toDomain() })
    }
}
