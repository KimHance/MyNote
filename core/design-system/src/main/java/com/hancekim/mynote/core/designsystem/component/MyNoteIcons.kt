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

    val ArrowUp: ImageVector by lazy {
        strokeIcon("arrow_up") {
            moveTo(12f, 19.5f)
            lineTo(12f, 5f)
            moveTo(5.5f, 11.5f)
            lineTo(12f, 5f)
            lineTo(18.5f, 11.5f)
        }
    }
}

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
