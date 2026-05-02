package com.lonx.lyrics.model

interface SearchSource {
    val sourceType: Source
    val supportedExtras: Set<String>
        get() = emptySet()

    suspend fun search(keyword: String, page: Int = 1,separator: String = "/", pageSize: Int = 20): List<SongSearchResult>
    suspend fun getLyrics(song: SongSearchResult): LyricsResult?
    suspend fun searchCover(keyword: String, pageSize: Int = 5): List<SongSearchResult>
}

object SearchResultExtraKeys {
    const val NETEASE_163_KEY = "netease_163_key"
    const val REPLAY_GAIN_TRACK_GAIN = "replaygain_track_gain"
    const val REPLAY_GAIN_TRACK_PEAK = "replaygain_track_peak"
    const val REPLAY_GAIN_REFERENCE_LOUDNESS = "replaygain_reference_loudness"
}
