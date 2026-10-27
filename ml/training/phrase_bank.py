"""Banque de phrases pour générer des messages par COMPOSITION (plus variés que les gabarits).

Chaque message d'arnaque combine 1 à 3 « indices » (remboursement, gain, demande de code...),
chaque message légitime combine des morceaux de reçus, conseils de sécurité, informations
opérateur et vie courante, y compris des cas piégeux (« urgent », « félicitations »,
numéro de téléphone, lien officiel...). Placeholders : {amount} {phone} {name} {op} {url} {ref} {n}.
"""

OPENERS = [
    "", "", "", "Bonjour,", "Bonsoir,", "Salut,", "Cher client,", "Cher abonné,", "Hello,",
    "Dear customer,", "{op}:", "ALERTE {op}:", "Info:", "Madame, Monsieur,", "Cher(e) {name},",
    "Attention!", "Message important:",
]

# ----------------------------- ARNAQUES -----------------------------
REFUND = [
    "j'ai envoyé {amount} sur votre compte par erreur",
    "mon transfert de {amount} est arrivé chez vous par erreur",
    "j'ai fait une erreur de numéro, l'argent est chez vous",
    "veuillez me renvoyer l'argent",
    "renvoie-moi les {amount} s'il te plaît",
    "remboursez-moi le montant reçu par erreur",
    "retournez {amount} au même numéro",
    "c'était pour quelqu'un d'autre, remets-moi mon argent",
    "I sent {amount} to you by mistake, please send it back",
    "kindly refund the money I sent wrongly",
    "envoi erroné de {amount}, merci de restituer la somme",
    "le dépôt de {amount} était une erreur, retournez-le moi",
    "mauvais numéro, l'argent est chez toi, retourne-le moi",
    "une erreur a crédité votre compte de {amount}, vous devez la rembourser",
]
PRIZE = [
    "vous avez gagné {amount}",
    "félicitations, vous êtes le grand gagnant du tirage {op}",
    "votre numéro a été tiré au sort et remporte {amount}",
    "vous avez gagné une moto offerte par {op}",
    "vous êtes sélectionné pour recevoir un cadeau de {amount}",
    "gagnez {amount} en répondant à ce message",
    "congratulations, you have won {amount} in the {op} promo",
    "you are our lucky winner of {amount}",
    "tombola {op}: un iPhone vous attend",
    "bonus exceptionnel de {amount} offert à votre numéro",
    "vous êtes l'heureux élu du jeu concours, votre lot de {amount} vous attend",
    "your number has been picked, claim your prize of {amount}",
]
PIN = [
    "envoyez-nous votre code PIN",
    "donnez-moi le code que vous venez de recevoir par SMS",
    "communiquez votre code secret pour valider",
    "entrez votre PIN sur le lien ci-dessous",
    "répondez avec votre code de vérification",
    "saisissez votre mot de passe {op} pour confirmer",
    "tapez votre code secret en répondant à ce message",
    "please send your PIN to confirm",
    "reply with the OTP you received",
    "confirm your password to unlock your account",
    "transmettez-moi le code de confirmation reçu",
    "pour annuler l'opération, indiquez votre code PIN",
    "confirmez votre PIN pour continuer à utiliser {op}",
    "fournissez votre code secret à 4 chiffres",
]
THREAT = [
    "votre compte {op} sera suspendu",
    "votre compte a été bloqué",
    "votre compte sera fermé dans 24h",
    "mise à jour KYC obligatoire de votre compte",
    "vérification d'identité requise sinon blocage",
    "votre ligne sera désactivée faute d'identification",
    "your account has been restricted",
    "your account will be closed if you do not verify your identity",
    "activité suspecte détectée sur votre compte {op}",
    "votre solde est gelé",
    "votre numéro est signalé et sera suspendu",
    "votre compte a été piraté",
]
FEE = [
    "payez {amount} de frais de dossier",
    "versez les frais de déblocage de {amount}",
    "envoyez {amount} pour couvrir les frais de dédouanement",
    "une caution de {amount} est exigée",
    "déposez {amount} de frais de traitement pour débloquer votre gain",
    "pay the {amount} release fee first",
    "pour recevoir le prêt, payez d'abord les frais d'inscription",
    "investissez {amount} et doublez votre argent en 48h",
    "prêt sans garantie, frais de dossier {amount}",
    "les frais de transfert de {amount} sont à votre charge",
    "envoyez {amount} sur {phone} pour recevoir le double",
    "registration fee {amount} via Mobile Money",
]
CONTACT = [
    "appelez le {phone}", "contactez-moi sur WhatsApp au {phone}", "écrivez-nous au {phone}",
    "call {phone} now", "joignez notre agent au {phone}", "WhatsApp {phone}",
    "rappelez-moi vite au {phone}", "rappelez immédiatement le {phone}",
]
URGENCY = [
    "c'est urgent", "répondez dans les 24h", "dans 2 heures il sera trop tard",
    "faites-le immédiatement", "au plus vite s'il vous plaît", "sinon votre compte sera bloqué",
    "dernier avertissement", "urgent, reply within 1 hour", "vite, avant minuit",
    "agissez maintenant", "sans délai", "avant ce soir sinon pénalité",
]
LINK = [
    "cliquez ici: {url}", "rendez-vous sur {url}", "confirmez sur {url}",
    "click {url} to claim", "détails: {url}", "connectez-vous: {url}",
]
IMPERSONATION = [
    "je suis agent {op}", "service client {op}", "{op} support team", "ici le conseiller {op}",
    "c'est {name}, mon nouveau numéro", "direction {op}", "maman c'est moi, j'ai changé de numéro",
    "ici le service technique {op}", "je suis de la banque",
    "c'est ton cousin, j'ai perdu mon téléphone",
]
SCAM_PRIMARY = [REFUND, PRIZE, PIN, THREAT, FEE]
SCAM_SECONDARY = [URGENCY, CONTACT, LINK, IMPERSONATION, REFUND, PRIZE, PIN, THREAT, FEE]

