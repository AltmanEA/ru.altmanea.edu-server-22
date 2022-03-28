package ru.altmanea.edu.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import ru.altmanea.edu.server.model.User
import userList
import userRoles
import java.util.*

val secret = "secret"
val jwtRealm = "JWT auth"

fun Application.auth(){
    install(RoleBasedAuthorization) {
        getRoles {principal ->
            val username = (principal as JWTPrincipal)
                .payload.getClaim("username").asString()
            val user = userList.find { it.username== username }
            userRoles[user]?: emptySet()
        }
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
    install(StatusPages){
        exception<AuthorizationException>{
            call.respond(HttpStatusCode.Forbidden, it.message)
        }
    }
    routing {
        post("jwt-login") {
            val user = call.receive<User>()
            val token = JWT.create()
                .withClaim("username", user.username)
                .withExpiresAt(Date(System.currentTimeMillis() + 600000))
                .sign(Algorithm.HMAC256(secret))
            call.respond(hashMapOf("token" to token))
        }
    }
}