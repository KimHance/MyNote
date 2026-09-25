package com.hancekim.mynote.feature.notes.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesTopBar(
    title: String,
    onMenu: () -> Unit,
    onToggleFont: () -> Unit,
    onTokenDialog: () -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MyNoteTheme.textStyles.sectionTitle,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        expandedHeight = MyNoteTheme.adaptive.headerHeight,
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Text(text = "☰", style = MyNoteTheme.textStyles.sectionTitle, color = colors.text)
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.notes_action_refresh),
                    tint = colors.accent,
                )
            }
            IconButton(onClick = onTokenDialog) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.notes_action_token),
                    tint = colors.accent,
                )
            }
            IconButton(onClick = onToggleFont) {
                Text(
                    text = stringResource(R.string.notes_action_font_scale),
                    style = MyNoteTheme.textStyles.treeLabel,
                    color = colors.accent,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.bg,
                titleContentColor = colors.text,
                navigationIconContentColor = colors.text,
                actionIconContentColor = colors.accent,
            ),
    )
}
