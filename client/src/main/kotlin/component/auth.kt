package component

import AuthUser
import Token
import kotlinext.js.jso
import kotlinx.css.*
import kotlinx.html.INPUT
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.*
import react.dom.button
import react.dom.p
import ru.altmanea.edu.server.model.Config.Companion.serverUrl
import ru.altmanea.edu.server.model.User
import styled.css
import styled.styledDiv
import styled.styledInput
import userInfo
import wrappers.axios
import kotlin.js.json

typealias Username = String
typealias Password = String

external interface AuthProps : Props {
    var user: User?
    var signIn: (Username, Password) -> Unit
    var signOff: () -> Unit
}

fun fAuth() = fc("Auth") { props: AuthProps ->
    val user = props.user
    if (user == null) {
        val userInput = useRef<INPUT>()
        val passInput = useRef<INPUT>()
        styledDiv {
            css {
                whiteSpace = WhiteSpace.preWrap
                margin(top = 10.px, bottom = 10.px)
            }
            +"Name: "
            styledInput {
                css { width = 50.px }
                ref = userInput
            }
            +" Pass: "
            styledInput {
                css { width = 50.px }
                ref = passInput
            }
            +"   "
            button {
                +"Sign in"
                attrs.onClickFunction = {
                    userInput.current?.value?.let { user ->
                        passInput.current?.value?.let { pass ->
                            props.signIn(user, pass)
                        }
                    }
                }
            }
        }
    } else {
        styledDiv {
            css {
                whiteSpace = WhiteSpace.preWrap
                margin(top = 10.px, bottom = 10.px)
            }
            +"${user.username}   "
            button {
                +"Sign off"
                attrs.onClickFunction = { props.signOff() }
            }
        }
    }
}

external interface AuthContainerProps : Props {
    var user: User?
    var signIn: (Pair<User, Token>) -> Unit
    var signOff: () -> Unit
}

interface AxiosData {
    val token: String
}

fun fAuthContainer() = fc("AuthContainer") { props: AuthContainerProps ->
    fun signInRequest(user: User) {
        axios<AxiosData>(
            jso {
                url = "$serverUrl/jwt-login"
                method = "Post"
                headers = json(
                    "Content-Type" to "application/json"
                )
                data = Json.encodeToString(user)
            }
        ).then {
            val token = it.data.token
            console.log(token)
            props.signIn(Pair(user, "Bearer $token"))
        }
    }
    child(fAuth()) {
        attrs.user = props.user
        attrs.signOff = props.signOff
        attrs.signIn = { user, pass ->
            signInRequest(User(user, pass))
        }
    }
}

fun fAuthProvider(render: Render) = fc<Props>("AuthManager") {
    val (userAndToken, setUserAndToken) = useState<AuthUser>(null)
    child(fAuthContainer()) {
        attrs.user = userAndToken?.first
        attrs.signOff = { setUserAndToken(null) }
        attrs.signIn = { setUserAndToken(it) }
    }
    if (userAndToken == null)
        p { +"Authentication is required" }
    else
        userInfo.Provider(userAndToken) {
            render()
        }
}

fun RBuilder.AuthProvider(render: Render) = child(fAuthProvider(render))