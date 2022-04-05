package ru.altmanea.edu.server.repo

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import ru.altmanea.edu.server.model.Lesson
import ru.altmanea.edu.server.model.Student

val lessonsRepo = ListRepo<Lesson>()

val lessonsRepoTestData = listOf(
    Lesson("Math"),
    Lesson("Phys"),
    Lesson("Story"),
)

