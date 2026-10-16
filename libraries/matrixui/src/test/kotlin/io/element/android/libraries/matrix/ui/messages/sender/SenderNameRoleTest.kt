@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.libraries.matrix.ui.messages.sender

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import chat.schildi.lib.preferences.ScPrefs
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class SenderNameRoleTest : RobolectricTest() {
    @Test
    fun `role labels are hidden by default and can be enabled`() = runAndroidComposeUiTest<ComponentActivity> {
        assertThat(ScPrefs.SHOW_DISCORD_ROLE_LABELS.defaultValue).isFalse()
        val showLabels = mutableStateOf(false)
        setContent {
            ElementPreview {
                CompositionLocalProvider(LocalDiscordRoles provides mapOf("@alice:example.org" to DiscordRole("Moderator", 0xff12ab34L))) {
                    SenderName(
                        senderId = UserId("@alice:example.org"),
                        senderProfile = ProfileDetails.Ready(displayName = "Alice", displayNameAmbiguous = false, avatarUrl = null, displayedStatus = null),
                        senderNameMode = SenderNameMode.Timeline(Color.Red),
                        showRoleLabel = showLabels.value,
                    )
                }
            }
        }
        onNodeWithText("Alice").assertIsDisplayed()
        onNodeWithText("Moderator").assertDoesNotExist()
        runOnIdle { showLabels.value = true }
        onNodeWithText("Moderator").assertIsDisplayed()
    }
}
