package com.lonx.lyrico.ui.components.song

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.LibraryEmptyState
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults

@Composable
fun SongListEmptyState(
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    LibraryEmptyState(
        title = stringResource(R.string.song_list_empty_title),
        summary = stringResource(R.string.song_list_empty_desc),
        modifier = modifier,
        action = {
            top.yukonga.miuix.kmp.basic.TextButton(
                text = stringResource(R.string.action_add_folder),
                onClick = onAddFolder,
                colors = MiuixButtonDefaults.textButtonColorsPrimary()
            )
        }
    )
}
