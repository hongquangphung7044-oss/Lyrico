package com.lonx.lyrico.worker.processor

import com.lonx.audiotag.model.AudioTagData
import com.lonx.lyrico.data.model.entity.BatchTaskEntity
import com.lonx.lyrico.data.model.entity.BatchTaskItemEntity
import com.lonx.lyrico.data.repository.SongRepository
import com.lonx.lyrico.utils.ReplayGainCalculateState
import com.lonx.lyrico.utils.ReplayGainScanner

class ReplayGainProcessor(
    private val songRepository: SongRepository,
    private val replayGainScanner: ReplayGainScanner
) : BatchTaskProcessor {

    override suspend fun process(
        task: BatchTaskEntity,
        item: BatchTaskItemEntity,
        onProgress: suspend (Float) -> Unit
    ): BatchTaskProcessResult {
        val song = songRepository.getSongByUri(item.songUri)
            ?: throw BatchTaskSkippedException("Song not found")

        val hasExisting = !song.replayGainTrackGain.isNullOrBlank() ||
                !song.replayGainTrackPeak.isNullOrBlank() ||
                !song.replayGainAlbumGain.isNullOrBlank() ||
                !song.replayGainAlbumPeak.isNullOrBlank() ||
                !song.replayGainReferenceLoudness.isNullOrBlank()
        if (hasExisting) {
            throw BatchTaskSkippedException("ReplayGain already exists")
        }

        var analysisSuccess = false
        var analysisResult: com.lonx.lyrico.utils.ReplayGainAnalysis? = null

        replayGainScanner.analyze(item.songUri).collect { state ->
            when (state) {
                is ReplayGainCalculateState.Success -> {
                    analysisResult = state.analysis
                    analysisSuccess = true
                }
                is ReplayGainCalculateState.Cancelled,
                is ReplayGainCalculateState.Failed -> {
                    analysisSuccess = false
                }
                is ReplayGainCalculateState.Progress -> {
                    onProgress(state.percent)
                }
            }
        }

        if (!analysisSuccess || analysisResult == null) {
            throw Exception("ReplayGain analysis failed")
        }

        val tagData = AudioTagData(
            replayGainTrackGain = replayGainScanner.formatGain(analysisResult),
            replayGainTrackPeak = replayGainScanner.formatPeak(analysisResult.peak),
            replayGainReferenceLoudness = "-18 LUFS"
        )

        val writeSuccess = songRepository.patchAudioTags(item.songUri, tagData)
        if (!writeSuccess) {
            throw Exception("Write failed")
        }

        songRepository.updateSongMetadata(
            tagData,
            item.songUri,
            System.currentTimeMillis()
        )

        return BatchTaskProcessResult()
    }
}
