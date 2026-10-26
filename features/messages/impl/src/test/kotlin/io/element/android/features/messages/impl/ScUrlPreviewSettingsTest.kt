/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import chat.schildi.lib.preferences.LocalScPreferencesStore
import chat.schildi.lib.preferences.NotExactlyACompositionLocal
import chat.schildi.lib.preferences.PreviewScPreferencesStore
import chat.schildi.lib.preferences.ScPref
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.aRoomInfo
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class ScUrlPreviewSettingsTest : RobolectricTest() {
    @Test
    fun `master switch blocks previews even if encrypted previews were enabled`() = checkPolicy(false, true, true, false)

    @Test
    fun `encrypted rooms need explicit permission`() = checkPolicy(true, false, true, false)

    @Test
    fun `unknown encryption status is treated as encrypted`() = checkPolicy(true, false, null, false)

    @Test
    fun `unencrypted rooms use the master setting`() = checkPolicy(true, false, false, true)

    @Test
    fun `encrypted previews work when both switches are enabled`() = checkPolicy(true, true, true, true)

    private fun checkPolicy(enabled: Boolean, encryptedEnabled: Boolean, encrypted: Boolean?, expected: Boolean) = runAndroidComposeUiTest {
        val previous = LocalScPreferencesStore
        val store = object : ScPreferencesStore by PreviewScPreferencesStore {
            @Suppress("UNCHECKED_CAST")
            override fun <T> getCachedOrDefaultValue(scPref: ScPref<T>): T = when (scPref) {
                ScPrefs.URL_PREVIEWS -> enabled as T
                ScPrefs.URL_PREVIEWS_IN_E2EE_ROOMS -> encryptedEnabled as T
                ScPrefs.YOUTUBE_PREVIEWS -> true as T
                else -> scPref.defaultValue
            }
            override fun <T> settingFlow(scPref: ScPref<T>): Flow<T> = flowOf(getCachedOrDefaultValue(scPref))
        }
        LocalScPreferencesStore = NotExactlyACompositionLocal(store)
        val room = FakeBaseRoom(initialRoomInfo = aRoomInfo().copy(isEncrypted = encrypted))
        var allowed: Boolean? = null
        try {
            setSafeContent { allowed = Unit.takeIfUrlPreviewsEnabledForRoom(room) != null }
            runOnIdle { assertThat(allowed).isEqualTo(expected) }
        } finally {
            LocalScPreferencesStore = previous
        }
    }
}
