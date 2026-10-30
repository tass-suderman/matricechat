package chat.matrice.features.trimmed

import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.poll.api.actions.EndPollAction
import io.element.android.features.poll.api.actions.SendPollResponseAction
import io.element.android.features.poll.api.create.CreatePollEntryPoint
import io.element.android.features.poll.api.history.PollHistoryEntryPoint
import io.element.android.features.poll.api.pollcontent.PollContentState
import io.element.android.features.poll.api.pollcontent.PollContentStateFactory
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent
import kotlinx.collections.immutable.persistentListOf

@ContributesBinding(AppScope::class)
class DisabledCreatePoll : CreatePollEntryPoint {
    override fun createNode(parentNode: Node, buildContext: BuildContext, params: CreatePollEntryPoint.Params): Node =
        UnavailableFeatureNode(buildContext)
}

@ContributesBinding(AppScope::class)
class DisabledPollHistory : PollHistoryEntryPoint {
    override fun createNode(parentNode: Node, buildContext: BuildContext): Node = UnavailableFeatureNode(buildContext)
}

@ContributesBinding(RoomScope::class)
class DisabledEndPoll : EndPollAction {
    override suspend fun execute(timeline: Timeline, pollStartId: EventId): Result<Unit> = featureUnavailable()
}

@ContributesBinding(RoomScope::class)
class DisabledPollResponse : SendPollResponseAction {
    override suspend fun execute(timeline: Timeline, pollStartId: EventId, answerIds: List<String>): Result<Unit> = featureUnavailable()
}

@ContributesBinding(RoomScope::class)
class DisabledPollContent : PollContentStateFactory {
    override suspend fun create(eventId: EventId?, isEditable: Boolean, isOwn: Boolean, content: PollContent): PollContentState =
        PollContentState(
            eventId = eventId,
            question = content.question,
            answerItems = persistentListOf(),
            pollKind = content.kind,
            maxSelections = content.maxSelections,
            isPollEditable = false,
            isPollEnded = true,
            isMine = isOwn,
        )
}
