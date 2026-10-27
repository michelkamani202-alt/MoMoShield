package cm.momoshield.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cm.momoshield.app.R
import cm.momoshield.app.domain.AnalysisResult
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import cm.momoshield.app.domain.PhoneNumberUtils
import cm.momoshield.app.domain.RuleEngine
import cm.momoshield.app.domain.Signal
import cm.momoshield.app.ui.AnalyzeUiState
import cm.momoshield.app.ui.adviceRes
import cm.momoshield.app.ui.explanationRes
import cm.momoshield.app.ui.theme.riskColor
import cm.momoshield.app.ui.titleRes
import kotlin.math.roundToInt

@Composable
fun AnalyzeScreen(
    state: AnalyzeUiState,
    reportedNumbers: Set<String>,
    onAnalyze: (text: String, sender: String) -> Unit,
    onClear: () -> Unit,
    onReportNumber: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current

    // Le texte en cours de frappe reste local à l'écran (pas de va-et-vient avec un flux), et survit
    // à la rotation. Le ViewModel le remplace seulement via inputVersion (partage, effacement).
    var text by rememberSaveable { mutableStateOf(state.text) }
    var sender by rememberSaveable { mutableStateOf(state.sender) }
    var appliedVersion by rememberSaveable { mutableStateOf(state.inputVersion) }
    LaunchedEffect(state.inputVersion) {
        // Ne recopier que si le ViewModel a imposé un nouveau texte : au simple retour sur l'onglet,
        // on ne doit pas écraser ce que l'utilisateur est en train de saisir.
        if (state.inputVersion != appliedVersion) {
            text = state.text
            sender = state.sender
            appliedVersion = state.inputVersion
        }
    }
    // Le verdict n'est affiché que s'il correspond exactement à ce qui est saisi.
    val visibleResult = state.result?.takeIf { state.text == text && state.sender == sender }
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var notificationEnabled by remember {
        mutableStateOf(
            run {
                val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                flat != null && flat.contains(context.packageName)
            }
        )
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                notificationEnabled = flat != null && flat.contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.analyze_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.analyze_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!notificationEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.notification_banner_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        stringResource(R.string.notification_banner_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.notification_banner_action))
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.notification_active_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.notification_active_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.notification_active_action))
                    }
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(RuleEngine.MAX_INPUT_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_message)) },
            minLines = 5,
            maxLines = 10,
        )
        OutlinedTextField(
            value = sender,
            onValueChange = { sender = it.take(MAX_SENDER_INPUT) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_sender)) },
            singleLine = true,
        )

        Text(stringResource(R.string.preset_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    text = "Cher client, suite à une fausse manoeuvre, j'ai envoyé 45000 f sur votre numero 699123456 par erreur. SVP renvoyez l'argent."
                    sender = "699123456"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.preset_scam_1), style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = {
                    text = "FELICITATIONS! Votre numero a gagne 250,000 FCFA au tirage MTN MoMo. Envoyez 5000 FCFA de frais de retrait au 670000000."
                    sender = "MTN-PRIZE"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.preset_scam_2), style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = {
                    text = "URGENT: Votre compte Momo sera bloque. Donnez votre code secret PIN et le code OTP recu par SMS immediatement."
                    sender = "SERVICE"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.preset_scam_3), style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = {
                clipboard.getText()?.text?.let { text = it.take(RuleEngine.MAX_INPUT_LENGTH) }
            }) {
                Text(stringResource(R.string.action_paste))
            }
            OutlinedButton(
                onClick = onClear,
                enabled = text.isNotEmpty() || sender.isNotEmpty() || state.result != null,
            ) {
                Text(stringResource(R.string.action_clear))
            }
            Button(
                onClick = { onAnalyze(text, sender) },
                enabled = !state.isAnalyzing && text.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(stringResource(R.string.action_analyze))
                }
            }
        }

        visibleResult?.let { result ->
            ResultCard(result = result, reportedNumbers = reportedNumbers, onReportNumber = onReportNumber)
        }

        Text(
            stringResource(R.string.privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val MAX_SENDER_INPUT = 40

@Composable
private fun ResultCard(
    result: AnalysisResult,
    reportedNumbers: Set<String>,
    onReportNumber: (String) -> Unit,
) {
    val color = riskColor(result.level)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, color),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(result.level.titleRes()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(stringResource(R.string.score_format, result.score), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { result.score / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = color,
            )
            Text(stringResource(result.level.adviceRes()), style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                stringResource(R.string.why_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (result.signals.isEmpty()) {
                Text(stringResource(R.string.no_signals), style = MaterialTheme.typography.bodyMedium)
            } else {
                result.signals.forEach { SignalRow(it) }
            }

            val probability = result.modelProbability
            Text(
                text = if (probability != null) {
                    stringResource(R.string.model_probability, (probability * 100).roundToInt())
                } else {
                    stringResource(R.string.model_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(R.string.breakdown_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.breakdown_rules, result.rulesScore),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (probability != null) {
                    stringResource(R.string.breakdown_model, (probability * 100).roundToInt())
                } else {
                    stringResource(R.string.model_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (result.detectedNumbers.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    stringResource(R.string.numbers_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                result.detectedNumbers.forEach { number ->
                    NumberRow(
                        number = number,
                        alreadyReported = number in reportedNumbers,
                        onReport = { onReportNumber(number) },
                    )
                }
            }

            Spacer(Modifier.size(4.dp))
            Text(
                stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignalRow(signal: Signal) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(signal.type.titleRes()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(signal.type.explanationRes()),
            style = MaterialTheme.typography.bodySmall,
        )
        signal.evidence?.takeIf { it.isNotBlank() }?.let { evidence ->
            Text(
                text = "« $evidence »",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumberRow(number: String, alreadyReported: Boolean, onReport: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(PhoneNumberUtils.format(number), style = MaterialTheme.typography.bodyMedium)
        if (alreadyReported) {
            Text(
                stringResource(R.string.already_reported),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            OutlinedButton(onClick = onReport) { Text(stringResource(R.string.action_report)) }
        }
    }
}
