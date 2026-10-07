package com.hancekim.mynote

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.lifecycleScope
import com.hancekim.mynote.core.ai.download.ModelDownloads
import com.hancekim.mynote.core.designsystem.font.TitleFontGate
import com.hancekim.mynote.core.designsystem.font.awaitFirstFrameAndLamp
import com.hancekim.mynote.core.designsystem.font.googleTitleFontSource
import com.hancekim.mynote.core.designsystem.font.titleFontWaitMillis
import com.hancekim.mynote.core.designsystem.theme.ProvideTitleFontFamily
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.NotesRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var navigator: Navigator

    @Inject lateinit var modelDownloads: ModelDownloads

    private var titleFontFamily by mutableStateOf(titleFontGate.fontFamily)

    private var titleFontApplied = false

    private val firstFrameReady = CompletableDeferred<Unit>()

    private var createdInTask = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        if (titleFontFamily == null) decideTitleFont(createdMillis = SystemClock.uptimeMillis())
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        createdInTask = taskId
        if (liveTasks.enter(createdInTask)) {
            splashScreen.setOnExitAnimationListener { it.fadeOutAfterIcon() }
        }
        consumeSettingsRequest(intent)
        holdFirstFrameUntilTitleFont()
        setContent {
            val fontFamily = titleFontFamily
            ProvideTitleFontFamily(fontFamily ?: FontFamily.Default) {
                NotesRoute()
            }
            if (fontFamily != null) SideEffect { titleFontApplied = true }
        }
    }

    override fun onDestroy() {
        liveTasks.leave(createdInTask)
        super.onDestroy()
    }

    private fun decideTitleFont(createdMillis: Long) {
        val source = googleTitleFontSource(this, BuildConfig.TITLE_FONT_NAME.ifEmpty { null })
        val lampMillis = (splashAnimationMillis() * animatorScale()).toLong()
        val giveUp: suspend () -> Unit = {
            awaitFirstFrameAndLamp(firstFrameReady::await) {
                titleFontWaitMillis(processStartMillis(), createdMillis, SystemClock.uptimeMillis(), lampMillis)
            }
        }
        lifecycleScope.launch {
            titleFontFamily = withContext(Dispatchers.Default) { titleFontGate.decide(source, giveUp) }
        }
    }

    private fun holdFirstFrameUntilTitleFont() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!titleFontApplied) {
                        firstFrameReady.complete(Unit)
                        return false
                    }
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }

    // 33+ 는 시스템이 이 값 대신 AVD 길이를 읽지만 둘은 같게 둔다.
    @Suppress("DEPRECATION")
    private fun splashAnimationMillis(): Int =
        theme
            .obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenAnimationDuration))
            .use { it.getInt(0, 0) }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSettingsRequest(intent)
    }

    private fun consumeSettingsRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) != true) return
        intent.removeExtra(EXTRA_OPEN_SETTINGS)
        navigator.dispatchImmediately(NavCommand.Push(NotesNavKey.Settings))
        if (intent.getBooleanExtra(EXTRA_RETRY_DOWNLOAD, false)) {
            intent.removeExtra(EXTRA_RETRY_DOWNLOAD)
            modelDownloads.start()
        }
    }

    companion object {
        private val titleFontGate = TitleFontGate()

        private val liveTasks = LiveTasks()

        const val EXTRA_OPEN_SETTINGS = "open_settings"

        const val EXTRA_RETRY_DOWNLOAD = "retry_download"

        private const val ACTION_OPEN_SETTINGS = "com.hancekim.mynote.action.OPEN_SETTINGS"

        private const val ACTION_RETRY_DOWNLOAD = "com.hancekim.mynote.action.RETRY_DOWNLOAD"

        fun settingsIntent(context: Context): Intent = openSettings(context, ACTION_OPEN_SETTINGS)

        fun retryIntent(context: Context): Intent =
            openSettings(context, ACTION_RETRY_DOWNLOAD).putExtra(EXTRA_RETRY_DOWNLOAD, true)

        private fun openSettings(context: Context, action: String): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ).putExtra(EXTRA_OPEN_SETTINGS, true)
    }
}

private fun processStartMillis(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Process.getStartRequestedUptimeMillis()
    } else {
        Process.getStartUptimeMillis()
    }
