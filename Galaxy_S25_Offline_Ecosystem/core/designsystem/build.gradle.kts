plugins { id("galaxy.android.library") }

android { namespace = "com.galaxyoffline.core.designsystem" }

dependencies {
    api(project(":core:common"))
    api(libs.androidx.datastore.preferences)
    api(libs.androidx.navigation.compose)
}
