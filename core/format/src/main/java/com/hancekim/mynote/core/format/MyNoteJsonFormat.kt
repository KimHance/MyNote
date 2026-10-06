package com.hancekim.mynote.core.format

import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal val WIRE_TYPES: Set<String> =
    WireBlockDto
        .serializer()
        .descriptor
        .getElementDescriptor(1)
        .elementNames
        .toSet()

private val prettyJson =
    Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

/**
 * JSON 형식 1 원문을 캐시 블록으로 읽는다. 어떤 입력에도 던지지 않고, 읽지 못한 자리는 상자가 된다.
 *
 * @param json 블록을 디코드하는 Json
 */
internal class MyNoteJsonFormat(
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
                    json.decodeFromJsonElement(WireBlockDto.serializer(), block).toCache()
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
