plugins { id("galaxy.android.library") }

android { namespace = "com.galaxyoffline.core.security" }

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:crypto"))
    api(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.process)
}
