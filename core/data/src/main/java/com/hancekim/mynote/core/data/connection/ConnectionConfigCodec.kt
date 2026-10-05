package com.hancekim.mynote.core.data.connection

import com.hancekim.mynote.core.domain.model.RepoConnection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class DecodedConfig(
    val connection: RepoConnection,
    val pending: Boolean,
)

object ConnectionConfigCodec {
    const val PENDING_MARKER = "\"pending\":true"

    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun encode(
        connection: RepoConnection,
        pending: Boolean = false,
    ): String =
        json.encodeToString(
            ConfigDto.serializer(),
            ConfigDto(
                owner = connection.owner,
                repo = connection.repo,
                branch = connection.branch,
                indexPath = connection.indexPath,
                isPublic = connection.isPublic,
                pending = pending,
            ),
        )

    fun decode(configJson: String?): DecodedConfig? {
        if (configJson.isNullOrBlank()) return null
        val dto = runCatching { json.decodeFromString(ConfigDto.serializer(), configJson) }.getOrNull() ?: return null
        if (dto.owner.isBlank() || dto.repo.isBlank() || dto.indexPath.isBlank()) return null
        return DecodedConfig(
            connection = RepoConnection(dto.owner, dto.repo, dto.branch, dto.indexPath, dto.isPublic),
            pending = dto.pending,
        )
    }
}

@Serializable
private data class ConfigDto(
    val owner: String,
    val repo: String,
    val branch: String,
    val indexPath: String,
    val isPublic: Boolean,
    val pending: Boolean = false,
)
