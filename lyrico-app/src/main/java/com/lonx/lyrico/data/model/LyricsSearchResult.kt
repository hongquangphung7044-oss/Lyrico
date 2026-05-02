package com.lonx.lyrico.data.model

import android.os.Parcelable
import com.lonx.lyrics.model.Source
import kotlinx.parcelize.Parcelize

@Parcelize
data class LyricsSearchResult(
    val title: String?,
    val artist: String?,
    val album: String?,
    val lyrics: String?,
    val date: String?,
    val trackerNumber: String?,
    val picUrl: String?,
    val source: Source? = null,
    val lyricsOnly: Boolean = false,
    val extras: Map<String, String> = emptyMap()
) : Parcelable
