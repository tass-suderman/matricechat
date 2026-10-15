package io.element.android.features.messages.impl.messagecomposer

import io.element.android.libraries.emoji.api.packs.CustomImage
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.ThreadId
import io.element.android.libraries.textcomposer.mentions.ResolvedSuggestion
import io.element.android.wysiwyg.compose.RichTextEditorState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import uniffi.wysiwyg_composer.newComposerModel

internal fun stickerContent(sticker: CustomImage, replyTo: EventId?, threadRoot: ThreadId?): String = buildJsonObject {
    put("body", sticker.name)
    put("url", sticker.url)
    put("info", sticker.info?.let { Json.parseToJsonElement(it) as? JsonObject } ?: JsonObject(emptyMap()))
    if (replyTo != null || threadRoot != null) {
        putJsonObject("m.relates_to") {
            if (threadRoot != null) {
                put("rel_type", "m.thread")
                put("event_id", threadRoot.value)
                put("is_falling_back", replyTo == null)
            }
            putJsonObject("m.in_reply_to") { put("event_id", replyTo?.value ?: threadRoot!!.value) }
        }
    }
}.toString()

internal suspend fun insertPickerEmoji(state: RichTextEditorState, emoji: ResolvedSuggestion.Emoji) {
    val selection = state.selection
    val html = newComposerModel().use { model ->
        model.setContentFromHtml(state.messageHtml)
        model.select(selection.first.toUInt(), selection.second.toUInt())
        val imageUrl = emoji.imageUrl
        if (imageUrl != null) {
            model.setLinkWithText(imageUrl, emoji.text, emptyList())
        } else {
            model.replaceText(emoji.text)
        }
        model.getContentAsMessageHtml()
    }
    state.setHtml(html)
    state.setSelection(selection.first + emoji.text.length)
}
