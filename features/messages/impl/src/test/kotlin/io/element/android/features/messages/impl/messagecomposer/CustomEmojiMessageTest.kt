package io.element.android.features.messages.impl.messagecomposer

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.textcomposer.model.Message
import org.jsoup.Jsoup
import org.junit.Test

class CustomEmojiMessageTest {
    @Test
    fun `chosen image becomes a Matrix emoticon while surrounding formatting is preserved`() {
        val message = Message(
            "<strong>Hi</strong> <a href=\"mxc://example.org/chosen\">:turtle:</a>",
            "**Hi** [:turtle:](<mxc://example.org/chosen>)",
            emptyList()
        )
        val result = formatCustomEmoji(message)
        val html = Jsoup.parseBodyFragment(result.html!!)
        val image = html.selectFirst("img")!!
        assertThat(image.attr("src")).isEqualTo("mxc://example.org/chosen")
        assertThat(image.attr("alt")).isEqualTo(":turtle:")
        assertThat(image.hasAttr("data-mx-emoticon")).isTrue()
        assertThat(html.selectFirst("strong")!!.text()).isEqualTo("Hi")
        assertThat(result.markdown).isEqualTo("**Hi** :turtle:")
    }

    @Test
    fun `code samples and ordinary links remain unchanged`() {
        val html = "<code><a href=\"mxc://example.org/a\">:a:</a></code> <a href=\"mxc://example.org/file\">file</a>"
        val message = Message(html, "unchanged", emptyList())
        assertThat(formatCustomEmoji(message)).isEqualTo(message)
    }

    @Test
    fun `duplicate shortcode labels retain distinct image URLs`() {
        val message = Message("<a href=\"mxc://one.org/a\">:same:</a><a href=\"mxc://two.org/b\">:same:</a>", "", emptyList())
        val html = Jsoup.parseBodyFragment(formatCustomEmoji(message).html!!)
        assertThat(html.select("img").map { it.attr("src") }).containsExactly("mxc://one.org/a", "mxc://two.org/b").inOrder()
    }
}
