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
}
