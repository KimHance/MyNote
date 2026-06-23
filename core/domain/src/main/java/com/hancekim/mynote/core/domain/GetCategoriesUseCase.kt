package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.data.model.Category
import com.hancekim.mynote.core.data.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 카테고리/노트 목록을 가져온다(읽기 전용).
 *
 * 지금은 Repository 위임뿐이지만, 이후 정렬·필터·병합 같은 도메인 규칙이 생기면 여기에 둔다.
 * feature 모듈은 `:core:data` 를 직접 모르고 이 UseCase 만 의존한다.
 */
class GetCategoriesUseCase @Inject constructor(
    private val repository: NotesRepository,
) {
    operator fun invoke(): Flow<List<Category>> = repository.getCategories()
}
