package com.lonx.lyrico.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedSelectionManager {

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    fun setUris(uris: Set<String>) {
        _selectedUris.value = uris
    }

    fun clearAll() {
        _selectedUris.value = emptySet()
    }
}
