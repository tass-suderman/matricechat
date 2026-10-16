package io.element.android.libraries.matrix.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PresenceTest {
    @Test
    fun `reported states are preserved`() {
        listOf("online", "unavailable", "offline").forEach { status ->
            assertThat(parsePresence("""{"presence":"$status","last_active_ago":42}""")).isEqualTo(status)
        }
    }

    @Test
    fun `missing and unknown states do not appear offline`() {
        listOf("{}", """{"presence":"unknown"}""", """{"presence":null}""").forEach { content ->
            assertThat(parsePresence(content)).isNull()
        }
    }
    @Test
    fun `presence status messages are preserved without requiring a known presence`() {
        val presence = parseUserPresence("""{"presence":"online","status_msg":"Working remotely"}""")
        assertThat(presence.presence).isEqualTo("online")
        assertThat(presence.statusMessage).isEqualTo("Working remotely")
        assertThat(parseUserPresence("""{"status_msg":"Away for lunch"}""").statusMessage).isEqualTo("Away for lunch")
    }

    @Test
    fun `missing blank and non-string status messages remain absent`() {
        listOf("{}", """{"status_msg":null}""", """{"status_msg":"  "}""", """{"status_msg":42}""", """{"status_msg":{}}""").forEach {
            assertThat(parseUserPresence(it).statusMessage).isNull()
        }
    }
}
