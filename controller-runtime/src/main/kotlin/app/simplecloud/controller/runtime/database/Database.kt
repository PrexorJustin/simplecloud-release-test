package app.simplecloud.controller.runtime.database

import org.jooq.DSLContext

class Database(
    val context: DSLContext
) {

    fun setup() {
        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")
        val setupInputStream = Database::class.java.getResourceAsStream("/schema.sql")
            ?: throw IllegalArgumentException("Database schema not found.")
        val setupCommands = setupInputStream.bufferedReader().use { it.readText() }.split(";")
        setupCommands.forEach {
            val trimmed = it.trim()
            if (trimmed.isNotEmpty())
                context.execute(org.jooq.impl.DSL.sql(trimmed))
        }
    }

}