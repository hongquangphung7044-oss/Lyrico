package com.lonx.lyrico.data.model

import androidx.annotation.StringRes
import com.lonx.lyrico.R
import com.lonx.lyrics.model.Source
import kotlinx.serialization.Serializable

@Serializable
enum class ExtraMetadataKey(
    val rawKey: String,
    @field:StringRes val labelRes: Int,
    @field:StringRes val summaryRes: Int,
    val defaultTarget: ExtraMetadataTarget
) {
    NETEASE_163_KEY(
        rawKey = "netease_163_key",
        labelRes = R.string.label_netease_163_key,
        summaryRes = R.string.label_netease_163_key_summary,
        defaultTarget = ExtraMetadataTarget.COMMENT
    ),
    REPLAY_GAIN_TRACK_GAIN(
        rawKey = "replaygain_track_gain",
        labelRes = R.string.label_replaygain_track_gain,
        summaryRes = R.string.label_replaygain_track_gain_summary,
        defaultTarget = ExtraMetadataTarget.REPLAY_GAIN_TRACK_GAIN
    ),
    REPLAY_GAIN_TRACK_PEAK(
        rawKey = "replaygain_track_peak",
        labelRes = R.string.label_replaygain_track_peak,
        summaryRes = R.string.label_replaygain_track_peak_summary,
        defaultTarget = ExtraMetadataTarget.REPLAY_GAIN_TRACK_PEAK
    ),
    REPLAY_GAIN_REFERENCE_LOUDNESS(
        rawKey = "replaygain_reference_loudness",
        labelRes = R.string.label_replaygain_reference_loudness,
        summaryRes = R.string.label_replaygain_reference_loudness_summary,
        defaultTarget = ExtraMetadataTarget.REPLAY_GAIN_REFERENCE_LOUDNESS
    )
}

@Serializable
enum class ExtraMetadataTarget(
    @field:StringRes val labelRes: Int
) {
    COMMENT(R.string.label_comment),
    REPLAY_GAIN_TRACK_GAIN(R.string.label_replaygain_track_gain),
    REPLAY_GAIN_TRACK_PEAK(R.string.label_replaygain_track_peak),
    REPLAY_GAIN_REFERENCE_LOUDNESS(R.string.label_replaygain_reference_loudness)
}

@Serializable
enum class ExtraWriteMode(
    @field:StringRes val labelRes: Int
) {
    DISABLED(R.string.extra_write_mode_disabled),
    SUPPLEMENT(R.string.extra_write_mode_supplement),
    OVERWRITE(R.string.extra_write_mode_overwrite)
}

@Serializable
data class ExtraMetadataWriteRule(
    val key: ExtraMetadataKey,
    val source: Source,
    val target: ExtraMetadataTarget = key.defaultTarget,
    val mode: ExtraWriteMode = ExtraWriteMode.DISABLED
)

object ExtraMetadataWriteDefaults {
    val DEFAULT_RULES = listOf(
        ExtraMetadataWriteRule(
            key = ExtraMetadataKey.NETEASE_163_KEY,
            source = Source.NE,
            target = ExtraMetadataTarget.COMMENT
        ),
        ExtraMetadataWriteRule(
            key = ExtraMetadataKey.REPLAY_GAIN_TRACK_GAIN,
            source = Source.QM,
            target = ExtraMetadataTarget.REPLAY_GAIN_TRACK_GAIN
        ),
        ExtraMetadataWriteRule(
            key = ExtraMetadataKey.REPLAY_GAIN_TRACK_PEAK,
            source = Source.QM,
            target = ExtraMetadataTarget.REPLAY_GAIN_TRACK_PEAK
        ),
        ExtraMetadataWriteRule(
            key = ExtraMetadataKey.REPLAY_GAIN_REFERENCE_LOUDNESS,
            source = Source.QM,
            target = ExtraMetadataTarget.REPLAY_GAIN_REFERENCE_LOUDNESS
        )
    )
}
