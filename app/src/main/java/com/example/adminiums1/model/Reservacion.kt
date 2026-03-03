package com.example.adminiums1.model

data class Reservacion(
    val id: String = "",
    val amenidad: String = "",
    val residenteUid: String = "",
    val residenteNombre: String = "",
    val unidad: String = "",
    val fecha: String = "",
    val horario: String = "",
    val timestamp: Long = 0L
)
