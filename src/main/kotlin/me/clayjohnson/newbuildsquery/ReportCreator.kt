package me.clayjohnson.newbuildsquery

import java.io.File
import java.time.Instant

class ReportCreator(
    private val develocityUrl: String,
    private val days: List<Int>,
    private val now: Instant,
) {
    fun createReport(builds: List<BuildInfo>, query: String, reportFile: String) {
        val text = days.joinToString(separator = "\n") {
            generateTableForDaysAgo(builds, it, query)
        }

        val report = File(reportFile)
        report.delete()
        report.parentFile.mkdirs()
        report.writeText(text)
    }

    private fun generateTableForDaysAgo(builds: List<BuildInfo>, daysAgo: Int, query: String): String {
        val buildCounts = getBuildsFromLastDays(builds, daysAgo)
                .groupingBy { it.projectName }
                .eachCount()
                .toList()
                .toMap()
        return generateMarkdown(buildCounts, daysAgo, query)
    }

    private fun createBaseDevelocityUrl(daysAgo: Int): String {
        val startTimeMin = now.daysAgo(daysAgo).toEpochMilli()
        val startTimeMax = now.toEpochMilli()

        return "$develocityUrl/scans?search.startTimeMin=$startTimeMin&search.startTimeMax=$startTimeMax"
    }

    private fun createDevelocityUrlWithQuery(daysAgo: Int, query: String): String {
        return "${createBaseDevelocityUrl(daysAgo)}&search.query=${query.urlEncode()}"
    }

    private fun createDevelocityUrlForProject(daysAgo: Int, projectName: String) : String {
        return "${createBaseDevelocityUrl(daysAgo)}&search.query=project:\"${projectName.urlEncode()}\""
    }

    private fun getBuildsFromLastDays(builds: List<BuildInfo>, days: Int): List<BuildInfo> {
        return builds.filter { it.buildStarted.isAfter(now.daysAgo(days)) }
    }

    private fun generateMarkdown(buildCounts: Map<String, Int>, daysAgo: Int, query: String): String {
        val headerRow = "# [Builds from last ${if (daysAgo == 1) "24 hours" else "$daysAgo days"}](${createDevelocityUrlWithQuery(daysAgo, query)})"

        val projectRows = buildCounts.map { (key, value) ->
            val newKey = if (key == BuildInfo.MISSING_PROJECT_NAME) {
                key
            } else {
                "[$key](${createDevelocityUrlForProject(daysAgo, key)}})"
            }

            newKey to value
        }.sortedBy { (key, _) -> key }.toMap().map {
            "| ${it.key} | ${it.value} |"
        }.joinToString(separator = "\n")

        return generateMarkdown(headerRow, projectRows)
    }

    private fun generateMarkdown(headerRow: String, projectRows: String): String {
        return """
$headerRow

| Project Name | Build Count |
| ------------ | ----------- |
$projectRows

""".trimIndent()
    }
}