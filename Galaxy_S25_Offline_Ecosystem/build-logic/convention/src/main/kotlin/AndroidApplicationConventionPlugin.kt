import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.kotlin.dsl.configure
import java.util.Properties

/**
 * Her bağımsız uygulama (APK) modülü bu eklentiyi uygular.
 * - Ortak SDK, Compose, R8 küçültme ve imzalama ayarları
 * - `verifyNoTrackers` görevi: reklam/analiz/telemetri SDK'sı bulunursa derleme başarısız olur
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val keystore = Properties().apply {
            val f = rootProject.file("keystore.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val hasKeystore = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
            .all { !keystore.getProperty(it).isNullOrBlank() }

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig {
                targetSdk = Galaxy.TARGET_SDK
                @Suppress("DEPRECATION")
                resourceConfigurations += listOf("tr")
            }
            if (hasKeystore) {
                signingConfigs.create("release") {
                    storeFile = rootProject.file(keystore.getProperty("storeFile"))
                    storePassword = keystore.getProperty("storePassword")
                    keyAlias = keystore.getProperty("keyAlias")
                    keyPassword = keystore.getProperty("keyPassword")
                }
            }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        rootProject.file("gradle/proguard-rules.pro"),
                    )
                    signingConfig = signingConfigs.getByName(if (hasKeystore) "release" else "debug")
                }
            }
            // Google'a özel şifreli bağımlılık meta verisi APK'ya eklenmez.
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
        }

        tasks.register("verifyNoTrackers") {
            group = "verification"
            description = "Reklam, analiz ve telemetri SDK'larının bulunmadığını doğrular."
            doLast {
                val found = configurations.getByName("releaseRuntimeClasspath")
                    .incoming.resolutionResult.allComponents
                    .mapNotNull { it.id as? ModuleComponentIdentifier }
                    .filter { id -> Galaxy.FORBIDDEN_GROUPS.any { id.group == it || id.group.startsWith("$it.") } }
                    .map { "${it.group}:${it.module}:${it.version}" }
                if (found.isNotEmpty()) {
                    throw GradleException("Yasaklı (reklam/analiz/telemetri) bağımlılık bulundu: $found")
                }
                logger.lifecycle("verifyNoTrackers: ${project.path} temiz.")
            }
        }
        Unit
    }
}
