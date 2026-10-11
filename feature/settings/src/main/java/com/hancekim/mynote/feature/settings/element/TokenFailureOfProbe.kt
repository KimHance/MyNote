package com.hancekim.mynote.feature.settings.element

import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.ui.element.TokenFailureKind

// 연결 화면의 issueOfProbe(hadToken = true) 와 같은 갈래다 — 편집기에는 토큰이 늘 있어 토큰 요청 · 알림 상자 갈래가 토큰 실패 종류로 모인다
fun tokenFailureOfProbe(failure: ConnectProbeFailure): TokenFailureKind =
    when (failure) {
        ConnectProbeFailure.NeedsToken -> TokenFailureKind.Rejected
        ConnectProbeFailure.TokenRejected -> TokenFailureKind.Rejected
        ConnectProbeFailure.RepoNotVisible -> TokenFailureKind.NotVisible
        ConnectProbeFailure.ManifestInaccessible -> TokenFailureKind.NoList
        ConnectProbeFailure.RateLimited -> TokenFailureKind.Limit
        ConnectProbeFailure.NoManifest -> TokenFailureKind.NoList
        ConnectProbeFailure.MalformedManifest -> TokenFailureKind.BadList
        ConnectProbeFailure.Offline -> TokenFailureKind.Offline
        ConnectProbeFailure.Transient -> TokenFailureKind.Transient
    }
