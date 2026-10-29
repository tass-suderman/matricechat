package io.element.android.features.userprofile.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chat.schildi.components.preferences.ColorPickerAlertDialog
import chat.schildi.components.preferences.PreferenceColorPreview
import chat.schildi.lib.preferences.FriendProfile
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.matrix.ui.friends.FriendProfiles
import io.element.android.libraries.matrix.ui.friends.LocalFriendProfiles
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import chat.schildi.lib.R as ScR

@Composable
internal fun FriendProfileSection(userId: String) {
    val friends = LocalFriendProfiles.current
    val save = friends.save ?: return
    var editing by remember(userId) { mutableStateOf(false) }
    var profile by remember(userId) { mutableStateOf(FriendProfile()) }
    var choosingColor by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ListItem(
        content = { Text(stringResource(ScR.string.screen_friend_profile_title)) },
        supportingContent = { Text(stringResource(ScR.string.screen_friend_profile_summary)) },
        onClick = {
            profile = friends.profiles[userId] ?: FriendProfile()
            failed = false
            editing = true
        },
    )
    if (editing) {
        Dialog(
            onDismissRequest = { if (!saving) editing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            PreferencePage(
                title = stringResource(ScR.string.screen_friend_profile_title),
                onBackClick = { if (!saving) editing = false },
            ) {
                FriendProfileEditor(
                    profile = profile,
                    saving = saving,
                    failed = failed,
                    onChange = { profile = it },
                    onChooseColor = { choosingColor = true },
                    onSave = {
                        saving = true
                        scope.launch {
                            try {
                                save(userId, profile)
                                editing = false
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failed = true
                            } finally {
                                saving = false
                            }
                        }
                    },
                )
            }
        }
    }
    if (choosingColor) {
        ColorPickerAlertDialog(
        initialValue = profile.color?.let(::Color) ?: ElementTheme.colors.textPrimary,
        defaultValue = 0,
        dismiss = { choosingColor = false },
        onChange = { profile = profile.copy(color = it.takeUnless { value -> value == 0 }) },
    )
    }
}

@Composable
private fun FriendProfileEditor(
    profile: FriendProfile,
    saving: Boolean,
    failed: Boolean,
    onChange: (FriendProfile) -> Unit,
    onChooseColor: () -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(ScR.string.screen_friend_profile_summary))
        TextField(
            value = profile.nickname,
            onValueChange = { onChange(profile.copy(nickname = it.replace("\n", "").replace("\r", "").take(100))) },
            label = stringResource(ScR.string.screen_friend_profile_nickname),
            singleLine = true,
            enabled = !saving,
        )
        PreferenceColorPreview(
            title = stringResource(ScR.string.screen_friend_profile_color),
            currentValue = profile.color?.let(::Color),
            enabled = !saving,
            onClick = onChooseColor,
        )
        TextButton(
            text = stringResource(CommonStrings.action_reset),
            enabled = !saving,
            onClick = { onChange(FriendProfile()) },
        )
        if (failed) Text(stringResource(ScR.string.error_friend_profile_save), color = ElementTheme.colors.textCriticalPrimary)
        TextButton(
            text = stringResource(CommonStrings.action_save),
            enabled = !saving,
            onClick = onSave,
        )
    }
}

internal class FriendProfilePreviewParam : PreviewParameterProvider<Pair<FriendProfile, String>> {
    override val values = sequenceOf(
        FriendProfile() to "empty",
        FriendProfile("Best friend", 0xffc45ad4.toInt()) to "saved",
        FriendProfile("Best friend") to "saving",
        FriendProfile("Best friend") to "error",
    )
}

@PreviewsDayNight
@Composable
internal fun FriendProfileEditorPreview(
    @PreviewParameter(FriendProfilePreviewParam::class) state: Pair<FriendProfile, String>,
) = ElementPreview {
    PreferencePage(title = "Friend appearance", onBackClick = {}) {
        FriendProfileEditor(state.first, state.second == "saving", state.second == "error", {}, {}, {})
    }
}

@PreviewsDayNight
@Composable
internal fun FriendProfileSectionPreview() = ElementPreview {
    CompositionLocalProvider(LocalFriendProfiles provides FriendProfiles(save = { _, _ -> })) {
        FriendProfileSection("@friend:example.org")
    }
}
