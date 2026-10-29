package io.element.android.libraries.preferences.impl.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import chat.schildi.lib.preferences.FriendProfile
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.preferences.test.FakePreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [36], manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FriendProfileStoreTest {
    @Test
    fun `friend preferences are persisted per account and reset removes the entry`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val data = FakePreferenceDataStoreFactory().create("friends")
        val alice = DefaultSessionPreferencesStore(context, SessionId("@alice:example.org"), backgroundScope, data)
        val bob = DefaultSessionPreferencesStore(
            context,
            SessionId("@bob:example.org"),
            backgroundScope,
            FakePreferenceDataStoreFactory().create("other")
        )
        val friendId = "@friend:example.org"
        val profile = FriendProfile("  Friend 🐢  ", 0xffee3311.toInt(), "Public name")
        alice.setFriendProfile(friendId, profile)
        assertThat(alice.getFriendProfiles().first()[friendId]).isEqualTo(profile.copy(nickname = "Friend 🐢"))
        assertThat(bob.getFriendProfiles().first()).isEmpty()
        val reopened = DefaultSessionPreferencesStore(context, SessionId("@alice:example.org"), backgroundScope, data)
        assertThat(reopened.getFriendProfiles().first()).isEqualTo(alice.getFriendProfiles().first())
        alice.setFriendProfile(friendId, FriendProfile())
        assertThat(reopened.getFriendProfiles().first()).isEmpty()
        alice.setFriendProfile(friendId, profile)
        alice.clear()
        assertThat(alice.getFriendProfiles().first()).isEmpty()
    }

    @Test
    fun `invalid nickname leaves existing friend preferences intact`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = DefaultSessionPreferencesStore(
            context,
            SessionId("@alice:example.org"),
            backgroundScope,
            FakePreferenceDataStoreFactory().create("invalid")
        )
        val id = "@friend:example.org"
        store.setFriendProfile(id, FriendProfile("Friend"))
        try {
            store.setFriendProfile(id, FriendProfile("a".repeat(101)))
            error("Expected invalid nickname to be rejected")
        } catch (_: IllegalArgumentException) {
            assertThat(store.getFriendProfiles().first()[id]?.nickname).isEqualTo("Friend")
        }
    }
}
