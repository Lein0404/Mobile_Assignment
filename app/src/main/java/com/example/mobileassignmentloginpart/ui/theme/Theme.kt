package com.example.mobileassignmentloginpart.ui.theme

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
    secondary = Color(0XFF7BA889),
    tertiary = Color(0XFFEBB353),
    surface = Color(0xFF303030),
    background = Color(0xFF181818),
    onPrimary = Color(0XFFFFFFFF),
    onSecondary = Color(0XFFFFFFFF),
    onTertiary = Color(0XFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onBackground = Color(0xFFD0D0D0),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0XFFEC5E3A),
    secondary = Color(0XFF5D8068),
    tertiary = Color(0XFFD29221),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFE5E5E5),
    onPrimary = Color(0XFFFFFFFF),
    onSecondary = Color(0XFFFFFFFF),
    onTertiary = Color(0XFFFFFFFF),
    onSurface = Color(0xFF4A4A4A),
    onBackground = Color(0xFF1B1B1B),
)

@Composable
fun MobileAssignmentLoginPartTheme(
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