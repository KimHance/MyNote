package com.hancekim.mynote.feature.stats

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface StatsTrace {
    fun started(period: StatsPeriod)

    fun computed(
        period: StatsPeriod,
        tookMillis: Long,
    )

    fun failed(cause: Throwable)
}

class LogStatsTrace
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : StatsTrace {
        private val enabled = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        override fun started(period: StatsPeriod) {
            if (enabled) Log.d(TAG, "aggregate start period=$period")
        }

        override fun computed(
            period: StatsPeriod,
            tookMillis: Long,
        ) {
            if (enabled) Log.d(TAG, "aggregate done period=$period took=${tookMillis}ms")
        }

        override fun failed(cause: Throwable) {
            Log.e(TAG, "aggregate failed", cause)
        }

        private companion object {
            const val TAG = "MyNoteStats"
        }
    }
