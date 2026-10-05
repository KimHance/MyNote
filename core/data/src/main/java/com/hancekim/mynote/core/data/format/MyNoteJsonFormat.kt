package com.hancekim.mynote.core.data.format

import com.hancekim.mynote.core.data.dto.NoteBlockDto
import com.hancekim.mynote.core.data.dto.UnsupportedReasonDto
import com.hancekim.mynote.core.data.dto.toDomain
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.firstSentence
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

const val PARSER_VERSION: Int = 1

private val CACHE_ONLY_TYPES = setOf("unknown", "unsupported")

internal val WIRE_TYPES: Set<String> =
    NoteBlockDto
        .serializer()
        .descriptor
        .getElementDescriptor(1)
        .elementNames
        .toSet() - CACHE_ONLY_TYPES

private val prettyJson =
    Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

data class DerivedNote(
    val blocksJson: String,
    val excerpt: String?,
)

class MyNoteJsonFormat(
    private val json: Json,
) {
    fun parse(raw: String): List<NoteBlockDto> {
        val root =
            try {
                json.parseToJsonElement(raw) as? JsonObject
            } catch (_: IllegalArgumentException) {
                null
            }
        val blocks =
            root?.get("blocks") as? JsonArray
                ?: return listOf(NoteBlockDto.Unsupported(UnsupportedReasonDto.BROKEN_NOTE, raw = raw))
        return blocks.map(::parseBlock)
    }

    fun derive(raw: String): DerivedNote {
        val blocks = parse(raw)
        val excerpt = Note(id = "", title = "", date = "", blocks = blocks.map { it.toDomain() }).firstSentence()
        return DerivedNote(blocksJson = encode(blocks), excerpt = excerpt)
    }

    fun encode(blocks: List<NoteBlockDto>): String = json.encodeToString(blocks)

    fun decode(blocksJson: String): List<NoteBlockDto> = json.decodeFromString(blocksJson)

    private fun parseBlock(element: JsonElement): NoteBlockDto {
        val block = element as? JsonObject ?: return unsupported(UnsupportedReasonDto.BROKEN_BLOCK, null, null, element)
        val type = block.string("type")
        val text = block.string("fallback") ?: block.salvage()
        return when {
            type == null -> {
                unsupported(UnsupportedReasonDto.BROKEN_BLOCK, null, text, block)
            }

            type !in WIRE_TYPES -> {
                unsupported(UnsupportedReasonDto.UNKNOWN_TYPE, type, text, block)
            }

            else -> {
                try {
                    json.decodeFromJsonElement(NoteBlockDto.serializer(), block)
                } catch (_: IllegalArgumentException) {
                    unsupported(UnsupportedReasonDto.BROKEN_BLOCK, type, text, block)
                }
            }
        }
    }

    private fun unsupported(
        reason: UnsupportedReasonDto,
        type: String?,
        text: String?,
        element: JsonElement,
    ): NoteBlockDto =
        NoteBlockDto.Unsupported(
            reason = reason,
            original = type,
            text = text,
            raw = prettyJson.encodeToString(JsonElement.serializer(), element),
        )
}

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.spansText(): String? =
    (this["spans"] as? JsonArray)
        ?.mapNotNull { (it as? JsonObject)?.string("text") }
        ?.joinToString("")

private fun JsonObject.salvage(): String? {
    val items =
        (this["items"] as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.spansText()?.takeIf(String::isNotEmpty) }
            ?.joinToString("\n")
    return listOfNotNull(spansText(), string("code"), items)
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .ifEmpty { null }
}
