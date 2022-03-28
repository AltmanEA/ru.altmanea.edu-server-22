package ru.altmanea.edu.server.auth

// Based on https://github.com/ximedes/ktor-authorization

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import ru.altmanea.edu.server.model.Role

class AuthorizationException(override val message: String) : Exception(message)

class RoleBasedAuthorization(config: Configuration) {
    private val getRoles = config._getRoles

    class Configuration {
        internal var _getRoles: (Principal) -> Set<Role> = { emptySet() }

        fun getRoles(builder: (Principal) -> Set<Role>) {
            _getRoles = builder
        }
    }

    fun interceptPipeline(
        pipeline: ApplicationCallPipeline,
        allowedRoles: Set<Role>
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Features, Authentication.ChallengePhase)
        pipeline.insertPhaseAfter(Authentication.ChallengePhase, AuthorizationPhase)

        pipeline.intercept(AuthorizationPhase) {
            val principal = call.authentication.principal<Principal>()
            if (principal == null)
                throw AuthorizationException("No user name")
            else
                if (allowedRoles.intersect(getRoles(principal)).isEmpty())
                    throw AuthorizationException("Permission is denied")
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, RoleBasedAuthorization> {
        override val key = AttributeKey<RoleBasedAuthorization>("RoleBasedAuthorization")

        val AuthorizationPhase = PipelinePhase("Authorization")

        override fun install(
            pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit
        ): RoleBasedAuthorization {
            val configuration = Configuration().apply(configure)
            return RoleBasedAuthorization(configuration)
        }
    }

}

class AuthorizedRouteSelector(private val description: String) : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Constant

    override fun toString(): String = "(authorize ${description})"
}

fun Route.authorizedRoute(
    allowedRoles: Set<Role>,
    build: Route.() -> Unit
): Route {
    val authorizedRoute = createChild(
        AuthorizedRouteSelector("Roles: ${allowedRoles.joinToString(", ")}")
    )
    application.feature(RoleBasedAuthorization).interceptPipeline(authorizedRoute, allowedRoles)
    authorizedRoute.build()
    return authorizedRoute
}
