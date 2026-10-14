package com.hancekim.mynote.feature.editor

import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.frame.ShellDestinations
import com.hancekim.mynote.core.ui.frame.sceneEdgeModifier
import com.hancekim.mynote.feature.editor.screen.EditorScreen
import com.hancekim.mynote.feature.editor.screen.PasteScreen

/**
 * 편집기 기능의 목적지 — 편집기 · 붙여넣기(local-notes). 둘 다 자기 상단 바를 그리고 셸 크롬(상단 바 · 플로팅 내비 · 알약)은
 * 셸이 숨긴다. 넓은 창에서는 상세 칸에 든다(디자인 C8). 상태는 셸이 Activity owner ViewModel 에서 묶어 넘기고,
 * [onShellBack] 은 셸의 뒤로가기다(붙여넣기 닫기 — 결정 13).
 */
fun editorDestinations(
    editor: () -> EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onShellBack: () -> Unit,
): ShellDestinations =
    { frame ->
        destination<NotesNavKey.Editor> { key ->
            EditorScreen(
                key = key,
                state = editor(),
                onIntent = onIntent,
                modifier = frame.sceneEdgeModifier(),
            )
        }
        destination<NotesNavKey.Paste> {
            PasteScreen(
                text = editor().pasteText,
                onIntent = onIntent,
                onClose = onShellBack,
                modifier = frame.sceneEdgeModifier(),
            )
        }
    }
