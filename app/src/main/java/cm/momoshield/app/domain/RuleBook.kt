package cm.momoshield.app.domain

/**
 * Une règle : un type d'indice, des motifs qui le déclenchent et des motifs de négation
 * qui l'annulent (ex. « ne partagez JAMAIS votre code PIN » est un conseil, pas une demande).
 */
internal class PatternRule(
    val type: SignalType,
    val patterns: List<Regex>,
    val negations: List<Regex> = emptyList(),
)

/**
 * Catalogue des règles. Tous les motifs s'appliquent à du texte déjà normalisé
 * (minuscules, sans accents) : on écrit donc « felicitations », pas « félicitations ».
 *
 * Les quantificateurs « .{0,N} » sont bornés pour éviter tout retour arrière catastrophique.
 * Le catalogue est à enrichir avec de vrais messages signalés par les utilisateurs.
 */
internal object RuleBook {

    private fun rx(pattern: String) = Regex(pattern)

    /** Secrets sans ambiguïté : PIN, mot de passe, OTP (avec tolérance aux fautes d'obfuscation). */
    private const val SECRET_WORDS =
        "(?:p[i1!l]n|code\\s*p[i1!l]n|c0de\\s*secr[e3]t|code confidentiel|mot de p[a@4]ss[e3]|otp|passw0rd|passcode)"

    /** Verbes qui demandent de DONNER quelque chose à quelqu'un. */
    private const val GIVE_VERBS =
        "(?:envoyez|envoyer|envoie|envoyant|donnez|donner|donne|donnant|communiquez|communiquer|" +
            "communiquant|transmettez|transmettre|transmettant|dites|fournissez|fournir|fournissant|" +
            "repondez|repondre)"

    /** Verbes de saisie ou de confirmation (suffisants si le mot est un secret sans ambiguïté). */
    private const val ENTER_VERBS =
        "(?:entrez|entrer|saisissez|saisir|tapez|taper|indiquez|indiquer|confirmez|confirmer|" +
            "mettez|renseignez)"

    private const val STATE =
        "(?:bloquee?|suspendue?|desactivee?|fermee?|restreinte?|gelee?|piratee?|resiliee?|expiree?)"

    private const val PHONE = "(?:(?:\\+|00)? ?237[ .-]?)?6(?:[ .-]?[0-9]){8}"

