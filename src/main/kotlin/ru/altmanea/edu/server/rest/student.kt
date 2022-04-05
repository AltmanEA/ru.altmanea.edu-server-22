package ru.altmanea.edu.server.rest

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import roleAdmin
import roleUser
import ru.altmanea.edu.server.auth.authorizedRoute
import ru.altmanea.edu.server.model.Config.Companion.studentsPath
import ru.altmanea.edu.server.model.Student
import ru.altmanea.edu.server.repo.studentsRepo

fun Route.student() =
    route(studentsPath) {
        authenticate("auth-jwt") {
            authorizedRoute(setOf(roleAdmin, roleUser)) {
                get {
                    if (!studentsRepo.isEmpty()) {
                        call.respond(studentsRepo.findAll())
                    } else {
                        call.respondText("No students found", status = HttpStatusCode.NotFound)
                    }
                }
                get("{id}") {
                    val id = call.parameters["id"] ?: return@get call.respondText(
                        "Missing or malformed id",
                        status = HttpStatusCode.BadRequest
                    )
                    val studentItem =
                        studentsRepo[id] ?: return@get call.respondText(
                            "No student with id $id",
                            status = HttpStatusCode.NotFound
                        )
                    call.respond(studentItem)
                }
                post("byFirstname") {
                    val firstname = try {
                        call.receive<Student>().firstname
                    } catch (e: Throwable) {
                        return@post call.respondText(
                            "Request body is not student", status = HttpStatusCode.BadRequest
                        )
                    }
                    call.respond(studentsRepo.findAll().filter { it.elem.firstname == firstname })
                }
                post("byUUIDs") {
                    val uuids = try {
                        call.receive<List<String>>()
                    } catch (e: Throwable) {
                        return@post call.respondText(
                            "Request body is not list uuid", status = HttpStatusCode.BadRequest
                        )
                    }
                    call.respond(uuids.map { studentsRepo[it] })
                }
            }

            authorizedRoute(setOf(roleAdmin)) {
                post {
                    val student = call.receive<Student>()
                    studentsRepo.create(student)
                    call.respondText("Student stored correctly", status = HttpStatusCode.Created)
                }
                delete("{id}") {
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    if (studentsRepo.delete(id)) {
                        call.respondText("Student removed correctly", status = HttpStatusCode.Accepted)
                    } else {
                        call.respondText("Not Found", status = HttpStatusCode.NotFound)
                    }
                }
                put("{id}") {
                    val id = call.parameters["id"] ?: return@put call.respondText(
                        "Missing or malformed id",
                        status = HttpStatusCode.BadRequest
                    )
                    studentsRepo[id] ?: return@put call.respondText(
                        "No student with id $id",
                        status = HttpStatusCode.NotFound
                    )
                    val newStudent = call.receive<Student>()
                    studentsRepo.update(id, newStudent)
                    call.respondText("Student updates correctly", status = HttpStatusCode.Created)
                }
            }
        }
    }
