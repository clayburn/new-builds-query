package me.clayjohnson.newasfbuildsquery

class QueryBuilder {
    fun buildFromResource(resource: String) = this::class.java.classLoader.getResourceAsStream(resource)!!
            .bufferedReader()
            .use { it.readText() }
            .lines()
            .filter { it.isNotBlank() }
            .joinToString(separator = " ") { "-project:\"${it.trim()}\"" }
}