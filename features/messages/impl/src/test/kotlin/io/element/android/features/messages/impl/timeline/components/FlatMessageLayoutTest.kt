/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.timeline.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineRoomInfo
import io.element.android.features.messages.impl.timeline.model.TimelineItemGroupPosition
import io.element.android.features.messages.impl.timeline.model.TimelineItemSelectionData
import io.element.android.features.messages.impl.timeline.model.bubble.aBubbleState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.testtags.TestTags
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class FlatMessageLayoutTest : RobolectricTest() {
    @Test
    fun `flat messages are disabled by default`() {
        assertThat(ScPrefs.FLAT_MESSAGES.defaultValue).isFalse()
    }

    @Test
    fun `own messages in direct chats keep the avatar and name at the start of a group`() = runAndroidComposeUiTest {
        var position by mutableStateOf<TimelineItemGroupPosition>(TimelineItemGroupPosition.First)
        setSafeContent {
            ElementPreview {
                ATimelineItemEventRow(
                    event = aTimelineItemEvent(isMine = true, groupPosition = position),
                    timelineRoomInfo = aTimelineRoomInfo(isDm = true),
                    flatLayout = true,
                )
            }
        }
        onNodeWithTag(TestTags.timelineItemSenderAvatar.value, useUnmergedTree = true).assertExists()
        onNodeWithTag(TestTags.timelineItemSenderName.value, useUnmergedTree = true).assertExists()
        runOnIdle { position = TimelineItemGroupPosition.Middle }
        onNodeWithTag(TestTags.timelineItemSenderAvatar.value, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `flat message uses available width and keeps click actions`() = runAndroidComposeUiTest {
        var clicks = 0
        setSafeContent {
            ElementTheme {
                Box(Modifier.width(300.dp)) {
                    MessageEventBubble(
                        state = aBubbleState(isMine = true),
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { clicks++ },
                        onLongClick = {},
                        flatLayout = true,
                    ) {
                        Text("Message")
                    }
                }
            }
        }
        onNodeWithTag(TestTags.messageBubble.value).assertWidthIsEqualTo(300.dp)
        onNodeWithText("Message").performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `own flat messages move aside for the selection indicator`() = runAndroidComposeUiTest {
        var progress by mutableFloatStateOf(0f)
        setSafeContent {
            ElementPreview {
                ATimelineItemEventRow(
                    event = aTimelineItemEvent(isMine = true, groupPosition = TimelineItemGroupPosition.First),
                    flatLayout = true,
                    selectionData = TimelineItemSelectionData(
                        isSelectionModeActive = true,
                        canBeSelected = true,
                        progress = { progress },
                    ),
                )
            }
        }
        val avatar = onNodeWithTag(TestTags.timelineItemSenderAvatar.value, useUnmergedTree = true)
        val before = avatar.fetchSemanticsNode().boundsInRoot.left
        runOnIdle { progress = 1f }
        assertThat(avatar.fetchSemanticsNode().boundsInRoot.left).isGreaterThan(before)
    }
}
