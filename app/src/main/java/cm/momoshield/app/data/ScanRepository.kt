package cm.momoshield.app.data

import cm.momoshield.app.domain.AnalysisResult
import cm.momoshield.app.domain.PhoneNumberUtils
import cm.momoshield.app.domain.RuleEngine
import cm.momoshield.app.domain.ScamAnalyzer
import cm.momoshield.app.ml.ScamClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Orchestre l'analyse (règles + TensorFlow Lite), l'historique et les numéros signalés.
 * Les fonctions suspendues sont sûres à appeler depuis le thread principal.
 */
class ScanRepository(
    private val database: AppDatabase,
    classifier: ScamClassifier,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val analyzer = ScamAnalyzer { text -> classifier.predict(text) }
    private val scanDao = database.scanDao()
    private val reportedDao = database.reportedNumberDao()

    val history: Flow<List<ScanEntity>> = scanDao.observeRecent()
    val reportedNumbers: Flow<List<ReportedNumberEntity>> = reportedDao.observeAll()

    /** Analyse un message, l'enregistre dans l'historique et renvoie le verdict. */
    suspend fun analyze(text: String, sender: String?): AnalysisResult {
        val cleanSender = sender?.trim()?.takeIf { it.isNotEmpty() }
        val candidates = (
            PhoneNumberUtils.extractFromText(text.take(RuleEngine.MAX_INPUT_LENGTH)) +
                listOfNotNull(cleanSender?.let { PhoneNumberUtils.normalize(it) })
            ).distinct()
        val reported = if (candidates.isEmpty()) {
            emptySet()
        } else {
            reportedDao.findAmong(candidates).map { it.number }.toSet()
        }

        val result = withContext(Dispatchers.Default) { analyzer.analyze(text, cleanSender, reported) }

        scanDao.insert(
            ScanEntity(
                createdAt = clock(),
                preview = text.trim().take(PREVIEW_LENGTH),
                sender = cleanSender,
                score = result.score,
                level = result.level.name,
                signalTypes = result.signals.joinToString(",") { it.type.name },
                modelProbability = result.modelProbability,
            ),
        )
        scanDao.trim()
        return result
    }

    /** Signale un numéro (n'importe quel format). Renvoie false si ce n'est pas un numéro valide. */
    suspend fun reportNumber(raw: String): Boolean {
        val canonical = PhoneNumberUtils.normalize(raw) ?: return false
        reportedDao.report(canonical, clock())
        return true
    }

    suspend fun unreportNumber(number: String) = reportedDao.delete(number)

    suspend fun deleteScan(id: Long) = scanDao.delete(id)

    suspend fun clearHistory() = scanDao.clear()

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val scans = scanDao.getAllSync()
        val reported = reportedDao.getAllSync()
        val sb = StringBuilder()
        sb.append("{\n  \"version\": 1,\n  \"scans\": [\n")
        scans.forEachIndexed { index, s ->
            sb.append("    {\"createdAt\": ${s.createdAt}, \"score\": ${s.score}, \"level\": \"${s.level}\", \"preview\": \"${s.preview.replace("\"", "\\\"")}\"}")
            if (index < scans.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n  \"reported\": [\n")
        reported.forEachIndexed { index, r ->
            sb.append("    {\"number\": \"${r.number}\", \"reportCount\": ${r.reportCount}, \"lastReportedAt\": ${r.lastReportedAt}}")
            if (index < reported.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n}")
        sb.toString()
    }

    companion object {
        const val PREVIEW_LENGTH = 200
    }
}
