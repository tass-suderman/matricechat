package chat.schildi.matrixsdk.urlpreview

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtube-nocookie.com", "www.youtube-nocookie.com")
private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")

fun youtubeVideoUrl(url: String): String? {
    val parsed = url.toHttpUrlOrNull() ?: return null
    val segments = parsed.pathSegments
    val id = when {
        parsed.host == "youtu.be" || parsed.host == "www.youtu.be" -> segments.firstOrNull()
        parsed.host !in youtubeHosts -> return null
        segments.firstOrNull() == "watch" -> parsed.queryParameter("v")
        segments.firstOrNull() in setOf("shorts", "embed", "live", "v") -> segments.getOrNull(1)
        else -> null
    } ?: return null
    if (!videoIdPattern.matches(id)) return null
    return "https://www.youtube.com/watch?v=$id"
}

internal fun youtubeMetadataUrl(videoUrl: String) = "https://www.youtube.com/oembed".toHttpUrl().newBuilder()
    .addQueryParameter("url", requireNotNull(youtubeVideoUrl(videoUrl)))
    .addQueryParameter("format", "json")
    .build()

@Serializable
private data class YouTubeMetadata(
    val title: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
)

internal fun decodeYouTubePreview(videoUrl: String, data: String): UrlPreview {
    val metadata = Json { ignoreUnknownKeys = true }.decodeFromString<YouTubeMetadata>(data)
    require(metadata.title.isNotBlank())
    val canonicalUrl = requireNotNull(youtubeVideoUrl(videoUrl))
    val thumbnail = metadata.thumbnailUrl?.toHttpUrlOrNull()?.takeIf {
        it.isHttps && (it.host == "ytimg.com" || it.host.endsWith(".ytimg.com"))
    }?.newBuilder()?.query(null)?.fragment(null)?.build()?.toString()
        ?: "https://i.ytimg.com/vi/${canonicalUrl.toHttpUrl().queryParameter("v")}/hqdefault.jpg"
    return UrlPreview(imageUrl = thumbnail, title = metadata.title, siteName = "YouTube", videoUrl = canonicalUrl)
}
