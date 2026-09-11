package com.hancekim.mynote.core.domain.model

data class Category(
    val name: String,
    val notes: List<Note>,
)

data class Note(
    val id: String,
    val title: String,
    val date: String,
    val blocks: List<NoteBlock>,
)
