/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.features.messages.api.timeline.voicemessages.composer.VoiceMessageComposerEvent
import io.element.android.features.messages.api.timeline.voicemessages.composer.VoiceMessageComposerState
import io.element.android.features.messages.api.timeline.voicemessages.composer.VoiceMessageComposerStatePreviewParam
import io.element.android.features.messages.api.timeline.voicemessages.composer.aVoiceMessageComposerState
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.textcomposer.TextComposer
import io.element.android.libraries.textcomposer.model.Suggestion
import io.element.android.libraries.textcomposer.model.VoiceMessagePlayerEvent
import io.element.android.libraries.textcomposer.model.VoiceMessageRecorderEvent
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun MessageComposerView(
    state: MessageComposerState,
    voiceMessageState: VoiceMessageComposerState,
    modifier: Modifier = Modifier,
    scBeforeSend: (() -> Unit)? = null,
    emojiPickerRenderer: io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer = io.element.android.libraries.emoji.api.picker.NoOpEmojiPickerRenderer,
) {
    val view = LocalView.current
    fun sendMessage() {
        if (scBeforeSend != null) {
            scBeforeSend()
        }
        state.eventSink(MessageComposerEvent.SendMessage)
    }

    fun sendUri(uri: Uri) {
        state.eventSink(MessageComposerEvent.SendUri(uri))
    }

    fun onAddAttachment() {
        state.eventSink(MessageComposerEvent.AddAttachment)
    }

    fun onCloseSpecialMode() {
        state.eventSink(MessageComposerEvent.CloseSpecialMode)
    }

    fun onDismissTextFormatting() {
        view.clearFocus()
        state.eventSink(MessageComposerEvent.ToggleTextFormatting(enabled = false))
    }

    fun onSuggestionReceived(suggestion: Suggestion?) {
        state.eventSink(MessageComposerEvent.SuggestionReceived(suggestion))
    }

    fun onError(error: Throwable) {
        state.eventSink(MessageComposerEvent.Error(error))
    }

    fun onTyping(typing: Boolean) {
        state.eventSink(MessageComposerEvent.TypingNotice(typing))
    }

    val coroutineScope = rememberCoroutineScope()
    fun onRequestFocus() {
        coroutineScope.launch {
            state.textEditorState.requestFocus()
        }
    }

    val onVoiceRecorderEvent = { press: VoiceMessageRecorderEvent ->
        voiceMessageState.eventSink(VoiceMessageComposerEvent.RecorderEvent(press))
    }

    val onSendVoiceMessage = {
        voiceMessageState.eventSink(VoiceMessageComposerEvent.SendVoiceMessage)
        state.eventSink(MessageComposerEvent.CloseSpecialMode)
    }

    val onDeleteVoiceMessage = {
        voiceMessageState.eventSink(VoiceMessageComposerEvent.DeleteVoiceMessage)
    }

    val onVoicePlayerEvent = { event: VoiceMessagePlayerEvent ->
        voiceMessageState.eventSink(VoiceMessageComposerEvent.PlayerEvent(event))
    }

    TextComposer(
        modifier = modifier,
        state = state.textEditorState,
        voiceMessageState = voiceMessageState.voiceMessageState,
        onRequestFocus = ::onRequestFocus,
        onSendMessage = ::sendMessage,
        composerMode = state.mode,
        isInThreadTimeline = state.isInThreadTimeline,
        showTextFormatting = state.showTextFormatting,
        onResetComposerMode = ::onCloseSpecialMode,
        onAddAttachment = ::onAddAttachment,
        onDismissTextFormatting = ::onDismissTextFormatting,
        onVoiceRecorderEvent = onVoiceRecorderEvent,
        onVoicePlayerEvent = onVoicePlayerEvent,
        onSendVoiceMessage = onSendVoiceMessage,
        onDeleteVoiceMessage = onDeleteVoiceMessage,
        onReceiveSuggestion = ::onSuggestionReceived,
        resolveMentionDisplay = state.resolveMentionDisplay,
        resolveAtRoomMentionDisplay = state.resolveAtRoomMentionDisplay,
        onError = ::onError,
        onTyping = ::onTyping,
        onSelectRichContent = ::sendUri,
        onOpenEmojiPicker = if (state.emojiPickerEnabled) ({ state.eventSink(MessageComposerEvent.ShowEmojiPicker(true)) }) else null,
    )

    val picker = state.emojiPickerState
    if (picker != null && picker.isReady) {
        io.element.android.libraries.designsystem.theme.components.ModalBottomSheet(
            onDismissRequest = { state.eventSink(MessageComposerEvent.ShowEmojiPicker(false)) },
            modifier = Modifier.imePadding(),
            scrollable = false,
        ) {
            emojiPickerRenderer.Render(
                state = picker,
                onSelectEmoji = { emoji ->
                    state.eventSink(MessageComposerEvent.InsertEmoji(io.element.android.libraries.textcomposer.mentions.ResolvedSuggestion.Emoji(
                        emoji.label,
                        emoji.shortcodes.firstOrNull() ?: emoji.label,
                        unicode = emoji.unicode,
                    )))
                },
                onSelectCustomEmoji = {},
                selectedEmojis = kotlinx.collections.immutable.persistentSetOf(),
                modifier = Modifier.fillMaxWidth().height(360.dp),
                onSelectCustomImage = { image ->
                    state.eventSink(MessageComposerEvent.InsertEmoji(io.element.android.libraries.textcomposer.mentions.ResolvedSuggestion.Emoji(
                        image.name,
                        image.shortcode,
                        imageUrl = image.url,
                    )))
                },
                onSelectSticker = if (!state.mode.isEditing) ({ image -> state.eventSink(MessageComposerEvent.SendSticker(image)) }) else null,
                autoFocusSearch = true,
            )
        }
    }
    AsyncActionView(
        async = state.stickerAction,
        onSuccess = {},
        onErrorDismiss = { state.eventSink(MessageComposerEvent.ClearStickerError) },
    )

    AsyncActionView(
        async = state.slashCommandAction,
        onSuccess = {},
        onErrorDismiss = { state.eventSink(MessageComposerEvent.ClearSlashError) },
    )
}

@PreviewsDayNight
@Composable
internal fun MessageComposerViewPreview(
    @PreviewParameter(MessageComposerStatePreviewParam::class) state: MessageComposerState,
) = ElementPreview {
    Column {
        MessageComposerView(
            modifier = Modifier.height(IntrinsicSize.Min),
            state = state,
            voiceMessageState = aVoiceMessageComposerState(),
        )
        MessageComposerView(
            modifier = Modifier.height(200.dp),
            state = state,
            voiceMessageState = aVoiceMessageComposerState(),
        )
        DisabledComposerView()
    }
}

@PreviewsDayNight
@Composable
internal fun MessageComposerViewVoicePreview(
    @PreviewParameter(VoiceMessageComposerStatePreviewParam::class) state: VoiceMessageComposerState,
) = ElementPreview {
    Column {
        MessageComposerView(
            modifier = Modifier.height(IntrinsicSize.Min),
            state = aMessageComposerState(),
            voiceMessageState = state,
        )
    }
}
