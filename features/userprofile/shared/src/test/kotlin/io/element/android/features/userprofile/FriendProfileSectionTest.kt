@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package io.element.android.features.userprofile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import chat.schildi.lib.preferences.FriendProfile
import com.google.common.truth.Truth.assertThat
import io.element.android.features.userprofile.shared.FriendProfileSection
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.ui.friends.FriendProfiles
import io.element.android.libraries.matrix.ui.friends.LocalFriendProfiles
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class FriendProfileSectionTest : RobolectricTest() {
    @Test
    fun `nickname saves without clearing colour and reset clears both`() = runAndroidComposeUiTest<ComponentActivity> {
        val id = "@friend:example.org"
        var profile by mutableStateOf(FriendProfile("Old nickname", 0xff8844aa.toInt()))
        setContent {
            ElementPreview {
                CompositionLocalProvider(LocalFriendProfiles provides FriendProfiles(mapOf(id to profile), save = { userId, updated ->
                    assertThat(userId).isEqualTo(id)
                    profile = updated
                })) {
                    FriendProfileSection(id)
                }
            }
        }
        onNodeWithText("Friend appearance").performClick()
        onNode(hasSetTextAction()).performTextReplacement("Best friend")
        onNodeWithText("Save").performClick()
        runOnIdle {
            assertThat(profile.nickname).isEqualTo("Best friend")
            assertThat(profile.color).isEqualTo(0xff8844aa.toInt())
        }
        onNodeWithText("Friend appearance").performClick()
        onNodeWithText("Reset").performClick()
        onNodeWithText("Save").performClick()
        runOnIdle { assertThat(profile).isEqualTo(FriendProfile()) }
    }

    @Test
    fun `failed save keeps the edited nickname available for retry`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            ElementPreview {
                CompositionLocalProvider(LocalFriendProfiles provides FriendProfiles(save = { _, _ -> error("Save failed") })) {
                    FriendProfileSection("@friend:example.org")
                }
            }
        }
        onNodeWithText("Friend appearance").performClick()
        onNode(hasSetTextAction()).performTextReplacement("Best friend")
        onNodeWithText("Save").performClick()
        onNodeWithText("Could not save friend appearance. Please try again.").assertIsDisplayed()
        onNodeWithText("Best friend").assertIsDisplayed()
    }
}
