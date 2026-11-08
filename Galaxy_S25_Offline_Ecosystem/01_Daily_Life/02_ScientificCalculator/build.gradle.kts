plugins {
    id("galaxy.android.application")
}

android {
    namespace = "com.galaxyoffline.scientificcalculator"
    defaultConfig {
        applicationId = "com.galaxyoffline.scientificcalculator"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:calculator"))
}
