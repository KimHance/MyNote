package com.hancekim.mynote.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.hancekim.mynote.core.database.entity.QuizPatternEntity
import com.hancekim.mynote.core.database.entity.WeeklySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface InsightDao {
    @Upsert
    suspend fun upsertSummary(summary: WeeklySummaryEntity)

    @Query(
        "SELECT * FROM weekly_summaries WHERE model_commit = :commit AND schema_version = :version " +
            "ORDER BY week_start DESC, generated_at DESC LIMIT 1",
    )
    fun observeLatestSummary(
        commit: String,
        version: Int,
    ): Flow<WeeklySummaryEntity?>

    @Query(
        "SELECT * FROM weekly_summaries WHERE model_commit = :commit AND schema_version = :version " +
            "ORDER BY week_start DESC",
    )
    fun observeSummaries(
        commit: String,
        version: Int,
    ): Flow<List<WeeklySummaryEntity>>

    @Query(
        "SELECT * FROM weekly_summaries WHERE model_commit = :commit AND schema_version = :version " +
            "ORDER BY week_start DESC",
    )
    suspend fun summaries(
        commit: String,
        version: Int,
    ): List<WeeklySummaryEntity>

    @Query("DELETE FROM weekly_summaries WHERE week_start < :before")
    suspend fun pruneSummaries(before: Long)

    @Upsert
    suspend fun upsertPattern(pattern: QuizPatternEntity)

    @Query(
        "SELECT * FROM quiz_patterns WHERE model_commit = :commit AND schema_version = :version " +
            "ORDER BY week_start DESC, generated_at DESC LIMIT 1",
    )
    fun observeLatestPattern(
        commit: String,
        version: Int,
    ): Flow<QuizPatternEntity?>

    @Query(
        "SELECT * FROM quiz_patterns WHERE model_commit = :commit AND schema_version = :version " +
            "ORDER BY week_start DESC, generated_at DESC LIMIT 1",
    )
    suspend fun latestPattern(
        commit: String,
        version: Int,
    ): QuizPatternEntity?

    @Query("DELETE FROM quiz_patterns WHERE week_start < :before")
    suspend fun prunePatterns(before: Long)

    @Transaction
    suspend fun prune(before: Long) {
        pruneSummaries(before)
        prunePatterns(before)
    }
}
