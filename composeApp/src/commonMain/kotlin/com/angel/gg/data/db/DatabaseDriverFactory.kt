package com.angel.gg.data.db

import app.cash.sqldelight.db.SqlDriver

// "expect" = le dice a Kotlin que cada plataforma provee su implementación
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}