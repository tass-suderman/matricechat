/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.preferences.tweaks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import chat.schildi.lib.preferences.EmojiReplacement
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.decodeEmojiReplacements
import chat.schildi.lib.preferences.encodeEmojiReplacements
import dev.zacsweers.metro.Inject
import io.element.android.libraries.emoji.api.picker.EmojiPickerPresenter
import io.element.android.libraries.emoji.api.picker.EmojiPickerState
import io.element.android.libraries.emoji.api.recentemojis.EmptyGetRecentEmojis
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class EmojiReplacementsState(
    val rules: List<EmojiReplacement>,
    val saveFailed: Boolean,
    val pickerState: EmojiPickerState,
    val onChange: (List<EmojiReplacement>) -> Unit,
)

@Inject
class EmojiReplacementsPresenter(
    private val store: ScPreferencesStore,
    pickerFactory: EmojiPickerPresenter.Factory,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) {
    private val picker = pickerFactory.create(EmptyGetRecentEmojis)
    private val saveMutex = Mutex()

    @Composable
    internal fun present(): EmojiReplacementsState {
        var rules by remember { mutableStateOf(decodeEmojiReplacements(store.getCachedOrDefaultValue(ScPrefs.EMOJI_REPLACEMENTS))) }
        var saveFailed by remember { mutableStateOf(false) }
        return EmojiReplacementsState(rules, saveFailed, picker.present()) { updated ->
            rules = updated
            sessionCoroutineScope.launch {
                saveMutex.withLock {
                    try {
                        store.setSetting(ScPrefs.EMOJI_REPLACEMENTS, encodeEmojiReplacements(updated))
                        saveFailed = false
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        saveFailed = true
                    }
                }
            }
        }
    }
}
