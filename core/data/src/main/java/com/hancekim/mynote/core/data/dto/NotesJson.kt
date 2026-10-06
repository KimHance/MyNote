package com.hancekim.mynote.core.data.dto

import kotlinx.serialization.json.Json

val NotesJson: Json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
