/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.libraries.mediaviewer.impl.local.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import io.element.android.compound.theme.ElementTheme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class MediaPlayerControllerViewTest : RobolectricTest() {
    @Test
    fun `speed control switches between normal and double speed`() = runAndroidComposeUiTest {
        setSafeContent {
            var state by remember { mutableStateOf(MediaPlayerControllerStatePreviewParam().values.first()) }
            ElementTheme {
                MediaPlayerControllerView(
                    state = state,
                    onTogglePlay = {},
                    onSeekChange = {},
                    onToggleMute = {},
                    audioFocus = null,
                    onTogglePlaybackSpeed = {
                        state = state.copy(playbackSpeed = if (state.playbackSpeed == 2f) 1f else 2f)
                    },
                )
            }
        }
        onNodeWithText("1×").performClick()
        onNodeWithText("2×").assertIsDisplayed().performClick()
        onNodeWithText("1×").assertIsDisplayed()
    }
}
