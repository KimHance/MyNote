package com.hancekim.mynote.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "weekly_summaries")
internal data class WeeklySummaryEntity(
    @PrimaryKey @ColumnInfo(name = "week_start") val weekStart: Long,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    val headline: String,
    val detail: String,
    val focus: String?,
    @ColumnInfo(name = "model_commit") val modelCommit: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
)

@Entity(tableName = "quiz_patterns")
internal data class QuizPatternEntity(
    @PrimaryKey @ColumnInfo(name = "week_start") val weekStart: Long,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "tag_0") val tag0: String,
    @ColumnInfo(name = "tag_1") val tag1: String?,
    @ColumnInfo(name = "tag_2") val tag2: String?,
    val sentence: String,
    @ColumnInfo(name = "model_commit") val modelCommit: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
)
