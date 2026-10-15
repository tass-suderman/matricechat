/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.textcomposer.model

import android.os.Parcelable
import android.text.Annotation
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.element.android.libraries.matrix.api.core.RoomIdOrAlias
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.core.toRoomIdOrAlias
import io.element.android.libraries.matrix.api.permalink.PermalinkBuilder
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.textcomposer.components.markdown.StableCharSequence
import io.element.android.libraries.textcomposer.mentions.MentionSpanProvider
import io.element.android.libraries.textcomposer.mentions.MentionType
import io.element.android.libraries.textcomposer.mentions.ResolvedSuggestion
import io.element.android.libraries.textcomposer.mentions.getMentionSpans
import kotlinx.parcelize.Parcelize

const val CUSTOM_EMOJI_ANNOTATION = "tortuechat.custom_emoji"

@Stable
class MarkdownTextEditorState(
    initialText: String?,
    initialFocus: Boolean,
) {
    var text by mutableStateOf(StableCharSequence(initialText ?: ""))
    var selection by mutableStateOf(0..0)
    var hasFocus by mutableStateOf(initialFocus)
    var requestFocusAction by mutableStateOf({})
    var lineCount by mutableIntStateOf(1)
    var currentSuggestion by mutableStateOf<Suggestion?>(null)

    fun insertSuggestion(
        resolvedSuggestion: ResolvedSuggestion,
        mentionSpanProvider: MentionSpanProvider,
    ) {
        val suggestion = currentSuggestion ?: return
        when (resolvedSuggestion) {
            is ResolvedSuggestion.Emoji -> {
                val currentText = SpannableStringBuilder(text.value())
                val replacement = resolvedSuggestion.text
                val hasFollowingSpace = suggestion.end < currentText.length && currentText[suggestion.end] == ' '
                currentText.replace(suggestion.start, suggestion.end, replacement + if (hasFollowingSpace) "" else " ")
                resolvedSuggestion.imageUrl?.let { url ->
                    currentText.setSpan(
                        Annotation(CUSTOM_EMOJI_ANNOTATION, url),
                        suggestion.start,
                        suggestion.start + replacement.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                text.update(currentText, true)
                val end = suggestion.start + replacement.length + 1
                selection = end..end
                currentSuggestion = null
            }
            is ResolvedSuggestion.AtRoom -> {
                val currentText = SpannableStringBuilder(text.value())
                val mentionSpan = mentionSpanProvider.createEveryoneMentionSpan()
                currentText.replace(suggestion.start, suggestion.end, "@ ")
                val end = suggestion.start + 1
                currentText.setSpan(mentionSpan, suggestion.start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.update(currentText, true)
                selection = IntRange(end + 1, end + 1)
            }
            is ResolvedSuggestion.Member -> {
                val currentText = SpannableStringBuilder(text.value())
                val userId = resolvedSuggestion.roomMember.userId
                val mentionSpan = mentionSpanProvider.createUserMentionSpan(userId)
                currentText.replace(suggestion.start, suggestion.end, "@ ")
                val end = suggestion.start + 1
                currentText.setSpan(mentionSpan, suggestion.start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.update(currentText, true)
                selection = IntRange(end + 1, end + 1)
            }
            is ResolvedSuggestion.Alias -> {
                val currentText = SpannableStringBuilder(text.value())
                val roomAlias = resolvedSuggestion.roomAlias
                val mentionSpan = mentionSpanProvider.createRoomMentionSpan(roomAlias.toRoomIdOrAlias())
                currentText.replace(suggestion.start, suggestion.end, "# ")
                val end = suggestion.start + 1
                currentText.setSpan(mentionSpan, suggestion.start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.update(currentText, true)
                selection = IntRange(end + 1, end + 1)
            }
            is ResolvedSuggestion.Command -> {
                // Just insert the command text
                text.update("${resolvedSuggestion.command.command} ", true)
                val length = resolvedSuggestion.command.command.length + 1
                selection = IntRange(length, length)
            }
        }
    }

    fun getMessageMarkdown(
        permalinkBuilder: PermalinkBuilder,
        mentionDisplayName: (UserId) -> String = { it.value }, // SC
    ): String {
        val charSequence = text.value()
        if (charSequence !is Spanned) return charSequence.toString()
        val replacements = charSequence.getMentionSpans().mapNotNull { mention ->
            val replacement = when (val type = mention.type) {
                is MentionType.User -> permalinkBuilder.permalinkForUser(type.userId).getOrNull()?.let { link ->
                    "[${mentionDisplayName(type.userId)}]($link)"
                }
                is MentionType.Everyone -> "@room"
                is MentionType.Room -> (type.roomIdOrAlias as? RoomIdOrAlias.Alias)?.roomAlias?.let { alias ->
                    permalinkBuilder.permalinkForRoomAlias(alias).getOrNull()?.let { link -> "[$alias]($link)" }
                }
                else -> null
            }
            replacement?.let { Triple(charSequence.getSpanStart(mention), charSequence.getSpanEnd(mention), it) }
        } + charSequence.getSpans(0, charSequence.length, Annotation::class.java)
            .filter { it.key == CUSTOM_EMOJI_ANNOTATION }
            .map { annotation ->
                val start = charSequence.getSpanStart(annotation)
                val end = charSequence.getSpanEnd(annotation)
                val label = charSequence.subSequence(start, end).toString().replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
                Triple(start, end, "[$label](<${annotation.value}>)")
            }
        return StringBuilder(charSequence).apply {
            replacements.sortedByDescending { it.first }.forEach { (start, end, replacement) -> replace(start, end, replacement) }
        }.toString()
    }

    fun getMentions(): List<IntentionalMention> {
        val mentionSpans = text.value().getMentionSpans()
        return mentionSpans.mapNotNull { mentionSpan ->
            when (mentionSpan.type) {
                is MentionType.User -> IntentionalMention.User(mentionSpan.type.userId)
                is MentionType.Everyone -> IntentionalMention.Room
                else -> null
            }
        }
    }

    @Parcelize
    data class SavedValue(
        val text: CharSequence,
        val selectionStart: Int,
        val selectionEnd: Int,
    ) : Parcelable
}

object MarkdownTextEditorStateSaver : Saver<MarkdownTextEditorState, MarkdownTextEditorState.SavedValue> {
    override fun restore(value: MarkdownTextEditorState.SavedValue): MarkdownTextEditorState {
        return MarkdownTextEditorState(
            initialText = "",
            initialFocus = false,
        ).apply {
            text.update(value.text, true)
            selection = value.selectionStart..value.selectionEnd
        }
    }

    override fun SaverScope.save(value: MarkdownTextEditorState): MarkdownTextEditorState.SavedValue {
        return MarkdownTextEditorState.SavedValue(
            text = value.text.value(),
            selectionStart = value.selection.first,
            selectionEnd = value.selection.last,
        )
    }
}

@Composable
fun rememberMarkdownTextEditorState(
    initialText: String? = null,
    initialFocus: Boolean = false,
): MarkdownTextEditorState {
    return rememberSaveable(saver = MarkdownTextEditorStateSaver) { MarkdownTextEditorState(initialText, initialFocus) }
}
