package com.hancenote.convention

import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

private const val NOTES_TEMPLATE_INPUT = "notesTemplateInput"
private const val NOTES_TEMPLATE_DIR = "public-repos/notes-template"

fun Project.configureNotesTemplateInput() {
    afterEvaluate {
        if (!hasProperty(NOTES_TEMPLATE_INPUT)) return@afterEvaluate
        val template = rootProject.fileTree(NOTES_TEMPLATE_DIR)
        tasks.withType<Test>().configureEach {
            inputs
                .files(template)
                .withPropertyName("notesTemplate")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }
}
