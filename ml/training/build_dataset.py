#!/usr/bin/env python3
"""Génère un jeu de données SYNTHÉTIQUE (arnaques vs messages légitimes) pour démarrer.

Chaque gabarit reçoit un identifiant de groupe : l'entraînement sépare train/test PAR GROUPE,
donc le score de test mesure la généralisation à des formulations jamais vues.

LIMITE IMPORTANTE : des messages synthétiques ne remplacent pas de vrais messages. Pour une
version publiée, ajoutez des messages réels anonymisés (colonne `text`, `label` 1=arnaque) dans
`data/real_samples.csv` : ils sont fusionnés automatiquement par train_model.py.
"""

import argparse
import csv
import random
import re
import unicodedata
from pathlib import Path

import phrase_bank as pb

FIRST_NAMES = [
    "Paul", "Marie", "Junior", "Aïcha", "Brice", "Carine", "Éric", "Flore", "Landry",
    "Sandrine", "Nadège", "Hervé", "Chantal", "Rodrigue", "Estelle", "Armand", "Mireille",
]
OPERATORS = ["MTN", "Orange", "MTN MoMo", "Orange Money", "Mobile Money", "MoMo", "MTN Mobile Money"]
AMOUNTS = [
    "1000", "2000", "5000", "10000", "15 000", "20.000", "25000", "30 000", "50000",
    "75 000", "100000", "150.000", "250 000", "500 000", "1 000 000", "2.500.000", "5.000.000",
]
CURRENCIES = ["FCFA", "F", "XAF", "F CFA", "francs"]
PREFIXES = (
    ["650", "651", "652", "653", "654"] + ["655", "656", "657", "658", "659"]
    + ["670", "671", "672", "673", "674", "675", "676", "677", "678", "679"]
    + ["680", "681", "682", "683", "690", "691", "692", "693", "694", "695", "696", "697", "698", "699"]
    + ["660", "661", "662"]
)
SCAM_URLS = [
    "http://mtn-momo-verif.xyz", "bit.ly/3xK9aZ", "https://orange-money-gain.top/claim",
    "www.mtn-cameroun-promo.online", "http://192.168.4.20/pay", "tinyurl.com/momo-bonus",
    "https://momo-secure-login.site", "orangemoney-verification.club/login",
    "http://mtn.cm@gain-momo.xyz", "cutt.ly/momoGain", "https://momo-bonus-cm.click/win",
]
LEGIT_URLS = [
    "https://www.mtn.cm", "www.orange.cm", "https://momo.mtn.cm/aide", "orange.cm/offres",
    "https://www.camtel.cm", "https://www.mtn.cm/momo",
]

