package com.lonx.lyrico.screens.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.ArtistSortBy
import com.lonx.lyrico.data.model.ArtistSortInfo
import com.lonx.lyrico.ui.components.bar.AlphabetSideBar
import com.lonx.lyrico.ui.components.bar.findScrollIndex
import com.lonx.lyrico.ui.components.fab.ScrollToTopButton
import com.lonx.lyrico.ui.components.library.LibraryEmptyState
import com.lonx.lyrico.ui.components.search.ArtistSongItem
import com.lonx.lyrico.viewmodel.ArtistLibraryViewModel
import com.lonx.lyrico.viewmodel.SortOrder
import com.ramcosta.composedestinations.generated.destinations.ArtistDetailDestination
import com.ramcosta.composedestinations.generated.destinations.LocalSearchDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionMode
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val SECTIONS_ASC = listOf("0") + ('A'..'Z').map { it.toString() } + listOf("#")
private val SECTIONS_DESC = SECTIONS_ASC.asReversed()

@Composable
fun ArtistsPage(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier
) {
    val viewModel: ArtistLibraryViewModel = koinViewModel()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val sortInfo by viewModel.sortInfo.collectAsStateWithLifecycle()
    val showScrollTopButton by viewModel.showScrollTopButton.collectAsStateWithLifecycle()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val showFab by remember {
        derivedStateOf {
            showScrollTopButton && listState.firstVisibleItemIndex > 0
        }
    }
    val topPadding by animateDpAsState(
        targetValue = TopAppBarDefaults.SmallTopAppBarCenterHeight + 12.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "backToTopPadding"
    )
    val sections = remember(sortInfo.order) {
        if (sortInfo.order == SortOrder.ASC) SECTIONS_ASC else SECTIONS_DESC
    }
    val sectionIndexMap = remember(artists, sortInfo) {
        val map = mutableMapOf<String, Int>()
        if (sortInfo.sortBy.supportsIndex) {
            artists.forEachIndexed { index, artist ->
                if (!map.containsKey(artist.groupKey)) {
                    map[artist.groupKey] = index
                }
            }
        }
        map
    }
    val enableIndex = artists.isNotEmpty() && sortInfo.sortBy.supportsIndex
    val refreshTexts = listOf(
        stringResource(R.string.pull_to_refresh),
        stringResource(R.string.release_to_refresh),
        stringResource(R.string.refreshing),
        stringResource(R.string.refresh_success)
    )
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SmallTopAppBar(
                    title = stringResource(R.string.artist_list_title, artists.size),
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigate(SettingsDestination()) }) {
                            Icon(
                                imageVector = MiuixIcons.Settings,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.navigate(LocalSearchDestination) }) {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                        OverlayIconDropdownMenu(
                            entries = listOf(
                                artistSortDropdownEntry(sortInfo, viewModel::onSortChange),
                                scrollTopButtonDropdownEntry(
                                    showScrollTopButton = showScrollTopButton,
                                    onShowScrollTopButtonChange = viewModel::setScrollToTopButtonEnabled
                                )
                            )
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Sort,
                                contentDescription = stringResource(R.string.cd_sort)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        top = paddingValues.calculateTopPadding(),
                        end = paddingValues.calculateEndPadding(layoutDirection)
                    )
                    .fillMaxSize()
            ) {
                if (artists.isEmpty()) {
                    LibraryEmptyState(
                        title = stringResource(R.string.empty_artists_title),
                        summary = stringResource(R.string.empty_library_index_summary),
                        modifier = Modifier.align(Alignment.Center),
                        action = {
                            TextButton(
                                text = stringResource(R.string.refresh),
                                onClick = { viewModel.refreshSongs() },
                                colors = MiuixButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    )
                } else {
                    PullToRefresh(
                        isRefreshing = scanState.isScanning,
                        onRefresh = { viewModel.refreshSongs() },
                        modifier = Modifier.fillMaxSize(),
                        topAppBarScrollBehavior = topAppBarScrollBehavior,
                        refreshTexts = refreshTexts
                    ) {
                        LazyColumnScrollbar(
                            state = listState,
                            settings = ScrollbarSettings.Default.copy(
                                enabled = !enableIndex,
                                alwaysShowScrollbar = !enableIndex,
                                selectionMode = ScrollbarSelectionMode.Full,
                                thumbUnselectedColor = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                thumbSelectedColor = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .scrollEndHaptic()
                                    .overScrollVertical()
                                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                                    .fillMaxHeight(),
                                state = listState,
                                overscrollEffect = null,
                                contentPadding = PaddingValues()
                            ) {
                                items(
                                    items = artists,
                                    key = { it.id }
                                ) { artist ->
                                    ArtistSongItem(
                                        name = artist.name,
                                        subtitle = stringResource(
                                            R.string.album_song_count,
                                            artist.albumCount,
                                            artist.songCount
                                        ),
                                        coverUri = artist.coverSongUri,
                                        coverLastModified = artist.coverSongLastModified,
                                        onClick = {
                                            navigator.navigate(ArtistDetailDestination(artistId = artist.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (enableIndex) {
                        AlphabetSideBar(
                            sections = sections,
                            onSectionSelected = { section ->
                                val index = findScrollIndex(
                                    section = section,
                                    sectionIndexMap = sectionIndexMap,
                                    order = sortInfo.order
                                )
                                scope.launch { listState.scrollToItem(index) }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
        ScrollToTopButton(
            visible = showFab,
            topPadding = topPadding,
            text = stringResource(R.string.action_scroll_to_top),
            icon = painterResource(R.drawable.ic_arrow_up_24dp),
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        )
    }
}

@Composable
private fun artistSortDropdownEntry(
    sortInfo: ArtistSortInfo,
    onSortChange: (ArtistSortInfo) -> Unit
): DropdownEntry {
    return DropdownEntry(
        items = ArtistSortBy.entries.map { sortBy ->
            val isSelected = sortInfo.sortBy == sortBy
            DropdownItem(
                text = stringResource(sortBy.labelRes),
                selected = isSelected,
                summary = if (isSelected) {
                    stringResource(
                        if (sortInfo.order == SortOrder.ASC) {
                            R.string.sort_ascending
                        } else {
                            R.string.sort_descending
                        }
                    )
                } else {
                    null
                },
                onClick = {
                    onSortChange(
                        ArtistSortInfo(
                            sortBy = sortBy,
                            order = if (isSelected && sortInfo.order == SortOrder.ASC) {
                                SortOrder.DESC
                            } else {
                                SortOrder.ASC
                            }
                        )
                    )
                }
            )
        }
    )
}

@Composable
private fun scrollTopButtonDropdownEntry(
    showScrollTopButton: Boolean,
    onShowScrollTopButtonChange: (Boolean) -> Unit
): DropdownEntry {
    return DropdownEntry(
        items = listOf(
            DropdownItem(
                text = stringResource(R.string.show_scroll_top_button),
                selected = showScrollTopButton,
                onClick = { onShowScrollTopButtonChange(!showScrollTopButton) }
            )
        )
    )
}
