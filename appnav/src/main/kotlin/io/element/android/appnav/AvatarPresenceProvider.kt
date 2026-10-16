package io.element.android.appnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.UserPresence
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

@Composable
internal fun rememberAvatarPresenceProvider(client: MatrixClient): (String) -> Flow<UserPresence?> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return remember(client, lifecycle) {
        val users = mutableMapOf<String, Flow<UserPresence?>>()
        val provider: (String) -> Flow<UserPresence?> = { userId: String ->
            users.getOrPut(userId) {
                channelFlow {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (isActive) {
                            send(client.getUserPresenceInfo(UserId(userId)).getOrNull())
                            delay(60_000)
                        }
                    }
                }.stateIn(client.sessionCoroutineScope, SharingStarted.WhileSubscribed(0, 0), null)
            }
        }
        provider
    }
}
