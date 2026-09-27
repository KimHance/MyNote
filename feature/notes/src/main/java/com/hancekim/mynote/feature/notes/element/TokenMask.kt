package com.hancekim.mynote.feature.notes.element

fun maskToken(token: String?): String? {
    val value = token?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (value.length <= VISIBLE_HEAD + VISIBLE_TAIL) return BULLETS
    return value.take(VISIBLE_HEAD) + BULLETS + value.takeLast(VISIBLE_TAIL)
}

fun maskedTokenTail(token: String?): String? =
    token?.trim()?.takeIf { it.length > VISIBLE_HEAD + VISIBLE_TAIL }?.takeLast(VISIBLE_TAIL)

private const val VISIBLE_HEAD = 4
private const val VISIBLE_TAIL = 4
private const val BULLETS = "••••••••"
