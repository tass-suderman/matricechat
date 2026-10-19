package chat.schildi.theme

import androidx.compose.runtime.Composable
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.userColor
import chat.schildi.lib.preferences.value
import io.element.android.compound.tokens.generated.SemanticColors

@Composable
internal fun SemanticColors.withCustomColors(): SemanticColors {
    if (!ScPrefs.CUSTOM_COLORS.value()) return this
    val canvas = (if (isLight) ScPrefs.CANVAS_LIGHT else ScPrefs.CANVAS_DARK).userColor()
    val surface = (if (isLight) ScPrefs.SURFACE_LIGHT else ScPrefs.SURFACE_DARK).userColor()
    val text = (if (isLight) ScPrefs.TEXT_LIGHT else ScPrefs.TEXT_DARK).userColor()
    val accent = (if (isLight) ScPrefs.ACCENT_LIGHT else ScPrefs.ACCENT_DARK).userColor()
    val link = (if (isLight) ScPrefs.LINK_LIGHT else ScPrefs.LINK_DARK).userColor()
    val accentIcon = (if (isLight) ScPrefs.ACCENT_ICON_LIGHT else ScPrefs.ACCENT_ICON_DARK).userColor()
    val accentBorder = (if (isLight) ScPrefs.ACCENT_BORDER_LIGHT else ScPrefs.ACCENT_BORDER_DARK).userColor()
    val badge = (if (isLight) ScPrefs.BADGE_LIGHT else ScPrefs.BADGE_DARK).userColor()
    val icon = (if (isLight) ScPrefs.ICON_LIGHT else ScPrefs.ICON_DARK).userColor()
    val border = (if (isLight) ScPrefs.BORDER_LIGHT else ScPrefs.BORDER_DARK).userColor()
    return copy(
        bgCanvasDefault = canvas ?: bgCanvasDefault,
        bgCanvasDefaultLevel1 = surface ?: bgCanvasDefaultLevel1,
        bgSubtlePrimary = surface ?: bgSubtlePrimary,
        bgSubtleSecondary = surface ?: bgSubtleSecondary,
        bgSubtleSecondaryLevel0 = surface ?: bgSubtleSecondaryLevel0,
        bgSubtleTertiary = surface ?: bgSubtleTertiary,
        textPrimary = text ?: textPrimary,
        textSecondary = text ?: textSecondary,
        textActionAccent = accent ?: textActionAccent,
        textLinkExternal = link ?: textLinkExternal,
        bgAccentRest = accent ?: bgAccentRest,
        bgActionPrimaryRest = accent ?: bgActionPrimaryRest,
        bgBadgeAccent = badge ?: bgBadgeAccent,
        iconAccentPrimary = accentIcon ?: iconAccentPrimary,
        iconAccentTertiary = accentIcon ?: iconAccentTertiary,
        iconPrimary = icon ?: iconPrimary,
        iconSecondary = icon ?: iconSecondary,
        iconTertiary = icon ?: iconTertiary,
        borderInteractivePrimary = border ?: borderInteractivePrimary,
        borderInteractiveSecondary = border ?: borderInteractiveSecondary,
        borderAccentPrimary = accentBorder ?: borderAccentPrimary,
        borderFocused = accentBorder ?: borderFocused,
    )
}
