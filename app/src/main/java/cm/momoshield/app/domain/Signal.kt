package cm.momoshield.app.domain

/**
 * Types d'indices détectés dans un message.
 *
 * [weight] est la probabilité « à elle seule » qu'un message portant cet indice soit une
 * arnaque. Les poids sont combinés par un ou-probabiliste (voir [RuleEngine]).
 * [critical] marque les indices qui justifient une alerte forte même isolés.
 */
enum class SignalType(val weight: Double, val critical: Boolean = false) {
    REPORTED_NUMBER(0.90, critical = true),
    SECRET_CODE_REQUEST(0.60, critical = true),
    REFUND_REQUEST(0.55),
    PRIZE_LURE(0.50),
    ADVANCE_FEE(0.50),
    ACCOUNT_THREAT(0.35),
    SENDER_MISMATCH(0.35),
    IMPERSONATION(0.25),
    CONTACT_REQUEST(0.25),
    URGENCY(0.20),

    /** Le poids réel dépend du lien (voir [UrlAnalyzer]) ; cette valeur est un plancher. */
    SUSPICIOUS_LINK(0.25),
}

/** Un indice détecté : son type, son poids effectif et l'extrait du message qui l'a déclenché. */
data class Signal(
    val type: SignalType,
    val weight: Double,
    val evidence: String? = null,
)
