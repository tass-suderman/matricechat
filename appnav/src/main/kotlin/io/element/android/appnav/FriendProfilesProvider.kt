package io.element.android.appnav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import chat.schildi.lib.preferences.FriendProfile
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.ui.friends.FriendProfiles
import io.element.android.libraries.preferences.api.store.SessionPreferencesStore
import kotlinx.coroutines.awaitCancellation

@Composable
internal fun friendProfiles(client: MatrixClient, store: SessionPreferencesStore): FriendProfiles {
    val profiles by remember(store) { store.getFriendProfiles() }.collectAsState(emptyMap())
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val names by produceState<Map<String, String?>>(emptyMap(), client, profiles, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            value = emptyMap()
            for (id in profiles.keys) {
                client.getProfile(UserId(id)).onSuccess { value = value + (id to it.displayName) }
            }
            awaitCancellation()
        }
    }
    val save: suspend (String, FriendProfile) -> Unit = remember(client, store, profiles, names) {
        { id, profile ->
            val result = client.getProfile(UserId(id))
            val original = if (result.isSuccess) result.getOrNull()?.displayName else names[id] ?: profiles[id]?.globalName
            store.setFriendProfile(id, profile.copy(globalName = original))
        }
    }
    return remember(profiles, names, save) { FriendProfiles(profiles, names, save) }
}
