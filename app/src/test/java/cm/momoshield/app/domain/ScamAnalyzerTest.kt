package cm.momoshield.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskCombinerTest {

    @Test
    fun withoutModelRulesScoreIsKept() {
        assertEquals(42, RiskCombiner.combine(42, null, emptyList()))
        assertEquals(42, RiskCombiner.combine(42, Float.NaN, emptyList()))
    }

    @Test
    fun modelIsBlendedWithRules() {
        // 0.6 * 50 + 0.4 * 100 = 70
        assertEquals(70, RiskCombiner.combine(50, 1.0f, emptyList()))
        // 0 règles mais modèle très sûr : au moins MEDIUM, jamais HIGH.
        val modelOnly = RiskCombiner.combine(0, 0.95f, emptyList())
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(modelOnly))
    }

    @Test
    fun optimisticModelCannotErasePowerfulRules() {
        // plancher = 0.8 * 100 = 80
        assertEquals(80, RiskCombiner.combine(100, 0.0f, emptyList()))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(RiskCombiner.combine(90, 0.0f, emptyList())))
    }

    @Test
    fun floorsForCriticalSignals() {
        val reported = listOf(Signal(SignalType.REPORTED_NUMBER, 0.9))
        assertEquals(90, RiskCombiner.combine(10, 0.0f, reported))
        val secret = listOf(Signal(SignalType.SECRET_CODE_REQUEST, 0.6))
        assertEquals(60, RiskCombiner.combine(10, 0.0f, secret))
    }

    @Test
    fun outputIsAlwaysWithinBounds() {
        assertEquals(100, RiskCombiner.combine(500, 5f, emptyList()))
        assertEquals(0, RiskCombiner.combine(-20, -3f, emptyList()))
    }
}

class ScamAnalyzerTest {

    private val scam = "Félicitations! Vous avez gagné 1.000.000 FCFA. Envoyez votre code PIN pour valider."

    @Test
    fun worksWithoutModel() {
        val r = ScamAnalyzer().analyze(scam)
        assertNull(r.modelProbability)
        assertEquals(RiskLevel.HIGH, r.level)
        assertEquals(r.rulesScore, r.score)
    }

    @Test
    fun usesModelWhenAvailable() {
        val r = ScamAnalyzer(modelScorer = { 0.9f }).analyze("Bonjour, ça va ?")
        assertEquals(0.9f, r.modelProbability!!, 0.0001f)
        assertEquals(RiskLevel.MEDIUM, r.level)
    }

    @Test
    fun modelFailureFallsBackToRules() {
        val r = ScamAnalyzer(modelScorer = { error("modèle cassé") }).analyze(scam)
        assertNull(r.modelProbability)
        assertEquals(RiskLevel.HIGH, r.level)
    }

    @Test
    fun invalidModelOutputIsIgnoredOrClamped() {
        assertNull(ScamAnalyzer(modelScorer = { Float.NaN }).analyze(scam).modelProbability)
        assertEquals(1f, ScamAnalyzer(modelScorer = { 7f }).analyze(scam).modelProbability!!, 0.0001f)
    }

    @Test
    fun blankTextNeverCallsModel() {
        var called = false
        val r = ScamAnalyzer(modelScorer = { called = true; 1f }).analyze("   ")
        assertTrue(!called)
        assertEquals(0, r.score)
        assertEquals(RiskLevel.LOW, r.level)
    }

    @Test
    fun reportedNumberFlowsThrough() {
        val r = ScamAnalyzer().analyze("Appelle le 677123456", reportedNumbers = setOf("237677123456"))
        assertEquals(RiskLevel.HIGH, r.level)
        assertTrue(r.score >= 90)
    }
}
