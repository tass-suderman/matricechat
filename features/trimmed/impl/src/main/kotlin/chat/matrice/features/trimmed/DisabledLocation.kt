package chat.matrice.features.trimmed

import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.enterprise.api.remoteconfig.MapTilerConfig
import io.element.android.features.location.api.LocationService
import io.element.android.features.location.api.ShareLocationEntryPoint
import io.element.android.features.location.api.ShowLocationEntryPoint
import io.element.android.features.location.api.live.ActiveLiveLocationShareManager
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.timeline.Timeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

@ContributesBinding(AppScope::class)
class DisabledLocationService : LocationService {
    override val mapTilerConfig: StateFlow<MapTilerConfig?> = MutableStateFlow(null)
}

@ContributesBinding(AppScope::class)
class DisabledLiveLocation : ActiveLiveLocationShareManager {
    override val sharingRoomIds: StateFlow<Set<RoomId>> = MutableStateFlow(emptySet())
    override suspend fun setup() = Unit
    override suspend fun startShare(roomId: RoomId, duration: Duration): Result<Unit> = featureUnavailable()
    override suspend fun stopShare(roomId: RoomId): Result<Unit> = featureUnavailable()
}

@ContributesBinding(AppScope::class)
class DisabledShareLocation : ShareLocationEntryPoint {
    override fun createNode(parentNode: Node, buildContext: BuildContext, timelineMode: Timeline.Mode): Node =
        UnavailableFeatureNode(buildContext)
}

@ContributesBinding(AppScope::class)
class DisabledShowLocation : ShowLocationEntryPoint {
    override fun createNode(parentNode: Node, buildContext: BuildContext, inputs: ShowLocationEntryPoint.Inputs): Node =
        UnavailableFeatureNode(buildContext)
}
