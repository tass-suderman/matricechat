/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import chat.schildi.lib.preferences.EmojiReplacement
import chat.schildi.lib.preferences.emojiReplacementTokenRegex
import chat.schildi.lib.preferences.replaceEmojiTokens
import io.element.android.libraries.textcomposer.model.Message
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeVisitor
import uniffi.wysiwyg_composer.newComposerModel
import java.util.UUID

internal fun formatEmojiReplacements(message: Message, rules: List<EmojiReplacement>): Message {
    if (rules.none { it.text.isNotBlank() && it.emoji.isNotBlank() }) return message
    val formatter = EmojiReplacementFormatter(rules)
    val html = message.html ?: newComposerModel().use { model ->
        model.setContentFromMarkdown(formatter.prepareMarkdown(message.markdown))
        model.getContentAsMessageHtml()
    }
    val updatedHtml = formatter.replaceHtml(html) ?: return message
    val updatedMarkdown = newComposerModel().use { model ->
        model.setContentFromHtml(updatedHtml)
        model.getContentAsMessageMarkdown()
    }
    return message.copy(html = updatedHtml, markdown = updatedMarkdown)
}

internal class EmojiReplacementFormatter(rules: List<EmojiReplacement>) {
    private val images = mutableMapOf<String, String>()
    private val effectiveRules = rules.map { rule ->
        if (rule.emoji.matches(Regex("""mxc://[^/\s?#<>]+/[^/\s?#<>]+"""))) {
            val marker = "emoji${UUID.randomUUID().toString().replace("-", "")}"
            images[marker] = rule.emoji
            rule.copy(emoji = marker)
        } else rule
    }
    private val escapes = mutableMapOf<String, Pair<String, String>>()

    // Markdown consumes backslashes, so protect escaped tokens until after parsing.
    fun prepareMarkdown(value: String): String {
        var markdown = value
        val activeRules = effectiveRules.filter { it.text.isNotBlank() && it.text.none(Char::isWhitespace) && it.emoji.isNotBlank() }
            .distinctBy { it.text }.sortedByDescending { it.text.length }
        for (rule in activeRules) {
            markdown = Regex("""\\+${Regex.escape(rule.text)}""").replace(markdown) { match ->
                val marker = "emoji${UUID.randomUUID().toString().replace("-", "")}"
                escapes[marker] = match.value to replaceEmojiTokens(match.value, effectiveRules)
                marker
            }
        }
        return markdown
    }

    fun replaceHtml(html: String): String? {
        val document = Jsoup.parseBodyFragment(html)
        document.outputSettings().prettyPrint(false)
        val text = StringBuilder()
        val nodes = mutableListOf<Pair<TextNode, Int>>()
        document.body().traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                if (node is org.jsoup.nodes.Element && (node.isBlock || node.tagName() == "br")) text.append('\n')
                if (node is TextNode) {
                    nodes += node to text.length
                    text.append(node.wholeText)
                }
            }
            override fun tail(node: Node, depth: Int) {
                if (node is org.jsoup.nodes.Element && node.isBlock) text.append('\n')
            }
        })
        val tokens = emojiReplacementTokenRegex.findAll(text).toList()
        val wholeMarkers = tokens.map { it.value }.toSet()
        var changed = false
        for ((node, start) in nodes) {
            val original = node.wholeText
            val protected = generateSequence(node.parent()) { it.parent() }.any { it.nodeName() in setOf("code", "pre", "a") }
            var updated = original
            if (!protected) {
                for (token in tokens.asReversed()) {
                    if (token.range.first >= start && token.range.last < start + original.length) {
                        val replacement = replaceEmojiTokens(token.value, effectiveRules)
                        updated = updated.replaceRange(token.range.first - start, token.range.last + 1 - start, replacement)
                    }
                }
            }
            for ((marker, values) in escapes) updated = updated.replace(marker, if (protected || marker !in wholeMarkers) values.first else values.second)
            if (updated != original) {
                node.text(updated)
                changed = true
            }
        }
        if (!changed) return null
        var updatedHtml = document.body().html()
        for ((marker, url) in images) {
            val link = document.createElement("a").attr("href", url).text(":emoji:")
            updatedHtml = updatedHtml.replace(marker, link.outerHtml())
        }
        return updatedHtml
    }
}
