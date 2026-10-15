/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.textcomposer.components.markdown

import android.content.ClipData
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Annotation
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.getSpans
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.libraries.textcomposer.mentions.LocalMentionSpanUpdater
import io.element.android.libraries.textcomposer.mentions.MentionSpan
import io.element.android.libraries.textcomposer.model.CUSTOM_EMOJI_ANNOTATION
import io.element.android.libraries.textcomposer.model.MarkdownTextEditorState
import io.element.android.libraries.textcomposer.model.Suggestion
import io.element.android.libraries.textcomposer.model.SuggestionType
import io.element.android.libraries.textcomposer.model.aMarkdownTextEditorState
import io.element.android.wysiwyg.compose.RichTextEditorStyle
import io.element.android.wysiwyg.compose.internal.applyStyleInCompose
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

@Suppress("ModifierMissing")
@Composable
fun MarkdownTextInput(
    state: MarkdownTextEditorState,
    placeholder: String,
    placeholderColor: androidx.compose.ui.graphics.Color,
    onTyping: (Boolean) -> Unit,
    onSendMessage: () -> Unit,
    onReceiveSuggestion: (Suggestion?) -> Unit,
    richTextEditorStyle: RichTextEditorStyle,
    onSelectRichContent: ((Uri) -> Unit)?,
) {
    // Copied from io.element.android.wysiwyg.internal.utils.UriContentListener
    class ReceiveUriContentListener(
        private val onContent: (uri: Uri) -> Unit,
    ) : OnReceiveContentListener {
        override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
            val split = payload.partition { item -> item.uri != null }
            val uriContent = split.first
            val remaining = split.second

            if (uriContent != null) {
                val clip: ClipData = uriContent.clip
                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri
                    // ... app-specific logic to handle the URI ...
                    onContent(uri)
                }
            }
            // Return anything that we didn't handle ourselves. This preserves the default platform
            // behavior for text and anything else for which we are not implementing custom handling.
            return remaining
        }
    }

    val mentionSpanUpdater = LocalMentionSpanUpdater.current
    val coroutineScope = rememberCoroutineScope()
    val emojiBitmaps = remember { mutableMapOf<String, Bitmap>() }
    val requestedEmoji = remember { mutableSetOf<String>() }

    fun updateEmojiPreviews(editText: MarkdownEditText) {
        val text = editText.editableText
        text.getSpans<CustomEmojiImageSpan>().forEach(text::removeSpan)
        text.getSpans<Annotation>().filter { it.key == CUSTOM_EMOJI_ANNOTATION }.forEach { annotation ->
            val start = text.getSpanStart(annotation)
            val end = text.getSpanEnd(annotation)
            val label = text.subSequence(start, end).toString()
            if (!label.startsWith(":") || !label.endsWith(":")) {
                text.removeSpan(annotation)
                return@forEach
            }
            val bitmap = emojiBitmaps[annotation.value]
            if (bitmap != null) {
                val side = (editText.textSize * 1.4f).roundToInt()
                val drawable = BitmapDrawable(editText.resources, bitmap).apply { setBounds(0, 0, side, side) }
                text.setSpan(CustomEmojiImageSpan(drawable), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (requestedEmoji.add(annotation.value)) {
                coroutineScope.launch {
                    val request = ImageRequest.Builder(editText.context)
                        .data(MediaRequestData(MediaSource(annotation.value), MediaRequestData.Kind.Content))
                        .size(64)
                        .allowHardware(false)
                        .build()
                    SingletonImageLoader.get(editText.context).execute(request).image?.toBitmap()?.let { image ->
                        emojiBitmaps[annotation.value] = image
                        updateEmojiPreviews(editText)
                    }
                }
            }
        }
        editText.invalidate()
    }

    AndroidView(
        modifier = Modifier
            .padding(top = 5.dp, bottom = 6.dp)
            .fillMaxWidth(),
        factory = { context ->
            MarkdownEditText(context).apply {
                tag = TestTags.plainTextEditor.value // Needed for UI tests
                setPadding(0)
                setBackgroundColor(Color.TRANSPARENT)
                val text = state.text.value()
                setText(text)
                setHint(placeholder)
                setHintTextColor(ColorStateList.valueOf(placeholderColor.toArgb()))
                contentDescription = placeholder
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                val textRange = 0..text.length
                setSelection(state.selection.first.coerceIn(textRange), state.selection.last.coerceIn(textRange))
                setOnFocusChangeListener { _, hasFocus ->
                    state.hasFocus = hasFocus
                }
                addTextChangedListener { editable ->
                    onTyping(!editable.isNullOrEmpty())
                    if (state.lineCount != lineCount) {
                        post { bringPointIntoView(selectionStart) }
                    }
                    state.text.update(editable, false)
                    state.lineCount = lineCount

                    state.currentSuggestion = editable?.checkSuggestionNeeded()
                    onReceiveSuggestion(state.currentSuggestion)
                }
                onSelectionChangeListener = { selStart, selEnd ->
                    state.selection = selStart..selEnd
                    state.currentSuggestion = editableText.checkSuggestionNeeded()
                    onReceiveSuggestion(state.currentSuggestion)
                }
                if (onSelectRichContent != null) {
                    ViewCompat.setOnReceiveContentListener(
                        this,
                        arrayOf("image/*"),
                        ReceiveUriContentListener { onSelectRichContent(it) }
                    )
                }
                state.requestFocusAction = { this.requestFocus() }
            }
        },
        update = { editText ->
            editText.contentDescription = placeholder
            editText.applyStyleInCompose(richTextEditorStyle)
            editText.onEnterKeyListener = {
                onSendMessage()
                true
            }
            val text = state.text.value()
            mentionSpanUpdater.updateMentionSpans(text)
            if (state.text.needsDisplaying()) {
                editText.updateEditableText(text)
                state.text.update(editText.editableText, false)
            }
            updateEmojiPreviews(editText)
            val newSelectionStart = state.selection.first
            val newSelectionEnd = state.selection.last
            val currentTextRange = 0..editText.editableText.length
            val didSelectionChange = { editText.selectionStart != newSelectionStart || editText.selectionEnd != newSelectionEnd }
            val isNewSelectionValid = { newSelectionStart in currentTextRange && newSelectionEnd in currentTextRange }
            if (didSelectionChange() && isNewSelectionValid()) {
                editText.setSelection(state.selection.first, state.selection.last)
            }
        }
    )
}

private fun Editable.checkSuggestionNeeded(): Suggestion? {
    if (this.isEmpty()) return null
    var start = Selection.getSelectionStart(this)
    var end = Selection.getSelectionEnd(this)
    val range = 0..this.length

    if (start !in range || end !in range) {
        Timber.tag("checkSuggestionNeeded").e("Selection indices are out of bounds: start=$start, end=$end, text length=${this.length}")
        return null
    }

    // Make sure the selection order is correct, if not swap them: sometimes we can get the end before the start
    val tempEnd = end
    end = maxOf(start, end)
    start = minOf(start, tempEnd)

    var startOfWord = start
    while ((startOfWord > 0 || startOfWord == length) && !this[startOfWord - 1].isWhitespace()) {
        startOfWord--
    }
    if (startOfWord !in indices) return null
    val firstChar = this[startOfWord]

    if (start != end) return null
    if (getSpans<Annotation>(startOfWord, startOfWord + 1).any { it.key == CUSTOM_EMOJI_ANNOTATION }) return null

    // If a mention span already exists we don't need suggestions
    if (getSpans<MentionSpan>(startOfWord, startOfWord + 1).isNotEmpty()) return null

    return if (firstChar in listOf('@', '#', '/', ':')) {
        var endOfWord = end
        while (endOfWord < this.length && !this[endOfWord].isWhitespace()) {
            endOfWord++
        }
        if (startOfWord + 1 > endOfWord) {
            Timber.tag("checkSuggestionNeeded").e("No need to show suggestions for an invalid range (${startOfWord + 1}..$endOfWord)")
            return null
        }

        val text = this.subSequence(startOfWord + 1, endOfWord).toString()
        val suggestionType = when (firstChar) {
            '@' -> SuggestionType.Mention
            '#' -> SuggestionType.Room
            '/' -> SuggestionType.Command
            ':' -> SuggestionType.Emoji
            else -> error("Unknown suggestion type. This should never happen.")
        }
        Suggestion(startOfWord, endOfWord, suggestionType, text)
    } else {
        null
    }
}

@PreviewsDayNight
@Composable
internal fun MarkdownTextInputPreview() {
    ElementPreview {
        val style = ElementRichTextEditorStyle.composerStyle(hasFocus = true)
        MarkdownTextInput(
            state = aMarkdownTextEditorState(initialText = "Hello, World!"),
            placeholder = "Placeholder",
            placeholderColor = ElementTheme.colors.textSecondary,
            onTyping = {},
            onSendMessage = {},
            onReceiveSuggestion = {},
            richTextEditorStyle = style,
            onSelectRichContent = {},
        )
    }
}

private class CustomEmojiImageSpan(drawable: BitmapDrawable) : ImageSpan(drawable, ALIGN_BOTTOM)
