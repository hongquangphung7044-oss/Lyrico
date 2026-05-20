package com.lonx.lyrico.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.data.model.AlbumSortBy
import com.lonx.lyrico.data.model.AlbumSortInfo
import com.lonx.lyrico.data.model.dao.AlbumListItem
import com.lonx.lyrico.data.model.entity.AlbumEntity
import com.lonx.lyrico.data.repository.LibraryIndexRepository
import com.lonx.lyrico.utils.LibraryScanManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AlbumLibraryViewModel(
    libraryIndexRepository: LibraryIndexRepository,
    private val libraryScanManager: LibraryScanManager
) : ViewModel() {
    private val _sortInfo = MutableStateFlow(AlbumSortInfo())
    val sortInfo: StateFlow<AlbumSortInfo> = _sortInfo
    val scanState = libraryScanManager.state
    val albums: StateFlow<List<AlbumListItem>> =
        combine(libraryIndexRepository.observeAlbums(), sortInfo) { albums, sort ->
            val sorted = when (sort.sortBy) {
                AlbumSortBy.NAME -> albums.sortedWith(
                    compareBy<AlbumListItem> { it.sortKey }.thenBy { it.name }
                )

                AlbumSortBy.ALBUM_ARTIST -> albums.sortedWith(
                    compareBy<AlbumListItem> { it.albumArtist.orEmpty().uppercase() }
                        .thenBy { it.sortKey }
                        .thenBy { it.name }
                )

                AlbumSortBy.SONG_COUNT -> albums.sortedWith(
                    compareByDescending<AlbumListItem> { it.songCount }
                        .thenBy { it.sortKey }
                        .thenBy { it.name }
                )
                AlbumSortBy.YEAR -> {
                    val comparator = if (sort.order == SortOrder.ASC) {
                        compareBy<AlbumListItem> { it.year?.toIntOrNull() ?: Int.MAX_VALUE }
                            .thenBy { it.sortKey }
                            .thenBy { it.name }
                    } else {
                        compareBy<AlbumListItem> { it.year?.toIntOrNull() == null }
                            .thenByDescending { it.year?.toIntOrNull() ?: Int.MIN_VALUE }
                            .thenBy { it.sortKey }
                            .thenBy { it.name }
                    }
                    return@combine albums.sortedWith(comparator)
                }
            }
            if (sort.order == SortOrder.ASC) sorted else sorted.asReversed()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun onSortChange(sortInfo: AlbumSortInfo) {
        _sortInfo.value = sortInfo
    }

    fun refreshSongs() {
        libraryScanManager.scanAll()
    }
}

