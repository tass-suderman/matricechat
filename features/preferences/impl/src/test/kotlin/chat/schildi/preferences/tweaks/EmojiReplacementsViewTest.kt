/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package chat.schildi.preferences.tweaks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import chat.schildi.lib.preferences.EmojiReplacement
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.emojibasebindings.Emoji
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.emoji.api.packs.CustomImage
import io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer
import io.element.android.libraries.emoji.api.picker.EmojiPickerState
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.collections.immutable.ImmutableSet
import org.junit.Test

class EmojiReplacementsViewTest : RobolectricTest() {
    @Test
    fun `add edit pick emoji and delete a rule`() = runAndroidComposeUiTest {
        var saved = emptyList<EmojiReplacement>()
        setSafeContent {
            var rules by remember { mutableStateOf(saved) }
            ElementTheme {
                EmojiReplacementsView(
                    state = EmojiReplacementsState(rules, false, object : EmojiPickerState { override val isReady = true }) {
                        saved = it
                        rules = it
                    },
                    renderer = TestPicker,
                    onBackClick = {},
                )
            }
        }
        onNodeWithText("Add New").performClick()
        onNodeWithText("Text").performTextInput("<3")
        onNodeWithText("Choose emoji").performClick()
        onNodeWithText("Pick heart").performClick()
        runOnIdle { assertThat(saved).containsExactly(EmojiReplacement("<3", "❤️")) }
        onNodeWithContentDescription("Delete replacement 1").performClick()
        runOnIdle { assertThat(saved).isEmpty() }
    }
}

private object TestPicker : EmojiPickerRenderer {
    @Composable
    override fun Render(
        state: EmojiPickerState,
        onSelectEmoji: (Emoji) -> Unit,
        onSelectCustomEmoji: (String) -> Unit,
        selectedEmojis: ImmutableSet<String>,
        modifier: Modifier,
        contentDescription: @Composable (Emoji, Boolean) -> String,
        onSelectCustomImage: ((CustomImage) -> Unit)?,
        onSelectSticker: ((CustomImage) -> Unit)?,
        autoFocusSearch: Boolean,
    ) {
        TextButton(text = "Pick heart", onClick = { onSelectCustomEmoji("❤️") })
    }
}
