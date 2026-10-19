package chat.schildi.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import chat.schildi.lib.preferences.ScColorPref
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.userColor
import chat.schildi.lib.preferences.value
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.compound.theme.isDark
import io.element.android.compound.theme.toMaterialColorScheme
import io.element.android.compound.tokens.sc.ElTypographyTokens
import io.element.android.compound.tokens.sc.ExposedTypographyTokens

object ScTheme {
    val exposures: ScThemeExposures
        @Composable
        @ReadOnlyComposable
        get() = LocalScExposures.current

    val yes: Boolean
        @Composable
        @ReadOnlyComposable
        get() = exposures.isScTheme

    val scTimeline: Boolean
        @Composable
        get() = ScPrefs.SC_TIMELINE_LAYOUT.value()

    val bubbleBgIncoming: Color?
        @Composable
        get() = getUserThemedColor(ScPrefs.BUBBLE_BG_LIGHT_INCOMING, ScPrefs.BUBBLE_BG_DARK_INCOMING) ?: exposures.bubbleBgIncoming

    val bubbleBgOutgoing: Color?
        @Composable
        get() = getUserThemedColor(ScPrefs.BUBBLE_BG_LIGHT_OUTGOING, ScPrefs.BUBBLE_BG_DARK_OUTGOING) ?: exposures.bubbleBgOutgoing

    val unreadNotificationColor: Color
        @Composable
        get() {
            getUserThemedColor(ScPrefs.UNREAD_LIGHT, ScPrefs.UNREAD_DARK)?.let { return it }
            // Unread indicators share an upstream icon token, but have an independent custom colour.
            val baseColors = if (ScPrefs.EL_THEME.value()) {
                if (ElementTheme.isLightTheme) elColorsLight else elColorsDark
            } else if (ElementTheme.isLightTheme) {
                sclSemanticColors
            } else if (ScPrefs.BLACK_THEME.value()) {
                scbSemanticColors
            } else {
                scdSemanticColors
            }
            return baseColors.iconAccentTertiary
        }

    val mentionBadgeColor: Color
        @Composable
        get() = getUserThemedColor(ScPrefs.MENTION_LIGHT, ScPrefs.MENTION_DARK) ?: ElementTheme.colors.bgCriticalPrimary

    @Composable
    private fun getUserThemedColor(lightPref: ScColorPref, darkPref: ScColorPref): Color? =
        if (ScPrefs.CUSTOM_COLORS.value()) (if (ElementTheme.isLightTheme) lightPref else darkPref).userColor() else null
}

// Element defaults to light compound colors, so follow that as fallback default for exposures as well
internal val LocalScExposures = staticCompositionLocalOf { elementLightScExposures }

fun getThemeExposures(darkTheme: Boolean, useElTheme: Boolean, useBlackTheme: Boolean) = when {
    useElTheme -> if (darkTheme) elementDarkScExposures else elementLightScExposures
    darkTheme -> if (useBlackTheme) scbExposures else scdExposures
    else -> sclExposures
}

