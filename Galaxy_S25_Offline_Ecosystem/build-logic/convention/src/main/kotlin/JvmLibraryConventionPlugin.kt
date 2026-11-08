import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/** Android'den bağımsız, JVM üzerinde test edilen saf Kotlin modülleri. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = Galaxy.JAVA
            targetCompatibility = Galaxy.JAVA
        }
        configureKotlin()
        dependencies.add("testImplementation", catalog.lib("junit"))
        Unit
    }
}
