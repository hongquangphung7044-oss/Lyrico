package com.lonx.lyrico.data.model.plugin

import com.lonx.lyrico.data.model.ConversionMode
import kotlinx.serialization.Serializable

@Serializable
data class PluginLyricsConfig(
    val pluginId: String,
    val removeEmptyLines: FollowGlobalBooleanMode = FollowGlobalBooleanMode.FOLLOW_GLOBAL,
    val normalizeWhitespace: FollowGlobalBooleanMode = FollowGlobalBooleanMode.FOLLOW_GLOBAL,
    val keepLyricsTags: FollowGlobalBooleanMode = FollowGlobalBooleanMode.FOLLOW_GLOBAL,
    val scriptConversion: PluginScriptConversionMode = PluginScriptConversionMode.FOLLOW_GLOBAL
)

@Serializable
data class PluginLyricsConfigStore(
    val configs: Map<String, PluginLyricsConfig> = emptyMap()
)

@Serializable
enum class FollowGlobalBooleanMode {
    FOLLOW_GLOBAL,
    ENABLED,
    DISABLED
}

@Serializable
enum class PluginScriptConversionMode {
    FOLLOW_GLOBAL,
    DISABLED,
    SIMPLIFIED,
    TRADITIONAL
}

data class ResolvedLyricsProcessPolicy(
    val removeEmptyLines: Boolean,
    val normalizeWhitespace: Boolean,
    val keepLyricsTags: Boolean,
    val conversionMode: ConversionMode
)

data class GlobalLyricsSettings(
    val removeEmptyLines: Boolean,
    val normalizeWhitespace: Boolean = false,
    val keepLyricsTags: Boolean = true,
    val conversionMode: ConversionMode = ConversionMode.NONE
)

fun defaultPluginLyricsConfig(pluginId: String): PluginLyricsConfig {
    return PluginLyricsConfig(pluginId = pluginId)
}

fun resolveLyricsProcessPolicy(
    global: GlobalLyricsSettings,
    plugin: PluginLyricsConfig?
): ResolvedLyricsProcessPolicy {
    return ResolvedLyricsProcessPolicy(
        removeEmptyLines = plugin?.removeEmptyLines.resolve(global.removeEmptyLines),
        normalizeWhitespace = plugin?.normalizeWhitespace.resolve(global.normalizeWhitespace),
        keepLyricsTags = plugin?.keepLyricsTags.resolve(global.keepLyricsTags),
        conversionMode = plugin?.scriptConversion.resolve(global.conversionMode)
    )
}

private fun FollowGlobalBooleanMode?.resolve(globalValue: Boolean): Boolean {
    return when (this) {
        FollowGlobalBooleanMode.ENABLED -> true
        FollowGlobalBooleanMode.DISABLED -> false
        FollowGlobalBooleanMode.FOLLOW_GLOBAL,
        null -> globalValue
    }
}

private fun PluginScriptConversionMode?.resolve(globalValue: ConversionMode): ConversionMode {
    return when (this) {
        PluginScriptConversionMode.DISABLED -> ConversionMode.NONE
        PluginScriptConversionMode.SIMPLIFIED -> ConversionMode.TRADITIONAL_TO_SIMPLIFIED
        PluginScriptConversionMode.TRADITIONAL -> ConversionMode.SIMPLIFIED_TO_TRADITIONAL
        PluginScriptConversionMode.FOLLOW_GLOBAL,
        null -> globalValue
    }
}
