"""Extraction de caractéristiques : miroir exact de TextNormalizer.kt et FeatureHasher.kt.

Toute modification ici doit être reportée dans le code Kotlin (et inversement).
Le test Kotlin `FeatureParityTest` vérifie l'égalité à partir de `feature_parity.tsv`.
"""

import re
import unicodedata

NUM_FEATURES = 2048

_TLDS = (
    "com|net|org|cm|info|xyz|top|club|online|site|link|click|co|me|ly|gl|app|biz|"
    "tk|ml|ga|cf|gq|ws|cc|live|shop|store|vip|icu"
)

URL_RE = re.compile(
    r"(?:https?://|www\.)[^ ]+|"
    r"\b[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:" + _TLDS + r")(?![a-z0-9-])(?:[/@:?#][^ ]*)?"
)
PHONE_RE = re.compile(r"(?<![0-9])(?:(?:\+|00)? ?237[ .-]?)?6(?:[ .-]?[0-9]){8}(?![0-9])")
_WS_RE = re.compile(r"[ \t\n\r\f\v]+")
_DIGITS_RE = re.compile(r"[0-9]+")
_TOKEN_RE = re.compile(r"[a-z]+")


def normalize(text: str) -> str:
    lowered = (
        text.lower()
        .replace("’", "'")
        .replace("‘", "'")
        .replace(" ", " ")
        .replace(" ", " ")
    )
    decomposed = unicodedata.normalize("NFD", lowered)
    no_marks = "".join(c for c in decomposed if not unicodedata.category(c).startswith("M"))
    return _WS_RE.sub(" ", no_marks).strip()


def for_model(text: str) -> str:
    s = normalize(text)
    s = URL_RE.sub(" urltoken ", s)
    s = PHONE_RE.sub(" phonetoken ", s)
    s = _DIGITS_RE.sub(" numtoken ", s)
    return s


def tokens(text: str) -> list[str]:
    return _TOKEN_RE.findall(for_model(text))


def fnv1a32(value: str) -> int:
    h = 0x811C9DC5
    for b in value.encode("utf-8"):
        h ^= b
        h = (h * 0x01000193) & 0xFFFFFFFF
    return h


def indices(text: str) -> list[int]:
    toks = tokens(text)
    feats = set(toks)
    feats.update(toks[i] + " " + toks[i + 1] for i in range(len(toks) - 1))
    return sorted({fnv1a32(f) % NUM_FEATURES for f in feats})


def vectorize(text: str):
    import numpy as np

    v = np.zeros(NUM_FEATURES, dtype=np.float32)
    v[indices(text)] = 1.0
    return v
