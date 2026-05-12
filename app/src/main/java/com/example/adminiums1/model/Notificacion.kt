package com.example.adminiums1.model

data class Notificacion(
    val id: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val timestamp: Long = 0,
    val leida: Boolean = false,
    val tipo: String = "deuda" // "deuda", "general", "incidencia"
)
