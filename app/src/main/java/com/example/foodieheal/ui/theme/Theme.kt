package com.example.foodieheal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0XFFF07F60),
    onPrimary = Color.White,

    secondary = Color(0XFF7BA889),
    onSecondary = Color.White,

    tertiary = Color(0xFF2E2E2E),     // neutral card/surface tone
    onTertiary = Color.White,

    surface = Color(0XFF1E1E1E),      // general surface background
    onSurface = Color(0XFFE6E6E6),    // soft white text (not too bright)

    background = Color(0XFF1C1C1C),   // main dark background
    onBackground = Color(0XFFF2F2F2),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0XFFEC5E3A),
    onPrimary = Color.White,

    secondary = Color(0XFF5D8068),
    onSecondary = Color.White,

    tertiary = Color(0XFFE2E2E2),     // neutral card/surface tone
    onTertiary = Color.Black,

    surface = Color(0XFFFFFFFF),      // standard white surface
    onSurface = Color(0XFF1B1B1B),

    background = Color(0XFFF2F2F2),   // soft light background
    onBackground = Color(0XFF1B1B1B),
)

@Composable
fun MobileAssignmentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}