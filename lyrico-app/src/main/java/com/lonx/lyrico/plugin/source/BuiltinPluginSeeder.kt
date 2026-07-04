package com.lonx.lyrico.plugin.source

import android.content.Context
import android.util.Log
import com.lonx.lyrico.BuildConfig
import com.lonx.lyrico.data.model.plugin.PluginManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 将打包在 assets/builtin_plugins/ 下的官方插件在应用启动时自动种入到
 * filesDir/plugins/sources/，从而无需用户通过文件选择器导入 ZIP。
 *
 * 触发时机：App 启动；仅当 [BuildConfig.VERSION_CODE] 与上次种入记录不一致时执行，
 * 同版本下多次启动不会重复工作。
 *
 * 升级行为：更新内置插件文件与版本，但保留用户的 enabled / customName / sortOrder。
 * 用户在 App 内卸载过的内置插件会被记录，后续升级不再自动装回（见 [markUserUninstalled]）。
 *
 * assets 目录不存在时（如未通过 CI 暂存插件的普通构建）整个流程为空操作，
 * 不影响应用编译与运行。
 */
class BuiltinPluginSeeder(
    private val context: Context,
    private val installer: SourcePluginInstaller,
    private val json: Json
) {

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val currentVersion = BuildConfig.VERSION_CODE
            if (prefs.getInt(KEY_SEEDED_VERSION, -1) == currentVersion) return@withContext

            val installRoot = File(context.filesDir, "plugins/sources")
            val uninstalledIds = prefs.getStringSet(KEY_UNINSTALLED_IDS, emptySet()) ?: emptySet()
            val pluginDirs = listAssetPluginDirs()
            val knownIds = mutableSetOf<String>()

            for (dirName in pluginDirs) {
                val manifest = readManifest(dirName) ?: continue
                knownIds += manifest.id
                if (manifest.id in uninstalledIds) {
                    Log.i(TAG, "Skipping user-uninstalled builtin plugin: ${manifest.id}")
                    continue
                }

                val staging = File(
                    context.filesDir,
                    "plugins/.builtin-staging-$dirName-${System.currentTimeMillis()}"
                )
                try {
                    if (staging.exists()) staging.deleteRecursively()
                    staging.mkdirs()
                    copyAssetDir("$ASSET_DIR/$dirName", staging)
                    installer.installFromDirectory(
                        sourceRoot = staging,
                        installRoot = installRoot,
                        enabled = true
                    )
                    Log.i(TAG, "Seeded builtin plugin: ${manifest.id}")
                } catch (e: Exception) {
                    // 降级（用户已手动导入更新版本）等情况跳过，保留用户版本
                    Log.w(TAG, "Failed to seed builtin plugin ${manifest.id}: ${e.message}")
                } finally {
                    staging.deleteRecursively()
                }
            }

            prefs.edit()
                .putInt(KEY_SEEDED_VERSION, currentVersion)
                .putStringSet(KEY_KNOWN_BUILTIN_IDS, knownIds)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Builtin plugin seeding failed", e)
        }
    }

    /**
     * 用户卸载插件时调用。仅当该 id 属于内置插件时记录，使其在后续升级中不被自动装回。
     */
    suspend fun markUserUninstalled(id: String) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val known = prefs.getStringSet(KEY_KNOWN_BUILTIN_IDS, emptySet()) ?: emptySet()
            if (id !in known) return@withContext
            val existing = prefs.getStringSet(KEY_UNINSTALLED_IDS, emptySet()) ?: emptySet()
            prefs.edit().putStringSet(KEY_UNINSTALLED_IDS, existing + id).apply()
            Log.i(TAG, "Marked builtin plugin as user-uninstalled: $id")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark builtin uninstalled: $id", e)
        }
    }

    private fun readManifest(dirName: String): PluginManifest? {
        return runCatching {
            context.assets.open("$ASSET_DIR/$dirName/${MANIFEST_FILE}").bufferedReader().use { reader ->
                json.decodeFromString<PluginManifest>(reader.readText())
            }
        }.getOrNull()
    }

    private fun listAssetPluginDirs(): List<String> {
        return try {
            context.assets.list(ASSET_DIR)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun copyAssetDir(assetPath: String, target: File) {
        val children = context.assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            // assetPath 本身是文件
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        target.mkdirs()
        for (child in children) {
            copyAssetDir("$assetPath/$child", File(target, child))
        }
    }

    private companion object {
        const val PREFS = "builtin_plugins"
        const val KEY_SEEDED_VERSION = "seeded_for_version"
        const val KEY_KNOWN_BUILTIN_IDS = "known_builtin_ids"
        const val KEY_UNINSTALLED_IDS = "uninstalled_ids"
        const val ASSET_DIR = "builtin_plugins"
        const val MANIFEST_FILE = "manifest.json"
        const val TAG = "BuiltinPluginSeeder"
    }
}
