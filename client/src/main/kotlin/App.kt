import component.fcContainerStudent
import component.fcContainerStudentList
import kotlinx.browser.document
import react.createElement
import react.dom.render
import react.query.QueryClient
import react.query.QueryClientProvider
import react.router.Route
import react.router.Routes
import react.router.dom.HashRouter
import react.router.dom.Link
import wrappers.cReactQueryDevtools

val queryClient = QueryClient()

fun main() {
    render(document.getElementById("root")!!) {
        HashRouter {
            QueryClientProvider {
                attrs.client = queryClient
                Link {
                    attrs.to = "/"
                    +"Students"
                }
                Routes {
                    Route {
                        attrs.index = true
                        attrs.element =
                            createElement(fcContainerStudentList())
                    }
                    Route {
                        attrs.path = "/student/:id"
                        attrs.element =
                            createElement(fcContainerStudent())
                    }
                }
                child(cReactQueryDevtools()) {}
            }
        }
    }
}

