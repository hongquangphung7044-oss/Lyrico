package com.lonx.lyrico.utils

import com.lonx.lyrico.data.model.lyrics.LyricsLine
import com.lonx.lyrico.data.model.lyrics.LyricsResult
import com.lonx.lyrico.data.model.plugin.ResolvedLyricsProcessPolicy

object PluginLyricsPostProcessor {
    private val lrcTagLinePattern = Regex("""(?m)^\[[A-Za-z][A-Za-z0-9_%-]*:.*]\r?\n?""")

    fun process(
        result: LyricsResult,
        policy: ResolvedLyricsProcessPolicy
    ): LyricsResult {
        val normalized = if (policy.normalizeWhitespace) {
            result.copy(
                original = result.original.normalizeWhitespace(),
                translated = result.translated?.normalizeWhitespace(),
                romanization = result.romanization?.normalizeWhitespace(),
                rawPlainLrc = result.rawPlainLrc.normalizeWhitespaceText(),
                rawVerbatimLrc = result.rawVerbatimLrc.normalizeWhitespaceText(),
                rawEnhancedLrc = result.rawEnhancedLrc.normalizeWhitespaceText(),
                rawTtml = result.rawTtml.normalizeWhitespaceText(),
                rawMultiPersonEnhancedLrc = result.rawMultiPersonEnhancedLrc.normalizeWhitespaceText()
            )
        } else {
            result
        }

        return if (policy.keepLyricsTags) {
            normalized
        } else {
            normalized.copy(
                tags = emptyMap(),
                rawPlainLrc = normalized.rawPlainLrc.removeLrcTagLines(),
                rawVerbatimLrc = normalized.rawVerbatimLrc.removeLrcTagLines(),
                rawEnhancedLrc = normalized.rawEnhancedLrc.removeLrcTagLines(),
                rawMultiPersonEnhancedLrc = normalized.rawMultiPersonEnhancedLrc.removeLrcTagLines()
            )
        }
    }

    private fun List<LyricsLine>.normalizeWhitespace(): List<LyricsLine> {
        return map { line ->
            line.copy(
                words = line.words.map { word ->
                    word.copy(text = word.text.normalizeWhitespaceText())
                }
            )
        }
    }

    private fun String.normalizeWhitespaceText(): String {
        if (isBlank()) return this
        return replace(Regex("""[ \t\u00A0]+"""), " ")
    }

    private fun String.removeLrcTagLines(): String {
        if (isBlank()) return this
        return replace(lrcTagLinePattern, "")
    }
}
