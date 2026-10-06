package com.hancekim.mynote.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class IndexNoteDto(
    val id: String,
    val title: String,
    val date: String,
    val path: String,
    val hash: String,
)
