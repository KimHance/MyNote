package com.hancekim.mynote.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.addPathNodes
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

    val CheckBox: ImageVector by lazy {
        strokeIcon("check_box") {
            roundedSquare(start = 3f, end = 21f, radius = 4f)
        }
    }

    val CheckBoxChecked: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "check_box_checked",
                defaultWidth = 21.dp,
                defaultHeight = 21.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
                roundedSquare(start = 2.15f, end = 21.85f, radius = 4.85f)
                moveTo(6.9f, 12.9f)
                lineTo(10.53f, 16.53f)
                lineTo(17.13f, 9.27f)
                arcTo(0.85f, 0.85f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 15.87f, y1 = 8.13f)
                lineTo(10.47f, 14.07f)
                lineTo(8.1f, 11.7f)
                arcTo(0.85f, 0.85f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6.9f, y1 = 12.9f)
                close()
            }.build()
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

    // 기기 노트(local-notes 디자인 C2) — 핸드오프 path 그대로. 빈 상태 타일 · 버튼 · 상단 바 · 출처 표기가 같이 쓴다

    /** 새 노트 — 종이 + 더하기. */
    val NewNote: ImageVector by lazy {
        svgStrokeIcon("note_new", "M12.5 20.5H7a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2h6l5 5v4M13 3.5v5h5M18 15.5v6M15 18.5h6")
    }

    /** 가져오기 — 받침 + 아래 화살표. 가져오기 대화상자의 `파일 열기` 줄도 쓴다. */
    val Import: ImageVector by lazy {
        svgStrokeIcon("import", "M4 13.5v5a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-5M12 3.5V15M7.5 10.5L12 15l4.5-4.5")
    }

    /** 고치기 — 연필. */
    val Edit: ImageVector by lazy {
        svgStrokeIcon("edit", "M14.5 5.5l4 4L9 19H5v-4zM12.5 7.5l4 4")
    }

    /** 지우기 — 휴지통. */
    val Delete: ImageVector by lazy {
        svgStrokeIcon(
            "delete",
            "M4.5 6.5h15M9.5 6.5v-2a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v2" +
                "M6.5 6.5l.9 12.6a1.5 1.5 0 0 0 1.5 1.4h6.2a1.5 1.5 0 0 0 1.5-1.4l.9-12.6M10 10.5v6M14 10.5v6",
        )
    }

    /** 이 기기 — 출처 표기 글리프(12dp 로 쓴다). 색 없이 모양으로 읽힌다. */
    val Device: ImageVector by lazy {
        svgStrokeIcon(
            "device",
            "M9.5 3h5A2.5 2.5 0 0 1 17 5.5v13a2.5 2.5 0 0 1-2.5 2.5h-5A2.5 2.5 0 0 1 7 18.5v-13A2.5 2.5 0 0 1 9.5 3z" +
                "M10.5 17.5h3",
        )
    }

    /** 붙여넣기 — 겹친 종이(뒤 종이는 가린 부분을 그리지 않는다). */
    val Paste: ImageVector by lazy {
        svgStrokeIcon(
            "paste",
            "M8 7V5.5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-2" +
                "M6 7h8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z",
        )
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

private fun PathBuilder.roundedSquare(
    start: Float,
    end: Float,
    radius: Float,
) {
    moveTo(start + radius, start)
    lineTo(end - radius, start)
    arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = end, y1 = start + radius)
    lineTo(end, end - radius)
    arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = end - radius, y1 = end)
    lineTo(start + radius, end)
    arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = start, y1 = end - radius)
    lineTo(start, start + radius)
    arcTo(radius, radius, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = start + radius, y1 = start)
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

/** SVG path 글자로 그린 선 아이콘 — 디자인 핸드오프 path 를 그대로 옮길 때. 규격은 [strokeIcon] 과 같다. */
private fun svgStrokeIcon(
    name: String,
    pathData: String,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 21.dp,
            defaultHeight = 21.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
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
