package com.lonx.lyrico.screens.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import com.lonx.lyrico.data.model.AlbumSortBy
import com.lonx.lyrico.data.model.AlbumSortInfo
import com.lonx.lyrico.ui.components.bar.AlphabetSideBar
import com.lonx.lyrico.ui.components.bar.findScrollIndex
import com.lonx.lyrico.ui.components.fab.ScrollToTopButton
import com.lonx.lyrico.ui.components.library.AlbumGridItem
import com.lonx.lyrico.ui.components.library.LibraryEmptyState
import com.lonx.lyrico.ui.components.library.rememberAlbumGridTextStyle
import com.lonx.lyrico.ui.components.search.AlbumSongItem
import com.lonx.lyrico.viewmodel.AlbumLibraryViewModel
import com.lonx.lyrico.viewmodel.SortOrder
import com.ramcosta.composedestinations.generated.destinations.AlbumDetailDestination
import com.ramcosta.composedestinations.generated.destinations.LocalSearchDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
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
fun AlbumsPage(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier
) {
    val viewModel: AlbumLibraryViewModel = koinViewModel()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val sortInfo by viewModel.sortInfo.collectAsStateWithLifecycle()
    val showScrollTopButton by viewModel.showScrollTopButton.collectAsStateWithLifecycle()
    val albumGridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val albumTextStyle = rememberAlbumGridTextStyle(albumGridColumns)
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val showFab by remember {
        derivedStateOf {
            showScrollTopButton && gridState.firstVisibleItemIndex > 0
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
    val sectionIndexMap = remember(albums, sortInfo) {
        val map = mutableMapOf<String, Int>()
        if (sortInfo.sortBy.supportsIndex) {
            albums.forEachIndexed { index, album ->
                if (!map.containsKey(album.groupKey)) {
                    map[album.groupKey] = index
                }
            }
        }
        map
    }
    val enableIndex = albums.isNotEmpty() && sortInfo.sortBy.supportsIndex
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
                    title = stringResource(R.string.album_list_title, albums.size),
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
                                albumGridColumnsDropdownEntry(
                                    columns = albumGridColumns,
                                    onColumnsChange = viewModel::setGridColumns
                                ),
                                albumSortDropdownEntry(sortInfo, viewModel::onSortChange),
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
                if (albums.isEmpty()) {
                    LibraryEmptyState(
                        title = stringResource(R.string.empty_albums_title),
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
                        LazyVerticalGridScrollbar(
                            state = gridState,
                            settings = ScrollbarSettings.Default.copy(
                                enabled = !enableIndex,
                                alwaysShowScrollbar = !enableIndex,
                                selectionMode = ScrollbarSelectionMode.Full,
                                thumbUnselectedColor = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                thumbSelectedColor = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(albumGridColumns),
                                state = gridState,
                                modifier = Modifier
                                    .scrollEndHaptic()
                                    .overScrollVertical()
                                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                                    .fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = paddingValues.calculateStartPadding(layoutDirection) + 12.dp,
                                    top = 12.dp,
                                    end = paddingValues.calculateEndPadding(layoutDirection) + 12.dp,
                                    bottom = 12.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                overscrollEffect = null
                            ) {
                                items(
                                    items = albums,
                                    key = { it.id }
                                ) { album ->
                                    AlbumGridItem(
                                        albumName = album.name,
                                        summary = buildAlbumSummary(
                                            songCountText = stringResource(R.string.song_count, album.songCount),
                                            year = album.year
                                        ),
                                        coverUri = album.coverSongUri,
                                        coverLastModified = album.coverSongLastModified,
                                        titleStyle = albumTextStyle.title,
                                        summaryStyle = albumTextStyle.summary,
                                        titleMaxLines = albumTextStyle.titleMaxLines,
                                        onClick = {
                                            navigator.navigate(AlbumDetailDestination(albumId = album.id))
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
                                scope.launch {
                                    gridState.scrollToItem(index)
                                }
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
                    gridState.animateScrollToItem(0)
                }
            }
        )
    }
}
private fun buildAlbumSummary(
    songCountText: String,
    year: String?
): String {
    return listOfNotNull(
        songCountText,
        year?.takeIf { it.length == 4 && it.all(Char::isDigit) }
    ).joinToString(" ")
}
@Composable
private fun albumGridColumnsDropdownEntry(
    columns: Int,
    onColumnsChange: (Int) -> Unit
): DropdownEntry {
    return DropdownEntry(
        items = listOf(2, 3, 4).map { count ->
            DropdownItem(
                text = stringResource(R.string.album_grid_columns_format, count),
                selected = columns == count,
                onClick = { onColumnsChange(count) }
            )
        }
    )
}
@Composable
private fun albumSortDropdownEntry(
    sortInfo: AlbumSortInfo,
    onSortChange: (AlbumSortInfo) -> Unit
): DropdownEntry {
    return DropdownEntry(
        items = AlbumSortBy.entries.map { sortBy ->
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
                        AlbumSortInfo(
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
