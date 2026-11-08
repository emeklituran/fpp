plugins {
    id("galaxy.android.application")
    id("galaxy.android.room")
}

android {
    namespace = "com.galaxyoffline.voicerecorder"
    defaultConfig {
        applicationId = "com.galaxyoffline.voicerecorder"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
}
