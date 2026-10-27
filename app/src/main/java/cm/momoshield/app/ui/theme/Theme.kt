package cm.momoshield.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cm.momoshield.app.domain.RiskLevel

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A2E65),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = Color(0xFF385E8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002D6D),
    primaryContainer = Color(0xFF004398),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBAC7DB),
    onSecondary = Color(0xFF243141),
    secondaryContainer = Color(0xFF3B4859),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1B1D21),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
)

@Composable
fun MoMoShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Couleur sémantique du niveau de risque (vert / orange / rouge), lisible en clair et en sombre. */
@Composable
fun riskColor(level: RiskLevel): Color {
    val dark = isSystemInDarkTheme()
    return when (level) {
        RiskLevel.LOW -> if (dark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
        RiskLevel.MEDIUM -> if (dark) Color(0xFFFFA726) else Color(0xFFB85C00)
        RiskLevel.HIGH -> if (dark) Color(0xFFEF5350) else Color(0xFFC62828)
    }
}
