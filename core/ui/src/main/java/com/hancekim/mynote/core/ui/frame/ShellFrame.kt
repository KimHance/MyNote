package com.hancekim.mynote.core.ui.frame

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.hancekim.mynote.core.navigation.registry.DestinationRegistration
import com.hancekim.mynote.core.navigation.scene.rememberSceneEdgeProgress
import com.hancekim.mynote.core.navigation.scene.sceneEdge
import com.hancekim.mynote.core.ui.element.NotesDrafts
import com.hancekim.mynote.core.ui.shell.NotesScrollState

/**
 * 셸 틀 계약 — 등록된 목적지 화면이 셸에게서 받는 값은 이것뿐이다(FM-4).
 *
 * 값은 화면이 읽는 순간에 구독된다. 알약 예약 여백처럼 애니메이션으로 바뀌는 값도 읽는 화면만 다시 그린다.
 * 홀더(스크롤 · 원문 펼침 · draft)는 셸이 창 등급 분기 위에 들고 있어 접기 · 펴기에 남는다.
 */
@Stable
interface ShellFrame {
    /** 위 여백 — 모델 알약 예약. */
    val contentTopPadding: Dp

    /** 아래 여백 — 플로팅 바 예약 · 내비 막대. */
    val contentBottomPadding: Dp

    /** 끝 여백 — 플로팅 레일 예약. */
    val contentEndPadding: Dp

    /** 내용이 두 단인가 — 펴진 고정 패널을 빼고 셸이 잰다. */
    val twoPane: Boolean

    /** 씬이 쌓이는가 — 좁은 창에서만 씬 가장자리를 그린다. */
    val stackedScenes: Boolean

    val scroll: NotesScrollState

    val drafts: NotesDrafts

    val snackbarHostState: SnackbarHostState
}

/** 기능 등록 파일이 돌려주는 꼴. */
typealias ShellDestinations = DestinationRegistration<ShellFrame>

/**
 * 쌓이는 씬이면 씬 가장자리(모서리 · 그림자)를 건다.
 *
 * 쌓이는 씬인지는 틀이 안다 — Modifier 의 확장으로 두면 등록 화면마다 틀 값을 꺼내 넘겨야 한다.
 */
@Suppress("ModifierFactoryExtensionFunction")
@Composable
fun ShellFrame.sceneEdgeModifier(): Modifier {
    val edgeProgress = rememberSceneEdgeProgress()
    return if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier
}
