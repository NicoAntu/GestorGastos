package com.angel.gg.data.db

import app.cash.sqldelight.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

// Reemplaza asFlow() + mapToList()
fun <T : Any> Query<T>.toFlow(): Flow<List<T>> = callbackFlow {
    val listener = object : Query.Listener {
        override fun queryResultsChanged() {
            trySend(executeAsList())
        }
    }
    addListener(listener)
    trySend(executeAsList())
    awaitClose { removeListener(listener) }
}.flowOn(Dispatchers.Default)

// Reemplaza asFlow() + mapToOneOrNull()
fun <T : Any> Query<T>.toFlowOne(): Flow<T?> = callbackFlow {
    val listener = object : Query.Listener {
        override fun queryResultsChanged() {
            trySend(executeAsOneOrNull())
        }
    }
    addListener(listener)
    trySend(executeAsOneOrNull())
    awaitClose { removeListener(listener) }
}.flowOn(Dispatchers.Default)