package com.hancenote.convention

import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

private class RepoTestInput(
    val property: String,
    val name: String,
    val dir: String,
)

private val REPO_TEST_INPUTS =
    listOf(
        RepoTestInput("notesTemplateInput", "notesTemplate", "public-repos/notes-template"),
        RepoTestInput("notesInput", "notes", "notes"),
        RepoTestInput("notesInput", "frontMatterBundle", "notes-tests/fixtures/front-matter"),
    )

/** 모듈이 켠 속성마다 루트의 폴더를 그 모듈 `Test` 태스크의 입력으로 더한다. */
fun Project.configureRepoTestInputs() {
    afterEvaluate {
        val trees =
            REPO_TEST_INPUTS
                .filter { hasProperty(it.property) }
                .map { it.name to rootProject.fileTree(it.dir) }
        if (trees.isEmpty()) return@afterEvaluate
        tasks.withType<Test>().configureEach {
            trees.forEach { (name, tree) ->
                inputs
                    .files(tree)
                    .withPropertyName(name)
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
    }
}
