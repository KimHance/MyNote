package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.common.throttle.Throttler
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun throttledProcess(
    minIntervalMillis: Long = Throttler.DEFAULT_MIN_INTERVAL_MILLIS,
    action: () -> Unit,
): () -> Unit {
    val latest by rememberUpdatedState(action)
    val throttler = remember(minIntervalMillis) { Throttler(minIntervalMillis) }
    return remember(throttler) { { throttler.process { latest() } } }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320)
@Composable
private fun ThrottledProcessPreview() {
    MyNoteTheme {
        val colors = MyNoteTheme.colors
        val shape = RoundedCornerShape(15.dp)
        var raw by remember { mutableIntStateOf(0) }
        var throttled by remember { mutableIntStateOf(0) }
        val onThrottled = throttledProcess { throttled++ }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(interactionSource = null, indication = null, role = Role.Button) {
                            raw++
                            onThrottled()
                        }.background(colors.accent, shape),
                contentAlignment = Alignment.Center,
            ) {
                Text("빠르게 눌러보기", style = MyNoteTheme.textStyles.body, color = colors.bg)
            }
            Text("누른 횟수 $raw · 통과 $throttled", style = MyNoteTheme.textStyles.body, color = colors.text2)
        }
    }
}
