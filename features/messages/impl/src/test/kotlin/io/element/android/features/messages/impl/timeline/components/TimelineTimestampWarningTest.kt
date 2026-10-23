/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.timeline.components

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.timeline.item.event.MessageShield
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test

class TimelineTimestampWarningTest : RobolectricTest() {
    @Test
    fun `toggle hides only mismatch icon and restores normal timestamp colour`() = runAndroidComposeUiTest {
        val event = aTimelineItemEvent(messageShield = MessageShield.MismatchedSender(true))
        var hide by mutableStateOf(false)
        var normalColor = Color.Unspecified
        var criticalColor = Color.Unspecified
        val label = ApplicationProvider.getApplicationContext<Context>().getString(CommonStrings.a11y_encryption_details)
        setSafeContent {
            ElementPreview {
                normalColor = ElementTheme.colors.textSecondary
                criticalColor = ElementTheme.colors.textCriticalPrimary
                TimelineEventTimestampView(event, {}, hideMismatchedSenderWarning = hide)
            }
        }
        onNodeWithContentDescription(label).assertExists()
        val before = mutableListOf<TextLayoutResult>()
        onNodeWithText(event.sentTime).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(before) }
        assertThat(before.single().layoutInput.style.color).isEqualTo(criticalColor)
        runOnIdle { hide = true }
        onNodeWithContentDescription(label).assertDoesNotExist()
        val after = mutableListOf<TextLayoutResult>()
        onNodeWithText(event.sentTime).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(after) }
        assertThat(after.single().layoutInput.style.color).isEqualTo(normalColor)
    }

    @Test
    fun `other critical encryption warnings remain visible`() = runAndroidComposeUiTest {
        val event = aTimelineItemEvent(messageShield = MessageShield.VerificationViolation(true))
        val label = ApplicationProvider.getApplicationContext<Context>().getString(CommonStrings.a11y_encryption_details)
        setSafeContent {
            ElementPreview {
                TimelineEventTimestampView(event, {}, hideMismatchedSenderWarning = true)
            }
        }
        onNodeWithContentDescription(label).assertExists()
    }
}
