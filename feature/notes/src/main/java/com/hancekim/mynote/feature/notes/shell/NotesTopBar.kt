package com.hancekim.mynote.feature.notes.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.hancekim.mynote.core.designsystem.R as DsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesTopBar(
    title: String,
    showBack: Boolean,
    onMenu: () -> Unit,
    onBack: () -> Unit,
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
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(DsR.string.ds_action_back),
                        tint = colors.text,
                    )
                }
            } else {
                IconButton(onClick = onMenu) {
                    Text(text = "☰", style = MyNoteTheme.textStyles.sectionTitle, color = colors.text)
                }
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
