package com.angel.gg.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.angel.gg.db.GastosDatabase
import java.io.File
import java.util.Properties

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbDir  = File(System.getProperty("user.home"), ".gestorgastos")
        val dbFile = File(dbDir, "gastos.db")
        dbDir.mkdirs()

        val esNueva = !dbFile.exists()

        val driver = JdbcSqliteDriver(
            url        = "jdbc:sqlite:${dbFile.absolutePath}",
            properties = Properties().apply { put("foreign_keys", "true") }
        )

        if (esNueva) {
            // Base de datos nueva — crear todas las tablas
            GastosDatabase.Schema.create(driver)
            driver.execute(
                identifier = null,
                sql        = "PRAGMA user_version = ${GastosDatabase.Schema.version}",
                parameters = 0
            )
        } else {
            // Base de datos existente — verificar migraciones
            val currentVersion = driver.executeQuery<Long>(
                identifier = null,
                sql        = "PRAGMA user_version",
                mapper     = { cursor ->
                    QueryResult.Value(
                        if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L
                    )
                },
                parameters = 0
            ).value

            val schemaVersion = GastosDatabase.Schema.version

            if (currentVersion < schemaVersion) {
                GastosDatabase.Schema.migrate(
                    driver     = driver,
                    oldVersion = currentVersion,
                    newVersion = schemaVersion
                )
                driver.execute(
                    identifier = null,
                    sql        = "PRAGMA user_version = $schemaVersion",
                    parameters = 0
                )
            }
        }

        return driver
    }
}