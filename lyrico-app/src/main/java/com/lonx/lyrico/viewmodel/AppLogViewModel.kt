package com.lonx.lyrico.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.AppLogType
import com.lonx.lyrico.data.model.entity.AppLogEntity
import com.lonx.lyrico.data.repository.AppLogRepository
import com.lonx.lyrico.utils.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AppLogEvent {
    data class ShowMessage(val message: UiMessage) : AppLogEvent()
}

class AppLogViewModel(
    private val appLogRepository: AppLogRepository
) : ViewModel() {
    val logs: StateFlow<List<AppLogEntity>> = appLogRepository.observeLatest().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    private val _events = MutableSharedFlow<AppLogEvent>()
    val events = _events.asSharedFlow()

    fun clearLogs() {
        viewModelScope.launch {
            appLogRepository.clear()
        }
    }

    fun exportLogs(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = appLogRepository.exportText()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(text.toByteArray(Charsets.UTF_8))
                }
                _events.emit(AppLogEvent.ShowMessage(UiMessage.StringResource(R.string.export_success)))
            } catch (e: Exception) {
                appLogRepository.logException(
                    type = AppLogType.APP,
                    tag = TAG,
                    message = "Failed to export logs",
                    throwable = e
                )
                _events.emit(
                    AppLogEvent.ShowMessage(
                        UiMessage.StringResource(R.string.export_failed, e.message ?: "Unknown error")
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "AppLogViewModel"
    }
}
