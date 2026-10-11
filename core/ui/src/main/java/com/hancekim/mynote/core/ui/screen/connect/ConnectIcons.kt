package com.hancekim.mynote.core.ui.screen.connect

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ConnectIcons {
    val Folder: ImageVector by lazy {
        strokeIcon("folder") {
            moveTo(3f, 7f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5f, y1 = 5f)
            lineTo(9.2f, 5f)
            lineTo(11.4f, 7.2f)
            lineTo(19f, 7.2f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 21f, y1 = 9.2f)
            lineTo(21f, 17f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19f, y1 = 19f)
            lineTo(5f, 19f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3f, y1 = 17f)
            close()
        }
    }

    val Download: ImageVector by lazy {
        strokeIcon("download") {
            moveTo(12f, 4f)
            lineTo(12f, 15f)
            moveTo(7.5f, 10.5f)
            lineTo(12f, 15f)
            lineTo(16.5f, 10.5f)
            moveTo(5f, 19.5f)
            lineTo(19f, 19.5f)
        }
    }

    val Lock: ImageVector by lazy {
        strokeIcon("lock") {
            moveTo(6f, 11f)
            lineTo(18f, 11f)
            lineTo(18f, 19.5f)
            lineTo(6f, 19.5f)
            close()
            moveTo(8.5f, 11f)
            lineTo(8.5f, 8f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.5f, y1 = 8f)
            lineTo(15.5f, 11f)
        }
    }

    val External: ImageVector by lazy {
        strokeIcon("external") {
            moveTo(11f, 6f)
            lineTo(6f, 6f)
            lineTo(6f, 18f)
            lineTo(18f, 18f)
            lineTo(18f, 13f)
            moveTo(14f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 10f)
            moveTo(19f, 5f)
            lineTo(11f, 13f)
        }
    }

    val Alert: ImageVector by lazy {
        strokeIcon("alert") {
            moveTo(21f, 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3f, y1 = 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 21f, y1 = 12f)
            close()
            moveTo(12f, 7.5f)
            lineTo(12f, 12.5f)
            moveTo(12f, 15.8f)
            lineTo(12f, 16.2f)
        }
    }
}

private fun strokeIcon(
    name: String,
    block: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()
