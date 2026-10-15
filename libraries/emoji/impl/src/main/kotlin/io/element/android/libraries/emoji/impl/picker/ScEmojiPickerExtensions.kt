package io.element.android.libraries.emoji.impl.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import chat.schildi.lib.R
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun FreeformReactionView(onSelectCustomEmoji: (String) -> Unit) {
    val text = remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Top) {
        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            modifier = Modifier.focusRequester(focusRequester).weight(1f),
            singleLine = true,
            label = { Text(stringResource(R.string.sc_freeform_reaction)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSelectCustomEmoji(text.value) }),
        )
        SendButton(Modifier.align(Alignment.Bottom)) { onSelectCustomEmoji(text.value) }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

// More or less a copy of upstream's SendButton which is not available here
@Composable
private fun SendButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier
            .size(48.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(36.dp)
                .background(ElementTheme.colors.iconAccentTertiary)
        ) {
            Icon(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .align(Alignment.Center),
                imageVector = CompoundIcons.Send(),
                contentDescription = stringResource(CommonStrings.action_send),
                tint = Color.White,
            )
        }
    }
}
