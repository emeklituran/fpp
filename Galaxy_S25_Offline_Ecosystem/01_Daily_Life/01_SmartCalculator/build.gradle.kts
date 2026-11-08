plugins {
    id("galaxy.android.application")
}

android {
    namespace = "com.galaxyoffline.calculator"
    defaultConfig {
        applicationId = "com.galaxyoffline.calculator"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:calculator"))
}
