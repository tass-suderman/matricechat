package chat.schildi.matrixsdk.urlpreview

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class YouTubePreviewTest {
    private val id = "dQw4w9WgXcQ"
    private val clean = "https://www.youtube.com/watch?v=$id"

    @Test
    fun `video variants are canonicalized without any shared tracking data`() {
        for (url in listOf(
            "https://youtu.be/$id?si=secret&t=42#tracking",
            "https://www.youtube.com/watch?v=$id&si=secret&utm_source=chat&feature=shared",
            "https://m.youtube.com/shorts/$id?feature=share",
            "https://music.youtube.com/watch?v=$id&list=tracking",
            "https://www.youtube-nocookie.com/embed/$id?start=12",
            "https://www.youtube.com/live/$id?si=secret",
            "http://youtube.com/v/$id?tracking=secret",
        )) {
            assertThat(youtubeVideoUrl(url)).isEqualTo(clean)
            val request = youtubeMetadataUrl(url)
            assertThat(request.host).isEqualTo("www.youtube.com")
            assertThat(request.queryParameterNames).containsExactly("url", "format")
            assertThat(request.queryParameter("url")).isEqualTo(clean)
            assertThat(request.toString()).doesNotContain("secret")
        }
    }

    @Test
    fun `invalid videos and lookalike domains are not treated as YouTube`() {
        for (url in listOf(
            "https://youtube.com.evil.org/watch?v=$id",
            "https://notyoutube.com/watch?v=$id",
            "https://youtube.com@evil.org/watch?v=$id",
            "https://youtube.com/playlist?list=$id",
            "https://youtube.com/watch?v=bad-id",
            "https://youtube.com/watch?v=$id/evil",
            "https://youtu.be/",
            "javascript:youtube.com/watch?v=$id",
        )) assertThat(youtubeVideoUrl(url)).isNull()
    }

    @Test
    fun `only metadata is used and thumbnail tracking is removed`() {
        val preview = decodeYouTubePreview(clean, """{
            "title":"A video", "thumbnail_url":"https://i.ytimg.com/vi/$id/hqdefault.jpg?tracking=secret",
            "html":"<iframe src='https://youtube.com/embed/$id'></iframe>", "author_name":"Channel"
        }""")
        assertThat(preview.title).isEqualTo("A video")
        assertThat(preview.imageUrl).isEqualTo("https://i.ytimg.com/vi/$id/hqdefault.jpg")
        assertThat(preview.videoUrl).isEqualTo(clean)
        assertThat(preview.siteName).isEqualTo("YouTube")
        assertThat(preview.description).isNull()
    }

    @Test
    fun `unexpected thumbnail origins use the standard YouTube thumbnail`() {
        val preview = decodeYouTubePreview(clean, """{"title":"Video", "thumbnail_url":"https://evil.org/pixel"}""")
        assertThat(preview.imageUrl).isEqualTo("https://i.ytimg.com/vi/$id/hqdefault.jpg")
    }
    @Test
    fun `generic preview responses cannot opt into the YouTube-only image path`() {
        val preview = Json { ignoreUnknownKeys = true }.decodeFromString<UrlPreview>(
            """{"og:title":"Page", "videoUrl":"https://www.youtube.com/watch?v=$id"}"""
        )
        assertThat(preview.videoUrl).isNull()
    }

}
