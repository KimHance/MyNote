package com.hance.convention

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCircuit() {
    with(pluginManager) {
        apply("com.google.devtools.ksp")
        apply("kotlin-parcelize")
    }
    extensions.configure<KspExtension> {
        arg("circuit.codegen.mode", "hilt")
    }
    dependencies {
        add("ksp", libs.findLibrary("circuit.codegen").get())
    }
}