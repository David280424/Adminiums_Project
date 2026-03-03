package com.example.adminiums1.model

data class Visitante(
    val id: String = "",
    val nombre: String = "",
    val autorizadoPor: String = "",
    val unidad: String = "",
    val vigencia: String = "",
    val qrCode: String = "",
    val timestamp: Long = 0L,
    val validado: Boolean = false
)
