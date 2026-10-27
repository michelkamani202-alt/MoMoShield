package cm.momoshield.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garde-fou de non-régression : 100 messages écrits à la main (50 arnaques, 50 légitimes) qui ont
 * servi à régler les règles. Ce n'est PAS une mesure de généralisation (voir README) : si une
 * modification du catalogue de règles fait échouer ce test, elle casse un cas déjà géré.
 */
class RulesRegressionTest {

    private data class Sample(val text: String, val isScam: Boolean)

    private fun load(): List<Sample> {
        val stream = javaClass.classLoader!!.getResourceAsStream("rules_regression.tsv")
            ?: error("rules_regression.tsv introuvable")
        return stream.bufferedReader(Charsets.UTF_8).readLines().filter { it.isNotBlank() }.map {
            val (text, label) = it.split('\t')
            Sample(text, label == "1")
        }
    }

    @Test
    fun rulesAloneKeepPerfectPrecisionAndHighRecall() {
        val samples = load()
        assertEquals(100, samples.size)

        val falsePositives = samples.filter { !it.isScam && RuleEngine.evaluate(it.text).score >= RiskLevel.MEDIUM_THRESHOLD }
        val missedScams = samples.filter { it.isScam && RuleEngine.evaluate(it.text).score < RiskLevel.MEDIUM_THRESHOLD }

        assertTrue("Faux positifs : ${falsePositives.map { it.text }}", falsePositives.isEmpty())
        assertTrue("Arnaques manquées : ${missedScams.map { it.text }}", missedScams.size <= 2)
    }

    @Test
    fun ruleEngineIsFastOnLongInputs() {
        val longText = "Bonjour félicitations vous avez gagné, envoyez votre code PIN. ".repeat(100)
        val start = System.nanoTime()
        repeat(20) { RuleEngine.evaluate(longText) }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("Trop lent : $elapsedMs ms pour 20 analyses", elapsedMs < 3_000)
    }
}
