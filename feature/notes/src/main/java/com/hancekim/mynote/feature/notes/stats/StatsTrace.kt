package com.hancekim.mynote.feature.notes.stats

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StatsTrace
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val enabled = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        fun started(period: StatsPeriod) {
            if (enabled) Log.d(TAG, "aggregate start period=$period")
        }

        fun computed(
            period: StatsPeriod,
            tookMillis: Long,
        ) {
            if (enabled) Log.d(TAG, "aggregate done period=$period took=${tookMillis}ms")
        }

        fun failed(cause: Throwable) {
            Log.e(TAG, "aggregate failed", cause)
        }

        private companion object {
            const val TAG = "MyNoteStats"
        }
    }
