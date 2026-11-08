// Eklenti bağımlılıkları önce Maven Central'dan alınır (Gradle eklenti portalı bazı dosyaları GitHub'a yönlendirir;
// GitHub indirme sunucusu kesintideyken derleme durmasın).
pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/") // Google'ın resmi Maven Central aynası (önce denenir; 5xx hatasında Gradle sonraki depoya geçmez)
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/") // Google'ın resmi Maven Central aynası (önce denenir; 5xx hatasında Gradle sonraki depoya geçmez)
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
rootProject.name = "build-logic"
include(":convention")
