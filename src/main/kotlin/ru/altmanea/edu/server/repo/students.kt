package ru.altmanea.edu.server.repo

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import ru.altmanea.edu.server.model.Config
import ru.altmanea.edu.server.model.Item
import ru.altmanea.edu.server.model.Student

//val studentsRepo = ListRepo<Student>()
val studentsRepo = ExposedRepo(StudentsTable)

fun Repo<Student>.urlByUUID(uuid: String) =
    this[uuid]?.let {
        Config.studentsURL + it.uuid
    }

val studentsRepoTestData = listOf(
    Student("Sheldon", "Cooper"),
    Student("Leonard", "Hofstadter"),
    Student("Howard", "Wolowitz"),
    Student("Penny", "Hofstadter"),
)

object StudentsTable: ItemTable<Student>() {
    val firstname = varchar("firstname", 50)
    val surname = varchar("surname", 50)
    override fun readItem(result: ResultRow) =
        RepoItem(
            Student(
                result[firstname],
                result[surname]
            ),
            result[id].toString(),
            result[etag]
        )

    override fun fill(builder: UpdateBuilder<Number>, elem: Student) {
        builder[etag] = System.currentTimeMillis()
        builder[firstname] = elem.firstname
        builder[surname] = elem.surname
    }
}