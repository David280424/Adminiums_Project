package com.example.adminiums1.model

data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "", // "residente", "vigilante", "admin"
    val unidad: String = "",
    val edificioId: String = "", // Vínculo con el condominio
    val balance: Double = 0.0,
    val proximoPago: Double = 0.0,
    val fechaVencimiento: String = ""
)
