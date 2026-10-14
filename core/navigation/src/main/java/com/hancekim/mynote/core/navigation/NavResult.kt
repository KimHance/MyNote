package com.hancekim.mynote.core.navigation

enum class NavResult {
    Applied,
    Throttled,
    Ignored,

    /** 떠나기 지킴([LeaveGuard])이 막았다 — 스택은 그대로이고 지킴 쪽이 사용자에게 묻는다. 종료로 이어지지 않는다. */
    Guarded,
}
