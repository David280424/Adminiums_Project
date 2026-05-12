package com.example.adminiums1.model

data class RegistroAcceso(
    val id             : String = "",
    val residenteUid   : String = "",
    val residenteNombre: String = "",
    val unidad         : String = "",
    val metodo         : String = "",   // "manual" | "qr" | "visitante_qr"
    val hora           : String = "",   // "HH:mm"
    val fecha          : String = "",   // "dd/MM/yyyy"
    val vigilanteNombre: String = "",   // Nombre del vigilante que registró
    val tipoPersona    : String = "residente", // "residente" | "visitante"
    val timestamp      : Long   = 0L
)
