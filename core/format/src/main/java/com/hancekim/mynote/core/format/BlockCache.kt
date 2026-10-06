package com.hancekim.mynote.core.format

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.encodeToStream
import java.io.ByteArrayOutputStream

private val BLOCKS = ListSerializer(NoteBlockDto.serializer())

/** 블록 캐시 JSON 을 쓰고 읽는다. */
internal object BlockCache {
    fun encode(blocks: List<NoteBlockDto>): String = BlockJson.encodeToString(BLOCKS, blocks)

    /**
     * UTF-8 크기가 상한을 넘지 않을 때만 쓴다. 넘으면 끝까지 쓰지 않고 null 이다.
     *
     * @param maxBytes 캐시 JSON 의 UTF-8 크기 상한
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun encode(
        blocks: List<NoteBlockDto>,
        maxBytes: Int,
    ): String? {
        val out = CappedBytes(maxBytes)
        try {
            BlockJson.encodeToStream(BLOCKS, blocks, out)
        } catch (_: CapReached) {
            return null
        }
        return out.toString(Charsets.UTF_8.name())
    }

    fun decode(blocksJson: String): List<NoteBlockDto> = BlockJson.decodeFromString(BLOCKS, blocksJson)
}

private class CapReached : RuntimeException(null, null, false, false)

private class CappedBytes(
    private val maxBytes: Int,
) : ByteArrayOutputStream() {
    override fun write(b: Int) {
        if (count + 1 > maxBytes) throw CapReached()
        super.write(b)
    }

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        if (count + len > maxBytes) throw CapReached()
        super.write(b, off, len)
    }
}
