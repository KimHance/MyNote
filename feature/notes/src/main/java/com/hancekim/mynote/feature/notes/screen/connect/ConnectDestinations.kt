package com.hancekim.mynote.feature.notes.screen.connect

import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.frame.ShellDestinations
import com.hancekim.mynote.core.ui.frame.sceneEdgeModifier
import com.hancekim.mynote.feature.notes.element.ConnectPlace

/**
 * 연결 기능의 목적지 — 쌓은 연결 화면. 상태는 화면이 자기 ViewModel(Activity owner)에서 받는다.
 * [hasConnection] 은 셸 상태에서 묶어 오고, [onShellBack] 은 셸의 뒤로가기다(결정 13).
 * 내용 키는 기본값 그대로다 — 넓은 창 판정이 그 키로 알아본다(RC-14).
 */
fun connectDestinations(
    hasConnection: () -> Boolean,
    onShellBack: () -> Unit,
): ShellDestinations =
    { frame ->
        destination<NotesNavKey.Connect> {
            val modifier = frame.sceneEdgeModifier()
            ConnectRoute(
                place = ConnectPlace.Stacked,
                hasConnection = hasConnection(),
                drafts = frame.drafts,
                snackbarHostState = frame.snackbarHostState,
                onShellBack = onShellBack,
                modifier = modifier,
            )
        }
    }
