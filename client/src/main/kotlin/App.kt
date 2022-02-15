import kotlinx.browser.document
import react.dom.h1
import react.dom.render
import ru.altmanea.edu.server.model.Student

val sheldon = Student("Sheldon", "Cooper")

fun main() {
    render(document.getElementById("root")!!){
        h1 { +"Hello, ${sheldon.fullname}" }
    }
}

