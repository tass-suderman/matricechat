package io.element.android.libraries.matrix.api.media

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import org.junit.Test

class MediaSpoilersTest {
    @Test
    fun `FluffyChat and Discord bridge spoiler flags survive event and edit envelopes`() {
        assertThat(mediaSpoilerFlag(MEDIA_SPOILER_CONTENT)).isTrue()
        assertThat(mediaSpoilerFlag("""{"content":$MEDIA_SPOILER_CONTENT}""")).isTrue()
        assertThat(mediaSpoilerFlag("""{"content":{"m.new_content":$MEDIA_SPOILER_CONTENT}}""")).isTrue()
        assertThat(mediaSpoilerFlag("""{"content":{"$MEDIA_SPOILER_KEY":false}}""")).isFalse()
    }

    @Test
    fun `absent malformed and string flags do not mark media as spoilers`() {
        listOf(null, "not json", "{}", """{"$MEDIA_SPOILER_KEY":"true"}""").forEach {
            assertThat(mediaSpoilerFlag(it)).isNull()
        }
    }

    @Test
    fun `spoiler metadata reaches both original media and its thumbnail`() {
        val source = MediaSource("mxc://example.org/original", "encrypted-file-json")
        val info = ImageInfo(null, null, "image/png", null, null, MediaSource("mxc://example.org/thumbnail"), null)
        val image = ImageMessageType("image.png", null, null, source, info)
        val result = image.withMediaSpoiler(true) as ImageMessageType
        assertThat(result.source.isSpoiler).isTrue()
        assertThat(result.source.json).isEqualTo("encrypted-file-json")
        assertThat(result.info!!.thumbnailSource!!.isSpoiler).isTrue()
        assertThat((result.withMediaSpoiler(false) as ImageMessageType).source.isSpoiler).isFalse()
    }
}
