package com.example.adminiums1.model

data class RegistroAcceso(
    val id             : String = "",
    val residenteUid   : String = "",
    val residenteNombre: String = "",
    val unidad         : String = "",
    val metodo         : String = "",   // "manual" | "qr"
    val hora           : String = "",   // "HH:mm"
    val fecha          : String = "",   // "dd/MM/yyyy"
    val timestamp      : Long   = 0L
)
