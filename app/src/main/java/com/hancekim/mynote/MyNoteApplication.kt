package com.hancekim.mynote

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hancekim.mynote.download.ModelWaitNotice
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyNoteApplication : Application(), Configuration.Provider {
    @Inject lateinit var tokenSync: TokenSync

    @Inject lateinit var modelWaitNotice: ModelWaitNotice

    @Inject lateinit var studyLogMaintenance: StudyLogMaintenance

    @Inject lateinit var insightScheduleSync: InsightScheduleSync

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        tokenSync.start()
        modelWaitNotice.start()
        studyLogMaintenance.start()
        insightScheduleSync.start()
    }
}