# --------------------------------------------------------------------------------------
# Gabarits d'arnaques (label = 1)
# --------------------------------------------------------------------------------------
SCAM_TEMPLATES = [
    # --- remboursement / erreur de transfert ---
    "Bonjour, j'ai envoyé {amount} par erreur sur votre compte {op}. Merci de me renvoyer l'argent svp.",
    "Frère j'ai fait une erreur de transfert, c'était pour mon ami. Renvoie-moi les {amount} vite stp. Mon numéro {phone}",
    "Vous avez reçu {amount} de {phone}. Message: renvoyez {amount} au même numéro, envoi erroné.",
    "Bonsoir madame, j'ai envoyé {amount} sur votre numéro par erreur. Je suis à l'hôpital, rembourse-moi urgent",
    "Hello, I sent {amount} to your number by mistake. Please send it back to {phone} urgently.",
    "Salut c'est {name}, mon transfert de {amount} est arrivé chez toi par erreur. Retourne l'argent maintenant sinon je porte plainte",
    "Service {op}: une erreur de dépôt de {amount} a été détectée sur votre compte. Veuillez retourner le montant au {phone} dans les 24h.",
    "Mon frère pardon j'ai mal tapé le numéro. Les {amount} sont chez toi. Peux-tu me les remettre svp, je t'appelle après",
    "Papa j'ai envoyé l'argent de la scolarité à ton numéro par erreur ({amount}). Renvoie vite sur {phone}",
    "Transaction {ref}: vous avez reçu {amount}. L'expéditeur a fait une erreur, merci de rembourser immédiatement au {phone}.",
    # --- gain / loterie ---
    "FÉLICITATIONS! Vous avez gagné {amount} au grand tirage {op}. Appelez le {phone} pour récupérer votre lot.",
    "Cher client {op}, vous êtes le gagnant de notre tombola. Pour réclamer {amount} cliquez {url}",
    "Congratulations! You have been selected to win {amount}. Contact {phone} on WhatsApp to claim your prize.",
    "Promo spéciale {op}: gagnez une moto et {amount}. Envoyez votre code PIN pour valider votre participation.",
    "Vous avez gagné un iPhone 15 grâce à la loterie {op} Cameroun. Payez les frais de livraison de {amount} pour le recevoir",
    "{op} anniversaire: vous êtes tiré au sort, bonus de {amount} offert. Cliquez {url} maintenant",
    "WINNER! Your number won {amount} in the {op} lucky draw. Reply with your PIN to receive the money.",
    "Félicitations {name}, votre numéro a gagné {amount}. Pour retirer, envoyez {amount} de frais de dossier au {phone}",
    # --- demande de PIN / code ---
    "{op}: pour annuler la transaction de {amount} entrez votre code PIN sur {url}",
    "Service client {op}. Votre compte est en cours de vérification, communiquez-nous votre code secret et le code reçu par SMS.",
    "Please confirm your OTP code to receive {amount}. Reply with the code now.",
    "Bonjour je suis agent {op}, donnez-moi le code de vérification que vous venez de recevoir pour débloquer votre compte",
    "Sécurité {op}: votre PIN a expiré. Saisissez votre ancien PIN et le nouveau sur {url}",
    "Pour annuler le dépôt de {amount}, tapez votre code secret en répondant à ce message.",
    "{op} support: send us your PIN and the code you received to unlock your wallet.",
    # --- menace sur le compte / KYC ---
    "{op}: votre compte sera suspendu sous 24h. Mettez à jour vos informations KYC sur {url}",
    "Alerte! Votre compte {op} a été bloqué. Cliquez ici {url} pour le réactiver immédiatement.",
    "Dear customer your {op} account has been restricted. Verify your identity at {url}",
    "Votre ligne sera désactivée faute d'identification. Appelez le {phone} pour régulariser votre situation",
    "{op}: dernier avertissement, votre compte sera fermé. Vérification d'identité obligatoire: {url}",
    "Your {op} account will be suspended today. Update your details now: {url}",
    "Mise à jour obligatoire de votre compte {op} avant minuit, sinon blocage définitif. {url}",
    # --- frais d'avance (prêt, emploi, colis, héritage, investissement) ---
    "Prêt rapide sans garantie! Obtenez {amount} en 1h. Déposez d'abord les frais de dossier de {amount} au {phone}",
    "Offre d'emploi: pour valider votre dossier envoyez {amount} de frais d'inscription sur {phone} via {op}",
    "Votre colis est bloqué à la douane. Payez {amount} de frais de dédouanement pour le débloquer: {phone}",
    "Je suis avocat, un parent décédé vous a laissé {amount}. Contactez-moi pour les frais de transfert",
    "Investissez {amount} et recevez le double en 48h. Garanti! Envoyez sur {phone} ({op})",
    "Bourse d'études disponible. Versez {amount} de frais de traitement pour confirmer votre place. {phone}",
    "Your parcel is held. Pay the {amount} clearance fee to {phone} to release it.",
    "Crédit immédiat {amount} sans dossier. Payez la caution de {amount} et recevez l'argent en 10 minutes",
    # --- usurpation d'un proche / agent ---
    "Maman c'est mon nouveau numéro, mon téléphone est cassé. Envoie-moi {amount} sur {op} {phone} urgent",
    "Salut c'est {name}, je suis bloqué en route, dépanne-moi de {amount} stp, je te rembourse ce soir. {phone}",
    "Bonjour, je suis conseiller {op}. Pour sécuriser votre compte appelez le {phone} tout de suite.",
    "Hi it's me, new number. Please send {amount} on {op} to {phone}, it's urgent, I'll explain later.",
    "Agent {op} agréé: votre transaction de {amount} est en attente, contactez-moi sur WhatsApp {phone}",
    # --- lien-appât ---
    "Cliquez {url} pour gagner un bonus de {amount} {op}",
    "{op}: recevez {amount} gratuitement, offre limitée, inscrivez-vous sur {url}",
    "Votre remboursement de {amount} est prêt. Récupérez-le ici: {url}",
    "Nouvelle promo {op}: doublez votre solde instantanément! {url}",
    "Facture impayée {amount}. Réglez maintenant pour éviter la coupure: {url}",
]

