/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.emojibasebindings.Emoji
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toSp
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.emoji.api.packs.CustomImage
import io.element.android.libraries.emoji.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmojiPickerView(
    state: DefaultEmojiPickerState,
    onSelectEmoji: (Emoji) -> Unit,
    onSelectCustomEmoji: (String) -> Unit, // SC
    selectedEmojis: ImmutableSet<String>,
    modifier: Modifier = Modifier,
    contentDescription: @Composable (emoji: Emoji, isSelected: Boolean) -> String = { emoji, _ -> emoji.unicode },
    onSelectCustomImage: ((CustomImage) -> Unit)? = null,
    onSelectSticker: ((CustomImage) -> Unit)? = null,
    autoFocusSearch: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val composerPicker = onSelectSticker != null || onSelectCustomImage != null
    val canPickStickers = state.stickersEnabled && onSelectSticker != null
    var stickerTab by remember { mutableStateOf(false) }
    val pageCount = state.categories.size + (if (state.customImagesEnabled) 1 else 0) + (if (composerPicker) 0 else 1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var skinPickerEmoji by remember { mutableStateOf<Emoji?>(null) }
    val query = state.searchQuery.text.toString().trim().trim(':')
    fun selectCustom(url: String) {
        val image = state.customImages.firstOrNull { it.url == url }
        if (image != null && onSelectCustomImage != null) onSelectCustomImage(image) else onSelectCustomEmoji(url)
    }
    LaunchedEffect(Unit) { state.eventSink(EmojiPickerEvent.RefreshCustomImages) }
    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch) {
            focusRequester.requestFocus()
            androidx.compose.runtime.withFrameNanos { }
            keyboard?.show()
        }
    }
    Column(modifier) {
        io.element.android.libraries.designsystem.theme.components.TextField(
            value = state.searchQuery.text.toString(),
            onValueChange = { state.searchQuery.setTextAndPlaceCursorAtEnd(it) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = true,
            placeholder = stringResource(R.string.emoji_picker_search_placeholder),
        )
        if (composerPicker) {
            SecondaryTabRow(selectedTabIndex = if (stickerTab && canPickStickers) 1 else 0) {
                Tab(selected = !stickerTab || !canPickStickers, onClick = { stickerTab = false }, text = {
                    io.element.android.libraries.designsystem.theme.components.Text(stringResource(R.string.screen_emoji_picker_emoji_tab))
                })
                if (canPickStickers) {
                    Tab(selected = stickerTab, onClick = { stickerTab = true }, text = {
                        io.element.android.libraries.designsystem.theme.components.Text(stringResource(R.string.screen_emoji_picker_sticker_tab))
                    })
                }
            }
        }
        if (stickerTab && canPickStickers) {
            val stickers = state.stickers.filter { it.matches(query) }
            CustomEmojiGrid(stickers, state.customImagesError, { url ->
                stickers.firstOrNull { it.url == url }?.let { onSelectSticker?.invoke(it) }
            }, isLoading = state.customImagesLoading)
        } else {
            if (pageCount > 0) {
                androidx.compose.material3.SecondaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                    state.categories.forEachIndexed { index, category ->
                        Tab(
                            icon = {
                                when (val icon = category.icon) {
                                    is IconSource.Resource -> Icon(icon.id, stringResource(category.titleId))
                                    is IconSource.Vector -> Icon(icon.vector, stringResource(category.titleId))
                                }
                            },
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        )
                    }
                    if (!composerPicker) {
                        Tab(
                            text = {
                                io.element.android.libraries.designsystem.theme.components.Text(
                                stringResource(chat.schildi.lib.R.string.sc_freeform_reaction)
                            )
                            },
                            selected = pagerState.currentPage == state.categories.size,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(state.categories.size) } },
                        )
                    }
                    if (state.customImagesEnabled) {
                        val index = state.categories.size + if (composerPicker) 0 else 1
                        Tab(
                            icon = {
                                Icon(
                                    io.element.android.compound.tokens.generated.CompoundIcons.Sticker(),
                                stringResource(chat.schildi.lib.R.string.common_custom_emoji)
                                )
                            },
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        )
                    }
                }
            }
            if (query.isNotEmpty()) {
                val emojis =
                    (state.searchResults as? SearchBarResultState.Results<ImmutableList<Emoji>>)?.results ?: kotlinx.collections.immutable.persistentListOf()
                EmojiResults(
                    emojis = emojis,
                    customImages = state.customImages.filter { it.matches(query) },
                    onSelectCustomEmoji = ::selectCustom,
                    isEmojiSelected = { it.unicode in selectedEmojis },
                    onSelectEmoji = onSelectEmoji,
                    onLongPress = { skinPickerEmoji = it },
                    skinPickerEmoji = skinPickerEmoji,
                    onDismissSkinPicker = { skinPickerEmoji = null },
                    selectedEmojis = selectedEmojis,
                    contentDescription = contentDescription,
                )
            } else if (pageCount > 0) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { index ->
                    when {
                        index < state.categories.size -> EmojiResults(
                            emojis = state.categories[index].emojis,
                            isEmojiSelected = { it.unicode in selectedEmojis },
                            onSelectEmoji = onSelectEmoji,
                            onLongPress = { skinPickerEmoji = it },
                            skinPickerEmoji = skinPickerEmoji,
                            onDismissSkinPicker = { skinPickerEmoji = null },
                            selectedEmojis = selectedEmojis,
                            contentDescription = contentDescription,
                        )
                        !composerPicker && index == state.categories.size -> FreeformReactionView(onSelectCustomEmoji)
                        else -> CustomEmojiGrid(
                            state.customImages,
                            state.customImagesError,
                            ::selectCustom,
                            selectedEmojis,
                            state.customImagesLoading
                        )
                    }
                }
            }
        }
    }
}

