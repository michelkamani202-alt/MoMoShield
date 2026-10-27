package cm.momoshield.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import cm.momoshield.app.ui.MainViewModel
import cm.momoshield.app.ui.MainViewModelFactory
import cm.momoshield.app.ui.MoMoShieldRoot
import cm.momoshield.app.ui.theme.MoMoShieldTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MoMoShieldApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Après une rotation, l'intent d'origine est rejoué : on ne le traite qu'au premier lancement.
        if (savedInstanceState == null) handleShareIntent(intent)
        setContent {
            MoMoShieldTheme {
                MoMoShieldRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** « Partager » un SMS ou un message depuis une autre application l'envoie à l'analyse. */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return
        if (intent.type?.startsWith("text/") != true) return
        val shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        if (!shared.isNullOrBlank()) viewModel.onSharedText(shared)
    }
}
