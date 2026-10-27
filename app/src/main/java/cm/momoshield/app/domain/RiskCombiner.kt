package cm.momoshield.app.domain

import kotlin.math.roundToInt

/**
 * Fusionne le score des règles (0-100) et la probabilité du modèle TensorFlow Lite (0-1).
 *
 * - Sans modèle : le score des règles est conservé tel quel.
 * - Avec modèle : moyenne pondérée 60 % règles / 40 % modèle, mais un score de règles élevé ne
 *   peut pas être fortement dilué par un modèle trop optimiste (plancher à 80 % du score règles).
 * - Un numéro déjà signalé impose au moins 90 ; une demande de code secret impose au moins 60.
 */
object RiskCombiner {

    const val RULES_WEIGHT = 0.6
    const val MODEL_WEIGHT = 0.4
    const val RULES_FLOOR_RATIO = 0.8
    const val REPORTED_NUMBER_FLOOR = 90
    const val CRITICAL_SIGNAL_FLOOR = 60

    fun combine(rulesScore: Int, modelProbability: Float?, signals: List<Signal>): Int {
        val rules = rulesScore.coerceIn(0, 100).toDouble()

        var score = if (modelProbability == null || modelProbability.isNaN()) {
            rules
        } else {
            val model = modelProbability.coerceIn(0f, 1f) * 100.0
            maxOf(RULES_WEIGHT * rules + MODEL_WEIGHT * model, RULES_FLOOR_RATIO * rules)
        }

        if (signals.any { it.type == SignalType.REPORTED_NUMBER }) {
            score = maxOf(score, REPORTED_NUMBER_FLOOR.toDouble())
        }
        if (signals.any { it.type == SignalType.SECRET_CODE_REQUEST }) {
            score = maxOf(score, CRITICAL_SIGNAL_FLOOR.toDouble())
        }
        return score.roundToInt().coerceIn(0, 100)
    }
}
