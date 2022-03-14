package ru.altmanea.edu.server.model

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val name: String,
    val students: Set<String> = emptySet(),
    val marks: Map<String, Int> = emptyMap()
)