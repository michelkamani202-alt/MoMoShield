#!/usr/bin/env python3
"""Entraîne le classifieur d'arnaques, le convertit en TensorFlow Lite et vérifie la conversion.

Sorties :
  - app/src/main/assets/scam_classifier.tflite   (modèle embarqué dans l'application)
  - app/src/test/resources/feature_parity.tsv    (cas de test Kotlin <-> Python)
  - ml/training/model_report.json                (métriques d'évaluation)

Usage :
  python build_dataset.py
  python train_model.py
"""

import argparse
import csv
import json
import os
import sys
from pathlib import Path

os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import numpy as np  # noqa: E402
import tensorflow as tf  # noqa: E402
from sklearn.metrics import accuracy_score, precision_score, recall_score, roc_auc_score  # noqa: E402
from sklearn.model_selection import train_test_split  # noqa: E402

from challenge_set import CHALLENGE  # noqa: E402
from text_features import NUM_FEATURES, indices, vectorize  # noqa: E402

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
DEFAULT_DATASET = HERE / "data" / "dataset.csv"
REAL_SAMPLES = HERE / "data" / "real_samples.csv"
MODEL_OUT = ROOT / "app" / "src" / "main" / "assets" / "scam_classifier.tflite"
PARITY_OUT = ROOT / "app" / "src" / "test" / "resources" / "feature_parity.tsv"
REPORT_OUT = HERE / "model_report.json"
SEED = 42

# Textes de contrôle ajoutés au fichier de parité (accents, liens, numéros, cas limites).
PARITY_EXTRAS = [
    "Félicitations! Vous avez gagné 5.000.000 FCFA. Appelez le +237 699 12 34 56",
    "felicitations vous avez gagne une moto, cliquez http://mtn-momo-verif.xyz/win",
    "Ne partagez jamais votre code PIN avec personne.",
    "Vous avez reçu 5 000 FCFA de Paul 677 12 34 56. Nouveau solde: 12 000 FCFA. ID 123456789012",
    "bit.ly/3xK9aZ pour récupérer votre bonus",
    "mtn.cm@evil-site.xyz/login urgent",
    "WINNER!!! You won 100000 XAF, send your PIN now",
    "   espaces    multiples   et espaces insécables ",
    "J’ai envoyé 25000 F par erreur, renvoie-moi l’argent",
    "Œuvre d'art à 6 000 000 F, réf 6991234567890",
    "",
    "!!! ??? ...",
    "ÀÉÎÕÜ çÇ ñ",
    "https://www.mtn.cm/momo offres du mois",
    "Appelle le 00237699123456 ou le 237 699123456.",
]


