package com.lonx.lyrico.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.SettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
@OptIn(ExperimentalLayoutApi::class)
@Destination<RootGraph>(route = "lyrics_cleanup_rules")
fun LyricsCleanupRulesScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    var addingRule by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<String?>(null) }
    var deletingRule by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val invalidOrDuplicateRule = stringResource(R.string.invalid_or_duplicate_rule)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.non_lyrics_cleanup_rules_title),
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .fillMaxHeight(),
            contentPadding = scaffoldContentPadding(
                paddingValues = paddingValues,
                bottomExtra = 12.dp
            ),
            overscrollEffect = null
        ) {
            item(key = "rules") {
                SmallTitle(text = stringResource(R.string.non_lyrics_cleanup_rules_section))
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ChipGrid(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        uiState.lyricsTagLineKeywords.forEach { rule ->
                            CleanupRuleChip(
                                text = rule,
                                onClick = { editingRule = rule },
                                onDelete = { deletingRule = rule }
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.non_lyrics_cleanup_rules_hint),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            text = stringResource(R.string.add_non_lyrics_cleanup_rule),
                            onClick = {
                                errorMessage = null
                                addingRule = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                        TextButton(
                            text = stringResource(R.string.reset_to_default),
                            onClick = viewModel::resetNonLyricsContentRules,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    CleanupRuleInputDialog(
        show = addingRule,
        title = stringResource(R.string.add_non_lyrics_cleanup_rule),
        initialValue = "",
        errorMessage = errorMessage,
        onDismiss = {
            addingRule = false
            errorMessage = null
        },
        onSave = { value ->
            if (viewModel.addNonLyricsContentRule(value)) {
                addingRule = false
                errorMessage = null
            } else {
                errorMessage = invalidOrDuplicateRule
            }
        }
    )

    CleanupRuleInputDialog(
        show = editingRule != null,
        title = stringResource(R.string.edit_non_lyrics_cleanup_rule),
        initialValue = editingRule.orEmpty(),
        errorMessage = errorMessage,
        onDismiss = {
            editingRule = null
            errorMessage = null
        },
        onSave = { value ->
            val oldRule = editingRule ?: return@CleanupRuleInputDialog
            if (viewModel.updateNonLyricsContentRule(oldRule, value)) {
                editingRule = null
                errorMessage = null
            } else {
                errorMessage = invalidOrDuplicateRule
            }
        }
    )

    WindowDialog(
        show = deletingRule != null,
        title = stringResource(R.string.non_lyrics_cleanup_delete_rule_title),
        onDismissRequest = { deletingRule = null }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    R.string.non_lyrics_cleanup_delete_rule_message,
                    deletingRule.orEmpty()
                ),
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { deletingRule = null },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = stringResource(R.string.common_delete),
                    onClick = {
                        deletingRule?.let(viewModel::removeNonLyricsContentRule)
                        deletingRule = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun CleanupRuleChip(
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MiuixTheme.colorScheme.primary.copy(alpha = 0.48f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(end = 8.dp),
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = MiuixIcons.Delete,
            contentDescription = stringResource(R.string.common_delete),
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
        )
    }
}

@Composable
private fun CleanupRuleInputDialog(
    show: Boolean,
    title: String,
    initialValue: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(show, initialValue) { mutableStateOf(initialValue) }

    WindowDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = { text = it },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage ?: stringResource(R.string.non_lyrics_cleanup_rule_input_hint),
                color = if (errorMessage == null) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MiuixTheme.colorScheme.error
                },
                fontSize = MiuixTheme.textStyles.footnote1.fontSize
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = { onSave(text) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
