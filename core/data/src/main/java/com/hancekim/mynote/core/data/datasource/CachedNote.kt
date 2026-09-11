package com.hancekim.mynote.core.data.datasource

data class CachedNote(
    val id: String,
    val group: String,
    val title: String,
    val date: String,
    val contentHash: String,
    val blocksJson: String,
)
