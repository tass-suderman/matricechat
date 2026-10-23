/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.libraries.designsystem.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import chat.schildi.lib.preferences.LocalScPreferencesStore
import chat.schildi.lib.preferences.NotExactlyACompositionLocal
import chat.schildi.lib.preferences.PreviewScPreferencesStore
import chat.schildi.lib.preferences.ScPref
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class WallpaperBackgroundTest : RobolectricTest() {
    @Test
    fun `bar opacity is independent and scoped to wallpaper screens`() = runAndroidComposeUiTest {
        val previous = LocalScPreferencesStore
        val store = object : ScPreferencesStore by PreviewScPreferencesStore {
            @Suppress("UNCHECKED_CAST")
            override fun <T> getCachedOrDefaultValue(scPref: ScPref<T>): T = when (scPref) {
                ScPrefs.TOP_BAR_OPACITY -> 25 as T
                ScPrefs.BOTTOM_BAR_OPACITY -> 75 as T
                else -> scPref.defaultValue
            }
            override fun <T> settingFlow(scPref: ScPref<T>): Flow<T> = flowOf(getCachedOrDefaultValue(scPref))
        }
        LocalScPreferencesStore = NotExactlyACompositionLocal(store)
        var outside = Color.Unspecified
        var top = Color.Unspecified
        var bottom = Color.Unspecified
        var disabled = Color.Unspecified
        try {
            setSafeContent {
                ElementTheme {
                    outside = Color.Red.topBarBackground()
                    WallpaperBackground {
                        top = Color.Red.topBarBackground()
                        bottom = Color.Blue.bottomBarBackground()
                    }
                    WallpaperBackground(enabled = false) { disabled = Color.Red.topBarBackground() }
                }
            }
            runOnIdle {
                assertThat(outside).isEqualTo(Color.Red)
                assertThat(top).isEqualTo(Color.Red.copy(alpha = 0.25f))
                assertThat(bottom).isEqualTo(Color.Blue.copy(alpha = 0.75f))
                assertThat(disabled).isEqualTo(Color.Red)
            }
        } finally {
            LocalScPreferencesStore = previous
        }
    }
}
