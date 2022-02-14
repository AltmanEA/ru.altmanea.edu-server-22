package ru.altmanea.edu.server.model

import kotlinx.serialization.*

@Serializable
class Student(
    val firstname: String,
    val surname: String
)