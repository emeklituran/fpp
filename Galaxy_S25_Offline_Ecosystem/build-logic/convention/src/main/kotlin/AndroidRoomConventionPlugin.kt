import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Room kullanan uygulamalar: KSP, Room bağımlılıkları ve şema dışa aktarımı (migration için). */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "${projectDir}/schemas")
            arg("room.generateKotlin", "true")
        }
        dependencies.add("implementation", libs.lib("androidx-room-runtime"))
        dependencies.add("implementation", libs.lib("androidx-room-ktx"))
        dependencies.add("ksp", libs.lib("androidx-room-compiler"))
        Unit
    }
}
