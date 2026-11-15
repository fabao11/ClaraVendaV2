package pt.vendaclara.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Claro = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF475569), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0), onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFB45309), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7), onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF4F8F7), onBackground = Color(0xFF0F1B1A),
    surface = Color(0xFFF4F8F7), onSurface = Color(0xFF0F1B1A),
    surfaceVariant = Color(0xFFE0EBE9), onSurfaceVariant = Color(0xFF3F4F4D),
    outline = Color(0xFF7A8F8C), outlineVariant = Color(0xFFC7D6D3),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFCDAD7), onErrorContainer = Color(0xFF410002),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF9FCFB),
    surfaceContainer = Color(0xFFEEF5F4), surfaceContainerHigh = Color(0xFFE8F0EF),
    surfaceContainerHighest = Color(0xFFE0EBE9)
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF0F4F49), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFB6C2D0), onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155), onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFFCD34D), onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF5B3A0A), onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0E1514), onBackground = Color(0xFFE1ECEA),
    surface = Color(0xFF0E1514), onSurface = Color(0xFFE1ECEA),
    surfaceVariant = Color(0xFF243130), onSurfaceVariant = Color(0xFFB4C7C4),
    outline = Color(0xFF7F9491), outlineVariant = Color(0xFF3A4B49),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFFCDAD7),
    surfaceContainerLowest = Color(0xFF0A1110), surfaceContainerLow = Color(0xFF131C1B),
    surfaceContainer = Color(0xFF172120), surfaceContainerHigh = Color(0xFF1E2A29),
    surfaceContainerHighest = Color(0xFF263534)
)

private val Formas = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val Tipografia = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
)

@Composable
fun VendaClaraTema(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Escuro else Claro,
        shapes = Formas,
        typography = Tipografia,
        content = content
    )
}
