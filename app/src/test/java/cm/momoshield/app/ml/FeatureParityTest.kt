package cm.momoshield.app.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vérifie que l'extraction de caractéristiques Kotlin est identique à celle de Python
 * (`ml/training/text_features.py`) sur les cas de `feature_parity.tsv`.
 * Si ce test échoue, le modèle recevrait des entrées différentes de celles de l'entraînement.
 */
class FeatureParityTest {

    private data class Case(val text: String, val indices: IntArray, val probability: Float)

    private fun loadCases(): List<Case> {
        val stream = javaClass.classLoader!!.getResourceAsStream("feature_parity.tsv")
            ?: error("feature_parity.tsv introuvable : lancez ml/training/train_model.py")
        return stream.bufferedReader(Charsets.UTF_8).readLines().map { line ->
            val parts = line.split('\t')
            require(parts.size == 3) { "Ligne invalide : $line" }
            val indices = if (parts[1].isEmpty()) IntArray(0) else parts[1].split(',').map { it.toInt() }.toIntArray()
            Case(parts[0], indices, parts[2].toFloat())
        }
    }

    @Test
    fun fnv1aMatchesReferenceValues() {
        // Valeurs de référence FNV-1a 32 bits connues.
        assertEquals(0x811C9DC5L, FeatureHasher.fnv1a32(""))
        assertEquals(0xE40C292CL, FeatureHasher.fnv1a32("a"))
        assertEquals(0xBF9CF968L, FeatureHasher.fnv1a32("foobar"))
    }

    @Test
    fun kotlinFeaturesEqualPythonFeatures() {
        val cases = loadCases()
        assertTrue("Au moins 50 cas attendus, trouvé ${cases.size}", cases.size >= 50)
        for (case in cases) {
            assertArrayEquals(
                "Indices différents pour : « ${case.text} »",
                case.indices,
                FeatureHasher.indices(case.text),
            )
        }
    }

    @Test
    fun vectorHasExpectedShapeAndBinaryValues() {
        val v = FeatureHasher.vectorize("Félicitations! Vous avez gagné 5000 FCFA")
        assertEquals(FeatureHasher.NUM_FEATURES, v.size)
        assertTrue(v.all { it == 0f || it == 1f })
        assertTrue(v.sum() > 0f)
        assertEquals(0f, FeatureHasher.vectorize("").sum(), 0f)
    }

    @Test
    fun probabilitiesInParityFileAreValid() {
        for (case in loadCases()) {
            assertTrue(case.probability in 0f..1f)
        }
    }
}
