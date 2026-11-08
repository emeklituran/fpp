// Satranç kuralları: hamle üretimi chesslib'den; FEN, doğrulama, eşitlik ve beraberlik kuralları bu modülde.
plugins {
    id("galaxy.jvm.library")
}

dependencies {
    implementation(project(":chess-chesslib"))
}

tasks.test {
    maxHeapSize = "1g"
    // Derin perft seti yalnızca -PdeepPerft=true ile (ayrı CI işi); her derlemede hızlı set çalışır.
    systemProperty("deepPerft", (project.findProperty("deepPerft") ?: "false").toString())
}
