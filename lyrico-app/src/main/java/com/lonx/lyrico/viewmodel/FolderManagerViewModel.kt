package com.lonx.lyrico.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.data.LyricoDatabase
import com.lonx.lyrico.data.model.entity.FolderEntity
import com.lonx.lyrico.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class FolderManagerUiState(
    val folders: List<FolderEntity> = emptyList(),
    val scanningFolderIds: Set<Long> = emptySet(),
    val error: String? = null
) {
    val isLoading: Boolean
        get() = scanningFolderIds.isNotEmpty()
}

class FolderManagerViewModel(
    private val database: LyricoDatabase,
    private val songRepository: SongRepository
) : ViewModel() {

    private val folderDao = database.folderDao()
    private val scanState = MutableStateFlow(FolderManagerUiState())

    val uiState: StateFlow<FolderManagerUiState> =
        combine(
            folderDao.getAllFolders(),
            scanState
        ) { folders, state ->
            state.copy(folders = folders)
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                FolderManagerUiState()
            )

    fun addFolder(path: String, treeUri: String) {
        viewModelScope.launch {
            val id = folderDao.upsertAndGetId(
                path = path,
                treeUri = treeUri,
                addedBySaf = true
            )
            folderDao.setIgnored(id, false)
            scanFolder(id)
        }
    }

    fun addFolderByPath(path: String) {
        viewModelScope.launch {
            val id = folderDao.upsertAndGetId(path, addedBySaf = true)
            folderDao.setIgnored(id, false)
            scanFolder(id)
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            folderDao.deleteFolderPermanently(folder.id)
        }
    }

    fun refreshFolder(folder: FolderEntity) {
        viewModelScope.launch {
            scanFolder(folder.id)
        }
    }

    private suspend fun scanFolder(folderId: Long) {
        if (folderId in scanState.value.scanningFolderIds) return

        scanState.update {
            it.copy(
                scanningFolderIds = it.scanningFolderIds + folderId,
                error = null
            )
        }
        try {
            songRepository.synchronize(
                fullRescan = false,
                folderIds = setOf(folderId)
            )
        } catch (e: Exception) {
            scanState.update { it.copy(error = e.message) }
        } finally {
            scanState.update {
                it.copy(scanningFolderIds = it.scanningFolderIds - folderId)
            }
        }
    }
}
