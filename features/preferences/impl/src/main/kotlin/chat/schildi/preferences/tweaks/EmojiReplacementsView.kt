/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package chat.schildi.preferences.tweaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import chat.schildi.lib.R
import chat.schildi.lib.preferences.EmojiReplacement
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer
import io.element.android.libraries.emoji.api.picker.EmojiPickerState
import io.element.android.libraries.emoji.api.picker.NoOpEmojiPickerRenderer
import kotlinx.collections.immutable.persistentSetOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmojiReplacementsView(
    state: EmojiReplacementsState,
    renderer: EmojiPickerRenderer,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedRow by remember { mutableStateOf<Int?>(null) }
    PreferencePage(
        title = stringResource(R.string.screen_settings_emoji_replacements_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.screen_settings_emoji_replacements_help),
            modifier = Modifier.padding(16.dp),
            style = ElementTheme.typography.fontBodySmRegular,
        )
        if (state.saveFailed) {
            Text(stringResource(R.string.error_emoji_replacements_save), color = ElementTheme.colors.textCriticalPrimary)
        }
        state.rules.forEachIndexed { index, rule ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = rule.text,
                    onValueChange = { text ->
                        state.onChange(state.rules.toMutableList().apply { this[index] = rule.copy(text = text) })
                    },
                    label = stringResource(R.string.screen_settings_emoji_replacements_word),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = when {
                        rule.emoji.startsWith("mxc://") -> stringResource(R.string.common_custom_emoji)
                        rule.emoji.isEmpty() -> stringResource(R.string.screen_settings_emoji_replacements_choose)
                        else -> rule.emoji
                    },
                    onClick = { selectedRow = index },
                )
                IconButton(onClick = { state.onChange(state.rules.filterIndexed { row, _ -> row != index }) }) {
                    Icon(CompoundIcons.Delete(), contentDescription = stringResource(R.string.a11y_delete_emoji_replacement, index + 1))
                }
            }
        }
        TextButton(
            text = stringResource(R.string.screen_settings_emoji_replacements_add),
            onClick = { state.onChange(state.rules + EmojiReplacement()) },
            modifier = Modifier.padding(16.dp),
        )
    }
    val row = selectedRow
    if (row != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedRow = null },
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
            scrollable = false,
        ) {
            if (state.pickerState.isReady) {
                renderer.Render(
                    state = state.pickerState,
                    onSelectEmoji = { emoji ->
                        state.onChange(state.rules.toMutableList().apply { this[row] = this[row].copy(emoji = emoji.unicode) })
                        selectedRow = null
                    },
                    onSelectCustomEmoji = { emoji ->
                        state.onChange(state.rules.toMutableList().apply { this[row] = this[row].copy(emoji = emoji) })
                        selectedRow = null
                    },
                    selectedEmojis = persistentSetOf(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

internal class EmojiReplacementsPreviewParam : PreviewParameterProvider<List<EmojiReplacement>> {
    override val values = sequenceOf(emptyList(), listOf(EmojiReplacement("<3", "❤️"), EmojiReplacement()))
}

@PreviewsDayNight
@Composable
internal fun EmojiReplacementsPreview(
    @PreviewParameter(EmojiReplacementsPreviewParam::class) rules: List<EmojiReplacement>,
) = ElementPreview {
    EmojiReplacementsView(
        state = EmojiReplacementsState(rules, false, object : EmojiPickerState { override val isReady = false }, {}),
        renderer = NoOpEmojiPickerRenderer,
        onBackClick = {},
    )
}
