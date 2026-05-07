package com.lonx.lyrico.worker.processor

import com.lonx.audiotag.model.AudioTagData
import com.lonx.lyrico.data.model.BatchTaskType
import com.lonx.lyrico.data.model.ConversionMode
import com.lonx.lyrico.data.model.LyricFormat
import com.lonx.lyrico.data.model.LyricRenderConfig
import com.lonx.lyrico.data.model.entity.BatchTaskEntity
import com.lonx.lyrico.data.model.entity.BatchTaskItemEntity
import com.lonx.lyrico.data.repository.SongRepository
import com.lonx.lyrico.utils.LyricDecoder
import com.lonx.lyrico.utils.LyricEncoder
import com.lonx.lyrico.viewmodel.LyricsFormatConfig
import kotlinx.serialization.json.Json

class LyricsFormatProcessor(
    private val songRepository: SongRepository
) : BatchTaskProcessor {

    override suspend fun process(
        task: BatchTaskEntity,
        item: BatchTaskItemEntity,
        onProgress: suspend (Float) -> Unit
    ): BatchTaskProcessResult {
        val config = task.configJson?.let {
            Json.decodeFromString<LyricsFormatConfig>(it)
        } ?: throw BatchTaskSkippedException("No config")

        val song = songRepository.getSongByUri(item.songUri)
            ?: throw BatchTaskSkippedException("Song not found")

        val lyrics = song.lyrics
        if (lyrics.isNullOrBlank()) {
            throw BatchTaskSkippedException("No lyrics")
        }

        val currentFormat = LyricDecoder.detectFormat(lyrics)
            ?: throw BatchTaskSkippedException("Unknown lyrics format")
        if (currentFormat == config.targetFormat) {
            throw BatchTaskSkippedException("Already target format")
        }

        val convertedLyrics = convertLyricsFormat(lyrics, currentFormat, config.targetFormat)
        if (convertedLyrics == lyrics) {
            throw BatchTaskSkippedException("Converted lyrics are unchanged")
        }

        val success = songRepository.patchAudioTags(
            item.songUri,
            AudioTagData(lyrics = convertedLyrics)
        )

        if (!success) {
            throw Exception("Write failed")
        }

        songRepository.updateSongMetadata(
            AudioTagData(lyrics = convertedLyrics),
            item.songUri,
            System.currentTimeMillis()
        )

        return BatchTaskProcessResult()
    }

    private fun convertLyricsFormat(
        lyrics: String,
        currentFormat: LyricFormat,
        targetFormat: LyricFormat
    ): String {
        val lyricsResult = try {
            LyricDecoder.decode(lyrics)
        } catch (e: Exception) {
            throw Exception("Decode failed: ${e.message ?: e::class.java.simpleName}", e)
        } ?: throw BatchTaskSkippedException("Unknown lyrics format")

        if (lyricsResult.original.isEmpty()) {
            throw BatchTaskSkippedException("No original lyric lines")
        }

        if (targetFormat.requiresWordLevelTiming() && !lyricsResult.isWordByWord) {
            throw BatchTaskSkippedException(
                "Cannot convert $currentFormat to $targetFormat: source lyrics have no word-level timing"
            )
        }

        val config = LyricRenderConfig(
            format = targetFormat,
            conversionMode = ConversionMode.NONE,
            showTranslation = lyricsResult.translated != null,
            showRomanization = lyricsResult.romanization != null,
            removeEmptyLines = true,
            onlyTranslationIfAvailable = false
        )
        val converted = LyricEncoder.encode(lyricsResult, config)
        if (converted.isBlank()) {
            throw Exception("Encode failed: empty result")
        }
        return converted
    }

    private fun LyricFormat.requiresWordLevelTiming(): Boolean {
        return this == LyricFormat.VERBATIM_LRC || this == LyricFormat.ENHANCED_LRC
    }
}
