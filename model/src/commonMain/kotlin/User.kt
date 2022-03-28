package ru.altmanea.edu.server.model

import kotlinx.serialization.Serializable

@Serializable
class User(
    val username: String,
    val password: String
)