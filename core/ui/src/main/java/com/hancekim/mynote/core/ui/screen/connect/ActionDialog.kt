package com.hancekim.mynote.core.ui.screen.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.GlassSurface
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.core.ui.screen.SolidDestructiveButton
import com.hancekim.mynote.core.designsystem.R as DsR

internal val CONNECT_DIALOG_MAX_WIDTH: Dp = 420.dp

private val DIALOG_RADIUS: Dp = 26.dp

private val DIALOG_MARGIN: Dp = 24.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionDialog(
    title: String,
    body: String,
    confirmLabel: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(DIALOG_MARGIN),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            radius = DIALOG_RADIUS,
            backdrop = null,
            modifier = Modifier.widthIn(max = CONNECT_DIALOG_MAX_WIDTH).fillMaxWidth(),
            tint = colors.glassPanel,
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KeepWordsText(
                        text = title,
                        style = MyNoteTheme.textStyles.sectionTitle,
                        color = colors.text,
                        modifier = Modifier.semantics { heading() },
                    )
                    KeepWordsText(
                        text = body,
                        style = MyNoteTheme.textStyles.bodySmall,
                        color = colors.bodyText,
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SecondaryButton(text = stringResource(DsR.string.ds_action_cancel), onClick = onDismiss)
                    if (destructive) {
                        SolidDestructiveButton(text = confirmLabel, onClick = onConfirm)
                    } else {
                        PrimaryButton(text = confirmLabel, onClick = onConfirm)
                    }
                }
            }
        }
    }
}
