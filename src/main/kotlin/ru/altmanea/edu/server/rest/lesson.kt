package ru.altmanea.edu.server.rest

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import roleAdmin
import roleUser
import ru.altmanea.edu.server.auth.authorizedRoute
import ru.altmanea.edu.server.model.Config.Companion.lessonsPath
import ru.altmanea.edu.server.model.Lesson
import ru.altmanea.edu.server.repo.RepoItem
import ru.altmanea.edu.server.repo.lessonsRepo
import ru.altmanea.edu.server.repo.studentsRepo
import ru.altmanea.edu.server.repo.urlByUUID

fun Route.lesson() {
    route(lessonsPath) {
        authenticate("auth-jwt") {
            authorizedRoute(setOf(roleAdmin, roleUser)) {
                get {
                    if (!lessonsRepo.isEmpty()) {
                        call.respond(lessonsRepo.findAll())
                    } else {
                        call.respondText("No lessons found", status = HttpStatusCode.NotFound)
                    }
                }
                get("{id}") {
                    val id = call.parameters["id"] ?: return@get call.respondText(
                        "Missing or malformed id",
                        status = HttpStatusCode.BadRequest
                    )
                    val lessonItem =
                        lessonsRepo[id] ?: return@get call.respondText(
                            "No lesson with name $id",
                            status = HttpStatusCode.NotFound
                        )
                    call.respond(lessonItem)
                }
            }
            authorizedRoute(setOf(roleAdmin)) {
                post {
                    val lesson = call.receive<Lesson>()
                    lessonsRepo.create(lesson)
                    call.respondText("Lesson stored correctly", status = HttpStatusCode.Created)
                }
                delete("{id}") {
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    if (lessonsRepo.delete(id)) {
                        call.respondText("Lesson removed correctly", status = HttpStatusCode.Accepted)
                    } else {
                        call.respondText("Not Found", status = HttpStatusCode.NotFound)
                    }
                }
                put("{id}/name") {
                    val id = call.parameters["id"] ?: return@put call.respondText(
                        "Missing or malformed id",
                        status = HttpStatusCode.BadRequest
                    )
                    val lessonItem = lessonsRepo[id] ?: return@put call.respondText(
                        "No lesson with id $id",
                        status = HttpStatusCode.NotFound
                    )
                    val clientEtag = call.request.headers["etag"]?.toLong()
                    call.application.log.info("Update ${lessonItem.uuid} lesson name. Server etag is ${lessonItem.etag}, client etag is $clientEtag")
                    if (lessonItem.etag != clientEtag) {
                        call.respondText(
                            "Element had updated on server",
                            status = HttpStatusCode.BadRequest
                        )
                    } else {
                        val newName = call.receive<Lesson>().name
                        val newLesson = lessonItem.elem.copy(name = newName)
                        lessonsRepo.update(lessonItem.uuid, newLesson)
                        call.respondText("Lesson name updates correctly", status = HttpStatusCode.Created)
                    }
                }
            }
        }
    }
    route("$lessonsPath{lessonId}/students/{studentId}") {
        authenticate("auth-jwt") {
            authorizedRoute(setOf(roleAdmin)) {
                post {
                    when (val lsResult = lsParameters()) {
                        is LSOk -> {
                            val oldElem = lsResult.lessonItem.elem
                            val newElem = oldElem.copy(students = oldElem.students + lsResult.studentLink)
                            lessonsRepo.update(lsResult.lessonItem.uuid, newElem)
                            call.respond(lessonsRepo[lsResult.lessonItem.uuid]!!)
                        }
                        is LSFail -> call.respondText(lsResult.text, status = lsResult.code)
                    }
                }
                delete {
                    when (val lsResult = lsParameters()) {
                        is LSOk -> {
                            val oldElem = lsResult.lessonItem.elem
                            val newElem = oldElem.copy(students = oldElem.students - lsResult.studentLink)
                            lessonsRepo.update(lsResult.lessonItem.uuid, newElem)
                            call.respond(lessonsRepo[lsResult.lessonItem.uuid]!!)
                        }
                        is LSFail -> call.respondText(lsResult.text, status = lsResult.code)
                    }
                }
                post("marks") {
                    when (val lsResult = lsParameters()) {
                        is LSOk -> {
                            if (lsResult.studentLink !in lsResult.lessonItem.elem.students)
                                return@post call.respondText(
                                    "No student ${lsResult.studentLink} in lesson ${lsResult.lessonItem.elem.name}",
                                    status = HttpStatusCode.NotFound
                                )
                            val mark = call.receive<String>().toIntOrNull()
                                ?: return@post call.respondText(
                                    "Mark is wrong",
                                    status = HttpStatusCode.BadRequest
                                )
                            val oldElem = lsResult.lessonItem.elem
                            val newElem = oldElem.copy(
                                marks = oldElem.marks + (lsResult.studentLink to mark)
                            )
                            lessonsRepo.update(lsResult.lessonItem.uuid, newElem)
                            call.respond(lessonsRepo[lsResult.lessonItem.uuid]!!)
                        }
                        is LSFail -> call.respondText(lsResult.text, status = lsResult.code)
                    }
                }
                delete("marks") {
                    when (val lsResult = lsParameters()) {
                        is LSOk -> {
                            if (lsResult.studentLink !in lsResult.lessonItem.elem.students)
                                return@delete call.respondText(
                                    "No student ${lsResult.studentLink} in lesson ${lsResult.lessonItem.elem.name}",
                                    status = HttpStatusCode.NotFound
                                )
                            val oldElem = lsResult.lessonItem.elem
                            val newElem = oldElem.copy(
                                marks = oldElem.marks - lsResult.studentLink
                            )
                            lessonsRepo.update(lsResult.lessonItem.uuid, newElem)
                            call.respond(lessonsRepo[lsResult.lessonItem.uuid]!!)
                        }
                        is LSFail -> call.respondText(lsResult.text, status = lsResult.code)
                    }
                }
            }
        }
    }
}



private sealed interface LSResult
private class LSOk(
    val lessonItem: RepoItem<Lesson>,
    val studentLink: String
) : LSResult

private class LSFail(
    val text: String,
    val code: HttpStatusCode
) : LSResult

private fun PipelineContext<Unit, ApplicationCall>.lsParameters(): LSResult {
    val lessonId = call.parameters["lessonId"]
        ?: return LSFail("LessonId wrong", HttpStatusCode.BadRequest)
    val studentId = call.parameters["studentId"]
        ?: return LSFail("StudentId wrong", HttpStatusCode.BadRequest)
    val lessonItem = lessonsRepo[lessonId]
        ?: return LSFail("No lesson with id $lessonId", HttpStatusCode.NotFound)
    val studentLink = studentsRepo.urlByUUID(studentId)
        ?: return LSFail("No student with id $studentId", HttpStatusCode.NotFound)
    return LSOk(lessonItem, studentLink)
}