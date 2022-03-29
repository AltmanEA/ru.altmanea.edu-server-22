package component

import kotlinext.js.jso
import kotlinx.html.INPUT
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.Props
import react.dom.*
import react.fc
import react.query.useMutation
import react.query.useQuery
import react.query.useQueryClient
import react.router.dom.Link
import react.useContext
import react.useRef
import ru.altmanea.edu.server.model.Config.Companion.studentsURL
import ru.altmanea.edu.server.model.Item
import ru.altmanea.edu.server.model.Student
import userInfo
import wrappers.QueryError
import wrappers.axios
import wrappers.fetch
import wrappers.fetchText
import kotlin.js.json

external interface StudentListProps : Props {
    var students: List<Item<Student>>
    var addStudent: (String, String) -> Unit
    var deleteStudent: (Int) -> Unit
}

fun fcStudentList() = fc("StudentList") { props: StudentListProps ->

    val firstnameRef = useRef<INPUT>()
    val surnameRef = useRef<INPUT>()

    span {
        p {
            +"Firstname: "
            input {
                ref = firstnameRef
            }
        }
        p {
            +"Surname: "
            input {
                ref = surnameRef
            }
        }
        button {
            +"Add student"
            attrs.onClickFunction = {
                firstnameRef.current?.value?.let { firstname ->
                    surnameRef.current?.value?.let { surname ->
                        props.addStudent(firstname, surname)
                    }
                }
            }
        }
    }

    h3 { +"Students" }
    ol {
        props.students.mapIndexed { index, studentItem ->
            li {
                Link {
                    attrs.to = "/students/${studentItem.uuid}"
                    +"${studentItem.elem.fullname} \t"
                }
                button {
                    +"X"
                    attrs.onClickFunction = {
                        props.deleteStudent(index)
                    }
                }
            }
        }
    }
}

@Serializable
class ClientItemStudent(
    override val elem: Student,
    override val uuid: String,
    override val etag: Long
) : Item<Student>

fun fcContainerStudentList() = fc("QueryStudentList") { _: Props ->
    val queryClient = useQueryClient()
    val token = useContext(userInfo)?.second
    val authHeader = "Authorization" to token

    val query = useQuery<String, QueryError, String, String>(
        "studentList",
        {
            fetchText(
                studentsURL,
                jso { headers = json(authHeader)}
            )
        }
    )

    val addStudentMutation = useMutation<Any, Any, Any, Any>(
        { student: Student ->
            axios<String>(jso {
                url = studentsURL
                method = "Post"
                headers = json(
                    "Content-Type" to "application/json",
                    authHeader
                )
                data = Json.encodeToString(student)
            })
        },
        options = jso {
            onSuccess = { _: Any, _: Any, _: Any? ->
                queryClient.invalidateQueries<Any>("studentList")
            }
        }
    )

    val deleteStudentMutation = useMutation<Any, Any, Any, Any>(
        { studentItem: Item<Student> ->
            axios<String>(jso {
                url = "$studentsURL/${studentItem.uuid}"
                method = "Delete"
                headers = json(authHeader)
            })
        },
        options = jso {
            onSuccess = { _: Any, _: Any, _: Any? ->
                queryClient.invalidateQueries<Any>("studentList")
            }
        }
    )

    if (query.isLoading) div { +"Loading .." }
    else if (query.isError) div { +"Error!" }
    else {
        val items: List<ClientItemStudent> =
            Json.decodeFromString(query.data ?: "")
        child(fcStudentList()) {
            attrs.students = items
            attrs.addStudent = { f, s ->
                addStudentMutation.mutate(Student(f, s), null)
            }
            attrs.deleteStudent = {
                deleteStudentMutation.mutate(items[it], null)
            }
        }
    }
}
