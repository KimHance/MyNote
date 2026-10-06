import com.hancenote.convention.configureKotlin
import com.hancenote.convention.configureRepoTestInputs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/** 안드로이드를 쓰지 않는 순수 JVM 모듈용. `import android.*` 가 컴파일 에러라 순수성을 빌드가 강제한다. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("hancenote.ktlint")
            }

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            configureKotlin()

            tasks.withType<Test>().configureEach {
                useJUnit()
                failOnNoDiscoveredTests.set(false)
            }

            configureRepoTestInputs()
        }
    }
}
