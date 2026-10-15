package io.element.android.libraries.designsystem.components.avatar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.preview.ElementPreview
import kotlin.math.hypot
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [36])
@RunWith(RobolectricTestRunner::class)
class AvatarPresenceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `presence badge straddles the circular picture edge without enlarging the avatar`() {
        verifyBadgeGeometry(AvatarSize.UserListItem, constrained = false)
    }

    @Test
    fun `whole presence badge fits inside a tightly clipped DM header frame`() {
        verifyBadgeGeometry(AvatarSize.CurrentRoomTopBar, constrained = true)
    }

    private fun verifyBadgeGeometry(size: AvatarSize, constrained: Boolean) {
        var pictureSizePx = 0f
        compose.setContent {
            pictureSizePx = with(LocalDensity.current) { size.dp.toPx() }
            ElementPreview {
                CompositionLocalProvider(LocalAvatarPresence provides { flowOf("online") }) {
                    val avatar = @androidx.compose.runtime.Composable {
                        Avatar(
                            anAvatarData(size = AvatarSize.RoomListItem).copy(presenceUserId = "@other:example.org"),
                            AvatarType.Room(),
                            Modifier.testTag("avatar"),
                            forcedAvatarSize = size.dp,
                        )
                    }
                    if (constrained) {
                        Box(Modifier.size(size.dp).clipToBounds()) { avatar() }
                    } else {
                        avatar()
                    }
                }
            }
        }
        val bounds = compose.onNodeWithTag("avatar").fetchSemanticsNode().boundsInRoot
        val badge = compose.onNodeWithContentDescription("Online", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertThat(bounds.width).isWithin(1f).of(pictureSizePx)
        assertThat(bounds.height).isWithin(1f).of(pictureSizePx)
        val radius = pictureSizePx / 2f
        val distance = hypot(badge.center.x - bounds.center.x, badge.center.y - bounds.center.y)
        assertThat(distance).isWithin(1f).of(radius)
        assertThat(badge.width).isWithin(1f).of(pictureSizePx * 0.28f)
        assertThat(badge.height).isWithin(1f).of(pictureSizePx * 0.28f)
        assertThat(badge.left).isAtLeast(bounds.left)
        assertThat(badge.top).isAtLeast(bounds.top)
        assertThat(badge.right).isAtMost(bounds.right)
        assertThat(badge.bottom).isAtMost(bounds.bottom)
    }

    @Test
    fun `named DM avatar uses the explicit partner identity without heroes`() {
        var requestedUser: String? = null
        compose.setContent {
            ElementPreview {
                CompositionLocalProvider(LocalAvatarPresence provides { userId ->
                    requestedUser = userId
                    flowOf("online")
                }) {
                    Avatar(
                        anAvatarData(id = "!dm:example.org").copy(presenceUserId = "@other:example.org"),
                        AvatarType.Room(),
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Online", useUnmergedTree = true).assertExists()
        assertThat(requestedUser).isEqualTo("@other:example.org")
    }
}
