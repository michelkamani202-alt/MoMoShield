package cm.momoshield.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private fun types(result: RuleResult) = result.signals.map { it.type }.toSet()

    // ---------- Messages légitimes : aucune alerte ----------

    @Test
    fun legitimateTransactionConfirmationIsLow() {
        val r = RuleEngine.evaluate(
            "Vous avez envoyé 5000 FCFA à Paul (699123456). Frais: 100 F. " +
                "Nouveau solde: 12000 FCFA. ID de transaction: 1234567890.",
        )
        assertEquals(0, r.score)
        assertTrue(r.signals.isEmpty())
    }

    @Test
    fun securityAdviceIsNotARequestForPin() {
        val messages = listOf(
            "Ne partagez jamais votre code PIN avec personne.",
            "Pour votre sécurité, ne communiquez jamais votre code secret.",
            "MTN ne vous demandera jamais votre PIN par SMS. Never share your PIN or OTP.",
            "Votre code de vérification est 482913. Ne le partagez avec personne.",
        )
        for (m in messages) {
            val r = RuleEngine.evaluate(m)
            assertFalse("Faux positif sur : $m", SignalType.SECRET_CODE_REQUEST in types(r))
            assertEquals("Score attendu 0 pour : $m", 0, r.score)
        }
    }

    @Test
    fun everydayMessagesAreLow() {
        val messages = listOf(
            "Salut, on se voit demain à 15h au marché Mokolo.",
            "Maman, j'arrive ce soir vers 19h. Prépare le ndolé stp.",
            "MTN: rechargez 1000F et recevez 2 Go valable 7 jours. Tapez *123#.",
            "Rappel: réunion du département jeudi à 10h en salle 204.",
            "Consultez https://www.mtn.cm/momo pour les offres.",
        )
        for (m in messages) {
            val r = RuleEngine.evaluate(m)
            assertEquals("Faux positif sur : $m -> ${r.signals}", RiskLevel.LOW, RiskLevel.fromScore(r.score))
        }
    }

    // ---------- Arnaques classiques ----------

    @Test
    fun wrongTransferRefundWithUrgencyIsHigh() {
        val r = RuleEngine.evaluate(
            "Bonjour, j'ai envoyé 25000 FCFA par erreur sur votre compte MoMo. " +
                "Veuillez me renvoyer l'argent au plus vite svp.",
        )
        assertTrue(SignalType.REFUND_REQUEST in types(r))
        assertTrue(SignalType.URGENCY in types(r))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(r.score))
    }

    @Test
    fun prizeWithFeeAndPhoneIsHigh() {
        val r = RuleEngine.evaluate(
            "FELICITATIONS! Vous avez gagné 5.000.000 FCFA au tirage MTN. " +
                "Pour récupérer votre gain appelez le 677123456 et payez les frais de dossier.",
        )
        assertTrue(SignalType.PRIZE_LURE in types(r))
        assertTrue(SignalType.ADVANCE_FEE in types(r))
        assertTrue(SignalType.CONTACT_REQUEST in types(r))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(r.score))
    }

    @Test
    fun accountThreatWithFakeLinkIsHigh() {
        val r = RuleEngine.evaluate(
            "MTN MoMo: votre compte sera bloqué dans 24h. " +
                "Cliquez http://mtn-momo-verif.xyz pour mettre à jour votre KYC.",
        )
        assertTrue(SignalType.ACCOUNT_THREAT in types(r))
        assertTrue(SignalType.SUSPICIOUS_LINK in types(r))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(r.score))
    }

    @Test
    fun pinRequestAloneIsHigh() {
        val r = RuleEngine.evaluate("Entrez votre code PIN pour recevoir votre remboursement")
        assertTrue(SignalType.SECRET_CODE_REQUEST in types(r))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(r.score))
    }

    @Test
    fun englishScamIsHigh() {
        val r = RuleEngine.evaluate(
            "Congratulations! You have won 500000 FCFA. Send your PIN to claim within 24 hours.",
        )
        assertTrue(SignalType.PRIZE_LURE in types(r))
        assertTrue(SignalType.SECRET_CODE_REQUEST in types(r))
        assertTrue(SignalType.URGENCY in types(r))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(r.score))
    }

    @Test
    fun detectionWorksWithoutAccents() {
        val withAccents = RuleEngine.evaluate("Félicitations ! Vous avez gagné une moto")
        val without = RuleEngine.evaluate("Felicitations ! Vous avez gagne une moto")
        assertTrue(SignalType.PRIZE_LURE in types(withAccents))
        assertEquals(withAccents.score, without.score)
    }

    // ---------- Numéros signalés, expéditeur ----------

    @Test
    fun reportedNumberForcesHighRisk() {
        val r = RuleEngine.evaluate(
            "Salut c'est moi, appelle-moi au 677 12 34 56",
            reportedNumbers = setOf("237677123456"),
        )
        assertTrue(SignalType.REPORTED_NUMBER in types(r))
        assertTrue(r.score >= 90)
        assertEquals(listOf("237677123456"), r.detectedNumbers)
    }

    @Test
    fun reportedSenderIsDetected() {
        val r = RuleEngine.evaluate(
            "Bonjour",
            sender = "+237 677 12 34 56",
            reportedNumbers = setOf("237677123456"),
        )
        assertTrue(SignalType.REPORTED_NUMBER in types(r))
    }

    @Test
    fun operatorMessageFromOrdinaryNumberIsSuspicious() {
        val numeric = RuleEngine.evaluate("MTN MoMo: transaction en attente.", sender = "677123456")
        assertTrue(SignalType.SENDER_MISMATCH in types(numeric))

        val named = RuleEngine.evaluate("MTN MoMo: transaction en attente.", sender = "MTN")
        assertFalse(SignalType.SENDER_MISMATCH in types(named))

        val shortCode = RuleEngine.evaluate("MTN MoMo: transaction en attente.", sender = "8100")
        assertFalse(SignalType.SENDER_MISMATCH in types(shortCode))
    }

    // ---------- Robustesse ----------

    @Test
    fun blankAndHugeInputsAreHandled() {
        assertEquals(0, RuleEngine.evaluate("").score)
        assertEquals(0, RuleEngine.evaluate("   \n\t ").score)
        val huge = "a".repeat(200_000) + " felicitations"
        val r = RuleEngine.evaluate(huge)
        // Le texte est tronqué à MAX_INPUT_LENGTH : la fin n'est pas analysée.
        assertEquals(0, r.score)
    }

    @Test
    fun scoreNeverExceedsHundredAndSignalsAreSorted() {
        val r = RuleEngine.evaluate(
            "URGENT félicitations vous avez gagné, envoyez votre code PIN, compte bloqué, " +
                "payez les frais de dossier, appelez le 677123456 http://mtn-gain.xyz par erreur renvoyez l'argent",
        )
        assertTrue(r.score in 0..100)
        assertEquals(r.signals.sortedByDescending { it.weight }, r.signals)
    }
}
