pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Galaxy_S25_Offline_Ecosystem"

// Ortak modüller (uygulamalar bunlara derleme zamanında bağlanır; her APK bağımsızdır).
include(":core:common", ":core:math", ":core:designsystem", ":core:calculator")

// Bağımsız APK üreten uygulama modülleri: Gradle yolu -> klasör.
// Liste `python3 tools/ecosystem.py modules` çıktısıyla uyumludur.
val apps = linkedMapOf(
    ":smartcalculator" to "01_Daily_Life/01_SmartCalculator",
    ":scientificcalculator" to "01_Daily_Life/02_ScientificCalculator",
    ":notes" to "01_Daily_Life/03_Notes",
    ":diary" to "01_Daily_Life/04_Diary",
    ":todo" to "01_Daily_Life/05_Todo",
    ":shoppinglist" to "01_Daily_Life/06_ShoppingList",
    ":reminder" to "01_Daily_Life/07_Reminder",
    ":calendar" to "01_Daily_Life/08_Calendar",
    ":alarm" to "01_Daily_Life/09_Alarm",
    ":stopwatch" to "01_Daily_Life/10_Stopwatch",
)
apps.forEach { (path, dir) ->
    include(path)
    project(path).projectDir = file(dir)
}
