package com.example.adminiums1.model

data class Condominio(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val ciudad: String = "",
    val imagenUrl: String = "", // Para que la UI se vea pro
    val cuotaBase: Double = 0.0,
    val totalUnidades: Int = 0
)