# --------------------------------------------------------------------------------------
# Gabarits légitimes (label = 0)
# --------------------------------------------------------------------------------------
LEGIT_TEMPLATES = [
    # --- confirmations de transaction ---
    "Vous avez envoyé {amount} à {name} ({phone}). Frais: 100 F. Nouveau solde: {amount}. ID de transaction: {ref}.",
    "Transfert réussi. {name} a reçu {amount}. Solde disponible: {amount}. Réf {ref}",
    "Vous avez reçu {amount} de {name} {phone}. Votre nouveau solde est de {amount}. ID: {ref}",
    "Retrait de {amount} effectué chez l'agent {name}. Frais: 150 F. Solde: {amount}. Réf: {ref}",
    "Paiement de facture ENEO {amount} réussi. Réf {ref}. Merci d'utiliser {op}.",
    "Achat de crédit de {amount} réussi pour le {phone}.",
    "Your payment of {amount} to {name} was successful. Balance: {amount}. Transaction ID {ref}.",
    "Dépôt de {amount} effectué sur votre compte {op}. Nouveau solde {amount}.",
    "Paiement de {amount} à {name} confirmé. Frais de service: 50 F. Réf {ref}.",
    "Vous avez payé votre abonnement Canal+ de {amount}. Transaction {ref} réussie.",
    "Transaction {ref} annulée. Aucun montant n'a été débité de votre compte.",
    "Solde de votre compte {op}: {amount}. Dernière opération le 12/05.",
    # --- conseils de sécurité ---
    "Ne partagez jamais votre code PIN {op}. Aucun agent ne vous le demandera.",
    "Pour votre sécurité, ne communiquez jamais votre code secret ni les codes reçus par SMS.",
    "Attention aux arnaques: {op} ne vous demandera jamais votre PIN par SMS ou par appel.",
    "Never share your PIN or OTP with anyone. {op} will never ask for it.",
    "Votre code de vérification est {n}. Ne le partagez avec personne.",
    "Your verification code is {n}. Do not share it with anyone.",
    "Conseil sécurité: changez régulièrement votre code PIN et ne le notez jamais sur votre téléphone.",
    "{op} vous rappelle: en cas de doute, appelez le service client depuis le menu officiel de l'application.",
    # --- promotions et informations opérateur ---
    "{op}: rechargez {amount} et recevez 2 Go valable 7 jours. Tapez *123#.",
    "Profitez du forfait illimité à {amount}. Composez #150# pour activer.",
    "Votre forfait internet expire demain. Rechargez via {op} pour continuer à naviguer.",
    "Bonne année de la part de {op}! Merci de votre fidélité.",
    "{op} vous informe d'une maintenance réseau ce soir de 23h à 2h. Merci de votre compréhension.",
    "Découvrez nos offres sur {url}",
    "Votre facture {op} du mois est disponible. Consultez-la sur {url} ou en agence.",
    "Nouveau: transférez de l'argent à l'étranger depuis l'application {op}. Plus d'infos sur {url}",
    "Votre forfait a été renouvelé pour {amount}. Valable jusqu'au 30.",
    # --- famille, amis, travail, études ---
    "Salut, on se voit demain à 15h au marché Mokolo. Apporte les documents.",
    "Maman, j'arrive ce soir vers 19h. Prépare le ndolé stp.",
    "Rappel: réunion du département jeudi à 10h en salle 204.",
    "Merci pour hier, c'était super! À bientôt.",
    "Je t'envoie {amount} pour le taxi et le repas, tu vas recevoir un SMS de confirmation.",
    "Peux-tu me rembourser les {amount} de la tontine ce week-end? Pas de souci si tu es serré.",
    "Hey, are we still on for the match tonight? Kickoff is at 8pm.",
    "Ton colis est arrivé au point relais de Bonamoussadi. Présente ta pièce d'identité pour le retirer.",
    "Les résultats du semestre sont affichés sur le tableau à la faculté.",
    "Happy birthday {name}! Wishing you all the best.",
    "Bonjour, votre rendez-vous est confirmé pour lundi à 9h à la clinique.",
    "Papa a dit que tu peux passer prendre les clés après 17h.",
    "Culte de dimanche à 9h. Apportez vos bibles.",
    "Facture ENEO du mois: {amount}. Date limite de paiement le 25. Payez via {op} ou en agence.",
    "Merci de m'envoyer le rapport avant vendredi.",
    "Urgent: réunion à 8h demain, merci d'être ponctuel.",
    "Bonsoir, je t'ai envoyé {amount} pour les frais de scolarité de ton frère, dis-moi quand tu reçois.",
    "Le cours de réseaux est reporté à mardi. Venez avec vos TP.",
    "J'ai bien reçu ton virement de {amount}, merci beaucoup! On se parle demain.",
    "Rendez-vous à la gare routière de Douala à 6h, le bus part à 6h30.",
    "Can you call me when you get this? I'm at the office until 5pm.",
    "Félicitations pour ton diplôme {name}! Toute la famille est fière de toi.",
    "Ta commande est en cours de livraison, le livreur t'appellera à son arrivée.",
    "Le loyer du mois est à payer avant le 5, merci. Tu peux passer par {op}.",
    "Hello, the electricity bill is {amount}. I will pay it tomorrow morning.",
    "Bonjour {name}, ton entretien est prévu mercredi à 14h. Pense à apporter ton CV.",
    "Les frais de scolarité du second semestre sont à régler à la comptabilité avant le 15.",
    "Merci de confirmer votre présence à la réunion de demain avant 18h.",
]