private fun CustomImage.matches(query: String) = shortcode.contains(query, ignoreCase = true) || name.contains(query, ignoreCase = true)

@Composable
private fun EmojiResults(
    emojis: ImmutableList<Emoji>,
    isEmojiSelected: (Emoji) -> Boolean,
    onSelectEmoji: (Emoji) -> Unit,
    onLongPress: (Emoji) -> Unit,
    skinPickerEmoji: Emoji?,
    onDismissSkinPicker: () -> Unit,
    selectedEmojis: ImmutableSet<String>,
    contentDescription: @Composable (emoji: Emoji, isSelected: Boolean) -> String,
    customImages: List<CustomImage> = emptyList(),
    onSelectCustomEmoji: (String) -> Unit = {},
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = 48.dp),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(customImages) { image -> CustomEmojiItem(image, image.url in selectedEmojis, onSelectCustomEmoji) }
        items(emojis, key = { it.unicode }) { item ->
            EmojiItem(
                modifier = Modifier.aspectRatio(1f),
                item = item,
                isSelected = isEmojiSelected(item),
                onSelectEmoji = onSelectEmoji,
                onLongPress = onLongPress,
                skinPickerEmoji = skinPickerEmoji,
                onDismissSkinPicker = onDismissSkinPicker,
                emojiSize = 32.dp.toSp(),
                selectedSkinUnicodes = selectedEmojis,
                hasSelectedSkin = item.skins?.any { skin -> skin.unicode in selectedEmojis } == true,
                contentDescription = contentDescription,
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun EmojiPickerViewPreview(@PreviewParameter(DefaultEmojiPickerStatePreviewParam::class) state: DefaultEmojiPickerState) = ElementPreview {
    EmojiPickerView(
        state = state,
        onSelectEmoji = {},
        onSelectCustomEmoji = {}, // SC
        selectedEmojis = persistentSetOf("😀", "😄", "😃"),
        modifier = Modifier.fillMaxWidth(),
    )
}
