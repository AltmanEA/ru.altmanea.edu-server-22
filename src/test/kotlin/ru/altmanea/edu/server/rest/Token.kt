package ru.altmanea.edu.server.rest

import kotlinx.serialization.Serializable

@Serializable
internal class Token(
    val token: String
)