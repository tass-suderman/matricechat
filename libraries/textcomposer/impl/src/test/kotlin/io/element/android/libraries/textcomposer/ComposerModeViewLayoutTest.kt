/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.libraries.textcomposer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.ui.media.contentvalidation.ContentValidationValue
import io.element.android.libraries.matrix.ui.messages.reply.InReplyToDetails
import io.element.android.libraries.matrix.ui.messages.reply.InReplyToView
import io.element.android.libraries.textcomposer.model.MessageComposerMode
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class ComposerModeViewLayoutTest : RobolectricTest() {
    @Test
    fun `mention toggle sits below close without changing reply content size`() = runAndroidComposeUiTest {
        val details = InReplyToDetails.Loading(EventId("\$reply"))
        setSafeContent {
            ElementPreview {
                var mode by remember { mutableStateOf(MessageComposerMode.Reply(details, hideImage = false)) }
                Column {
                    ComposerModeView(
                        composerMode = mode,
                        onResetComposerMode = {},
                        onToggleReplyPing = { mode = mode.copy(pingReply = !mode.pingReply) },
                        modifier = Modifier.width(300.dp).testTag("reply"),
                    )
                    Box(Modifier.width(300.dp).testTag("original").padding(top = 8.dp, start = 8.dp, end = 8.dp).padding(4.dp)) {
                        InReplyToView(details, hideImage = false, contentValidationValue = ContentValidationValue.Valid)
                    }
                }
            }
        }
        val original = onNodeWithTag("original").fetchSemanticsNode().boundsInRoot
        val reply = onNodeWithTag("reply").fetchSemanticsNode().boundsInRoot
        assertThat(reply.size).isEqualTo(original.size)
        val close = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
        val toggle = onNodeWithText("@ On").fetchSemanticsNode().boundsInRoot
        assertThat(toggle.top).isAtLeast(close.bottom)
        onNodeWithText("@ On").performClick()
        onNodeWithText("@ Off").assertExists()
        assertThat(onNodeWithTag("reply").fetchSemanticsNode().boundsInRoot.size).isEqualTo(original.size)
    }
}
