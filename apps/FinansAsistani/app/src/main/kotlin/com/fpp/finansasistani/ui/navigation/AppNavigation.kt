package com.fpp.finansasistani.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpaceDashboard
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fpp.finansasistani.AppContainer
import com.fpp.finansasistani.ui.editors.DebtEditorScreen
import com.fpp.finansasistani.ui.editors.ExpenseEditorScreen
import com.fpp.finansasistani.ui.editors.IncomeEditorScreen
import com.fpp.finansasistani.ui.editors.SavingEditorScreen
import com.fpp.finansasistani.ui.screens.DashboardScreen
import com.fpp.finansasistani.ui.screens.HelpScreen
import com.fpp.finansasistani.ui.screens.PaymentTableScreen
import com.fpp.finansasistani.ui.screens.RecordKind
import com.fpp.finansasistani.ui.screens.RecordsScreen
import com.fpp.finansasistani.ui.screens.SettingsScreen
import com.fpp.finansasistani.ui.screens.SimulationScreen
import com.fpp.finansasistani.viewmodel.AppViewModelFactory
import com.fpp.finansasistani.viewmodel.FinanceViewModel
import com.fpp.finansasistani.viewmodel.SettingsViewModel
import com.fpp.finansasistani.viewmodel.SimulationViewModel

private object Routes {
    const val DASHBOARD = "ozet"
    const val RECORDS = "kayitlar"
    const val SIMULATION = "plan"
    const val SETTINGS = "ayarlar"
    const val HELP = "yardim"
    const val TABLE = "tablo"
    const val ARG_ID = "id"

    fun editorBase(kind: RecordKind) = when (kind) {
        RecordKind.INCOME -> "gelir"
        RecordKind.DEBT -> "borc"
        RecordKind.EXPENSE -> "gider"
        RecordKind.SAVING -> "birikim"
    }

    fun editorPattern(kind: RecordKind) = "${editorBase(kind)}?$ARG_ID={$ARG_ID}"
    fun editor(kind: RecordKind, id: Long = -1L) = "${editorBase(kind)}?$ARG_ID=$id"
}

private data class TopDestination(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDestination(Routes.DASHBOARD, "Özet", Icons.Outlined.SpaceDashboard),
    TopDestination(Routes.RECORDS, "Kayıtlar", Icons.AutoMirrored.Outlined.ReceiptLong),
    TopDestination(Routes.SIMULATION, "Plan", Icons.AutoMirrored.Outlined.TrendingDown),
    TopDestination(Routes.SETTINGS, "Ayarlar", Icons.Outlined.Settings),
)

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Geri yığınında bir önceki ekran varsa geri döner (çift dokunmada boş ekran oluşmasını önler). */
private fun NavHostController.safeBack() {
    if (previousBackStackEntry != null) popBackStack()
}

@Composable
fun FinansAppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember(container) { AppViewModelFactory(container) }
    // Etkinlik (Activity) kapsamlı ortak ViewModel'lar.
    val financeVm: FinanceViewModel = viewModel(factory = factory)
    val simulationVm: SimulationViewModel = viewModel(factory = factory)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(container) {
        container.messageBus.messages.collect { message -> snackbar.showSnackbar(message) }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = topDestinations.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                snackbar,
                modifier = if (showBottomBar) Modifier else Modifier.navigationBarsPadding().imePadding(),
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navController.navigateTopLevel(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            val openEditor: (RecordKind, Long) -> Unit = { kind, id ->
                navController.navigate(Routes.editor(kind, id)) { launchSingleTop = true }
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    vm = financeVm,
                    onAdd = { openEditor(it, -1L) },
                    onOpenRecords = { navController.navigateTopLevel(Routes.RECORDS) },
                    onOpenSimulation = { navController.navigateTopLevel(Routes.SIMULATION) },
                    onOpenHelp = { navController.navigate(Routes.HELP) { launchSingleTop = true } },
                )
            }
            composable(Routes.RECORDS) {
                RecordsScreen(
                    vm = financeVm,
                    snackbar = snackbar,
                    onAdd = { openEditor(it, -1L) },
                    onEdit = openEditor,
                )
            }
            composable(Routes.SIMULATION) {
                SimulationScreen(
                    financeVm = financeVm,
                    simVm = simulationVm,
                    onAdd = { openEditor(it, -1L) },
                    onOpenTable = { navController.navigate(Routes.TABLE) { launchSingleTop = true } },
                )
            }
            composable(Routes.SETTINGS) {
                val settingsVm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    vm = settingsVm,
                    bus = container.messageBus,
                    onOpenHelp = { navController.navigate(Routes.HELP) { launchSingleTop = true } },
                )
            }
            composable(Routes.HELP) { HelpScreen(onBack = navController::safeBack) }
            composable(Routes.TABLE) { PaymentTableScreen(simVm = simulationVm, onBack = navController::safeBack) }

            RecordKind.entries.forEach { kind ->
                composable(
                    route = Routes.editorPattern(kind),
                    arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType; defaultValue = -1L }),
                ) { entry ->
                    val id = entry.arguments?.getLong(Routes.ARG_ID) ?: -1L
                    val back = navController::safeBack
                    when (kind) {
                        RecordKind.INCOME -> IncomeEditorScreen(id, financeVm, back)
                        RecordKind.DEBT -> DebtEditorScreen(id, financeVm, back)
                        RecordKind.EXPENSE -> ExpenseEditorScreen(id, financeVm, back)
                        RecordKind.SAVING -> SavingEditorScreen(id, financeVm, back)
                    }
                }
            }
        }
    }
}
