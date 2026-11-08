import java.security.MessageDigest

plugins {
    id("galaxy.android.application")
    id("galaxy.android.room")
}

// Stockfish ikilileri depoya konmaz (≈100 MB); engine/build_stockfish.sh tarafından bu klasöre üretilir (CI'da önbellekli).
val stockfishDir = layout.projectDirectory.dir("engine/prebuilt")

android {
    namespace = "com.galaxyoffline.chess"
    defaultConfig {
        applicationId = "com.galaxyoffline.chess"
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        // Release: yalnızca arm64 (Galaxy S25). Debug: arm64 + x86_64 (emülatör testleri).
        getByName("release") { ndk { abiFilters += "arm64-v8a" } }
        getByName("debug") { ndk { abiFilters += listOf("arm64-v8a", "x86_64") } }
    }
    sourceSets.getByName("main").jniLibs.srcDir(stockfishDir.dir("jniLibs"))
    packaging {
        jniLibs {
            // Motor ayrı süreç olarak çalıştırılır: dosyanın yerel kütüphane klasörüne çıkarılması gerekir.
            useLegacyPackaging = true
            keepDebugSymbols += "**/libstockfish.so"
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":chess-rules"))
    implementation(project(":chess-engine"))
    implementation(libs.androidx.lifecycle.process)
}

/** Motor dosyaları yoksa ya da özetleri tutmuyorsa derleme durur (motorsuz APK sessizce üretilmesin). */
val verifyStockfish by tasks.registering {
    val dir = stockfishDir
    doLast {
        val sums = dir.file("SHA256SUMS").asFile
        if (!sums.exists()) throw GradleException(
            "Stockfish ikilileri yok: ${dir.asFile}. Önce engine/build_stockfish.sh ${dir.asFile} çalıştırılmalı (CI bunu yapar).")
        sums.readLines().filter { it.isNotBlank() }.forEach { line ->
            val (sha, rel) = line.trim().split(Regex("\\s+"), limit = 2)
            val f = dir.dir("jniLibs").file(rel).asFile
            if (!f.exists()) throw GradleException("Eksik motor dosyası: $f")
            val got = MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }
            if (got != sha) throw GradleException("Motor dosyası özeti tutmuyor: $rel")
        }
    }
}
tasks.named("preBuild") { dependsOn(verifyStockfish) }
