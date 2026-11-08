plugins {
    id("galaxy.android.application")
}

android {
    namespace = "com.galaxyoffline.unitconverter"
    defaultConfig {
        applicationId = "com.galaxyoffline.unitconverter"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:math"))
}
