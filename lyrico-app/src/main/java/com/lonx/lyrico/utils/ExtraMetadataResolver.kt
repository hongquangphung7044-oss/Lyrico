package com.lonx.lyrico.utils

import com.lonx.audiotag.model.AudioTagData
import com.lonx.lyrico.data.model.ExtraMetadataKey
import com.lonx.lyrico.data.model.ExtraMetadataTarget
import com.lonx.lyrico.data.model.ExtraMetadataWriteRule
import com.lonx.lyrico.data.model.ExtraWriteMode
import com.lonx.lyrico.data.model.ScoredSearchResult
import com.lonx.lyrico.data.model.entity.SongEntity

class ExtraMetadataResolver(
    private val minimumScore: Double = 0.35
) {
    fun resolve(
        currentSong: SongEntity,
        scoredResults: List<ScoredSearchResult>,
        rules: List<ExtraMetadataWriteRule>,
        currentTagData: AudioTagData? = null
    ): AudioTagData {
        var output = AudioTagData()

        rules.asSequence()
            .filter { it.mode != ExtraWriteMode.DISABLED }
            .forEach { rule ->
                val value = findBestExtraValue(rule, scoredResults) ?: return@forEach
                output = writeIfAllowed(output, currentSong, currentTagData, rule, value)
            }

        return output
    }

    fun mergeNonNull(first: AudioTagData, second: AudioTagData): AudioTagData {
        return first.copy(
            title = second.title ?: first.title,
            artist = second.artist ?: first.artist,
            album = second.album ?: first.album,
            albumArtist = second.albumArtist ?: first.albumArtist,
            genre = second.genre ?: first.genre,
            date = second.date ?: first.date,
            language = second.language ?: first.language,
            trackNumber = second.trackNumber ?: first.trackNumber,
            composer = second.composer ?: first.composer,
            lyricist = second.lyricist ?: first.lyricist,
            comment = second.comment ?: first.comment,
            lyrics = second.lyrics ?: first.lyrics,
            copyright = second.copyright ?: first.copyright,
            rating = second.rating ?: first.rating,
            replayGainTrackGain = second.replayGainTrackGain ?: first.replayGainTrackGain,
            replayGainTrackPeak = second.replayGainTrackPeak ?: first.replayGainTrackPeak,
            replayGainAlbumGain = second.replayGainAlbumGain ?: first.replayGainAlbumGain,
            replayGainAlbumPeak = second.replayGainAlbumPeak ?: first.replayGainAlbumPeak,
            replayGainReferenceLoudness = second.replayGainReferenceLoudness
                ?: first.replayGainReferenceLoudness,
            picUrl = second.picUrl ?: first.picUrl,
            pictures = if (second.pictures.isNotEmpty()) second.pictures else first.pictures,
            customFields = if (second.customFields.isNotEmpty()) second.customFields else first.customFields
        )
    }

    private fun findBestExtraValue(
        rule: ExtraMetadataWriteRule,
        scoredResults: List<ScoredSearchResult>
    ): String? {
        return scoredResults
            .asSequence()
            .filter { it.result.source == rule.source }
            .filter { it.source?.supportedExtras?.contains(rule.key.rawKey) != false }
            .filter { it.score >= minimumScore }
            .sortedByDescending { it.score }
            .mapNotNull { it.result.extras[rule.key.rawKey]?.takeIf(String::isNotBlank) }
            .firstOrNull()
    }

    private fun writeIfAllowed(
        currentOutput: AudioTagData,
        currentSong: SongEntity,
        currentTagData: AudioTagData?,
        rule: ExtraMetadataWriteRule,
        value: String
    ): AudioTagData {
        return when (rule.target) {
            ExtraMetadataTarget.COMMENT -> writeComment(currentOutput, currentSong, rule, value)
            ExtraMetadataTarget.REPLAY_GAIN_TRACK_GAIN -> {
                val currentValue = currentTagData?.replayGainTrackGain
                    ?: currentSong.replayGainTrackGain
                if (canWriteReplayGain(rule.mode, currentValue)) {
                    currentOutput.copy(replayGainTrackGain = value)
                } else currentOutput
            }
            ExtraMetadataTarget.REPLAY_GAIN_TRACK_PEAK -> {
                val currentValue = currentTagData?.replayGainTrackPeak
                    ?: currentSong.replayGainTrackPeak
                if (canWriteReplayGain(rule.mode, currentValue)) {
                    currentOutput.copy(replayGainTrackPeak = value)
                } else currentOutput
            }
            ExtraMetadataTarget.REPLAY_GAIN_REFERENCE_LOUDNESS -> {
                val currentValue = currentTagData?.replayGainReferenceLoudness
                    ?: currentSong.replayGainReferenceLoudness
                if (canWriteReplayGain(rule.mode, currentValue)) {
                    currentOutput.copy(replayGainReferenceLoudness = value)
                } else currentOutput
            }
        }
    }

    private fun writeComment(
        currentOutput: AudioTagData,
        currentSong: SongEntity,
        rule: ExtraMetadataWriteRule,
        value: String
    ): AudioTagData {
        if (rule.key != ExtraMetadataKey.NETEASE_163_KEY) return currentOutput
        val currentComment = currentSong.comment
        val canWrite = when (rule.mode) {
            ExtraWriteMode.DISABLED -> false
            ExtraWriteMode.OVERWRITE -> true
            ExtraWriteMode.SUPPLEMENT -> currentComment.isNullOrBlank() || isNetease163Key(currentComment)
        }
        return if (canWrite) currentOutput.copy(comment = value) else currentOutput
    }

    private fun canWriteReplayGain(
        mode: ExtraWriteMode,
        currentValue: String?
    ): Boolean {
        return when (mode) {
            ExtraWriteMode.DISABLED -> false
            ExtraWriteMode.SUPPLEMENT -> currentValue.isNullOrBlank()
            ExtraWriteMode.OVERWRITE -> true
        }
    }

    private fun isNetease163Key(value: String?): Boolean {
        return value?.startsWith("163 key(Don't modify):") == true
    }
}
