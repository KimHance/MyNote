package com.hancekim.mynote.feature.notes.element

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val CLASSIC_PREFIX = "ghp_"

class SecretText(
    val value: String,
) {
    override fun toString(): String = "SecretText(hidden)"
}

@Stable
class TokenDraft {
    var text by mutableStateOf("")
    var revealed by mutableStateOf(false)

    val isBlank: Boolean get() = text.isBlank()

    val isClassic: Boolean get() = isClassicToken(text)

    fun secret(): SecretText = SecretText(text)

    fun clear() {
        text = ""
        revealed = false
    }

    override fun toString(): String = "TokenDraft(hidden)"
}

@Stable
class NotesDrafts {
    val connectToken = TokenDraft()
    val settingsToken = TokenDraft()

    private var handledResetCount = 0

    fun consumeReset(count: Int) {
        if (count == handledResetCount) return
        handledResetCount = count
        connectToken.clear()
    }
}

fun isClassicToken(token: String): Boolean = token.trim().startsWith(CLASSIC_PREFIX)
