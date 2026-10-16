@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.features.userprofile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.features.userprofile.api.UserProfileVerificationState
import io.element.android.features.userprofile.shared.UserProfileHeaderSection
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.ui.components.LocalUserStatusMessage
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class UserProfileStatusMessageTest : RobolectricTest() {
    @Test
    fun `presence status appears below the full username and disappears when cleared`() = runAndroidComposeUiTest<ComponentActivity> {
        val status = MutableStateFlow<String?>("At lunch")
        setContent {
            ElementPreview {
                CompositionLocalProvider(LocalUserStatusMessage provides { status }) {
                    UserProfileHeaderSection(
                        avatarUrl = null,
                        userId = UserId("@alice:example.org"),
                        userName = "Alice",
                        verificationState = UserProfileVerificationState.UNKNOWN,
                        openAvatarPreview = {},
                        onUserIdClick = {},
                        withdrawVerificationClick = {},
                        displayedStatus = null,
                    )
                }
            }
        }
        val username = onNodeWithText("@alice:example.org").fetchSemanticsNode().boundsInRoot
        val message = onNodeWithText("At lunch").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(message.top).isAtLeast(username.bottom)
        runOnIdle { status.value = null }
        onNodeWithText("At lunch").assertDoesNotExist()
    }
}
