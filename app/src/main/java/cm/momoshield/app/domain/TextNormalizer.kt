package cm.momoshield.app.domain

import java.text.Normalizer

/**
 * Normalisation de texte partagée par le moteur de règles et le modèle TensorFlow Lite.
 *
 * IMPORTANT : [forModel] et [tokens] doivent rester strictement identiques à
 * `ml/training/text_features.py`. Le test `FeatureParityTest` le vérifie.
 */
object TextNormalizer {

    private val COMBINING_MARKS = Regex("\\p{M}+")
    private val WHITESPACE = Regex("[ \t\n\r\u000C\u000B]+")
    private val DIGITS = Regex("[0-9]+")
    private val TOKEN = Regex("[a-z]+")

    /** Domaines de premier niveau reconnus pour les liens sans « http ». */
    private const val TLDS =
        "com|net|org|cm|info|xyz|top|club|online|site|link|click|co|me|ly|gl|app|biz|" +
            "tk|ml|ga|cf|gq|ws|cc|live|shop|store|vip|icu"

    /** Lien : avec schéma ou « www. », ou nom de domaine nu suivi d'un TLD connu. */
    val URL_REGEX = Regex(
        "(?:https?://|www\\.)[^ ]+|" +
            "\\b[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.(?:$TLDS)(?![a-z0-9-])(?:[/@:?#][^ ]*)?",
    )

    /** Numéro mobile camerounais : [+237 | 00237 | 237] puis 6XXXXXXXX, séparateurs tolérés. */
    val PHONE_REGEX = Regex(
        "(?<![0-9])(?:(?:\\+|00)? ?237[ .-]?)?6(?:[ .-]?[0-9]){8}(?![0-9])",
    )

    /**
     * Minuscules, guillemets typographiques unifiés, accents retirés, espaces réduits.
     * Beaucoup de SMS sont écrits sans accents : toutes les règles travaillent sur ce texte.
     */
    fun normalize(text: String): String {
        val lowered = text.lowercase()
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace(' ', ' ')
            .replace(' ', ' ')
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        val noMarks = COMBINING_MARKS.replace(decomposed, "")
        return WHITESPACE.replace(noMarks, " ").trim()
    }

    /** Texte préparé pour le modèle : liens, numéros et nombres remplacés par des jetons. */
    fun forModel(text: String): String {
        var s = normalize(text)
        s = URL_REGEX.replace(s, " urltoken ")
        s = PHONE_REGEX.replace(s, " phonetoken ")
        s = DIGITS.replace(s, " numtoken ")
        return s
    }

    /** Jetons alphabétiques (a-z) du texte préparé pour le modèle. */
    fun tokens(text: String): List<String> =
        TOKEN.findAll(forModel(text)).map { it.value }.toList()
}
