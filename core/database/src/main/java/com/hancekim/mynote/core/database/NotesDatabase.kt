package com.hancekim.mynote.core.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.hancekim.mynote.core.database.dao.InsightDao
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.dao.StudyLogDao
import com.hancekim.mynote.core.database.entity.BookmarkEventEntity
import com.hancekim.mynote.core.database.entity.QuizAnswerEntity
import com.hancekim.mynote.core.database.entity.QuizAttemptEntity
import com.hancekim.mynote.core.database.entity.QuizPatternEntity
import com.hancekim.mynote.core.database.entity.ReadSessionEntity
import com.hancekim.mynote.core.database.entity.SourceEntity
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import com.hancekim.mynote.core.database.entity.WeeklySummaryEntity

@Database(
    entities = [
        SourceEntity::class,
        SourceNoteEntity::class,
        ReadSessionEntity::class,
        QuizAttemptEntity::class,
        QuizAnswerEntity::class,
        BookmarkEventEntity::class,
        WeeklySummaryEntity::class,
        QuizPatternEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
internal abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    abstract fun studyLogDao(): StudyLogDao

    abstract fun insightDao(): InsightDao
}
