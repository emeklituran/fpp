package com.galaxyoffline.shoppinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.shoppinglist.ui.ListDetailScreen
import com.galaxyoffline.shoppinglist.ui.ListsScreen
import com.galaxyoffline.shoppinglist.ui.ShoppingViewModel

val Accent = AppAccent(primaryLight = Color(0xFFD84315), primaryDark = Color(0xFFFFB59E))

val Info = AppInfo(
    name = "Alışveriş Listesi",
    description = "Kategorili, fiyat tahminli ve öneri destekli, tamamen çevrimdışı alışveriş listeleri.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.ShoppingCart,
    limitations = listOf("Fiyatlar sizin girdiğiniz tahmini değerlerdir; mağaza fiyatı çekilmez (internet yok)."),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setGalaxyContent(Accent) { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: ShoppingViewModel = viewModel()
    NavHost(nav, startDestination = "listeler") {
        composable("listeler") {
            ListsScreen(vm, onOpen = { nav.navigate("liste/$it") { launchSingleTop = true } },
                onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } })
        }
        composable("liste/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            ListDetailScreen(vm, entry.arguments?.getLong("id") ?: -1L, onBack = nav::safeBack)
        }
        galaxyStandardScreens(nav, Info) {
            ActionRow("Ürün önerilerini temizle", subtitle = "Daha önce eklenen ürünlerin geçmişini sil", icon = Icons.Outlined.DeleteSweep) { vm.clearHistory() }
        }
    }
}
