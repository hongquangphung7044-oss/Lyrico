package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.ExtraMetadataTarget
import com.lonx.lyrico.data.model.ExtraMetadataWriteRule
import com.lonx.lyrico.data.model.ExtraWriteMode
import com.lonx.lyrico.viewmodel.SettingsViewModel
import com.lonx.lyrics.model.Source
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
@Destination<RootGraph>(route = "extra_metadata_writes")
fun ExtraMetadataWritesScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val rules = uiState.extraMetadataWriteRules
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.extra_metadata_writes_title),
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
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
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 12.dp
            ),
            overscrollEffect = null
        ) {
            item("intro") {
                Text(
                    text = stringResource(R.string.extra_metadata_writes_summary),
                    fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Source.entries.forEach { source ->
                val sourceRules = rules.filter { it.source == source }
                if (sourceRules.isNotEmpty()) {
                    item("title_${source.name}") {
                        SmallTitle(text = stringResource(source.labelRes))
                    }
                    item("card_${source.name}") {
                        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                            sourceRules.forEach { rule ->
                                ExtraMetadataRuleItem(
                                    rule = rule,
                                    onRuleChanged = { updatedRule ->
                                        viewModel.setExtraMetadataWriteRules(
                                            rules.map {
                                                if (it.key == updatedRule.key && it.source == updatedRule.source) {
                                                    updatedRule
                                                } else {
                                                    it
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraMetadataRuleItem(
    rule: ExtraMetadataWriteRule,
    onRuleChanged: (ExtraMetadataWriteRule) -> Unit
) {
    val modeItems = ExtraWriteMode.entries.map { stringResource(it.labelRes) }
    val selectedModeIndex = ExtraWriteMode.entries.indexOf(rule.mode).coerceAtLeast(0)
    WindowDropdownPreference(
        title = stringResource(rule.key.labelRes),
        summary = buildString {
            append(stringResource(rule.key.summaryRes))
            if (rule.target == ExtraMetadataTarget.COMMENT) {
                append("\n")
                append(stringResource(R.string.extra_metadata_comment_warning))
            }
        },
        items = modeItems,
        selectedIndex = selectedModeIndex,
        onSelectedIndexChange = { index ->
            onRuleChanged(rule.copy(mode = ExtraWriteMode.entries[index]))
        }
    )
}
