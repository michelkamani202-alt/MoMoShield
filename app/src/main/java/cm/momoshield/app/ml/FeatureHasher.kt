package cm.momoshield.app.ml

import cm.momoshield.app.domain.TextNormalizer

/**
 * Transforme un texte en vecteur d'entrée pour le modèle TensorFlow Lite :
 * « hashing trick » sur les mots (unigrammes) et paires de mots (bigrammes), valeurs binaires.
 *
 * IMPORTANT : doit rester strictement identique à `ml/training/text_features.py`
 * (même normalisation, même fonction de hachage FNV-1a 32 bits, même taille).
 */
object FeatureHasher {

    const val NUM_FEATURES = 2048

    private const val FNV_OFFSET = 0x811C9DC5L
    private const val FNV_PRIME = 0x01000193L
    private const val MASK_32 = 0xFFFFFFFFL

    /** FNV-1a 32 bits sur les octets UTF-8 ; résultat non signé dans un Long. */
    fun fnv1a32(value: String): Long {
        var hash = FNV_OFFSET
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            hash = hash xor (byte.toLong() and 0xFFL)
            hash = (hash * FNV_PRIME) and MASK_32
        }
        return hash
    }

    /** Indices distincts, triés, des caractéristiques actives pour [text]. */
    fun indices(text: String): IntArray {
        val tokens = TextNormalizer.tokens(text)
        val features = HashSet<String>(tokens.size * 2)
        for (i in tokens.indices) {
            features += tokens[i]
            if (i + 1 < tokens.size) features += tokens[i] + " " + tokens[i + 1]
        }
        return features.map { (fnv1a32(it) % NUM_FEATURES).toInt() }.distinct().sorted().toIntArray()
    }

    /** Vecteur d'entrée du modèle : 1.0 aux indices actifs, 0.0 ailleurs. */
    fun vectorize(text: String): FloatArray {
        val vector = FloatArray(NUM_FEATURES)
        for (index in indices(text)) vector[index] = 1f
        return vector
    }
}
