package me.clayjohnson.newbuildsquery

import com.gradle.develocity.api.model.Build
import java.time.Instant

data class BuildInfo(
    val projectName: String,
    val buildStarted: Instant,
) {
    constructor(build: Build) : this(
        projectName = build.projectName(),
        buildStarted = build.buildStarted(),
    )

    companion object {
        const val MISSING_PROJECT_NAME = "(N/A)"
    }
}

fun Build.projectName(): String = this.models?.let {
    it.gradleAttributes?.model?.rootProjectName ?: it.mavenAttributes?.model?.topLevelProjectName
} ?: BuildInfo.MISSING_PROJECT_NAME

fun Build.buildStarted(): Instant = Instant.ofEpochMilli(this.models?.run {
    gradleAttributes?.model?.buildStartTime
        ?: mavenAttributes?.model?.buildStartTime
        ?: throw IllegalArgumentException("Build start time is missing.")
}!!)