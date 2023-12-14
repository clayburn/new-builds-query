package me.clayjohnson

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.gradle.develocity.api.BuildsApi
import com.gradle.develocity.api.client.ApiClient
import com.gradle.develocity.api.model.Build
import com.gradle.develocity.api.model.BuildModelName
import com.gradle.develocity.api.model.BuildsQuery
import java.io.File
import java.net.URLEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer

class App : CliktCommand() {
    private val apiKey by option(envvar = "API_KEY").required()
    private val develocityUrl by option().required()
    private val days by option().int().default(28)
    private val reportFile by option().default("report.md")

    override fun run() {
        val buildsApi = BuildsApi().apply {
            apiClient = ApiClient().apply {
                basePath = develocityUrl
                setBearerToken(apiKey)
            }
        }

        val query = this::class.java.classLoader.getResourceAsStream("known-projects.txt")!!
            .bufferedReader()
            .use { it.readText() }
            .lines()
            .filter { it.isNotBlank() }
            .joinToString(separator = " ") { "-project:\"${it.trim()}\"" }
        val knownProjectFilter = Consumer { buildsQuery: BuildsQuery ->
            buildsQuery.query = query
        }

        val builds = BuildsProcessor(
            buildsApi =  buildsApi,
            buildsQueryCustomizer = knownProjectFilter,
        ).process(Instant.now().daysAgo(days)).map { BuildInfo(it) }

        createReport(builds, query)
    }

    private fun createReport(builds: List<BuildInfo>, query: String) {
        val report = File(reportFile)
        report.delete()

        report.writeText(listOf(1, 7, 28).joinToString(separator = "\n") { createReport(builds, it, query) })
    }

    private fun createReport(builds: List<BuildInfo>, days: Int, query: String) : String {
        val now = Instant.now()
        val baseUrl = "$develocityUrl/scans?search.startTimeMin=${now.daysAgo(days).toEpochMilli()}&search.startTimeMax=${now.toEpochMilli()}"
        val baseUrlWithQuery = "$baseUrl&search.query=${query.urlEncode()}"
        val buildCounts = getBuildsFromLastDays(builds, days)
            .groupingBy { it.projectName }
            .eachCount()
            .toList()
            .sortedByDescending { (_, value) -> value }
            .toMap()
            .map { "| [${it.key}]($baseUrl&search.query=project:\"${it.key.urlEncode()}\") | ${it.value} |" }
            .joinToString(separator = "\n")

        return """
# [Builds from last ${ if(days == 1)  "24 hours" else "$days days" }]($baseUrlWithQuery)
            
| Project Name | Build Count |
| ------------ | ----------- |
$buildCounts

        """.trimIndent()
    }

    private fun getBuildsFromLastDays(builds: List<BuildInfo>, days: Int) : List<BuildInfo> {
        val instant = Instant.now().daysAgo(days)
        return builds.filter { it.buildStarted.isAfter(instant) }
    }

}

data class BuildInfo(
    val projectName: String,
    val buildStarted: Instant,
) {
    constructor(apiBuild: Build) : this(
        projectName = (apiBuild.models?.gradleAttributes?.model?.rootProjectName ?: apiBuild.models?.mavenAttributes?.model?.topLevelProjectName)!!,
        buildStarted = Instant.ofEpochMilli((apiBuild.models?.gradleAttributes?.model?.buildStartTime ?: apiBuild.models?.mavenAttributes?.model?.buildStartTime)!!),
    )
}

class BuildsProcessor(
    private val buildsApi: BuildsApi,
    private val buildsQueryCustomizer: Consumer<BuildsQuery>,
) {
    fun process(fromInstant: Instant) : List<Build> {
        var fromApplicator = Consumer {
            buildsQuery: BuildsQuery -> buildsQuery.fromInstant(fromInstant.toEpochMilli())
        }

        val builds = mutableListOf<Build>()

        while (true) {
            val query = BuildsQuery().apply {
                reverse = false
                maxWaitSecs = 20
                models = mutableListOf(BuildModelName.GRADLE_ATTRIBUTES, BuildModelName.MAVEN_ATTRIBUTES)
            }
            fromApplicator.accept(query)
            buildsQueryCustomizer.accept(query)

            val result = buildsApi.getBuilds(query)
            builds.addAll(result)
            if (result.isNotEmpty()) {
                fromApplicator = Consumer {
                    buildsQuery: BuildsQuery -> buildsQuery.fromBuild(result.last().id)
                }
            } else {
                break
            }
        }

        return builds.toList()
    }
}

fun main(args: Array<String>) = App().main(args)

fun String.urlEncode(): String = URLEncoder.encode(this, "utf-8")

fun Instant.daysAgo(days: Int): Instant = this.minus(days.toLong(), ChronoUnit.DAYS)