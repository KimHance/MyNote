package com.hancekim.mynote.core.format

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val ROOT = "/parity"

/**
 * 파리티 고정 사본의 한 노트 — 옮기기 전 JSON 과 옮긴 마크다운.
 *
 * @property group 그룹(폴더 이름)
 * @property id 노트 id(파일 이름)
 * @property hash 기준 목록이 든 JSON 파일 바이트의 sha256
 */
class ParityNote(
    val group: String,
    val id: String,
    val title: String,
    val date: String,
    val hash: String,
    val jsonBytes: ByteArray,
    val markdownBytes: ByteArray,
) {
    val jsonPath: String get() = "notes/$group/$id.json"
    val markdownPath: String get() = "notes/$group/$id.md"
    val json: String get() = jsonBytes.decodeToString()
    val markdown: String get() = markdownBytes.decodeToString()
}

/** 기준 목록 사본의 순서대로 읽은 파리티 사본 전부. */
object ParityNotes {
    val indexBytes: ByteArray by lazy { resource("$ROOT/index.json") }

    val all: List<ParityNote> by lazy {
        Json.parseToJsonElement(indexBytes.decodeToString()).jsonObject.flatMap { (group, entries) ->
            entries.jsonArray.map { element ->
                val entry = element.jsonObject.mapValues { it.value.jsonPrimitive.content }
                val id = entry.getValue("id")
                ParityNote(
                    group = group,
                    id = id,
                    title = entry.getValue("title"),
                    date = entry.getValue("date"),
                    hash = entry.getValue("hash"),
                    jsonBytes = resource("$ROOT/json/$group/$id.json"),
                    markdownBytes = resource("$ROOT/md/$group/$id.md"),
                )
            }
        }
    }

    private fun resource(path: String): ByteArray =
        checkNotNull(ParityNotes::class.java.getResourceAsStream(path)) { "시험 자료 없음: $path" }.use { it.readBytes() }
}
