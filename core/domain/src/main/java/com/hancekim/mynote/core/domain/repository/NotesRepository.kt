package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 노트 데이터 소스 추상화(읽기 전용). **인터페이스는 domain 이 소유**한다(DIP).
 * 구현체(`:core:data`)가 이 인터페이스를 구현하며 domain 을 의존한다.
 */
interface NotesRepository {
    fun getCategories(): Flow<List<Category>>
}
