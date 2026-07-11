package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF),      // Softer, elegant purple/lavender
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF121115),    // Deep obsidian black for maximum eye comfort
    surface = Color(0xFF1C1A20),       // Sophisticated dark card background
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2930), // Comfortable dark slate card surface
    onSurfaceVariant = Color(0xFFE6E1E5)
  )

private val LightColorScheme = DarkColorScheme // Force dark theme by default even in light mode to protect eyes during long roleplay reading sessions

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark mode as standard default for roleplay/novel reading
  // Dynamic color is available on Android 12+, disabled by default to maintain obsidian/deep purple theme consistency
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
