/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.timeline.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import chat.schildi.lib.preferences.LocalScPreferencesStore
import chat.schildi.lib.preferences.NotExactlyACompositionLocal
import chat.schildi.lib.preferences.PreviewScPreferencesStore
import chat.schildi.lib.preferences.ScPref
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.theme.ScTheme
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.timeline.components.virtual.TimelineItemReadMarkerView
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class ChatAccentColourTest : RobolectricTest() {
    @Test
    fun `one chat colour controls cursor divider and fade in both themes`() = checkColours(true)

    @Test
    fun `disabling custom colours restores the existing theme colours`() = checkColours(false)

    private fun checkColours(customEnabled: Boolean) = runAndroidComposeUiTest {
        val previous = LocalScPreferencesStore
        val colour = Color(0xffbc27de)
        val store = object : ScPreferencesStore by PreviewScPreferencesStore {
            @Suppress("UNCHECKED_CAST")
            override fun <T> getCachedOrDefaultValue(scPref: ScPref<T>): T = when (scPref) {
                ScPrefs.CUSTOM_COLORS -> customEnabled as T
                ScPrefs.CHAT_ACCENT -> colour.toArgb() as T
                else -> scPref.defaultValue
            }
            override fun <T> settingFlow(scPref: ScPref<T>): Flow<T> = flowOf(getCachedOrDefaultValue(scPref))
        }
        LocalScPreferencesStore = NotExactlyACompositionLocal(store)
        val cursors = mutableListOf<Pair<Color, Color>>()
        val markerColours = mutableListOf<Color>()
        val fades = mutableListOf<List<Color>?>()
        val label = ApplicationProvider.getApplicationContext<Context>().getString(R.string.screen_room_timeline_read_marker_title).uppercase()
        try {
            setSafeContent {
                Column {
                    for (theme in listOf(Theme.Light, Theme.Dark)) {
                        ScTheme(theme = theme) {
                            cursors += ElementRichTextEditorStyle.composerStyle(true).cursor.color to ElementTheme.colors.iconAccentTertiary
                            markerColours += ScTheme.chatAccentColor ?: ScTheme.exposures.unreadIndicatorLine ?: ElementTheme.colors.textActionAccent
                            fades += scMessageHighlightColors()
                            TimelineItemReadMarkerView()
                        }
                    }
                }
            }
            runOnIdle {
                for ((cursor, fallback) in cursors) assertThat(cursor).isEqualTo(if (customEnabled) colour else fallback)
                if (customEnabled) for (fade in fades) {
                    assertThat(fade).containsExactly(colour.copy(alpha = 0.5f), Color.Transparent).inOrder()
                }
            }
            for (index in 0..1) {
                val layouts = mutableListOf<TextLayoutResult>()
                onAllNodesWithText(label)[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertThat(layouts.single().layoutInput.style.color).isEqualTo(if (customEnabled) colour else markerColours[index])
            }
        } finally {
            LocalScPreferencesStore = previous
        }
    }
}
