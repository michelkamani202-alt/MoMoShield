"""Jeu de test de contrôle : 40 messages écrits à la main APRÈS le réglage des règles et du
modèle, et évalués une seule fois pour obtenir une mesure honnête (voir le README).

Si vous modifiez les règles ou le modèle en regardant ses erreurs, ce jeu devient un jeu de
réglage : écrivez alors de nouveaux messages pour mesurer la généralisation.
Label 1 = arnaque, 0 = message légitime.
"""

SCAMS = [
    "Cher client Orange Money, suite à une mise à niveau de notre système, veuillez renvoyer votre PIN actuel par SMS pour conserver votre compte.",
    "Bonjour, je me suis trompé en validant le transfert, la somme de 32 000 F est arrivée sur ton compte. S'il te plait remets moi ça aujourd'hui.",
    "GAGNANT! Le numéro 6 99 12 34 56 a été choisi par MTN pour gagner un frigo et 400.000 F. Pour confirmer, appelez le 650 11 22 33.",
    "Offre spéciale: recevez un crédit de 20.000 F gratuit. Il suffit de cliquer sur https://free-credit-mtn.top",
    "Je suis le Dr Mballa, directeur d'une banque à Abidjan. J'ai 7 millions de dollars à transférer, vous aurez 40%. Répondez-moi.",
    "Votre ligne Orange sera coupée dans 2 heures faute de mise à jour de vos données. Rendez-vous sur orange-maj-cm.online",
    "Tantine c'est Brice, j'ai eu un accident et je n'ai plus mon téléphone, envoie 50.000 sur Orange Money au 697 33 44 55 s'il te plaît",
    "Pour toucher votre bourse, envoyez d'abord 10.000 FCFA de frais d'enregistrement au 677 55 66 77, merci",
    "MoMo Alert: Un retrait de 150.000 F est en cours. Si ce n'est pas vous, donnez votre code secret au conseiller pour annuler.",
    "Hello, you have received 250000 FCFA from an overseas client by mistake, kindly refund 200000 to avoid legal action",
    "Le service client MTN vous informe que vous avez gagné 1 million. Veuillez envoyer 8.000 F pour les frais de transfert.",
    "Investissement garanti: déposez 25.000 F aujourd'hui, recevez 75.000 F dans 3 jours. Plus de 500 témoins!",
    "Bonsoir, votre colis Chronopost est en attente. Régularisez les frais de 3.500 F ici: bit.ly/3colis-cm",
    "URGENT votre carte SIM est sur le point d'expirer. Appelez le service client au 699 87 65 43 pour la renouveler gratuitement",
    "Ma chère, ton oncle de l'étranger t'a envoyé 300.000 F. Pour retirer, il faut d'abord payer 15.000 F d'impôt à l'agent",
    "Entrez votre mot de passe MoMo sur http://momo-cm-secure.click pour recevoir votre remboursement de 12.000 F",
    "I'm a US soldier in Syria, I have a box of gold. I need your help to ship it, send $200 for the shipping fees",
    "Camtel: votre facture est impayée. Réglez immédiatement 45.000 F sur le 655 12 12 12 sous peine de coupure définitive",
    "Frère, mon cousin a fait une erreur et a envoyé 40.000 sur ton compte, c'est l'argent des frais de l'hôpital. Retourne-le stp",
    "Félicitations, votre numéro a été sélectionné pour le programme de bourses Orange 2026. Envoyez votre code PIN pour valider votre dossier",
]

LEGIT = [
    "Orange Money: Vous avez retiré 20.000 F chez l'agent Mballa. Frais: 300 F. Solde: 14.250 F. Réf 9981726354",
    "Tonton, le colis est bien arrivé à Garoua. Merci pour tout, que Dieu te bénisse.",
    "Rappel: la séance de travaux pratiques de Systèmes d'exploitation aura lieu jeudi à 14h au labo 3.",
    "MTN MoMo: Paiement de 4.500 F à Total Station Bonapriso réussi. Nouveau solde 22.700 F. ID 5561728844",
    "Salut, tu peux m'envoyer la correction de l'exercice 3? Je l'ai perdue.",
    "Votre code de confirmation est 774512. Il expire dans 5 minutes. Ne le communiquez à personne.",
    "Urgent: le serveur est en panne, merci d'appeler l'administrateur dès que possible.",
    "Bonjour, nous vous informons que votre forfait Orange 5 Go a été activé avec succès jusqu'au 15.",
    "Dimanche on fait le baptême de la petite à 11h, vous êtes tous invités chez nous à Logpom",
    "Le loyer de novembre est à régler avant le 5. Tu peux faire le dépôt par Mobile Money sur mon numéro habituel.",
    "Félicitations pour ton admission à l'Université de Douala! On est très fiers de toi.",
    "Ta commande Jumia est en route et sera livrée demain entre 10h et 14h. Aucun paiement supplémentaire n'est requis.",
    "Je t'ai envoyé 8.000 F, c'est pour le transport de demain. Dis-moi quand ça arrive.",
    "Cher client MTN, restez vigilant: aucun agent ne vous demandera votre code PIN. En cas de doute, appelez le 8000.",
    "Ma mère est à l'hôpital, peux-tu m'appeler quand tu vois ce message? C'est important, je suis au 699 20 30 40.",
    "Les inscriptions pour la certification CCNA sont ouvertes jusqu'au 30 novembre, frais d'examen 45.000 F à régler au secrétariat.",
    "Retrait de 100.000 F effectué avec succès chez l'agent Tchinda. Solde: 3.400 F.",
    "Salut, le prof a dit que le TP est à rendre vendredi par mail. N'oublie pas ton rapport.",
    "Tes résultats du concours sont disponibles. Connecte-toi sur https://www.univ-douala.cm pour les consulter.",
    "Maman a gagné au tournoi de belote hier, elle est trop contente! 😄",
]

HOLDOUT = [(t, 1) for t in SCAMS] + [(t, 0) for t in LEGIT]
