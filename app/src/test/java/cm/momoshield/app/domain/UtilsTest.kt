package cm.momoshield.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun removesAccentsCaseAndExtraSpaces() {
        assertEquals("felicitations ! vous avez gagne", TextNormalizer.normalize("  FÉLICITATIONS !\n Vous  avez   gagné "))
    }

    @Test
    fun unifiesTypographicApostrophes() {
        assertEquals("j'ai envoye", TextNormalizer.normalize("J’ai envoyé"))
    }

    @Test
    fun normalizeIsIdempotent() {
        val once = TextNormalizer.normalize("Bonjour, à très bientôt ! 5 000 F")
        assertEquals(once, TextNormalizer.normalize(once))
    }

    @Test
    fun modelTextReplacesUrlsPhonesAndNumbers() {
        val s = TextNormalizer.forModel("Appelez +237 699 12 34 56 ou allez sur http://mtn-gain.xyz/win pour 5000 F")
        assertTrue(s, s.contains("phonetoken"))
        assertTrue(s, s.contains("urltoken"))
        assertTrue(s, s.contains("numtoken"))
        assertTrue(s, !s.contains("699") && !s.contains("xyz") && !s.contains("5000"))
    }

    @Test
    fun tokensAreLowercaseLetters() {
        assertEquals(listOf("vous", "avez", "recu", "numtoken", "f"), TextNormalizer.tokens("Vous avez reçu 5000 F"))
    }
}

class PhoneNumberUtilsTest {

    @Test
    fun normalizesCommonFormats() {
        val expected = "237699123456"
        assertEquals(expected, PhoneNumberUtils.normalize("699123456"))
        assertEquals(expected, PhoneNumberUtils.normalize("+237 699 12 34 56"))
        assertEquals(expected, PhoneNumberUtils.normalize("00237699123456"))
        assertEquals(expected, PhoneNumberUtils.normalize("237-699-123-456"))
        assertEquals(expected, PhoneNumberUtils.normalize("(+237) 6 99 12 34 56"))
    }

    @Test
    fun rejectsInvalidNumbers() {
        assertNull(PhoneNumberUtils.normalize(""))
        assertNull(PhoneNumberUtils.normalize("abc"))
        assertNull(PhoneNumberUtils.normalize("12345"))
        assertNull(PhoneNumberUtils.normalize("299123456")) // ne commence pas par 6
        assertNull(PhoneNumberUtils.normalize("6991234567")) // 10 chiffres
        assertNull(PhoneNumberUtils.normalize("+33 6 12 34 56 78")) // numéro français
    }

    @Test
    fun extractsNumbersFromTextWithoutDuplicates() {
        val text = "Appelle le 699 12 34 56 ou le +237 677123456, sinon rappelle le 699123456."
        assertEquals(listOf("237699123456", "237677123456"), PhoneNumberUtils.extractFromText(text))
    }

    @Test
    fun ignoresAmountsAndLongIds() {
        assertTrue(PhoneNumberUtils.extractFromText("Vous avez reçu 6 000 000 FCFA").isEmpty())
        assertTrue(PhoneNumberUtils.extractFromText("ID de transaction 6991234567890").isEmpty())
    }

    @Test
    fun formatsForDisplay() {
        assertEquals("+237 699 12 34 56", PhoneNumberUtils.format("237699123456"))
        assertEquals("abc", PhoneNumberUtils.format("abc"))
    }
}

class UrlAnalyzerTest {

    private fun concerns(text: String) =
        UrlAnalyzer.analyze(TextNormalizer.normalize(text)).map { it.concern }

    @Test
    fun officialDomainsAreIgnored() {
        assertTrue(concerns("Voir https://www.mtn.cm/momo").isEmpty())
        assertTrue(concerns("Rendez-vous sur orange.cm").isEmpty())
        assertTrue(concerns("https://momo.mtn.cm/aide").isEmpty())
    }

    @Test
    fun lookalikeDomainsAreFlagged() {
        assertEquals(listOf(UrlConcern.LOOKALIKE_BRAND), concerns("http://mtn-momo-verif.xyz"))
        assertEquals(listOf(UrlConcern.LOOKALIKE_BRAND), concerns("https://fakemtn.cm/login"))
        assertEquals(listOf(UrlConcern.LOOKALIKE_BRAND), concerns("orange-money-gain.top"))
    }

    @Test
    fun userInfoTrickIsFlagged() {
        assertEquals(listOf(UrlConcern.USERINFO_TRICK), concerns("https://mtn.cm@evil-site.xyz/login"))
        assertEquals(listOf(UrlConcern.USERINFO_TRICK), concerns("mtn.cm@evil-site.xyz/login"))
    }

    @Test
    fun otherTricksAreFlagged() {
        assertEquals(listOf(UrlConcern.IP_ADDRESS), concerns("http://192.168.10.5/pay"))
        assertEquals(listOf(UrlConcern.SHORTENER), concerns("clique bit.ly/3xYz"))
        assertEquals(listOf(UrlConcern.SUSPICIOUS_TLD), concerns("gagne-gros.xyz"))
        assertEquals(listOf(UrlConcern.UNOFFICIAL), concerns("https://www.example-shop.com/promo"))
        assertEquals(listOf(UrlConcern.PUNYCODE), concerns("http://xn--mtn-9la.com"))
    }

    @Test
    fun trailingPunctuationDoesNotHideOfficialDomain() {
        assertTrue(concerns("Allez sur https://www.mtn.cm.").isEmpty())
        assertTrue(concerns("(https://www.mtn.cm/momo)").isEmpty())
    }

    @Test
    fun plainTextWithoutLinkHasNoFinding() {
        assertTrue(concerns("Bonjour, à demain.").isEmpty())
        assertTrue(concerns("Le solde est de 5.000 F.").isEmpty())
    }
}
