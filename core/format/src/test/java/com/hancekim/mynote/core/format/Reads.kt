package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import kotlinx.coroutines.runBlocking

/** 원문을 미리 쥐고 캐시를 읽는다. */
internal fun NoteFormat.read(
    blocksJson: String,
    path: String?,
    raw: String?,
): List<NoteBlock> = runBlocking { read(blocksJson, path) { raw } }
