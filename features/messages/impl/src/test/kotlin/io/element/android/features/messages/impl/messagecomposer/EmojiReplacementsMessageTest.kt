/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import chat.schildi.lib.preferences.EmojiReplacement
import chat.schildi.lib.preferences.decodeEmojiReplacements
import chat.schildi.lib.preferences.encodeEmojiReplacements
import chat.schildi.lib.preferences.replaceEmojiTokens
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.textcomposer.model.Message
import org.jsoup.Jsoup
import org.junit.Test

class EmojiReplacementsMessageTest {
    private val rules = listOf(EmojiReplacement("<3", "❤️"), EmojiReplacement("hi", "👋"))

    @Test
    fun `matches complete tokens including unicode spaces and newlines`() {
        assertThat(replaceEmojiTokens("<3 hi\n<3\u00a0hi x<3 <3x higher", rules))
            .isEqualTo("❤️ 👋\n❤️\u00a0👋 x<3 <3x higher")
    }

    @Test
    fun `backslashes escape matches and can themselves be escaped`() {
        assertThat(replaceEmojiTokens("""\<3 \\<3 \\\<3 \other""", rules)).isEqualTo("""<3 \❤️ \<3 \other""")
    }

    @Test
    fun `first duplicate wins and empty or multiword rows are ignored`() {
        val custom = rules + listOf(EmojiReplacement("hi", "other"), EmojiReplacement("", "x"), EmojiReplacement("a b", "x"))
        assertThat(replaceEmojiTokens("hi a b", custom)).isEqualTo("👋 a b")
        assertThat(replaceEmojiTokens("hi", listOf(EmojiReplacement("hi", "")))).isEqualTo("hi")
    }

    @Test
    fun `rules round trip through settings storage`() {
        assertThat(decodeEmojiReplacements(encodeEmojiReplacements(rules))).isEqualTo(rules)
        assertThat(decodeEmojiReplacements("invalid")).isEmpty()
    }

    @Test
    fun `empty rules preserve the original message`() {
        val message = Message(null, "<3", emptyList())
        assertThat(formatEmojiReplacements(message, emptyList())).isSameInstanceAs(message)
    }

    @Test
    fun `formatting is preserved while code links and adjacent word fragments are protected`() {
        val message = Message("<p><strong>&lt;3</strong> <code>&lt;3</code> <a href='https://example.org/hi'>hi</a> a<strong>hi</strong></p>", "**<3** `<3` [hi](https://example.org/hi) a**hi**", emptyList())
        val result = EmojiReplacementFormatter(rules).replaceHtml(message.html!!)
        val html = Jsoup.parseBodyFragment(result!!)
        assertThat(html.selectFirst("strong")!!.text()).isEqualTo("❤️")
        assertThat(html.selectFirst("code")!!.text()).isEqualTo("<3")
        assertThat(html.selectFirst("a")!!.text()).isEqualTo("hi")
        assertThat(html.select("strong").last()!!.text()).isEqualTo("hi")
    }

    @Test
    fun `markdown escaping sends a literal emoticon`() {
        val formatter = EmojiReplacementFormatter(rules)
        val protected = formatter.prepareMarkdown("""\<3 <3""")
        val result = formatter.replaceHtml(org.jsoup.nodes.Entities.escape(protected))
        assertThat(Jsoup.parseBodyFragment(result!!).text()).isEqualTo("<3 ❤️")
    }
    @Test
    fun `escaped words in formatting stay literal and custom emoji become images`() {
        val formatter = EmojiReplacementFormatter(rules)
        val protected = formatter.prepareMarkdown("""\<3""")
        val escaped = formatter.replaceHtml("<strong>${org.jsoup.nodes.Entities.escape(protected)}</strong>")
        assertThat(Jsoup.parseBodyFragment(escaped!!).selectFirst("strong")!!.text()).isEqualTo("<3")
        val customHtml = EmojiReplacementFormatter(listOf(EmojiReplacement("heart", "mxc://example.org/heart")))
            .replaceHtml("<p>heart</p>")!!
        val custom = formatCustomEmoji(Message(customHtml, "heart", emptyList()))
        assertThat(Jsoup.parseBodyFragment(custom.html!!).selectFirst("img")!!.attr("src")).isEqualTo("mxc://example.org/heart")
    }

}
