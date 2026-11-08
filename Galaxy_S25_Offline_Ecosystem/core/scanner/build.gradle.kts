plugins { id("galaxy.android.library") }

android { namespace = "com.galaxyoffline.core.scanner" }

dependencies {
    api(project(":core:designsystem"))
    api(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}
