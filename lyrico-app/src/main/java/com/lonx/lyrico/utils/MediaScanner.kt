package com.lonx.lyrico.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.lonx.lyrico.data.model.SongFile
import com.lonx.lyrico.data.model.entity.FolderEntity
import java.util.Locale
import kotlin.math.abs

data class SafScanResult(
    val songs: List<SafScannedSongFile>,
    val successfulFolderIds: Set<Long>,
    val failedFolderIds: Set<Long>
)

data class SafScannedSongFile(
    val songFile: SongFile,
    val rootFolderId: Long
)

class MediaScanner(
    private val context: Context,
) {

    private val TAG = "MediaScanner"

    fun querySongsFromSafFolders(folders: List<FolderEntity>): SafScanResult {
        val results = mutableListOf<SafScannedSongFile>()
        val successfulFolderIds = mutableSetOf<Long>()
        val failedFolderIds = mutableSetOf<Long>()
        val visitedUris = mutableSetOf<String>()

        for (folder in folders) {
            val treeUriString = folder.treeUri

            if (treeUriString.isNullOrBlank()) {
                failedFolderIds.add(folder.id)
                continue
            }

            try {
                val treeUri = Uri.parse(treeUriString)
                val root = DocumentFile.fromTreeUri(context, treeUri)

                if (root == null || !root.exists() || !root.isDirectory) {
                    failedFolderIds.add(folder.id)
                    continue
                }

                scanDocumentDirectory(
                    root = root,
                    displayRootPath = folder.path,
                    currentRelativePath = "",
                    rootFolderId = folder.id,
                    output = results,
                    visitedUris = visitedUris
                )

                successfulFolderIds.add(folder.id)
            } catch (e: Exception) {
                Log.e(TAG, "扫描 SAF 文件夹失败: ${folder.path}", e)
                failedFolderIds.add(folder.id)
            }
        }

        return SafScanResult(results, successfulFolderIds, failedFolderIds)
    }

    private fun scanDocumentDirectory(
        root: DocumentFile,
        displayRootPath: String,
        currentRelativePath: String,
        rootFolderId: Long,
        output: MutableList<SafScannedSongFile>,
        visitedUris: MutableSet<String>
    ) {
        val children = try {
            root.listFiles()
        } catch (e: Exception) {
            Log.w(TAG, "读取 SAF 子文件失败: ${root.uri}", e)
            return
        }

        for (child in children) {
            try {
                val name = child.name ?: continue

                if (child.isDirectory) {
                    if (shouldSkipDirectory(name)) continue

                    val nextRelativePath =
                        if (currentRelativePath.isBlank()) name else "$currentRelativePath/$name"

                    scanDocumentDirectory(
                        root = child,
                        displayRootPath = displayRootPath,
                        currentRelativePath = nextRelativePath,
                        rootFolderId = rootFolderId,
                        output = output,
                        visitedUris = visitedUris
                    )
                } else if (child.isFile) {
                    if (!isSupportedAudioFile(name, child.type)) continue

                    val uriString = child.uri.toString()
                    if (!visitedUris.add(uriString)) continue

                    val filePath = buildDisplayPath(displayRootPath, currentRelativePath, name)

                    output.add(
                        SafScannedSongFile(
                            rootFolderId = rootFolderId,
                            songFile = SongFile(
                                mediaId = createVirtualMediaId(uriString),
                                uri = child.uri,
                                filePath = filePath,
                                fileName = name,
                                lastModified = child.lastModified(),
                                dateAdded = child.lastModified(),
                                duration = 0L,
                                fileSize = child.length()
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "处理 SAF 文件失败: ${child.uri}", e)
            }
        }
    }

    private val supportedAudioExtensions = setOf(
        "mp3", "flac", "m4a", "mp4", "ogg", "opus", "wav", "aac", "wma", "ape"
    )

    private fun shouldSkipDirectory(name: String): Boolean {
        val normalized = name.lowercase(Locale.ROOT)
        return normalized.startsWith(".") ||
                normalized == "android" ||
                normalized == "data" ||
                normalized == "obb" ||
                normalized == "cache" ||
                normalized == "tmp"
    }

    private fun isSupportedAudioFile(fileName: String, mimeType: String?): Boolean {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)

        return extension in supportedAudioExtensions ||
                mimeType?.startsWith("audio/") == true
    }

    private fun buildDisplayPath(
        rootPath: String,
        relativePath: String,
        fileName: String
    ): String {
        return if (relativePath.isBlank()) {
            "${rootPath.trimEnd('/')}/$fileName"
        } else {
            "${rootPath.trimEnd('/')}/$relativePath/$fileName"
        }
    }

    private fun createVirtualMediaId(uriString: String): Long {
        val hash = uriString.hashCode().toLong()
        return -abs(hash).coerceAtLeast(1L)
    }
}
