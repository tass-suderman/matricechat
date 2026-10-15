package io.element.android.features.preferences.impl.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferenceCategory
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.preferences.api.SettingsBackupService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun SettingsBackupSection(service: SettingsBackupService) {
    var state by remember { mutableStateOf(SettingsBackupUiState()) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            state = SettingsBackupUiState(busy = true)
            scope.launch {
                val result = service.exportSettings(uri)
                state = SettingsBackupUiState(notice = if (result.isSuccess) BackupNotice.Exported else BackupNotice.Failed)
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }
    SettingsBackupSectionContent(
        state = state,
        onExport = {
            state = SettingsBackupUiState()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
            exporter.launch("matrix-client-settings-$date.zip")
        },
        onImport = {
            state = SettingsBackupUiState()
            importer.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        },
    )
    pendingImport?.let { source ->
        ConfirmationDialog(
            title = stringResource(R.string.screen_settings_backup_import),
            content = stringResource(R.string.screen_settings_backup_import_confirmation),
            submitText = stringResource(R.string.screen_settings_backup_import),
            onDismiss = { pendingImport = null },
            onSubmitClick = {
                pendingImport = null
                state = SettingsBackupUiState(busy = true)
                scope.launch {
                    val result = service.importSettings(source)
                    state = SettingsBackupUiState(notice = if (result.isSuccess) BackupNotice.Imported else BackupNotice.Failed)
                }
            },
        )
    }
}

internal enum class BackupNotice { Exported, Imported, Failed }
internal data class SettingsBackupUiState(val busy: Boolean = false, val notice: BackupNotice? = null)

@Composable
private fun SettingsBackupSectionContent(state: SettingsBackupUiState, onExport: () -> Unit, onImport: () -> Unit) {
    PreferenceCategory(title = stringResource(R.string.screen_settings_backup_title)) {
        ListItem(
            content = { Text(stringResource(R.string.screen_settings_backup_export)) },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.ExportArchive())),
            supportingContent = { Text(stringResource(R.string.screen_settings_backup_summary)) },
            enabled = !state.busy,
            onClick = onExport,
        )
        ListItem(
            content = { Text(stringResource(R.string.screen_settings_backup_import)) },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Download())),
            enabled = !state.busy,
            onClick = onImport,
        )
        if (state.busy) CircularProgressIndicator()
        val message = when (state.notice) {
            BackupNotice.Exported -> R.string.screen_settings_backup_exported
            BackupNotice.Imported -> R.string.screen_settings_backup_imported
            BackupNotice.Failed -> R.string.error_settings_backup
            null -> null
        }
        message?.let { Text(stringResource(it)) }
    }
}

internal class SettingsBackupPreviewParam : PreviewParameterProvider<SettingsBackupUiState> {
    override val values = sequenceOf(
        SettingsBackupUiState(), SettingsBackupUiState(busy = true),
        SettingsBackupUiState(notice = BackupNotice.Exported), SettingsBackupUiState(notice = BackupNotice.Imported),
        SettingsBackupUiState(notice = BackupNotice.Failed),
    )
}

@PreviewsDayNight
@Composable
internal fun SettingsBackupSectionPreview(@PreviewParameter(SettingsBackupPreviewParam::class) state: SettingsBackupUiState) = ElementPreview {
    SettingsBackupSectionContent(state, {}, {})
}