# ----------------------------- LÉGITIMES -----------------------------
RECEIPT = [
    "vous avez envoyé {amount} à {name} ({phone}). frais: 100 F. nouveau solde: {amount}. id: {ref}",
    "transfert réussi. {name} a reçu {amount}. solde: {amount}",
    "vous avez reçu {amount} de {name}. nouveau solde: {amount}. réf {ref}",
    "retrait de {amount} effectué chez l'agent {name}. frais: 150 F",
    "paiement de facture {amount} réussi. réf {ref}",
    "achat de crédit de {amount} réussi pour le {phone}",
    "your payment of {amount} to {name} was successful. balance {amount}",
    "dépôt de {amount} effectué. nouveau solde {amount}",
    "transaction {ref} annulée, aucun montant débité",
    "paiement de {amount} confirmé, frais de service 50 F",
    "solde de votre compte: {amount}",
    "you have received {amount} from {name}. transaction id {ref}",
    "retrait annulé, votre compte n'a pas été débité",
    "vous avez payé {amount} à {name}. merci d'utiliser {op}",
]
SECURITY = [
    "ne partagez jamais votre code PIN",
    "ne communiquez jamais votre code secret ni les codes reçus par SMS",
    "{op} ne vous demandera jamais votre PIN",
    "never share your PIN or OTP with anyone",
    "votre code de vérification est {n}, ne le partagez avec personne",
    "your verification code is {n}. do not share it",
    "changez régulièrement votre code PIN",
    "méfiez-vous des arnaques par SMS, ne cliquez pas sur les liens inconnus",
    "en cas de doute, appelez le service client depuis l'application officielle",
    "si vous n'êtes pas à l'origine de cette demande, ignorez ce message",
    "n'acceptez pas les offres de gains par SMS, ce sont des arnaques",
    "aucun agent ne vous demandera votre code secret",
]
INFO = [
    "rechargez {amount} et recevez 2 Go valable 7 jours",
    "profitez du forfait illimité à {amount}, composez #150#",
    "votre forfait expire demain, rechargez pour continuer",
    "bonne année de la part de {op}, merci de votre fidélité",
    "maintenance réseau ce soir de 23h à 2h",
    "découvrez nos offres sur {url}",
    "votre facture du mois est disponible sur {url}",
    "votre forfait a été renouvelé pour {amount}",
    "nouvelle offre {op}: appels illimités le week-end",
    "l'agence de Bonamoussadi est ouverte de 8h à 17h",
    "merci d'avoir contacté le service client, votre demande a été enregistrée",
    "félicitations, votre ligne a été activée avec succès",
    "votre demande de changement de SIM a été enregistrée, passez en agence avec votre CNI",
    "your data bundle expires in 2 days, recharge via {op}",
    "votre numéro a été identifié avec succès", "votre inscription a bien été enregistrée",
    "votre demande est en cours de traitement", "merci d'avoir choisi {op}",
    "votre recharge a été effectuée avec succès", "activation de votre forfait réussie",
]
PERSONAL = [
    "on se voit demain à 15h au marché", "j'arrive ce soir vers 19h",
    "la réunion est reportée à jeudi 10h", "merci pour hier, c'était super",
    "tu peux passer prendre les clés après 17h", "culte de dimanche à 9h",
    "le cours de réseaux est reporté à mardi, venez avec vos TP",
    "rendez-vous à la gare routière à 6h", "can you call me when you get this?",
    "are we still on for the match tonight?", "happy birthday {name}, all the best",
    "félicitations pour ton diplôme {name}", "les résultats sont affichés à la faculté",
    "ton rendez-vous est confirmé lundi à 9h", "j'ai bien reçu ton virement de {amount}, merci",
    "je t'ai envoyé {amount} pour le taxi", "peux-tu me rembourser les {amount} de la tontine ce week-end",
    "bonne nuit, à demain", "on a gagné le match 2-1!",
    "mon téléphone est en charge, je te rappelle plus tard",
    "appelle-moi au {phone} quand tu arrives", "je suis au bureau jusqu'à 17h, appelle si urgent",
    "urgent: réunion à 8h demain, soyez ponctuels",
    "le loyer est à payer avant le 5, tu peux passer par {op}",
    "les frais de scolarité sont à régler à la comptabilité avant le 15",
    "ta commande est en cours de livraison", "ton colis est arrivé au point relais",
    "facture ENEO du mois {amount}, à payer avant le 25", "envoie-moi le rapport avant vendredi",
    "j'ai versé {amount} pour la scolarité, confirme-moi la réception",
    "regarde {url}", "j'ai supprimé le fichier par erreur, tu peux me le renvoyer?",
    "merci pour ton aide, je te rembourse vendredi sans faute",
    "je suis bien arrivé, je t'appelle ce soir", "le bébé a de la fièvre, je l'emmène à l'hôpital",
    "on se retrouve à l'église, apporte ta bible", "n'oublie pas d'apporter ton CV demain",
    "le prof a donné les notes, passe voir à la scolarité",
    "tu as bien reçu les {amount} que je t'ai envoyés?",
    "que Dieu vous bénisse", "bonne fête à toutes les mamans", "félicitations à toute l'équipe",
    "félicitations pour votre mariage", "bravo pour ta promotion", "le livreur est devant la porte",
    "je suis devant chez toi", "bon courage pour les examens", "merci mon frère, que Dieu te garde",
    "mon cher ami, comment vas-tu?", "joyeuses fêtes à toute la famille", "bonne année à tous",
    "joyeux noël, que la paix soit avec vous", "je t'embrasse, à très bientôt",
]
LEGIT_PRIMARY = [RECEIPT, SECURITY, INFO, PERSONAL, PERSONAL]
LEGIT_SECONDARY = [PERSONAL, INFO, SECURITY, RECEIPT]
