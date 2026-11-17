package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SwarDarkColorScheme = darkColorScheme(
  primary = SwarCyan,
  onPrimary = SwarDeepNight,
  primaryContainer = SwarStageIndigo,
  onPrimaryContainer = SwarCyan,
  secondary = SwarViolet,
  onSecondary = Color.White,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = Color.White,
  tertiary = SwarCoral,
  onTertiary = Color.White,
  background = DarkBg,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceMuted,
)

private val SwarLightColorScheme = lightColorScheme(
  primary = SwarCyanDark,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFDFF4FF),
  onPrimaryContainer = Color(0xFF00344C),
  secondary = SwarViolet,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFEDE7FF),
  onSecondaryContainer = Color(0xFF280B6B),
  tertiary = SwarCoral,
  onTertiary = Color.White,
  background = LightBg,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceMuted,
)

@Composable
fun SwarRoomTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) SwarDarkColorScheme else SwarLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = SwarTypography,
    content = content,
  )
}
