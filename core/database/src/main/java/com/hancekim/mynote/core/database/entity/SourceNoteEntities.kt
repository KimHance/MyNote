package com.hancekim.mynote.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "sources")
internal data class SourceEntity(
    @PrimaryKey val id: String,
    val kind: String,
    @ColumnInfo(name = "config_json") val configJson: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

@Entity(
    tableName = "source_notes",
    indices = [Index(value = ["source_id", "doc_id"], unique = true)],
)
internal data class SourceNoteEntity(
    @PrimaryKey @ColumnInfo(name = "note_key") val key: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "doc_id") val docId: String,
    val path: String?,
    @ColumnInfo(name = "group_name") val group: String,
    val title: String,
    val date: String?,
    @ColumnInfo(name = "epoch_day") val epochDay: Long?,
    @ColumnInfo(name = "first_seen_at") val firstSeenAt: Long,
    val revision: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long?,
    @ColumnInfo(name = "parser_version") val parserVersion: Int,
    val raw: String?,
    @ColumnInfo(name = "blocks_json") val blocksJson: String,
    val excerpt: String?,
    val kind: String? = null,
    val summary: String? = null,
    @ColumnInfo(name = "aliases_json") val aliasesJson: String? = null,
    @ColumnInfo(name = "source_url") val sourceUrl: String? = null,
    @ColumnInfo(name = "tags_json") val tagsJson: String? = null,
    @ColumnInfo(name = "warnings_json") val warningsJson: String? = null,
)

internal data class SourceNoteListRow(
    @ColumnInfo(name = "note_key") val key: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val path: String?,
    @ColumnInfo(name = "group_name") val group: String,
    val title: String,
    val date: String?,
    @ColumnInfo(name = "blocks_json") val blocksJson: String,
)

internal data class SourceNoteStaleRow(
    @ColumnInfo(name = "note_key") val key: String,
    val path: String?,
    val raw: String,
)

internal data class SourceNoteDerivedRow(
    val key: String,
    val blocksJson: String,
    val excerpt: String?,
    val parserVersion: Int,
)

internal data class SourceNoteStateRow(
    @ColumnInfo(name = "doc_id") val docId: String,
    val revision: String?,
    @ColumnInfo(name = "has_raw") val hasRaw: Boolean,
    @ColumnInfo(name = "first_seen_at") val firstSeenAt: Long,
)