@Composable
fun ScTheme(
    theme: Theme = if (isSystemInDarkTheme()) Theme.Dark else Theme.Light,
    applySystemBarsUpdate: Boolean = true,
    lightStatusBar: Boolean = !theme.isDark(),
    dynamicColor: Boolean = false, // true to enable MaterialYou
    useElTypography: Boolean = ScPrefs.EL_TYPOGRAPHY.value(),
    content: @Composable () -> Unit,
) {
    val darkTheme = theme.isDark()
    val typography = if (useElTypography) elTypography else scTypography
    val typographyTokens = if (useElTypography) ElTypographyTokens else ScTypographyTokens
    val useElTheme = ScPrefs.EL_THEME.value()
    val useBlackTheme = ScPrefs.BLACK_THEME.value()

    val customColorsEnabled = ScPrefs.CUSTOM_COLORS.value()
    val surface = if (customColorsEnabled) (if (darkTheme) ScPrefs.SURFACE_DARK else ScPrefs.SURFACE_LIGHT).userColor() else null
    val text = if (customColorsEnabled) (if (darkTheme) ScPrefs.TEXT_DARK else ScPrefs.TEXT_LIGHT).userColor() else null
    val mutedUnread = if (customColorsEnabled) (if (darkTheme) ScPrefs.MUTED_UNREAD_DARK else ScPrefs.MUTED_UNREAD_LIGHT).userColor() else null
    val badge = if (customColorsEnabled) (if (darkTheme) ScPrefs.BADGE_DARK else ScPrefs.BADGE_LIGHT).userColor() else null
    val baseExposures = getThemeExposures(darkTheme, useElTheme, useBlackTheme)
    val currentExposures = remember {
        // EleLight is default
        elementLightScExposures.copy()
    }.apply {
        updateColorsFrom(baseExposures.copy(
        appBarBg = surface ?: baseExposures.appBarBg,
        spaceBarBg = surface ?: baseExposures.spaceBarBg,
        composerBlockBg = surface ?: baseExposures.composerBlockBg,
        composerBlockFg = text ?: baseExposures.composerBlockFg,
        tertiaryFgNoAlpha = text ?: baseExposures.tertiaryFgNoAlpha,
        greenBg = badge ?: baseExposures.greenBg,
        unreadBadgeColor = mutedUnread ?: baseExposures.unreadBadgeColor,
        unreadBadgeOnToolbarColor = mutedUnread ?: baseExposures.unreadBadgeOnToolbarColor,
    ))
    }

    val lightColors = (if (useElTheme) elColorsLight else sclSemanticColors).withCustomColors()
    val darkColors = (if (useElTheme) {
        elColorsDark
    } else if (useBlackTheme) {
        scbSemanticColors
    } else {
        scdSemanticColors
    }).withCustomColors()
    CompositionLocalProvider(
        LocalScExposures provides currentExposures
    ) {
        ElementTheme(
            theme = theme,
            applySystemBarsUpdate = applySystemBarsUpdate,
            lightStatusBar = lightStatusBar,
            dynamicColor = dynamicColor,
            compoundLight = lightColors,
            compoundDark = darkColors,
            materialColorsLight = lightColors.toMaterialColorScheme(),
            materialColorsDark = darkColors.toMaterialColorScheme(),
            typography = typography,
            typographyTokens = typographyTokens,
            content = content,
        )
    }
}

/**
 * Can be used to force a composable in dark theme.
 * It will automatically change the system ui colors back to normal when leaving the composition.
 * (Copy of io.element.android.compound.theme.ForcedDarkElementTheme but using ScTheme)
 */
@Composable
fun ForcedDarkScTheme(
    lightStatusBar: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val wasDarkTheme = !ElementTheme.colors.isLight
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(Unit) {
        onDispose {
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = colorScheme.background.toArgb(),
                    darkScrim = colorScheme.background.toArgb(),
                ),
                navigationBarStyle = if (wasDarkTheme) {
                    SystemBarStyle.dark(Color.Transparent.toArgb())
                } else {
                    SystemBarStyle.light(
                        scrim = Color.Transparent.toArgb(),
                        darkScrim = Color.Transparent.toArgb()
                    )
                }
            )
        }
    }
    ScTheme(theme = Theme.Dark, lightStatusBar = lightStatusBar, content = content)
}

// Calculate the color as if with alpha on white background
fun Color.fakeAlpha(alpha: Float) = Color(
    1f - alpha * (1f - red),
    1f - alpha * (1f - green),
    1f - alpha * (1f - blue),
    1f,
)

val ExposedTypographyTokens.scBubbleFont
    @Composable
    get() = if (ScPrefs.EL_TYPOGRAPHY.value()) fontBodyLgRegular else fontBodyMdRegular
val ExposedTypographyTokens.scBubbleSmallFont
    @Composable
    get() = if (ScPrefs.EL_TYPOGRAPHY.value()) fontBodyXsRegular else fontBodySmRegular
