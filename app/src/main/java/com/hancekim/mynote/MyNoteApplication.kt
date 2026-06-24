package com.hancekim.mynote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyNoteApplication : Application() {
    // 주입만으로 TokenSync 가 생성되어 토큰 → 홀더 동기화 collector 가 시작된다.
    @Inject lateinit var tokenSync: TokenSync
}
