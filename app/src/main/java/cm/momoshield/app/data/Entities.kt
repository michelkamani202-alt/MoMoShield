package cm.momoshield.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une analyse enregistrée dans l'historique. Par confidentialité, seul un aperçu du message
 * est conservé (voir [ScanRepository.PREVIEW_LENGTH]), jamais le texte complet.
 */
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val preview: String,
    val sender: String?,
    val score: Int,
    /** Nom de [cm.momoshield.app.domain.RiskLevel]. */
    val level: String,
    /** Noms de [cm.momoshield.app.domain.SignalType] séparés par des virgules. */
    val signalTypes: String,
    val modelProbability: Float?,
)

/** Numéro signalé par l'utilisateur, au format canonique « 237XXXXXXXXX ». */
@Entity(tableName = "reported_numbers")
data class ReportedNumberEntity(
    @PrimaryKey val number: String,
    val reportCount: Int,
    val firstReportedAt: Long,
    val lastReportedAt: Long,
)
