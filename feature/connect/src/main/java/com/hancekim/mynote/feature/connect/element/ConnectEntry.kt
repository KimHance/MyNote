package com.hancekim.mynote.feature.connect.element

enum class EntryEmphasis { Equal, LinkFirst }

enum class NoticeWays { None, CreateAndGuide }

const val TWO_COLUMN_MIN_WIDTH_DP = 640f

fun entryVisible(
    step: ConnectStep,
    tokenPanelOpen: Boolean,
    notice: ConnectNotice?,
    templateAvailable: Boolean,
): Boolean = templateAvailable && step == ConnectStep.Editing && !tokenPanelOpen && notice == null

fun entryEmphasis(
    place: ConnectPlace,
    hasConnection: Boolean,
): EntryEmphasis = if (place == ConnectPlace.Root && !hasConnection) EntryEmphasis.Equal else EntryEmphasis.LinkFirst

fun twoColumnsOf(
    emphasis: EntryEmphasis,
    shortHeight: Boolean,
    contentWidthDp: Float,
    entryShown: Boolean,
): Boolean =
    emphasis == EntryEmphasis.Equal && shortHeight && contentWidthDp >= TWO_COLUMN_MIN_WIDTH_DP && entryShown

fun noticeWays(
    notice: ConnectNotice,
    templateAvailable: Boolean,
): NoticeWays =
    if (templateAvailable && notice == ConnectNotice.NoList) NoticeWays.CreateAndGuide else NoticeWays.None
