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
        maven("https://maven-central.storage-download.googleapis.com/maven2/") // Google'ın resmi Maven Central aynası (önce denenir; 5xx hatasında Gradle sonraki depoya geçmez)
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/") // Google'ın resmi Maven Central aynası (önce denenir; 5xx hatasında Gradle sonraki depoya geçmez)
        mavenCentral()
    }
}

rootProject.name = "Galaxy_S25_Offline_Ecosystem"

// Ortak modüller (uygulamalar bunlara derleme zamanında bağlanır; her APK bağımsızdır).
include(":core:common", ":core:math", ":core:crypto", ":core:designsystem", ":core:calculator", ":core:security", ":core:scanner")

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
    ":passwordvault" to "05_Security/81_PasswordVault",
    ":authenticator" to "05_Security/88_Authenticator",
    ":incomeexpense" to "04_Finance/61_IncomeExpense",
    ":qrcode" to "09_Text_Documents/159_QrCode",
    ":pdfreader" to "06_Media/107_PdfReader",
    ":unitconverter" to "02_Calculators/37_UnitConverter",
    ":billreminder" to "03_Home_Life/52_BillReminder",
    ":habittracker" to "01_Daily_Life/19_HabitTracker",
    ":voicerecorder" to "06_Media/103_VoiceRecorder",
    ":timer" to "01_Daily_Life/11_Timer",
    ":chess" to "12_Games/213_Chess",
)
// Satranç uygulamasına özel modüller (yalnızca 12_Games/213_Chess içinde; diğer uygulamalar kullanmaz).
val chessModules = linkedMapOf(
    ":chess-chesslib" to "12_Games/213_Chess/third_party/chesslib",
    ":chess-rules" to "12_Games/213_Chess/rules",
    ":chess-engine" to "12_Games/213_Chess/engine",
)
chessModules.forEach { (path, dir) ->
    include(path)
    project(path).projectDir = file(dir)
}

apps.forEach { (path, dir) ->
    include(path)
    project(path).projectDir = file(dir)
}
