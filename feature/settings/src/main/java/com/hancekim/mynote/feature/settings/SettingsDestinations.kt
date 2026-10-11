package com.hancekim.mynote.feature.settings

import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.frame.ShellDestinations
import com.hancekim.mynote.core.ui.frame.sceneEdgeModifier
import com.hancekim.mynote.feature.settings.screen.SettingsScreen

/**
 * 설정 기능의 목적지. 설정 상태는 화면 안에서 읽는다. [onEnableNotifications] 는 시스템 알림 설정을 여는 루트의 일이다.
 */
fun settingsDestinations(
    state: () -> SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onEnableNotifications: () -> Unit,
): ShellDestinations =
    { frame ->
        destination<NotesNavKey.Settings> {
            val modifier = frame.sceneEdgeModifier()
            SettingsScreen(
                uiState = state(),
                onIntent = onIntent,
                onEnableNotifications = onEnableNotifications,
                drafts = frame.drafts,
                snackbarHostState = frame.snackbarHostState,
                modifier = modifier,
            )
        }
    }
