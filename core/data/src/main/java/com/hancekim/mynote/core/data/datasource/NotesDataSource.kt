package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.data.dto.CategoryDto

/**
 * 노트 원본(DTO)을 가져오는 데이터 소스 추상화.
 * 구현: 로컬 assets([LocalAssetNotesDataSource]) / 원격 GitHub raw(:core:network).
 */
interface NotesDataSource {
    suspend fun getCategories(): List<CategoryDto>
}
