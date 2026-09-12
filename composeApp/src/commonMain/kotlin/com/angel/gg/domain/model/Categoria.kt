package com.angel.gg.domain.model

data class Categoria(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val color: String,
    val icono: String,
    val orden: Int,
    val fijada: Boolean
)