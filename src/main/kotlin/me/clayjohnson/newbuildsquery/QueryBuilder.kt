package me.clayjohnson.newbuildsquery

import java.nio.file.Files
import java.nio.file.Path

class QueryBuilder {
    fun buildFromFile(knownProjects: Path) = Files.readAllLines(knownProjects)
            .filter { it.isNotBlank() }
            .joinToString(separator = " ") { "-project:\"${it.trim()}\"" }
}