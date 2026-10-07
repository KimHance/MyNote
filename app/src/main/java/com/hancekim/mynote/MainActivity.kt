package com.hancekim.mynote

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hancekim.mynote.core.ai.download.ModelDownloads
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.NotesRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var navigator: Navigator

    @Inject lateinit var modelDownloads: ModelDownloads

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { it.fadeOutAfterIcon() }
        consumeSettingsRequest(intent)
        setContent {
            NotesRoute()
        }
    }

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
