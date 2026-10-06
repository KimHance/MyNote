package com.hancekim.mynote.feature.notes.screen.connect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

internal const val TOKEN_PAGE_URL = "https://github.com/settings/personal-access-tokens/new"

internal fun openExternal(
    context: Context,
    url: String,
): Boolean {
    val intent =
        Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
