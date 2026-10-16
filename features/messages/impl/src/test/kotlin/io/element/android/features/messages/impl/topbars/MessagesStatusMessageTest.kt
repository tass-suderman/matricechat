@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.features.messages.impl.topbars

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import io.element.android.features.messages.impl.SharedHistoryIcon
import io.element.android.features.messages.impl.aMessagesState
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.anAvatarData
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.ui.components.LocalUserStatusMessage
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class MessagesStatusMessageTest : RobolectricTest() {
    @Test
    fun `DM header shows the other user's presence status message`() = runAndroidComposeUiTest<ComponentActivity> {
        val avatar = anAvatarData(size = AvatarSize.TimelineRoom).copy(presenceUserId = "@alice:example.org")
        setContent {
            ElementPreview {
                CompositionLocalProvider(LocalUserStatusMessage provides { userId ->
                    flowOf("At lunch".takeIf { userId == "@alice:example.org" })
                }) {
                    MessagesViewTopBar(
                        state = aMessagesState().copy(roomAvatar = avatar),
                        roomName = "Alice",
                        roomAvatar = avatar,
                        isTombstoned = false,
                        heroes = persistentListOf(),
                        dmUserIdentityState = null,
                        sharedHistoryIcon = SharedHistoryIcon.NONE,
                        dmUserStatus = null,
                        onRoomDetailsClick = {},
                        onBackClick = {},
                        menuActions = {},
                    )
                }
            }
        }
        onNodeWithText("Alice").assertIsDisplayed()
        onNodeWithText("At lunch").assertIsDisplayed()
    }
}
