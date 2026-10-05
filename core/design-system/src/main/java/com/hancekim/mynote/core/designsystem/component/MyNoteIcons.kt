package com.hancekim.mynote.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object MyNoteIcons {
    val Home: ImageVector by lazy {
        strokeIcon("nav_home") {
            moveTo(3f, 10.5f)
            lineTo(12f, 3.5f)
            lineTo(21f, 10.5f)
            lineTo(21f, 20f)
            lineTo(3f, 20f)
            close()
        }
    }

    val Search: ImageVector by lazy {
        strokeIcon("nav_search") {
            moveTo(16.5f, 10.5f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 4.5f, y1 = 10.5f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 16.5f, y1 = 10.5f)
            close()
            moveTo(14.9f, 14.9f)
            lineTo(20f, 20f)
        }
    }

    val Bookmark: ImageVector by lazy {
        strokeIcon("nav_bookmark") {
            moveTo(6f, 3.5f)
            lineTo(18f, 3.5f)
            lineTo(18f, 20.5f)
            lineTo(12f, 15.8f)
            lineTo(6f, 20.5f)
            close()
        }
    }

    val BookmarkFilled: ImageVector by lazy {
        filledIcon("bookmark_filled") {
            moveTo(6f, 3.5f)
            lineTo(18f, 3.5f)
            lineTo(18f, 20.5f)
            lineTo(12f, 15.8f)
            lineTo(6f, 20.5f)
            close()
        }
    }

    val Stats: ImageVector by lazy {
        strokeIcon("nav_stats") {
            moveTo(5f, 20f)
            lineTo(5f, 13f)
            moveTo(12f, 20f)
            lineTo(12f, 5f)
            moveTo(19f, 20f)
            lineTo(19f, 10f)
        }
    }

    val Settings: ImageVector by lazy {
        strokeIcon("settings") {
            moveTo(15f, 12f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 9f, y1 = 12f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 12f)
            close()
            moveTo(19f, 12f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 5f, y1 = 12f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 19f, y1 = 12f)
            close()
            moveTo(12f, 2.6f)
            lineTo(12f, 5.2f)
            moveTo(12f, 18.8f)
            lineTo(12f, 21.4f)
            moveTo(2.6f, 12f)
            lineTo(5.2f, 12f)
            moveTo(18.8f, 12f)
            lineTo(21.4f, 12f)
            moveTo(5.3f, 5.3f)
            lineTo(7.2f, 7.2f)
            moveTo(16.8f, 16.8f)
            lineTo(18.7f, 18.7f)
            moveTo(18.7f, 5.3f)
            lineTo(16.8f, 7.2f)
            moveTo(7.2f, 16.8f)
            lineTo(5.3f, 18.7f)
        }
    }

    val Key: ImageVector by lazy {
        strokeIcon("key") {
            moveTo(12f, 15f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 5f, y1 = 15f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 15f)
            close()
            moveTo(11.1f, 12.9f)
            lineTo(19.5f, 4.5f)
            moveTo(16.4f, 7.6f)
            lineTo(18.6f, 9.8f)
            moveTo(14.2f, 9.8f)
            lineTo(16f, 11.6f)
        }
    }

    val ArrowUp: ImageVector by lazy {
        strokeIcon("arrow_up") {
            moveTo(12f, 19.5f)
            lineTo(12f, 5f)
            moveTo(5.5f, 11.5f)
            lineTo(12f, 5f)
            lineTo(18.5f, 11.5f)
        }
    }

    val Sparkle: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "sparkle",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = SolidColor(Color.White)) {
                fourPointStar(cx = 10.5f, cy = 10.5f, r = 8f)
            }.path(fill = SolidColor(Color.White), fillAlpha = 0.55f) {
                fourPointStar(cx = 19f, cy = 18.6f, r = 3.4f)
            }.build()
    }

    val Check: ImageVector by lazy {
        strokeIcon("check") {
            moveTo(4.5f, 12.5f)
            lineTo(9.5f, 17.5f)
            lineTo(19.5f, 6.5f)
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("close") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon("chevron_right") {
            moveTo(9.5f, 4.5f)
            lineTo(17f, 12f)
            lineTo(9.5f, 19.5f)
        }
    }

    val Warn: ImageVector by lazy {
        strokeIcon("warn") {
            moveTo(12f, 3.5f)
            lineTo(21.5f, 20.5f)
            lineTo(2.5f, 20.5f)
            close()
            moveTo(12f, 9.5f)
            lineTo(12f, 14.5f)
            moveTo(12f, 17.6f)
            lineTo(12f, 17.7f)
        }
    }
}

private fun PathBuilder.fourPointStar(
    cx: Float,
    cy: Float,
    r: Float,
) {
    val near = r * 0.1f
    val far = r * 0.433f
    moveTo(cx, cy - r)
    curveTo(cx + near, cy - far, cx + far, cy - near, cx + r, cy)
    curveTo(cx + far, cy + near, cx + near, cy + far, cx, cy + r)
    curveTo(cx - near, cy + far, cx - far, cy + near, cx - r, cy)
    curveTo(cx - far, cy - near, cx - near, cy - far, cx, cy - r)
    close()
}

private fun filledIcon(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 21.dp,
            defaultHeight = 21.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.White),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        ).build()

private fun strokeIcon(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 21.dp,
            defaultHeight = 21.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        ).build()
