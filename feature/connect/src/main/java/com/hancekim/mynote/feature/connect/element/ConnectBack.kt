package com.hancekim.mynote.feature.connect.element

enum class ConnectStep { Editing, Probing, Syncing, Done }

enum class ConnectBackAction { PopScreen, CancelProbe, Ignore }

/** 쌓은 연결 화면의 뒤로 — 확인 중이면 요청 취소, 첫 동기화 중이면 무시, 그 밖은 이전 화면(RC-24 · RC-1). */
fun connectBackAction(step: ConnectStep): ConnectBackAction =
    when (step) {
        ConnectStep.Probing -> ConnectBackAction.CancelProbe
        ConnectStep.Syncing, ConnectStep.Done -> ConnectBackAction.Ignore
        ConnectStep.Editing -> ConnectBackAction.PopScreen
    }
