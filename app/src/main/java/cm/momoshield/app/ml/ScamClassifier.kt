package cm.momoshield.app.ml

import android.content.Context
import android.util.Log
import java.io.FileInputStream
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

/** Classifieur de texte : renvoie la probabilité (0-1) qu'un message soit une arnaque. */
interface ScamClassifier : AutoCloseable {
    /** Probabilité d'arnaque, ou null si le modèle est indisponible ou a échoué. */
    fun predict(text: String): Float?
}

/**
 * Implémentation TensorFlow Lite. Le modèle `scam_classifier.tflite` (assets) prend un vecteur de
 * [FeatureHasher.NUM_FEATURES] flottants et renvoie une probabilité. Si le chargement échoue
 * (fichier absent, bibliothèque native non supportée...), [predict] renvoie null et l'application
 * continue avec le moteur de règles seul.
 */
class TfLiteScamClassifier(context: Context) : ScamClassifier {

    private val appContext = context.applicationContext

    // Chargé au premier predict(), qui s'exécute hors du thread principal (Dispatchers.Default).
    private val lazyInterpreter = lazy { loadInterpreter(appContext) }
    private val interpreter: Interpreter? get() = lazyInterpreter.value
    private val input = Array(1) { FloatArray(FeatureHasher.NUM_FEATURES) }
    private val output = Array(1) { FloatArray(1) }

    @Synchronized
    override fun predict(text: String): Float? {
        val tflite = interpreter ?: return null
        return try {
            val features = FeatureHasher.vectorize(text)
            System.arraycopy(features, 0, input[0], 0, features.size)
            tflite.run(input, output)
            output[0][0].takeIf { !it.isNaN() }?.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "Inférence TensorFlow Lite impossible", e)
            null
        }
    }

    @Synchronized
    override fun close() {
        if (lazyInterpreter.isInitialized()) interpreter?.close()
    }

    private fun loadInterpreter(context: Context): Interpreter? =
        try {
            val model = context.assets.openFd(MODEL_FILE).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
            Interpreter(model, Interpreter.Options().setNumThreads(NUM_THREADS))
        } catch (t: Throwable) {
            // Throwable : couvre aussi UnsatisfiedLinkError (bibliothèque native absente).
            Log.e(TAG, "Modèle $MODEL_FILE non chargé : analyse par règles uniquement", t)
            null
        }

    private companion object {
        const val TAG = "TfLiteScamClassifier"
        const val MODEL_FILE = "scam_classifier.tflite"
        const val NUM_THREADS = 2
    }
}
