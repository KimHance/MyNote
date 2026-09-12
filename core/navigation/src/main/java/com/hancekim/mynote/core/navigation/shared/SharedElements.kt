package com.hancekim.mynote.core.navigation.shared

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

internal val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun ProvideNavAnimatedScope(content: @Composable () -> Unit) {
    val scope = LocalNavAnimatedScope.current ?: LocalNavAnimatedContentScope.current
    CompositionLocalProvider(LocalNavAnimatedScope provides scope) {
        content()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.navSharedElement(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    return with(shared) {
        this@navSharedElement.sharedElement(
            rememberSharedContentState(key),
            animatedVisibilityScope = animated,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.navSharedBounds(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    return with(shared) {
        this@navSharedBounds.sharedBounds(
            rememberSharedContentState(key),
            animatedVisibilityScope = animated,
        )
    }
}
