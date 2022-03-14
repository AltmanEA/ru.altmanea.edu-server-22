package component

import kotlinext.js.jso
import kotlinx.css.input
import kotlinx.html.INPUT
import kotlinx.html.SELECT
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.Props
import react.dom.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.input
import react.fc
import react.query.useMutation
import react.query.useQuery
import react.query.useQueryClient
import react.router.useParams
import react.useRef
import ru.altmanea.edu.server.model.Config
import ru.altmanea.edu.server.model.Item
import ru.altmanea.edu.server.model.Lesson
import ru.altmanea.edu.server.model.Student
import wrappers.AxiosResponse
import wrappers.QueryError
import wrappers.axios
import kotlin.js.json

external interface LessonProps : Props {
    var lesson: Item<Lesson>
    var students: List<Item<Student>>
    var updateLessonName: (String) -> Unit
    var addStudent: (String) -> Unit
    var markStudent: (String, Int) -> Unit
}

interface MySelect{
    val value: String
}

fun fcLesson() = fc("Lesson") { props: LessonProps ->
    val newNameRef = useRef<INPUT>()
    val selectRef = useRef<SELECT>()

    h3 { +props.lesson.elem.name }

    div {
        input{
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
            props.students.map {
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
                console.log(uuid)
            }
        }
    }

    val students = props.lesson.elem.students
    console.log(students)

//    if(students.isNotEmpty()){
//        h4 { +"Students" }
//        ol {
//            students.map {
//                li { +it }
//            }
//        }
//    }
}

fun fcContainerLesson() = fc("ContainerLesson") { _: Props ->
    val lessonParams = useParams()
    val queryClient = useQueryClient()

    val lessonId = lessonParams["id"] ?: "Route param error"

    val queryLesson = useQuery<Any, QueryError, AxiosResponse<Item<Lesson>>, Any>(
        lessonId,
        {
            axios<Item<Lesson>>(jso {
                url = Config.lessonsPath + lessonId
            })
        }
    )

    val queryStudents = useQuery<Any, QueryError, AxiosResponse<Array<Item<Student>>>, Any>(
        "studentList",
        {
            axios<Array<Student>>(jso {
                url = Config.studentsURL
            })
        }
    )

    val updateLessonNameMutation = useMutation<Any, Any, String, Any>(
        { name ->
            axios<String>(jso {
                url = "${Config.lessonsURL}/$lessonId/name"
                method = "Put"
                headers = json(
                    "Content-Type" to "application/json",
                )
                data = Json.encodeToString(Lesson(name))
            })
        },
        options = jso {
            onSuccess = { _: Any, _: Any, _: Any? ->
                queryClient.invalidateQueries<Any>(lessonId)
                queryClient.invalidateQueries<Any>("lessonList")
            }
        }
    )

    if (queryLesson.isLoading or queryStudents.isLoading) div { +"Loading .." }
    else if (queryLesson.isError or queryStudents.isError) div { +"Error!" }
    else {
        val lessonItem = queryLesson.data?.data!!
        val studentItems = queryStudents.data?.data?.toList() ?: emptyList()
        child(fcLesson()) {
            attrs.lesson = lessonItem
            attrs.students = studentItems
            attrs.updateLessonName = {
                updateLessonNameMutation.mutate(it, null)
            }
        }
    }
}

