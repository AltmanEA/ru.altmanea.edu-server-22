package component

import kotlinext.js.jso
import kotlinx.html.INPUT
import kotlinx.html.SELECT
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.*
import react.dom.*
import react.query.useMutation
import react.query.useQuery
import react.query.useQueryClient
import react.router.useParams
import ru.altmanea.edu.server.model.Config
import ru.altmanea.edu.server.model.Item
import ru.altmanea.edu.server.model.Lesson
import ru.altmanea.edu.server.model.Student
import userInfo
import wrappers.QueryError
import wrappers.axios
import wrappers.fetchText
import kotlin.js.json

external interface LessonProps : Props {
    var lesson: Item<Lesson>
    var allStudents: List<Item<Student>>
    var studentUUIDs: List<String>
    var updateLessonName: (String) -> Unit
    var addStudent: (String) -> Unit
}

interface MySelect {
    val value: String
}

fun fcLesson() = fc("Lesson") { props: LessonProps ->
    val newNameRef = useRef<INPUT>()
    val selectRef = useRef<SELECT>()

    h3 { +props.lesson.elem.name }

    div {
        input {
            ref = newNameRef
        }
        button {
            +"Update lesson name"
            attrs.onClickFunction = {
                newNameRef.current?.value?.let {
                    props.updateLessonName(it)
                }
            }
        }
    }
    div {
        select {
            ref = selectRef
            props.allStudents.map {
                val student = Student(it.elem.firstname, it.elem.surname)
                option {
                    +student.fullname
                    attrs.value = it.uuid
                }
            }
        }
        button {
            +"Add student"
            attrs.onClickFunction = {
                val select = selectRef.current.unsafeCast<MySelect>()
                val uuid = select.value
                props.addStudent(uuid)
            }
        }
    }

    child(fcContainerLessonStudents()) {
        attrs.studentUUIDS = props.studentUUIDs
    }
}

fun fcContainerLesson() = fc("ContainerLesson") { _: Props ->
    val lessonParams = useParams()
    val queryClient = useQueryClient()
    val token = useContext(userInfo)?.second
    val authHeader = "Authorization" to token

    val lessonId = lessonParams["id"] ?: "Route param error"

    val queryLesson = useQuery<String, QueryError, String, String>(
        lessonId,
        {
            fetchText(
                Config.lessonsPath + lessonId,
                jso { headers = json(authHeader) }
            )
        },
        options = jso {
            refetchOnWindowFocus = false
        }
    )

    val queryStudents = useQuery<String, QueryError, String, String>(
        "studentList",
        {
            fetchText(
                Config.studentsURL,
                jso { headers = json(authHeader) }
            )
        }
    )

    val updateLessonNameMutation = useMutation<Any, Any, Pair<String, Long>, Any>(
        { (name, etag) ->
            axios<String>(jso {
                url = "${Config.lessonsURL}/$lessonId/name"
                method = "Put"
                headers = json(
                    "Content-Type" to "application/json",
                    "etag" to etag,
                    authHeader
                )
                data = Json.encodeToString(Lesson(name))
            })
        },
        options = jso {
            onSuccess = { _: Any, _: Any, _: Any? ->
                queryClient.invalidateQueries<Any>(lessonId)
            }
        }
    )

    val addStudentMutation = useMutation<Any, Any, String, Any>(
        { studentId ->
            axios<String>(jso {
                url = "${Config.lessonsURL}/$lessonId/students/$studentId"
                method = "Post"
                headers = json(
                    "Content-Type" to "application/json",
                    authHeader
                )
            })
        },
        options = jso {
            onSuccess = { _: Any, _: Any, _: Any? ->
                queryClient.invalidateQueries<Any>(lessonId)
            }
        }
    )

    if (queryLesson.isLoading or queryStudents.isLoading) div { +"Loading .." }
    else if (queryLesson.isError or queryStudents.isError) div { +"Error!" }
    else {
        val lessonItem: ClientItemLesson =
            Json.decodeFromString(queryLesson.data ?: "")
        val studentsUUIDs = lessonItem.elem.students.map {
            it.substringAfterLast("/")
        }
        val studentItems: List<ClientItemStudent> =
            Json.decodeFromString<List<ClientItemStudent>>(queryStudents.data ?: "")
                .filter { it.uuid !in studentsUUIDs }
        child(fcLesson()) {
            attrs.lesson = lessonItem
            attrs.allStudents = studentItems
            attrs.studentUUIDs = studentsUUIDs
            attrs.updateLessonName = {
                updateLessonNameMutation.mutate(it to lessonItem.etag, null)
            }
            attrs.addStudent = {
                addStudentMutation.mutate(it, null)
            }
        }
    }
}



