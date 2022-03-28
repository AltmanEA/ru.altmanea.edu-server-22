package ru.altmanea.edu.server.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.netty.handler.codec.http.HttpHeaders.addHeader
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import ru.altmanea.edu.server.main
import ru.altmanea.edu.server.model.Config
import ru.altmanea.edu.server.model.Lesson
import ru.altmanea.edu.server.model.Student
import ru.altmanea.edu.server.repo.RepoItem
import kotlin.test.assertEquals

internal class LessonKtTest {

    @Test
    fun testLessonRoute() {
        withTestApplication(Application::main) {
            val tokenAdmin = handleRequest(HttpMethod.Post, "/jwt-login") {
                setBodyAndHeaders("""{ "username": "admin", "password": "admin" }""")
            }.run {
                "Bearer ${decodeBody<Token>().token}"
            }

            val lessonItems = handleRequest(HttpMethod.Get, Config.lessonsPath) {
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<List<RepoItem<Lesson>>>()
            }
            assertEquals(3, lessonItems.size)
            val math = lessonItems.find { it.elem.name == "Math" }
            check(math != null)

            handleRequest(HttpMethod.Get, Config.lessonsPath + math.uuid) {
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Math", decodeBody<RepoItem<Lesson>>().elem.name)
            }
            handleRequest(HttpMethod.Get, Config.lessonsPath + "Jack") {
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            handleRequest(HttpMethod.Post, Config.lessonsPath) {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Lesson("Chem")
                    )
                )
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            val lessonItemsWithChem = handleRequest(HttpMethod.Get, Config.lessonsPath) {
                addHeader("Authorization", tokenAdmin)
            }.run {
                decodeBody<List<RepoItem<Lesson>>>()
            }
            assertEquals(4, lessonItemsWithChem.size)
            val chem = lessonItemsWithChem.find { it.elem.name == "Chem" }
            check(chem != null)

            handleRequest(HttpMethod.Put, Config.lessonsPath + chem.uuid + "/name") {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Lesson("Chemistry")
                    )
                )
                addHeader("Authorization", tokenAdmin)
                addHeader("etag", chem.etag.toString())
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            handleRequest(HttpMethod.Delete, Config.lessonsPath + chem.uuid) {
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }
            handleRequest(HttpMethod.Delete, Config.lessonsPath + chem.uuid) {
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun testLessonStudentRoute() {
        withTestApplication(Application::main) {
            val tokenAdmin = handleRequest(HttpMethod.Post, "/jwt-login") {
                setBodyAndHeaders("""{ "username": "admin", "password": "admin" }""")
            }.run {
                "Bearer ${decodeBody<Token>().token}"
            }

            val lessonItems = handleRequest(HttpMethod.Get, Config.lessonsPath){
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<List<RepoItem<Lesson>>>()
            }
            assertEquals(3, lessonItems.size)
            val math = lessonItems.find { it.elem.name == "Math" }
            check(math != null)
            val studentItems = handleRequest(HttpMethod.Get, Config.studentsPath){
                addHeader("Authorization", tokenAdmin)
            }.run {
                decodeBody<List<RepoItem<Student>>>()
            }
            val sheldon = studentItems.find { it.elem.firstname == "Sheldon" }
            check(sheldon != null)
            val lsPath = Config.lessonsPath + math.uuid + "/students/" + sheldon.uuid


            val mathWithSheldon = handleRequest(HttpMethod.Post, lsPath){
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<RepoItem<Lesson>>()
            }
            assertEquals(1, mathWithSheldon.elem.students.size)
            assertEquals(0, mathWithSheldon.elem.marks.size)

            val mathWithSheldonMark = handleRequest(HttpMethod.Post, "$lsPath/marks") {
                setBodyAndHeaders(
                    Json.encodeToString(5)
                )
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<RepoItem<Lesson>>()
            }
            assertEquals(1, mathWithSheldonMark.elem.students.size)
            assertEquals(1, mathWithSheldonMark.elem.marks.size)
        }
    }
}
