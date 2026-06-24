package com.hancekim.mynote.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.entity.NoteEntity

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
internal abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
