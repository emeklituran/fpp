// Eklenti bağımlılıkları önce Maven Central'dan alınır (Gradle eklenti portalı bazı dosyaları GitHub'a yönlendirir;
// GitHub indirme sunucusu kesintideyken derleme durmasın).
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
rootProject.name = "build-logic"
include(":convention")
