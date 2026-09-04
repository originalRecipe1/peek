package org.peek.app.ui.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import okhttp3.OkHttpClient
import org.peek.app.domain.model.ExtractedMedia
import org.peek.app.domain.model.ExtractionResult
import org.peek.app.domain.model.PlaybackSource

@UnstableApi
class Media3PlaybackMapper(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
) {
    fun map(
        extraction: ExtractionResult,
        video: ExtractedMedia.Video,
    ): MediaSource {
        val videoSource = video.videoSource.toMediaSource(extraction)
        val audioSource = video.audioSource?.toMediaSource(extraction)
        return if (audioSource == null) {
            videoSource
        } else {
            MergingMediaSource(videoSource, audioSource)
        }
    }

    fun map(
        extraction: ExtractionResult,
        audio: ExtractedMedia.Audio,
    ): MediaSource = audio.source.toMediaSource(extraction)

    private fun PlaybackSource.toMediaSource(extraction: ExtractionResult): MediaSource {
        val dataSourceFactory = OkHttpDataSource.Factory(client)
            .setDefaultRequestProperties(headers)
        val sourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .apply { mediaMimeType?.let(::setMimeType) }
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(extraction.title)
                    .setArtist(extraction.author)
                    .build(),
            )
            .build()
        return sourceFactory.createMediaSource(mediaItem)
    }
}
