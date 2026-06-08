package com.lonx.lyrico.utils

import com.lonx.lyrico.data.model.lyrics.LyricFormat
import com.lonx.lyrico.data.model.lyrics.LyricsResult
import com.lonx.lyrico.utils.lyrics.document.LyricsDocumentPipeline

object LyricDecoder {
    fun detectFormat(lyricsText: String): LyricFormat? {
        if (lyricsText.isBlank()) return null

        val sampleLines = lyricsText.lines().filter { it.isNotBlank() }

        if (sampleLines.any { it.contains("<tt ") || it.contains("xmlns=\"http://www.w3.org/ns/ttml\"") }) {
            return LyricFormat.TTML
        }

        var hasEnhanced = false
        var hasVerbatim = false
        var hasPlain = false

        for (line in sampleLines) {
            if (line.startsWith("[ti:") || line.startsWith("[ar:") || line.startsWith("[al:")) continue

            val matches = LyricFormatter.LRC_TIME_PATTERN.findAll(line).toList()
            if (matches.isEmpty()) continue

            val hasBracket = matches.any { it.value.startsWith("[") }
            val hasAngle = matches.any { it.value.startsWith("<") }

            if (hasBracket && hasAngle) {
                hasEnhanced = true
                break
            } else if (hasBracket && matches.size > 1) {
                hasVerbatim = true
                break
            } else if (hasBracket && matches.size == 1) {
                hasPlain = true
            }
        }

        return when {
            hasEnhanced -> LyricFormat.ENHANCED_LRC
            hasVerbatim -> LyricFormat.VERBATIM_LRC
            hasPlain -> LyricFormat.PLAIN_LRC
            else -> null
        }
    }

    fun decode(lyricsText: String): LyricsResult? {
        val format = detectFormat(lyricsText) ?: return null
        return LyricsDocumentPipeline.parse(lyricsText, format)?.let { document ->
            with(LyricsDocumentPipeline) { document.toLyricsResult() }
        }
    }
}
