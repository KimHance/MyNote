package com.hancekim.mynote.core.data.repository

import android.content.Context
import com.hancekim.mynote.core.data.model.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * `assets/notes.json` 을 읽어 emit 하는 더미 구현(네트워크 미연동 단계).
 *
 * 실제 GitHub 연동 시 이 클래스만 네트워크 구현으로 교체한다.
 */
class DummyNotesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotesRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getCategories(): Flow<List<Category>> = flow {
        val text = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        emit(json.decodeFromString<List<Category>>(text))
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val ASSET_FILE = "notes.json"
    }
}
