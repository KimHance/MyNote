package com.hancekim.mynote.core.data.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val NotesJson: Json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        serializersModule =
            SerializersModule {
                polymorphicDefaultDeserializer(NoteBlockDto::class) { NoteBlockDto.Unknown.serializer() }
            }
    }
