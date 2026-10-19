/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.TimelineItemGroupPosition
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent

class FlatMessageLayoutPreviewParam : PreviewParameterProvider<TimelineItem.Event> {
    override val values = sequenceOf(false, true).flatMap { isMine ->
        sequenceOf(TimelineItemGroupPosition.None, TimelineItemGroupPosition.First, TimelineItemGroupPosition.Middle, TimelineItemGroupPosition.Last).map { position ->
            aTimelineItemEvent(isMine = isMine, groupPosition = position)
        }
    }
}
