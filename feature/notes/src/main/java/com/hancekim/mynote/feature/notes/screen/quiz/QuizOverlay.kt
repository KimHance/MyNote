package com.hancekim.mynote.feature.notes.screen.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.QuizUiState

@Composable
internal fun BoxScope.QuizOverlay(
    state: QuizUiState,
    noteTitle: String,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    startInset: Dp = 0.dp,
) {
    var lastSetup by remember { mutableStateOf<QuizUiState.Setup?>(null) }
    if (state is QuizUiState.Setup) lastSetup = state

    BackHandler(enabled = state != QuizUiState.Idle) { onIntent(NotesIntent.DismissQuiz) }

    if (state.coversReader) {
        Box(
            modifier =
                modifier
                    .matchParentSize()
                    .padding(start = startInset)
                    .background(MyNoteTheme.colors.bg)
                    .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            when (state) {
                is QuizUiState.Loading -> QuizGeneratingScreen(state = state, onIntent = onIntent)
                is QuizUiState.Solving -> QuizSolveScreen(state = state, onIntent = onIntent)
                is QuizUiState.Result -> QuizResultScreen(state = state, onIntent = onIntent)
                is QuizUiState.Failed -> QuizFailedScreen(state = state, onIntent = onIntent)
                else -> Unit
            }
        }
    }

    val setup = lastSetup
    if (setup != null) {
        AnimatedVisibility(
            visible = state is QuizUiState.Setup,
            enter = fadeIn(tween(SCRIM_FADE_MILLIS)),
            exit = fadeOut(tween(CLOSE_MILLIS)),
            modifier = Modifier.matchParentSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = startInset)
                    .background(MyNoteTheme.colors.scrim)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "닫기",
                    ) { onIntent(NotesIntent.DismissQuiz) },
            )
        }

        AnimatedVisibility(
            visible = state is QuizUiState.Setup,
            enter =
                fadeIn(tween(OPEN_MILLIS, easing = QuizEasing)) +
                    scaleIn(
                        animationSpec = tween(OPEN_MILLIS, easing = QuizEasing),
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin(0.5f, 0.85f),
                    ) + slideInVertically(tween(OPEN_MILLIS, easing = QuizEasing)) { it / 8 },
            exit =
                fadeOut(tween(CLOSE_MILLIS, easing = QuizEasing)) +
                    scaleOut(
                        animationSpec = tween(CLOSE_MILLIS, easing = QuizEasing),
                        targetScale = 0.92f,
                        transformOrigin = TransformOrigin(0.5f, 0.85f),
                    ),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .padding(start = startInset + 22.dp, end = 22.dp),
        ) {
            QuizSetupDialog(
                state = setup,
                noteTitle = noteTitle,
                onIntent = onIntent,
                bodyModifier =
                    Modifier.animateEnterExit(
                        enter =
                            fadeIn(tween(BODY_MILLIS, delayMillis = BODY_DELAY_MILLIS)) +
                                slideInVertically(
                                    tween(BODY_MILLIS, delayMillis = BODY_DELAY_MILLIS),
                                ) { it / 4 },
                        exit = fadeOut(tween(BODY_EXIT_MILLIS)),
                    ),
            )
        }
    }
}

private const val OPEN_MILLIS = 340
private const val CLOSE_MILLIS = 250
private const val SCRIM_FADE_MILLIS = 220
private const val BODY_MILLIS = 190
private const val BODY_DELAY_MILLIS = 130
private const val BODY_EXIT_MILLIS = 150
