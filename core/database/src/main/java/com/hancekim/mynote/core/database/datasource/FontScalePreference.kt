package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.FontScale

private const val NORMAL = "normal"
private const val LARGE = "large"

internal fun FontScale.toPreference(): String =
    when (this) {
        FontScale.Normal -> NORMAL
        FontScale.Large -> LARGE
    }

internal fun String?.toFontScale(): FontScale =
    when (this) {
        NORMAL -> FontScale.Normal
        LARGE -> FontScale.Large
        else -> FontScale.Default
    }
