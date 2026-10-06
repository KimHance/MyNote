plugins {
    alias(libs.plugins.hancenote.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    `java-test-fixtures`
}

extra["notesInput"] = true

dependencies {
    implementation(projects.core.domain)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.javax.inject)
    implementation(libs.jetbrains.markdown)

    testFixturesImplementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
