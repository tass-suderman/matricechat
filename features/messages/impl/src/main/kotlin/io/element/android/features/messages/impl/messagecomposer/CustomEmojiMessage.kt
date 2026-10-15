package io.element.android.features.messages.impl.messagecomposer

import io.element.android.libraries.textcomposer.model.Message
import org.jsoup.Jsoup
import uniffi.wysiwyg_composer.newComposerModel

/** Keep the chosen MXC in a link while editing; encode it as a Matrix emoticon when sending. */
internal fun formatCustomEmoji(message: Message): Message {
    if (!(message.html ?: message.markdown).contains("mxc://")) return message
    val html = message.html ?: newComposerModel().use { model ->
        model.setContentFromMarkdown(message.markdown)
        model.getContentAsMessageHtml()
    }
    val document = Jsoup.parseBodyFragment(html)
    document.outputSettings().prettyPrint(false)
    var fallback = message.markdown
    var changed = false
    document.select("a[href^=mxc://]").forEach { link ->
        val label = link.text()
        val url = link.attr("href")
        if (!label.startsWith(":") || !label.endsWith(":") || link.parents().any { it.tagName() in setOf("code", "pre") }) return@forEach
        if (!url.matches(Regex("""mxc://[^/\s?#<>]+/[^/\s?#<>]+"""))) return@forEach
        val image = document.createElement("img")
            .attr("src", url)
            .attr("alt", label)
            .attr("data-mx-emoticon", "")
            .attr("width", "32")
            .attr("height", "32")
        link.replaceWith(image)
        val escaped = label.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
        fallback = fallback.replace("[$escaped](<$url>)", label).replace("[$escaped]($url)", label)
        changed = true
    }
    return if (changed) message.copy(html = document.body().html(), markdown = fallback) else message
}
