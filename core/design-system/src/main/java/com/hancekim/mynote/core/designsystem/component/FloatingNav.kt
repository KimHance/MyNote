package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.hancekim.mynote.core.designfoundation.token.MyNoteColors
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.kyant.backdrop.Backdrop
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class FloatingNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

object FloatingNavDefaults {
    val BarHeight: Dp = 60.dp
}

private const val LABEL_FADE_IN_MILLIS = 150
private const val LABEL_FADE_IN_DELAY = 60
private const val LABEL_FADE_OUT_MILLIS = 90

private const val HIDE_TRANSFORM_MILLIS = 340
private const val HIDE_ALPHA_MILLIS = 200

@Composable
fun FloatingNavBar(
    items: ImmutableList<FloatingNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    val progress by hideProgress(hidden)
    val alpha by hideAlpha(hidden)
    GlassSurface(
        radius = 24.dp,
        backdrop = backdrop,
        modifier =
            modifier.graphicsLayer {
                translationY = size.height * 1.6f * progress
                this.alpha = alpha
            },
    ) {
        Row(
            modifier = Modifier.padding(7.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { item ->
                BarItem(
                    item = item,
                    selected = item.id == selectedId,
                    onClick = { onSelect(item.id) },
                )
            }
        }
    }
}

@Composable
fun FloatingNavRail(
    items: ImmutableList<FloatingNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    hidden: Boolean,
    rightMargin: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    val progress by hideProgress(hidden)
    val alpha by hideAlpha(hidden)

    GlassSurface(
        radius = 22.dp,
        backdrop = backdrop,
        modifier =
            modifier.graphicsLayer {
                translationX = (size.width + rightMargin.toPx()) * progress
                this.alpha = alpha
            },
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { item ->
                RailItem(
                    item = item,
                    selected = item.id == selectedId,
                    onClick = { onSelect(item.id) },
                )
            }
        }
    }
}

@Composable
private fun hideProgress(hidden: Boolean) =
    animateFloatAsState(
        targetValue = if (hidden) 1f else 0f,
        animationSpec = tween(HIDE_TRANSFORM_MILLIS, easing = FastOutSlowInEasing),
        label = "floatingNavTransform",
    )

@Composable
private fun hideAlpha(hidden: Boolean) =
    animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = tween(HIDE_ALPHA_MILLIS, easing = FastOutSlowInEasing),
        label = "floatingNavAlpha",
    )

@Composable
private fun BarItem(
    item: FloatingNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val selection by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navSelection",
    )

    Row(
        modifier =
            Modifier
                .height(ItemHeight)
                .selectionPill({ selection }, { pressed }, colors, ItemRadius)
                .selectable(
                    selected = selected,
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                ).padding(horizontal = ItemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavIcon(
            item = item,
            selection = { selection },
            colors = colors,
            contentDescription = if (selected) null else item.label,
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally() + fadeIn(tween(LABEL_FADE_IN_MILLIS, delayMillis = LABEL_FADE_IN_DELAY)),
            exit = shrinkHorizontally() + fadeOut(tween(LABEL_FADE_OUT_MILLIS)),
        ) {
            Text(
                text = item.label,
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.01).em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

@Composable
private fun NavIcon(
    item: FloatingNavItem,
    selection: () -> Float,
    colors: MyNoteColors,
    contentDescription: String?,
) {
    val painter = rememberVectorPainter(item.icon)
    val description = contentDescription
    Box(
        modifier =
            Modifier
                .size(IconSize)
                .graphicsLayer {
                    val scale = lerp(1f, SELECTED_ICON_SCALE, selection())
                    scaleX = scale
                    scaleY = scale
                }.drawBehind {
                    val tint = lerp(colors.text2, colors.accent, selection())
                    val canvasSize = this@drawBehind.size
                    with(painter) { draw(canvasSize, colorFilter = ColorFilter.tint(tint)) }
                }.semantics {
                    if (description != null) {
                        this.contentDescription = description
                        role = Role.Image
                    }
                },
    )
}

@Composable
private fun RailItem(
    item: FloatingNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val selection by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "railSelection",
    )

    Box(
        modifier =
            Modifier
                .size(RailItemSize)
                .selectionPill({ selection }, { pressed }, colors, RailItemRadius)
                .selectable(
                    selected = selected,
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        NavIcon(
            item = item,
            selection = { selection },
            colors = colors,
            contentDescription = item.label,
        )
    }
}

private fun Modifier.selectionPill(
    selection: () -> Float,
    pressed: () -> Boolean,
    colors: MyNoteColors,
    radius: Dp,
): Modifier =
    this
        .clip(RoundedCornerShape(radius))
        .drawBehind {
            val t = selection().coerceIn(0f, 1f)
            if (t > 0f) {
                val r = radius.toPx()

                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            colors.navItemSelectedTop.copy(alpha = colors.navItemSelectedTop.alpha * t),
                            colors.navItemSelectedBottom.copy(alpha = colors.navItemSelectedBottom.alpha * t),
                        ),
                    ),
                )

                drawRect(
                    brush =
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colors.navItemCaustic.copy(alpha = colors.navItemCaustic.alpha * t),
                            ),
                            startY = size.height - r,
                            endY = size.height,
                        ),
                    topLeft = Offset(0f, size.height - r),
                    size = Size(size.width, r),
                )

                drawRect(
                    brush =
                        Brush.verticalGradient(
                            listOf(colors.navItemRim.copy(alpha = colors.navItemRim.alpha * t), Color.Transparent),
                            endY = r,
                        ),
                    size = Size(size.width, r),
                )

                // 좌상단 스페큘러 — 작고 밝게. 이게 있어야 구체로 읽힌다
                val spot = Offset(r * 0.9f, r * 0.55f)
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            listOf(
                                colors.navItemSpecular.copy(alpha = colors.navItemSpecular.alpha * t),
                                Color.Transparent,
                            ),
                            center = spot,
                            radius = r * 0.85f,
                        ),
                    radius = r * 0.85f,
                    center = spot,
                )
            }
            if (pressed()) drawRect(colors.navItemPressed)
        }

private val ItemHeight = 46.dp
private val ItemRadius = 23.dp
private val ItemHorizontalPadding = 13.dp
private val IconSize = 21.dp
private val RailItemSize = 46.dp
private val RailItemRadius = 15.dp
private const val SELECTED_ICON_SCALE = 1.1f

private val previewNavItems: ImmutableList<FloatingNavItem> =
    persistentListOf(
        FloatingNavItem("home", "홈", MyNoteIcons.Home),
        FloatingNavItem("search", "검색", MyNoteIcons.Search),
        FloatingNavItem("bookmark", "북마크", MyNoteIcons.Bookmark),
        FloatingNavItem("stats", "통계", MyNoteIcons.Stats),
    )

private class HiddenPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview(name = "FloatingNavBar", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun FloatingNavBarPreview(
    @PreviewParameter(HiddenPreviewParameterProvider::class) hidden: Boolean,
) {
    MyNoteTheme {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            FloatingNavBar(
                items = previewNavItems,
                selectedId = "home",
                onSelect = {},
                hidden = hidden,
            )
        }
    }
}

@Preview(name = "FloatingNavRail", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 160)
@Composable
private fun FloatingNavRailPreview(
    @PreviewParameter(HiddenPreviewParameterProvider::class) hidden: Boolean,
) {
    MyNoteTheme {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            FloatingNavRail(
                items = previewNavItems,
                selectedId = "home",
                onSelect = {},
                hidden = hidden,
                rightMargin = 20.dp,
            )
        }
    }
}
