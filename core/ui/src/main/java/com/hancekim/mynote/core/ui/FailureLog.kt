package com.hancekim.mynote.core.ui

import android.util.Log
import javax.inject.Inject

fun interface FailureLog {
    fun failed(
        tag: String,
        message: String,
        cause: Throwable,
    )
}

class AndroidFailureLog
    @Inject
    constructor() : FailureLog {
        override fun failed(
            tag: String,
            message: String,
            cause: Throwable,
        ) {
            Log.e(tag, message, cause)
        }
    }
