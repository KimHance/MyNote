package com.hancekim.mynote.core.database.datasource

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hancekim.mynote.core.data.datasource.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.tokenDataStore by preferencesDataStore(name = "github_token")
private val TOKEN_KEY = stringPreferencesKey("github_token")

/** DataStore Preferences 기반 토큰 저장소(앱 전용 저장소, 평문). */
class DataStoreTokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TokenStore {

    override suspend fun currentToken(): String? =
        context.tokenDataStore.data.map { it[TOKEN_KEY] }.first()?.ifBlank { null }

    override suspend fun saveToken(token: String?) {
        context.tokenDataStore.edit { prefs ->
            val t = token?.ifBlank { null }
            if (t == null) prefs.remove(TOKEN_KEY) else prefs[TOKEN_KEY] = t
        }
    }

    override fun observeToken(): Flow<String?> =
        context.tokenDataStore.data.map { it[TOKEN_KEY]?.ifBlank { null } }
}
