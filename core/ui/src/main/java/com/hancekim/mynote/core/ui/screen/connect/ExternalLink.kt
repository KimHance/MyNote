package com.hancekim.mynote.core.ui.screen.connect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.net.toUri

const val TOKEN_PAGE_URL = "https://github.com/settings/personal-access-tokens/new"

private const val CHROME_PACKAGE = "com.android.chrome"
private const val RESOLVER_PACKAGE = "android"
private const val BROWSER_PROBE_URL = "https://example.com"

/** [url] 을 브라우저 앱으로 연다. 주소를 맡겠다고 한 다른 앱은 거치지 않는다. 열 앱이 없으면 false. */
fun openExternal(
    context: Context,
    url: String,
): Boolean {
    val view =
        Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browser = pickBrowser(context.defaultBrowserPackage(), context.browserPackages())
    if (browser != null && context.tryStart(Intent(view).setPackage(browser))) return true
    return context.tryStart(view)
}

/** 기본 브라우저 · Chrome · 처음 찾은 브라우저 순으로 고른다. 고를 것이 없으면 null. */
internal fun pickBrowser(
    defaultPackage: String?,
    installed: List<String>,
): String? =
    when {
        defaultPackage != null && defaultPackage != RESOLVER_PACKAGE -> defaultPackage
        CHROME_PACKAGE in installed -> CHROME_PACKAGE
        else -> installed.firstOrNull()
    }

private fun Context.tryStart(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

private fun browserProbe(): Intent =
    Intent(Intent.ACTION_VIEW, BROWSER_PROBE_URL.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)

private fun Context.defaultBrowserPackage(): String? =
    resolveActivity(browserProbe(), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName

private fun Context.browserPackages(): List<String> =
    queryActivities(browserProbe(), PackageManager.MATCH_ALL).map { it.activityInfo.packageName }.distinct()

@Suppress("DEPRECATION")
private fun Context.resolveActivity(
    intent: Intent,
    flags: Int,
): ResolveInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        packageManager.resolveActivity(intent, flags)
    }

@Suppress("DEPRECATION")
private fun Context.queryActivities(
    intent: Intent,
    flags: Int,
): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        packageManager.queryIntentActivities(intent, flags)
    }
