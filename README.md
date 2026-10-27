# MoMo Shield — détecteur d’arnaques Mobile Money (Cameroun)

Application Android (Kotlin, Jetpack Compose) qui analyse un SMS ou un message WhatsApp lié à
**MTN MoMo / Orange Money** et dit s’il ressemble à une arnaque, en expliquant pourquoi.

- 100 % local : **aucune permission** (ni Internet, ni lecture des SMS). L’utilisateur colle ou partage le message.
- Détection hybride : **règles + regex** (explicables) **+ modèle TensorFlow Lite** (généralisation).
- Bilingue français / anglais, fonctionne hors connexion, 0 outil payant.

## 1. Démarrer

1. Ouvrir le dossier `MoMoShield` dans **Android Studio** (version récente, JDK 17 inclus).
2. Laisser la synchronisation Gradle se terminer (connexion Internet requise la première fois).
3. Brancher un téléphone (débogage USB) ou lancer un émulateur, puis **Run ▶**.
4. Tests unitaires : `./gradlew testDebugUnitTest` (ou clic droit sur `app/src/test` → *Run Tests*).

Pour tester le partage : dans l’application Messages ou WhatsApp, appuyer longuement sur un message → **Partager** → *MoMo Shield*.

## 2. Plan de développement (étapes réalisées)

| Étape | Contenu | Vérification |
|---|---|---|
| 1. Cadrage | Problème, choix 100 % gratuit, 100 % local | — |
| 2. Squelette | Gradle (catalogue de versions), manifeste sans permission, ressources FR/EN, icône | Contrôles statiques XML / ressources |
| 3. Règles + regex | `TextNormalizer`, `PhoneNumberUtils`, `UrlAnalyzer`, `RuleBook`, `RuleEngine` | 47 tests JUnit exécutés |
| 4. Données | Room (historique, numéros signalés), `ScanRepository` | Relecture indépendante |
| 5. Interface | Compose : Analyser, Historique, Signalés, Conseils + partage de texte | Relecture indépendante |
| 6. TensorFlow Lite | Jeu de données, entraînement, conversion `.tflite`, wrapper Kotlin, fusion avec les règles | Parité Kotlin/Python + TFLite = Keras (écart 1e-7) |
| 7. Contrôle final | Relecture de tous les fichiers, mesures sur jeux de test | Voir §5 |

## 3. Structure du projet

```
MoMoShield/
├── settings.gradle.kts · build.gradle.kts · gradle.properties
├── gradle/libs.versions.toml            # versions centralisées
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/scam_classifier.tflite       # modèle TFLite (≈130 Ko)
│       │   ├── res/ (values FR, values-en, icône, thèmes)
│       │   └── java/cm/momoshield/app/
│       │       ├── MainActivity.kt · MoMoShieldApp.kt
│       │       ├── domain/            # Kotlin pur, testé sur JVM
│       │       │   ├── TextNormalizer.kt    # minuscules, sans accents, jetons
│       │       │   ├── PhoneNumberUtils.kt  # numéros +237 6XXXXXXXX
│       │       │   ├── UrlAnalyzer.kt       # faux domaines, raccourcisseurs, astuce « @ »
│       │       │   ├── RuleBook.kt          # catalogue des regex par type d’indice
│       │       │   ├── RuleEngine.kt        # score = 1 − Π(1 − poids)
│       │       │   ├── RiskCombiner.kt      # fusion règles (60 %) + modèle (40 %)
│       │       │   └── ScamAnalyzer.kt      # point d’entrée du domaine
│       │       ├── ml/
│       │       │   ├── FeatureHasher.kt     # mots + bigrammes → vecteur 2048 (FNV-1a)
│       │       │   └── ScamClassifier.kt    # wrapper TensorFlow Lite
│       │       ├── data/                    # Room + ScanRepository
│       │       └── ui/                      # ViewModel, navigation, écrans Compose
│       └── test/                            # JUnit (règles, parité Python, non-régression)
└── ml/training/                             # chaîne d’entraînement Python
    ├── phrase_bank.py · build_dataset.py    # génère le jeu synthétique
    ├── text_features.py                     # miroir exact de TextNormalizer/FeatureHasher
    ├── train_model.py                       # entraîne, convertit en .tflite, vérifie
    ├── challenge_set.py · holdout_set.py    # jeux de test écrits à la main
    └── requirements.txt
```

