package cm.momoshield.app.domain

import kotlin.math.roundToInt

/** Résultat du moteur de règles seul (sans modèle ML). */
data class RuleResult(
    /** Score de 0 à 100 issu uniquement des règles. */
    val score: Int,
    val signals: List<Signal>,
    /** Numéros mobiles trouvés dans le message et l'expéditeur, au format canonique 237XXXXXXXXX. */
    val detectedNumbers: List<String>,
)

/**
 * Détection par règles et expressions régulières.
 *
 * Chaque indice détecté a un poids w (probabilité que le message soit frauduleux compte tenu de
 * cet indice seul). Les indices sont combinés par un « ou » probabiliste :
 * score = 1 − Π(1 − wᵢ). Ainsi, plusieurs indices faibles s'additionnent sans jamais dépasser 100.
 */
object RuleEngine {

    /** Longueur maximale analysée : borne le temps de calcul et la taille stockée. */
    const val MAX_INPUT_LENGTH = 5_000

    private val OPERATOR_MENTION =
        Regex("\\b(?:mtn|orange|momo|mobile money|camtel|nexttel)\\b")

    private const val MIN_SENDER_DIGITS = 8

    /**
     * @param text message à analyser (brut, avec accents ou non).
     * @param sender expéditeur affiché par le téléphone, s'il est connu.
     * @param reportedNumbers numéros (format canonique) déjà signalés par l'utilisateur ; les
     *   numéros du message qui en font partie déclenchent l'indice [SignalType.REPORTED_NUMBER].
     */
    fun evaluate(
        text: String,
        sender: String? = null,
        reportedNumbers: Set<String> = emptySet(),
    ): RuleResult {
        val normalized = TextNormalizer.normalize(text.take(MAX_INPUT_LENGTH))
        val signals = mutableListOf<Signal>()

        if (normalized.isNotEmpty()) {
            for (rule in RuleBook.rules) {
                if (rule.negations.any { it.containsMatchIn(normalized) }) continue
                val match = rule.patterns.firstNotNullOfOrNull { it.find(normalized) } ?: continue
                signals += Signal(rule.type, rule.type.weight, match.value.take(EVIDENCE_MAX_LENGTH))
            }

            UrlAnalyzer.analyze(normalized).maxByOrNull { it.concern.risk }?.let { worst ->
                signals += Signal(SignalType.SUSPICIOUS_LINK, worst.concern.risk, worst.host)
            }
        }

        val senderNumber = sender?.let { PhoneNumberUtils.normalize(it) }
        val numbers = (PhoneNumberUtils.extractFromText(normalized) + listOfNotNull(senderNumber)).distinct()

        numbers.firstOrNull { it in reportedNumbers }?.let { known ->
            signals += Signal(
                SignalType.REPORTED_NUMBER,
                SignalType.REPORTED_NUMBER.weight,
                PhoneNumberUtils.format(known),
            )
        }

        if (normalized.isNotEmpty() && sender != null &&
            OPERATOR_MENTION.containsMatchIn(normalized) && isNumericSender(sender)
        ) {
            signals += Signal(SignalType.SENDER_MISMATCH, SignalType.SENDER_MISMATCH.weight, sender.trim())
        }

        val sorted = signals.sortedByDescending { it.weight }
        return RuleResult(score = combine(sorted), signals = sorted, detectedNumbers = numbers)
    }

    /** Un « vrai » message d'opérateur vient d'un nom (MTN, ORANGE) ; un numéro ordinaire est suspect. */
    private fun isNumericSender(sender: String): Boolean {
        val trimmed = sender.trim()
        if (trimmed.isEmpty()) return false
        val allowed = trimmed.all { it in '0'..'9' || it in "+ -.()" }
        return allowed && trimmed.count { it in '0'..'9' } >= MIN_SENDER_DIGITS
    }

    internal fun combine(signals: List<Signal>): Int {
        val safeProbability = signals.fold(1.0) { acc, s -> acc * (1.0 - s.weight.coerceIn(0.0, 1.0)) }
        return ((1.0 - safeProbability) * 100.0).roundToInt().coerceIn(0, 100)
    }

    private const val EVIDENCE_MAX_LENGTH = 80
}
