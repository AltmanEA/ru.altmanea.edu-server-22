package component

import kotlinext.js.jso
import kotlinx.html.INPUT
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.*
import react.query.useMutation
import react.query.useQuery
import react.query.useQueryClient
import react.router.useParams
import ru.altmanea.edu.server.model.Config
import ru.altmanea.edu.server.model.Item
import ru.altmanea.edu.server.model.Student
import wrappers.AxiosResponse
import wrappers.QueryError
import wrappers.axios
import kotlin.js.json

external interface StudentProps : Props {
    var students: Item<Student>
    var updateStudent: (String, String) -> Unit
}


fun fcStudent() = fc("Student") { props: StudentProps ->
    val firstnameRef = useRef<INPUT>()
    val surnameRef = useRef<INPUT>()

    val (firstname, setFirstname) = useState(props.students.elem.firstname)
    val (surname, setSurname) = useState(props.students.elem.surname)

    fun onInputEdit(setter: StateSetter<String>, ref: MutableRefObject<INPUT>) =
        { _: Event ->
            setter(ref.current?.value ?: "ERROR!")
        }

    span {
        p {
            +"Firstname: "
            input {
                ref = firstnameRef
                attrs.value = firstname
                attrs.onChangeFunction = onInputEdit(setFirstname, firstnameRef)
            }
        }
        p {
            +"Surname: "
            input {
                ref = surnameRef
                attrs.value = surname
                attrs.onChangeFunction = onInputEdit(setSurname, surnameRef)
            }
        }
        button {
            +"Update student"
            attrs.onClickFunction = {
                firstnameRef.current?.value?.let { firstname ->
                    surnameRef.current?.value?.let { surname ->
                        props.updateStudent(firstname, surname)
                    }
                }
            }
        }
    }
}

class MutationData(
    val oldStudent: Item<Student>,
    val newStudent: Student,
)

fun fcContainerStudent() = fc("ContainerStudent") { _: Props ->
    val studentParams = useParams()
    val queryClient = useQueryClient()

    val studentId = studentParams["id"] ?: "Route param error"

    val query = useQuery<Any, QueryError, AxiosResponse<Item<Student>>, Any>(
        studentId,
        {
            axios<Array<Student>>(jso {
                url = Config.studentsPath + studentId
            })
        }
    )

    val updateStudentMutation = useMutation<Any, Any, MutationData, Any>(
        { mutationData ->
            axios<String>(jso {
                url = "${Config.studentsURL}/${mutationData.oldStudent.uuid}"
                method = "Put"
                headers = json(
                    "Content-Type" to "application/json",
                )
                data = JSON.stringify(mutationData.newStudent)
            })
        },
        options = jso {
            onSuccess = { _: Any, _: Any, _: Any? ->
                queryClient.invalidateQueries<Any>(studentId)
            }
        }
    )

    if (query.isLoading) div { +"Loading .." }
    else if (query.isError) div { +"Error!" }
    else {
        val studentItem = query.data?.data!!
        child(fcStudent()) {
            attrs.students = studentItem
            attrs.updateStudent = { f, s ->
                updateStudentMutation.mutate(MutationData(studentItem, Student(f, s)), null)
            }
        }
    }
}
