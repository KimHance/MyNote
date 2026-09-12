package com.hancekim.mynote.core.designfoundation.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MyNoteColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentSoftBorder: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val bodyText: Color,
    val glass: Color,
    val glassOpaque: Color,
    val glassBorder: Color,
    val navItemSelectedTop: Color,
    val navItemSelectedBottom: Color,
    val navItemRim: Color,
    val navItemSpecular: Color,
    val navItemCaustic: Color,
    val navItemPressed: Color,
)

val StudyNotesDarkColors: MyNoteColors =
    MyNoteColors(
        bg = Color(0xFF0E1217),
        surface = Color(0xFF161C24),
        surface2 = Color(0xFF1B232D),
        border = Color(0xFF283039),
        accent = Color(0xFF6EA8FE),
        accentSoft = Color(0x246EA8FE),
        accentSoftBorder = Color(0x426EA8FE),
        text = Color(0xFFE6EDF3),
        text2 = Color(0xFF9AA7B4),
        text3 = Color(0xFF687585),
        bodyText = Color(0xFFC8D2DC),
        glass = Color(0xA8181F28),
        glassOpaque = Color(0xEB181F28),
        glassBorder = Color(0x17FFFFFF),
        navItemSelectedTop = Color(0x2B6EA8FE),
        navItemSelectedBottom = Color(0x0D6EA8FE),
        navItemRim = Color(0x2EFFFFFF),
        navItemSpecular = Color(0x59FFFFFF),
        navItemCaustic = Color(0x4DB8D4FF),
        navItemPressed = Color(0x0FFFFFFF),
    )
