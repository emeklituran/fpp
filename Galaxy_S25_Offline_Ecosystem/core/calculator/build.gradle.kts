plugins {
    id("galaxy.android.library")
    id("galaxy.android.room")
}

android { namespace = "com.galaxyoffline.core.calculator" }

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:math"))
}
