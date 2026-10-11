package com.hancekim.mynote.core.ui.element

import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.model.FontScale

fun FontScale.toUi(): MyNoteFontScale =
    when (this) {
        FontScale.Normal -> MyNoteFontScale.Normal
        FontScale.Large -> MyNoteFontScale.Large
    }

fun MyNoteFontScale.toDomain(): FontScale =
    when (this) {
        MyNoteFontScale.Normal -> FontScale.Normal
        MyNoteFontScale.Large -> FontScale.Large
    }
