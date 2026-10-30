package chat.matrice.features.trimmed

import io.element.android.features.call.api.CurrentCall
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class DisabledFeaturesTest {
    @Test
    fun `calls never report an active call`() {
        assertEquals(CurrentCall.None, DisabledCurrentCallService().currentCall.value)
    }

    @Test
    fun `location service never exposes a map provider`() {
        val service = DisabledLocationService()
        assertNull(service.mapTilerConfig.value)
        assertFalse(service.isServiceAvailable().value)
    }

    @Test
    fun `live location cannot start or claim to stop a share`() = runTest {
        val manager = DisabledLiveLocation()
        manager.setup()
        val roomId = RoomId("!room:example.org")
        assertTrue(manager.startShare(roomId, 5.minutes).isFailure)
        assertTrue(manager.stopShare(roomId).isFailure)
        assertTrue(manager.sharingRoomIds.value.isEmpty())
    }
}
