import com.hance.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * ktlint + compose-rules(`io.nlopez.compose.rules`).
 *
 * compose-rules 는 포매팅이 아니라 **설계 검사**다(`modifier-missing-check`,
 * `unstable-collections` 등). 규칙 설정은 루트 `.editorconfig` 에 둔다.
 */
class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            extensions.configure<KtlintExtension> {
                version.set(libs.findVersion("ktlint").get().requiredVersion)
            }

            dependencies {
                add("ktlintRuleset", libs.findLibrary("compose-rules-ktlint").get())
            }
        }
    }
}
