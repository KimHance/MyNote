package com.hancekim.mynote.feature.notes.screen.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.connect.KeepWordsText
import com.hancekim.mynote.core.ui.screen.connect.ProgressRing
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.core.ui.R as UiR

@Composable
fun ConnectReadingScreen(modifier: Modifier = Modifier) {
    val colors = MyNoteTheme.colors
    Surface(modifier = modifier.fillMaxSize(), color = colors.bg) {
        Box(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KeepWordsText(
                    text = stringResource(UiR.string.ui_app_title),
                    style = MyNoteTheme.textStyles.sectionTitle,
                    color = colors.text,
                )
                ProgressRing()
                KeepWordsText(
                    text = stringResource(R.string.notes_connect_reading),
                    style = MyNoteTheme.textStyles.caption,
                    color = colors.text3,
                )
            }
        }
    }
}

@Preview(name = "연결 · 읽는 중", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 800)
@Composable
private fun ConnectReadingPreview() {
    MyNoteTheme { ConnectReadingScreen() }
}
