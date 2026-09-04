package org.peek.app.data.extractor.ytdlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.peek.app.domain.model.ExtractedMedia
import org.peek.app.domain.model.StreamFormat

class YtDlpJsonParserTest {
    @Test
    fun `parses a muxed progressive stream`() {
        val result = YtDlpJsonParser.parse(
            sourceUrl = "https://example.com/post/1",
            json = """
                {
                  "extractor_key": "Example",
                  "title": "A title",
                  "uploader": "A creator",
                  "duration": 42.8,
                  "url": "https://cdn.example.com/media.mp4?token=secret",
                  "format_id": "22",
                  "ext": "mp4",
                  "protocol": "https",
                  "vcodec": "avc1.64001F",
                  "acodec": "mp4a.40.2",
                  "http_headers": {"Referer": "https://example.com/"}
                }
            """.trimIndent(),
        )

        val video = result.media.single() as ExtractedMedia.Video
        assertEquals("A title", result.title)
        assertEquals(42L, video.durationSeconds)
        assertEquals(StreamFormat.Progressive, video.videoSource.format)
        assertEquals("https://example.com/", video.videoSource.headers["Referer"])
        assertNull(video.audioSource)
    }

    @Test
    fun `keeps split video and audio sources with per-format headers`() {
        val result = YtDlpJsonParser.parse(
            sourceUrl = "https://example.com/post/2",
            json = """
                {
                  "title": "Split stream",
                  "http_headers": {"User-Agent": "Peek test", "Referer": "https://common/"},
                  "requested_formats": [
                    {
                      "format_id": "137",
                      "url": "https://cdn.example.com/video.mp4",
                      "ext": "mp4",
                      "protocol": "https",
                      "vcodec": "avc1.640028",
                      "acodec": "none",
                      "http_headers": {"Referer": "https://video/"}
                    },
                    {
                      "format_id": "140",
                      "url": "https://cdn.example.com/audio.m4a",
                      "ext": "m4a",
                      "protocol": "https",
                      "vcodec": "none",
                      "acodec": "mp4a.40.2"
                    }
                  ]
                }
            """.trimIndent(),
        )

        val video = result.media.single() as ExtractedMedia.Video
        assertEquals("137", video.videoSource.formatId)
        assertEquals("https://video/", video.videoSource.headers["Referer"])
        assertEquals("Peek test", video.videoSource.headers["User-Agent"])
        assertEquals("140", video.audioSource?.formatId)
        assertEquals("audio/mp4", video.audioSource?.mediaMimeType)
    }

    @Test
    fun `ignores aggregate root codecs when split sources carry the URLs`() {
        val result = YtDlpJsonParser.parse(
            sourceUrl = "https://www.instagram.com/reel/example/",
            json = """
                {
                  "title": "Instagram-style split stream",
                  "ext": "mp4",
                  "vcodec": "vp9",
                  "acodec": "aac",
                  "width": 1080,
                  "height": 1080,
                  "requested_formats": [
                    {
                      "format_id": "video",
                      "url": "https://cdn.example.com/video.mp4",
                      "video_ext": "mp4",
                      "vcodec": "vp9",
                      "acodec": "none"
                    },
                    {
                      "format_id": "audio",
                      "url": "https://cdn.example.com/audio.m4a",
                      "audio_ext": "m4a",
                      "vcodec": "none",
                      "acodec": "aac"
                    }
                  ]
                }
            """.trimIndent(),
        )

        val video = result.media.single() as ExtractedMedia.Video
        assertEquals("video", video.videoSource.formatId)
        assertEquals("audio", video.audioSource?.formatId)
    }

    @Test
    fun `reads split sources nested in requested downloads`() {
        val result = YtDlpJsonParser.parse(
            sourceUrl = "https://example.com/post/nested",
            json = """
                {
                  "title": "Nested split stream",
                  "requested_downloads": [{
                    "requested_formats": [
                      {
                        "format_id": "video",
                        "url": "https://cdn.example.com/video.webm",
                        "ext": "webm",
                        "vcodec": "vp9",
                        "acodec": "none"
                      },
                      {
                        "format_id": "audio",
                        "url": "https://cdn.example.com/audio.webm",
                        "ext": "webm",
                        "vcodec": "none",
                        "acodec": "opus"
                      }
                    ]
                  }]
                }
            """.trimIndent(),
        )

        val video = result.media.single() as ExtractedMedia.Video
        assertEquals("video", video.videoSource.formatId)
        assertEquals("audio", video.audioSource?.formatId)
        assertEquals("audio/webm", video.audioSource?.mediaMimeType)
    }

    @Test
    fun `selects playable sources from an unselected formats list`() {
        val result = YtDlpJsonParser.parse(
            sourceUrl = "https://www.instagram.com/reel/example/",
            json = """
                {
                  "extractor_key": "Instagram",
                  "title": "Public reel",
                  "formats": [
                    {
                      "format_id": "low",
                      "url": "https://cdn.example.com/low.mp4",
                      "width": 480,
                      "height": 854,
                      "tbr": 400
                    },
                    {
                      "format_id": "1080",
                      "url": "https://cdn.example.com/full.mp4",
                      "width": 1080,
                      "height": 1920,
                      "tbr": 1800
                    },
                    {
                      "format_id": "medium",
                      "url": "https://cdn.example.com/medium.mp4",
                      "width": 720,
                      "height": 1280,
                      "tbr": 900
                    }
                  ]
                }
            """.trimIndent(),
        )

        val video = result.media.single() as ExtractedMedia.Video
        assertEquals("low", video.videoSource.formatId)
        assertNull(video.audioSource)
    }

