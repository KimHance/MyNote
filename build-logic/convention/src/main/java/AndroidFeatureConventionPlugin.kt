import com.hancenote.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("hancenote.android.library.compose")
                apply("hancenote.android.hilt")
            }

            dependencies {
                add("implementation", project(":core:common"))
                add("implementation", project(":core:design-system"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:navigation"))
                add("implementation", libs.findLibrary("androidx.lifecycle.runtime").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.runtimeCompose").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.viewModelCompose").get())
                add("implementation", libs.findLibrary("kotlinx.coroutines.android").get())
            }
        }
    }
}
