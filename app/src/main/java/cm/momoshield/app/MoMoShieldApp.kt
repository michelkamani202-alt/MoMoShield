package cm.momoshield.app

import android.app.Application
import android.content.Context
import cm.momoshield.app.data.AppDatabase
import cm.momoshield.app.data.ScanRepository
import cm.momoshield.app.ml.ScamClassifier
import cm.momoshield.app.ml.TfLiteScamClassifier

/** Conteneur de dépendances minimal (pas besoin d'un framework d'injection pour ce projet). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    private val classifier: ScamClassifier by lazy { TfLiteScamClassifier(appContext) }

    val repository: ScanRepository by lazy { ScanRepository(database, classifier) }
}

class MoMoShieldApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
