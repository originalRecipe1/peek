package org.peek.app.domain.model

data class ExtractionResult(
    val sourceUrl: String,
    val platform: String?,
    val title: String?,
    val author: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val media: List<ExtractedMedia>,
)

sealed interface ExtractedMedia {
    data class Video(
        val videoSource: PlaybackSource,
        val audioSource: PlaybackSource?,
        val durationSeconds: Long?,
    ) : ExtractedMedia

    data class Image(
        val source: PlaybackSource,
    ) : ExtractedMedia

    data class Audio(
        val source: PlaybackSource,
        val durationSeconds: Long?,
        val artworkUrl: String?,
    ) : ExtractedMedia
}

data class PlaybackSource(
    val url: String,
    val headers: Map<String, String>,
    val format: StreamFormat,
    val mediaMimeType: String?,
    val formatId: String?,
)

enum class StreamFormat {
    Progressive,
    Hls,
    Dash,
}
