package com.lonx.lyrico.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lonx.lyrico.data.model.plugin.PluginLyricsConfig
import com.lonx.lyrico.data.model.plugin.PluginLyricsConfigStore
import com.lonx.lyrico.data.model.plugin.defaultPluginLyricsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class PluginLyricsConfigRepositoryImpl(
    private val context: Context,
    private val json: Json
) : PluginLyricsConfigRepository {
    override val configsFlow: Flow<Map<String, PluginLyricsConfig>>
        get() = context.settingsDataStore.data.map { preferences ->
            decodeStore(preferences[PLUGIN_LYRICS_CONFIG_STORE]).configs
        }

    override suspend fun getConfig(pluginId: String): PluginLyricsConfig {
        val stablePluginId = pluginId.toStableSourceId()
        return configsFlow.first()[stablePluginId] ?: defaultPluginLyricsConfig(stablePluginId)
    }

    override suspend fun updateConfig(config: PluginLyricsConfig) {
        val stablePluginId = config.pluginId.toStableSourceId()
        context.settingsDataStore.edit { preferences ->
            val currentStore = decodeStore(preferences[PLUGIN_LYRICS_CONFIG_STORE])
            val normalizedConfig = config.copy(pluginId = stablePluginId)
            preferences[PLUGIN_LYRICS_CONFIG_STORE] = json.encodeToString(
                currentStore.copy(
                    configs = currentStore.configs + (stablePluginId to normalizedConfig)
                )
            )
        }
    }

    override suspend fun removeConfig(pluginId: String) {
        val stablePluginId = pluginId.toStableSourceId()
        context.settingsDataStore.edit { preferences ->
            val currentStore = decodeStore(preferences[PLUGIN_LYRICS_CONFIG_STORE])
            preferences[PLUGIN_LYRICS_CONFIG_STORE] = json.encodeToString(
                currentStore.copy(configs = currentStore.configs - stablePluginId)
            )
        }
    }

    private fun decodeStore(raw: String?): PluginLyricsConfigStore {
        return raw
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                runCatching {
                    json.decodeFromString<PluginLyricsConfigStore>(value)
                }.getOrNull()
            }
            ?: PluginLyricsConfigStore()
    }

    private fun String.toStableSourceId(): String {
        return trim()
    }

    private companion object {
        val PLUGIN_LYRICS_CONFIG_STORE = stringPreferencesKey("plugin_lyrics_config_store")
    }
}
