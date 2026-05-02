package com.lonx.lyrico.data.model

import com.lonx.lyrics.model.SearchSource
import com.lonx.lyrics.model.SongSearchResult

data class ScoredSearchResult(
    val result: SongSearchResult,
    val score: Double,
    val source: SearchSource? = null
)
