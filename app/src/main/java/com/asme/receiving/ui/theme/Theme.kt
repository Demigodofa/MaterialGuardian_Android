package com.asme.receiving.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.asme.receiving.ui.MaterialGuardianColors

private val DarkColorScheme = darkColorScheme(
    primary = MaterialGuardianColors.PrimaryButton,
    secondary = MaterialGuardianColors.ExportButton,
    background = MaterialGuardianColors.ScreenBackground,
    surface = MaterialGuardianColors.CardBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = MaterialGuardianColors.PrimaryButton,
    onPrimary = MaterialGuardianColors.PrimaryButtonText,
    secondary = MaterialGuardianColors.ExportButton,
    onSecondary = MaterialGuardianColors.ExportButtonText,
    background = MaterialGuardianColors.ScreenBackground,
    onBackground = MaterialGuardianColors.TextPrimary,
    surface = MaterialGuardianColors.CardBackground,
    onSurface = MaterialGuardianColors.TextPrimary,
    error = MaterialGuardianColors.DeleteButton,
    onError = MaterialGuardianColors.DeleteButtonText,
)

@Composable
fun MaterialGuardianTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
