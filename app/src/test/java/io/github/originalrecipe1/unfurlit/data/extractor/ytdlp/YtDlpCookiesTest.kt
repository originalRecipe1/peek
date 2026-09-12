package io.github.originalrecipe1.unfurlit.data.extractor.ytdlp

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test
import io.github.originalrecipe1.unfurlit.data.network.PlaybackCookieJar
import io.github.originalrecipe1.unfurlit.domain.model.ExtractedMedia

class YtDlpCookiesTest {
    private val source = "https://video.tiktok.com/media/video.mp4"
    private val future = System.currentTimeMillis() / 1000 + 3600

    @Test
    fun `decodes Python quoted and octal escaped token values`() {
        val cookies = YtDlpCookies.parse(
            """tt_chain_token="abc=="; Domain=.tiktok.com; Path=/; Secure; other="a\075b"; Domain=.tiktok.com; Path=/""",
            source,
        )
        assertEquals(listOf("abc==", "a=b"), cookies.map { it.value })
        assertTrue(YtDlpCookies.parse("""token="a\073injected=value"; Domain=.tiktok.com; Path=/""", source).isEmpty())
    }

    @Test
    fun `preserves multiple scoped cookies and omits attributes from request header`() {
        val cookies = YtDlpCookies.parse("ttwid=one; Domain=.tiktok.com; Path=/; Secure; Expires=$future; tt_chain_token=two; Domain=.tiktok.com; Path=/media; Secure", source)
        val selected = PlaybackCookieJar(cookies).loadForRequest(source.toHttpUrl())
        assertEquals(listOf("ttwid=one", "tt_chain_token=two"), selected.map { "${it.name}=${it.value}" })
        assertTrue(cookies.all { !it.toString().contains(it.value) })
    }

    @Test
    fun `cookies do not escape their domain path protocol or expiry`() {
        val cookies = YtDlpCookies.parse("session=value; Domain=.tiktok.com; Path=/media; Secure; Expires=$future", source)
        val jar = PlaybackCookieJar(cookies)
        assertEquals(1, jar.loadForRequest("https://other.tiktok.com/media/segment".toHttpUrl()).size)
        for (url in listOf("https://evil.example/media/a", "https://tiktok.com.evil.example/media/a", "https://video.tiktok.com/mediabad/a", "http://video.tiktok.com/media/a")) {
            assertTrue(jar.loadForRequest(url.toHttpUrl()).isEmpty())
        }
        assertTrue(PlaybackCookieJar(cookies.map { it.copy(expiresAtMillis = 1) }).loadForRequest(source.toHttpUrl()).isEmpty())
    }

    @Test
    fun `rejects absent foreign expired malformed or oversized scope`() {
        for (value in listOf(
            "session=value", "session=value; Domain=unrelated.example; Path=/",
            "session=value; Domain=.tiktok.com; Expires=1",
            "session=value; Domain=.tiktok.com; Expires=oops",
            "session=value\r\nInjected: true; Domain=.tiktok.com",
            "session=\"unterminated; Domain=.tiktok.com",
            "x".repeat(32769),
        )) assertTrue(value.take(50), YtDlpCookies.parse(value, source).isEmpty())
    }

    @Test
    fun `root and split format cookies stay with their own playback sources`() {
        val single = YtDlpJsonParser.parse("https://tiktok.com/post", """
            {"url":"$source","ext":"mp4","vcodec":"h264","acodec":"aac",
             "cookies":"ttwid=value; Domain=.tiktok.com; Path=/; Secure"}
        """.trimIndent()).media.single() as ExtractedMedia.Video
        assertEquals("ttwid", single.videoSource.cookies.single().name)
        assertNull(single.videoSource.headers["Cookie"])
        val split = YtDlpJsonParser.parse("https://tiktok.com/post", """
            {"cookies":"root=wrong; Domain=.tiktok.com; Path=/", "requested_formats":[
              {"url":"$source","ext":"mp4","vcodec":"h264","acodec":"none",
               "cookies":"video=right; Domain=.tiktok.com; Path=/"},
              {"url":"https://audio.example/audio.m4a","ext":"m4a","vcodec":"none","acodec":"aac"}
            ]}
        """.trimIndent()).media.single() as ExtractedMedia.Video
        assertEquals("video", split.videoSource.cookies.single().name)
        assertTrue(split.audioSource!!.cookies.isEmpty())
    }
}
