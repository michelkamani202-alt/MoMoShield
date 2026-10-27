package cm.momoshield.app.domain

/** Verdict complet présenté à l'utilisateur. */
data class AnalysisResult(
    val score: Int,
    val level: RiskLevel,
    val signals: List<Signal>,
    /** Numéros mobiles détectés (format canonique 237XXXXXXXXX). */
    val detectedNumbers: List<String>,
    val rulesScore: Int,
    /** Probabilité d'arnaque donnée par TensorFlow Lite (0-1), ou null si le modèle est indisponible. */
    val modelProbability: Float?,
)

/**
 * Point d'entrée du domaine : règles + modèle optionnel.
 *
 * [modelScorer] renvoie la probabilité d'arnaque (0-1) ou null si le modèle n'est pas disponible.
 * Toute exception du modèle est absorbée : l'application retombe alors sur les règles seules.
 */
class ScamAnalyzer(private val modelScorer: (String) -> Float? = { null }) {

    fun analyze(
        text: String,
        sender: String? = null,
        reportedNumbers: Set<String> = emptySet(),
    ): AnalysisResult {
        val rules = RuleEngine.evaluate(text, sender, reportedNumbers)

        val probability: Float? = if (text.isBlank()) {
            null
        } else {
            runCatching { modelScorer(text.take(RuleEngine.MAX_INPUT_LENGTH)) }
                .getOrNull()
                ?.takeIf { !it.isNaN() }
                ?.coerceIn(0f, 1f)
        }

        val score = RiskCombiner.combine(rules.score, probability, rules.signals)
        return AnalysisResult(
            score = score,
            level = RiskLevel.fromScore(score),
            signals = rules.signals,
            detectedNumbers = rules.detectedNumbers,
            rulesScore = rules.score,
            modelProbability = probability,
        )
    }
}
