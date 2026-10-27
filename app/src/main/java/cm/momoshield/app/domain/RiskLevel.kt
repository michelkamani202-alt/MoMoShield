package cm.momoshield.app.domain

/** Niveau de risque affiché à l'utilisateur, dérivé d'un score de 0 à 100. */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        const val MEDIUM_THRESHOLD = 30
        const val HIGH_THRESHOLD = 60

        fun fromScore(score: Int): RiskLevel = when {
            score >= HIGH_THRESHOLD -> HIGH
            score >= MEDIUM_THRESHOLD -> MEDIUM
            else -> LOW
        }
    }
}
