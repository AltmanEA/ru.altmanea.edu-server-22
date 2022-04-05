package ru.altmanea.edu.server.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import ru.altmanea.edu.server.main
import ru.altmanea.edu.server.model.Config
import ru.altmanea.edu.server.model.Lesson
import ru.altmanea.edu.server.model.Student
import ru.altmanea.edu.server.repo.RepoItem
import java.util.UUID
import kotlin.test.assertEquals

internal class StudentsKtTest {
    @Test
    fun testStudentRoute() {
        withTestApplication(Application::main) {
            val tokenAdmin = handleRequest(HttpMethod.Post, "/jwt-login") {
                setBodyAndHeaders("""{ "username": "admin", "password": "admin" }""")
            }.run {
                "Bearer ${decodeBody<Token>().token}"
            }
            val tokenTutor = handleRequest(HttpMethod.Post, "/jwt-login") {
                setBodyAndHeaders("""{ "username": "tutor", "password": "tutor" }""")
            }.run {
                "Bearer ${decodeBody<Token>().token}"
            }

            val studentItems = handleRequest(HttpMethod.Get, Config.studentsPath) {
                addHeader("Authorization", tokenTutor)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<List<RepoItem<Student>>>()
            }
            assertEquals(4, studentItems.size)
            val sheldon = studentItems.find { it.elem.firstname == "Sheldon" }
            check(sheldon != null)

            handleRequest(HttpMethod.Get, Config.studentsPath + sheldon.uuid){
                addHeader("Authorization", tokenTutor)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Sheldon", decodeBody<RepoItem<Student>>().elem.firstname)
            }
            handleRequest(HttpMethod.Get, Config.studentsPath + UUID.randomUUID().toString()){
                addHeader("Authorization", tokenTutor)
            }.run {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            handleRequest(HttpMethod.Post, Config.studentsPath) {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Student("Raj", "Koothrappali")
                    )
                )
                addHeader("Authorization", tokenTutor)
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
            handleRequest(HttpMethod.Post, Config.studentsPath) {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Student("Raj", "Koothrappali")
                    )
                )
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            val studentItemsWithRaj = handleRequest(HttpMethod.Get, Config.studentsPath){
                addHeader("Authorization", tokenTutor)
            }.run {
                decodeBody<List<RepoItem<Student>>>()
            }
            assertEquals(5, studentItemsWithRaj.size)
            val raj = studentItemsWithRaj.find { it.elem.firstname == "Raj" }
            check(raj != null)
            assertEquals("Koothrappali", raj.elem.surname)

            handleRequest(HttpMethod.Delete, Config.studentsPath + raj.uuid){
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }
            handleRequest(HttpMethod.Delete, Config.studentsPath + raj.uuid){
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            val penny = studentItems.find { it.elem.firstname == "Penny" }
            check(penny != null)
            handleRequest(HttpMethod.Put, Config.studentsPath + penny.uuid) {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Student("Penny", "Waitress")
                    )
                )
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // byFirstName
            val howard = handleRequest(HttpMethod.Post, Config.studentsPath + "/byFirstname") {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Student("Howard", "")
                    )
                )
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<List<RepoItem<Student>>>()
            }
            assertEquals(1, howard.size)
            handleRequest(HttpMethod.Post, Config.studentsPath + "/byFirstname") {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Lesson("Howard")
                    )
                )
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
            handleRequest(HttpMethod.Post, Config.studentsPath) {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Student("Howard", "Duck")
                    )
                )
                addHeader("Authorization", tokenAdmin)
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            val howards = handleRequest(HttpMethod.Post, Config.studentsPath + "/byFirstname") {
                setBodyAndHeaders(
                    Json.encodeToString(
                        Student("Howard", "")
                    )
                )
                addHeader("Authorization", tokenAdmin)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<List<RepoItem<Student>>>()
            }
            assertEquals(2, howards.size)

            // byUUIDs
            val students = handleRequest(HttpMethod.Post, Config.studentsPath + "/byUUIDs") {
                setBodyAndHeaders(
                    Json.encodeToString(
                        studentItems
                            .slice(0..2)
                            .map { it.uuid }
                    )
                )
                addHeader("Authorization", tokenTutor)
            }.run {
                assertEquals(HttpStatusCode.OK, response.status())
                decodeBody<List<RepoItem<Student>>>()
            }
            assertEquals(3, students.size)
        }
    }
}