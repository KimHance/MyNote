import com.android.build.api.dsl.LibraryExtension
import com.hancenote.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("hancenote.android.library.compose")
                apply("hancenote.android.hilt")
            }

            // 문구 접두사는 모듈 이름 그대로다(`:feature:quiz` → `quiz_`). lint 는 접두사를 적은 모듈만 보므로
            // 새 기능 모듈이 빼먹지 않게 여기서 건다 — 파이썬 FM-11 시험이 이 줄을 읽는다.
            extensions.configure<LibraryExtension> {
                resourcePrefix = "${name}_"
            }

            // 기능 모듈은 서로를 의존하지 않는다(FM-2). 두 기능 이상이 쓰는 것은 `:core:ui` 를 거친다.
            dependencies {
                add("implementation", project(":core:common"))
                add("implementation", project(":core:design-system"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:navigation"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.findLibrary("androidx.lifecycle.runtime").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.runtimeCompose").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.viewModelCompose").get())
                add("implementation", libs.findLibrary("kotlinx.coroutines.android").get())
                add("implementation", libs.findLibrary("androidx.hilt.navigation.compose").get())
                add("implementation", libs.findLibrary("androidx.compose.material.icons.core").get())

                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("kotlinx.coroutines.test").get())
            }
        }
    }
}
