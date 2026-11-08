plugins {
    id("galaxy.android.application")
    id("galaxy.android.room")
}

android {
    namespace = "com.galaxyoffline.passwordvault"
    defaultConfig {
        applicationId = "com.galaxyoffline.passwordvault"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:security"))
    testImplementation(libs.org.json)
}
