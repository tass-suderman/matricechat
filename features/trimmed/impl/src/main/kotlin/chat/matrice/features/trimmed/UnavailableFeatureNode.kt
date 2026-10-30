package chat.matrice.features.trimmed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text

internal fun featureUnavailable(): Result<Unit> = Result.failure(UnsupportedOperationException("Feature excluded from this build"))

internal class UnavailableFeatureNode(buildContext: BuildContext) : Node(buildContext) {
    @Composable
    override fun View(modifier: Modifier) {
        UnavailableFeatureView(modifier)
    }
}

@Composable
private fun UnavailableFeatureView(modifier: Modifier = Modifier) {
    Text(stringResource(chat.schildi.lib.R.string.common_feature_unavailable_trimmed), modifier = modifier)
}

@PreviewsDayNight
@Composable
private fun UnavailableFeaturePreview() = ElementPreview {
    UnavailableFeatureView()
}
