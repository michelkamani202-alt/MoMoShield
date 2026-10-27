package cm.momoshield.app.ui

import androidx.annotation.StringRes
import cm.momoshield.app.R
import cm.momoshield.app.domain.RiskLevel
import cm.momoshield.app.domain.SignalType

// Les `when` sont exhaustifs : ajouter un SignalType ou un RiskLevel sans texte ne compile pas.

@StringRes
fun SignalType.titleRes(): Int = when (this) {
    SignalType.REPORTED_NUMBER -> R.string.signal_reported_number_title
    SignalType.SECRET_CODE_REQUEST -> R.string.signal_secret_code_title
    SignalType.REFUND_REQUEST -> R.string.signal_refund_title
    SignalType.PRIZE_LURE -> R.string.signal_prize_title
    SignalType.ADVANCE_FEE -> R.string.signal_advance_fee_title
    SignalType.ACCOUNT_THREAT -> R.string.signal_account_threat_title
    SignalType.SENDER_MISMATCH -> R.string.signal_sender_mismatch_title
    SignalType.IMPERSONATION -> R.string.signal_impersonation_title
    SignalType.CONTACT_REQUEST -> R.string.signal_contact_title
    SignalType.URGENCY -> R.string.signal_urgency_title
    SignalType.SUSPICIOUS_LINK -> R.string.signal_link_title
}

@StringRes
fun SignalType.explanationRes(): Int = when (this) {
    SignalType.REPORTED_NUMBER -> R.string.signal_reported_number_text
    SignalType.SECRET_CODE_REQUEST -> R.string.signal_secret_code_text
    SignalType.REFUND_REQUEST -> R.string.signal_refund_text
    SignalType.PRIZE_LURE -> R.string.signal_prize_text
    SignalType.ADVANCE_FEE -> R.string.signal_advance_fee_text
    SignalType.ACCOUNT_THREAT -> R.string.signal_account_threat_text
    SignalType.SENDER_MISMATCH -> R.string.signal_sender_mismatch_text
    SignalType.IMPERSONATION -> R.string.signal_impersonation_text
    SignalType.CONTACT_REQUEST -> R.string.signal_contact_text
    SignalType.URGENCY -> R.string.signal_urgency_text
    SignalType.SUSPICIOUS_LINK -> R.string.signal_link_text
}

@StringRes
fun RiskLevel.titleRes(): Int = when (this) {
    RiskLevel.LOW -> R.string.level_low_title
    RiskLevel.MEDIUM -> R.string.level_medium_title
    RiskLevel.HIGH -> R.string.level_high_title
}

@StringRes
fun RiskLevel.adviceRes(): Int = when (this) {
    RiskLevel.LOW -> R.string.level_low_advice
    RiskLevel.MEDIUM -> R.string.level_medium_advice
    RiskLevel.HIGH -> R.string.level_high_advice
}

@StringRes
fun UiMessage.textRes(): Int = when (this) {
    UiMessage.NUMBER_REPORTED -> R.string.msg_number_reported
    UiMessage.INVALID_NUMBER -> R.string.msg_invalid_number
    UiMessage.EMPTY_TEXT -> R.string.msg_empty_text
    UiMessage.ANALYSIS_FAILED -> R.string.msg_analysis_failed
}

/** Lecture tolérante d'un niveau stocké en base (valeur inconnue -> LOW). */
fun parseRiskLevel(name: String): RiskLevel =
    RiskLevel.entries.firstOrNull { it.name == name } ?: RiskLevel.LOW
