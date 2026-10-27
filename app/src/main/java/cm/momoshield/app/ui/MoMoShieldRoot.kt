package cm.momoshield.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cm.momoshield.app.R
import cm.momoshield.app.ui.screens.AnalyzeScreen
import cm.momoshield.app.ui.screens.HistoryScreen
import cm.momoshield.app.ui.screens.ReportedScreen
import cm.momoshield.app.ui.screens.TipsScreen
import kotlinx.coroutines.launch

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    ANALYZE("analyze", R.string.nav_analyze, Icons.Default.Search),
    HISTORY("history", R.string.nav_history, Icons.AutoMirrored.Filled.List),
    REPORTED("reported", R.string.nav_reported, Icons.Default.Warning),
    TIPS("tips", R.string.nav_tips, Icons.Default.Info),
}

@Composable
fun MoMoShieldRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val reported by viewModel.reportedNumbers.collectAsStateWithLifecycle()
    val reportedSet = remember(reported) { reported.map { it.number }.toSet() }

    LaunchedEffect(viewModel, navController) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    // Dans une coroutine à part : showSnackbar suspend jusqu'à la disparition du message
                    // et bloquerait sinon les événements suivants (navigation comprise).
                    launch { snackbarHostState.showSnackbar(context.getString(event.message.textRes())) }
                }
                UiEvent.OpenAnalyze -> navController.navigate(Destination.ANALYZE.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.ANALYZE.route,
            // Avec enableEdgeToEdge, le clavier ne redimensionne plus la fenêtre : imePadding le fait.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            composable(Destination.ANALYZE.route) {
                AnalyzeScreen(
                    state = state,
                    reportedNumbers = reportedSet,
                    onAnalyze = viewModel::analyze,
                    onClear = viewModel::clearInput,
                    onReportNumber = viewModel::reportNumber,
                )
            }
            composable(Destination.HISTORY.route) {
                HistoryScreen(
                    history = history,
                    onDelete = viewModel::deleteScan,
                    onClearAll = viewModel::clearHistory,
                )
            }
            composable(Destination.REPORTED.route) {
                ReportedScreen(
                    reported = reported,
                    onAdd = viewModel::reportNumber,
                    onRemove = viewModel::unreportNumber,
                )
            }
            composable(Destination.TIPS.route) {
                TipsScreen()
            }
        }
    }
}
