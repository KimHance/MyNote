package com.hancekim.mynote.feature.connect.element

enum class ConnectStep { Editing, Probing, Syncing, Done }

enum class ConnectPlace { Root, Stacked }

enum class ConnectBackAction { ShowExitHint, PopScreen, CancelProbe, Ignore }

fun connectBackAction(
    place: ConnectPlace,
    step: ConnectStep,
): ConnectBackAction =
    when (step) {
        ConnectStep.Probing -> {
            ConnectBackAction.CancelProbe
        }

        ConnectStep.Syncing, ConnectStep.Done -> {
            ConnectBackAction.Ignore
        }

        ConnectStep.Editing -> {
            if (place == ConnectPlace.Root) ConnectBackAction.ShowExitHint else ConnectBackAction.PopScreen
        }
    }
