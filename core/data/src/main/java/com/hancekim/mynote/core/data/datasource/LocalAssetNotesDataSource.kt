package com.hancekim.mynote.core.data.datasource

import android.content.Context
import com.hancekim.mynote.core.data.dto.CategoryDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** `assets/notes.json` 더미 소스(오프라인/개발용). */
class LocalAssetNotesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotesDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getCategories(): List<CategoryDto> = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        json.decodeFromString<List<CategoryDto>>(text)
    }

    private companion object {
        const val ASSET_FILE = "notes.json"
    }
}