    val rules: List<PatternRule> = listOf(

        PatternRule(
            type = SignalType.URGENCY,
            patterns = listOf(
                rx("\\burgent\\w*"),
                rx("\\bimmediat\\w*"),
                rx("\\bsans delai\\b"),
                rx("\\b(?:dans|sous) (?:les )?[0-9]+ ?(?:h|heures?|minutes?|min|jours?)\\b"),
                rx("\\bavant (?:ce soir|minuit|demain)\\b"),
                rx("\\b(?:vite|rapidement|au plus vite|agissez)\\b"),
                rx("\\bdernier(?:e)? (?:avertissement|chance|delai)\\b"),
                rx("\\bwithin [0-9]+ ?(?:hours?|hrs?|minutes?|mins?)\\b"),
                rx("\\b(?:right now|as soon as possible|asap|hurry)\\b"),
            ),
        ),

        PatternRule(
            type = SignalType.REFUND_REQUEST,
            patterns = listOf(
                rx("\\b(?:envoy\\w*|transf\\w*|depos\\w*|depot\\w*|vers\\w*|credit\\w*|recu\\w*|tomb\\w*|arrive\\w*|effectu\\w*)\\b.{0,50}\\bpar erreur\\b"),
                rx("\\bpar erreur\\b.{0,40}\\b(?:argent|fcfa|cfa|xaf|compte|numero|montant|somme)\\b"),
                rx("\\berreur de (?:transfert|depot|numero)\\b"),
                rx("\\b(?:mal tape|envoi errone|transfert errone|mauvais numero|s'est trompe|trompe de numero)\\b"),
                rx("\\berreur a (?:credit\\w*|depos\\w*|envoy\\w*)\\b"),
                rx("\\bvous devez\\b.{0,20}\\b(?:restitu\\w*|rembours\\w*|retourn\\w*|renvoy\\w*)\\b"),
                rx("\\b(?:renvoi\\w*|renvoy\\w*|rembours\\w*|retourn\\w*|restitu\\w*|rends?|rendez)\\b.{0,50}\\b(?:argent|montant|somme|fcfa|cfa|xaf|sous)\\b"),
                rx("\\b(?:remets?|remettre)[- ](?:moi|me)\\b.{0,40}\\b(?:[0-9]{3,}|argent|montant|somme)\\b"),
                rx("\\b(?:sent|transferred|deposited)\\b.{0,40}\\b(?:by mistake|in error|wrongly)\\b"),
                rx("\\bmistakenly\\b.{0,20}\\b(?:sent|transferred)\\b"),
                rx("\\b(?:please|pls|kindly)\\b.{0,10}\\b(?:return|refund|send back)\\b.{0,15}\\b(?:it|money|funds|amount)\\b"),
                rx("\\b(?:send|give|pay) (?:it |the money |the amount |me )?back\\b"),
                rx("\\b(?:send am back|return am|mistake send\\w*|wrong send)\\b"),
            ),
        ),

        PatternRule(
            type = SignalType.PRIZE_LURE,
            patterns = listOf(
                rx("\\bfelicitations?\\b.{0,60}\\b(?:vous|votre|client|abonne|numero|gagn\\w*|lot|prix|tire\\w*|selectionn\\w*)\\b"),
                rx("\\bvous (?:avez|etes) (?:gagne|ete (?:selectionne|choisi|tire))"),
                rx("\\bheureux (?:elu|gagnant)\\b"),
                rx("\\bjeux? concours\\b"),
                rx("\\bvotre lot\\b"),
                rx("\\bgrand(?:e)? gagnant(?:e)?\\b"),
                rx("\\bnumero\\b.{0,20}\\b(?:chanceux|gagnant|tire au sort)\\b"),
                rx("\\b(?:tirage au sort|grand tirage|loterie|tombola)\\b"),
                rx("\\bgagnez\\b.{0,40}\\b(?:moto|iphone|samsung|voiture|telephone|terrain|maison|villa|parcelle|fcfa|cfa|millions?)\\b"),
                rx("\\bpour (?:recevoir|recuperer|retirer|reclamer) (?:votre|ton|ta) (?:voiture|moto|lot|cadeau|gain|prix|terrain)\\b"),
                rx("\\b(?:cadeau|lot|prix)\\b.{0,30}\\b(?:gratuit|offert|a gagner)\\b"),
                rx("\\bcongratulations?\\b"),
                rx("\\byou (?:have )?(?:won|been selected)\\b"),
                rx("\\b(?:winner|lottery|jackpot|prize)\\b"),
                rx("\\b(?:win grand prix|free money|dash you money|e remain small\\w*)\\b"),
            ),
            negations = listOf(
                // « Félicitations pour ton diplôme / à toute l'équipe » : un compliment, pas un appât.
                rx("\\bfelicitations? (?:pour|a toute|a tout|a tous|a l'equipe|a la famille|a toi)\\b"),
            ),
        ),

        PatternRule(
            type = SignalType.SECRET_CODE_REQUEST,
            patterns = listOf(
                // Verbe de saisie/remise + secret sans ambiguïté (PIN, mot de passe, code secret, OTP).
                rx("\\b(?:$GIVE_VERBS|$ENTER_VERBS)\\b.{0,40}\\b$SECRET_WORDS\\b"),
                // « donnez-moi le code que vous venez de recevoir » : code générique, mais à donner à quelqu'un.
                rx("\\b$GIVE_VERBS\\b.{0,30}\\bcode\\b.{0,30}\\b(?:recu|recevoir|sms|verification|confirmation|securite|validation|qui vient)\\b"),
                rx("\\b(?:send|give|share|provide|read out|tell|reply with|confirm|type|enter)\\b.{0,30}\\b(?:pin|otp|password|passcode)\\b"),
                rx("\\b(?:send|give|share|provide|read out|tell|reply with)\\b.{0,30}\\b(?:verification|security|confirmation) code\\b"),
                rx("\\bthe code (?:you|we) (?:received|sent|just)\\b"),
            ),
            negations = listOf(
                rx("\\bjamais\\b.{0,60}\\b(?:$SECRET_WORDS|code)\\b"),
                rx("\\b(?:$SECRET_WORDS|code)\\b.{0,60}\\bjamais\\b"),
                rx("\\bne (?:\\w+ ){0,3}pas\\b.{0,50}\\b(?:$SECRET_WORDS|code)\\b"),
                rx("\\bnever\\b.{0,60}\\b(?:share|give|send|disclose|reveal|tell)\\b"),
                rx("\\b(?:do not|don't|dont) (?:share|give|send|disclose|reveal|tell)\\b"),
            ),
        ),

        PatternRule(
            type = SignalType.ACCOUNT_THREAT,
            patterns = listOf(
                rx("\\b(?:compte|solde|ligne|carte sim|sim|numero|portefeuille)\\b.{0,40}\\b$STATE\\b"),
                rx("\\b(?:sera|va etre|sera automatiquement) $STATE\\b"),
                rx("\\bactivite (?:suspecte|illegale|frauduleuse)\\b"),
                rx("\\bconnexion suspecte\\b"),
                rx("\\b(?:mise|mettez|mettre) a jour\\b.{0,40}\\b(?:compte|kyc|identite|informations?|paiement|donnees)\\b"),
                rx("\\bkyc\\b"),
                rx("\\bverification (?:de votre |de )?(?:identite|compte)\\b"),
                rx("\\bfaute d'identification\\b"),
                rx("\\baccount\\b.{0,40}\\b(?:blocked|suspended|deactivated|closed|restricted|locked)\\b"),
                rx("\\bverify your (?:account|identity)\\b"),
                rx("\\bupdate your (?:account|details|information)\\b"),
                rx("\\bpin will expire\\b"),
            ),
        ),

        PatternRule(
            type = SignalType.IMPERSONATION,
            patterns = listOf(
                rx("\\b(?:service client(?:ele)?|support|assistance|agent|conseiller|equipe|direction|directeur)\\b.{0,25}\\b(?:mtn|orange|momo|mobile money|camtel|nexttel)\\b"),
                rx("\\b(?:mtn|orange|momo|mobile money|camtel|nexttel)\\b.{0,25}\\b(?:service client(?:ele)?|support|assistance|agent|conseiller|equipe)\\b"),
                rx("\\bje suis\\b.{0,30}\\b(?:agent|conseiller|responsable|avocat|de la banque)\\b"),
                rx("\\bici (?:la banque|le service (?:technique|securite|fraude))\\b"),
                rx("\\bservice (?:technique|securite) (?:mtn|orange|momo)\\b"),
                rx("\\b(?:c'est mon nouveau numero|nouveau numero|change de numero)\\b"),
                rx("\\bce n'est pas mon (?:vrai |ancien )?numero\\b"),
                rx("\\bj'ai (?:perdu|casse) mon (?:portable|telephone)\\b"),
                rx("\\bc'est (?:ton|ta|votre) (?:fils|fille|cousin\\w*|oncle|tante|neveu|niece)\\b"),
                rx("\\bdepann\\w*[- ](?:moi|me)\\b"),
                rx("\\bcustomer (?:service|care)\\b"),
                rx("\\bi am a (?:lawyer|barrister|attorney)\\b"),
            ),
        ),

        PatternRule(
            type = SignalType.ADVANCE_FEE,
            patterns = listOf(
                rx("\\b(?:frais|taxes?|droits?)\\b.{0,12}\\b(?:de|d')\\s?(?:dossier|deblocage|dedouanement|douane|traitement|garantie|activation)\\b"),
                rx("\\bfrais notari\\w*"),
                rx("\\b(?:payer|payez|paie|verser|versez|deposer|deposez|regler|reglez|envoyer|envoyez|envoie)\\b.{0,40}\\b(?:frais|taxe|caution|participation)\\b"),
                rx("\\b(?:paie|payez|payer)\\b.{0,20}\\b(?:de|pour le) (?:transport|livraison|dedouanement|douane)\\b"),
                rx("\\bpour (?:debloquer|recevoir|retirer|recuperer|valider)\\b.{0,50}\\b(?:payer|payez|verser|versez|deposer|deposez|envoyer|envoyez|envoie)\\b"),
                rx("\\bparticip\\w*\\b.{0,25}\\bpayant\\b"),
                rx("\\bsans garantie\\b"),
                rx("\\b(?:double|triple|doubler|tripler)\\b.{0,25}\\b(?:48h|24h|heures?|jours?)\\b"),
                rx("\\bdoubl\\w*\\b.{0,30}\\b(?:argent|mise|depot|solde|gain)\\b"),
                rx("\\b(?:send|envoyez|deposez|depose|versez|verse)\\b.{0,50}\\b(?:and|et) (?:receive|recevez|recois|recevras|get)\\b"),
                rx("\\btransfert\\b.{0,30}\\b[0-9]+ ?(?:millions?|milliards?)\\b"),
                rx("\\btu auras [0-9]+ ?%"),
                rx("\\bheritage\\b.{0,40}\\b(?:milliards?|millions?|fcfa|beneficiaire)\\b"),
                rx("\\b(?:lawyer|barrister|attorney|avocat|notaire)\\b.{0,80}\\b(?:left|bequeath\\w*|inheritance|estate|heritage|laisse|legue|decede)\\b"),
                rx("\\b(?:processing|clearance|delivery|release|activation|registration|admission) fees?\\b"),
                rx("\\bpay\\b.{0,30}\\bfees?\\b"),
            ),
        ),

        PatternRule(
            type = SignalType.CONTACT_REQUEST,
            patterns = listOf(
                rx("\\b(?:appel\\w*|rappel\\w*|contact\\w*|joign\\w*|ecri\\w*|call|text)\\b.{0,40}$PHONE"),
                rx("\\b(?:envoie|envoyez|envoyer|depose\\w*|deposez|verse\\w*|transfere\\w*|send)\\b.{0,50}$PHONE"),
                rx("\\b(?:contact\\w*|ecri\\w*|message|joign\\w*)\\b.{0,25}\\bwhatsapp\\b"),
                rx("\\bwhatsapp\\b.{0,15}$PHONE"),
                rx("\\bwa\\.me\\b"),
            ),
        ),
    )
}
