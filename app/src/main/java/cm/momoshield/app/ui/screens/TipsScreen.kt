package cm.momoshield.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cm.momoshield.app.R

private data class Tip(@StringRes val title: Int, @StringRes val body: Int)

private val TIPS = listOf(
    Tip(R.string.tip1_title, R.string.tip1_body),
    Tip(R.string.tip2_title, R.string.tip2_body),
    Tip(R.string.tip3_title, R.string.tip3_body),
    Tip(R.string.tip4_title, R.string.tip4_body),
    Tip(R.string.tip5_title, R.string.tip5_body),
    Tip(R.string.tip6_title, R.string.tip6_body),
    Tip(R.string.tip7_title, R.string.tip7_body),
    Tip(R.string.tip8_title, R.string.tip8_body),
    Tip(R.string.tip9_title, R.string.tip9_body),
)

/** Conseils de prévention : fonctionnent entièrement hors connexion. */
@Composable
fun TipsScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tips_title), style = MaterialTheme.typography.headlineSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TIPS) { tip ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(tip.title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(tip.body), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
