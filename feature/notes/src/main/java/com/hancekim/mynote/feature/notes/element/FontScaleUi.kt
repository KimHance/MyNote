package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.model.FontScale

internal fun FontScale.toUi(): MyNoteFontScale =
    when (this) {
        FontScale.Normal -> MyNoteFontScale.Normal
        FontScale.Large -> MyNoteFontScale.Large
    }

internal fun MyNoteFontScale.toDomain(): FontScale =
    when (this) {
        MyNoteFontScale.Normal -> FontScale.Normal
        MyNoteFontScale.Large -> FontScale.Large
    }