## 4. Comment ça marche

1. **Normalisation** : minuscules, accents retirés (beaucoup de SMS sont écrits sans accents).
2. **Règles** : 9 familles d’indices (demande de PIN, « envoi par erreur », faux gain, frais d’avance,
   menace sur le compte, usurpation, urgence, demande de contact, lien suspect) + numéro déjà signalé
   + expéditeur numérique prétendant être un opérateur. Des **négations** évitent de signaler les conseils
   de sécurité (« ne partagez jamais votre PIN »).
3. **Score des règles** = `1 − Π(1 − poids)` : des indices faibles s’additionnent sans dépasser 100.
4. **Modèle TFLite** : 2 couches denses sur un vecteur de 2048 caractéristiques (mots + paires de mots hachés).
   Opérateurs utilisés : `FULLY_CONNECTED` et `LOGISTIC` (version 1), compatibles avec tout runtime TFLite.
5. **Fusion** (`RiskCombiner`) : 60 % règles + 40 % modèle, avec des planchers : numéro signalé ≥ 90, demande de code secret ≥ 60.
   Si le modèle est indisponible, l’application continue avec les règles seules.
6. **Niveaux** : < 30 faible, 30–59 moyen, ≥ 60 élevé.

## 5. Mesures honnêtes (à lire avant de se fier au modèle)

Le modèle est entraîné sur des **messages synthétiques** (gabarits + phrases composées), faute de corpus réel.
Trois jeux de test ont été utilisés :

| Jeu | Rôle | Règles seules | Règles + TFLite |
|---|---|---|---|
| 100 messages écrits à la main (`challenge_set.py`) | **Jeu de réglage** : les règles ont été corrigées en regardant ses erreurs | 100 % | 100 % |
| 40 messages écrits à la main **après** le réglage (`holdout_set.py`) | **Mesure de généralisation**, évaluée une seule fois | précision 100 %, rappel 50 % | précision 100 %, rappel 85 %, exactitude 92,5 %, 0 faux positif |

Lecture : sur des messages jamais vus, **les règles seules manquent la moitié des arnaques** ; le modèle TFLite
récupère la plupart d’entre elles. Les arnaques manquées sont surtout des fictions longues en anglais
(« soldat américain », « héritage »). Le jeu de 40 messages est petit : ne pas lui accorder une précision au point près.

## 6. Améliorer le modèle avec de vrais messages

1. Collecter des SMS d’arnaque et des messages légitimes **anonymisés** (retirer noms et numéros).
2. Les mettre dans `ml/training/data/real_samples.csv` avec les colonnes `text,label` (`1` = arnaque).
3. Relancer :
   ```bash
   cd ml/training
   python -m venv .venv && source .venv/bin/activate
   pip install -r requirements.txt
   python build_dataset.py
   python train_model.py
   ```
   Le script réécrit `app/src/main/assets/scam_classifier.tflite` **et** `app/src/test/resources/feature_parity.tsv`
   (test de parité Kotlin ↔ Python).
4. Toute modification de `TextNormalizer.kt` / `FeatureHasher.kt` doit être reportée dans `text_features.py` (et inversement) :
   le test `FeatureParityTest` échoue si les deux divergent.

## 7. Limites connues

- Le catalogue de règles et la liste de domaines officiels (`UrlAnalyzer.OFFICIAL_DOMAINS`) sont à **vérifier et compléter** avant publication.
- Les formats exacts des SMS officiels MTN/Orange n’ont pas été vérifiés : les règles ne s’appuient pas dessus.
- Le dictionnaire du modèle repose sur des mots exacts : les fautes d’orthographe rares ne sont pas toujours reconnues.
- Aucune synchronisation en ligne des numéros signalés (volontaire : zéro permission). Une base communautaire (Firebase / Supabase)
  nécessiterait la permission Internet et une modération.
- TensorFlow Lite 2.16.1 est la version la plus sûre à résoudre ; pour une publication sur Google Play, évaluer la migration vers
  **LiteRT** (`com.google.ai.edge.litert`), qui remplace TensorFlow Lite.
- L’application n’a pas pu être compilée dans l’environnement de génération (pas de SDK Android) : première compilation à faire dans Android Studio.

## 8. Idées pour la suite

Lecture automatique des notifications (avec consentement explicite), widget « vérifier le presse-papiers »,
base communautaire modérée, mode hors connexion pour les conseils en langues locales.
