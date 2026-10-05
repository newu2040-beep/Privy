package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class PastelPalette(val displayName: String, val previewColor: Color) {
    CLASSIC("Slate", Color(0xFF1E293B)),
    LILAC("Lilac", Color(0xFFC084FC)),
    MINT("Mint", Color(0xFF6EE7B7)),
    PEACH("Peach", Color(0xFFFDA4AF)),
    SKY("Sky", Color(0xFF7DD3FC)),
    BUTTER("Honey", Color(0xFFFCD34D))
}

fun getPastelColorScheme(palette: PastelPalette, isDark: Boolean): ColorScheme {
    return if (isDark) {
        when (palette) {
            PastelPalette.CLASSIC -> darkColorScheme(
                primary = Color.White,
                onPrimary = PrivyOffBlack,
                primaryContainer = PrivyCardElevatedDark,
                onPrimaryContainer = Color.White,
                secondary = PrivyBlueDark,
                onSecondary = Color.White,
                secondaryContainer = PrivyCardElevatedDark,
                onSecondaryContainer = PrivyBlueDark,
                background = PrivyDarkBg,
                onBackground = PrivyTextDark,
                surface = PrivyCardDark,
                onSurface = PrivyTextDark,
                surfaceVariant = PrivyCardElevatedDark,
                onSurfaceVariant = PrivyMutedDark,
                outline = PrivyBorderDark,
                outlineVariant = PrivyBorderDark.copy(alpha = 0.5f)
            )
            PastelPalette.LILAC -> darkColorScheme(
                primary = PastelLilacDarkAccent,
                onPrimary = Color.Black,
                primaryContainer = PastelLilacDarkCard,
                onPrimaryContainer = PastelLilacDarkAccent,
                secondary = PastelLilacSecondary,
                onSecondary = Color.White,
                background = PastelLilacDarkBg,
                onBackground = Color(0xFFF5EEFF),
                surface = PastelLilacDarkCard,
                onSurface = Color(0xFFF5EEFF),
                surfaceVariant = Color(0xFF281F38),
                onSurfaceVariant = Color(0xFFD8B4FE),
                outline = Color(0xFF3B2D54),
                outlineVariant = Color(0xFF3B2D54).copy(alpha = 0.5f)
            )
            PastelPalette.MINT -> darkColorScheme(
                primary = PastelMintDarkAccent,
                onPrimary = Color.Black,
                primaryContainer = PastelMintDarkCard,
                onPrimaryContainer = PastelMintDarkAccent,
                secondary = PastelMintSecondary,
                onSecondary = Color.White,
                background = PastelMintDarkBg,
                onBackground = Color(0xFFEEFDF7),
                surface = PastelMintDarkCard,
                onSurface = Color(0xFFEEFDF7),
                surfaceVariant = Color(0xFF1B362E),
                onSurfaceVariant = Color(0xFFA7F3D0),
                outline = Color(0xFF244A3E),
                outlineVariant = Color(0xFF244A3E).copy(alpha = 0.5f)
            )
            PastelPalette.PEACH -> darkColorScheme(
                primary = PastelPeachDarkAccent,
                onPrimary = Color.Black,
                primaryContainer = PastelPeachDarkCard,
                onPrimaryContainer = PastelPeachDarkAccent,
                secondary = PastelPeachSecondary,
                onSecondary = Color.White,
                background = PastelPeachDarkBg,
                onBackground = Color(0xFFFFF1F2),
                surface = PastelPeachDarkCard,
                onSurface = Color(0xFFFFF1F2),
                surfaceVariant = Color(0xFF3C2029),
                onSurfaceVariant = Color(0xFFFECDD3),
                outline = Color(0xFF502B37),
                outlineVariant = Color(0xFF502B37).copy(alpha = 0.5f)
            )
            PastelPalette.SKY -> darkColorScheme(
                primary = PastelSkyDarkAccent,
                onPrimary = Color.Black,
                primaryContainer = PastelSkyDarkCard,
                onPrimaryContainer = PastelSkyDarkAccent,
                secondary = PastelSkySecondary,
                onSecondary = Color.White,
                background = PastelSkyDarkBg,
                onBackground = Color(0xFFF0F9FF),
                surface = PastelSkyDarkCard,
                onSurface = Color(0xFFF0F9FF),
                surfaceVariant = Color(0xFF1C3249),
                onSurfaceVariant = Color(0xFFBAE6FD),
                outline = Color(0xFF264463),
                outlineVariant = Color(0xFF264463).copy(alpha = 0.5f)
            )
            PastelPalette.BUTTER -> darkColorScheme(
                primary = PastelButterDarkAccent,
                onPrimary = Color.Black,
                primaryContainer = PastelButterDarkCard,
                onPrimaryContainer = PastelButterDarkAccent,
                secondary = PastelButterSecondary,
                onSecondary = Color.White,
                background = PastelButterDarkBg,
                onBackground = Color(0xFFFFFBEB),
                surface = PastelButterDarkCard,
                onSurface = Color(0xFFFFFBEB),
                surfaceVariant = Color(0xFF382D16),
                onSurfaceVariant = Color(0xFFFDE68A),
                outline = Color(0xFF4C3D1E),
                outlineVariant = Color(0xFF4C3D1E).copy(alpha = 0.5f)
            )
        }
    } else {
        when (palette) {
            PastelPalette.CLASSIC -> lightColorScheme(
                primary = PrivyDarkCharcoal,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF3F4F6),
                onPrimaryContainer = PrivyDarkCharcoal,
                secondary = PrivyBlue,
                onSecondary = Color.White,
                secondaryContainer = PrivyBlueLight,
                onSecondaryContainer = PrivyBlue,
                background = PrivyLightBg,
                onBackground = PrivyDarkCharcoal,
                surface = PrivyCardLight,
                onSurface = PrivyDarkCharcoal,
                surfaceVariant = Color(0xFFF3F4F6),
                onSurfaceVariant = PrivyMutedLight,
                outline = PrivyBorderLight,
                outlineVariant = PrivyBorderLight.copy(alpha = 0.7f)
            )
            PastelPalette.LILAC -> lightColorScheme(
                primary = PastelLilacPrimary,
                onPrimary = Color.White,
                primaryContainer = PastelLilacAccent,
                onPrimaryContainer = PastelLilacPrimary,
                secondary = PastelLilacSecondary,
                onSecondary = Color.White,
                background = PastelLilacBg,
                onBackground = Color(0xFF2E1065),
                surface = PastelLilacCard,
                onSurface = Color(0xFF2E1065),
                surfaceVariant = Color(0xFFF3E8FF),
                onSurfaceVariant = Color(0xFF6B21A8),
                outline = Color(0xFFE9D5FF),
                outlineVariant = Color(0xFFE9D5FF).copy(alpha = 0.7f)
            )
            PastelPalette.MINT -> lightColorScheme(
                primary = PastelMintPrimary,
                onPrimary = Color.White,
                primaryContainer = PastelMintAccent,
                onPrimaryContainer = PastelMintPrimary,
                secondary = PastelMintSecondary,
                onSecondary = Color.White,
                background = PastelMintBg,
                onBackground = Color(0xFF064E3B),
                surface = PastelMintCard,
                onSurface = Color(0xFF064E3B),
                surfaceVariant = Color(0xFFD1FAE5),
                onSurfaceVariant = Color(0xFF047857),
                outline = Color(0xFFA7F3D0),
                outlineVariant = Color(0xFFA7F3D0).copy(alpha = 0.7f)
            )
            PastelPalette.PEACH -> lightColorScheme(
                primary = PastelPeachPrimary,
                onPrimary = Color.White,
                primaryContainer = PastelPeachAccent,
                onPrimaryContainer = PastelPeachPrimary,
                secondary = PastelPeachSecondary,
                onSecondary = Color.White,
                background = PastelPeachBg,
                onBackground = Color(0xFF881337),
                surface = PastelPeachCard,
                onSurface = Color(0xFF881337),
                surfaceVariant = Color(0xFFFFE4E6),
                onSurfaceVariant = Color(0xFFBE123C),
                outline = Color(0xFFFECDD3),
                outlineVariant = Color(0xFFFECDD3).copy(alpha = 0.7f)
            )
            PastelPalette.SKY -> lightColorScheme(
                primary = PastelSkyPrimary,
                onPrimary = Color.White,
                primaryContainer = PastelSkyAccent,
                onPrimaryContainer = PastelSkyPrimary,
                secondary = PastelSkySecondary,
                onSecondary = Color.White,
                background = PastelSkyBg,
                onBackground = Color(0xFF0C4A6E),
                surface = PastelSkyCard,
                onSurface = Color(0xFF0C4A6E),
                surfaceVariant = Color(0xFFE0F2FE),
                onSurfaceVariant = Color(0xFF0369A1),
                outline = Color(0xFFBAE6FD),
                outlineVariant = Color(0xFFBAE6FD).copy(alpha = 0.7f)
            )
            PastelPalette.BUTTER -> lightColorScheme(
                primary = PastelButterPrimary,
                onPrimary = Color.White,
                primaryContainer = PastelButterAccent,
                onPrimaryContainer = PastelButterPrimary,
                secondary = PastelButterSecondary,
                onSecondary = Color.White,
                background = PastelButterBg,
                onBackground = Color(0xFF78350F),
                surface = PastelButterCard,
                onSurface = Color(0xFF78350F),
                surfaceVariant = Color(0xFFFEF3C7),
                onSurfaceVariant = Color(0xFFB45309),
                outline = Color(0xFFFDE68A),
                outlineVariant = Color(0xFFFDE68A).copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PrivyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pastelPalette: PastelPalette = PastelPalette.CLASSIC,
    content: @Composable () -> Unit
) {
    val colorScheme = getPastelColorScheme(pastelPalette, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