def load_rows(dataset: Path):
    texts, labels, groups = [], [], []
    with dataset.open(newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            texts.append(row["text"])
            labels.append(int(row["label"]))
            groups.append(row.get("group") or f"row{len(groups)}")
    if REAL_SAMPLES.exists():
        with REAL_SAMPLES.open(newline="", encoding="utf-8") as f:
            for i, row in enumerate(csv.DictReader(f)):
                texts.append(row["text"])
                labels.append(int(row["label"]))
                groups.append(f"real{i}")
        print(f"+ échantillons réels fusionnés depuis {REAL_SAMPLES}")
    return texts, np.array(labels, dtype=np.float32), np.array(groups)


def build_model(hidden: int, dropout: float) -> tf.keras.Model:
    inputs = tf.keras.Input(shape=(NUM_FEATURES,), name="features")
    x = inputs
    if hidden > 0:
        x = tf.keras.layers.Dropout(dropout)(x)  # désactive des mots au hasard : évite le par-cœur
        x = tf.keras.layers.Dense(
            hidden, activation="relu", kernel_regularizer=tf.keras.regularizers.l2(1e-3)
        )(x)
        x = tf.keras.layers.Dropout(dropout)(x)
    outputs = tf.keras.layers.Dense(
        1, activation="sigmoid", name="scam_probability",
        kernel_regularizer=tf.keras.regularizers.l2(1e-3),
    )(x)
    return tf.keras.Model(inputs, outputs, name="scam_classifier")


def convert_to_tflite(model: tf.keras.Model) -> bytes:
    try:
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        return converter.convert()
    except Exception as exc:  # repli pour certaines combinaisons TF/Keras
        print(f"from_keras_model a échoué ({exc!r}); repli via SavedModel", file=sys.stderr)
        export_dir = HERE / "_saved_model"
        model.export(str(export_dir))
        return tf.lite.TFLiteConverter.from_saved_model(str(export_dir)).convert()


def tflite_predict(interpreter, x: np.ndarray) -> float:
    inp = interpreter.get_input_details()[0]
    out = interpreter.get_output_details()[0]
    interpreter.set_tensor(inp["index"], x.reshape(1, NUM_FEATURES).astype(np.float32))
    interpreter.invoke()
    return float(interpreter.get_tensor(out["index"])[0][0])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--epochs", type=int, default=40)
    parser.add_argument("--hidden", type=int, default=16, help="0 = régression logistique")
    parser.add_argument("--dropout", type=float, default=0.3)
    args = parser.parse_args()

    tf.keras.utils.set_random_seed(SEED)
    texts, y, groups = load_rows(args.dataset)
    X = np.stack([vectorize(t) for t in texts])
    print(f"{len(texts)} messages, {int(y.sum())} arnaques, {len(set(groups))} groupes de gabarits")

    # Validation aléatoire stratifiée (pour l'arrêt anticipé). La VRAIE mesure est le jeu de défi
    # écrit à la main (challenge_set.py), jamais utilisé pour entraîner.
    tr_idx, va_idx = train_test_split(
        np.arange(len(texts)), test_size=0.15, random_state=SEED, stratify=y
    )

    model = build_model(args.hidden, args.dropout)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="binary_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(
        X[tr_idx], y[tr_idx],
        validation_data=(X[va_idx], y[va_idx]),
        epochs=args.epochs,
        batch_size=64,
        verbose=2,
        callbacks=[
            tf.keras.callbacks.EarlyStopping(
                monitor="val_loss", patience=6, restore_best_weights=True
            )
        ],
    )

    X_ch = np.stack([vectorize(t) for t, _ in CHALLENGE])
    y_ch = np.array([label for _, label in CHALLENGE])
    proba = model.predict(X_ch, verbose=0).ravel()
    pred = (proba >= 0.5).astype(int)
    val_pred = (model.predict(X[va_idx], verbose=0).ravel() >= 0.5).astype(int)
    report = {
        "samples": len(texts), "train": int(len(tr_idx)), "validation": int(len(va_idx)),
        "validation_accuracy": round(float(accuracy_score(y[va_idx], val_pred)), 4),
        "challenge_size": len(CHALLENGE),
        "challenge_accuracy": round(float(accuracy_score(y_ch, pred)), 4),
        "challenge_precision": round(float(precision_score(y_ch, pred, zero_division=0)), 4),
        "challenge_recall": round(float(recall_score(y_ch, pred, zero_division=0)), 4),
        "challenge_auc": round(float(roc_auc_score(y_ch, proba)), 4),
        "hidden": args.hidden, "dropout": args.dropout,
        "note": "Entraîné sur données synthétiques ; le jeu de défi (100 messages écrits à la main) "
                "n'est jamais vu à l'entraînement mais reste petit.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    REPORT_OUT.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    # ---- Conversion TensorFlow Lite ----
    tflite_bytes = convert_to_tflite(model)
    MODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    MODEL_OUT.write_bytes(tflite_bytes)
    print(f"Modèle TFLite : {MODEL_OUT} ({len(tflite_bytes) / 1024:.0f} Ko)")

    # ---- Vérification : TFLite == Keras ----
    interpreter = tf.lite.Interpreter(model_content=tflite_bytes)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]
    out = interpreter.get_output_details()[0]
    assert list(inp["shape"]) == [1, NUM_FEATURES], inp["shape"]
    assert inp["dtype"] == np.float32 and out["dtype"] == np.float32
    assert list(out["shape"]) == [1, 1], out["shape"]

    check_idx = va_idx[:300]
    keras_p = model.predict(X[check_idx], verbose=0).ravel()
    tflite_p = np.array([tflite_predict(interpreter, X[i]) for i in check_idx])
    max_diff = float(np.max(np.abs(keras_p - tflite_p)))
    print(f"Écart max Keras/TFLite sur {len(check_idx)} messages : {max_diff:.2e}")
    assert max_diff < 1e-4, "La conversion TFLite diffère du modèle Keras"

    # ---- Fichier de parité Kotlin <-> Python ----
    rng = np.random.default_rng(SEED)
    sample_idx = rng.choice(len(texts), size=45, replace=False)
    parity_texts = [texts[i] for i in sample_idx] + [t for t, _ in CHALLENGE[::7]] + PARITY_EXTRAS
    PARITY_OUT.parent.mkdir(parents=True, exist_ok=True)
    with PARITY_OUT.open("w", encoding="utf-8", newline="\n") as f:
        for t in parity_texts:
            clean = t.replace("\t", " ").replace("\r", " ").replace("\n", " ")
            p = tflite_predict(interpreter, vectorize(clean))
            f.write(f"{clean}\t{','.join(map(str, indices(clean)))}\t{p:.6f}\n")
    print(f"Parité : {len(parity_texts)} cas écrits dans {PARITY_OUT}")


if __name__ == "__main__":
    main()
