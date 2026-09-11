package com.hancekim.mynote.core.domain.model

data class SyncOutcome(
    val added: Int = 0,
    val updated: Int = 0,
    val deleted: List<String> = emptyList(),
) {
    val changed: Int get() = added + updated

    val hasChanges: Boolean get() = changed > 0 || deleted.isNotEmpty()
}
