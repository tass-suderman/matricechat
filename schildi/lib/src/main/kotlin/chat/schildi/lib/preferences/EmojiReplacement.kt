/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.lib.preferences

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EmojiReplacement(val text: String = "", val emoji: String = "")

fun decodeEmojiReplacements(value: String): List<EmojiReplacement> =
    runCatching { Json.decodeFromString<List<EmojiReplacement>>(value) }.getOrDefault(emptyList())

fun encodeEmojiReplacements(rules: List<EmojiReplacement>): String = Json.encodeToString(rules)

/** Exact whitespace-delimited tokens; an odd number of leading backslashes escapes a match. */
fun replaceEmojiTokens(text: String, rules: List<EmojiReplacement>): String {
    val replacements = rules.filter { it.text.isNotBlank() && it.text.none(Char::isWhitespace) && it.emoji.isNotBlank() }
        .distinctBy { it.text }.associate { it.text to it.emoji }
    return Regex("(?U)\\S+").replace(text) { match ->
        val token = match.value
        val slashes = token.takeWhile { it == '\\' }.length
        val word = token.drop(slashes)
        val emoji = replacements[word]
        if (emoji == null) token else "\\".repeat(slashes / 2) + if (slashes % 2 == 1) word else emoji
    }
}
