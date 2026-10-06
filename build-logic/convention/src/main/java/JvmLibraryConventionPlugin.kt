import com.hancenote.convention.configureKotlin
import com.hancenote.convention.configureNotesTemplateInput
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * 안드로이드를 쓸 이유가 없는 모듈용. `:core:domain` 이 그렇다.
 *
 * 안드로이드 라이브러리로 두면 매니페스트·R·리소스 머지·계측 러너가 전부 딸려오는데
 * 하나도 안 쓴다. 더 중요한 건 **`import android.*` 가 컴파일러 차원에서 막힌다**는 것이다 —
 * "domain 은 순수 Kotlin" 이라는 규칙이 문서가 아니라 빌드로 강제된다.
 */
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

            configureNotesTemplateInput()
        }
    }
}
