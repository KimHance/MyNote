package com.hancekim.mynote.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteBrandColors

internal const val MARK_WIDTH = 76.1f
internal const val MARK_HEIGHT = 92f
internal const val WORDMARK_WIDTH = 315.3f
internal const val WORDMARK_HEIGHT = 114f
internal const val WORDMARK_X_HEIGHT = 72f

private const val LOGO_STROKE = 8.5f

/**
 * 아치 창 두 개로 만든 A 마크. 오른쪽 창의 빛은 로고 전용 주광 그라디언트다.
 *
 * @param frame 창틀 선 색
 */
internal fun heroMark(frame: Color): ImageVector =
    ImageVector
        .Builder(
            name = "hero_mark",
            defaultWidth = MARK_WIDTH.dp,
            defaultHeight = MARK_HEIGHT.dp,
            viewportWidth = MARK_WIDTH,
            viewportHeight = MARK_HEIGHT,
        ).path(
            fill = null,
            stroke = SolidColor(frame),
            strokeLineWidth = LOGO_STROKE,
        ) {
            moveTo(4.25f, 92f)
            verticalLineTo(21.15f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 38.05f, y1 = 21.15f)
            verticalLineTo(92f)
            moveTo(38.05f, 21.15f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 71.85f, y1 = 21.15f)
            verticalLineTo(92f)
        }.path(
            fill =
                Brush.linearGradient(
                    0f to MyNoteBrandColors.LampTop,
                    1f to MyNoteBrandColors.LampBottom,
                    start = Offset(0f, 0f),
                    end = Offset(0f, MARK_HEIGHT),
                ),
        ) {
            moveTo(45.5f, 92f)
            verticalLineTo(21.15f)
            arcTo(9.45f, 9.45f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 64.4f, y1 = 21.15f)
            verticalLineTo(92f)
            close()
        }.build()

/**
 * 소문자 mynote 워드마크. x높이는 뷰포트의 WORDMARK_X_HEIGHT 다.
 *
 * @param ink 선 색
 */
internal fun heroWordmark(ink: Color): ImageVector =
    ImageVector
        .Builder(
            name = "hero_wordmark",
            defaultWidth = WORDMARK_WIDTH.dp,
            defaultHeight = WORDMARK_HEIGHT.dp,
            viewportWidth = WORDMARK_WIDTH,
            viewportHeight = WORDMARK_HEIGHT,
        ).path(
            fill = null,
            stroke = SolidColor(ink),
            strokeLineWidth = LOGO_STROKE,
        ) {
            moveTo(4.25f, 90f)
            verticalLineTo(39.15f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 38.05f, y1 = 39.15f)
            verticalLineTo(90f)
            moveTo(38.05f, 39.15f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 71.85f, y1 = 39.15f)
            verticalLineTo(90f)
            moveTo(90.85f, 18f)
            verticalLineTo(68.85f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 124.65f, y1 = 68.85f)
            moveTo(124.65f, 18f)
            verticalLineTo(114f)
            moveTo(143.65f, 90f)
            verticalLineTo(39.15f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 177.45f, y1 = 39.15f)
            verticalLineTo(90f)
            moveTo(195.45f, 39.15f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 229.25f, y1 = 39.15f)
            verticalLineTo(69.85f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 195.45f, y1 = 69.85f)
            close()
            moveTo(252f, 0f)
            verticalLineTo(90f)
            moveTo(240.5f, 22.25f)
            horizontalLineTo(267.5f)
            moveTo(273.01f, 51.5f)
            horizontalLineTo(311.05f)
            verticalLineTo(39.15f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 277.25f, y1 = 39.15f)
            verticalLineTo(69.85f)
            arcTo(16.9f, 16.9f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 309.07f, y1 = 77.78f)
        }.build()
