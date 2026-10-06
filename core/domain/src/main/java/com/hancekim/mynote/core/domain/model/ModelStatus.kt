package com.hancekim.mynote.core.domain.model

sealed interface ModelStatus {
    data object Missing : ModelStatus

    data class Paused(val received: Long, val total: Long) : ModelStatus

    data object WaitingForWifi : ModelStatus

    data object Preparing : ModelStatus

    data class Downloading(val received: Long, val total: Long) : ModelStatus

    data class Retrying(val received: Long, val total: Long) : ModelStatus

    data object Verifying : ModelStatus

    data object Ready : ModelStatus

    data class Failed(val reason: String) : ModelStatus
}
