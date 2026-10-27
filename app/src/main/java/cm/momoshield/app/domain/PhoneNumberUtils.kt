package cm.momoshield.app.domain

/** Outils pour les numéros mobiles camerounais (9 chiffres commençant par 6, indicatif 237). */
object PhoneNumberUtils {

    private const val COUNTRY_CODE = "237"

    /**
     * Retourne le numéro sous forme canonique « 237XXXXXXXXX », ou null si [raw] n'est pas
     * un numéro mobile camerounais valide. Accepte « +237 6 99 12 34 56 », « 00237699123456 »,
     * « 699123456 », etc.
     */
    fun normalize(raw: String): String? {
        var digits = raw.filter { it in '0'..'9' }
        if (digits.startsWith("00$COUNTRY_CODE")) digits = digits.removePrefix("00")
        if (digits.length == 12 && digits.startsWith(COUNTRY_CODE)) {
            digits = digits.substring(COUNTRY_CODE.length)
        }
        return if (digits.length == 9 && digits[0] == '6') COUNTRY_CODE + digits else null
    }

    /** Extrait les numéros mobiles présents dans un texte, sous forme canonique, sans doublon. */
    fun extractFromText(text: String): List<String> {
        val normalizedText = TextNormalizer.normalize(text)
        return TextNormalizer.PHONE_REGEX.findAll(normalizedText)
            .mapNotNull { normalize(it.value) }
            .distinct()
            .toList()
    }

    /** Affichage lisible : « 237699123456 » devient « +237 699 12 34 56 ». */
    fun format(canonical: String): String {
        if (canonical.length != 12 || !canonical.startsWith(COUNTRY_CODE)) return canonical
        val n = canonical.substring(COUNTRY_CODE.length)
        return "+$COUNTRY_CODE ${n.substring(0, 3)} ${n.substring(3, 5)} ${n.substring(5, 7)} ${n.substring(7, 9)}"
    }
}
