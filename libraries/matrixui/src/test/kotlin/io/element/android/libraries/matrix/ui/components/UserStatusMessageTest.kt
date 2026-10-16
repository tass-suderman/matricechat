package io.element.android.libraries.matrix.ui.components

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.user.DisplayedStatus
import io.element.android.libraries.matrix.api.user.UserStatus
import org.junit.Test

class UserStatusMessageTest {
    @Test
    fun `profile status includes the full message and takes precedence over presence`() {
        val status = DisplayedStatus.UserSet(UserStatus("☕", "Taking a break"))
        assertThat(userStatusMessage(status, "Old presence message")).isEqualTo("☕ Taking a break")
    }

    @Test
    fun `presence is used when profile status is absent or blank`() {
        listOf(null, DisplayedStatus.UserSet(UserStatus("", "  ")), DisplayedStatus.InCall(0L)).forEach {
            assertThat(userStatusMessage(it, "Working remotely")).isEqualTo("Working remotely")
        }
    }

    @Test
    fun `missing or blank statuses produce no subtitle`() {
        assertThat(userStatusMessage(null, null)).isNull()
        assertThat(userStatusMessage(null, "  ")).isNull()
        assertThat(userStatusMessage(DisplayedStatus.UserSet(UserStatus("", "")), null)).isNull()
    }
}
