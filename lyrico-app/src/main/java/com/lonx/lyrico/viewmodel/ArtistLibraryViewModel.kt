package com.lonx.lyrico.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.data.model.ArtistSortBy
import com.lonx.lyrico.data.model.ArtistSortInfo
import com.lonx.lyrico.data.model.dao.ArtistListItem
import com.lonx.lyrico.data.repository.LibraryIndexRepository
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.utils.LibraryScanManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArtistLibraryViewModel(
    private val libraryIndexRepository: LibraryIndexRepository,
    private val libraryScanManager: LibraryScanManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val sortInfo: StateFlow<ArtistSortInfo> = settingsRepository.artistSortInfo
        .stateIn(viewModelScope, SharingStarted.Eagerly, ArtistSortInfo())

    val showScrollTopButton = settingsRepository.showScrollTopButton
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val scanState = libraryScanManager.state
    val artists: StateFlow<List<ArtistListItem>> =
        combine(libraryIndexRepository.observeArtists(), sortInfo) { artists, sort ->
            val sorted = when (sort.sortBy) {
                ArtistSortBy.NAME -> artists.sortedWith(
                    compareBy<ArtistListItem> { it.sortKey }.thenBy { it.name }
                )
                ArtistSortBy.SONG_COUNT -> artists.sortedWith(
                    compareByDescending<ArtistListItem> { it.songCount }.thenBy { it.sortKey }.thenBy { it.name }
                )
                ArtistSortBy.ALBUM_COUNT -> artists.sortedWith(
                    compareByDescending<ArtistListItem> { it.albumCount }.thenBy { it.sortKey }.thenBy { it.name }
                )
            }
            if (sort.order == SortOrder.ASC) sorted else sorted.asReversed()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun onSortChange(sortInfo: ArtistSortInfo) {
        viewModelScope.launch {
            settingsRepository.saveArtistSortInfo(sortInfo)
        }
    }

    fun setScrollToTopButtonEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveShowScrollTopButton(enabled)
        }
    }

    fun refreshSongs() {
        libraryScanManager.scanAll {
            libraryIndexRepository.rebuildArtistIndex()
        }
    }
}