ABBREVIATIONS = [
    (r"\bs'il vous plaît\b", "svp"), (r"\bs'il te plaît\b", "stp"), (r"\bbonjour\b", "bjr"),
    (r"\bbonsoir\b", "bsr"), (r"\bpour\b", "pr"), (r"\bmaintenant\b", "mtn"), (r"\bmerci\b", "mrc"),
    (r"\btoujours\b", "tjrs"), (r"\bplease\b", "pls"), (r"\bmessage\b", "msg"),
]
PREFIX_NOISE = ["", "", "", "Bonjour, ", "Salut, ", "Hello, ", "Attention! ", "ALERTE: ", "Info: "]
SUFFIX_NOISE = ["", "", "", " Merci.", " Merci!", " 🙏", " 🙂", " Cordialement.", " ...", " !!!"]

PLACEHOLDER = re.compile(r"\{(\w+)\}")


def strip_accents(s: str) -> str:
    d = unicodedata.normalize("NFD", s)
    return "".join(c for c in d if not unicodedata.category(c).startswith("M"))


def make_phone(r: random.Random) -> str:
    digits = r.choice(PREFIXES) + "".join(r.choice("0123456789") for _ in range(6))
    style = r.choice(["plain", "spaced", "intl", "intl_spaced", "dashed", "pairs"])
    if style == "plain":
        return digits
    if style == "spaced":
        return f"{digits[:3]} {digits[3:6]} {digits[6:]}"
    if style == "intl":
        return "+237" + digits
    if style == "intl_spaced":
        return f"+237 {digits[0]} {digits[1:3]} {digits[3:5]} {digits[5:7]} {digits[7:]}"
    if style == "dashed":
        return f"{digits[:3]}-{digits[3:6]}-{digits[6:]}"
    return f"{digits[0]} {digits[1:3]} {digits[3:5]} {digits[5:7]} {digits[7:]}"


