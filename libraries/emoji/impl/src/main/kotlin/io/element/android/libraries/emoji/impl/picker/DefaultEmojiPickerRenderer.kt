/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.ContributesBinding
import io.element.android.emojibasebindings.Emoji
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.emoji.api.packs.CustomImage
import io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer
import io.element.android.libraries.emoji.api.picker.EmojiPickerState
import kotlinx.collections.immutable.ImmutableSet

@ContributesBinding(SessionScope::class)
class DefaultEmojiPickerRenderer : EmojiPickerRenderer {
    @Composable
    override fun Render(
        state: EmojiPickerState,
        onSelectEmoji: (Emoji) -> Unit,
        onSelectCustomEmoji: (String) -> Unit, // SC
        selectedEmojis: ImmutableSet<String>,
        modifier: Modifier,
        contentDescription: @Composable (Emoji, Boolean) -> String,
        onSelectCustomImage: ((CustomImage) -> Unit)?,
        onSelectSticker: ((CustomImage) -> Unit)?,
        autoFocusSearch: Boolean,
    ) {
        if (state is DefaultEmojiPickerState) {
            EmojiPickerView(
                state = state,
                onSelectEmoji = onSelectEmoji,
                onSelectCustomEmoji = onSelectCustomEmoji, // SC
                selectedEmojis = selectedEmojis,
                contentDescription = contentDescription,
                modifier = modifier,
                onSelectCustomImage = onSelectCustomImage,
                onSelectSticker = onSelectSticker,
                autoFocusSearch = autoFocusSearch,
            )
        } else {
            error("Unsupported state type: ${state::class.java}")
        }
    }
}
