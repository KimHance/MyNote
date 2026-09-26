package com.hancekim.mynote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyNoteApplication : Application() {
    @Inject lateinit var tokenSync: TokenSync

    override fun onCreate() {
        super.onCreate()
        tokenSync.start()
    }
}
