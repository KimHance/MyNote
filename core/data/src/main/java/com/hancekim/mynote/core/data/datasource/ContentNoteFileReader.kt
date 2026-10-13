package com.hancekim.mynote.core.data.datasource

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.hancekim.mynote.core.domain.model.FileText
import com.hancekim.mynote.core.domain.repository.NoteFileReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import javax.inject.Inject

/** 시스템 파일 고르기가 준 `content://` 주소를 읽는다(LN-6 (b) · LN-7). 권한 · 사라짐은 던진다. */
class ContentNoteFileReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NoteFileReader {
    override suspend fun displayName(uri: String): String? =
        withContext(Dispatchers.IO) {
            val parsed = uri.toUri()
            context.contentResolver
                .query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null }
                ?: parsed.lastPathSegment
        }

    override suspend fun read(
        uri: String,
        maxBytes: Int,
    ): FileText =
        withContext(Dispatchers.IO) {
            val input =
                context.contentResolver.openInputStream(uri.toUri()) ?: throw FileNotFoundException(uri)
            input.use { readStrictUtf8(it, maxBytes) }
        }
}

/** [maxBytes] + 1 바이트까지만 읽고, 넘으면 [FileText.TooLarge]. 대체 문자 없이 UTF-8 로 풀지 못하면 [FileText.NotUtf8]. */
internal fun readStrictUtf8(
    input: InputStream,
    maxBytes: Int,
): FileText {
    val out = ByteArrayOutputStream()
    val chunk = ByteArray(CHUNK_BYTES)
    var total = 0
    while (true) {
        val read = input.read(chunk, 0, minOf(chunk.size, maxBytes + 1 - total))
        if (read < 0) break
        out.write(chunk, 0, read)
        total += read
        if (total > maxBytes) return FileText.TooLarge
    }
    val decoder =
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
    return try {
        FileText.Read(decoder.decode(ByteBuffer.wrap(out.toByteArray())).toString())
    } catch (_: CharacterCodingException) {
        FileText.NotUtf8
    }
}

private const val CHUNK_BYTES = 8 * 1_024
