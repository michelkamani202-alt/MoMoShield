package cm.momoshield.app.domain

/** Raison pour laquelle un lien est jugé suspect. */
enum class UrlConcern(val risk: Double) {
    /** « mtn.cm@site-pirate.xyz » : la partie avant @ est un leurre. */
    USERINFO_TRICK(0.60),
    LOOKALIKE_BRAND(0.60),
    IP_ADDRESS(0.50),
    PUNYCODE(0.50),
    SHORTENER(0.40),
    SUSPICIOUS_TLD(0.40),
    UNOFFICIAL(0.25),
}

data class UrlFinding(val url: String, val host: String, val concern: UrlConcern)

/**
 * Analyse des liens d'un message. Un lien vers un domaine officiel d'opérateur est ignoré ;
 * tout autre lien reçoit au minimum un risque faible, plus élevé pour les astuces classiques
 * (faux domaine imitant l'opérateur, raccourcisseur, adresse IP, extension douteuse).
 *
 * La liste [OFFICIAL_DOMAINS] est à vérifier et à compléter avant publication.
 */
object UrlAnalyzer {

    val OFFICIAL_DOMAINS: Set<String> = setOf(
        // Opérateurs
        "mtn.cm", "mtn.com", "orange.cm", "orange.com", "camtel.cm", "nexttel.cm",
        // Institutions : tout sous-domaine de gov.cm et l'Université de Douala
        "gov.cm", "univ-douala.cm",
    )

    private val SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "cutt.ly", "rb.gy",
        "shorturl.at", "tiny.cc", "s.id",
    )

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "club", "online", "site", "link", "click", "tk", "ml", "ga", "cf",
        "gq", "icu", "vip", "live", "shop", "store", "ws", "cc", "biz",
    )

    private val BRAND_KEYWORDS = listOf("mtn", "orange", "momo", "camtel", "nexttel", "mobilemoney")

    private val IPV4 = Regex("^[0-9]{1,3}(?:\\.[0-9]{1,3}){3}$")
    private val EDGE_PUNCTUATION = charArrayOf('.', ',', ';', ':', ')', '(', '"', '\'', '!', '?', '>', '<')

    /** [normalizedText] doit avoir été passé par [TextNormalizer.normalize]. */
    fun analyze(normalizedText: String): List<UrlFinding> =
        TextNormalizer.URL_REGEX.findAll(normalizedText)
            .map { it.value.trim(*EDGE_PUNCTUATION) }
            .filter { it.isNotEmpty() }
            .mapNotNull { url -> classify(url) }
            .toList()

    private fun classify(url: String): UrlFinding? {
        val withoutScheme = url.removePrefix("https://").removePrefix("http://")
        val authorityEnd = withoutScheme.indexOfFirst { it == '/' || it == '?' || it == '#' }
        val authority = if (authorityEnd >= 0) withoutScheme.substring(0, authorityEnd) else withoutScheme
        val hasUserInfo = authority.contains('@')
        val host = authority.substringAfterLast('@').substringBefore(':').trim(*EDGE_PUNCTUATION)
        if (host.isEmpty()) return null

        val concern = when {
            hasUserInfo -> UrlConcern.USERINFO_TRICK
            IPV4.matches(host) -> UrlConcern.IP_ADDRESS
            host.contains("xn--") -> UrlConcern.PUNYCODE
            isOfficial(host) -> return null
            BRAND_KEYWORDS.any { host.contains(it) } -> UrlConcern.LOOKALIKE_BRAND
            host in SHORTENERS -> UrlConcern.SHORTENER
            host.substringAfterLast('.') in SUSPICIOUS_TLDS -> UrlConcern.SUSPICIOUS_TLD
            else -> UrlConcern.UNOFFICIAL
        }
        return UrlFinding(url = url, host = host, concern = concern)
    }

    private fun isOfficial(host: String): Boolean =
        OFFICIAL_DOMAINS.any { host == it || host.endsWith(".$it") }
}
