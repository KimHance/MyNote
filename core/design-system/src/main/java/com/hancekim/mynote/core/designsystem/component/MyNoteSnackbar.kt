package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun MyNoteSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        MyNoteSnackbar(data)
    }
}

@Composable
fun MyNoteSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Snackbar(
        snackbarData = data,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(SNACKBAR_RADIUS),
        containerColor = colors.surface2,
        contentColor = colors.text,
        actionColor = colors.accent,
        actionContentColor = colors.accent,
        dismissActionContentColor = colors.text3,
    )
}

private val SNACKBAR_RADIUS = 20.dp

private class SnackbarPreviewProvider : PreviewParameterProvider<String?> {
    override val values = sequenceOf(null, "되돌리기")
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 390)
@Composable
private fun MyNoteSnackbarPreview(
    @PreviewParameter(SnackbarPreviewProvider::class) action: String?,
) {
    MyNoteTheme {
        Box(Modifier.padding(vertical = 12.dp)) {
            MyNoteSnackbar(PreviewSnackbarData(message = "퀴즈를 그만뒀습니다", action = action))
        }
    }
}

private class PreviewSnackbarData(
    message: String,
    action: String?,
) : SnackbarData {
    override val visuals: SnackbarVisuals =
        object : SnackbarVisuals {
            override val actionLabel = action
            override val duration = androidx.compose.material3.SnackbarDuration.Indefinite
            override val message = message
            override val withDismissAction = false
        }

    override fun dismiss() = Unit

    override fun performAction() = Unit
}
