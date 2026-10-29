/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import chat.schildi.lib.preferences.ScPreferencesStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.home.impl.datasource.RoomListRoomSummaryFactory
import io.element.android.features.home.impl.datasource.applyInviteFilterSetting
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.matrix.api.roomlist.RoomList
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.roomlist.updateVisibleRange
import io.element.android.libraries.matrix.ui.friends.FriendProfiles
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private const val PAGE_SIZE = 30

@AssistedInject
class RoomListSearchDataSource(
    @Assisted coroutineScope: CoroutineScope,
    private val roomListService: RoomListService,
    coroutineDispatchers: CoroutineDispatchers,
    scPreferencesStore: ScPreferencesStore,
    private val roomSummaryFactory: RoomListRoomSummaryFactory,
) {
    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): RoomListSearchDataSource
    }

    private val roomList = roomListService.createRoomList(
        pageSize = PAGE_SIZE,
        source = RoomList.Source.All,
        coroutineScope = coroutineScope
    )

    val loadingState = roomList.loadingState

    val roomSummaries = roomList.summaries
        .map { roomSummaries ->
            roomSummaries
                .map(roomSummaryFactory::create)
                .toImmutableList()
        }
        .applyInviteFilterSetting(scPreferencesStore)
        .flowOn(coroutineDispatchers.computation)

    suspend fun updateVisibleRange(visibleRange: IntRange) {
        roomList.updateVisibleRange(visibleRange)
    }

    suspend fun setSearchQuery(
        searchQuery: String,
        additionalFilters: RoomListFilter?,
        friends: FriendProfiles = FriendProfiles(),
    ) = coroutineScope {
        roomListService.allRooms.summaries.onStart { emit(emptyList()) }.collectLatest { summaries ->
            val matchingRooms = summaries
                .filter { summary ->
                    val info = summary.info
                    info.isDm && friends.roomName(info)?.contains(searchQuery, ignoreCase = true) == true
                }.map { it.info.id }
            val nameFilter = if (matchingRooms.isEmpty()) {
                RoomListFilter.NormalizedMatchRoomName(searchQuery)
            } else {
                RoomListFilter.any(RoomListFilter.NormalizedMatchRoomName(searchQuery), RoomListFilter.Identifiers(matchingRooms))
            }
            val filter = if (searchQuery.isBlank()) {
                RoomListFilter.None
            } else {
                if (additionalFilters != null) {
                    RoomListFilter.all(additionalFilters, nameFilter)
                } else {
                    nameFilter
                }
            }
            roomList.updateFilter(filter)
        }
    }
}
