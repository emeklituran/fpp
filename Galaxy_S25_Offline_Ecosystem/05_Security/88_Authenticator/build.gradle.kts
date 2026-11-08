plugins {
    id("galaxy.android.application")
    id("galaxy.android.room")
}

android {
    namespace = "com.galaxyoffline.authenticator"
    defaultConfig {
        applicationId = "com.galaxyoffline.authenticator"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:security"))
    implementation(project(":core:scanner"))
    testImplementation(libs.org.json)
}
