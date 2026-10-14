package com.hancekim.mynote.feature.connect.element

enum class NoticeWays { None, CreateAndGuide }

/** 연결 화면의 구역. */
enum class ConnectSection { Link, Entry }

fun entryVisible(
    step: ConnectStep,
    tokenPanelOpen: Boolean,
    notice: ConnectNotice?,
    templateAvailable: Boolean,
): Boolean = templateAvailable && step == ConnectStep.Editing && !tokenPanelOpen && notice == null

/**
 * 연결 화면의 구역 순서 — 링크 구역이 첫째, 입구 구역은 그 아래 한 단이다(RC-25). 연결 화면은 늘 쌓은 자리라
 * (홈 첫 실행 빈 상태 · 홈 안내 · 설정 — `local-notes` 결정 4) 여는 길 · 연결 유무 · 창 크기와 무관하게 같은 모양이다.
 */
fun connectSectionsOf(entryShown: Boolean): List<ConnectSection> =
    if (entryShown) listOf(ConnectSection.Link, ConnectSection.Entry) else listOf(ConnectSection.Link)

fun noticeWays(
    notice: ConnectNotice,
    templateAvailable: Boolean,
): NoticeWays =
    if (templateAvailable && notice == ConnectNotice.NoList) NoticeWays.CreateAndGuide else NoticeWays.None
