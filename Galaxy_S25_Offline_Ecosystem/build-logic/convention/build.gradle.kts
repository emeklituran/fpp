import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.galaxyoffline.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("galaxyAndroidApplication") {
            id = "galaxy.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("galaxyAndroidLibrary") {
            id = "galaxy.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("galaxyAndroidRoom") {
            id = "galaxy.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("galaxyJvmLibrary") {
            id = "galaxy.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