    @Test
    fun `selects split video and audio from available formats`() {
        val result = YtDlpJsonParser.parse(
            sourceUrl = "https://example.com/watch/split-formats",
            json = """
                {
                  "formats": [
                    {
                      "format_id": "video-720",
                      "url": "https://cdn.example.com/video.mp4",
                      "height": 720,
                      "vcodec": "h264",
                      "acodec": "none"
                    },
                    {
                      "format_id": "audio-best",
                      "url": "https://cdn.example.com/audio.m4a",
                      "vcodec": "none",
                      "acodec": "aac",
                      "abr": 128
                    }
                  ]
                }
            """.trimIndent(),
        )

        val video = result.media.single() as ExtractedMedia.Video
        assertEquals("video-720", video.videoSource.formatId)
        assertEquals("audio-best", video.audioSource?.formatId)
    }

    @Test
    fun `recognizes adaptive manifests`() {
        val hls = YtDlpJsonParser.parse(
            "https://example.com/hls",
            """{"url":"https://cdn.example.com/master.m3u8","protocol":"m3u8_native","vcodec":"h264","acodec":"aac"}""",
        ).media.single() as ExtractedMedia.Video

        assertEquals(StreamFormat.Hls, hls.videoSource.format)
        assertEquals("application/x-mpegURL", hls.videoSource.mediaMimeType)
    }

    @Test
    fun `drops unsafe headers and private stream URLs`() {
        val privateStream = runCatching {
            YtDlpJsonParser.parse(
                "https://example.com/post/3",
                """{"url":"http://127.0.0.1/admin","vcodec":"h264","acodec":"aac"}""",
            )
        }
        assertTrue(privateStream.isFailure)

        val result = YtDlpJsonParser.parse(
            "https://example.com/post/4",
            """
                {
                  "url":"https://cdn.example.com/media.mp4",
                  "vcodec":"h264",
                  "acodec":"aac",
                  "http_headers": {
                    "User-Agent": "Safe value",
                    "Bad Header": "ignored",
                    "X-Injected": "first\r\nSecond: value"
                  }
                }
            """.trimIndent(),
        )
        val video = result.media.single() as ExtractedMedia.Video
        assertEquals(mapOf("User-Agent" to "Safe value"), video.videoSource.headers)
    }

    @Test
    fun `parses an image with required headers`() {
        val result = YtDlpJsonParser.parse(
            "https://example.com/post/image",
            """
                {
                  "title": "A still image",
                  "url": "https://cdn.example.com/photo.webp?token=short-lived",
                  "ext": "webp",
                  "vcodec": "unknown",
                  "acodec": "unknown",
                  "http_headers": {"Referer": "https://example.com/"}
                }
            """.trimIndent(),
        )

        val image = result.media.single() as ExtractedMedia.Image
        assertEquals("image/webp", image.source.mediaMimeType)
        assertEquals("https://example.com/", image.source.headers["Referer"])
    }

    @Test
    fun `parses an audio stream and artwork`() {
        val result = YtDlpJsonParser.parse(
            "https://example.com/post/audio",
            """
                {
                  "title": "An audio post",
                  "thumbnail": "https://cdn.example.com/art.jpg",
                  "duration": 12.9,
                  "url": "https://cdn.example.com/audio.opus",
                  "ext": "opus",
                  "vcodec": "none",
                  "acodec": "opus"
                }
            """.trimIndent(),
        )

        val audio = result.media.single() as ExtractedMedia.Audio
        assertEquals(12L, audio.durationSeconds)
        assertEquals("audio/ogg", audio.source.mediaMimeType)
        assertEquals("https://cdn.example.com/art.jpg", audio.artworkUrl)
    }

    @Test
    fun `normalizes playlist entries into a mixed media gallery`() {
        val result = YtDlpJsonParser.parse(
            "https://example.com/post/gallery",
            """
                {
                  "extractor_key": "ExampleGallery",
                  "title": "Three items",
                  "http_headers": {"User-Agent": "Peek test"},
                  "entries": [
                    {
                      "url": "https://cdn.example.com/one.jpg",
                      "ext": "jpg",
                      "http_headers": {"Referer": "https://gallery.example.com/"}
                    },
                    null,
                    {
                      "url": "https://cdn.example.com/two.mp4",
                      "ext": "mp4",
                      "vcodec": "h264",
                      "acodec": "aac"
                    },
                    {
                      "url": "https://cdn.example.com/three.mp3",
                      "ext": "mp3",
                      "vcodec": "none",
                      "acodec": "mp3"
                    }
                  ]
                }
            """.trimIndent(),
        )

        assertEquals(3, result.media.size)
        val image = result.media[0] as ExtractedMedia.Image
        assertEquals("Peek test", image.source.headers["User-Agent"])
        assertEquals("https://gallery.example.com/", image.source.headers["Referer"])
        assertTrue(result.media[1] is ExtractedMedia.Video)
        assertTrue(result.media[2] is ExtractedMedia.Audio)
    }

    @Test
    fun `rejects a playlist with no playable entries`() {
        val result = runCatching {
            YtDlpJsonParser.parse(
                "https://example.com/post/empty",
                """{"entries":[null,{"title":"Unavailable"}]}""",
            )
        }

        assertTrue(result.isFailure)
    }
}
