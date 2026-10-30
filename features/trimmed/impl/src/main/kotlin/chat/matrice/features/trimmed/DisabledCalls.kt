package chat.matrice.features.trimmed

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.call.api.CallData
import io.element.android.features.call.api.CurrentCall
import io.element.android.features.call.api.CurrentCallService
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.callnative.api.NativeCallEntryPoint
import io.element.android.features.callnative.api.NativeCallHost
import io.element.android.features.callnative.api.NativeCallPip
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ContributesBinding(AppScope::class)
class DisabledCalls : ElementCallEntryPoint {
    override fun startCall(callData: CallData) = Unit

    override suspend fun handleIncomingCall(
        callData: CallData,
        eventId: EventId,
        senderId: UserId,
        roomName: String?,
        senderName: String?,
        avatarUrl: String?,
        timestamp: Long,
        expirationTimestamp: Long,
        notificationChannelId: String,
        textContent: String?,
    ) = Unit
}

@ContributesBinding(AppScope::class)
class DisabledCurrentCallService : CurrentCallService {
    override val currentCall: StateFlow<CurrentCall> = MutableStateFlow(CurrentCall.None)
}

@ContributesBinding(AppScope::class)
class DisabledNativeCalls : NativeCallEntryPoint {
    override fun startCall(callData: CallData) = Unit
}

@ContributesBinding(AppScope::class)
class DisabledNativeCallHost : NativeCallHost {
    @Composable
    override fun Render(modifier: Modifier, content: @Composable (Modifier) -> Unit) = content(modifier)
}

@ContributesBinding(AppScope::class)
class DisabledNativeCallPip : NativeCallPip {
    override fun onUserLeaveHint(activity: Activity) = Unit
}
