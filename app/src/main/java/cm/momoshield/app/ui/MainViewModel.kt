package cm.momoshield.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cm.momoshield.app.data.ReportedNumberEntity
import cm.momoshield.app.data.ScanEntity
import cm.momoshield.app.data.ScanRepository
import cm.momoshield.app.domain.AnalysisResult
import cm.momoshield.app.domain.RuleEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * État de l'écran d'analyse. Le texte en cours de frappe vit dans l'écran (Compose) pour éviter les
 * sauts de curseur ; le ViewModel ne reçoit le texte qu'à l'analyse. [inputVersion] augmente quand
 * le ViewModel impose un nouveau texte (partage, effacement) pour que l'écran le recopie.
 */
data class AnalyzeUiState(
    val text: String = "",
    val sender: String = "",
    val inputVersion: Int = 0,
    val isAnalyzing: Boolean = false,
    val result: AnalysisResult? = null,
)

enum class UiMessage { NUMBER_REPORTED, INVALID_NUMBER, EMPTY_TEXT, ANALYSIS_FAILED }

/** Événements ponctuels (message à afficher, navigation) consommés une seule fois par l'UI. */
sealed interface UiEvent {
    data class ShowMessage(val message: UiMessage) : UiEvent
    data object OpenAnalyze : UiEvent
}

class MainViewModel(private val repository: ScanRepository) : ViewModel() {

    private val _state = MutableStateFlow(AnalyzeUiState())
    val state: StateFlow<AnalyzeUiState> = _state.asStateFlow()

    val history: StateFlow<List<ScanEntity>> = repository.history.toState(emptyList())
    val reportedNumbers: StateFlow<List<ReportedNumberEntity>> = repository.reportedNumbers.toState(emptyList())

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    private fun <T> Flow<T>.toState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    private var analysisJob: Job? = null

    fun clearInput() {
        analysisJob?.cancel()
        _state.update { AnalyzeUiState(inputVersion = it.inputVersion + 1) }
    }

    /** Texte reçu par « Partager » depuis une autre application : on l'analyse tout de suite. */
    fun onSharedText(text: String) {
        analysisJob?.cancel()
        val clipped = text.take(RuleEngine.MAX_INPUT_LENGTH)
        _state.update { AnalyzeUiState(text = clipped, inputVersion = it.inputVersion + 1) }
        _events.trySend(UiEvent.OpenAnalyze)
        analyze(clipped, "")
    }

    fun analyze(text: String, sender: String) {
        val cleanText = text.take(RuleEngine.MAX_INPUT_LENGTH)
        val cleanSender = sender.take(MAX_SENDER_LENGTH)
        if (cleanText.isBlank()) {
            _events.trySend(UiEvent.ShowMessage(UiMessage.EMPTY_TEXT))
            return
        }
        analysisJob?.cancel() // une analyse plus récente remplace l'ancienne
        _state.update { it.copy(text = cleanText, sender = cleanSender, isAnalyzing = true, result = null) }
        analysisJob = viewModelScope.launch {
            val result = try {
                repository.analyze(cleanText, cleanSender)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _state.update { it.copy(isAnalyzing = false, result = result) }
            if (result == null) _events.trySend(UiEvent.ShowMessage(UiMessage.ANALYSIS_FAILED))
        }
    }

    fun reportNumber(raw: String) {
        viewModelScope.launch {
            val ok = repository.reportNumber(raw)
            _events.trySend(UiEvent.ShowMessage(if (ok) UiMessage.NUMBER_REPORTED else UiMessage.INVALID_NUMBER))
        }
    }

    fun unreportNumber(number: String) {
        viewModelScope.launch { repository.unreportNumber(number) }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch { repository.deleteScan(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_SENDER_LENGTH = 40
    }
}

class MainViewModelFactory(private val repository: ScanRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) { "ViewModel inconnu : $modelClass" }
        return MainViewModel(repository) as T
    }
}
