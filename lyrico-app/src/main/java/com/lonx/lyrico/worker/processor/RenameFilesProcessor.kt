package com.lonx.lyrico.worker.processor

import com.lonx.audiotag.model.AudioTagData
import com.lonx.lyrico.data.model.CharacterMappingRule
import com.lonx.lyrico.data.model.entity.BatchTaskEntity
import com.lonx.lyrico.data.model.entity.BatchTaskItemEntity
import com.lonx.lyrico.data.repository.RenameSongResult
import com.lonx.lyrico.data.repository.SongRepository
import com.lonx.lyrico.utils.ConflictResolver
import com.lonx.lyrico.utils.FileNameSanitizer
import com.lonx.lyrico.utils.FormatParser
import kotlinx.serialization.Serializable

@Serializable
data class RenameFilesTaskConfig(
    val renameFormat: String,
    val characterMappingRules: List<CharacterMappingRule> = emptyList()
)

@Serializable
data class RenameFilesTaskResult(
    val originalUri: String? = null,
    val newUri: String? = null,
    val originalPath: String? = null,
    val newPath: String
)

class RenameFilesProcessor(
    private val songRepository: SongRepository
) : BatchTaskProcessor {

    override suspend fun process(
        task: BatchTaskEntity,
        item: BatchTaskItemEntity,
        onProgress: suspend (Float) -> Unit
    ): BatchTaskProcessResult {
        val config = task.configJson?.let {
            kotlinx.serialization.json.Json.decodeFromString<RenameFilesTaskConfig>(it)
        } ?: throw BatchTaskSkippedException("No config")

        val song = songRepository.getSongByUri(item.songUri)
            ?: throw BatchTaskSkippedException("Song not found")

        val tagData = convertToAudioTagData(song)

        val tokens = FormatParser.parseFormat(config.renameFormat)
        var newFileName = FormatParser.buildFileName(tokens, tagData)
        newFileName = FileNameSanitizer.sanitize(newFileName, config.characterMappingRules)

        if (newFileName.isEmpty()) {
            newFileName = song.fileName.substringBeforeLast(".")
        }

        val extension = song.fileName.substringAfterLast(".", missingDelimiterValue = "")
        if (extension.isNotEmpty()) {
            newFileName = "$newFileName.$extension"
        }

        if (newFileName == song.fileName) {
            throw BatchTaskSkippedException("Same file name")
        }

        val result = songRepository.renameSongAndGetResult(song, newFileName)
            ?: throw Exception("Failed to rename file")

        return BatchTaskProcessResult(
            resultJson = kotlinx.serialization.json.Json.encodeToString(
                RenameFilesTaskResult.serializer(),
                RenameFilesTaskResult(
                    originalUri = result.oldUri,
                    newUri = result.newUri,
                    originalPath = song.filePath,
                    newPath = result.newFilePath
                )
            ),
            updatedFilePath = result.newFilePath,
            updatedFileName = result.newFileName
        )
    }

    private fun convertToAudioTagData(song: com.lonx.lyrico.data.model.entity.SongEntity): AudioTagData {
        return AudioTagData(
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumArtist = song.albumArtist,
            genre = song.genre,
            date = song.date,
            trackNumber = song.trackerNumber,
            discNumber = song.discNumber,
            composer = song.composer,
            lyricist = song.lyricist,
            comment = song.comment,
            lyrics = song.lyrics,
            copyright = song.copyright,
            rating = song.rating,
            fileName = song.fileName,
            durationMilliseconds = song.durationMilliseconds,
            bitrate = song.bitrate,
            sampleRate = song.sampleRate,
            channels = song.channels
        )
    }
}
