package ru.altmanea.edu.server.repo

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import ru.altmanea.edu.server.model.Item
import java.util.*

abstract class ItemTable<E>(name: String = "") : UUIDTable(name) {
    val etag = long("etag")
    abstract fun readItem(result: ResultRow): RepoItem<E>
    abstract fun fill(builder: UpdateBuilder<Number>, elem: E)
}


open class ExposedRepo<E>(
    val table: ItemTable<E>
) : Repo<E> {
    override fun get(uuid: String) =
        transaction {
            table
                .select { table.id eq UUID.fromString(uuid) }
                .firstOrNull()
                ?.let { table.readItem(it) }
        }

    override fun findAll() =
        transaction {
            table
                .selectAll()
                .mapNotNull {
                    table.readItem(it)
                }
        }

    override fun create(element: E) =
        transaction {
            table.insert {
                fill(it, element)
            }
            true
        }

    override fun update(uuid: String, value: E) =
        transaction {
            table.update(
                { table.id eq UUID.fromString(uuid) }
            ) { table.fill(it, value) } > 0
        }

    override fun delete(uuid: String) =
        transaction {
            table.deleteWhere {
                table.id eq UUID.fromString(uuid)
            } > 0
        }

    override fun isEmpty() =
        transaction {
            table.select {
                table.id.isNotNull()
            }.empty()
        }


}