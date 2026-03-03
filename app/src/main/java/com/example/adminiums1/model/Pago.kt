package com.example.adminiums1.model

data class Pago(
    val id: String = "",
    val residenteUid: String = "",
    val monto: Double = 0.0,
    val fecha: String = "",
    val estado: String = "" // "pagado", "pendiente"
)
