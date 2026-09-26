package com.hancekim.mynote.feature.notes.screen.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.DEFAULT_FLIP_IN_MILLIS
import com.hancekim.mynote.core.designsystem.component.DEFAULT_FLIP_OUT_MILLIS
import com.hancekim.mynote.core.designsystem.component.flip
import com.hancekim.mynote.core.designsystem.component.rememberFlipState
import com.hancekim.mynote.core.designsystem.theme.MyNoteEasing
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.QuizUiState
import com.kyant.backdrop.Backdrop
import com.hancekim.mynote.core.designsystem.R as DsR

@Composable
internal fun BoxScope.QuizOverlay(
    state: QuizUiState,
    noteTitle: String,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    startInset: Dp = 0.dp,
    backdrop: Backdrop? = null,
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

    val settingUp = state is QuizUiState.Setup

    AnimatedVisibility(
        visible = settingUp,
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
                    onClickLabel = stringResource(DsR.string.ds_action_close),
                ) { onIntent(NotesIntent.DismissQuiz) },
        )
    }

    AnimatedVisibility(
        visible = settingUp,
        enter = fadeIn(tween(DEFAULT_FLIP_IN_MILLIS, easing = MyNoteEasing)),
        exit = fadeOut(tween(DEFAULT_FLIP_OUT_MILLIS, easing = MyNoteEasing)),
        modifier =
            Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .padding(start = startInset + 22.dp, end = 22.dp),
    ) {
        val flipState = rememberFlipState()
        val setup = lastSetup
        if (setup != null) {
            QuizSetupDialog(
                state = setup,
                noteTitle = noteTitle,
                onIntent = onIntent,
                backdrop = backdrop,
                modifier = Modifier.flip(flipState),
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

private const val CLOSE_MILLIS = 250
private const val SCRIM_FADE_MILLIS = 220
private const val BODY_MILLIS = 190
private const val BODY_DELAY_MILLIS = 130
private const val BODY_EXIT_MILLIS = 150
