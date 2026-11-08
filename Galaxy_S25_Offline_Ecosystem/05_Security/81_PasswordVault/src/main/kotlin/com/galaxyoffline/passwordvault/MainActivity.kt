package com.galaxyoffline.passwordvault

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.galaxyoffline.core.designsystem.AppInfo
import com.galaxyoffline.core.designsystem.PermissionUse
import com.galaxyoffline.core.designsystem.screens.GalaxyRoutes
import com.galaxyoffline.core.designsystem.screens.galaxyStandardScreens
import com.galaxyoffline.core.designsystem.screens.safeBack
import com.galaxyoffline.core.designsystem.theme.AppAccent
import com.galaxyoffline.core.designsystem.theme.setGalaxyContent
import com.galaxyoffline.core.security.Biometrics
import com.galaxyoffline.core.security.LockScreen
import com.galaxyoffline.core.security.applySecureWindow
import com.galaxyoffline.passwordvault.ui.GeneratorScreen
import com.galaxyoffline.passwordvault.ui.ItemScreen
import com.galaxyoffline.passwordvault.ui.ListScreen
import com.galaxyoffline.passwordvault.ui.SetupScreen
import com.galaxyoffline.passwordvault.ui.VaultSettingsSection
import com.galaxyoffline.passwordvault.ui.VaultViewModel

val Accent = AppAccent(primaryLight = Color(0xFF1B5E20), primaryDark = Color(0xFF8BD48F))

val Info = AppInfo(
    name = "Şifre Kasası",
    description = "Ana şifre ve AES-256-GCM ile şifrelenmiş, isteğe bağlı biyometrik kilitli, tamamen çevrimdışı şifre kasası.",
    packageName = BuildConfig.APPLICATION_ID,
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    icon = Icons.Outlined.Shield,
    permissions = listOf(PermissionUse("Biyometri", "İsteğe bağlı: kasayı parmak izi veya yüz tanıma ile açmak için.")),
    limitations = listOf(
        "Ana şifre unutulursa veriler kurtarılamaz (arka kapı yoktur).",
        "Tarayıcı ve diğer uygulamalarda otomatik doldurma bu sürümde yoktur.",
        "Yeni parmak izi eklendiğinde güvenlik gereği biyometrik açma kapanır; ana şifreyle açıp yeniden etkinleştirin.",
    ),
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySecureWindow()
        setGalaxyContent(Accent) { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val vm: VaultViewModel = viewModel()
    val setUp by vm.setUp.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val lockout by vm.lockoutSeconds.collectAsStateWithLifecycle()
    val bio by vm.biometricEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when {
        !setUp -> SetupScreen(vm)
        !unlocked -> {
            var bioError by remember { mutableStateOf<String?>(null) }
            LockScreen(Info.name, busy, error ?: bioError, lockout, bio && Biometrics.strongAvailable(context), onUnlock = vm::unlock, onBiometric = {
                val c = vm.bioDecryptCipher()
                val a = context as? FragmentActivity
                if (c == null || a == null) bioError = "Biyometrik anahtar geçersiz (yeni parmak izi eklenmiş olabilir). Ana şifreyi kullanın."
                else Biometrics.authenticateCipher(a, c, "Şifre Kasası", "Kasayı açmak için doğrulayın", vm::unlockWithBiometric) { bioError = it }
            })
        }
        else -> {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "liste") {
                composable("liste") {
                    ListScreen(vm, onOpen = { nav.navigate("kayit/$it") { launchSingleTop = true } },
                        onNew = { nav.navigate("kayit/-1") { launchSingleTop = true } },
                        onGenerator = { nav.navigate("uretici") { launchSingleTop = true } },
                        onSettings = { nav.navigate(GalaxyRoutes.SETTINGS) { launchSingleTop = true } })
                }
                composable("kayit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    ItemScreen(vm, e.arguments?.getLong("id") ?: -1L, onClose = nav::safeBack)
                }
                composable("uretici") { GeneratorScreen(vm, onBack = nav::safeBack) }
                galaxyStandardScreens(nav, Info) { VaultSettingsSection(vm) }
            }
        }
    }
}
