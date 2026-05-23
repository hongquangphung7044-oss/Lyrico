package com.lonx.lyrico.data.repository

import com.lonx.lyrico.data.model.plugin.PluginLyricsConfig
import kotlinx.coroutines.flow.Flow

interface PluginLyricsConfigRepository {
    val configsFlow: Flow<Map<String, PluginLyricsConfig>>

    suspend fun getConfig(pluginId: String): PluginLyricsConfig

    suspend fun updateConfig(config: PluginLyricsConfig)

    suspend fun removeConfig(pluginId: String)
}