def fill(template: str, r: random.Random, urls: list[str]) -> str:
    makers = {
        "amount": lambda: f"{r.choice(AMOUNTS)} {r.choice(CURRENCIES)}",
        "phone": lambda: make_phone(r),
        "name": lambda: r.choice(FIRST_NAMES),
        "op": lambda: r.choice(OPERATORS),
        "url": lambda: r.choice(urls),
        "ref": lambda: "".join(r.choice("0123456789") for _ in range(r.choice([8, 10, 12]))),
        "n": lambda: "".join(r.choice("0123456789") for _ in range(r.choice([4, 5, 6]))),
    }
    return PLACEHOLDER.sub(lambda m: makers[m.group(1)](), template)


def typo(word: str, r: random.Random) -> str:
    if len(word) < 4:
        return word
    i = r.randrange(1, len(word) - 2)
    return word[:i] + word[i + 1] + word[i] + word[i + 2:]


def augment(text: str, r: random.Random) -> str:
    text = r.choice(PREFIX_NOISE) + text + r.choice(SUFFIX_NOISE)
    if r.random() < 0.20:
        for pattern, repl in ABBREVIATIONS:
            if r.random() < 0.6:
                text = re.sub(pattern, repl, text, flags=re.IGNORECASE)
    if r.random() < 0.15:
        words = text.split(" ")
        j = r.randrange(len(words))
        words[j] = typo(words[j], r)
        text = " ".join(words)
    if r.random() < 0.35:
        text = strip_accents(text)
    case = r.random()
    if case < 0.25:
        text = text.lower()
    elif case < 0.30:
        text = text.upper()
    if r.random() < 0.12:
        text = re.sub(r"[!?.,:;]", "", text)
    if r.random() < 0.05:
        text = text.replace(" ", "  ", 2)
    return text.strip()


def compose(r: random.Random, label: int) -> str:
    """Assemble 1 à 3 morceaux de la banque de phrases en un message."""
    if label == 1:
        primary, secondary = pb.SCAM_PRIMARY, pb.SCAM_SECONDARY
    else:
        primary, secondary = pb.LEGIT_PRIMARY, pb.LEGIT_SECONDARY
    pieces = [r.choice(r.choice(primary))]
    for _ in range(r.choices([0, 1, 2], weights=[3, 4, 2])[0]):
        pieces.append(r.choice(r.choice(secondary)))
    if label == 1 and len(pieces) > 1 and r.random() < 0.5:
        r.shuffle(pieces)
    sep = r.choice([". ", ". ", ", ", " - ", " ! ", " "])
    body = sep.join(pieces)
    opener = r.choice(pb.OPENERS)
    text = (opener + " " + body).strip()
    return text[:1].upper() + text[1:] if r.random() < 0.7 else text


def generate(per_template: int, seed: int, composed: int):
    r = random.Random(seed)
    rows = []
    for label, templates, urls, prefix in (
        (1, SCAM_TEMPLATES, SCAM_URLS, "S"),
        (0, LEGIT_TEMPLATES, LEGIT_URLS, "L"),
    ):
        for idx, template in enumerate(templates):
            seen = set()
            attempts = 0
            while len(seen) < per_template and attempts < per_template * 20:
                attempts += 1
                text = augment(fill(template, r, urls), r)
                if text not in seen:
                    seen.add(text)
                    rows.append((text, label, f"{prefix}{idx:03d}"))
        seen = set()
        attempts = 0
        while len(seen) < composed and attempts < composed * 20:
            attempts += 1
            text = augment(fill(compose(r, label), r, urls), r)
            if text not in seen:
                seen.add(text)
                rows.append((text, label, f"C{label}"))
    r.shuffle(rows)
    return rows


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--per-template", type=int, default=20)
    parser.add_argument("--composed", type=int, default=2500, help="messages composes par classe")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "data" / "dataset.csv")
    args = parser.parse_args()

    rows = generate(args.per_template, args.seed, args.composed)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["text", "label", "group"])
        w.writerows(rows)
    scam = sum(1 for _, y, _ in rows if y == 1)
    print(f"{len(rows)} messages écrits dans {args.out} (arnaques: {scam}, légitimes: {len(rows) - scam})")
    print(f"gabarits: {len(SCAM_TEMPLATES)} arnaques, {len(LEGIT_TEMPLATES)} légitimes")


if __name__ == "__main__":
    main()
