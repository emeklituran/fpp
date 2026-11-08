import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Ekosistem genelindeki ortak sürüm ve SDK kararları tek yerde tutulur. */
object Galaxy {
    const val COMPILE_SDK = 36
    /** Android 12 (API 31): Galaxy S25 (Android 15/16) ve son yıllardaki cihazlar; dinamik renk yerleşik. */
    const val MIN_SDK = 31
    /** Android 16 (API 36). */
    const val TARGET_SDK = 36
    val JAVA = JavaVersion.VERSION_17

    /** Bu grupları içeren bir bağımlılık derlemede bulunursa derleme başarısız olur. */
    val FORBIDDEN_GROUPS = listOf(
        "com.google.firebase", "com.google.android.gms", "com.google.android.ads",
        "com.google.ads", "com.facebook", "com.appsflyer", "com.adjust", "io.sentry",
        "com.crashlytics", "com.onesignal", "com.mixpanel", "com.amplitude", "com.segment",
        "com.unity3d.ads", "com.applovin", "com.ironsource", "com.chartboost", "com.vungle",
        "com.inmobi", "com.mopub", "com.flurry", "com.yandex.android", "com.huawei.hms",
        "io.branch", "com.newrelic", "com.datadoghq", "com.bugsnag", "com.instabug",
    )
}

internal val Project.catalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).orElseThrow {
    IllegalStateException("Sürüm kataloğunda '$alias' bulunamadı")
}

internal fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
}

internal fun Project.configureAndroidCommon(ext: CommonExtension<*, *, *, *, *, *>) {
    ext.apply {
        compileSdk = Galaxy.COMPILE_SDK
        defaultConfig {
            minSdk = Galaxy.MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = Galaxy.JAVA
            targetCompatibility = Galaxy.JAVA
        }
        buildFeatures {
            compose = true
            buildConfig = true
        }
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "/META-INF/*.version"
                excludes += "DebugProbesKt.bin"
            }
        }
        testOptions {
            unitTests.isReturnDefaultValues = true
            animationsDisabled = true
        }
        lint {
            abortOnError = true
            checkReleaseBuilds = true
        }
    }
    configureKotlin()

    val bom = dependencies.platform(catalog.lib("androidx-compose-bom"))
    dependencies.add("implementation", bom)
    dependencies.add("androidTestImplementation", bom)
    listOf(
        "androidx-core-ktx", "androidx-lifecycle-runtime-ktx", "androidx-lifecycle-runtime-compose",
        "androidx-lifecycle-viewmodel-compose", "androidx-activity-compose", "androidx-compose-ui",
        "androidx-compose-ui-graphics", "androidx-compose-ui-tooling-preview", "androidx-compose-material3",
        "androidx-compose-material-icons-extended",
    ).forEach { dependencies.add("implementation", catalog.lib(it)) }
    dependencies.add("debugImplementation", catalog.lib("androidx-compose-ui-tooling"))
    dependencies.add("debugImplementation", catalog.lib("androidx-compose-ui-test-manifest"))
    dependencies.add("testImplementation", catalog.lib("junit"))
    listOf("androidx-test-core", "androidx-test-runner", "androidx-test-ext-junit", "androidx-compose-ui-test-junit4", "junit")
        .forEach { dependencies.add("androidTestImplementation", catalog.lib(it)) }
}
