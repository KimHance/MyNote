package com.hancekim.mynote.core.navigation.scene

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.hancekim.mynote.core.navigation.motion.SceneMotion
import com.hancekim.mynote.core.navigation.shared.LocalNavAnimatedScope

internal data class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val detailEntry: NavEntry<T>,
    val panel: @Composable () -> Unit,
    val header: @Composable () -> Unit,
    val panelExpanded: () -> Boolean,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(detailEntry)

    override val content: @Composable () -> Unit = {
        val fadeOffsetPx = with(LocalDensity.current) { SceneMotion.BodyFadeOffset.roundToPx() }

        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = panelExpanded(),
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {
                panel()
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                header()
                AnimatedContent(
                    targetState = detailEntry,
                    contentKey = { it.contentKey },
                    transitionSpec = {
                        val inSpec =
                            tween<Float>(
                                SceneMotion.BODY_FADE_IN_MILLIS,
                                delayMillis = SceneMotion.BODY_FADE_OUT_MILLIS,
                            )
                        val inOffsetSpec =
                            tween<IntOffset>(
                                SceneMotion.BODY_FADE_IN_MILLIS,
                                delayMillis = SceneMotion.BODY_FADE_OUT_MILLIS,
                            )
                        val enter = fadeIn(inSpec) + slideInVertically(inOffsetSpec) { fadeOffsetPx }
                        val exit =
                            fadeOut(tween(SceneMotion.BODY_FADE_OUT_MILLIS)) +
                                slideOutVertically(tween(SceneMotion.BODY_FADE_OUT_MILLIS)) { fadeOffsetPx }
                        enter togetherWith exit
                    },
                    label = "listDetail",
                ) { entry ->
                    CompositionLocalProvider(LocalNavAnimatedScope provides this@AnimatedContent) {
                        entry.Content()
                    }
                }
            }
        }
    }
}

class ListDetailSceneStrategy<T : Any>(
    private val sceneKey: Any,
    private val panel: @Composable () -> Unit,
    private val header: @Composable () -> Unit,
    private val panelExpanded: () -> Boolean,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val detail = entries.lastOrNull() ?: return null
        return ListDetailScene(
            key = sceneKey,
            previousEntries = entries.dropLast(1),
            detailEntry = detail,
            panel = panel,
            header = header,
            panelExpanded = panelExpanded,
        )
    }
}
